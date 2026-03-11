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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class JobDefinitionFunctionalTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ManagementService managementService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  static final BpmnModelInstance SIMPLE_ASYNC_PROCESS = Bpmn.createExecutableProcess("simpleAsyncProcess")
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
        .operatonAsyncBefore()
      .endEvent()
      .done();

  @Test
  void testCreateJobInstanceSuspended() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    // given suspended job definition:
    managementService.suspendJobDefinitionByProcessDefinitionKey("simpleAsyncProcess");

    // if I start a new instance
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // then the new job instance is created as suspended:
    assertThat(managementService.createJobQuery().suspended().singleResult()).isNotNull();
    assertThat(managementService.createJobQuery().active().singleResult()).isNull();
  }

  @Test
  void testCreateJobInstanceActive() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    // given that the job definition is not suspended:

    // if I start a new instance
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // then the new job instance is created as active:
    assertThat(managementService.createJobQuery().suspended().singleResult()).isNull();
    assertThat(managementService.createJobQuery().active().singleResult()).isNotNull();
  }

  @Test
  void testJobExecutorOnlyAcquiresActiveJobs() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    // given suspended job definition:
    managementService.suspendJobDefinitionByProcessDefinitionKey("simpleAsyncProcess");

    // if I start a new instance
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // then the new job executor will not acquire the job:
    AcquiredJobs acquiredJobs = acquireJobs();
    assertThat(acquiredJobs.size()).isZero();

    // -------------------------

    // given a active job definition:
    managementService.activateJobDefinitionByProcessDefinitionKey("simpleAsyncProcess", true);

    // then the new job executor will not acquire the job:
    acquiredJobs = acquireJobs();
    assertThat(acquiredJobs.size()).isEqualTo(1);
  }

  @Test
  void testExclusiveJobs() {
    testRule.deploy(Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .serviceTask("task1")
          .operatonExpression("${true}")
          .operatonAsyncBefore()
        .serviceTask("task2")
          .operatonExpression("${true}")
          .operatonAsyncBefore()
        .endEvent()
        .done());

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .activityIdIn("task2")
      .singleResult();

    // given that the second task is suspended
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // if I start a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    testRule.waitForJobExecutorToProcessAllJobs(10000L);

    // then the second task is not executed
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    // there is a suspended job instance
    Job job = managementService.createJobQuery()
      .singleResult();
    assertThat(jobDefinition.getId()).isEqualTo(job.getJobDefinitionId());
    assertThat(job.isSuspended()).isTrue();

    // if I unsuspend the job definition, the job is executed:
    managementService.activateJobDefinitionById(jobDefinition.getId(), true);

    testRule.waitForJobExecutorToProcessAllJobs(10000);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  protected AcquiredJobs acquireJobs() {
    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();

    return processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(new AcquireJobsCmd(jobExecutor));
  }

}
