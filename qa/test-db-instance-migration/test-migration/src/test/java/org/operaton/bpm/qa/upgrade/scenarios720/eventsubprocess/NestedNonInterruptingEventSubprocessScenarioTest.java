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
package org.operaton.bpm.qa.upgrade.scenarios720.eventsubprocess;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import org.operaton.bpm.qa.upgrade.UpgradeTestRule;
import org.operaton.bpm.qa.upgrade.util.ThrowBpmnErrorDelegate.ThrowBpmnErrorDelegateException;
import org.operaton.bpm.qa.upgrade.util.ThrowBpmnErrorDelegate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ScenarioUnderTest("NestedNonInterruptingEventSubprocessScenario")
@Origin("7.24.0")
public class NestedNonInterruptingEventSubprocessScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("init.1")
  public void testInitCompletionCase1() {
    // given
    Task innerTask = rule.taskQuery().taskDefinitionKey("innerTask").singleResult();
    Task eventSubprocessTask = rule.taskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    // when
    rule.getTaskService().complete(innerTask.getId());
    rule.getTaskService().complete(eventSubprocessTask.getId());

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.2")
  public void testInitCompletionCase2() {
    // given
    Task innerTask = rule.taskQuery().taskDefinitionKey("innerTask").singleResult();
    Task eventSubprocessTask = rule.taskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    // when
    rule.getTaskService().complete(eventSubprocessTask.getId());
    rule.getTaskService().complete(innerTask.getId());

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.4")
  public void testInitDeletion() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(instance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.5")
  public void testInitThrowError() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task eventSubprocessTask = rule.taskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.ERROR_INDICATOR_VARIABLE, true);
    rule.getTaskService().complete(eventSubprocessTask.getId());

    // then
    Task escalatedTask = rule.taskQuery().singleResult();
    Assert.assertEquals("escalatedTask", escalatedTask.getTaskDefinitionKey());
    Assert.assertNotNull(escalatedTask);

    rule.getTaskService().complete(escalatedTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.6")
  public void testInitThrowUnhandledException() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task eventSubprocessTask = rule.taskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_INDICATOR_VARIABLE, true);
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_MESSAGE_VARIABLE, "unhandledException");

    // when/then
    assertThatThrownBy(() -> rule.getTaskService().complete(eventSubprocessTask.getId()))
      .isInstanceOf(ThrowBpmnErrorDelegateException.class)
      .hasMessage("unhandledException");
  }

  @Test
  @ScenarioUnderTest("init.innerTask.1")
  public void testInitTask1Completion() {
    // given
    Task eventSubprocessTask = rule.taskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();

    // when
    rule.getTaskService().complete(eventSubprocessTask.getId());

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.innerTask.3")
  public void testInitTask1Deletion() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(instance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.innerTask.4")
  public void testInitTask1ThrowError() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task eventSubprocessTask = rule.taskQuery().singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.ERROR_INDICATOR_VARIABLE, true);
    rule.getTaskService().complete(eventSubprocessTask.getId());

    // then
    Task escalatedTask = rule.taskQuery().singleResult();
    Assert.assertEquals("escalatedTask", escalatedTask.getTaskDefinitionKey());
    Assert.assertNotNull(escalatedTask);

    rule.getTaskService().complete(escalatedTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.innerTask.5")
  public void testInitTask1ThrowUnhandledException() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task eventSubprocessTask = rule.taskQuery().singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_INDICATOR_VARIABLE, true);
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_MESSAGE_VARIABLE, "unhandledException");

    // when/then
    assertThatThrownBy(() -> rule.getTaskService().complete(eventSubprocessTask.getId()))
      .isInstanceOf(ThrowBpmnErrorDelegateException.class)
      .hasMessage("unhandledException");
  }

}
