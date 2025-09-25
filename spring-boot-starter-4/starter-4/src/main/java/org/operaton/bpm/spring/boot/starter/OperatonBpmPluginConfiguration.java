/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
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
package org.operaton.bpm.spring.boot.starter;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.devtools.restart.ConditionalOnInitializedRestarter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.spring.boot.starter.plugin.ApplicationContextClassloaderSwitchPlugin;
import org.operaton.bpm.spring.boot.starter.spin.OperatonJacksonFormatConfiguratorJSR310;
import org.operaton.bpm.spring.boot.starter.spin.OperatonJacksonFormatConfiguratorJdk8;
import org.operaton.bpm.spring.boot.starter.spin.OperatonJacksonFormatConfiguratorParameterNames;
import org.operaton.bpm.spring.boot.starter.spin.SpringBootSpinProcessEnginePlugin;
import org.operaton.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.operaton.spin.plugin.impl.SpinProcessEnginePlugin;

@Configuration
public class OperatonBpmPluginConfiguration {

  /*
     When `operaton-spin-dataformat-all` is used as a dependency,
     SpinDataFormatConfigurationJSR310, SpinDataFormatConfigurationParameterNames
     and SpinDataFormatConfigurationJdk8 are not used. The `operaton-spin-dataformat-all`
     artifact comes with a shaded Jackson ObjectMapper (prefixed with `spinjar`),
     which breaks auto-configuration for Jackson Java 8 modules.
  */

  @ConditionalOnClass({ JacksonJsonDataFormat.class, JavaTimeModule.class })
  @ConditionalOnMissingClass("spinjar.com.fasterxml.jackson.databind.ObjectMapper")
  @Configuration
  static class SpinDataFormatConfigurationJSR310 {

    @Bean
    @ConditionalOnMissingBean(name = "spinDataFormatConfiguratorJSR310")
    public static OperatonJacksonFormatConfiguratorJSR310 spinDataFormatConfiguratorJSR310() {
      return new OperatonJacksonFormatConfiguratorJSR310();
    }

  }

  @ConditionalOnClass({ JacksonJsonDataFormat.class, ParameterNamesModule.class })
  @ConditionalOnMissingClass("spinjar.com.fasterxml.jackson.databind.ObjectMapper")
  @Configuration
  static class SpinDataFormatConfigurationParameterNames {

    @Bean
    @ConditionalOnMissingBean(name = "spinDataFormatConfiguratorParameterNames")
    public static OperatonJacksonFormatConfiguratorParameterNames spinDataFormatConfiguratorParameterNames() {
      return new OperatonJacksonFormatConfiguratorParameterNames();
    }

  }

  @ConditionalOnClass({ JacksonJsonDataFormat.class, Jdk8Module.class })
  @ConditionalOnMissingClass("spinjar.com.fasterxml.jackson.databind.ObjectMapper")
  @Configuration
  static class SpinDataFormatConfigurationJdk8 {

    @Bean
    @ConditionalOnMissingBean(name = "spinDataFormatConfiguratorJdk8")
    public static OperatonJacksonFormatConfiguratorJdk8 spinDataFormatConfiguratorJdk8() {
      return new OperatonJacksonFormatConfiguratorJdk8();
    }

  }

  @ConditionalOnClass(SpinProcessEnginePlugin.class)
  @Configuration
  static class SpinConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "spinProcessEnginePlugin")
    public static ProcessEnginePlugin spinProcessEnginePlugin(Optional<OperatonJacksonFormatConfiguratorJSR310> dataFormatConfiguratorJsr310,
                                                              Optional<OperatonJacksonFormatConfiguratorParameterNames> dataFormatConfiguratorParameterNames,
                                                              Optional<OperatonJacksonFormatConfiguratorJdk8> dataFormatConfiguratorJdk8) {
      return new SpringBootSpinProcessEnginePlugin(dataFormatConfiguratorJsr310, dataFormatConfiguratorParameterNames,
          dataFormatConfiguratorJdk8);
    }

  }

  @ConditionalOnClass(ConnectProcessEnginePlugin.class)
  @Configuration
  static class ConnectConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "connectProcessEnginePlugin")
    public static ProcessEnginePlugin connectProcessEnginePlugin() {
      return new ConnectProcessEnginePlugin();
    }
  }

  /*
    Provide option to apply application context classloader switch when Spring
    Spring Developer tools are enabled
    For more details: https://jira.camunda.com/browse/CAM-9043
   */
  @ConditionalOnInitializedRestarter
  @Configuration
  static class InitializedRestarterConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "applicationContextClassloaderSwitchPlugin")
    public ApplicationContextClassloaderSwitchPlugin applicationContextClassloaderSwitchPlugin() {
      return new ApplicationContextClassloaderSwitchPlugin();
    }
  }

}
