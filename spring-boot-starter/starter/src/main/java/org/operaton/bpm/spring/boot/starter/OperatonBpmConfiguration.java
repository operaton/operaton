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

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.CompositeProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonAuthorizationConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDatasourceConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDeploymentConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonFailedJobConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonHistoryConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonHistoryLevelAutoHandlingConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonJobConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonMetricsConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.condition.NeedsHistoryAutoConfigurationCondition;
import org.operaton.bpm.spring.boot.starter.configuration.id.IdGeneratorConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultAuthorizationConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultDatasourceConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultDeploymentConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultFailedJobConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultHistoryConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultHistoryLevelAutoHandlingConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultJobConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultJobConfiguration.JobConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultMetricsConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.GenericPropertiesConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.custom.CreateAdminUserConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.impl.custom.CreateFilterConfiguration;
import org.operaton.bpm.spring.boot.starter.event.EventPublisherPlugin;
import org.operaton.bpm.spring.boot.starter.jdbc.HistoryLevelDeterminator;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.telemetry.OperatonIntegrationDeterminator;
import org.operaton.bpm.spring.boot.starter.util.OperatonSpringBootUtil;

import static org.operaton.bpm.spring.boot.starter.jdbc.HistoryLevelDeterminatorJdbcTemplateImpl.createHistoryLevelDeterminator;

@Import({JobConfiguration.class, IdGeneratorConfiguration.class})
public class OperatonBpmConfiguration {

  @Bean
  @ConditionalOnMissingBean(ProcessEngineConfigurationImpl.class)
  public ProcessEngineConfigurationImpl processEngineConfigurationImpl(List<ProcessEnginePlugin> processEnginePlugins) {
    final SpringProcessEngineConfiguration configuration = OperatonSpringBootUtil.springProcessEngineConfiguration();
    configuration.getProcessEnginePlugins().add(new CompositeProcessEnginePlugin(processEnginePlugins));
    return configuration;
  }

  @Bean
  @ConditionalOnMissingBean(DefaultProcessEngineConfiguration.class)
  public static OperatonProcessEngineConfiguration operatonProcessEngineConfiguration(OperatonBpmProperties operatonBpmProperties,
                                                                                      Optional<IdGenerator> idGenerator) {
    return new DefaultProcessEngineConfiguration(operatonBpmProperties, idGenerator);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonDatasourceConfiguration.class)
  public static OperatonDatasourceConfiguration operatonDatasourceConfiguration(OperatonBpmProperties operatonBpmProperties,
                                                                                PlatformTransactionManager transactionManager,
                                                                                @Qualifier("operatonBpmTransactionManager") Optional<PlatformTransactionManager> operatonTransactionManager,
                                                                                DataSource dataSource,
                                                                                @Qualifier("operatonBpmDataSource") Optional<DataSource> operatonDataSource) {
    return new DefaultDatasourceConfiguration(operatonBpmProperties, transactionManager, operatonTransactionManager.orElse(null),
            dataSource, operatonDataSource.orElse(null));
  }

  @Bean
  @ConditionalOnMissingBean(OperatonJobConfiguration.class)
  @ConditionalOnProperty(prefix = "operaton.bpm.job-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
  public static OperatonJobConfiguration operatonJobConfiguration(OperatonBpmProperties operatonBpmProperties,
                                                                  JobExecutor jobExecutor) {
    return new DefaultJobConfiguration(operatonBpmProperties, jobExecutor);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonHistoryConfiguration.class)
  public static OperatonHistoryConfiguration operatonHistoryConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new DefaultHistoryConfiguration(operatonBpmProperties);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonMetricsConfiguration.class)
  public static OperatonMetricsConfiguration operatonMetricsConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new DefaultMetricsConfiguration(operatonBpmProperties);
  }

  //TODO to be removed within CAM-8108
  @Bean(name = "historyLevelAutoConfiguration")
  @ConditionalOnMissingBean(OperatonHistoryLevelAutoHandlingConfiguration.class)
  @ConditionalOnProperty(prefix = "operaton.bpm", name = "history-level", havingValue = "auto", matchIfMissing = false)
  @Conditional(NeedsHistoryAutoConfigurationCondition.class)
  public static OperatonHistoryLevelAutoHandlingConfiguration historyLevelAutoHandlingConfiguration(
      OperatonBpmProperties operatonBpmProperties,
      HistoryLevelDeterminator historyLevelDeterminator) {
    return new DefaultHistoryLevelAutoHandlingConfiguration(operatonBpmProperties, historyLevelDeterminator);
  }

  //TODO to be removed within CAM-8108
  @Bean(name = "historyLevelDeterminator")
  @ConditionalOnMissingBean(name = { "operatonBpmJdbcTemplate", "historyLevelDeterminator" })
  @ConditionalOnBean(name = "historyLevelAutoConfiguration")
  public static HistoryLevelDeterminator historyLevelDeterminator(OperatonBpmProperties operatonBpmProperties,
                                                                  JdbcTemplate jdbcTemplate) {
    return createHistoryLevelDeterminator(operatonBpmProperties, jdbcTemplate);
  }

  //TODO to be removed within CAM-8108
  @Bean(name = "historyLevelDeterminator")
  @ConditionalOnBean(name = { "operatonBpmJdbcTemplate", "historyLevelAutoConfiguration", "historyLevelDeterminator" })
  @ConditionalOnMissingBean(name = "historyLevelDeterminator")
  public static HistoryLevelDeterminator historyLevelDeterminatorMultiDatabase(OperatonBpmProperties operatonBpmProperties,
                                                                               @Qualifier("operatonBpmJdbcTemplate") JdbcTemplate jdbcTemplate) {
    return createHistoryLevelDeterminator(operatonBpmProperties, jdbcTemplate);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonAuthorizationConfiguration.class)
  public static OperatonAuthorizationConfiguration operatonAuthorizationConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new DefaultAuthorizationConfiguration(operatonBpmProperties);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonDeploymentConfiguration.class)
  public static OperatonDeploymentConfiguration operatonDeploymentConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new DefaultDeploymentConfiguration(operatonBpmProperties);
  }

  @Bean
  public GenericPropertiesConfiguration genericPropertiesConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new GenericPropertiesConfiguration(operatonBpmProperties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "operaton.bpm.admin-user", name = "id")
  public CreateAdminUserConfiguration createAdminUserConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new CreateAdminUserConfiguration(operatonBpmProperties);
  }

  @Bean
  @ConditionalOnMissingBean(OperatonFailedJobConfiguration.class)
  public static OperatonFailedJobConfiguration failedJobConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new DefaultFailedJobConfiguration(operatonBpmProperties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "operaton.bpm.filter", name = "create")
  public CreateFilterConfiguration createFilterConfiguration(OperatonBpmProperties operatonBpmProperties) {
    return new CreateFilterConfiguration(operatonBpmProperties);
  }

  @Bean
  public EventPublisherPlugin eventPublisherPlugin(OperatonBpmProperties properties,
                                                   ApplicationEventPublisher publisher) {
    return new EventPublisherPlugin(properties.getEventing(), publisher);
  }

  @Bean
  public OperatonIntegrationDeterminator operatonIntegrationDeterminator(ProcessEngine processEngine) {
    return new OperatonIntegrationDeterminator(processEngine);
  }
}
