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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class DefaultProcessEngineConfiguration extends AbstractOperatonConfiguration implements OperatonProcessEngineConfiguration {

  @Autowired
  private Optional<IdGenerator> idGenerator;

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    setProcessEngineName(configuration);
    setDefaultSerializationFormat(configuration);
    setIdGenerator(configuration);
    setJobExecutorAcquireByPriority(configuration);
    setDefaultNumberOfRetries(configuration);
  }

  private void setIdGenerator(SpringProcessEngineConfiguration configuration) {
    idGenerator.ifPresent(configuration::setIdGenerator);
  }

  private void setDefaultSerializationFormat(SpringProcessEngineConfiguration configuration) {
    String defaultSerializationFormat = operatonBpmProperties.getDefaultSerializationFormat();
    if (StringUtils.hasText(defaultSerializationFormat)) {
      configuration.setDefaultSerializationFormat(defaultSerializationFormat);
    } else {
      logger.warn("Ignoring invalid defaultSerializationFormat='{}'", defaultSerializationFormat);
    }
  }

  private void setProcessEngineName(SpringProcessEngineConfiguration configuration) {
    String processEngineName = StringUtils.trimAllWhitespace(operatonBpmProperties.getProcessEngineName());
    if (!StringUtils.isEmpty(processEngineName) && !processEngineName.contains("-")) {

      if (operatonBpmProperties.getGenerateUniqueProcessEngineName()) {
        if (!processEngineName.equals(ProcessEngines.NAME_DEFAULT)) {
          throw new RuntimeException(String.format("A unique processEngineName cannot be generated "
            + "if a custom processEngineName is already set: %s", processEngineName));
        }
        processEngineName = OperatonBpmProperties.getUniqueName(operatonBpmProperties.UNIQUE_ENGINE_NAME_PREFIX);
      }

      configuration.setProcessEngineName(processEngineName);
    } else {
      logger.warn("Ignoring invalid processEngineName='{}' - must not be null, blank or contain hyphen", operatonBpmProperties.getProcessEngineName());
    }
  }

  private void setJobExecutorAcquireByPriority(SpringProcessEngineConfiguration configuration) {
    Optional.ofNullable(operatonBpmProperties.getJobExecutorAcquireByPriority())
      .ifPresent(configuration::setJobExecutorAcquireByPriority);
  }

  private void setDefaultNumberOfRetries(SpringProcessEngineConfiguration configuration) {
    Optional.ofNullable(operatonBpmProperties.getDefaultNumberOfRetries())
      .ifPresent(configuration::setDefaultNumberOfRetries);
  }
}
