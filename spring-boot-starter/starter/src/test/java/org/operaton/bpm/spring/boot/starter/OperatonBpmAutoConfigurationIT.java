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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.impl.cfg.CompositeProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.operaton.spin.plugin.impl.SpinObjectValueSerializer;
import org.operaton.spin.plugin.impl.SpinProcessEnginePlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@SpringBootTest(classes = { TestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class OperatonBpmAutoConfigurationIT {

  @Autowired
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Autowired
  private ApplicationContext appContext;

  @Test
  void ensureProcessEngineServicesAreExposedAsBeans() {
    for (Class<?> classToCheck : getProcessEngineServicesClasses()) {
      Object bean = appContext.getBean(classToCheck);
      assertNotNull(classToCheck + " must be exposed as @Bean. Check configuration", bean);
      String beanName = convertToBeanName(classToCheck);
      assertSame(classToCheck + " must be exposed as '" + beanName + "'. Check configuration", bean, appContext.getBean(beanName));
    }

  }

  @Test
  void ensureSpinProcessEnginePluginIsCorrectlyLoaded() {
    // given
    List<ProcessEnginePlugin> plugins = processEngineConfiguration.getProcessEnginePlugins();
    List<TypedValueSerializer<?>> serializers = processEngineConfiguration.getVariableSerializers().getSerializers();

    if (plugins.get(0) instanceof CompositeProcessEnginePlugin) {
      plugins = ((CompositeProcessEnginePlugin) plugins.get(0)).getPlugins();
    }

    boolean isJacksonJsonDataFormat = serializers.stream().anyMatch(s ->
        s instanceof SpinObjectValueSerializer
        && s.getSerializationDataformat().equals("application/json"));

    // then
    assertThat(plugins.stream().anyMatch(plugin -> plugin instanceof SpinProcessEnginePlugin)).isTrue();
    assertThat(isJacksonJsonDataFormat).isTrue();
  }

  @Test
  void ensureConnectProcessEnginePluginIsCorrectlyLoaded() {
    // given
    List<ProcessEnginePlugin> plugins = processEngineConfiguration.getProcessEnginePlugins();

    if (plugins.get(0) instanceof CompositeProcessEnginePlugin) {
      plugins = ((CompositeProcessEnginePlugin) plugins.get(0)).getPlugins();
    }

    // then
    assertThat(plugins.stream().anyMatch(plugin -> plugin instanceof ConnectProcessEnginePlugin)).isTrue();
  }

  private String convertToBeanName(Class<?> beanClass) {
    return StringUtils.uncapitalize(beanClass.getSimpleName());
  }

  private List<Class<?>> getProcessEngineServicesClasses() {
    List<Class<?>> classes = new ArrayList<>();
    for (Method method : ProcessEngineServices.class.getMethods()) {
      classes.add(method.getReturnType());
    }
    return classes;
  }

}
