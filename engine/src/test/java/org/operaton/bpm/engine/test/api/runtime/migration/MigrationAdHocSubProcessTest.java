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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.migration.MigrationPlanValidationException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_ACTIVE_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETED_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETION_CONDITION_SATISFIED;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_LAST_COMPLETED_ACTIVITY_ID;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationAdHocSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(engine);

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @BeforeEach
  void services() {
    runtimeService = engine.getRuntimeService();
    taskService = engine.getTaskService();
    managementService = engine.getManagementService();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateEmptyScopeAndUseTargetCommands(boolean renamed) {
    ProcessDefinition source = deploy(model("", "", tasks()));
    String targetId = renamed ? "renamed" : "adhoc";
    String targetModel = model("", "", tasks() + "<userTask id=\"taskC\"/>").replace("adhoc", targetId);
    ProcessDefinition target = deploy(targetModel);
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    runtimeService.setVariableLocal(scopeId, "scopeVariable", "retained");
    runtimeService.setVariable(process.getId(), "processVariable", "retained");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", targetId).build();

    migrate(plan, process);

    assertThat(scope(process, targetId)).isEqualTo(scopeId);
    assertThat(runtimeService.getVariableLocal(scopeId, "scopeVariable")).isEqualTo("retained");
    assertThat(runtimeService.getVariable(process.getId(), "processVariable")).isEqualTo("retained");
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).extracting(AdHocActivity::getActivityId)
        .containsExactlyInAnyOrder("taskA", "taskB", "taskC");
    trigger(scopeId, "taskC");
    complete(process, "taskC");
    assertAfterAndFinish(process);
  }

  @Test
  void generateEqualActivityMappingsForAdHocScopes() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()));
    MigrationPlan plan = equalPlan(source, target);
    assertThat(plan.getInstructions()).anySatisfy(instruction -> {
      assertThat(instruction.getSourceActivityId()).isEqualTo("adhoc");
      assertThat(instruction.getTargetActivityId()).isEqualTo("adhoc");
    });
    ProcessInstance process = start(source);
    migrate(plan, process);
    runtimeService.completeAdHocSubProcess(scope(process, "adhoc"));
    assertAfterAndFinish(process);
  }

  @Test
  void migrateBeforeEntryAndEnterChangedTargetAdHocScope() {
    String sourceXml = model("", "", tasks()).replace(
        "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/>",
        "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"before\"/>"
        + "<userTask id=\"before\"/><sequenceFlow id=\"toAdHoc\" sourceRef=\"before\" targetRef=\"adhoc\"/>");
    ProcessDefinition source = deploy(sourceXml);
    ProcessDefinition target = deploy(sourceXml.replace("adhoc", "renamed"));
    ProcessInstance process = start(source);
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("before", "before").build();
    migrate(plan, process);
    complete(process, "before");
    String scopeId = scope(process, "renamed");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @Test
  void migrateAfterScopeCompletedWithoutAdHocMapping() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()).replace("adhoc", "renamed"));
    ProcessInstance process = start(source);
    runtimeService.completeAdHocSubProcess(scope(process, "adhoc"));
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("after", "after").build();
    migrate(plan, process);
    assertAfterAndFinish(process);
  }

  @Test
  void migrateActiveChildrenAndRepeatedCompletionContextToRenamedActivities() {
    String xml = model("cancelRemainingInstances=\"false\"", "${nrOfCompletedAdHocActivities >= 3}", tasks());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("adhoc", "renamed").replace("taskA", "renamedA")
        .replace("taskB", "renamedB"));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    for (int i = 0; i < 2; i++) {
      trigger(scopeId, "taskA");
      complete(process, "taskA");
    }
    trigger(scopeId, "taskB");
    Task before = task(process, "taskB");
    runtimeService.setVariableLocal(before.getExecutionId(), "childVariable", 42);
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "renamed").mapActivities("taskA", "renamedA")
        .mapActivities("taskB", "renamedB").build();

    migrate(plan, process);

    assertThat(scope(process, "renamed")).isEqualTo(scopeId);
    Task after = task(process, "renamedB");
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(runtimeService.getVariableLocal(after.getExecutionId(), "childVariable")).isEqualTo(42);
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETED_ACTIVITY_IDS))
        .isEqualTo(List.of("renamedA", "renamedA"));
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_ACTIVE_ACTIVITY_IDS)).isEqualTo(List.of("renamedB"));
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_LAST_COMPLETED_ACTIVITY_ID)).isEqualTo("renamedA");
    assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(2);
    complete(process, "renamedB");
    assertAfterAndFinish(process);
  }

  @Test
  void keepUnmappedCompletedActivityAsHistoricalId() {
    String condition = "${nrOfCompletedAdHocActivities >= 2}";
    ProcessDefinition source = deploy(model("", condition, tasks()));
    ProcessDefinition target = deploy(model("", condition, "<userTask id=\"taskB\"/>"));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    migrate(equalPlan(source, target), process);
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETED_ACTIVITY_IDS)).isEqualTo(List.of("taskA"));
    trigger(scopeId, "taskB");
    complete(process, "taskB");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveCompletionLatchWhileDrainingEvenWhenTargetConditionChanges(boolean removeCondition) {
    ProcessDefinition source = deploy(model("cancelRemainingInstances=\"false\"", "${done}", tasks()));
    ProcessDefinition target = deploy(model("cancelRemainingInstances=\"false\"", removeCondition ? "" : "${false}", tasks()));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB");
    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskA");
    runtimeService.setVariable(process.getId(), "done", false);
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETION_CONDITION_SATISFIED)).isEqualTo(true);

    migrate(equalPlan(source, target), process);

    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETION_CONDITION_SATISFIED)).isEqualTo(true);
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).isEmpty();
    assertThatThrownBy(() -> trigger(scopeId, "taskA")).isInstanceOf(BadUserRequestException.class);
    complete(process, "taskB");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void allowParallelToSequentialWhenAtMostOneChildIsOpen(int childCount) {
    ProcessDefinition source = deploy(model("ordering=\"Parallel\"", "", tasks()));
    ProcessDefinition target = deploy(model("ordering=\"Sequential\"", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    if (childCount == 1) {
      trigger(scopeId, "taskA");
    }
    migrate(equalPlan(source, target), process);
    if (childCount == 0) {
      trigger(scopeId, "taskA");
    }
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).isEmpty();
    assertThatThrownBy(() -> trigger(scopeId, "taskB")).isInstanceOf(BadUserRequestException.class);
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @Test
  void allowSequentialToParallelAndStartAdditionalChild() {
    ProcessDefinition source = deploy(model("ordering=\"Sequential\"", "", tasks()));
    ProcessDefinition target = deploy(model("ordering=\"Parallel\"", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    migrate(equalPlan(source, target), process);
    trigger(scopeId, "taskB");
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isEqualTo(2);
    complete(process, "taskA");
    complete(process, "taskB");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectParallelToSequentialAtomicallyWithTwoOpenChildren(boolean asyncBefore) {
    String children = asyncBefore ? tasks().replace("<userTask", "<userTask operaton:asyncBefore=\"true\"") : tasks();
    ProcessDefinition source = deploy(model("", "${false}", children));
    ProcessDefinition target = deploy(model("ordering=\"Sequential\"", "${false}", children));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB");
    runtimeService.setVariableLocal(scopeId, "marker", 19);
    List<String> taskIds = taskService.createTaskQuery().processInstanceId(process.getId()).list().stream().map(Task::getId).toList();
    List<String> jobIds = managementService.createJobQuery().processInstanceId(process.getId()).list().stream().map(Job::getId).toList();
    MigrationPlan plan = equalPlan(source, target);

    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("sequential ad-hoc subprocess");

    assertSourceState(process, source, scopeId);
    assertThat(runtimeService.getVariableLocal(scopeId, "marker")).isEqualTo(19);
    assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list()).extracting(Task::getId)
        .containsExactlyInAnyOrderElementsOf(taskIds);
    assertThat(managementService.createJobQuery().processInstanceId(process.getId()).list()).extracting(Job::getId)
        .containsExactlyInAnyOrderElementsOf(jobIds);
    executeJobs(process);
    complete(process, "taskA");
    complete(process, "taskB");
    runtimeService.completeAdHocSubProcess(scopeId);
    assertAfterAndFinish(process);
  }

  @Test
  void rejectMissingMappingForAnEmptyEnteredScope() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId()).build();
    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("entered ad-hoc subprocess");
    assertSourceState(process, source, scopeId);
  }

  @Test
  void rejectMissingMappingForAnActiveChild() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").build();
    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("no migration instruction");
    assertSourceState(process, source, scopeId);
    assertThat(task(process, "taskA")).isNotNull();
  }

  @Test
  void rejectImplicitRecreationOfAnIdleAdHocOwnerWithoutMapping() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()));
    ProcessInstance process = start(source);
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("taskA", "taskA").build();
    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("idle entered ad-hoc subprocess");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void allowPlanForStateSensitiveAdHocAndOrdinarySubprocessConversion(boolean toAdHoc) {
    String adhoc = model("", "", tasks());
    String ordinary = adhoc.replace("adHocSubProcess", "subProcess")
        .replace("<userTask id=\"taskA\"/>", "<startEvent id=\"innerStart\"/>"
            + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"taskA\"/><userTask id=\"taskA\"/>");
    ProcessDefinition source = deploy(toAdHoc ? ordinary : adhoc);
    ProcessDefinition target = deploy(toAdHoc ? adhoc : ordinary);
    assertThat(runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").build().getInstructions()).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateNestedNormalAndAdHocChildScopes(boolean nestedAdHoc) {
    String inner = nestedAdHoc
        ? "<adHocSubProcess id=\"inner\"><userTask id=\"nestedTask\"/></adHocSubProcess>"
        : "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
          + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"nestedTask\"/>"
          + "<userTask id=\"nestedTask\"/></subProcess>";
    String xml = model("", "", inner);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), "inner");
    if (nestedAdHoc) {
      trigger(scope(process, "inner"), "nestedTask");
    }
    Task before = task(process, "nestedTask");
    migrate(equalPlan(source, target), process);
    assertThat(task(process, "nestedTask").getId()).isEqualTo(before.getId());
    complete(process, "nestedTask");
    assertAfterAndFinish(process);
  }

  @Test
  void migrateAdHocNestedInOrdinarySubprocess() {
    String xml = model("", "", tasks()).replace("<startEvent id=\"start\"/>",
        "<startEvent id=\"outerStart\"/><sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"outer\"/>"
        + "<subProcess id=\"outer\"><startEvent id=\"start\"/>")
        .replace("</process>", "</subProcess></process>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("adhoc", "renamed"));
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("outer", "outer").mapActivities("adhoc", "renamed")
        .mapActivities("taskA", "taskA").build();
    migrate(plan, process);
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateMultiInstanceAdHocScopesAndContinueRemainingIterations(boolean sequential) {
    String xml = model("", "", multiInstance(sequential) + tasks());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    List<Execution> scopes = runtimeService.createExecutionQuery().processInstanceId(process.getId())
        .activityId("adhoc").list();
    assertThat(scopes).hasSize(sequential ? 1 : 2);
    for (Execution scope : scopes) {
      trigger(scope.getId(), "taskA");
    }
    List<String> taskIds = taskService.createTaskQuery().processInstanceId(process.getId()).list().stream().map(Task::getId).toList();

    migrate(equalPlan(source, target), process);

    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list()).extracting(Task::getId)
        .containsExactlyInAnyOrderElementsOf(taskIds);
    for (String id : taskIds) {
      taskService.complete(id);
    }
    if (sequential) {
      String secondScope = scope(process, "adhoc");
      assertThat(runtimeService.getVariable(secondScope, "loopCounter")).isEqualTo(1);
      trigger(secondScope, "taskB");
      complete(process, "taskB");
    }
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateMultiInstanceChildBodyAndInstances(boolean sequential) {
    String children = "<userTask id=\"taskA\">" + multiInstance(sequential) + "</userTask>";
    ProcessDefinition source = deploy(model("ordering=\"Sequential\"", "", children));
    ProcessDefinition target = deploy(model("ordering=\"Sequential\"", "", children));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    migrate(equalPlan(source, target), process);
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).isEmpty();
    complete(process, "taskA");
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateAdHocScopeAsyncTransition(boolean asyncAfter) {
    String attribute = asyncAfter ? "operaton:asyncAfter=\"true\"" : "operaton:asyncBefore=\"true\"";
    ProcessDefinition source = deploy(model(attribute, "", tasks()));
    ProcessDefinition target = deploy(model(attribute, "", tasks()).replace("adhoc", "renamed"));
    ProcessInstance process = start(source);
    if (asyncAfter) {
      runtimeService.completeAdHocSubProcess(scope(process, "adhoc"));
    }
    Job job = singleJob(process);
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "renamed").build();
    migrate(plan, process);
    assertThat(singleJob(process).getId()).isEqualTo(job.getId());
    managementService.executeJob(job.getId());
    if (!asyncAfter) {
      trigger(scope(process, "renamed"), "taskA");
      complete(process, "taskA");
    }
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateAsyncChildJobAndCompleteExactlyOnce(boolean asyncAfter) {
    String attribute = asyncAfter ? "operaton:asyncAfter=\"true\"" : "operaton:asyncBefore=\"true\"";
    String xml = model("", "${nrOfCompletedAdHocActivities >= 1}", "<userTask id=\"taskA\" " + attribute + "/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    if (asyncAfter) {
      complete(process, "taskA");
    }
    Job before = singleJob(process);
    migrate(equalPlan(source, target), process);
    assertThat(singleJob(process).getId()).isEqualTo(before.getId());
    managementService.executeJob(before.getId());
    if (!asyncAfter) {
      complete(process, "taskA");
    }
    assertAfterAndFinish(process);
  }

  @Test
  void migrateTimerBoundaryJobAndKeepItsIdentityAndDueDate() {
    String xml = model("", "", tasks()).replace("</process>",
        "<boundaryEvent id=\"timeout\" attachedToRef=\"adhoc\"><timerEventDefinition>"
        + "<timeDuration>PT1H</timeDuration></timerEventDefinition></boundaryEvent>"
        + "<sequenceFlow id=\"timeoutFlow\" sourceRef=\"timeout\" targetRef=\"after\"/></process>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), "taskA");
    Job before = singleJob(process);
    migrate(equalPlan(source, target), process);
    Job after = singleJob(process);
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.getDuedate()).isEqualTo(before.getDuedate());
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
    managementService.executeJob(after.getId());
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).taskDefinitionKey("taskA").count()).isZero();
    assertAfterAndFinish(process);
  }

  @Test
  void migrateMessageBoundarySubscriptionAndCorrelateAfterwards() {
    String xml = model("", "", tasks()).replace("<process", "<message id=\"message\" name=\"continue\"/><process")
        .replace("</process>", "<boundaryEvent id=\"messageBoundary\" attachedToRef=\"adhoc\">"
        + "<messageEventDefinition messageRef=\"message\"/></boundaryEvent>"
        + "<sequenceFlow id=\"messageFlow\" sourceRef=\"messageBoundary\" targetRef=\"after\"/></process>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), "taskA");
    String subscriptionId = runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId())
        .singleResult().getId();
    migrate(equalPlan(source, target), process);
    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(process.getId()).singleResult().getId())
        .isEqualTo(subscriptionId);
    runtimeService.createMessageCorrelation("continue").processInstanceId(process.getId()).correlate();
    assertAfterAndFinish(process);
  }

  @Test
  void rollBackEarlierInstanceWhenLaterInstanceCannotMigrate() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("ordering=\"Sequential\"", "", tasks()));
    ProcessInstance first = start(source);
    ProcessInstance second = start(source);
    String firstScope = scope(first, "adhoc");
    String secondScope = scope(second, "adhoc");
    trigger(secondScope, "taskA", "taskB");
    MigrationPlan plan = equalPlan(source, target);

    assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(first.getId(), second.getId()).execute())
        .isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("sequential ad-hoc subprocess");

    assertSourceState(first, source, firstScope);
    assertSourceState(second, source, secondScope);
    trigger(firstScope, "taskA", "taskB");
    assertThat(taskService.createTaskQuery().processInstanceId(first.getId()).count()).isEqualTo(2);
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void preserveHistoryIdentityAndLeaveFinishedActivityHistoryOnSourceDefinition() {
    String xml = model("", "${false}", tasks());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("adhoc", "renamed").replace("taskB", "renamedB"));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    trigger(scopeId, "taskB");
    HistoricActivityInstance before = engine.getHistoryService().createHistoricActivityInstanceQuery()
        .processInstanceId(process.getId()).activityId("adhoc").singleResult();
    String taskId = task(process, "taskB").getId();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "renamed").mapActivities("taskB", "renamedB").build();

    migrate(plan, process);

    HistoricActivityInstance after = engine.getHistoryService().createHistoricActivityInstanceQuery()
        .processInstanceId(process.getId()).activityId("renamed").singleResult();
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.getStartTime()).isEqualTo(before.getStartTime());
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(after.getEndTime()).isNull();
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(process.getId())
        .activityId("taskA").singleResult().getProcessDefinitionId()).isEqualTo(source.getId());
    assertThat(engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(taskId).singleResult()
        .getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(engine.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(process.getId())
        .variableName(NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES).singleResult().getValue()).isEqualTo(1);
    complete(process, "renamedB");
    runtimeService.completeAdHocSubProcess(scopeId);
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateAddingOrRemovingOrdinaryScopeInsideAdHoc(boolean addScope) {
    String wrapped = "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"taskA\"/>"
        + "<userTask id=\"taskA\"/></subProcess>";
    ProcessDefinition source = deploy(model("", "${false}", addScope ? "<userTask id=\"taskA\"/>" : wrapped));
    ProcessDefinition target = deploy(model("", "${false}", addScope ? wrapped : "<userTask id=\"taskA\"/>"));
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), addScope ? "taskA" : "inner");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").build();
    migrate(plan, process);
    String scopeId = scope(process, "adhoc");
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_ACTIVE_ACTIVITY_IDS))
        .isEqualTo(List.of(addScope ? "inner" : "taskA"));
    complete(process, "taskA");
    runtimeService.completeAdHocSubProcess(scopeId);
    assertAfterAndFinish(process);
  }

  @Test
  void rejectSequentialTargetWhenRemovingNormalScopeWouldExposeTwoChildren() {
    String wrapped = "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"fork\"/>"
        + "<parallelGateway id=\"fork\"/><sequenceFlow id=\"a\" sourceRef=\"fork\" targetRef=\"taskA\"/>"
        + "<sequenceFlow id=\"b\" sourceRef=\"fork\" targetRef=\"taskB\"/>" + tasks() + "</subProcess>";
    ProcessDefinition source = deploy(model("", "", wrapped));
    ProcessDefinition target = deploy(model("ordering=\"Sequential\"", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "inner");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").build();
    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("sequential ad-hoc subprocess");
    assertSourceState(process, source, scopeId);
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isEqualTo(2);
  }

  @Test
  void addFreshAdHocAncestorAroundRunningActivityWhilePreservingOuterOwner() {
    ProcessDefinition source = deploy(model("", "", "<userTask id=\"taskA\"/>"));
    ProcessDefinition target = deploy(model("", "", "<adHocSubProcess id=\"inner\">"
        + "<userTask id=\"taskA\"/></adHocSubProcess>"));
    ProcessInstance process = start(source);
    String outer = scope(process, "adhoc");
    trigger(outer, "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").build();

    migrate(plan, process);

    assertThat(scope(process, "adhoc")).isEqualTo(outer);
    assertThat(runtimeService.getVariableLocal(scope(process, "inner"), NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(0);
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @Test
  void rejectMigrationOfEndedProcessInstance() {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model("", "", tasks()));
    ProcessInstance process = start(source);
    runtimeService.completeAdHocSubProcess(scope(process, "adhoc"));
    assertAfterAndFinish(process);
    MigrationPlan plan = equalPlan(source, target);
    assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute())
        .isInstanceOf(ProcessEngineException.class);
  }


  @Test
  void preserveInputVariablesWithoutReplayingMappingsAndRunTargetOutputMappingOnCompletion() {
    String mappings = "<extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"scopeInput\">${input}</operaton:inputParameter>"
        + "<operaton:outputParameter name=\"result\">${scopeInput}</operaton:outputParameter>"
        + "</operaton:inputOutput></extensionElements>";
    String xml = model("", "", mappings + tasks());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("${input}", "${changedInput}"));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(),
        Map.of("input", "original", "changedInput", "changed"));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");

    migrate(equalPlan(source, target), process);

    assertThat(runtimeService.getVariableLocal(scopeId, "scopeInput")).isEqualTo("original");
    assertThat(runtimeService.getVariables(process.getId())).doesNotContainKey("result");
    complete(process, "taskA");
    assertThat(runtimeService.getVariable(process.getId(), "result")).isEqualTo("original");
    assertAfterAndFinish(process);
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveRepeatedConcurrentActivationsAndRejectSequentialTarget(boolean sequentialTarget) {
    ProcessDefinition source = deploy(model("", "", tasks()));
    ProcessDefinition target = deploy(model(sequentialTarget ? "ordering=\"Sequential\"" : "", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    trigger(scopeId, "taskA");
    List<String> ids = taskService.createTaskQuery().processInstanceId(process.getId()).list().stream()
        .map(Task::getId).toList();
    assertThat(ids).hasSize(2);
    MigrationPlan plan = equalPlan(source, target);
    if (sequentialTarget) {
      assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
          .hasMessageContaining("sequential ad-hoc subprocess");
      assertSourceState(process, source, scopeId);
    } else {
      migrate(plan, process);
    }
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list()).extracting(Task::getId)
        .containsExactlyInAnyOrderElementsOf(ids);
    complete(process, "taskA");
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }

  @Test
  void explicitlyCompleteMigratedScopeAndCancelItsActiveChildren() {
    ProcessDefinition source = deploy(model("cancelRemainingInstances=\"true\"", "", tasks()));
    ProcessDefinition target = deploy(model("cancelRemainingInstances=\"true\"", "", tasks()));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB");
    migrate(equalPlan(source, target), process);
    runtimeService.completeAdHocSubProcess(scopeId);
    assertAfterAndFinish(process);
  }

  @Test
  void rejectAsyncChildMigrationWhenTargetContinuationIsMissing() {
    ProcessDefinition source = deploy(model("", "", "<userTask id=\"taskA\" operaton:asyncBefore=\"true\"/>"));
    ProcessDefinition target = deploy(model("", "", "<userTask id=\"taskA\"/>"));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    Job before = singleJob(process);
    MigrationPlan plan = equalPlan(source, target);
    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class);
    assertSourceState(process, source, scopeId);
    assertThat(singleJob(process).getId()).isEqualTo(before.getId());
    managementService.executeJob(before.getId());
    complete(process, "taskA");
    assertAfterAndFinish(process);
  }


  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void preservePendingActivityEndBeforeOneOrTwoOutgoingFlows(int outgoingCount) {
    String xml = model("", "${nrOfCompletedAdHocActivities >= 1}", asyncFlowChildren(outgoingCount));
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job job = singleJob(process);
    assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(0);

    migrate(equalPlan(source, target), process);
    managementService.executeJob(job.getId());

    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void selectTargetFlowsAfterPendingActivityEndWhenConditionChangesOrIsRemoved(boolean removeCondition) {
    ProcessDefinition source = deploy(model("", "${nrOfCompletedAdHocActivities >= 1}", asyncFlowChildren(2)));
    ProcessDefinition target = deploy(model("", removeCondition ? "" : "${false}", asyncFlowChildren(2)));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job job = singleJob(process);

    migrate(equalPlan(source, target), process);
    managementService.executeJob(job.getId());

    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).extracting(AdHocActivity::getActivityId)
        .contains("taskB", "taskC");
    trigger(scopeId, "taskB", "taskC");
    complete(process, "taskB");
    complete(process, "taskC");
    if (!removeCondition) {
      assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(3);
      runtimeService.completeAdHocSubProcess(scopeId);
    }
    assertAfterAndFinish(process);
  }

  @Test
  void addingConditionDoesNotReplayAlreadyCompletedTransitionSelectedActivity() {
    ProcessDefinition source = deploy(model("", "", asyncFlowChildren(1)));
    ProcessDefinition target = deploy(model("", "${nrOfCompletedAdHocActivities >= 1}", asyncFlowChildren(1)));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job job = singleJob(process);

    migrate(equalPlan(source, target), process);
    managementService.executeJob(job.getId());

    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();
    assertThat(runtimeService.getStartableAdHocActivities(scopeId)).extracting(AdHocActivity::getActivityId)
        .contains("taskB");
    assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isNull();
    trigger(scopeId, "taskB");
    complete(process, "taskB");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void retainedLatchSuppressesOutgoingFlowAfterTargetConditionRemoval(boolean asyncAfter) {
    String children = "<userTask id=\"taskA\"/><userTask id=\"taskB\" "
        + (asyncAfter ? "operaton:asyncAfter=\"true\"" : "") + "/>"
        + "<userTask id=\"taskC\"/><sequenceFlow id=\"next\" sourceRef=\"taskB\" targetRef=\"taskC\"/>";
    ProcessDefinition source = deploy(model("cancelRemainingInstances=\"false\"", "${done}", children));
    ProcessDefinition target = deploy(model("cancelRemainingInstances=\"false\"", "", children));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB");
    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskA");
    if (asyncAfter) {
      complete(process, "taskB");
    }

    migrate(equalPlan(source, target), process);

    if (asyncAfter) {
      managementService.executeJob(singleJob(process).getId());
    } else {
      complete(process, "taskB");
    }
    assertAfterAndFinish(process);
  }

  private String asyncFlowChildren(int outgoingCount) {
    return "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"/><userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"nextB\" sourceRef=\"taskA\" targetRef=\"taskB\"/>"
        + (outgoingCount == 2 ? "<userTask id=\"taskC\"/>"
            + "<sequenceFlow id=\"nextC\" sourceRef=\"taskA\" targetRef=\"taskC\"/>" : "");
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migratePendingScopedActivityEndWithLocalVariablesAndOutputMappings(boolean rename) {
    String children = scopedAsyncTask() + "<userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
    String xml = model("", "${nrOfCompletedAdHocActivities >= 1}", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(rename ? xml.replace("taskA", "renamedA") : xml);
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("input", "retained"));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job before = singleJob(process);
    assertThat(runtimeService.getVariableLocal(before.getExecutionId(), "localInput")).isEqualTo("retained");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", rename ? "renamedA" : "taskA").build();

    migrate(plan, process);

    assertThat(singleJob(process).getId()).isEqualTo(before.getId());
    assertThat(runtimeService.getVariableLocal(before.getExecutionId(), "localInput")).isEqualTo("retained");
    assertThat(runtimeService.getVariables(process.getId())).doesNotContainKey("mappedOutput");
    managementService.executeJob(before.getId());
    assertThat(runtimeService.getVariable(process.getId(), "mappedOutput")).isEqualTo("retained");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(strings = {"remove", "add"})
  void changePendingActivityEndScopeUsingTargetModelMappings(String change) {
    boolean addScope = "add".equals(change);
    String plainTask = "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"/>";
    String sourceChildren = addScope ? plainTask : scopedAsyncTask();
    String targetChildren = addScope
        ? scopedAsyncTask().replace("${input}", "${inputMustNotBeReplayed}").replace("${localInput}", "${input}")
        : plainTask;
    ProcessDefinition source = deploy(model("", "${nrOfCompletedAdHocActivities >= 1}", sourceChildren));
    ProcessDefinition target = deploy(model("", "${nrOfCompletedAdHocActivities >= 1}", targetChildren));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("input", "retained"));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job before = singleJob(process);
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").build();

    migrate(plan, process);

    Job after = singleJob(process);
    assertThat(after.getId()).isEqualTo(before.getId());
    if (addScope) {
      assertThat(runtimeService.getVariableLocal(after.getExecutionId(), "localInput")).isNull();
    } else {
      assertThat(runtimeService.getVariableLocal(after.getExecutionId(), "localInput")).isEqualTo("retained");
    }
    assertThat(runtimeService.getVariables(process.getId())).doesNotContainKey("mappedOutput");
    managementService.executeJob(after.getId());
    if (addScope) {
      assertThat(runtimeService.getVariable(process.getId(), "mappedOutput")).isEqualTo("retained");
    } else {
      assertThat(runtimeService.getVariables(process.getId())).doesNotContainKey("mappedOutput");
    }
    assertAfterAndFinish(process);
  }

  private String scopedAsyncTask() {
    return "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"localInput\">${input}</operaton:inputParameter>"
        + "<operaton:outputParameter name=\"mappedOutput\">${localInput}</operaton:outputParameter>"
        + "</operaton:inputOutput></extensionElements></userTask>";
  }


  @Test
  void preserveAdHocScopeWhenReattachingOneOfTwoOpenChildren() {
    ProcessDefinition source = deploy(model("", "${false}", tasks()));
    String targetChildren = "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"taskA\"/>"
        + "<userTask id=\"taskA\"/></subProcess><userTask id=\"taskB\"/>";
    ProcessDefinition target = deploy(model("", "${false}", targetChildren));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    runtimeService.setVariableLocal(scopeId, "scopeMarker", "kept");
    trigger(scopeId, "taskA", "taskB");
    runtimeService.setVariableLocal(task(process, "taskA").getExecutionId(), "childMarker", "kept");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").build();

    migrate(plan, process);

    assertThat(scope(process, "adhoc")).isEqualTo(scopeId);
    assertThat(runtimeService.getVariableLocal(scopeId, "scopeMarker")).isEqualTo("kept");
    assertThat(runtimeService.getVariableLocal(task(process, "taskA").getExecutionId(), "childMarker")).isEqualTo("kept");
    assertThat((List<String>) runtimeService.getVariableLocal(scopeId, AD_HOC_ACTIVE_ACTIVITY_IDS))
        .containsExactlyInAnyOrder("inner", "taskB");
    complete(process, "taskA");
    complete(process, "taskB");
    runtimeService.completeAdHocSubProcess(scopeId);
    assertAfterAndFinish(process);
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void useChangedTargetCancellationPolicyWhenCompletionIsFirstReached(boolean targetCancelRemaining) {
    ProcessDefinition source = deploy(model("cancelRemainingInstances=\"" + !targetCancelRemaining + "\"",
        "${done}", tasks()));
    ProcessDefinition target = deploy(model("cancelRemainingInstances=\"" + targetCancelRemaining + "\"",
        "${done}", tasks()));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB");

    migrate(equalPlan(source, target), process);
    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskA");

    if (!targetCancelRemaining) {
      assertThat(task(process, "taskB")).isNotNull();
      assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETION_CONDITION_SATISFIED)).isEqualTo(true);
      assertThat(runtimeService.getStartableAdHocActivities(scopeId)).isEmpty();
      complete(process, "taskB");
    }
    assertAfterAndFinish(process);
  }

  @Test
  void preserveExistingLatchAndApplyChangedCancellationPolicyOnNextContinuation() {
    String children = tasks() + "<userTask id=\"taskC\"/>";
    ProcessDefinition source = deploy(model("cancelRemainingInstances=\"false\"", "${done}", children));
    ProcessDefinition target = deploy(model("cancelRemainingInstances=\"true\"", "${false}", children));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA", "taskB", "taskC");
    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskA");

    migrate(equalPlan(source, target), process);

    // Migration does not eagerly reevaluate completion or cancel live children.
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskB", "taskC");
    assertThat(runtimeService.getVariableLocal(scopeId, AD_HOC_COMPLETION_CONDITION_SATISFIED)).isEqualTo(true);
    complete(process, "taskB");
    assertAfterAndFinish(process);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2})
  void preserveAsyncJobRetriesDueDatePriorityAndIncidentAcrossRename(int retries) {
    String children = "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"/>";
    String xml = model("", "${nrOfCompletedAdHocActivities >= 1}", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("taskA", "renamedA"));
    ProcessInstance process = start(source);
    String scopeId = scope(process, "adhoc");
    trigger(scopeId, "taskA");
    complete(process, "taskA");
    Job before = singleJob(process);
    Date dueDate = new Date(1000L);
    managementService.setJobDuedate(before.getId(), dueDate);
    managementService.setJobPriority(before.getId(), 47);
    managementService.setJobRetries(before.getId(), retries);
    Incident incident = runtimeService.createIncidentQuery().processInstanceId(process.getId()).singleResult();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "renamedA").build();

    migrate(plan, process);

    Job after = singleJob(process);
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.getRetries()).isEqualTo(retries);
    assertThat(after.getDuedate()).isEqualTo(dueDate);
    assertThat(after.getPriority()).isEqualTo(47);
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(runtimeService.getVariableLocal(scopeId, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(0);
    if (retries == 0) {
      assertThat(incident).isNotNull();
      Incident migrated = runtimeService.createIncidentQuery().incidentId(incident.getId()).singleResult();
      assertThat(migrated.getProcessDefinitionId()).isEqualTo(target.getId());
      assertThat(migrated.getActivityId()).isEqualTo("renamedA");
      assertThat(migrated.getIncidentTimestamp()).isEqualTo(incident.getIncidentTimestamp());
      assertThat(migrated.getConfiguration()).isEqualTo(before.getId());
      managementService.setJobRetries(after.getId(), 1);
      assertThat(runtimeService.createIncidentQuery().incidentId(incident.getId()).count()).isZero();
    } else {
      assertThat(incident).isNull();
      assertThat(runtimeService.createIncidentQuery().processInstanceId(process.getId()).count()).isZero();
    }
    managementService.executeJob(after.getId());
    assertAfterAndFinish(process);
  }

  private ProcessDefinition deploy(String xml) {
    return testHelper.deployAndGetDefinition(Bpmn.readModelFromStream(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  private String model(String attributes, String condition, String children) {
    return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"adhoc-migration\">"
        + "<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/>"
        + "<adHocSubProcess id=\"adhoc\" " + attributes + ">" + children
        + (condition.isEmpty() ? "" : "<completionCondition>" + condition + "</completionCondition>")
        + "</adHocSubProcess><sequenceFlow id=\"leave\" sourceRef=\"adhoc\" targetRef=\"after\"/>"
        + "<userTask id=\"after\"/><sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"end\"/>"
        + "<endEvent id=\"end\"/></process></definitions>";
  }

  private String tasks() {
    return "<userTask id=\"taskA\"/><userTask id=\"taskB\"/>";
  }

  private String multiInstance(boolean sequential) {
    return "<multiInstanceLoopCharacteristics isSequential=\"" + sequential + "\">"
        + "<loopCardinality>2</loopCardinality></multiInstanceLoopCharacteristics>";
  }

  private ProcessInstance start(ProcessDefinition definition) {
    return runtimeService.startProcessInstanceById(definition.getId());
  }

  private MigrationPlan equalPlan(ProcessDefinition source, ProcessDefinition target) {
    return runtimeService.createMigrationPlan(source.getId(), target.getId()).mapEqualActivities().build();
  }

  private void migrate(MigrationPlan plan, ProcessInstance process) {
    runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute();
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult()
        .getProcessDefinitionId()).isEqualTo(plan.getTargetProcessDefinitionId());
  }

  private String scope(ProcessInstance process, String activityId) {
    Execution execution = runtimeService.createExecutionQuery().processInstanceId(process.getId())
        .activityId(activityId).singleResult();
    assertThat(execution).isNotNull();
    return execution.getId();
  }

  private void trigger(String scopeId, String... activities) {
    runtimeService.triggerAdHocActivities(scopeId, List.of(activities), null);
  }

  private Task task(ProcessInstance process, String activityId) {
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(process.getId()).taskDefinitionKey(activityId).list();
    assertThat(tasks).isNotEmpty();
    return tasks.get(0);
  }

  private void complete(ProcessInstance process, String activityId) {
    taskService.complete(task(process, activityId).getId());
  }

  private Job singleJob(ProcessInstance process) {
    Job job = managementService.createJobQuery().processInstanceId(process.getId()).singleResult();
    assertThat(job).isNotNull();
    return job;
  }

  private void executeJobs(ProcessInstance process) {
    for (Job job : new ArrayList<>(managementService.createJobQuery().processInstanceId(process.getId()).list())) {
      managementService.executeJob(job.getId());
    }
  }

  private void assertSourceState(ProcessInstance process, ProcessDefinition source, String scopeId) {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult()
        .getProcessDefinitionId()).isEqualTo(source.getId());
    assertThat(scope(process, "adhoc")).isEqualTo(scopeId);
  }

  private void assertAfterAndFinish(ProcessInstance process) {
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactly("after");
    complete(process, "after");
    testHelper.assertProcessEnded(process.getId());
  }
}
