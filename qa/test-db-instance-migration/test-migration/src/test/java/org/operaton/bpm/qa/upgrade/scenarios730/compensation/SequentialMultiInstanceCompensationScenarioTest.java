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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import org.operaton.bpm.qa.upgrade.UpgradeTestRule;

/**
 * @author Thorben Lindhauer
 *
 */
@ScenarioUnderTest("SequentialMultiInstanceCompensationScenario")
@Origin("7.24.0")
public class SequentialMultiInstanceCompensationScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("singleActivityHandler.multiInstancePartial.1")
  public void testSingleActivityHandlerMultiInstancePartialCompletion() {
    // given the last multi instance task
    Task lastMiTask = rule.taskQuery().singleResult();

    // when completing it
    rule.getTaskService().complete(lastMiTask.getId());

    // then it is possible to throw compensation, compensate the three instances,
    // and finish the process successfully
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(beforeCompensateTask);
    rule.getTaskService().complete(beforeCompensateTask.getId());

    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("singleActivityHandler.multiInstancePartial.2")
  public void testSingleActivityHandlerMultiInstancePartialDeletion() {
    // when throwing compensation
    Task lastMiTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(lastMiTask.getId());

    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("singleActivityHandler.beforeCompensate.1")
  public void testSingleActivityHandlerBeforeCompensateCompletion() {
    // given
    Task beforeCompensateTask = rule.taskQuery().singleResult();

    // when throwing compensation
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to compensate the three instances,
    // and finish the process successfully
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("singleActivityHandler.beforeCompensate.2")
  public void testSingleActivityHandlerBeforeCompensateDeletion() {
    // when throwing compensation
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("singleActivityHandler.beforeCompensate.throwCompensate.1")
  public void testSingleActivityHandlerThrowCompensateCompletion() {
    // given active compensation
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    // when completing the compensation handlers
    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    // then it is possible to complete the process successfully
    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);

    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("singleActivityHandler.beforeCompensate.throwCompensate.2")
  public void testSingleActivityHandlerThrowCompensateDeletion() {
    // it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.multiInstancePartial.1")
  public void testDefaultHandlerMultiInstancePartialCompletion() {
    // given the last multi instance task
    Task lastMiTask = rule.taskQuery().singleResult();

    // when completing it
    rule.getTaskService().complete(lastMiTask.getId());

    // then it is possible to throw compensation, compensate the three instances,
    // and finish the process successfully
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(beforeCompensateTask);
    rule.getTaskService().complete(beforeCompensateTask.getId());

    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.multiInstancePartial.2")
  public void testDefaultHandlerMultiInstancePartialDeletion() {
    // when throwing compensation
    Task lastMiTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(lastMiTask.getId());

    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.beforeCompensate.1")
  public void testDefaultHandlerBeforeCompensateCompletion() {
    // given
    Task beforeCompensateTask = rule.taskQuery().singleResult();

    // when throwing compensation
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to compensate the three instances,
    // and finish the process successfully
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.beforeCompensate.2")
  public void testDefaultHandlerBeforeCompensateDeletion() {
    // when throwing compensation
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.beforeCompensate.throwCompensate.1")
  public void testDefaultHandlerThrowCompensateCompletion() {
    // given active compensation
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    // when completing the compensation handlers
    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    // then it is possible to complete the process successfully
    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);

    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("defaultHandler.beforeCompensate.throwCompensate.2")
  public void testDefaultHandlerThrowCompensateDeletion() {
    // it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.multiInstancePartial.1")
  public void testSubProcessHandlerMultiInstancePartialCompletion() {
    // given the last multi instance task
    Task lastMiTask = rule.taskQuery().singleResult();

    // when completing it
    rule.getTaskService().complete(lastMiTask.getId());

    // then it is possible to throw compensation, compensate the three instances,
    // and finish the process successfully
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(beforeCompensateTask);
    rule.getTaskService().complete(beforeCompensateTask.getId());

    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.multiInstancePartial.2")
  public void testSubProcessHandlerMultiInstancePartialDeletion() {
    // when throwing compensation
    Task lastMiTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(lastMiTask.getId());

    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.beforeCompensate.1")
  public void testSubProcessHandlerBeforeCompensateCompletion() {
    // given
    Task beforeCompensateTask = rule.taskQuery().singleResult();

    // when throwing compensation
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to compensate the three instances,
    // and finish the process successfully
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);
    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.beforeCompensate.2")
  public void testSubProcessHandlerBeforeCompensateDeletion() {
    // when throwing compensation
    Task beforeCompensateTask = rule.taskQuery().singleResult();
    rule.getTaskService().complete(beforeCompensateTask.getId());

    // then it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.beforeCompensate.throwCompensate.1")
  public void testSubProcessHandlerThrowCompensateCompletion() {
    // given active compensation
    List<Task> miCompensationTasks = rule.taskQuery().list();
    Assert.assertEquals(3, miCompensationTasks.size());

    // when completing the compensation handlers
    for (int i = 0; i < miCompensationTasks.size(); i++) {
      Assert.assertEquals(3 - i, rule.taskQuery().count());

      Task compensationTask = miCompensationTasks.get(i);
      Assert.assertEquals("undoTask", compensationTask.getTaskDefinitionKey());

      rule.getTaskService().complete(compensationTask.getId());
    }

    // then it is possible to complete the process successfully
    Task afterCompensateTask = rule.taskQuery().singleResult();
    Assert.assertNotNull(afterCompensateTask);

    rule.getTaskService().complete(afterCompensateTask.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("subProcessHandler.beforeCompensate.throwCompensate.2")
  public void testSubProcessHandlerThrowCompensateDeletion() {
    // it is possible to delete the process instance
    rule.getRuntimeService().deleteProcessInstance(rule.processInstance().getId(), null);

    // and the process is ended
    rule.assertScenarioEnded();
  }

}
