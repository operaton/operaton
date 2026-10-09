/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationAdHocIntegrationTest {

  @RegisterExtension
  static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(engine);

  RuntimeService runtimeService;
  TaskService taskService;

  @BeforeEach
  void services() {
    runtimeService = engine.getRuntimeService();
    taskService = engine.getTaskService();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveCalledProcessOrRejectMissingCallMappingAtomically(boolean mapCall) {
    deploy(definitions("<process id=\"called\" isExecutable=\"true\"><startEvent id=\"calledStart\"/>"
        + "<sequenceFlow id=\"calledFlow\" sourceRef=\"calledStart\" targetRef=\"calledTask\"/>"
        + "<userTask id=\"calledTask\"/></process>"));
    String xml = model("", "<callActivity id=\"call\" calledElement=\"called\"/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("id=\"call\"", "id=\"renamedCall\""));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    String scope = scope(process);
    runtimeService.triggerAdHocActivities(scope, List.of("call"), null);
    ProcessInstance called = runtimeService.createProcessInstanceQuery().superProcessInstanceId(process.getId()).singleResult();
    assertThat(called).isNotNull();
    runtimeService.setVariable(called.getId(), "calledVariable", "retained");
    Task calledTask = task(called.getId(), "calledTask");
    var builder = runtimeService.createMigrationPlan(source.getId(), target.getId()).mapActivities("adhoc", "adhoc");
    if (mapCall) {
      builder.mapActivities("call", "renamedCall");
    }
    MigrationPlan plan = builder.build();

    if (mapCall) {
      migrate(plan, process);
    } else {
      assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
          .hasMessageContaining("no migration instruction");
      assertDefinition(process, source);
    }

    assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId(process.getId()).singleResult().getId())
        .isEqualTo(called.getId());
    assertThat(task(called.getId(), "calledTask").getId()).isEqualTo(calledTask.getId());
    assertThat(runtimeService.getVariable(called.getId(), "calledVariable")).isEqualTo("retained");
    taskService.complete(calledTask.getId());
    testHelper.assertProcessEnded(called.getId());
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveExternalTaskLockRetriesAndVariablesOrRejectMissingMapping(boolean mapExternal) {
    String xml = model("", "<serviceTask id=\"external\" operaton:type=\"external\" operaton:topic=\"work\"/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("id=\"external\"", "id=\"renamedExternal\""));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("businessVariable", "retained"));
    runtimeService.triggerAdHocActivities(scope(process), List.of("external"), null);
    String externalId = engine.getExternalTaskService().fetchAndLock(1, "worker")
        .topic("work", 600000L).execute().get(0).getId();
    engine.getExternalTaskService().setRetries(externalId, 7);
    ExternalTask before = engine.getExternalTaskService().createExternalTaskQuery().externalTaskId(externalId).singleResult();
    runtimeService.setVariableLocal(before.getExecutionId(), "externalLocal", "retained");
    var builder = runtimeService.createMigrationPlan(source.getId(), target.getId()).mapActivities("adhoc", "adhoc");
    if (mapExternal) {
      builder.mapActivities("external", "renamedExternal");
    }
    MigrationPlan plan = builder.build();

    if (mapExternal) {
      migrate(plan, process);
    } else {
      assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
          .hasMessageContaining("no migration instruction");
      assertDefinition(process, source);
    }

    ExternalTask after = engine.getExternalTaskService().createExternalTaskQuery().externalTaskId(externalId).singleResult();
    assertThat(after.getExecutionId()).isEqualTo(before.getExecutionId());
    assertThat(after.getActivityInstanceId()).isEqualTo(before.getActivityInstanceId());
    assertThat(after.getActivityId()).isEqualTo(mapExternal ? "renamedExternal" : "external");
    assertThat(after.getProcessDefinitionId()).isEqualTo(mapExternal ? target.getId() : source.getId());
    assertThat(after.getWorkerId()).isEqualTo("worker");
    assertThat(after.getRetries()).isEqualTo(7);
    assertThat(after.getLockExpirationTime()).isEqualTo(before.getLockExpirationTime());
    assertThat(runtimeService.getVariableLocal(after.getExecutionId(), "externalLocal")).isEqualTo("retained");
    assertThat(runtimeService.getVariable(process.getId(), "businessVariable")).isEqualTo("retained");
    engine.getExternalTaskService().complete(externalId, "worker", Map.of("externalResult", "completed"));
    assertThat(runtimeService.getVariable(process.getId(), "externalResult")).isEqualTo("completed");
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveEventSubprocessSubscriptionAndCorrelateAfterMigration(boolean activeBeforeMigration) {
    String children = "<userTask id=\"taskA\"/><subProcess id=\"eventSubprocess\" triggeredByEvent=\"true\">"
        + "<startEvent id=\"messageStart\" isInterrupting=\"false\"><messageEventDefinition messageRef=\"notice\"/></startEvent>"
        + "<sequenceFlow id=\"eventFlow\" sourceRef=\"messageStart\" targetRef=\"eventTask\"/>"
        + "<userTask id=\"eventTask\"/><sequenceFlow id=\"eventFinish\" sourceRef=\"eventTask\" targetRef=\"eventEnd\"/>"
        + "<endEvent id=\"eventEnd\"/></subProcess>";
    String xml = model("${false}", children).replace("<process", "<message id=\"notice\" name=\"notice\"/><process");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    String scope = scope(process);
    String subscription = runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId())
        .eventName("notice").singleResult().getId();
    String taskId = null;
    if (activeBeforeMigration) {
      runtimeService.createMessageCorrelation("notice").processInstanceId(process.getId()).correlate();
      taskId = task(process.getId(), "eventTask").getId();
    }
    migrate(runtimeService.createMigrationPlan(source.getId(), target.getId()).mapEqualActivities().build(), process);
    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId()).eventName("notice")
        .singleResult().getId()).isEqualTo(subscription);
    if (activeBeforeMigration) {
      assertThat(task(process.getId(), "eventTask").getId()).isEqualTo(taskId);
    }

    runtimeService.createMessageCorrelation("notice").processInstanceId(process.getId()).correlate();

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(process.getId()).taskDefinitionKey("eventTask").list();
    assertThat(tasks).hasSize(activeBeforeMigration ? 2 : 1);
    tasks.forEach(task -> taskService.complete(task.getId()));
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveCompensationOwnershipOrRejectUnmappedSubscriptionAtomically(boolean mapCompensation) {
    String children = "<userTask id=\"taskA\"/><boundaryEvent id=\"compensationBoundary\" attachedToRef=\"taskA\">"
        + "<compensateEventDefinition/></boundaryEvent><userTask id=\"compensationHandler\" isForCompensation=\"true\"/>"
        + "<association id=\"compensationAssociation\" sourceRef=\"compensationBoundary\" targetRef=\"compensationHandler\"/>";
    String xml = model("", children).replace(
        "<sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"end\"/>",
        "<sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"compensate\"/>"
        + "<intermediateThrowEvent id=\"compensate\"><compensateEventDefinition waitForCompletion=\"true\"/>"
        + "</intermediateThrowEvent><sequenceFlow id=\"compensated\" sourceRef=\"compensate\" targetRef=\"end\"/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    runtimeService.triggerAdHocActivities(scope(process), List.of("taskA"), null);
    taskService.complete(task(process.getId(), "taskA").getId());
    Task afterTask = task(process.getId(), "after");
    List<String> subscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId())
        .eventType("compensate").list().stream().map(EventSubscription::getId).toList();
    assertThat(subscriptions).isNotEmpty();
    var builder = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("after", "after");
    if (mapCompensation) {
      builder.mapActivities("compensationBoundary", "compensationBoundary");
    }
    MigrationPlan plan = builder.build();

    if (mapCompensation) {
      migrate(plan, process);
    } else {
      assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
          .hasMessageContaining("compensation");
      assertDefinition(process, source);
    }

    assertThat(task(process.getId(), "after").getId()).isEqualTo(afterTask.getId());
    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId()).eventType("compensate").list())
        .extracting(EventSubscription::getId).containsExactlyInAnyOrderElementsOf(subscriptions);
    taskService.complete(afterTask.getId());
    Task handler = task(process.getId(), "compensationHandler");
    taskService.complete(handler.getId());
    testHelper.assertProcessEnded(process.getId());
  }

  @Test
  void migrateEnabledAndRunningActivitiesThroughAsyncBatch() {
    String xml = model("${false}", "<userTask id=\"taskA\"/><userTask id=\"taskB\"/><userTask id=\"taskC\"/>"
        + "<sequenceFlow id=\"enableB\" sourceRef=\"taskA\" targetRef=\"taskB\"/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("taskB", "renamedB"));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId());
    String scope = scope(process);
    runtimeService.triggerAdHocActivities(scope, List.of("taskA", "taskC"), null);
    taskService.complete(task(process.getId(), "taskA").getId());
    String runningTask = task(process.getId(), "taskC").getId();
    String enabledExecution = runtimeService.createExecutionQuery().processInstanceId(process.getId())
        .activityId("taskB").singleResult().getId();
    runtimeService.setVariableLocal(enabledExecution, "enabledLocal", "retained");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapEqualActivities().mapActivities("taskB", "renamedB").build();
    Batch batch = runtimeService.newMigration(plan).processInstanceIds(process.getId()).executeAsync();

    try {
      Job seed = engine.getManagementService().createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();
      engine.getManagementService().executeJob(seed.getId());
      List<Job> executionJobs = engine.getManagementService().createJobQuery()
          .jobDefinitionId(batch.getBatchJobDefinitionId()).list();
      assertThat(executionJobs).isNotEmpty();
      executionJobs.forEach(job -> engine.getManagementService().executeJob(job.getId()));
      Job monitor = engine.getManagementService().createJobQuery().jobDefinitionId(batch.getMonitorJobDefinitionId()).singleResult();
      engine.getManagementService().executeJob(monitor.getId());
      assertThat(engine.getManagementService().createBatchQuery().batchId(batch.getId()).count()).isZero();
      assertDefinition(process, target);
      assertThat(task(process.getId(), "taskC").getId()).isEqualTo(runningTask);
      assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId("renamedB")
          .singleResult().getId()).isEqualTo(enabledExecution);
      assertThat(runtimeService.getVariableLocal(enabledExecution, "enabledLocal")).isEqualTo("retained");
      assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
          .contains("renamedB");
      runtimeService.triggerAdHocActivities(scope, List.of("renamedB"), null);
      taskService.complete(task(process.getId(), "renamedB").getId());
      taskService.complete(runningTask);
      runtimeService.completeAdHocSubProcess(scope);
      finish(process);
    } finally {
      if (engine.getManagementService().createBatchQuery().batchId(batch.getId()).count() > 0) {
        engine.getManagementService().deleteBatch(batch.getId(), true);
      }
      if (engine.getHistoryService().createHistoricBatchQuery().batchId(batch.getId()).count() > 0) {
        engine.getHistoryService().deleteHistoricBatch(batch.getId());
      }
    }
  }

  private String model(String condition, String children) {
    return definitions("<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/><adHocSubProcess id=\"adhoc\">"
        + children + (condition.isEmpty() ? "" : "<completionCondition>" + condition + "</completionCondition>")
        + "</adHocSubProcess><sequenceFlow id=\"leave\" sourceRef=\"adhoc\" targetRef=\"after\"/>"
        + "<userTask id=\"after\"/><sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"end\"/>"
        + "<endEvent id=\"end\"/></process>");
  }

  private String definitions(String process) {
    return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"adhoc-integration-migration\">"
        + process + "</definitions>";
  }

  private ProcessDefinition deploy(String xml) {
    return testHelper.deployAndGetDefinition(Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  private String scope(ProcessInstance process) {
    Execution execution = runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").singleResult();
    assertThat(execution).isNotNull();
    return execution.getId();
  }

  private Task task(String processId, String activity) {
    Task task = taskService.createTaskQuery().processInstanceId(processId).taskDefinitionKey(activity).singleResult();
    assertThat(task).isNotNull();
    return task;
  }

  private void migrate(MigrationPlan plan, ProcessInstance process) {
    runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute();
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult()
        .getProcessDefinitionId()).isEqualTo(plan.getTargetProcessDefinitionId());
  }

  private void assertDefinition(ProcessInstance process, ProcessDefinition definition) {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult()
        .getProcessDefinitionId()).isEqualTo(definition.getId());
  }

  private void finish(ProcessInstance process) {
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactly("after");
    taskService.complete(task(process.getId(), "after").getId());
    testHelper.assertProcessEnded(process.getId());
  }
}
