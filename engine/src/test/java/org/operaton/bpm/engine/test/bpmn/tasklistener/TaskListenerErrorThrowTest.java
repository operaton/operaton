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
package org.operaton.bpm.engine.test.bpmn.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.junit.Before;
import org.junit.Test;

public class TaskListenerErrorThrowTest extends AbstractTaskListenerTest {
  /*
  Testing BPMN Error Throw in Task Listener
   */

  public static final String ERROR_CODE = "208";

  @Before
  public void resetListenerCounters() {
    ThrowBPMNErrorListener.reset();
  }

  @Test
  public void testThrowErrorOnCreateAndCatchOnUserTask() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnUserTask(TaskListener.EVENTNAME_CREATE);

    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnAssignmentAndCatchOnUserTask() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnUserTask(TaskListener.EVENTNAME_ASSIGNMENT);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    firstTask.setAssignee("elmo");
    engineRule.getTaskService().saveTask(firstTask);

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnCompleteAndCatchOnUserTask() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnUserTask(TaskListener.EVENTNAME_COMPLETE);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    taskService.complete(firstTask.getId());

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnCreateAndCatchOnSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnSubprocess(TaskListener.EVENTNAME_CREATE);

    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnAssignmentAndCatchOnSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnSubprocess(TaskListener.EVENTNAME_ASSIGNMENT);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    firstTask.setAssignee("elmo");
    engineRule.getTaskService().saveTask(firstTask);

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnCompleteAndCatchOnSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnSubprocess(TaskListener.EVENTNAME_COMPLETE);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    taskService.complete(firstTask.getId());

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnCreateAndCatchOnEventSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnEventSubprocess(TaskListener.EVENTNAME_CREATE);
    System.out.println(Bpmn.convertToString(model));
    testRule.deploy(model);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnAssignmentAndCatchOnEventSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnEventSubprocess(TaskListener.EVENTNAME_ASSIGNMENT);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    firstTask.setAssignee("elmo");
    engineRule.getTaskService().saveTask(firstTask);

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnCompleteAndCatchOnEventSubprocess() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnEventSubprocess(TaskListener.EVENTNAME_COMPLETE);

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    taskService.complete(firstTask.getId());

