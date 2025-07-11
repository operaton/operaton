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
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.MessageReceiveModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationMessageCatchEventTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateEventSubscription() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("messageCatch", "messageCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertEventSubscriptionMigrated("messageCatch", "messageCatch", MessageReceiveModels.MESSAGE_NAME);

    // and it is possible to trigger the receive task
    rule.getRuntimeService().correlateMessage(MessageReceiveModels.MESSAGE_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionChangeActivityId() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS)
        .changeElementId("messageCatch", "newMessageCatch"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("messageCatch", "newMessageCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertEventSubscriptionMigrated("messageCatch", "newMessageCatch", MessageReceiveModels.MESSAGE_NAME);

    // and it is possible to trigger the receive task
    rule.getRuntimeService().correlateMessage(MessageReceiveModels.MESSAGE_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionPreserveMessageName() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
        .startEvent()
        .intermediateCatchEvent("messageCatch")
          .message("new" + MessageReceiveModels.MESSAGE_NAME)
        .userTask("userTask")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("messageCatch", "messageCatch")
        .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the message event subscription's event name has not changed
    testHelper.assertEventSubscriptionMigrated("messageCatch", "messageCatch", MessageReceiveModels.MESSAGE_NAME);

    // and it is possible to trigger the receive task
    rule.getRuntimeService().correlateMessage(MessageReceiveModels.MESSAGE_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionUpdateMessageName() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS)
      .renameMessage(MessageReceiveModels.MESSAGE_NAME, "new" + MessageReceiveModels.MESSAGE_NAME));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("messageCatch", "messageCatch")
          .updateEventTrigger()
        .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the message event subscription's event name has changed
    testHelper.assertEventSubscriptionMigrated(
        "messageCatch", MessageReceiveModels.MESSAGE_NAME,
        "messageCatch", "new" + MessageReceiveModels.MESSAGE_NAME);

    // and it is possible to trigger the event
    rule.getRuntimeService().correlateMessage("new" + MessageReceiveModels.MESSAGE_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionUpdateMessageNameWithExpression() {
    // given
    String newMessageName = "new" + MessageReceiveModels.MESSAGE_NAME + "-${var}";
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(MessageReceiveModels.ONE_MESSAGE_CATCH_PROCESS)
        .renameMessage(MessageReceiveModels.MESSAGE_NAME, newMessageName));

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("messageCatch", "messageCatch")
        .updateEventTrigger()
        .build();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "foo");

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan, variables);

    // then the message event subscription's event name has changed
    String resolvedMessageName = "new" + MessageReceiveModels.MESSAGE_NAME + "-foo";
    testHelper.assertEventSubscriptionMigrated(
        "messageCatch", MessageReceiveModels.MESSAGE_NAME,
        "messageCatch", resolvedMessageName);

    // and it is possible to trigger the event
    rule.getRuntimeService().correlateMessage(resolvedMessageName);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }
}
