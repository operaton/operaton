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
package org.operaton.bpm.engine.impl.jobexecutor.setInitialRetries;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.jobexecutor.FailingDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobDeclarationRetriesTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ManagementService managementService;
  RuntimeService runtimeService;

  @Test
  void testRetryTimeCycleWithZeroRetriesAndFailure() {
    // given
    
    String retryInterval = "R0/PT5M";
    String processDefinitionName = "testRetryTimeCycleWithZeroRetriesAndFailure";
    BpmnModelInstance bpmnModelInstance = getBpmnModelInstance(processDefinitionName, retryInterval);
    testRule.deploy(bpmnModelInstance);

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey(processDefinitionName).getId();
    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    String jobId = job.getId();

    // then
    assertThat(job.getRetries()).isEqualTo(1);

    // when
    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(Exception.class);

    job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();

    // then
    assertThat(job.getRetries()).isZero();
  }

  @Test
  void testRetryTimeCycleWithFailure() {
    // given
    
    String retryInterval = "R5/PT5M";
    String processDefinitionName = "testRetryTimeCycleWithFailure";
    BpmnModelInstance bpmnModelInstance = getBpmnModelInstance(processDefinitionName, retryInterval);
    testRule.deploy(bpmnModelInstance);

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey(processDefinitionName).getId();
    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    String jobId = job.getId();
    assertThat(job.getRetries()).isEqualTo(5);

    // when
    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(Exception.class);
    job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();

    // then
    assertThat(job.getRetries()).isEqualTo(4);
  }

  @Test
  void testRetryIntervalsWithFailure() {
    // given
    
    String retryInterval = "PT10M,PT17M,PT20M";
    String processDefinitionName = "testRetryIntervalsWithFailure";
    BpmnModelInstance bpmnModelInstance = getBpmnModelInstance(processDefinitionName, retryInterval);
    testRule.deploy(bpmnModelInstance);

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey(processDefinitionName).getId();
    Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    String jobId = job.getId();

    // then
    assertThat(job.getRetries()).isEqualTo(4);

    // when
    assertThatThrownBy(() -> managementService.executeJob(jobId))
      .isInstanceOf(Exception.class);

    // then
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(3);
  }

  private static BpmnModelInstance getBpmnModelInstance(String processDefinitionName, String retryStrategy) {
    return Bpmn.createExecutableProcess(processDefinitionName)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask()
        .operatonAsyncBefore()
        .operatonFailedJobRetryTimeCycle(retryStrategy)
        .operatonClass(FailingDelegate.class.getName())
        .endEvent()
        .done();
  }
}