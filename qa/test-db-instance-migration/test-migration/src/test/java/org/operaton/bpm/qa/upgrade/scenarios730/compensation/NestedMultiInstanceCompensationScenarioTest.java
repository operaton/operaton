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
package org.operaton.bpm.qa.upgrade.scenarios730.compensation;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import org.operaton.bpm.qa.upgrade.UpgradeTestRule;

/**
 * @author Thorben Lindhauer
 */
@ScenarioUnderTest("NestedMultiInstanceCompensationScenario")
@Origin("7.24.0")
public class NestedMultiInstanceCompensationScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("init.throwInner.1")
  public void testInitThrowInnerCompletion() {
    // given
    List<Task> undoTasks = rule.taskQuery().list();

    // when
    for (Task undoTask : undoTasks) {
      rule.getTaskService().complete(undoTask.getId());
    }

    // then the process has successfully completed
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.throwInner.2")
  public void testInitThrowInnerDeletion() {
    // given
    ProcessInstance processInstance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(processInstance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.throwOuter.1")
  public void testInitThrowOuterCompletion() {
    // given
    List<Task> undoTasks = rule.taskQuery().list();

    // when
    for (Task undoTask : undoTasks) {
      rule.getTaskService().complete(undoTask.getId());
    }

    // then the process has successfully completed
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.throwOuter.2")
  public void testInitThrowOuterDeletion() {
    // given
    ProcessInstance processInstance = rule.processInstance();

    // when
    rule.getRuntimeService().deleteProcessInstance(processInstance.getId(), null);

    // then
    rule.assertScenarioEnded();
  }

}
