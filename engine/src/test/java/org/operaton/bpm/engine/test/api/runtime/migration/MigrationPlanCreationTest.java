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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.UserTaskBuilder;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.MigrationPlanAssert.assertThat;
import static org.operaton.bpm.engine.test.util.MigrationPlanAssert.migrate;
import static org.operaton.bpm.engine.test.util.MigrationPlanAssert.variable;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationPlanCreationTest {

  public static final String MESSAGE_NAME = "Message";
  public static final String SIGNAL_NAME = "Signal";
  public static final String ERROR_CODE = "Error";
  public static final String ESCALATION_CODE = "Escalation";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  RuntimeService runtimeService;

  @Test
  void testExplicitInstructionGeneration() {

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask")
      );
  }

  @Test
  void testMigrateNonExistingSourceDefinition() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan("aNonExistingProcDefId", processDefinition.getId())
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Source process definition with id 'aNonExistingProcDefId' does not exist");
  }

  @Test
  void testMigrateNullSourceDefinition() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(null, processDefinition.getId())
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Source process definition id is null");
  }

  @Test
  void testMigrateNonExistingTargetDefinition() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(processDefinition.getId(), "aNonExistingProcDefId")
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Target process definition with id 'aNonExistingProcDefId' does not exist");
  }

  @Test
  void testMigrateNullTargetDefinition() {
    // given
    ProcessDefinition processDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(processDefinition.getId(), null)
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Target process definition id is null");
  }

  @Test
  void testMigrateNonExistingSourceActivityId() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("thisActivityDoesNotExist", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("thisActivityDoesNotExist", "Source activity 'thisActivityDoesNotExist' does not exist");
      });
  }

  @Test
  void testMigrateNullSourceActivityId() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities(null, "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures(null, "Source activity id is null");
      });
  }

  @Test
  void testMigrateNonExistingTargetActivityId() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", "thisActivityDoesNotExist");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask", "Target activity 'thisActivityDoesNotExist' does not exist");
      });
  }

  @Test
  void testMigrateNullTargetActivityId() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", null);

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask", "Target activity id is null");
      });
  }

  @Test
  void testMigrateTaskToHigherScope() {
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceDefinition)
      .hasTargetProcessDefinition(targetDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask")
      );
  }

  @Test
  void testMigrateToUnsupportedActivityType() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_RECEIVE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", "receiveTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Activities have incompatible types (UserTaskActivityBehavior is not compatible with ReceiveTaskActivityBehavior)"
          );
      });
  }

  @Test
  void testNotMigrateActivitiesOfDifferentType() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.SUBPROCESS_PROCESS)
      .swapElementIds("userTask", "subProcess")
    );
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask", "Activities have incompatible types (UserTaskActivityBehavior is not "
              + "compatible with SubProcessActivityBehavior)");
      });
  }

  @Test
  void testNotMigrateBoundaryEventsOfDifferentType() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
      .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done()
    );
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
      .boundaryEvent("boundary").signal(SIGNAL_NAME)
      .done()
    );
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("userTask", "userTask")
        .mapActivities("boundary", "boundary");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("boundary", "Events are not of the same type (boundaryMessage != boundarySignal)");
      });
  }

  @Test
  void testMigrateSubProcessToProcessDefinition() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapActivities("subProcess", targetDefinition.getId());

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("subProcess", "Target activity '%s' does not exist".formatted(targetDefinition.getId()));
      });
  }

  @Test
  void testMapEqualActivitiesWithParallelMultiInstance() {
    // given
    BpmnModelInstance testProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .<UserTaskBuilder>getBuilderForElementById("userTask")
      .multiInstance().parallel().cardinality("3").multiInstanceDone().done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Target activity 'userTask' is a descendant of multi-instance body 'userTask#multiInstanceBody' "
          + "that is not mapped from the source process definition."
          );
      });
  }

  @Test
  void testMapEqualBoundaryEvents() {
    BpmnModelInstance testProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("boundary", "boundary")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask"),
        migrate("boundary").to("boundary")
      );
  }

  @Test
  void testMapBoundaryEventsWithDifferentId() {
    BpmnModelInstance sourceProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(sourceProcess)
      .changeElementId("boundary", "newBoundary");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("boundary", "newBoundary")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask"),
        migrate("boundary").to("newBoundary")
      );
  }

  @Test
  void testMapBoundaryToMigratedActivity() {
    BpmnModelInstance sourceProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(sourceProcess)
      .changeElementId("userTask", "newUserTask");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "newUserTask")
      .mapActivities("boundary", "boundary")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("newUserTask"),
        migrate("boundary").to("boundary")
      );
  }

  @Test
  void testMapBoundaryToParallelActivity() {
    // given
    BpmnModelInstance sourceProcess = modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask2")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask1", "userTask1")
        .mapActivities("userTask2", "userTask2")
        .mapActivities("boundary", "boundary");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("boundary",
            "The source activity's event scope (userTask1) must be mapped to the target activity's event scope (userTask2)"
          );
      });
  }

  @Test
  void testMapBoundaryToHigherScope() {
    BpmnModelInstance sourceProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("boundary", "boundary")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask"),
        migrate("boundary").to("boundary")
      );
  }

  @Test
  void testMapBoundaryToLowerScope() {
    BpmnModelInstance sourceProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .mapActivities("boundary", "boundary")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask"),
        migrate("boundary").to("boundary")
      );
  }

  @Test
  void testMapBoundaryToChildActivity() {
    // given
    BpmnModelInstance sourceProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask", "userTask")
        .mapActivities("boundary", "boundary");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("boundary",
            "The source activity's event scope (subProcess) must be mapped to the target activity's event scope (userTask)"
          );
      });
  }

  @Test
  void testMapBoundaryToParentActivity() {
    // given
    BpmnModelInstance sourceProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("userTask")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();
    BpmnModelInstance targetProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("boundary").message(MESSAGE_NAME)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetProcess);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("subProcess", "subProcess")
        .mapActivities("userTask", "userTask")
        .mapActivities("boundary", "boundary");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("boundary",
            "The source activity's event scope (userTask) must be mapped to the target activity's event scope (subProcess)",
            "The closest mapped ancestor 'subProcess' is mapped to scope 'subProcess' which is not an ancestor of target scope 'boundary'"
          );
      });
  }

  @Test
  void testMapAllBoundaryEvents() {
    BpmnModelInstance testProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .activityBuilder("subProcess")
        .boundaryEvent("error").error(ERROR_CODE)
      .moveToActivity("subProcess")
        .boundaryEvent("escalation").escalation(ESCALATION_CODE)
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("error", "error")
      .mapActivities("escalation", "escalation")
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("subProcess").to("subProcess"),
        migrate("error").to("error"),
        migrate("escalation").to("escalation"),
        migrate("userTask").to("userTask")
      );

  }

  @Test
  void testMapProcessDefinitionWithEventSubProcess() {
    BpmnModelInstance testProcess = modify(ProcessModels.ONE_TASK_PROCESS)
      .addSubProcessTo(ProcessModels.PROCESS_KEY)
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent().message(MESSAGE_NAME)
      .endEvent()
      .subProcessDone()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask")
      );
  }

  @Test
  void testMapSubProcessWithEventSubProcess() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.NESTED_EVENT_SUB_PROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.NESTED_EVENT_SUB_PROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("subProcess").to("subProcess"),
        migrate("userTask").to("userTask")
      );
  }

  @Test
  void testMapActivityWithUnmappedParentWhichHasAEventSubProcessChild() {
    BpmnModelInstance testProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .addSubProcessTo("subProcess")
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent().message(MESSAGE_NAME)
      .endEvent()
      .subProcessDone()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("userTask").to("userTask")
      );
  }

  @Test
  void testMapUserTaskInEventSubProcess() {
    BpmnModelInstance testProcess = modify(ProcessModels.SUBPROCESS_PROCESS)
      .addSubProcessTo("subProcess")
      .triggerByEvent()
      .embeddedSubProcess()
      .startEvent().message(MESSAGE_NAME)
      .userTask("innerTask")
      .endEvent()
      .subProcessDone()
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(testProcess);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(testProcess);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .mapActivities("innerTask", "innerTask")
        .build();

      assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasInstructions(
          migrate("userTask").to("userTask"),
          migrate("innerTask").to("innerTask")
        );
  }

  @Test
  void testNotMapActivitiesMoreThanOnce() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask1", "userTask1")
        .mapActivities("userTask1", "userTask2");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask1",
            "There are multiple mappings for source activity id 'userTask1'",
            "There are multiple mappings for source activity id 'userTask1'"
          );
      });
  }

  @Test
  void testCannotUpdateEventTriggerForNonEvent() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask").updateEventTrigger();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("userTask",
            "Cannot update event trigger because the activity does not define a persistent event trigger"
          );
      });
  }

  @Test
  void testCannotUpdateEventTriggerForEventSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("eventSubProcess", "eventSubProcess").updateEventTrigger();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasInstructionFailures("eventSubProcess",
            "Cannot update event trigger because the activity does not define a persistent event trigger"
          );
      });
  }

  @Test
  void testCanUpdateEventTriggerForEventSubProcessStartEvent() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(EventSubProcessModels.TIMER_EVENT_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("eventSubProcessStart", "eventSubProcessStart").updateEventTrigger()
      .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasInstructions(
        migrate("eventSubProcessStart").to("eventSubProcessStart").updateEventTrigger(true)
      );
  }

  @Test
  void shouldSetVariable() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Collections.singletonMap("foo", "bar"))
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar")
        );
  }

  @Test
  void shouldSetVariables() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    variables.put("bar", 5);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(variables)
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar"),
            variable().name("bar").value(5)
        );
  }

  @Test
  void shouldSetUntypedVariable() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Variables.putValue("foo", "bar"))
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
      .hasSourceProcessDefinition(sourceProcessDefinition)
      .hasTargetProcessDefinition(targetProcessDefinition)
      .hasVariables(
          variable().name("foo").value("bar")
      );
  }

  @Test
  void shouldSetUntypedVariables() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValue("bar", 5)
        )
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar"),
            variable().name("bar").value(5)
        );
  }

  @Test
  void shouldSetMapOfTypedVariable() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(Collections.singletonMap("foo", Variables.stringValue("bar")))
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar").typed()
        );
  }

  @Test
  void shouldSetVariableMapOfTypedVariable() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValueTyped("foo", Variables.stringValue("bar"))
        )
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar").typed()
        );
  }

  @Test
  void shouldSetTypedAndUntypedVariables() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(
            Variables.putValue("foo", "bar")
                .putValueTyped("bar", Variables.integerValue(5))
        )
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .hasVariables(
            variable().name("foo").value("bar"),
            variable().name("bar").value(5).typed()
        );
  }

  @Test
  void shouldSetNullVariables() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(null)
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .variablesNull();
  }

  @Test
  void shouldSetEmptyVariables() {
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .setVariables(new HashMap<>())
        .mapEqualActivities()
        .build();

    assertThat(migrationPlan)
        .hasSourceProcessDefinition(sourceProcessDefinition)
        .hasTargetProcessDefinition(targetProcessDefinition)
        .variablesEmpty();
  }

  @Test
  void shouldThrowValidationExceptionDueToSerializationFormatForbiddenForVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    var migrationPlanBuilder = runtimeService
          .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
          .setVariables(
              Variables.putValueTyped("foo",
                  Variables.serializedObjectValue()
                      .serializedValue("[]")
                      .objectTypeName(ArrayList.class.getName())
                      .serializationDataFormat(Variables.SerializationDataFormats.JAVA.getName())
                      .create())
          )
          .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasVariableFailures("foo",
              "Cannot set variable with name foo. Java serialization format is prohibited");
      });
  }

  @Test
  void shouldThrowValidationExceptionDueToSerializationFormatForbiddenForVariables() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    ObjectValue objectValue = Variables.serializedObjectValue()
        .serializedValue("[]")
        .objectTypeName(ArrayList.class.getName())
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA.getName())
        .create();
    var migrationPlanBuilder = runtimeService
          .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
          .setVariables(
              Variables.putValueTyped("foo", objectValue)
              .putValueTyped("bar", objectValue)
          )
          .mapEqualActivities();

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasVariableFailures("foo",
              "Cannot set variable with name foo. Java serialization format is prohibited")
          .hasVariableFailures("bar",
              "Cannot set variable with name bar. Java serialization format is prohibited");
      });
  }

  @Test
  void shouldThrowExceptionDueToInstructionsAndSerializationFormatForbiddenForVariable() {
    // given
    ProcessDefinition sourceProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition =
        testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    ObjectValue objectValue = Variables.serializedObjectValue()
        .serializedValue("[]")
        .objectTypeName(ArrayList.class.getName())
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA.getName())
        .create();
    var migrationPlanBuilder = runtimeService
          .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
          .setVariables(Variables.putValueTyped("foo", objectValue))
          .mapActivities("foo", "bar")
          .mapActivities("bar", "foo");

    // when/then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        var exception = (MigrationPlanValidationException) e;
        assertThat(exception.getValidationReport())
          .hasVariableFailures("foo",
              "Cannot set variable with name foo. Java serialization format is prohibited")
          .hasInstructionFailures("foo",
              "Source activity 'foo' does not exist", "Target activity 'bar' does not exist")
          .hasInstructionFailures("bar",
              "Source activity 'bar' does not exist", "Target activity 'foo' does not exist");
      });
  }

}
