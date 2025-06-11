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
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.TimerCatchModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.ClockTestUtil;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationTimerCatchEventTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateJob() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "timerCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "timerCatch");

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("timerCatch").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("timerCatch"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("timerCatch", testHelper.getSingleActivityInstanceBeforeMigration("timerCatch").getId())
      .done());

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobChangeActivityId() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(TimerCatchModels.ONE_TIMER_CATCH_PROCESS)
        .changeElementId("timerCatch", "newTimerCatch"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "newTimerCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "newTimerCatch");

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobPreserveTimerConfiguration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent()
      .intermediateCatchEvent("timerCatch")
        .timerWithDuration("PT50M")
      .userTask("userTask")
      .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "timerCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated(       // this also asserts that the due has not changed
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "timerCatch");

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobUpdateTimerConfiguration() {
    // given
    ClockTestUtil.setClockToDateWithoutMilliseconds();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
      .startEvent()
      .intermediateCatchEvent("timerCatch")
        .timerWithDuration("PT50M")
      .userTask("userTask")
      .endEvent()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "timerCatch")
        .updateEventTrigger()
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    Date newDueDate = new DateTime(ClockUtil.getCurrentTime()).plusMinutes(50).toDate();
    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "timerCatch",
        newDueDate);

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobChangeProcessKey() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(TimerCatchModels.ONE_TIMER_CATCH_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "timerCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "timerCatch");

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.ONE_TIMER_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(TimerCatchModels.SUBPROCESS_TIMER_CATCH_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("timerCatch", "timerCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertJobMigrated(
        testHelper.snapshotBeforeMigration.getJobs().get(0),
        "timerCatch");

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("timerCatch").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("timerCatch"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess")
          .activity("timerCatch", testHelper.getSingleActivityInstanceBeforeMigration("timerCatch").getId())
      .done());

    // and it is possible to trigger the event
    Job jobAfterMigration = testHelper.snapshotAfterMigration.getJobs().get(0);
    rule.getManagementService().executeJob(jobAfterMigration.getId());

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }
}
