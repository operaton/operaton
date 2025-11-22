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

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.health.HealthService;
import org.operaton.bpm.spring.boot.starter.actuator.JobExecutorHealthIndicator;
import org.operaton.bpm.spring.boot.starter.actuator.ProcessEngineHealthIndicator;
import org.operaton.bpm.spring.boot.starter.actuator.OperatonHealthIndicator;

@Configuration
@ConditionalOnProperty(prefix = "management.health.operaton", name = "enabled", matchIfMissing = true)
@ConditionalOnClass(HealthIndicator.class)
@DependsOn("runtimeService")
public class OperatonBpmActuatorConfiguration {

  @Bean
  @ConditionalOnBean(name = "jobExecutor")
  @ConditionalOnMissingBean(name = "jobExecutorHealthIndicator")
  public HealthIndicator jobExecutorHealthIndicator(JobExecutor jobExecutor) {
    return new JobExecutorHealthIndicator(jobExecutor);
  }

  @Bean
  @ConditionalOnMissingBean(name = "processEngineHealthIndicator")
  public HealthIndicator processEngineHealthIndicator(ProcessEngine processEngine) {
    return new ProcessEngineHealthIndicator(processEngine);
  }

  @Bean
  @ConditionalOnBean(HealthService.class)
  @ConditionalOnMissingBean(name = "operatonHealthIndicator")
  public HealthIndicator operatonHealthIndicator(HealthService healthService) {
    return new OperatonHealthIndicator(healthService);
  }
}
