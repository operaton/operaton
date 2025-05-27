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
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CallActivityModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationCallActivityTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @BeforeEach
  void deployOneTaskProcess() {
    testHelper.deployAndGetDefinition(
        modify(ProcessModels.ONE_TASK_PROCESS)
          .changeElementId(ProcessModels.PROCESS_KEY, "oneTaskProcess"));
  }

  @BeforeEach
  void deployOneTaskCase() {
    testHelper.deploy("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn");
  }

  @Test
  void testCallBpmnProcessSimpleMigration() {
    // given
    BpmnModelInstance model = CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("callActivity").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // and it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCallCmmnCaseSimpleMigration() {
    // given
    BpmnModelInstance model = CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
        .child("callActivity").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // and it is possible to complete the called case instance
    CaseExecution caseExecution = rule.getCaseService()
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    testHelper.completeTask("PI_HumanTask_1");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());

    // and close the called case instance
    rule.getCaseService().withCaseExecution(caseExecution.getCaseInstanceId()).close();
    testHelper.assertCaseEnded(caseExecution.getCaseInstanceId());
  }

  @Test
  void testCallBpmnProcessAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"));
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        CallActivityModels.subProcessBpmnCallActivityProcess("oneTaskProcess"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("callActivity").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess")
          .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // and it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCallBpmnProcessParallelMultiInstance() {
    // given
    BpmnModelInstance model = modify(CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"))
      .activityBuilder("callActivity")
      .multiInstance()
      .parallel()
      .cardinality("1")
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity#multiInstanceBody", "callActivity#multiInstanceBody")
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity#multiInstanceBody"))
            .child("callActivity").concurrent().noScope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginMiBody("callActivity")
          .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // and the link between calling and called instance is maintained correctly

    testHelper.assertSuperExecutionOfProcessInstance(
        rule.getRuntimeService()
          .createProcessInstanceQuery()
          .processDefinitionKey("oneTaskProcess")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // and it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCallCmmnCaseParallelMultiInstance() {
    // given
    BpmnModelInstance model = modify(CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase"))
      .activityBuilder("callActivity")
      .multiInstance()
      .parallel()
      .cardinality("1")
      .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity#multiInstanceBody", "callActivity#multiInstanceBody")
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
    .hasProcessDefinitionId(targetProcessDefinition.getId())
    .matches(
      describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
        .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity#multiInstanceBody"))
          .child("callActivity").concurrent().noScope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("callActivity"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginMiBody("callActivity")
          .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // and the link between calling and called instance is maintained correctly
    testHelper.assertSuperExecutionOfCaseInstance(
        rule.getCaseService()
          .createCaseInstanceQuery()
          .caseDefinitionKey("oneTaskCase")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // and it is possible to complete the called case instance
    CaseExecution caseExecution = rule.getCaseService()
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult();

    testHelper.completeTask("PI_HumanTask_1");

    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());

    // and close the called case instance
    rule.getCaseService().withCaseExecution(caseExecution.getCaseInstanceId()).close();
    testHelper.assertCaseEnded(caseExecution.getCaseInstanceId());
  }

  @Test
  void testCallBpmnProcessParallelMultiInstanceRemoveMiBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        modify(CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"))
          .activityBuilder("callActivity")
          .multiInstance()
          .parallel()
          .cardinality("1")
          .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("callActivity").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // then the link between calling and called instance is maintained correctly

    testHelper.assertSuperExecutionOfProcessInstance(
        rule.getRuntimeService()
          .createProcessInstanceQuery()
          .processDefinitionKey("oneTaskProcess")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // then it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCallCmmnCaseParallelMultiInstanceRemoveMiBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        modify(CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase"))
          .activityBuilder("callActivity")
          .multiInstance()
          .parallel()
          .cardinality("1")
          .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("callActivity").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());


    // and the link between calling and called instance is maintained correctly
    testHelper.assertSuperExecutionOfCaseInstance(
        rule.getCaseService()
          .createCaseInstanceQuery()
          .caseDefinitionKey("oneTaskCase")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // and it is possible to complete the called case instance
    CaseExecution caseExecution = rule.getCaseService()
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult();

    testHelper.completeTask("PI_HumanTask_1");

    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());

    // and close the called case instance
    rule.getCaseService().withCaseExecution(caseExecution.getCaseInstanceId()).close();
    testHelper.assertCaseEnded(caseExecution.getCaseInstanceId());
  }

  @Test
  void testCallBpmnProcessSequentialMultiInstanceRemoveMiBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        modify(CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"))
          .activityBuilder("callActivity")
          .multiInstance()
          .sequential()
          .cardinality("1")
          .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("callActivity").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // then the link between calling and called instance is maintained correctly

    testHelper.assertSuperExecutionOfProcessInstance(
        rule.getRuntimeService()
          .createProcessInstanceQuery()
          .processDefinitionKey("oneTaskProcess")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // then it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCallCmmnCaseSequentialMultiInstanceRemoveMiBody() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(
        modify(CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase"))
          .activityBuilder("callActivity")
          .multiInstance()
          .sequential()
          .cardinality("1")
          .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CallActivityModels.oneCmmnCallActivityProcess("oneTaskCase"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("callActivity").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("callActivity", testHelper.getSingleActivityInstanceBeforeMigration("callActivity").getId())
      .done());

    // then the link between calling and called instance is maintained correctly
    testHelper.assertSuperExecutionOfCaseInstance(
        rule.getCaseService()
          .createCaseInstanceQuery()
          .caseDefinitionKey("oneTaskCase")
          .singleResult()
          .getId(),
        testHelper.getSingleExecutionIdForActivityAfterMigration("callActivity"));

    // and it is possible to complete the called case instance
    CaseExecution caseExecution = rule.getCaseService()
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult();

    testHelper.completeTask("PI_HumanTask_1");

    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());

    // and close the called case instance
    rule.getCaseService().withCaseExecution(caseExecution.getCaseInstanceId()).close();
    testHelper.assertCaseEnded(caseExecution.getCaseInstanceId());
  }

  @Test
  void testCallBpmnProcessReconfigureCallActivity() {
    // given
    BpmnModelInstance model = CallActivityModels.oneBpmnCallActivityProcess("oneTaskProcess");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(model).callActivityBuilder("callActivity")
        .calledElement("foo")
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("callActivity", "callActivity")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the called instance has not changed (e.g. not been migrated to a different process definition)
    ProcessInstance calledInstance = rule
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processDefinitionKey("oneTaskProcess")
      .singleResult();
    assertThat(calledInstance).isNotNull();

    // and it is possible to complete the called process instance
    testHelper.completeTask("userTask");
    // and the calling process instance
    testHelper.completeTask("userTask");

    testHelper.assertProcessEnded(processInstance.getId());
  }

}
