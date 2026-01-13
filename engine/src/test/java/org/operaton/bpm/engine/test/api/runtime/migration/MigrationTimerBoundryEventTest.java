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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.util.DateTestUtil.formatDate;

class MigrationTimerBoundryEventTest {

  private static final String DUE_DATE_IN_THE_PAST = "2018-02-11T12:13:14Z";

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  ManagementService managementService;
  RuntimeService runtimeService;
  TaskService taskService;

  @AfterEach
  void cleanUpJobs() {
    List<Job> jobs = managementService.createJobQuery().list();
    if (!jobs.isEmpty()) {
      for (Job job : jobs) {
        managementService.deleteJob(job.getId());
      }
    }
  }

  @Test
  void testMigrationNonInterruptingTimerEvent() {
    // given
    BpmnModelInstance model = createModel(false, DUE_DATE_IN_THE_PAST);
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).isEmpty();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
  }

  @Test
  void testMigrationInterruptingTimerEvent() {
    // given
    BpmnModelInstance model = createModel(true, DUE_DATE_IN_THE_PAST);
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).isEmpty();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isZero();
  }

  @Test
  void testMigrationNonTriggeredInterruptingTimerEvent() {
    // given
    Date futureDueDate = DateUtils.addYears(ClockUtil.getCurrentTime(), 1);
    BpmnModelInstance model = createModel(true, formatDate(futureDueDate));
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).hasSize(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
  }

  @Test
  void testMigrationTwoNonInterruptingTimerEvents() {
    // given
    Date futureDueDate = DateUtils.addYears(ClockUtil.getCurrentTime(), 1);
    BpmnModelInstance model = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .boundaryEvent("timerPast")
          .cancelActivity(false)
          .timerWithDate(DUE_DATE_IN_THE_PAST)
        .userTask("past")
        .moveToActivity("userTask")
          .boundaryEvent("timerFuture")
          .cancelActivity(false)
          .timerWithDate(formatDate(futureDueDate))
        .userTask("future")
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(model);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(model);

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().duedateLowerThan(ClockUtil.getCurrentTime()).singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).hasSize(1);
    assertThat(managementService.createJobQuery().duedateHigherThan(ClockUtil.getCurrentTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("past").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("future").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
  }

  @Test
  void testMigrationWithTargetNonInterruptingTimerEvent() {
    // given
    BpmnModelInstance sourceModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .userTask("afterTimer")
        .endEvent("endEvent")
        .done();
    BpmnModelInstance targetModel = createModel(false, DUE_DATE_IN_THE_PAST);
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isZero();
    assertThat(managementService.createJobQuery().count()).isOne();
  }

  @Test
  void testMigrationWithSourceNonInterruptingTimerEvent() {
    // given
    BpmnModelInstance sourceModel = createModel(false, DUE_DATE_IN_THE_PAST);
    BpmnModelInstance targetModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .userTask("afterTimer")
        .endEvent("endEvent")
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).isEmpty();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isOne();
  }

  @Test
  void testMigrationTwoToOneNonInterruptingTimerEvents() {
    // given
    Date futureDueDate = DateUtils.addYears(ClockUtil.getCurrentTime(), 1);
    BpmnModelInstance sourceModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .boundaryEvent("timerPast")
          .cancelActivity(false)
          .timerWithDate(DUE_DATE_IN_THE_PAST)
        .userTask("past")
        .moveToActivity("userTask")
          .boundaryEvent("timerFuture")
          .cancelActivity(false)
          .timerWithDate(formatDate(futureDueDate))
        .userTask("future")
        .done();
    BpmnModelInstance targetModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
          .boundaryEvent("timerFuture")
          .cancelActivity(false)
          .timerWithDate(formatDate(futureDueDate))
        .userTask("future")
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().activityId("timerPast").singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .mapActivities("timerPast", "timerFuture")
        .mapActivities("past", "future")
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().duedateHigherThan(ClockUtil.getCurrentTime()).list();
    assertThat(list).hasSize(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("future").count()).isOne();
  }

  @Test
  void testMigrationOneToTwoNonInterruptingTimerEvents() {
    // given
    Date futureDueDate = DateUtils.addYears(ClockUtil.getCurrentTime(), 1);
    BpmnModelInstance sourceModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
          .boundaryEvent("timerFuture")
          .cancelActivity(false)
          .timerWithDate(formatDate(futureDueDate))
        .userTask("future")
        .done();
    BpmnModelInstance targetModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .boundaryEvent("timerPast")
          .cancelActivity(false)
          .timerWithDate(DUE_DATE_IN_THE_PAST)
        .userTask("past")
        .moveToActivity("userTask")
          .boundaryEvent("timerFuture")
          .cancelActivity(false)
          .timerWithDate(formatDate(futureDueDate))
        .userTask("future")
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    assertThat(managementService.createJobQuery().activityId("timerPast").singleResult()).isNull();

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertBoundaryTimerJobMigrated("timerFuture", "timerFuture");
    testHelper.assertBoundaryTimerJobCreated("timerPast");
  }

  @Test
  void testMigrationNonInterruptingTimerEventDifferentActivityId() {
    // given
    BpmnModelInstance sourceModel = createModel(false, DUE_DATE_IN_THE_PAST);
    BpmnModelInstance targetModel = Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .boundaryEvent("timer2")
          .cancelActivity(false)
          .timerWithDate(DUE_DATE_IN_THE_PAST)
        .userTask("afterTimer")
        .endEvent("endEvent")
        .done();
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(targetModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .mapActivities("timer", "timer2")
      .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).isEmpty();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterTimer").count()).isOne();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isOne();
  }

  protected BpmnModelInstance createModel(boolean isCancelActivity, String date) {
    return Bpmn.createExecutableProcess()
        .startEvent("startEvent")
        .userTask("userTask").name("User Task")
        .boundaryEvent("timer")
          .cancelActivity(isCancelActivity)
          .timerWithDate(date)
        .userTask("afterTimer")
        .endEvent("endEvent")
        .done();
  }
}
