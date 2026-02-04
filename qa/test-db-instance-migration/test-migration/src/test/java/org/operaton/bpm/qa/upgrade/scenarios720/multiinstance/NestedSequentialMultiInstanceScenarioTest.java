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
package org.operaton.bpm.qa.upgrade.scenarios720.multiinstance;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;

import static org.operaton.bpm.qa.upgrade.util.ActivityInstanceAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.qa.upgrade.util.ActivityInstanceAssert.describeActivityInstanceTree;

@ScenarioUnderTest("NestedSequentialMultiInstanceSubprocessScenario")
@Origin("7.2.0")
public class NestedSequentialMultiInstanceScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("init.1")
  public void testInitCompletionCase1() {
    // given
    Task innerMiSubProcessTask = rule.taskQuery().taskDefinitionKey("innerSubProcessTask").singleResult();

    // when the first instance innerSubProcessTask and the other eight instances are completed
    rule.getTaskService().complete(innerMiSubProcessTask.getId());

    for (int i = 0; i < 8; i++) {
      innerMiSubProcessTask = rule.taskQuery().taskDefinitionKey("innerSubProcessTask").singleResult();
      Assert.assertNotNull(innerMiSubProcessTask);
      rule.getTaskService().complete(innerMiSubProcessTask.getId());
    }

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.2")
  public void testInitActivityInstanceTree() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(instance.getId());

    // then
    Assert.assertNotNull(activityInstance);
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(instance.getProcessDefinitionId())
        // the subprocess itself misses because it was no scope in 7.2
        .beginMiBody("outerMiSubProcess")
          // the subprocess itself misses because it was no scope in 7.2
          .beginMiBody("innerMiSubProcess")
            .activity("innerSubProcessTask")
      .done());
  }

  @Test
  @ScenarioUnderTest("init.3")
  public void testInitDeletion() {
    // given
    ProcessInstance instance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(instance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.4")
  public void testInitThrowError() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task innerMiSubProcessTask = rule.taskQuery().taskDefinitionKey("innerSubProcessTask").singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.ERROR_INDICATOR_VARIABLE, true);
    rule.getTaskService().complete(innerMiSubProcessTask.getId());

    // then
    Task escalatedTask = rule.taskQuery().singleResult();
    Assert.assertEquals("escalatedTask", escalatedTask.getTaskDefinitionKey());
    Assert.assertNotNull(escalatedTask);

    rule.getTaskService().complete(escalatedTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.5")
  public void testInitThrowUnhandledException() {
    // given
    ProcessInstance instance = rule.processInstance();
    Task innerMiSubProcessTask = rule.taskQuery().taskDefinitionKey("innerSubProcessTask").singleResult();

    // when
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_INDICATOR_VARIABLE, true);
    rule.getRuntimeService().setVariable(instance.getId(), ThrowBpmnErrorDelegate.EXCEPTION_MESSAGE_VARIABLE, "unhandledException");

    // when/then
    assertThatThrownBy(() -> rule.getTaskService().complete(innerMiSubProcessTask.getId()))
      .isInstanceOf(ThrowBpmnErrorDelegateException.class)
      .hasMessage("unhandledException");
  }

}
