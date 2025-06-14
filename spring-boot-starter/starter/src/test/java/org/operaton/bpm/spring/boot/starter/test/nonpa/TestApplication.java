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
package org.operaton.bpm.spring.boot.starter.test.nonpa;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.spin.plugin.impl.SpinProcessEnginePlugin;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@SuppressWarnings("unused")
public class TestApplication {

  @ConditionalOnProperty(prefix = "operaton.bpm", name = "process-engine-name", havingValue = "nojpaTestEngine")
  @Configuration
  public static class TestConfiguration {

    @Bean(name = "spinProcessEnginePlugin")
    public ProcessEnginePlugin spinProcessEnginePlugin() {
      return new SpinProcessEnginePlugin() {

        // When testing the NoJpaAutoConfigurationIT test, ensure that no Custom DataFormat
        // Serializers are loaded, otherwise the test's assumption will fail
        @Override
        public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
          registerFunctionMapper(processEngineConfiguration);
          registerScriptResolver(processEngineConfiguration);
          registerValueTypes(processEngineConfiguration);
          registerFallbackSerializer(processEngineConfiguration);
        }
      };
    }
  }

  @Bean
  public HistoryEventHandler customHistoryEventHandler() {
    return new CustomHistoryEventHandler();
  }

  public static class CustomHistoryEventHandler implements HistoryEventHandler {
    @Override
    public void handleEvent(HistoryEvent historyEvent) {
      // noop
    }

    @Override
    public void handleEvents(List<HistoryEvent> list) {
      // noop
    }
  }
}
