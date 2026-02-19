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

import java.util.Optional;

import org.springframework.util.StringUtils;

import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

public class DefaultProcessEngineConfiguration extends AbstractOperatonConfiguration
    implements OperatonProcessEngineConfiguration {

  private final Optional<IdGenerator> idGenerator;

  public DefaultProcessEngineConfiguration(OperatonBpmProperties operatonBpmProperties,
                                           Optional<IdGenerator> idGenerator) {
    super(operatonBpmProperties);
    this.idGenerator = idGenerator;
  }

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    setProcessEngineName(configuration);
    setDefaultSerializationFormat(configuration);
    setIdGenerator(configuration);
    setJobExecutorAcquireByPriority(configuration);
    setJobExecutorAcquireWithSkipLocked(configuration);
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
      LOG.ignoringInvalidDefaultSerializationFormat(defaultSerializationFormat);
    }
  }

  private void setProcessEngineName(SpringProcessEngineConfiguration configuration) {
    String processEngineName = StringUtils.trimAllWhitespace(operatonBpmProperties.getProcessEngineName());
    if (StringUtils.hasText(processEngineName) && !processEngineName.contains("-")) {

      if (Boolean.TRUE.equals(operatonBpmProperties.getGenerateUniqueProcessEngineName())) {
        if (!ProcessEngines.NAME_DEFAULT.equals(processEngineName)) {
          throw new RuntimeException(("A unique processEngineName cannot be generated "
                  + "if a custom processEngineName is already set: %s").formatted(processEngineName));
        }
        processEngineName = OperatonBpmProperties.getUniqueName(OperatonBpmProperties.UNIQUE_ENGINE_NAME_PREFIX);
      }

      configuration.setProcessEngineName(processEngineName);
    } else {
      LOG.ignoringInvalidProcessEngineName(operatonBpmProperties.getProcessEngineName());
    }
  }

  private void setJobExecutorAcquireByPriority(SpringProcessEngineConfiguration configuration) {
    Optional.ofNullable(operatonBpmProperties.getJobExecutorAcquireByPriority())
            .ifPresent(configuration::setJobExecutorAcquireByPriority);
  }

  private void setJobExecutorAcquireWithSkipLocked(SpringProcessEngineConfiguration configuration) {
    Optional.ofNullable(operatonBpmProperties.getJobExecutorAcquireWithSkipLocked())
      .ifPresent(configuration::setJobExecutorAcquireWithSkipLocked);
  }

  private void setDefaultNumberOfRetries(SpringProcessEngineConfiguration configuration) {
    Optional.ofNullable(operatonBpmProperties.getDefaultNumberOfRetries())
            .ifPresent(configuration::setDefaultNumberOfRetries);
  }
}
