/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.health.DefaultHealthService;
import org.operaton.bpm.health.FrontendHealthContributor;
import org.operaton.bpm.health.HealthService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class OperatonBpmHealthServiceConfiguration {

  @Bean
  @ConditionalOnMissingBean(HealthService.class)
  public HealthService operatonHealthService(ObjectProvider<DataSource> dataSourceProvider,
                                             ObjectProvider<JobExecutor> jobExecutorProvider,
                                             ObjectProvider<FrontendHealthContributor> frontendHealthContributorProvider) {
    return new DefaultHealthService(
      dataSourceProvider.getIfAvailable(),
      jobExecutorProvider.getIfAvailable(),
      frontendHealthContributorProvider.getIfAvailable()
    );
  }
}
