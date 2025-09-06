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
package org.operaton.bpm.spring.boot.starter.telemetry;

import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.OperatonIntegration;

@ConditionalOnBean(ProcessEngine.class)
public class OperatonIntegrationDeterminator implements InitializingBean {

  protected ProcessEngine processEngine;

  public OperatonIntegrationDeterminator(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    Set<String> operatonIntegration = configuration.getTelemetryData()
        .getProduct()
        .getInternals()
        .getOperatonIntegration();
    operatonIntegration.add(OperatonIntegration.SPRING_BOOT_STARTER);
  }

}
