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

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.operaton.bpm.engine.test.util.MigrationPlanValidationReportAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationInstruction;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ExternalTaskModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ServiceTaskModels;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigrationExternalTaskTest {

  public static final String WORKER_ID = "foo";

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected MigrationTestRule testHelper = new MigrationTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testHelper);

  @Test
  public void testTrees() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the execution and activity instance tree are exactly as before migration
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("externalTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("externalTask"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("externalTask", testHelper.getSingleActivityInstanceBeforeMigration("externalTask").getId())
      .done());

  }

  @Test
  public void testProperties() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY)
        .changeElementId("externalTask", "newExternalTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "newExternalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    ExternalTask externalTaskBeforeMigration = rule.getExternalTaskService().createExternalTaskQuery().singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then all properties are the same apart from the process reference
    ExternalTask externalTaskAfterMigration = rule.getExternalTaskService().createExternalTaskQuery().singleResult();

    assertThat(externalTaskAfterMigration.getActivityId()).isEqualTo("newExternalTask");
    assertThat(externalTaskAfterMigration.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    assertThat(externalTaskAfterMigration.getProcessDefinitionKey()).isEqualTo("new" + ProcessModels.PROCESS_KEY);

    assertThat(externalTaskAfterMigration.getPriority()).isEqualTo(externalTaskBeforeMigration.getPriority());
    assertThat(externalTaskAfterMigration.getActivityInstanceId()).isEqualTo(externalTaskBeforeMigration.getActivityInstanceId());
    assertThat(externalTaskAfterMigration.getErrorMessage()).isEqualTo(externalTaskBeforeMigration.getErrorMessage());
    assertThat(externalTaskAfterMigration.getExecutionId()).isEqualTo(externalTaskBeforeMigration.getExecutionId());
    assertThat(externalTaskAfterMigration.getId()).isEqualTo(externalTaskBeforeMigration.getId());
    assertThat(externalTaskAfterMigration.getLockExpirationTime()).isEqualTo(externalTaskBeforeMigration.getLockExpirationTime());
    assertThat(externalTaskAfterMigration.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(externalTaskAfterMigration.getRetries()).isEqualTo(externalTaskBeforeMigration.getRetries());
    assertThat(externalTaskAfterMigration.getTenantId()).isEqualTo(externalTaskBeforeMigration.getTenantId());
    assertThat(externalTaskAfterMigration.getTopicName()).isEqualTo(externalTaskBeforeMigration.getTopicName());
    assertThat(externalTaskAfterMigration.getWorkerId()).isEqualTo(externalTaskBeforeMigration.getWorkerId());
  }


  @Test
  public void testContinueProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the task
    LockedExternalTask task = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    rule.getExternalTaskService().complete(task.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testChangeTaskConfiguration() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .serviceTaskBuilder("externalTask")
        .operatonTopic("new" + ExternalTaskModels.TOPIC)
        .operatonTaskPriority(Integer.toString(ExternalTaskModels.PRIORITY * 2))
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the task's topic and priority have not changed
    ExternalTask externalTaskAfterMigration = rule.getExternalTaskService().createExternalTaskQuery().singleResult();
    assertThat(externalTaskAfterMigration.getPriority()).isEqualTo(ExternalTaskModels.PRIORITY.longValue());
    assertThat(externalTaskAfterMigration.getTopicName()).isEqualTo(ExternalTaskModels.TOPIC);

  }

  @Test
  public void testChangeTaskType() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
        .startEvent()
        .businessRuleTask("externalBusinessRuleTask")
          .operatonType(ExternalTaskModels.EXTERNAL_TASK_TYPE)
          .operatonTopic(ExternalTaskModels.TOPIC)
          .operatonTaskPriority(ExternalTaskModels.PRIORITY.toString())
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalBusinessRuleTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the task and process can be completed
    LockedExternalTask task = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    rule.getExternalTaskService().complete(task.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());

  }

  @Test
  public void testLockedTaskProperties() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY)
        .changeElementId("externalTask", "newExternalTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "newExternalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    ExternalTask externalTaskBeforeMigration = rule.getExternalTaskService().createExternalTaskQuery().singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the locking properties have not been changed
    ExternalTask externalTaskAfterMigration = rule.getExternalTaskService().createExternalTaskQuery().singleResult();

    assertThat(externalTaskAfterMigration.getLockExpirationTime()).isEqualTo(externalTaskBeforeMigration.getLockExpirationTime());
    assertThat(externalTaskAfterMigration.getWorkerId()).isEqualTo(externalTaskBeforeMigration.getWorkerId());
  }

  @Test
  public void testLockedTaskContinueProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY)
        .changeElementId("externalTask", "newExternalTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "newExternalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    LockedExternalTask externalTask = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the task and the process
    rule.getExternalTaskService().complete(externalTask.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void cannotMigrateFromExternalToClassDelegateServiceTask() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ServiceTaskModels.oneClassDelegateServiceTask("foo.Bar"));
    var runtimeService = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("externalTask", "serviceTask");

    try {
      runtimeService.build();
      Assert.fail("exception expected");
    } catch (MigrationPlanValidationException e) {
      // then
      assertThat(e.getValidationReport())
      .hasInstructionFailures("externalTask",
        "Activities have incompatible types (ExternalTaskActivityBehavior is not compatible with"
        + " ClassDelegateActivityBehavior)"
      );
    }
  }

  @Test
  public void testAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the task
    LockedExternalTask task = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    rule.getExternalTaskService().complete(task.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testRemoveParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.SUBPROCESS_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "externalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then it is possible to complete the task
    LockedExternalTask task = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    rule.getExternalTaskService().complete(task.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testIncident() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .changeElementId("externalTask", "newExternalTask"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("externalTask", "newExternalTask")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    ExternalTask externalTask = rule.getExternalTaskService().createExternalTaskQuery().singleResult();
    rule.getExternalTaskService().setRetries(externalTask.getId(), 0);

    Incident incidentBeforeMigration = rule.getRuntimeService().createIncidentQuery().singleResult();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then the incident has migrated
    Incident incidentAfterMigration = rule.getRuntimeService().createIncidentQuery().singleResult();
    assertNotNull(incidentAfterMigration);

    assertThat(incidentAfterMigration.getId()).isEqualTo(incidentBeforeMigration.getId());
    assertThat(incidentAfterMigration.getIncidentType()).isEqualTo(Incident.EXTERNAL_TASK_HANDLER_TYPE);
    assertThat(incidentAfterMigration.getConfiguration()).isEqualTo(externalTask.getId());

    assertThat(incidentAfterMigration.getActivityId()).isEqualTo("newExternalTask");
    assertThat(incidentAfterMigration.getProcessDefinitionId()).isEqualTo(targetProcessDefinition.getId());
    assertThat(incidentAfterMigration.getExecutionId()).isEqualTo(externalTask.getExecutionId());

    // and it is possible to complete the process
    rule.getExternalTaskService().setRetries(externalTask.getId(), 1);

    LockedExternalTask task = fetchAndLockSingleTask(ExternalTaskModels.TOPIC);
    rule.getExternalTaskService().complete(task.getId(), WORKER_ID);

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testIncidentWithoutMapExternalTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS)
        .changeElementId("externalTask", "newExternalTask"));

    //external task is not mapped to new external task
    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    ExternalTask externalTask = rule.getExternalTaskService().createExternalTaskQuery().singleResult();
    rule.getExternalTaskService().setRetries(externalTask.getId(), 0);

    Incident incidentBeforeMigration = rule.getRuntimeService().createIncidentQuery().singleResult();
    assertNotNull(incidentBeforeMigration);

    // when migration is executed
    try {
      testHelper.migrateProcessInstance(migrationPlan, processInstance);
      Assert.fail("Exception expected!");
    } catch (Exception ex) {
      Assert.assertTrue(ex instanceof MigratingProcessInstanceValidationException);
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithoutIdTest.bpmn"})
  public void testProcessDefinitionWithoutIdField() {
     // given

    ProcessDefinition sourceProcessDefinition = testHelper.deploy("org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithoutIdTest.bpmn").getDeployedProcessDefinitions().get(0);
    ProcessDefinition targetProcessDefinition = testHelper.deploy("org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithoutIdTest.bpmn").getDeployedProcessDefinitions().get(0);

    //external task is not mapped to new external task
    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    List<MigrationInstruction> instructions = migrationPlan.getInstructions();
    // test that the messageEventDefinition without an id isn't included
    assertThat(instructions).hasSize(2);
  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithoutIdTest.bpmn"})
  public void testProcessDefinitionWithIdField() {
     // given

    ProcessDefinition sourceProcessDefinition = testHelper.deploy("org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithIdTest.bpmn").getDeployedProcessDefinitions().get(0);
    ProcessDefinition targetProcessDefinition = testHelper.deploy("org/operaton/bpm/engine/test/api/externaltask/ExternalTaskWithIdTest.bpmn").getDeployedProcessDefinitions().get(0);

    //external task is not mapped to new external task
    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    List<MigrationInstruction> instructions = migrationPlan.getInstructions();
    assertThat(instructions).hasSize(2);
  }

  protected LockedExternalTask fetchAndLockSingleTask(String topic) {
    List<LockedExternalTask> tasks = rule
      .getExternalTaskService()
      .fetchAndLock(1, WORKER_ID)
      .topic(topic, 1000L)
      .execute();

    assertThat(tasks).hasSize(1);

    return tasks.get(0);
  }
}
