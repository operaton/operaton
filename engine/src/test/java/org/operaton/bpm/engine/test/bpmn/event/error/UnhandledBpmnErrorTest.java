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
package org.operaton.bpm.engine.test.bpmn.event.error;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class UnhandledBpmnErrorTest {

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setEnableExceptionsAfterUnhandledBpmnError(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;

  @Before
  public void setUp() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
  }

  @Test
  public void testThrownInJavaDelegate() {

    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask().operatonClass(ThrowBpmnErrorDelegate.class)
        .endEvent().done();
    testRule.deploy(instance);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no error handler");
  }

  @Test
  @Deployment
  public void testUncaughtErrorSimpleProcess() {

    // given simple process definition

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no error handler");
  }

  @Test
  @Deployment
  public void testUnhandledErrorInEmbeddedSubprocess() {
    // given
    runtimeService.startProcessInstanceByKey("boundaryErrorOnEmbeddedSubprocess");

    // assume
    // After process start, usertask in subprocess should exist
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("subprocessTask", task.getName());
    String taskId = task.getId();

    // when/then
    // After task completion, error end event is reached which is never caught in the process
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no error handler");
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/UnhandledBpmnErrorTest.testUncaughtErrorOnCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/UnhandledBpmnErrorTest.subprocess.bpmn20.xml" })
  public void testUncaughtErrorOnCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("uncaughtErrorOnCallActivity");

    // assume
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("Task in subprocess", task.getName());
    String taskId = task.getId();

    // when/then
    // Completing the task will reach the end error event,
    // which is never caught in the process
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no error handler");
  }

  @Test
  @Deployment
  public void testUncaughtErrorOnEventSubprocess() {

    // given
    runtimeService.startProcessInstanceByKey("process").getId();

    // assume
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("subprocessTask", task.getName());
    String taskId = task.getId();

    // when/then
    // After task completion, error end event is reached which is never caught in the process
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no error handler");
  }
}