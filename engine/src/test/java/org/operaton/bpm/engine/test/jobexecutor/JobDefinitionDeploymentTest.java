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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These testcases verify that job definitions are created upon deployment of the process definition.
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class JobDefinitionDeploymentTest {

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;

  @Deployment
  @Test
  void testTimerStartEvent() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerStartEventJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theStart");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DATE: 2036-11-14T11:12:22");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

    // there exists a job with the correct job definition id:
    Job timerStartJob = managementService.createJobQuery().singleResult();
    assertThat(timerStartJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
  }

  @Deployment
  @Test
  void testTimerBoundaryEvent() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerExecuteNestedActivityJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theBoundaryEvent");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DATE: 2036-11-14T11:12:22");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testMultipleTimerBoundaryEvents() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess");

    // then assert
    assertThat(jobDefinitionQuery.count()).isEqualTo(2);

    JobDefinition jobDefinition = jobDefinitionQuery.activityIdIn("theBoundaryEvent1").singleResult();
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerExecuteNestedActivityJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theBoundaryEvent1");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DATE: 2036-11-14T11:12:22");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

    jobDefinition = jobDefinitionQuery.activityIdIn("theBoundaryEvent2").singleResult();
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerExecuteNestedActivityJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theBoundaryEvent2");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DURATION: PT5M");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testEventBasedGateway() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess");

    // then assert
    assertThat(jobDefinitionQuery.count()).isEqualTo(2);

    JobDefinition jobDefinition = jobDefinitionQuery.activityIdIn("timer1").singleResult();
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerCatchIntermediateEventJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("timer1");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DURATION: PT5M");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

    jobDefinition = jobDefinitionQuery.activityIdIn("timer2").singleResult();
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerCatchIntermediateEventJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("timer2");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DURATION: PT10M");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testTimerIntermediateEvent() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(TimerCatchIntermediateEventJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("timer");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo("DURATION: PT5M");
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testAsyncContinuation() {

    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theService");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_BEFORE);
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testAsyncContinuationOfMultiInstance() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theService" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX);
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_AFTER);
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment
  @Test
  void testAsyncContinuationOfActivityWrappedInMultiInstance() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().processDefinitionKey("testProcess").singleResult();

    // then assert
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getJobType()).isEqualTo(AsyncContinuationJobHandler.TYPE);
    assertThat(jobDefinition.getActivityId()).isEqualTo("theService");
    assertThat(jobDefinition.getJobConfiguration()).isEqualTo(MessageJobDeclaration.ASYNC_AFTER);
    assertThat(jobDefinition.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testAsyncContinuation.bpmn20.xml",
    "org/operaton/bpm/engine/test/jobexecutor/JobDefinitionDeploymentTest.testMultipleProcessesWithinDeployment.bpmn20.xml"})
  @Test
  void testMultipleProcessDeployment() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    List<JobDefinition> jobDefinitions = query.list();
    assertThat(jobDefinitions).hasSize(3);

    assertThat(query.processDefinitionKey("testProcess").list()).hasSize(1);
    assertThat(query.processDefinitionKey("anotherTestProcess").list()).hasSize(2);
  }

}
