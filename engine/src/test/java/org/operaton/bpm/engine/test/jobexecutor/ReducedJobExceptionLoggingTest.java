/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class ReducedJobExceptionLoggingTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    // XXX disabled caching because tests got flaky. see https://github.com/operaton/operaton/issues/671
    .cacheForConfigurationResource(false)
    .build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  protected static ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension().watch("org.operaton.bpm.engine.jobexecutor", Level.DEBUG);

  private RuntimeService runtimeService;
  private ManagementService managementService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setEnableReducedJobExceptionLogging(false);
    List<Job> jobs = managementService.createJobQuery().processDefinitionKey("failingProcess").list();
    for (Job job : jobs) {
      managementService.deleteJob(job.getId());
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  void shouldLogAllFailingJobExceptions() {
    // given
    processEngineConfiguration.setEnableReducedJobExceptionLogging(false);

    // when
    runtimeService.startProcessInstanceByKey("failingProcess");
    processEngineConfiguration.getJobExecutor().start();
    testRule.waitForJobExecutorToProcessAllJobs();
    processEngineConfiguration.getJobExecutor().shutdown();

    List<ILoggingEvent> filteredLogList = loggingRule.getFilteredLog("Exception while executing job");

    // then
    assertThat(filteredLogList).hasSize(3);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  void shouldLogOnlyOneFailingJobException() {
    // given
    processEngineConfiguration.setEnableReducedJobExceptionLogging(true);

    // when
    runtimeService.startProcessInstanceByKey("failingProcess");
    processEngineConfiguration.getJobExecutor().start();
    testRule.waitForJobExecutorToProcessAllJobs();
    processEngineConfiguration.getJobExecutor().shutdown();

    List<ILoggingEvent> filteredLogList = loggingRule.getFilteredLog("Exception while executing job");

    // then
    assertThat(filteredLogList).hasSize(1);
  }
}
