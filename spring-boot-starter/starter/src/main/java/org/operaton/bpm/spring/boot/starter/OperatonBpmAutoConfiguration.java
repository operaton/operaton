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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.spring.ProcessEngineFactoryBean;
import org.operaton.bpm.engine.spring.SpringProcessEngineServicesConfiguration;
import org.operaton.bpm.spring.boot.starter.event.ProcessApplicationEventPublisher;
import org.operaton.bpm.spring.boot.starter.property.ManagementProperties;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.util.OperatonBpmVersion;

@EnableConfigurationProperties({
  OperatonBpmProperties.class,
  ManagementProperties.class
})
@Import({
  OperatonBpmConfiguration.class,
  OperatonBpmActuatorConfiguration.class,
  OperatonBpmHealthServiceConfiguration.class,
  OperatonBpmPluginConfiguration.class,
  OperatonBpmTelemetryConfiguration.class,
  SpringProcessEngineServicesConfiguration.class
})
@Configuration
@ConditionalOnProperty(prefix = OperatonBpmProperties.PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
public class OperatonBpmAutoConfiguration {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Configuration
  class ProcessEngineConfigurationImplDependingConfiguration {

    protected ProcessEngineConfigurationImpl processEngineConfigurationImpl;

    @Autowired
    public ProcessEngineConfigurationImplDependingConfiguration(ProcessEngineConfigurationImpl processEngineConfigurationImpl){
      this.processEngineConfigurationImpl = processEngineConfigurationImpl;
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean() {
      final ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
      factoryBean.setProcessEngineConfiguration(processEngineConfigurationImpl);

      return factoryBean;
    }

    @Bean
    @Primary
    public CommandExecutor commandExecutorTxRequired() {
      return processEngineConfigurationImpl.getCommandExecutorTxRequired();
    }

    @Bean
    public CommandExecutor commandExecutorTxRequiresNew() {
      return processEngineConfigurationImpl.getCommandExecutorTxRequiresNew();
    }

    @Bean
    public CommandExecutor commandExecutorSchemaOperations() {
      return processEngineConfigurationImpl.getCommandExecutorSchemaOperations();
    }
  }

  @Bean
  public OperatonBpmVersion operatonBpmVersion() {
    return new OperatonBpmVersion();
  }

  @Bean
  public ProcessApplicationEventPublisher processApplicationEventPublisher(ApplicationEventPublisher publisher) {
    return new ProcessApplicationEventPublisher(publisher);
  }

}
