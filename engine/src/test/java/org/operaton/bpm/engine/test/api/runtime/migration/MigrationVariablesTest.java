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
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.migration.models.AsyncProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationVariablesTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().randomEngineName().closeEngineAfterAllTests()
      .configurator(config -> config.setJavaSerializationFormatEnabled(true)).build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  protected static final BpmnModelInstance ONE_BOUNDARY_TASK = ModifiableBpmnModelInstance.modify(ProcessModels.ONE_TASK_PROCESS)
      .activityBuilder("userTask")
      .boundaryEvent()
      .message("Message")
      .done();

  protected static final BpmnModelInstance CONCURRENT_BOUNDARY_TASKS = ModifiableBpmnModelInstance.modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .activityBuilder("userTask1")
      .boundaryEvent()
      .message("Message")
      .moveToActivity("userTask2")
      .boundaryEvent()
      .message("Message")
      .done();

  protected static final BpmnModelInstance SUBPROCESS_CONCURRENT_BOUNDARY_TASKS = ModifiableBpmnModelInstance.modify(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS)
      .activityBuilder("userTask1")
      .boundaryEvent()
      .message("Message")
      .moveToActivity("userTask2")
      .boundaryEvent()
      .message("Message")
      .done();

  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  void testVariableAtScopeExecutionInScopeActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtConcurrentExecutionInScopeActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree concurrentExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(concurrentExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtScopeExecutionInNonScopeActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId(), Variables.createVariables().putValue("foo", 42));

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtConcurrentExecutionInNonScopeActivity() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree concurrentExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(concurrentExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtConcurrentExecutionInScopeActivityAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(SUBPROCESS_CONCURRENT_BOUNDARY_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0)
        .getParent();

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    ExecutionTree userTask1CCExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0)
        .getParent();

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    ActivityInstance subProcessInstance = testHelper.getSingleActivityInstanceAfterMigration("subProcess");
    // for variables at concurrent executions that are parent of a leaf-scope-execution, the activity instance is
    // the activity instance id of the parent activity instance (which is probably a bug)
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1CCExecutionAfter.getId(), subProcessInstance.getId());
  }

  @Test
  void testVariableAtConcurrentExecutionInScopeActivityRemoveParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SUBPROCESS_CONCURRENT_BOUNDARY_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0)
        .getParent();

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    ExecutionTree userTask1CCExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0)
        .getParent();

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    // for variables at concurrent executions that are parent of a leaf-scope-execution, the activity instance is
    // the activity instance id of the parent activity instance (which is probably a bug)
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1CCExecutionAfter.getId(), processInstance.getId());
  }

  @Test
  void testVariableAtConcurrentExecutionInNonScopeActivityAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0);

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    ExecutionTree userTask1CCExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0);

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1CCExecutionAfter.getId());
  }

  @Test
  void testVariableAtConcurrentExecutionInNonScopeActivityRemoveParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0);

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    ExecutionTree userTask1CCExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0);

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1CCExecutionAfter.getId());
  }

  @Test
  void testVariableAtScopeExecutionInScopeActivityAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(SUBPROCESS_CONCURRENT_BOUNDARY_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask1")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariableLocal(task.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());
  }

  @Test
  void testVariableAtTaskAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.setVariableLocal(task.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    ExecutionTree userTask1ExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0);

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1ExecutionAfter.getId());
  }

  @Test
  void testVariableAtTaskAndConcurrentExecutionAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Task task = taskService.createTaskQuery().taskDefinitionKey("userTask1").singleResult();
    taskService.setVariableLocal(task.getId(), "foo", 42);
    runtimeService.setVariableLocal(task.getExecutionId(), "foo", 52);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance taskVarBeforeMigration = testHelper.snapshotBeforeMigration.getSingleTaskVariable(task.getId(), "foo");

    ExecutionTree userTask1ExecutionAfter  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask1")
        .get(0);

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(2);
    testHelper.assertVariableMigratedToExecution(taskVarBeforeMigration, userTask1ExecutionAfter.getId());
  }

  @Test
  void testVariableAtScopeExecutionBecomeNonScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getExecutions().get(0);

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, processInstance.getId());

    // and the variable is concurrent local, i.e. expands on tree expansion
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("userTask")
      .execute();

    VariableInstance variableAfterExpansion = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableAfterExpansion).isNotNull();
    assertThat(processInstance.getId()).isNotSameAs(variableAfterExpansion.getExecutionId());
  }

  @Test
  void testVariableAtConcurrentExecutionBecomeScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_SCOPE_TASKS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree concurrentExecution = executionTreeBeforeMigration.getLeafExecutions("userTask1").get(0);

    runtimeService.setVariableLocal(concurrentExecution.getId(), "foo", 42);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("foo");
    ExecutionTree userTask1CCExecution = testHelper.snapshotAfterMigration
      .getExecutionTree()
      .getLeafExecutions("userTask1")
      .get(0)
      .getParent();

    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, userTask1CCExecution.getId());
  }

  @Test
  void testVariableAtConcurrentAndScopeExecutionBecomeNonScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CONCURRENT_BOUNDARY_TASKS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getLeafExecutions("userTask1").get(0);
    ExecutionTree concurrentExecution = scopeExecution.getParent();

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", 42);
    runtimeService.setVariableLocal(concurrentExecution.getId(), "foo", 42);

    // when/then
    assertThatThrownBy(() -> testHelper.migrateProcessInstance(migrationPlan, processInstance))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The variable 'foo' exists in both, this scope"
          + " and concurrent local in the parent scope. Migrating to a non-scope activity would overwrite one of them.");
  }

  @Test
  void testVariableAtParentScopeExecutionAndScopeExecutionBecomeNonScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ONE_BOUNDARY_TASK);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree scopeExecution = executionTreeBeforeMigration.getLeafExecutions("userTask").get(0);

    runtimeService.setVariableLocal(scopeExecution.getId(), "foo", "userTaskScopeValue");
    runtimeService.setVariableLocal(processInstance.getId(), "foo", "processScopeValue");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the process scope variable was overwritten due to a compacted execution tree
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);

    VariableInstance variable = testHelper.snapshotAfterMigration.getVariables().iterator().next();

    assertThat(variable.getValue()).isEqualTo("userTaskScopeValue");
  }

  @Test
  void testVariableAtConcurrentExecutionAddParentScopeBecomeNonConcurrent() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.PARALLEL_TASK_AND_SUBPROCESS_PROCESS)
        .activityBuilder("subProcess")
        .operatonInputParameter("foo", "subProcessValue")
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());
    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree task1CcExecution = executionTreeBeforeMigration.getLeafExecutions("userTask1").get(0);
    ExecutionTree task2CcExecution = executionTreeBeforeMigration.getLeafExecutions("userTask2").get(0);

    runtimeService.setVariableLocal(task1CcExecution.getId(), "foo", "task1Value");
    runtimeService.setVariableLocal(task2CcExecution.getId(), "foo", "task2Value");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the io mapping variable was overwritten due to a compacted execution tree
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(2);

    List<String> values = new ArrayList<>();
    for (VariableInstance variable : testHelper.snapshotAfterMigration.getVariables()) {
      values.add((String) variable.getValue());
    }

    assertThat(values)
            .contains("task1Value")
            .contains("task2Value");
  }

  @Test
  void testAddScopeWithInputMappingAndVariableOnConcurrentExecutions() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS)
          .activityBuilder("subProcess").operatonInputParameter("foo", "inputOutputValue").done()
      );

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0);
    ExecutionTree userTask2CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask2")
        .get(0);

    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", "customValue");
    runtimeService.setVariableLocal(userTask2CCExecutionBefore.getId(), "foo", "customValue");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the scope variable instance has been overwritten during compaction (conform to prior behavior);
    // although this is tested here, changing this behavior may be ok in the future
    Collection<VariableInstance> variables = testHelper.snapshotAfterMigration.getVariables();
    assertThat(variables).hasSize(2);

    for (VariableInstance variable : variables) {
      assertThat(variable.getValue()).isEqualTo("customValue");
    }

    ExecutionTree subProcessExecution  = testHelper.snapshotAfterMigration.getExecutionTree()
        .getLeafExecutions("userTask2")
        .get(0)
        .getParent();

    assertThat(testHelper.snapshotAfterMigration.getSingleVariable(subProcessExecution.getId(), "foo")).isNotNull();
  }

  @Test
  void testVariableAtScopeAndConcurrentExecutionAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0);
    ExecutionTree userTask2CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask2")
        .get(0);

    runtimeService.setVariableLocal(processInstance.getId(), "foo", "processInstanceValue");
    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", "task1Value");
    runtimeService.setVariableLocal(userTask2CCExecutionBefore.getId(), "foo", "task2Value");

    VariableInstance processScopeVariable = runtimeService.createVariableInstanceQuery().variableValueEquals("foo", "processInstanceValue").singleResult();
    VariableInstance task1Variable = runtimeService.createVariableInstanceQuery().variableValueEquals("foo", "task1Value").singleResult();
    VariableInstance task2Variable = runtimeService.createVariableInstanceQuery().variableValueEquals("foo", "task2Value").singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the scope variable instance has been overwritten during compaction (conform to prior behavior);
    // although this is tested here, changing this behavior may be ok in the future
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(3);

    VariableInstance processScopeVariableAfterMigration = testHelper.snapshotAfterMigration.getVariable(processScopeVariable.getId());
    assertThat(processScopeVariableAfterMigration).isNotNull();
    assertThat(processScopeVariableAfterMigration.getValue()).isEqualTo("processInstanceValue");

    VariableInstance task1VariableAfterMigration = testHelper.snapshotAfterMigration.getVariable(task1Variable.getId());
    assertThat(task1VariableAfterMigration).isNotNull();
    assertThat(task1VariableAfterMigration.getValue()).isEqualTo("task1Value");

    VariableInstance task2VariableAfterMigration = testHelper.snapshotAfterMigration.getVariable(task2Variable.getId());
    assertThat(task2VariableAfterMigration).isNotNull();
    assertThat(task2VariableAfterMigration.getValue()).isEqualTo("task2Value");

  }

  @Test
  void testVariableAtScopeAndConcurrentExecutionRemoveParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    ProcessInstance processInstance = runtimeService
        .startProcessInstanceById(sourceProcessDefinition.getId());

    ExecutionTree executionTreeBeforeMigration =
        ExecutionTree.forExecution(processInstance.getId(), rule.getProcessEngine());

    ExecutionTree userTask1CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask1")
        .get(0);
    ExecutionTree userTask2CCExecutionBefore  = executionTreeBeforeMigration
        .getLeafExecutions("userTask2")
        .get(0);
    ExecutionTree subProcessExecution  = userTask1CCExecutionBefore.getParent();

    runtimeService.setVariableLocal(subProcessExecution.getId(), "foo", "subProcessValue");
    runtimeService.setVariableLocal(userTask1CCExecutionBefore.getId(), "foo", "task1Value");
    runtimeService.setVariableLocal(userTask2CCExecutionBefore.getId(), "foo", "task2Value");

    VariableInstance task1Variable = runtimeService.createVariableInstanceQuery().variableValueEquals("foo", "task1Value").singleResult();
    VariableInstance task2Variable = runtimeService.createVariableInstanceQuery().variableValueEquals("foo", "task2Value").singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the scope variable instance has been overwritten during compaction (conform to prior behavior);
    // although this is tested here, changing this behavior may be ok in the future
    Collection<VariableInstance> variables = testHelper.snapshotAfterMigration.getVariables();
    assertThat(variables).hasSize(2);

    VariableInstance task1VariableAfterMigration = testHelper.snapshotAfterMigration.getVariable(task1Variable.getId());
    assertThat(task1VariableAfterMigration).isNotNull();
    assertThat(task1VariableAfterMigration.getValue()).isEqualTo("task1Value");

    VariableInstance task2VariableAfterMigration = testHelper.snapshotAfterMigration.getVariable(task2Variable.getId());
    assertThat(task2VariableAfterMigration).isNotNull();
    assertThat(task2VariableAfterMigration.getValue()).isEqualTo("task2Value");

  }

  @Test
  void testVariableAtConcurrentExecutionInTransition() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("userTask")
        .startBeforeActivity("userTask")
        .execute();

    Execution concurrentExecution = runtimeService.createExecutionQuery().activityId("userTask").list().get(0);
    Job jobForExecution = rule.getManagementService().createJobQuery().executionId(concurrentExecution.getId()).singleResult();

    runtimeService.setVariableLocal(concurrentExecution.getId(), "var", "value");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    Job jobAfterMigration = rule.getManagementService().createJobQuery().jobId(jobForExecution.getId()).singleResult();

    testHelper.assertVariableMigratedToExecution(
        testHelper.snapshotBeforeMigration.getSingleVariable("var"),
        jobAfterMigration.getExecutionId());
  }

  @Test
  void testVariableAtConcurrentExecutionInTransitionAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_USER_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(AsyncProcessModels.ASYNC_BEFORE_SUBPROCESS_USER_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService()
        .createProcessInstanceById(sourceProcessDefinition.getId())
        .startBeforeActivity("userTask")
        .startBeforeActivity("userTask")
        .execute();

    Execution concurrentExecution = runtimeService.createExecutionQuery().activityId("userTask").list().get(0);
    Job jobForExecution = rule.getManagementService().createJobQuery().executionId(concurrentExecution.getId()).singleResult();

    runtimeService.setVariableLocal(concurrentExecution.getId(), "var", "value");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    Job jobAfterMigration = rule.getManagementService().createJobQuery().jobId(jobForExecution.getId()).singleResult();

    testHelper.assertVariableMigratedToExecution(
        testHelper.snapshotBeforeMigration.getSingleVariable("var"),
        jobAfterMigration.getExecutionId());
  }

  @Test
  void testCanMigrateWithObjectVariableThatFailsOnDeserialization()
  {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    ObjectValue objectValue = Variables
      .serializedObjectValue("does/not/deserialize")
      .serializationDataFormat(SerializationDataFormats.JAVA)
      .objectTypeName("and.this.is.a.nonexisting.Class")
      .create();

    runtimeService.setVariable(
        processInstance.getId(),
        "var",
        objectValue);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    ObjectValue migratedValue = runtimeService.getVariableTyped(processInstance.getId(), "var", false);
    assertThat(migratedValue.getValueSerialized()).isEqualTo(objectValue.getValueSerialized());
    assertThat(migratedValue.getObjectTypeName()).isEqualTo(objectValue.getObjectTypeName());
  }

}
