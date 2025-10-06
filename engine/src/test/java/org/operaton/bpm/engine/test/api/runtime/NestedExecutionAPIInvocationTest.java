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
package org.operaton.bpm.engine.test.api.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Thorben Lindhauer
 *
 */
public class NestedExecutionAPIInvocationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule1 = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineExtension engineRule2 = ProcessEngineExtension.builder().configurationResource("operaton.cfg.prefix_extended.xml").build();

  public static final String PROCESS_KEY_1 = "process";

  public static final String PROCESS_KEY_2 = "multiEngineProcess";

  public static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";

  public static final BpmnModelInstance PROCESS_MODEL = Bpmn.createExecutableProcess(PROCESS_KEY_1)
      .startEvent()
      .userTask("waitState")
      .serviceTask("startProcess")
        .operatonClass(NestedProcessStartDelegate.class.getName())
      .endEvent()
      .done();

  public static final BpmnModelInstance PROCESS_MODEL_2 = Bpmn.createExecutableProcess(PROCESS_KEY_2)
    .startEvent()
    .userTask("waitState")
    .serviceTask("startProcess")
      .operatonClass(StartProcessOnAnotherEngineDelegate.class.getName())
    .endEvent()
    .done();

  public static final BpmnModelInstance ONE_TASK_PROCESS_MODEL = Bpmn.createExecutableProcess(ONE_TASK_PROCESS_KEY)
  .startEvent()
    .userTask("waitState")
  .endEvent()
  .done();

  @BeforeEach
  void init() {

    StartProcessOnAnotherEngineDelegate.engine = engineRule2.getProcessEngine();
    NestedProcessStartDelegate.engine = engineRule1.getProcessEngine();

    // given
    Deployment deployment1 = engineRule1.getRepositoryService()
      .createDeployment()
      .addModelInstance("foo.bpmn", PROCESS_MODEL)
      .deploy();

    Deployment deployment2 = engineRule1.getRepositoryService()
      .createDeployment()
      .addModelInstance("boo.bpmn", PROCESS_MODEL_2)
      .deploy();

    engineRule1.manageDeployment(deployment1);
    engineRule1.manageDeployment(deployment2);

    Deployment deployment3 = engineRule2.getProcessEngine().getRepositoryService()
      .createDeployment()
      .addModelInstance("joo.bpmn", ONE_TASK_PROCESS_MODEL)
      .deploy();

    engineRule2.manageDeployment(deployment3);
  }

  @AfterEach
  void clearEngineReference() {
    StartProcessOnAnotherEngineDelegate.engine = null;
    NestedProcessStartDelegate.engine = null;
  }

  @Test
  void testWaitStateIsReachedOnNestedInstantiation() {

    engineRule1.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY_1);
    var taskService = engineRule1.getTaskService();
    String taskId = taskService
      .createTaskQuery()
      .singleResult()
      .getId();

    // when
    assertThatCode(() -> taskService.complete(taskId)).doesNotThrowAnyException();
  }

  @Test
  void testWaitStateIsReachedOnMultiEngine() {

    engineRule1.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY_2);
    var taskService = engineRule1.getTaskService();
    String taskId = taskService
      .createTaskQuery()
      .singleResult()
      .getId();

    // when
    assertThatCode(() -> taskService.complete(taskId)).doesNotThrowAnyException();
  }

  public static class StartProcessOnAnotherEngineDelegate implements JavaDelegate {

    public static ProcessEngine engine;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

      RuntimeService runtimeService = engine.getRuntimeService();

      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

      // then the wait state is reached immediately after instantiation
      ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
      ActivityInstance[] activityInstances = activityInstance.getActivityInstances("waitState");
      assertThat(activityInstances).hasSize(1);
    }
  }

  public static class NestedProcessStartDelegate implements JavaDelegate {

    public static ProcessEngine engine;
    @Override
    public void execute(DelegateExecution execution) throws Exception {

      RuntimeService runtimeService = engine.getRuntimeService();

      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

      // then the wait state is reached immediately after instantiation
      ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
      ActivityInstance[] activityInstances = activityInstance.getActivityInstances("waitState");
      assertThat(activityInstances).hasSize(1);
    }
  }
}
