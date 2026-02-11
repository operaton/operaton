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

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.NotifyAcquisitionRejectedJobsHandler;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.engine.spring.components.jobexecutor.SpringJobExecutor;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonJobConfiguration;
import org.operaton.bpm.spring.boot.starter.event.JobExecutorStartingEventListener;
import org.operaton.bpm.spring.boot.starter.property.JobExecutionProperty;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.operaton.bpm.spring.boot.starter.util.OperatonSpringBootUtil.join;

/**
 * Prepares JobExecutor and registers all known custom JobHandlers.
 */
public class DefaultJobConfiguration extends AbstractOperatonConfiguration implements OperatonJobConfiguration {

  protected JobExecutor jobExecutor;

  protected List<JobHandler<?>> customJobHandlers;

  public DefaultJobConfiguration(OperatonBpmProperties operatonBpmProperties,
                                 JobExecutor jobExecutor) {
    super(operatonBpmProperties);
    this.jobExecutor = jobExecutor;
  }

  @Override
  public void preInit(final SpringProcessEngineConfiguration configuration) {
    configureJobExecutor(configuration);
    registerCustomJobHandlers(configuration);
  }

  @Autowired(required = false)
  public void setCustomJobHandlers(List<JobHandler<?>> customJobHandlers) {
    this.customJobHandlers = customJobHandlers;
  }

  protected void registerCustomJobHandlers(SpringProcessEngineConfiguration configuration) {
    configuration.setCustomJobHandlers(join(configuration.getCustomJobHandlers(), customJobHandlers));
    for (JobHandler<?> jobHandler : configuration.getCustomJobHandlers()) {
      LOG.registerCustomJobHandler(jobHandler.getType());
    }
  }

  protected void configureJobExecutor(SpringProcessEngineConfiguration configuration) {
    // note: the job executor will be activated in
    // org.operaton.bpm.spring.boot.starter.runlistener.JobExecutorRunListener
    configuration.setJobExecutorActivate(false);
    configuration.setJobExecutorDeploymentAware(operatonBpmProperties.getJobExecution().isDeploymentAware());
    configuration.setJobExecutor(jobExecutor);

  }

  public static final class JobConfiguration {

    public static final String OPERATON_TASK_EXECUTOR_QUALIFIER = "operatonTaskExecutor";

    @Bean(name = OPERATON_TASK_EXECUTOR_QUALIFIER)
    @ConditionalOnMissingBean(name = OPERATON_TASK_EXECUTOR_QUALIFIER)
    @ConditionalOnProperty(prefix = "operaton.bpm.job-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static TaskExecutor operatonTaskExecutor(OperatonBpmProperties properties) {
      int corePoolSize = properties.getJobExecution().getCorePoolSize();
      int maxPoolSize = properties.getJobExecution().getMaxPoolSize();
      int queueCapacity = properties.getJobExecution().getQueueCapacity();

      final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

      threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
      threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
      threadPoolTaskExecutor.setQueueCapacity(queueCapacity);

      Optional.ofNullable(properties.getJobExecution().getKeepAliveSeconds())
          .ifPresent(threadPoolTaskExecutor::setKeepAliveSeconds);

      LOG.configureJobExecutorPool(corePoolSize, maxPoolSize);
      return threadPoolTaskExecutor;
    }

    @Bean
    @ConditionalOnMissingBean(JobExecutor.class)
    @ConditionalOnProperty(prefix = "operaton.bpm.job-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static JobExecutor jobExecutor(@Qualifier(OPERATON_TASK_EXECUTOR_QUALIFIER) final TaskExecutor taskExecutor,
                                          OperatonBpmProperties properties) {
      final SpringJobExecutor springJobExecutor = new SpringJobExecutor();
      springJobExecutor.setTaskExecutor(taskExecutor);
      springJobExecutor.setRejectedJobsHandler(new NotifyAcquisitionRejectedJobsHandler());

      JobExecutionProperty jobExecution = properties.getJobExecution();
      Optional.ofNullable(jobExecution.getLockTimeInMillis()).ifPresent(springJobExecutor::setLockTimeInMillis);
      Optional.ofNullable(jobExecution.getMaxJobsPerAcquisition())
          .ifPresent(springJobExecutor::setMaxJobsPerAcquisition);
      Optional.ofNullable(jobExecution.getWaitTimeInMillis()).ifPresent(springJobExecutor::setWaitTimeInMillis);
      Optional.ofNullable(jobExecution.getMaxWait()).ifPresent(springJobExecutor::setMaxWait);
      Optional.ofNullable(jobExecution.getBackoffTimeInMillis()).ifPresent(springJobExecutor::setBackoffTimeInMillis);
      Optional.ofNullable(jobExecution.getMaxBackoff()).ifPresent(springJobExecutor::setMaxBackoff);
      Optional.ofNullable(jobExecution.getBackoffDecreaseThreshold())
          .ifPresent(springJobExecutor::setBackoffDecreaseThreshold);
      Optional.ofNullable(jobExecution.getWaitIncreaseFactor()).ifPresent(springJobExecutor::setWaitIncreaseFactor);

      return springJobExecutor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "operaton.bpm.job-execution", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(JobExecutor.class)
    public static JobExecutorStartingEventListener jobExecutorStartingEventListener(JobExecutor jobExecutor) {
      return new JobExecutorStartingEventListener(jobExecutor);
    }

    private JobConfiguration() {
    }
  }
}
