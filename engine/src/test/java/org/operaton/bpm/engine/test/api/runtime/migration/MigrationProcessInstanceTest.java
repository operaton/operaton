/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class MigrationProcessInstanceTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected MigrationTestRule testRule = new MigrationTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

  protected RuntimeService runtimeService;

  @Before
  public void initServices() {
    runtimeService = rule.getRuntimeService();
  }

  @Test
  public void testMigrateWithIdVarargsArray() {
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
  public void testNullMigrationPlan() {
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
  public void testNullProcessInstanceIdsList() {
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
  public void testProcessInstanceIdsListWithNullValue() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();
    var migrationPlanExecutionBuilder = runtimeService.newMigration(migrationPlan).processInstanceIds(Arrays.asList("foo", null, "bar"));

    try {
      migrationPlanExecutionBuilder.execute();
      fail("Should not be able to migrate");
    }
    catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("process instance ids contains null value");
    }
  }

  @Test
  public void testNullProcessInstanceIdsArray() {
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
  public void testProcessInstanceIdsArrayWithNullValue() {
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
  public void testEmptyProcessInstanceIdsList() {
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
  public void testEmptyProcessInstanceIdsArray() {
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
  public void testNotMigrateProcessInstanceOfWrongProcessDefinition() {
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
  public void testNotMigrateUnknownProcessInstance() {
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
  public void testNotMigrateNullProcessInstance() {
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
  public void testMigrateProcessInstanceQuery() {
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
  public void testNullProcessInstanceQuery() {
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
  public void testEmptyProcessInstanceQuery() {
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
  public void testProcessInstanceQueryOfWrongProcessDefinition() {
    ProcessDefinition testProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition wrongProcessDefinition = testRule.deployAndGetDefinition(ProcessModels.SUBPROCESS_PROCESS);

    runtimeService.startProcessInstanceById(wrongProcessDefinition.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(testProcessDefinition.getId(), testProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstanceQuery wrongProcessInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionId(wrongProcessDefinition.getId());
    assertThat(wrongProcessInstanceQuery.count()).isEqualTo(1);
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
  public void testProcessInstanceIdsAndQuery() {
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
  public void testOverlappingProcessInstanceIdsAndQuery() {
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
      .processInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId()))
      .processInstanceQuery(sourceProcessInstanceQuery)
      .execute();

    assertThat(targetProcessInstanceQuery.count()).isEqualTo(2);
  }

}
