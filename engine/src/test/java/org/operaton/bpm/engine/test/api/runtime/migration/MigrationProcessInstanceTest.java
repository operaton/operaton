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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class MigrationProcessInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testRule = new MigrationTestExtension(rule);

  RuntimeService runtimeService;

  @Test
  void testMigrateWithIdVarargsArray() {
    ProcessDefinition sourceDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(sourceDefinition.getId());
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceById(sourceDefinition.getId());

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(processInstance1.getId(), processInstance2.getId())
      .execute();

    // then
    assertThat(runtimeService.createProcessInstanceQuery()
        .processDefinitionId(targetDefinition.getId()).count()).isEqualTo(2);

  }

  @Test
  void testNullMigrationPlan() {
    var migrationPlanExecutionBuilder = runtimeService.newMigration(null).processInstanceIds(Collections.<String>emptyList());
    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("migration plan is null");
    }
  }

  @Test
  void testNullProcessInstanceIdsList() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds((List<String>) null);

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testProcessInstanceIdsListWithNullValue() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(List.of("foo", null, "bar"));

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @Test
  void testNullProcessInstanceIdsArray() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds((String[]) null);

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testProcessInstanceIdsArrayWithNullValue() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds("foo", null, "bar");

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @Test
  void testEmptyProcessInstanceIdsList() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Collections.<String>emptyList());

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testEmptyProcessInstanceIdsArray() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(new String[]{});

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testNotMigrateProcessInstanceOfWrongProcessDefinition() {
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition wrongProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(wrongProcessDefinition.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Collections.singletonList(processInstance.getId()));

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).startsWith("ENGINE-23002");
    }
  }

  @Test
  void testNotMigrateUnknownProcessInstance() {
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Collections.singletonList("unknown"));

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).startsWith("ENGINE-23003");
    }
  }

  @Test
  void testNotMigrateNullProcessInstance() {
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Collections.<String>singletonList(null));

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @Test
  void testMigrateProcessInstanceQuery() {
    int processInstanceCount = 10;

    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    for(int i = 0; i < processInstanceCount; i++) {
      runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    }

    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    ProcessInstanceQuery targetProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(targetProcessDefinition.getId());

    assertThat(sourceProcessInstanceQuery.count()).isEqualTo(processInstanceCount);
    assertThat(targetProcessInstanceQuery.count()).isZero();


    runtimeService.newMigration(migrationPlan)
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    assertThat(sourceProcessInstanceQuery.count()).isZero();
    assertThat(targetProcessInstanceQuery.count()).isEqualTo(processInstanceCount);
  }

  @Test
  void testNullProcessInstanceQuery() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceQuery(null);

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testEmptyProcessInstanceQuery() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery emptyProcessInstanceQuery = runtimeService.createProcessInstanceQuery();
    assertThat(emptyProcessInstanceQuery.count()).isZero();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceQuery(emptyProcessInstanceQuery);

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids is empty");
    }
  }

  @Test
  void testProcessInstanceQueryOfWrongProcessDefinition() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition wrongProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    runtimeService.startProcessInstanceById(wrongProcessDefinition.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery wrongProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(wrongProcessDefinition.getId());
    assertThat(wrongProcessInstanceQuery.count()).isOne();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceQuery(wrongProcessInstanceQuery);

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).startsWith("ENGINE-23002");
    }
  }

  @Test
  void testProcessInstanceIdsAndQuery() {
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId());
    ProcessInstanceQuery targetProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(targetProcessDefinition.getId());

    assertThat(targetProcessInstanceQuery.count()).isZero();

    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(Collections.singletonList(processInstance1.getId()))
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    assertThat(targetProcessInstanceQuery.count()).isEqualTo(2);
  }

  @Test
  void testOverlappingProcessInstanceIdsAndQuery() {
    ProcessDefinition sourceProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    ProcessInstanceQuery sourceProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId());
    ProcessInstanceQuery targetProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(targetProcessDefinition.getId());

    assertThat(targetProcessInstanceQuery.count()).isZero();

    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance1.getId(), processInstance2.getId()))
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    assertThat(targetProcessInstanceQuery.count()).isEqualTo(2);
  }

}
