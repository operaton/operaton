/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.standalone.scripting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.scripting.env.ScriptEnvResolver;
import org.operaton.bpm.engine.repository.ProcessApplicationDeployment;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class EnvScriptCachingTest extends AbstractScriptEnvironmentTest {

  protected static final String SCRIPT_LANGUAGE = "groovy";
  protected static final String SCRIPT = "println 'hello world'";
  protected static final String ENV_SCRIPT = "println 'hello world from env script'";

  @Override
  protected ScriptEnvResolver getResolver() {
    return language -> new String[] { ENV_SCRIPT };
  }

  @Override
  protected String getScript() {
    return SCRIPT;
  }

  @Test
  public void testEnabledPaEnvScriptCaching() {
    // given
    ProcessApplicationDeployment deployment = repositoryService.createDeployment(processApplication.getReference())
        .addClasspathResource(processPath)
        .deploy();

    // when
    executeScript(processApplication, SCRIPT_LANGUAGE);

    // then
    Map<String, List<ExecutableScript>> environmentScripts = processApplication.getEnvironmentScripts();
    assertThat(environmentScripts).isNotNull();

    List<ExecutableScript> groovyEnvScripts = environmentScripts.get(SCRIPT_LANGUAGE);

    assertThat(groovyEnvScripts).isNotNull();
    assertThat(groovyEnvScripts).isNotEmpty();
    assertThat(groovyEnvScripts).hasSize(processEngineConfiguration.getEnvScriptResolvers().size());

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  public void testDisabledPaEnvScriptCaching() {
    // given
    processEngineConfiguration.setEnableFetchScriptEngineFromProcessApplication(false);

    ProcessApplicationDeployment deployment = repositoryService.createDeployment(processApplication.getReference())
        .addClasspathResource(processPath)
        .deploy();

    // when
    executeScript(processApplication, SCRIPT_LANGUAGE);

    // then
    Map<String, List<ExecutableScript>> environmentScripts = processApplication.getEnvironmentScripts();
    assertThat(environmentScripts).isNotNull();
    assertThat(environmentScripts.get(SCRIPT_LANGUAGE)).isNull();

    repositoryService.deleteDeployment(deployment.getId(), true);

    processEngineConfiguration.setEnableFetchScriptEngineFromProcessApplication(true);
  }

}
