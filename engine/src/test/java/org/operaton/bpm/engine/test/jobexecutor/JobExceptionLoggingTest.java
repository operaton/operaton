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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class JobExceptionLoggingTest {

  private static final String JOBEXECUTOR_LOGGER = "org.operaton.bpm.engine.jobexecutor";
  private static final String CONTEXT_LOGGER = "org.operaton.bpm.engine.context";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension().watch(CONTEXT_LOGGER, JOBEXECUTOR_LOGGER).level(Level.DEBUG);

  RuntimeService runtimeService;
  ManagementService managementService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void init() {
    processEngineConfiguration.setDefaultNumberOfRetries(1);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setDefaultNumberOfRetries(3);
    processEngineConfiguration.setEnableCmdExceptionLogging(true);
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.deleteJob(job.getId());
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/delegateThrowsException.bpmn20.xml")
  void shouldLogFailingJobOnlyOnceReducedLogging() {
    // given a job that always throws an Exception
    processEngineConfiguration.setEnableCmdExceptionLogging(false);
    runtimeService.startProcessInstanceByKey("testProcess");

    // when executing the job and wait
    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
    jobExecutor.start();
    testRule.waitForJobExecutorToProcessAllJobs();
    jobExecutor.shutdown();

    List<ILoggingEvent> jobLog = loggingRule.getFilteredLog(JOBEXECUTOR_LOGGER, "Exception while executing job");
    List<ILoggingEvent> ctxLog = loggingRule.getFilteredLog(CONTEXT_LOGGER, "Exception while closing command context");

    // then
    assertThat(jobLog).hasSize(1);
    assertThat(ctxLog).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/delegateThrowsException.bpmn20.xml")
  void shouldLogFailingJobTwiceDefaultLogging() {
    // given a job that always throws an Exception
    processEngineConfiguration.setEnableCmdExceptionLogging(true);
    runtimeService.startProcessInstanceByKey("testProcess");

    // when executing the job and wait
    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
    jobExecutor.start();
    testRule.waitForJobExecutorToProcessAllJobs();
    jobExecutor.shutdown();

    List<ILoggingEvent> jobLog = loggingRule.getFilteredLog(JOBEXECUTOR_LOGGER, "Exception while executing job");
    List<ILoggingEvent> ctxLog = loggingRule.getFilteredLog(CONTEXT_LOGGER, "Exception while closing command context");

    // then
    assertThat(jobLog).hasSize(1);
    assertThat(ctxLog).hasSize(1);
  }

  @Test
  void shouldNotLogExceptionWhenApiCallReducedLogging() {
    // given
    processEngineConfiguration.setEnableCmdExceptionLogging(false);
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("failingDelegate")
        .startEvent()
        .serviceTask()
          .operatonClass("org.operaton.bpm.engine.test.jobexecutor.FailingDelegate")
          .operatonAsyncBefore()
        .done();
    testRule.deploy(modelInstance);

    runtimeService.startProcessInstanceByKey("failingDelegate");
    Job job = managementService.createJobQuery().singleResult();

    // when
    RuntimeException expectedException = null;
    try {
      managementService.executeJob(job.getId());
    } catch (RuntimeException e) {
      expectedException = e;
    }
    List<ILoggingEvent> jobLog = loggingRule.getFilteredLog(JOBEXECUTOR_LOGGER, "Exception while executing job");
    List<ILoggingEvent> ctxLog = loggingRule.getFilteredLog(CONTEXT_LOGGER, "Exception while closing command context");

    // then
    // make sure the exceptions is thrown...
    assertThat(expectedException).isNotNull();
    assertThat(expectedException.getMessage()).contains("Expected Exception");
    // ...but not logged
    assertThat(jobLog).isEmpty();
    assertThat(ctxLog).isEmpty();
  }

  @Test
  void shouldNotLogExceptionWhenUserApiCallReducedLogging() {
    // given
    processEngineConfiguration.setEnableCmdExceptionLogging(false);
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("failingDelegate")
        .startEvent()
        .serviceTask()
          .operatonClass("org.operaton.bpm.engine.test.jobexecutor.FailingDelegate")
        .done();
    testRule.deploy(modelInstance);

    // when
    RuntimeException expectedException = null;
    try {
      runtimeService.startProcessInstanceByKey("failingDelegate");
    } catch (RuntimeException e) {
      expectedException = e;
    }
    List<ILoggingEvent> jobLog = loggingRule.getFilteredLog(JOBEXECUTOR_LOGGER, "Exception while executing job");
    List<ILoggingEvent> ctxLog = loggingRule.getFilteredLog(CONTEXT_LOGGER, "Exception while closing command context");

    // then
    // make sure the exceptions is thrown...
    assertThat(expectedException).isNotNull();
    assertThat(expectedException.getMessage()).contains("Expected Exception");
    // ...but not logged
    assertThat(jobLog).isEmpty();
    assertThat(ctxLog).isEmpty();
  }
}
