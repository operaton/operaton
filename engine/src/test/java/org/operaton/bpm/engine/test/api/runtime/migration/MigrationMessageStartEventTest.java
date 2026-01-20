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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.MessageReceiveModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;


class MigrationMessageStartEventTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;

  @Test
  void testMigrateEventSubscription() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MessageReceiveModels.MESSAGE_START_PROCESS);
    String sourceProcessDefinitionId = sourceProcessDefinition.getId();

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinitionId, sourceProcessDefinitionId)
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinitionId);
    EventSubscription eventSubscription = runtimeService
        .createEventSubscriptionQuery()
        .activityId("startEvent")
        .eventName(MessageReceiveModels.MESSAGE_NAME)
        .singleResult();

    // when
    runtimeService.newMigration(migrationPlan).processInstanceIds(processInstance.getId()).execute();

    // then
    assertEventSubscriptionMigrated(eventSubscription, "startEvent", MessageReceiveModels.MESSAGE_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionWithEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.MESSAGE_EVENT_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    runtimeService.newMigration(migrationPlan).processInstanceIds(processInstance.getId()).execute();

    // then
    EventSubscription eventSubscriptionAfter = runtimeService.createEventSubscriptionQuery().singleResult();

    assertThat(eventSubscriptionAfter).isNotNull();
    assertThat(eventSubscriptionAfter.getEventName()).isEqualTo(EventSubProcessModels.MESSAGE_NAME);

    runtimeService.correlateMessage(EventSubProcessModels.MESSAGE_NAME);
    testHelper.completeTask("eventSubProcessTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  protected void assertEventSubscriptionMigrated(EventSubscription eventSubscriptionBefore, String activityIdAfter, String eventName) {
    EventSubscription eventSubscriptionAfter = runtimeService.createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscriptionAfter).as("Expected that an event subscription with id '%s' exists after migration".formatted(eventSubscriptionBefore.getId())).isNotNull();

    assertThat(eventSubscriptionAfter.getEventType()).isEqualTo(eventSubscriptionBefore.getEventType());
    assertThat(eventSubscriptionAfter.getActivityId()).isEqualTo(activityIdAfter);
    assertThat(eventSubscriptionAfter.getEventName()).isEqualTo(eventName);
  }
}
