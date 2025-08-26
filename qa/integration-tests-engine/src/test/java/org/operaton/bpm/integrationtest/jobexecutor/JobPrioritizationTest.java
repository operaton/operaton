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
package org.operaton.bpm.integrationtest.jobexecutor;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.TransitionInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.jobexecutor.beans.PriorityBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class JobPrioritizationTest extends AbstractFoxPlatformIntegrationTest {

  protected ProcessInstance processInstance;

  @Deployment
  public static WebArchive createDeployment() {
    return initWebArchiveDeployment()
      .addClass(PriorityBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationTest.priorityProcess.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationTest.serviceTask.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationTest.userTask.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationTest.intermediateMessage.bpmn20.xml");
  }

  @AfterEach
  void tearDown() {
    if (processInstance != null) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "");
    }
  }

  @Test
  void testPriorityOnProcessElement() {
    // given
    processInstance = runtimeService.startProcessInstanceByKey("priorityProcess");

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    // then
    Assertions.assertEquals(PriorityBean.PRIORITY, job.getPriority());

  }

  @Test
  void testPriorityOnProcessStart() {

    // given
    processInstance = runtimeService.startProcessInstanceByKey("serviceTaskProcess");

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    // then
    Assertions.assertEquals(PriorityBean.PRIORITY, job.getPriority());
  }

  @Test
  void testPriorityOnModification() {

    // given
    processInstance = runtimeService.startProcessInstanceByKey("serviceTaskProcess");

    TransitionInstance transitionInstance = runtimeService.getActivityInstance(processInstance.getId())
        .getTransitionInstances("serviceTask")[0];

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("serviceTask")
      .cancelTransitionInstance(transitionInstance.getId())
      .execute();

    // then
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    Assertions.assertEquals(PriorityBean.PRIORITY, job.getPriority());
  }

  @Test
  void testPriorityOnInstantiationAtActivity() {

    // when
    processInstance = runtimeService.createProcessInstanceByKey("serviceTaskProcess")
      .startBeforeActivity("serviceTask")
      .execute();

    // then
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    Assertions.assertEquals(PriorityBean.PRIORITY, job.getPriority());
  }

  @Test
  void testPriorityOnAsyncAfterUserTask() {
    // given
    processInstance = runtimeService.startProcessInstanceByKey("userTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();

    // when
    taskService.complete(task.getId());

    // then
    Job asyncAfterJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    Assertions.assertEquals(PriorityBean.PRIORITY, asyncAfterJob.getPriority());
  }

  @Test
  void testPriorityOnAsyncAfterIntermediateCatchEvent() {
    // given
    processInstance = runtimeService.startProcessInstanceByKey("intermediateMessageProcess");

    // when
    runtimeService.correlateMessage("Message");

    // then
    Job asyncAfterJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    Assertions.assertEquals(PriorityBean.PRIORITY, asyncAfterJob.getPriority());
  }

}
