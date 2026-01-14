/*
 * Copyright and/or licensed under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. This file is licensed to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import static java.util.Arrays.asList;

/**
 * This JUnit 5 extension is intended to make the conversion from JUnit 4 Tests
 * like ContainerAuthenticationFilterTest as easy as possible. The issue is,
 * that these tests are run with @RunWith(org.junit.runners.Parameterized.class)
 * and use a static method annotated
 * with @org.junit.runners.Parameterized.Parameters to inject parameters into
 * the constructor of the test class or by using field injection
 * with @Paramater(0), @Parameter(1). This extension implements the same
 * mechanism for JUnit 5.
 *
 * To migrate the tests you can follow the following recipe:
 *
 * <ol>
 * <li>Remove the junit 4 imports
 *
 * <pre>
 * import org.junit.After;
 * import org.junit.Assert;
 * import org.junit.Before;
 * import org.junit.Test;
 * import org.junit.runner.RunWith;
 * import org.junit.runners.Parameterized;
 * import org.junit.runners.Parameterized.Parameters;
 * </pre>
 *
 * </li>
 *
 * <li>Add the imports for junit 5 and this class
 *
 * <pre>
 * import org.junit.jupiter.api.*;
 * import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
 * import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
 * import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
 * </pre>
 *
 * </li>
 * <li>Replace the class @RunWith(Parameterized.class) annotation
 * with @Parameterized</li>
 *
 * <li>Replace @Before with @BeforeEach and @After with @AfterEach</li>
 *
 * <li>Replace each @Test with @TestTemplate</li>
 *
 * <li>Use assertion methods from Assertions instead from Assert</li>
 * </ol>
 *
 */
public class ParameterizedTestExtension implements TestTemplateInvocationContextProvider {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Parameter {
    int value() default 0;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Parameters {
    String name() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @ExtendWith(ParameterizedTestExtension.class)
  public @interface Parameterized {
  }

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    // This extension “activates” if the test class is annotated with our marker
    return context.getTestClass().map(cls -> cls.isAnnotationPresent(Parameterized.class)).orElse(false);
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    // Look for a static method annotated with @Parameters in the class and it's superclasses

    Class<?> c = testClass;
    Method parametersMethod = null;
    do {
      Optional<Method> first = Arrays.stream(c.getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(Parameters.class) && Modifier.isStatic(m.getModifiers()))
        .findFirst();
      if (first.isPresent()) {
        parametersMethod = first.get();
        break;
      }
      c = c.getSuperclass();
    } while(c != null);
    if (parametersMethod == null) {
      throw new ExtensionConfigurationException("No static @Parameters method found in " + testClass.getName());
    }

    // Retrieve the name pattern from the annotation.
    Parameters parametersAnnotation = parametersMethod.getAnnotation(Parameters.class);
    String displayNameFormat = parametersAnnotation.name();

    Object parametersResult;
    try {
      parametersMethod.setAccessible(true);
      parametersResult = parametersMethod.invoke(null);
    } catch (Exception e) {
      throw new ExtensionConfigurationException("Failed to invoke @Parameters method", e);
    }

    if (!(parametersResult instanceof Collection)) {
      throw new ExtensionConfigurationException("@Parameters method must return a Collection<Object>");
    }

    @SuppressWarnings("unchecked")
    Collection<Object> parameterSets = (Collection<Object>) parametersResult;
    return parameterSets.stream().map(params -> new ParameterizedTestInvocationContext(params, displayNameFormat));
  }

  private static class ParameterizedTestInvocationContext implements TestTemplateInvocationContext {

    private final Object[] parameters;
    private final String displayNameFormat;

    ParameterizedTestInvocationContext(Object parameters, String displayNameFormat) {
      this.displayNameFormat = displayNameFormat;
      if (parameters instanceof Object[] parametersArray) {
        this.parameters = parametersArray;
      } else {
        this.parameters = new Object[] { parameters };
      }
    }

    @Override
    public String getDisplayName(int invocationIndex) {
      if ("".equals(displayNameFormat)) {
        return "Parameters: " + Arrays.toString(parameters);
      }
      return MessageFormat.format(displayNameFormat, parameters);
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return asList(
          // ParameterResolver for method/constructor parameters
          new ParameterResolver() {
            @Override
            public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
              // we assume the number of parameters from the static method exactly match the
              // number needed.
              return true;
            }

            @Override
            public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
              int index = parameterContext.getIndex();
              if (index >= parameters.length) {
                throw new ParameterResolutionException(
                    "Not enough parameters provided; index %d out of %d".formatted(index, parameters.length));
              }
              return parameters[index];
            }
          },
          // TestInstanceFactory to create a new test instance using the parameters
              (TestInstanceFactory) (factoryContext, extensionContext) -> {
                try {
                  Class<?> testClass = extensionContext.getRequiredTestClass();
                  // assume the test class has a single constructor.
                  Constructor<?> constructor = testClass.getDeclaredConstructors()[0];
                  constructor.setAccessible(true);
                  return constructor.newInstance(parameters);
                } catch (Exception e) {
                  throw new TestInstantiationException("Could not create test instance", e);
                }
              },
          // TestInstancePostProcessor for field injection using @Parameter(X)
          new TestInstancePostProcessor() {
            @Override
            public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
              // Iterate over declared fields and inject values for those annotated with
              // @Parameter
              Class<?> c = testInstance.getClass();
              do {
                addFields(testInstance, c);
                c = c.getSuperclass();
              } while(c != null);
            }

            private void addFields(Object testInstance, Class<?> c)
                throws IllegalArgumentException, IllegalAccessException {
              for (Field field : c.getDeclaredFields()) {
                Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
                if (parameterAnnotation != null) {
                  int index = parameterAnnotation.value();
                  if (index < 0 || index >= parameters.length) {
                    throw new ExtensionConfigurationException(
                        "Index %d is out of bounds for the provided parameters array.".formatted(index));
                  }
                  field.setAccessible(true);
                  field.set(testInstance, parameters[index]);
                }
              }
            }
          });
    }
  }
}