    // then
    verifyErrorGotCaught();
  }

  @Test
  @Deployment
  public void testThrowErrorOnCreateScriptListenerAndCatchOnUserTask() {
    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    Task resultTask = taskService.createTaskQuery().singleResult();
    assertThat(resultTask).isNotNull();
    assertThat(resultTask.getName()).isEqualTo("afterCatch");
  }

  @Test
  public void testThrowErrorOnAssignmentExpressionListenerAndCatchOnUserTask() {
    // given
    processEngineConfiguration.getBeans().put("myListener", new ThrowBPMNErrorListener());
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
                                  .startEvent()
                                    .userTask("mainTask")
                                      .operatonTaskListenerExpression(TaskListener.EVENTNAME_ASSIGNMENT, "${myListener.notify(task)}")
                                      .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
                                      .boundaryEvent("throw")
                                        .error(ERROR_CODE)
                                    .userTask("afterCatch")
                                    .moveToActivity("mainTask")
                                    .userTask("afterThrow")
                                  .endEvent()
                                  .done();
    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    // when
    firstTask.setAssignee("elmo");
    engineRule.getTaskService().saveTask(firstTask);

    // then
    verifyErrorGotCaught();
  }

  @Test
  public void testThrowErrorOnDeleteAndCatchOnUserTaskShouldNotTriggerPropagation() {
    // given
    BpmnModelInstance model = createModelThrowErrorInListenerAndCatchOnUserTask(TaskListener.EVENTNAME_DELETE);

    DeploymentWithDefinitions deployment      = testRule.deploy(model);
    ProcessInstance           processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    try {
      runtimeService.deleteProcessInstance(processInstance.getId(), "invoke delete listener");
    } catch (Exception e) {
      // then
      assertThat(e.getMessage()).contains("business error");
      assertThat(ThrowBPMNErrorListener.invocations).isEqualTo(1);
      assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_DELETE)).isZero();
    }

    // cleanup
    engineRule.getRepositoryService().deleteDeployment(deployment.getId(), true, true);
  }

  @Test
  public void testThrowUncaughtErrorOnCompleteAndCatchOnUserTask() {
    // given
    processEngineConfiguration.setEnableExceptionsAfterUnhandledBpmnError(true);
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
                                  .startEvent()
                                    .userTask("mainTask")
                                      .operatonTaskListenerClass(TaskListener.EVENTNAME_COMPLETE, ThrowBPMNErrorListener.class.getName())
                                      .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
                                    .userTask("afterThrow")
                                  .endEvent()
                                  .done();

    testRule.deploy(model);
    runtimeService.startProcessInstanceByKey("process");

    Task firstTask = taskService.createTaskQuery().singleResult();
    assertThat(firstTask).isNotNull();

    try {
      // when
      taskService.complete(firstTask.getId());
    } catch (ProcessEngineException e) {
      // then
      assertThat(e.getMessage()).contains("There was an exception while invoking the TaskListener");
      assertThat(e.getMessage()).contains("Execution with id 'mainTask' throws an error event with errorCode '208' and errorMessage 'business error 208', but no error handler was defined.");
    }

    // then
    Task resultTask = taskService.createTaskQuery().singleResult();
    assertThat(resultTask).isNotNull();
    assertThat(resultTask.getName()).isEqualTo("mainTask");
    assertThat(ThrowBPMNErrorListener.invocations).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_DELETE)).isZero();

    // cleanup
    processEngineConfiguration.setEnableExceptionsAfterUnhandledBpmnError(false);
  }

  // HELPER methods

  protected void verifyErrorGotCaught() {
    Task resultTask = taskService.createTaskQuery().singleResult();
    assertThat(resultTask).isNotNull();
    assertThat(resultTask.getName()).isEqualTo("afterCatch");
    assertThat(ThrowBPMNErrorListener.invocations).isEqualTo(1);
    assertThat(RecorderTaskListener.getEventCount(TaskListener.EVENTNAME_DELETE)).isEqualTo(1);
  }

  protected BpmnModelInstance createModelThrowErrorInListenerAndCatchOnUserTask(String eventName) {
    return Bpmn.createExecutableProcess("process")
               .startEvent()
                 .userTask("mainTask")
                   .operatonTaskListenerClass(eventName, ThrowBPMNErrorListener.class.getName())
                   .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
                   .boundaryEvent("throw")
                     .error(ERROR_CODE)
                 .userTask("afterCatch")
                 .moveToActivity("mainTask")
                 .userTask("afterThrow")
               .endEvent()
               .done();
  }

  protected BpmnModelInstance createModelThrowErrorInListenerAndCatchOnSubprocess(String eventName) {
    return Bpmn.createExecutableProcess("process")
               .startEvent()
                 .subProcess("sub")
                 .embeddedSubProcess()
                   .startEvent("inSub")
                     .userTask("mainTask")
                       .operatonTaskListenerClass(eventName, ThrowBPMNErrorListener.class.getName())
                       .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
                     .userTask("afterThrow")
                   .endEvent()
                 .moveToActivity("sub")
                   .boundaryEvent("throw")
                     .error(ERROR_CODE)
                 .userTask("afterCatch")
               .endEvent()
               .done();
  }

  protected BpmnModelInstance createModelThrowErrorInListenerAndCatchOnEventSubprocess(String eventName) {
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess("process");
    BpmnModelInstance model = processBuilder
        .startEvent()
          .userTask("mainTask")
            .operatonTaskListenerClass(eventName, ThrowBPMNErrorListener.class.getName())
            .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, RecorderTaskListener.class.getName())
          .userTask("afterThrow")
          .endEvent()
        .done();
    processBuilder.eventSubProcess()
                  .startEvent("errorEvent").error(ERROR_CODE)
                    .userTask("afterCatch")
                  .endEvent();
    return model;
  }

  public static class ThrowBPMNErrorListener implements TaskListener {
    public static int invocations = 0;

    @Override
    public void notify(DelegateTask delegateTask) {
      invocations++;
      throw new BpmnError(ERROR_CODE, "business error 208");
    }

    public static void reset() {
      invocations = 0;
    }
  }
}
