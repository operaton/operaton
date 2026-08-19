/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.operaton.bpm.wildfly.modules;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the assembled module tree from {@code target/modules} with JBoss Modules and evaluates
 * FEEL expressions through the {@code org.camunda.feel.feel-engine} module, exactly as Wildfly
 * does at runtime. This verifies that every runtime dependency of the unshaded feel-engine is
 * visible to the module's classloader - a plain classpath test cannot catch a missing or
 * non-exported module dependency.
 *
 * <p>The error-reporting path is tested separately: fastparse builds parse error messages by
 * re-parsing with failure tracing enabled, which instantiates classes (e.g.
 * {@code sourcecode.Name}) that a successful parse never touches.
 */
class FeelEngineModuleIT {

  private static Object feelEngineApi;
  private static Method evaluateExpression;

  @BeforeAll
  static void createFeelEngineFromModuleTree() throws Exception {
    Path modules = Path.of("target", "modules");
    assertThat(modules.resolve("org/camunda/feel/feel-engine/main/module.xml"))
        .as("assembled module tree, created in the prepare-package phase")
        .exists();

    ModuleLoader moduleLoader = new LocalModuleLoader(new File[] {
        modules.toFile(),
        createSlf4jModule().toFile()
    });
    ClassLoader feelEngineModule =
        moduleLoader.loadModule("org.camunda.feel.feel-engine").getClassLoader();

    Class<?> builderClass = feelEngineModule.loadClass("org.camunda.feel.api.FeelEngineBuilder");
    Object builder = builderClass.getMethod("forJava").invoke(null);
    feelEngineApi = builder.getClass().getMethod("build").invoke(builder);

    for (Method method : feelEngineApi.getClass().getMethods()) {
      if ("evaluateExpression".equals(method.getName())
          && method.getParameterCount() == 2
          && method.getParameterTypes()[0] == String.class
          && method.getParameterTypes()[1] == Map.class) {
        evaluateExpression = method;
        break;
      }
    }
    assertThat(evaluateExpression).as("FeelEngineApi.evaluateExpression(String, Map)").isNotNull();
  }

  /**
   * Wildfly provides the {@code org.slf4j} module, the distribution does not ship it. Provide a
   * minimal replacement so the feel-engine module's dependencies resolve outside of a server.
   */
  private static Path createSlf4jModule() throws Exception {
    Path slf4jJar = Path.of(
        org.slf4j.Logger.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path root = Path.of("target", "feel-it-modules");
    Path moduleDir = root.resolve("org/slf4j/main");
    Files.createDirectories(moduleDir);
    Files.copy(slf4jJar, moduleDir.resolve("slf4j-api.jar"), StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(moduleDir.resolve("module.xml"), """
        <module xmlns="urn:jboss:module:1.0" name="org.slf4j">
          <resources>
            <resource-root path="slf4j-api.jar" />
          </resources>
        </module>
        """);
    return root;
  }

  @Test
  void validExpressionEvaluatesSuccessfully() throws Exception {
    Object result = evaluateExpression.invoke(feelEngineApi,
        "if 1 + 2 > 2 then \"high\" else \"low\"", new HashMap<String, Object>());

    assertThat(isSuccess(result)).as("evaluation result: %s", result).isTrue();
  }

  @Test
  void parseErrorIsReportedAsEvaluationFailure() throws Exception {
    Object result = evaluateExpression.invoke(feelEngineApi,
        "1 +* not a valid expression", new HashMap<String, Object>());

    assertThat(isSuccess(result)).as("evaluation result: %s", result).isFalse();
    assertThat(result.toString()).contains("failed to parse expression");
  }

  private static boolean isSuccess(Object evaluationResult) throws Exception {
    return (boolean) evaluationResult.getClass().getMethod("isSuccess").invoke(evaluationResult);
  }
}
