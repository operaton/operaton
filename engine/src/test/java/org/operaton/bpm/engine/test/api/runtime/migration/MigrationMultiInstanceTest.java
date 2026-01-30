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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.MultiInstanceProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationMultiInstanceTest {

  public static final String NUMBER_OF_INSTANCES = "nrOfInstances";
  public static final String NUMBER_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";
  public static final String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";
  public static final String LOOP_COUNTER = "loopCounter";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateParallelMultiInstanceTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration(miBodyOf("userTask")))
            .child("userTask").concurrent().noScope().up()
            .child("userTask").concurrent().noScope().up()
            .child("userTask").concurrent().noScope().up()
          .done());

    ActivityInstance[] userTaskInstances = testHelper.snapshotBeforeMigration.getActivityTree().getActivityInstances("userTask");

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginMiBody("userTask", testHelper.getSingleActivityInstanceBeforeMigration(miBodyOf("userTask")).getId())
            .activity("userTask", userTaskInstances[0].getId())
            .activity("userTask", userTaskInstances[1].getId())
            .activity("userTask", userTaskInstances[2].getId())
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(3);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateParallelMultiInstanceTasksVariables() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService()
      .startProcessInstanceById(migrationPlan.getSourceProcessDefinitionId());

    List<Task> tasksBeforeMigration = rule.getTaskService().createTaskQuery().list();
    Map<String, Integer> loopCounterDistribution = new HashMap<>();
    for (Task task : tasksBeforeMigration) {
      Integer loopCounter = (Integer) rule.getTaskService().getVariable(task.getId(), LOOP_COUNTER);
      loopCounterDistribution.put(task.getId(), loopCounter);
    }

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Task> tasks = testHelper.snapshotAfterMigration.getTasks();
    Task firstTask = tasks.get(0);
    assertThat(rule.getTaskService().getVariable(firstTask.getId(), NUMBER_OF_INSTANCES)).isEqualTo(3);
    assertThat(rule.getTaskService().getVariable(firstTask.getId(), NUMBER_OF_ACTIVE_INSTANCES)).isEqualTo(3);
    assertThat(rule.getTaskService().getVariable(firstTask.getId(), NUMBER_OF_COMPLETED_INSTANCES)).isEqualTo(0);

    for (Task task : tasks) {
      Integer loopCounter = (Integer) rule.getTaskService().getVariable(task.getId(), LOOP_COUNTER);
      assertThat(loopCounter).isEqualTo(loopCounterDistribution.get(task.getId()));
    }
  }

  @Test
  void testMigrateParallelMultiInstancePartiallyComplete() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance =
        rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeAnyTask("userTask");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration(miBodyOf("userTask")))
            .child("userTask").concurrent().noScope().up()
            .child("userTask").concurrent().noScope().up()
            .child("userTask").concurrent().noScope().up()
          .done());

    ActivityInstance[] userTaskInstances = testHelper.snapshotBeforeMigration.getActivityTree().getActivityInstances("userTask");

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginMiBody("userTask", testHelper.getSingleActivityInstanceBeforeMigration(miBodyOf("userTask")).getId())
            .activity("userTask", userTaskInstances[0].getId())
            .activity("userTask", userTaskInstances[1].getId())
            .transition("userTask") // bug CAM-5609
        .done());

    List<Task> migratedTasks = testHelper.snapshotAfterMigration.getTasks();
    assertThat(migratedTasks).hasSize(2);
    for (Task migratedTask : migratedTasks) {
      assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    }

    // and it is possible to successfully complete the migrated instance
    for (Task migratedTask : migratedTasks) {
      rule.getTaskService().complete(migratedTask.getId());
    }
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testMigrateParallelMiBodyRemoveSubprocess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("subProcess"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures(miBodyOf("subProcess"),
            "Cannot remove the inner activity of a multi-instance body when the body is mapped"
          );
      });
  }


  @Test
  void testMigrateParallelMiBodyAddSubprocess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("subProcess"))
      .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures(miBodyOf("userTask"),
            "Must map the inner activity of a multi-instance body when the body is mapped"
          );
      });
  }

  @Test
  void testMigrateSequentialMultiInstanceTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration(miBodyOf("userTask")))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginMiBody("userTask", testHelper.getSingleActivityInstanceBeforeMigration(miBodyOf("userTask")).getId())
            .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    Task migratedTask = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(migratedTask).isNotNull();
    assertThat(migratedTask.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.completeTask("userTask");
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  void testMigrateSequentialMultiInstanceTasksVariables() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    Task task = testHelper.snapshotAfterMigration.getTaskForKey("userTask");
    assertThat(rule.getTaskService().getVariable(task.getId(), NUMBER_OF_INSTANCES)).isEqualTo(3);
    assertThat(rule.getTaskService().getVariable(task.getId(), NUMBER_OF_ACTIVE_INSTANCES)).isEqualTo(1);
    assertThat(rule.getTaskService().getVariable(task.getId(), NUMBER_OF_COMPLETED_INSTANCES)).isEqualTo(0);
    assertThat(rule.getTaskService().getVariable(task.getId(), NUMBER_OF_COMPLETED_INSTANCES)).isEqualTo(0);
  }

  @Test
  void testMigrateSequentialMultiInstancePartiallyComplete() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask")
      .build();

    // when
    ProcessInstance processInstance =
        rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    testHelper.completeAnyTask("userTask");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration(miBodyOf("userTask")))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
        describeActivityInstanceTree(targetProcessDefinition.getId())
          .beginMiBody("userTask", testHelper.getSingleActivityInstanceBeforeMigration(miBodyOf("userTask")).getId())
            .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask("userTask");
    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }


  @Test
  void testMigrateSequenatialMiBodyRemoveSubprocess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("subProcess"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures(miBodyOf("subProcess"),
            "Cannot remove the inner activity of a multi-instance body when the body is mapped"
          );
      });
  }


  @Test
  void testMigrateSequentialMiBodyAddSubprocess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_SUBPROCESS_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("subProcess"))
      .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures(miBodyOf("userTask"),
            "Must map the inner activity of a multi-instance body when the body is mapped"
          );
      });
  }

  @Test
  void testMigrateParallelToSequential() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.PAR_MI_ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(MultiInstanceProcessModels.SEQ_MI_ONE_TASK_PROCESS);
    var migrationPlanBuilder = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(miBodyOf("userTask"), miBodyOf("userTask"))
      .mapActivities("userTask", "userTask");

    // when then
    assertThatThrownBy(migrationPlanBuilder::build)
      .isInstanceOf(MigrationPlanValidationException.class)
      .satisfies(e -> {
        MigrationPlanValidationException ex = (MigrationPlanValidationException) e;
        assertThat(ex.getValidationReport())
          .hasInstructionFailures(miBodyOf("userTask"),
            "Activities have incompatible types (ParallelMultiInstanceActivityBehavior is not "
            + "compatible with SequentialMultiInstanceActivityBehavior)"
          );
      });
  }

  protected String miBodyOf(String activityId) {
    return activityId + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX;
  }

}
