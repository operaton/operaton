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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability.AD_HOC_ENABLED_ACTIVITY;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_ACTIVE_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_ENABLED_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MigrationAdHocEnabledActivityTest {

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
  @ValueSource(strings = {"Parallel", "Sequential"})
  void preserveEnabledExecutionIdentityVariablesAndContextAcrossRename(String ordering) {
    String xml = model(ordering, "${false}", chain());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("adhoc", "renamed").replace("taskB", "renamedB"));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    String token = enabled(process, "taskB").get(0).getId();
    runtimeService.setVariableLocal(token, "tokenMarker", "preserved");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "renamed").mapActivities("taskA", "taskA")
        .mapActivities("taskB", "renamedB").build();

    migrate(plan, process);

    assertThat(scope(process, "renamed")).isEqualTo(scope);
    assertThat(enabled(process, "renamedB")).extracting(Execution::getId).containsExactly(token);
    assertThat(runtimeService.getVariableLocal(token, "tokenMarker")).isEqualTo("preserved");
    assertThat(runtimeService.getVariableLocal(scope, AD_HOC_ENABLED_ACTIVITY_IDS)).isEqualTo(List.of("renamedB"));
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES)).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, AD_HOC_ACTIVE_ACTIVITY_IDS)).isEqualTo(List.of());
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(1);
    assertNoJobs(process);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .contains("renamedB");
    trigger(scope, "renamedB");
    assertThat(runtimeService.getVariableLocal(task(process, "renamedB").getExecutionId(), "tokenMarker"))
        .isEqualTo("preserved");
    complete(process, "renamedB");
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @Test
  void migrateOneRunningAndOneEnabledChildToSequentialOrdering() {
    String children = chain() + "<userTask id=\"taskC\"/>";
    ProcessDefinition source = deploy(model("Parallel", "${false}", children));
    ProcessDefinition target = deploy(model("Sequential", "${false}", children));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC");
    complete(process, "taskA");

    migrate(equalPlan(source, target), process);

    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES)).isEqualTo(1);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    complete(process, "taskC");
    trigger(scope, "taskB");
    complete(process, "taskB");
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @Test
  void migrateDuplicateEnabledInstancesWithoutCollapsingThem() {
    ProcessDefinition source = deploy(model("Parallel", "${false}", chain()));
    ProcessDefinition target = deploy(model("Sequential", "${false}", chain()));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    for (int i = 0; i < 2; i++) {
      trigger(scope, "taskA");
      complete(process, "taskA");
    }
    List<Execution> tokens = enabled(process, "taskB");
    assertThat(tokens).hasSize(2);
    for (int i = 0; i < tokens.size(); i++) {
      runtimeService.setVariableLocal(tokens.get(i).getId(), "tokenMarker", "value" + i);
    }
    List<String> ids = tokens.stream().map(Execution::getId).toList();

    migrate(equalPlan(source, target), process);

    assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactlyInAnyOrderElementsOf(ids);
    assertThat(runtimeService.getVariableLocal(scope, AD_HOC_ENABLED_ACTIVITY_IDS))
        .isEqualTo(List.of("taskB", "taskB"));
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES)).isEqualTo(2);
    List<Object> values = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      trigger(scope, "taskB");
      values.add(runtimeService.getVariableLocal(task(process, "taskB").getExecutionId(), "tokenMarker"));
      complete(process, "taskB");
    }
    assertThat(values).containsExactlyInAnyOrder("value0", "value1");
    assertThat(enabled(process, "taskB")).isEmpty();
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(4);
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @Test
  void rejectMissingEnabledTargetMappingWithoutChangingItsToken() {
    ProcessDefinition source = deploy(model("Parallel", "${false}", chain()));
    ProcessDefinition target = deploy(model("Parallel", "${false}", chain()));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    String token = enabled(process, "taskB").get(0).getId();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").build();

    assertThatThrownBy(() -> migrate(plan, process)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("no migration instruction");

    assertSource(process, source);
    assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactly(token);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(1);
    trigger(scope, "taskB");
    complete(process, "taskB");
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateEnabledActivityIntoOrdinaryWrapperAndOptionallyBackOut(boolean removeWrapper) {
    String sourceXml = model("Sequential", "${false}", chain());
    ProcessDefinition source = deploy(sourceXml);
    String targetChildren = "<userTask id=\"taskA\"/><subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"taskB\"/>"
        + "<userTask id=\"taskB\"/></subProcess><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"inner\"/>";
    ProcessDefinition target = deploy(model("Sequential", "${false}", targetChildren));
    ProcessDefinition third = deploy(sourceXml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    runtimeService.setVariableLocal(scope, "scopeMarker", "kept");
    trigger(scope, "taskA");
    complete(process, "taskA");
    runtimeService.setVariableLocal(enabled(process, "taskB").get(0).getId(), "tokenMarker", "kept");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB").build();

    migrate(plan, process);

    assertThat(scope(process, "adhoc")).isEqualTo(scope);
    assertThat(runtimeService.getVariableLocal(scope, "scopeMarker")).isEqualTo("kept");
    assertThat(enabled(process, "taskB")).hasSize(1);
    assertThat(runtimeService.getVariableLocal(scope, AD_HOC_ENABLED_ACTIVITY_IDS)).isEqualTo(List.of("taskB"));
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .contains("taskA", "taskB");
    if (removeWrapper) {
      MigrationPlan nextPlan = runtimeService.createMigrationPlan(target.getId(), third.getId())
          .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB").build();
      migrate(nextPlan, process);
    }
    trigger(scope, "taskB");
    assertThat(runtimeService.getVariableLocal(task(process, "taskB").getExecutionId(), "tokenMarker")).isEqualTo("kept");
    complete(process, "taskB");
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @Test
  void preserveEnabledNestedAdHocTargetWhenBothScopesAreMapped() {
    String children = "<adHocSubProcess id=\"inner\">" + chain() + "</adHocSubProcess>";
    String xml = model("Parallel", "", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml.replace("inner", "renamedInner").replace("taskB", "renamedB"));
    ProcessInstance process = start(source);
    trigger(scope(process, "adhoc"), "inner");
    String inner = scope(process, "inner");
    trigger(inner, "taskA");
    complete(process, "taskA");
    String token = enabled(process, "taskB").get(0).getId();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("inner", "renamedInner")
        .mapActivities("taskB", "renamedB").build();
    migrate(plan, process);
    assertThat(scope(process, "renamedInner")).isEqualTo(inner);
    assertThat(enabled(process, "renamedB")).extracting(Execution::getId).containsExactly(token);
    trigger(inner, "renamedB");
    complete(process, "renamedB");
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void migrateEnabledMultiInstanceBodyBeforeItsInstancesStart(boolean sequential) {
    String children = "<userTask id=\"taskA\"/><userTask id=\"taskB\">"
        + multiInstance(sequential) + "</userTask><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
    String xml = model("Sequential", "", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();
    migrate(equalPlan(source, target), process);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .contains("taskB");
    trigger(scope, "taskB");
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isEqualTo(sequential ? 1 : 2);
    complete(process, "taskB");
    complete(process, "taskB");
    finish(process);
  }

  @Test
  void preserveEnabledTokensSeparatelyInParallelAdHocMultiInstanceScopes() {
    String xml = model("Parallel", "", multiInstance(false) + chain());
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    List<Execution> scopes = runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").list();
    assertThat(scopes).hasSize(2);
    for (Execution scope : scopes) {
      trigger(scope.getId(), "taskA");
    }
    complete(process, "taskA");
    complete(process, "taskA");
    List<String> tokenIds = enabled(process, "taskB").stream().map(Execution::getId).toList();
    migrate(equalPlan(source, target), process);
    assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactlyInAnyOrderElementsOf(tokenIds);
    for (Execution scope : scopes) {
      trigger(scope.getId(), "taskB");
    }
    complete(process, "taskB");
    complete(process, "taskB");
    finish(process);
  }

  @Test
  void applyTargetAsyncBeforeOnlyWhenEnabledActivityIsTriggered() {
    ProcessDefinition source = deploy(model("Parallel", "", chain()));
    ProcessDefinition target = deploy(model("Parallel", "", chain().replace("id=\"taskB\"", "id=\"taskB\" operaton:asyncBefore=\"true\"")));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    migrate(equalPlan(source, target), process);
    assertNoJobs(process);
    trigger(scope, "taskB");
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();
    String job = engine.getManagementService().createJobQuery().processInstanceId(process.getId()).singleResult().getId();
    engine.getManagementService().executeJob(job);
    complete(process, "taskB");
    finish(process);
  }

  @Test
  void generateAndExecuteMappingForEnabledSynchronousServiceTask() {
    String children = "<userTask id=\"taskA\"/><serviceTask id=\"service\" operaton:class=\""
        + MarkCompleteDelegate.class.getName() + "\"/><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"service\"/>";
    String xml = model("Parallel", "", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    MigrationPlan plan = equalPlan(source, target);
    assertThat(plan.getInstructions()).anySatisfy(instruction ->
        assertThat(instruction.getSourceActivityId()).isEqualTo("service"));
    migrate(plan, process);
    trigger(scope, "service");
    assertThat(runtimeService.getVariable(process.getId(), "serviceCompleted")).isEqualTo(true);
    finish(process);
  }

  @Test
  void preserveJoinWaitAndEnabledLoopAcrossSuccessiveMigrations() {
    String children = "<userTask id=\"rootTask\"/><parallelGateway id=\"fork\"/>"
        + "<sequenceFlow id=\"rootFlow\" sourceRef=\"rootTask\" targetRef=\"fork\"/>"
        + "<userTask id=\"taskA\"/><userTask id=\"taskB\"/><userTask id=\"taskC\"/>"
        + "<sequenceFlow id=\"forkA\" sourceRef=\"fork\" targetRef=\"taskA\"/>"
        + "<sequenceFlow id=\"forkB\" sourceRef=\"fork\" targetRef=\"taskB\"/>"
        + "<parallelGateway id=\"join\"/><sequenceFlow id=\"joinA\" sourceRef=\"taskA\" targetRef=\"join\"/>"
        + "<sequenceFlow id=\"joinB\" sourceRef=\"taskB\" targetRef=\"join\"/>"
        + "<sequenceFlow id=\"joinC\" sourceRef=\"join\" targetRef=\"taskC\"/>"
        + "<sequenceFlow id=\"loop\" sourceRef=\"taskC\" targetRef=\"taskC\"/>";
    String xml = model("Parallel", "${nrOfCompletedAdHocActivities >= 5}", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessDefinition third = deploy(xml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "rootTask");
    complete(process, "rootTask");
    trigger(scope, "taskA");
    complete(process, "taskA");

    migrate(equalPlan(source, target), process);
    trigger(scope, "taskB");
    complete(process, "taskB");
    assertThat(enabled(process, "taskC")).hasSize(1);
    trigger(scope, "taskC");
    complete(process, "taskC");
    String loopToken = enabled(process, "taskC").get(0).getId();
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(4);

    migrate(equalPlan(target, third), process);

    assertThat(enabled(process, "taskC")).extracting(Execution::getId).containsExactly(loopToken);
    trigger(scope, "taskC");
    complete(process, "taskC");
    finish(process);
  }

  @Test
  void rollBackMigratedEnabledTokenWhenLaterProcessHasUnmappedEnabledTarget() {
    String children = chain() + "<userTask id=\"taskD\"/><userTask id=\"taskC\"/>"
        + "<sequenceFlow id=\"other\" sourceRef=\"taskD\" targetRef=\"taskC\"/>";
    String xml = model("Parallel", "", children);
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance first = start(source);
    ProcessInstance second = start(source);
    trigger(scope(first, "adhoc"), "taskA");
    complete(first, "taskA");
    trigger(scope(second, "adhoc"), "taskD");
    complete(second, "taskD");
    String firstToken = enabled(first, "taskB").get(0).getId();
    String secondToken = enabled(second, "taskC").get(0).getId();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB").build();
    assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(first.getId(), second.getId()).execute())
        .isInstanceOf(MigratingProcessInstanceValidationException.class).hasMessageContaining("no migration instruction");
    assertSource(first, source);
    assertSource(second, source);
    assertThat(enabled(first, "taskB")).extracting(Execution::getId).containsExactly(firstToken);
    assertThat(enabled(second, "taskC")).extracting(Execution::getId).containsExactly(secondToken);
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void explicitlyCompleteMigratedPendingOnlyWrapperWithoutLeakingScopeState(boolean cancelRemaining) {
    String sourceXml = model("Parallel", "${false}", chain()).replace(
        "ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"" + cancelRemaining + "\"");
    String targetXml = model("Parallel", "${false}", wrapperChildren(false)).replace(
        "ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"" + cancelRemaining + "\"");
    ProcessDefinition source = deploy(sourceXml);
    ProcessDefinition target = deploy(targetXml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB").build();
    migrate(plan, process);
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("wrapperLocal").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).isEqualTo(0);

    runtimeService.completeAdHocSubProcess(scope);

    assertPendingWrapperCleaned(process);
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void completionLatchDiscardsPendingOnlyWrapperWhileApplyingCancellationPolicy(boolean cancelRemaining) {
    String sourceXml = model("Parallel", "${done}", chain() + "<userTask id=\"taskC\"/>").replace(
        "ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"" + cancelRemaining + "\"");
    String targetXml = model("Parallel", "${done}", wrapperChildren(false) + "<userTask id=\"taskC\"/>").replace(
        "ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"" + cancelRemaining + "\"");
    ProcessDefinition source = deploy(sourceXml);
    ProcessDefinition target = deploy(targetXml);
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC");
    complete(process, "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB").mapActivities("taskC", "taskC").build();
    migrate(plan, process);

    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskC");

    assertPendingWrapperCleaned(process);
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void latchedCompletionDiscardsEnabledLeafButDrainsRunningSiblingInSameWrapper(boolean reparentAgain) {
    String sourceChildren = chain() + "<userTask id=\"taskC\"/><userTask id=\"taskD\"/>";
    String targetChildren = wrapperChildren(true) + "<userTask id=\"taskD\"/>";
    ProcessDefinition source = deploy(model("Parallel", "${done}", sourceChildren)
        .replace("ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"false\""));
    ProcessDefinition target = deploy(model("Parallel", "${done}", targetChildren)
        .replace("ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"false\""));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("done", false));
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC", "taskD");
    complete(process, "taskA");
    String activeTask = task(process, "taskC").getId();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "taskB")
        .mapActivities("taskC", "taskC").mapActivities("taskD", "taskD").build();
    migrate(plan, process);

    if (reparentAgain) {
      String enabledId = enabled(process, "taskB").get(0).getId();
      runtimeService.setVariableLocal(enabledId, "tokenLocal", "retained");
      String thirdChildren = targetChildren.replace("targetRef=\"inner\"", "targetRef=\"outer\"")
          .replace("<subProcess id=\"inner\">", "<subProcess id=\"outer\"><extensionElements><operaton:inputOutput>"
              + "<operaton:inputParameter name=\"outerLocal\">retained</operaton:inputParameter>"
              + "</operaton:inputOutput></extensionElements><startEvent id=\"outerStart\"/>"
              + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"inner\"/><subProcess id=\"inner\">")
          .replace("</subProcess>", "</subProcess></subProcess>");
      ProcessDefinition third = deploy(model("Parallel", "${done}", thirdChildren)
          .replace("ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"false\""));
      MigrationPlan secondPlan = runtimeService.createMigrationPlan(target.getId(), third.getId())
          .mapActivities("adhoc", "adhoc").mapActivities("inner", "inner")
          .mapActivities("taskB", "taskB").mapActivities("taskC", "taskC").mapActivities("taskD", "taskD").build();
      migrate(secondPlan, process);
      assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactly(enabledId);
      assertThat(runtimeService.getVariableLocal(enabledId, "tokenLocal")).isEqualTo("retained");
    }

    runtimeService.setVariable(process.getId(), "done", true);
    complete(process, "taskD");

    assertThat(enabled(process, "taskB")).isEmpty();
    assertThat(task(process, "taskC").getId()).isEqualTo(activeTask);
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("wrapperLocal").count()).isEqualTo(1);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    complete(process, "taskC");
    assertPendingWrapperCleaned(process);
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("outerLocal").count()).isZero();
    finish(process);
  }

  private String wrapperChildren(boolean runningSibling) {
    return "<userTask id=\"taskA\"/><subProcess id=\"inner\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"wrapperLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"taskB\"/>"
        + "<userTask id=\"taskB\"/>" + (runningSibling ? "<userTask id=\"taskC\"/>" : "")
        + "</subProcess><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"inner\"/>";
  }

  private void assertPendingWrapperCleaned(ProcessInstance process) {
    assertThat(enabled(process, "taskB")).isEmpty();
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("wrapperLocal").count()).isZero();
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(process.getId())
        .activityId("taskB").count()).isZero();
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(process.getId())
        .activityId("inner").unfinished().count()).isZero();
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void discardEnabledCompositeOnWrapperScopeWithoutEndingUnstartedComposite() {
    String pending = "<subProcess id=\"pending\"><extensionElements>"
        + "<operaton:executionListener event=\"end\" expression=\"${execution.setVariable('unstartedEnd', true)}\"/>"
        + "<operaton:inputOutput><operaton:inputParameter name=\"pendingInput\">never</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"pendingStart\"/>"
        + "<sequenceFlow id=\"pendingFlow\" sourceRef=\"pendingStart\" targetRef=\"taskB\"/>"
        + "<userTask id=\"taskB\"/></subProcess>";
    String sourceChildren = "<userTask id=\"taskA\"/>" + pending
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"pending\"/>";
    String targetChildren = "<userTask id=\"taskA\"/><subProcess id=\"inner\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"wrapperLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"pending\"/>"
        + pending + "</subProcess><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"inner\"/>";
    ProcessDefinition source = deploy(model("Parallel", "${false}", sourceChildren)
        .replace("ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"false\""));
    ProcessDefinition target = deploy(model("Parallel", "${false}", targetChildren)
        .replace("ordering=\"Parallel\"", "ordering=\"Parallel\" cancelRemainingInstances=\"false\""));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("unstartedEnd", false));
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("pending", "pending").build();
    migrate(plan, process);
    assertThat(enabled(process, "pending")).hasSize(1);

    runtimeService.completeAdHocSubProcess(scope);

    assertThat(runtimeService.getVariable(process.getId(), "unstartedEnd")).isEqualTo(false);
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(process.getId())
        .activityId("pending").count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("pendingInput").count()).isZero();
    assertPendingWrapperCleaned(process);
    finish(process);
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveSuspendedRunningAndEnabledStateAcrossMigration(boolean insertWrapper) {
    String children = chain() + "<userTask id=\"taskC\"/>";
    ProcessDefinition source = deploy(model("Parallel", "${false}", children));
    ProcessDefinition target = deploy(model("Parallel", "${false}",
        insertWrapper ? wrapperChildren(true) : children).replace("taskB", "renamedB"));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC");
    complete(process, "taskA");
    String beforeToken = enabled(process, "taskB").get(0).getId();
    runtimeService.setVariableLocal(beforeToken, "tokenPayload", "retained");
    String runningTask = task(process, "taskC").getId();
    runtimeService.suspendProcessInstanceById(process.getId());
    List<String> sourceExecutionIds = runtimeService.createExecutionQuery().processInstanceId(process.getId()).list().stream()
        .map(Execution::getId).toList();
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskB", "renamedB").mapActivities("taskC", "taskC").build();

    migrate(plan, process);

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId())
        .singleResult().isSuspended()).isTrue();
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .isNotEmpty().allMatch(Execution::isSuspended);
    assertThat(task(process, "taskC").getId()).isEqualTo(runningTask);
    assertThat(task(process, "taskC").isSuspended()).isTrue();
    String token = enabled(process, "renamedB").get(0).getId();
    assertThat(runtimeService.getVariableLocal(token, "tokenPayload")).isEqualTo("retained");
    if (!insertWrapper) {
      assertThat(token).isEqualTo(beforeToken);
    } else {
      String wrapperExecution = runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
          .variableName("wrapperLocal").singleResult().getExecutionId();
      assertThat(sourceExecutionIds).doesNotContain(wrapperExecution);
      assertThat(runtimeService.createExecutionQuery().executionId(wrapperExecution).singleResult().isSuspended()).isTrue();
      assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).hasSize(1);
    }
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .contains("renamedB");
    Map<String, Object> scopeState = runtimeService.getVariablesLocal(scope);
    List<String> executionIds = runtimeService.createExecutionQuery().processInstanceId(process.getId()).list().stream()
        .map(Execution::getId).toList();

    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(scope, List.of("renamedB"),
        Map.of("renamedB", Map.of("blockedTrigger", true)))).isInstanceOf(SuspendedEntityInteractionException.class);
    assertThatThrownBy(() -> runtimeService.completeAdHocSubProcess(scope, Map.of("blockedCompletion", true)))
        .isInstanceOf(SuspendedEntityInteractionException.class);

    assertThat(enabled(process, "renamedB")).extracting(Execution::getId).containsExactly(token);
    assertThat(task(process, "taskC").getId()).isEqualTo(runningTask);
    assertThat(runtimeService.getVariablesLocal(scope)).isEqualTo(scopeState);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .extracting(Execution::getId).containsExactlyInAnyOrderElementsOf(executionIds);
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("blockedTrigger").count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("blockedCompletion").count()).isZero();
    assertNoJobs(process);

    runtimeService.activateProcessInstanceById(process.getId());
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .noneMatch(Execution::isSuspended);
    complete(process, "taskC");
    trigger(scope, "renamedB");
    complete(process, "renamedB");
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ENABLED_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).isEmpty();
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveSuspendedAsyncAfterJobWhenInsertingOrdinaryWrapper(boolean suspendProcess) {
    String child = "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"pendingLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements></userTask>";
    ProcessDefinition source = deploy(model("Parallel", "${false}", child));
    String wrapper = "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"renamedA\"/>"
        + child.replace("taskA", "renamedA") + "</subProcess>";
    ProcessDefinition target = deploy(model("Parallel", "${false}", wrapper));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    Job before = engine.getManagementService().createJobQuery().processInstanceId(process.getId()).singleResult();
    if (suspendProcess) {
      runtimeService.suspendProcessInstanceById(process.getId());
    } else {
      engine.getManagementService().suspendJobById(before.getId());
    }
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "renamedA").build();

    migrate(plan, process);

    Job migrated = engine.getManagementService().createJobQuery().processInstanceId(process.getId()).singleResult();
    assertThat(migrated.getId()).isEqualTo(before.getId());
    assertThat(migrated.getExecutionId()).isEqualTo(before.getExecutionId());
    assertThat(migrated.isSuspended()).isTrue();
    assertThat(migrated.getRetries()).isEqualTo(before.getRetries());
    assertThat(migrated.getProcessDefinitionId()).isEqualTo(target.getId());
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId())
        .active().executable().count()).isZero();
    assertThat(runtimeService.getVariableLocal(migrated.getExecutionId(), "pendingLocal")).isEqualTo("retained");
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).list())
        .allMatch(execution -> execution.isSuspended() == suspendProcess);

    if (suspendProcess) {
      runtimeService.activateProcessInstanceById(process.getId());
    } else {
      engine.getManagementService().activateJobById(migrated.getId());
    }
    assertThat(engine.getManagementService().createJobQuery().jobId(migrated.getId())
        .singleResult().isSuspended()).isFalse();
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId())
        .active().executable().count()).isEqualTo(1);
    engine.getManagementService().executeJob(migrated.getId());
    assertNoJobs(process);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).isEqualTo(0);
    assertThat(runtimeService.getActivityInstance(process.getId()).getActivityInstances("inner")).isEmpty();
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId("renamedA").count())
        .isZero();
    assertThat(runtimeService.createVariableInstanceQuery().processInstanceIdIn(process.getId())
        .variableName("pendingLocal").count()).isZero();
    runtimeService.completeAdHocSubProcess(scope);
    finish(process);
  }

  @Test
  void rejectUnauthorizedMigrationBeforeChangingRunningOrEnabledState() {
    String xml = model("Parallel", "${false}", chain() + "<userTask id=\"taskC\"/>");
    ProcessDefinition source = deploy(xml);
    ProcessDefinition target = deploy(xml);
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC");
    complete(process, "taskA");
    String token = enabled(process, "taskB").get(0).getId();
    String task = task(process, "taskC").getId();
    MigrationPlan plan = equalPlan(source, target);
    boolean previous = engine.getProcessEngineConfiguration().isAuthorizationEnabled();
    try {
      engine.getProcessEngineConfiguration().setAuthorizationEnabled(true);
      engine.getIdentityService().setAuthenticatedUserId("migration-user-without-permission");
      assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute())
          .isInstanceOf(AuthorizationException.class);
    } finally {
      engine.getIdentityService().clearAuthentication();
      engine.getProcessEngineConfiguration().setAuthorizationEnabled(previous);
    }
    assertSource(process, source);
    assertThat(scope(process, "adhoc")).isEqualTo(scope);
    assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactly(token);
    assertThat(task(process, "taskC").getId()).isEqualTo(task);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(1);
  }

  @Test
  void rejectUnauthenticatedTenantBeforeChangingRunningOrEnabledState() {
    String xml = model("Parallel", "${false}", chain() + "<userTask id=\"taskC\"/>");
    ProcessDefinition source = testHelper.deployForTenantAndGetDefinition("tenantA", Bpmn.readModelFromStream(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    ProcessDefinition target = testHelper.deployForTenantAndGetDefinition("tenantA", Bpmn.readModelFromStream(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    ProcessInstance process = start(source);
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA", "taskC");
    complete(process, "taskA");
    String token = enabled(process, "taskB").get(0).getId();
    String task = task(process, "taskC").getId();
    MigrationPlan plan = equalPlan(source, target);
    boolean previous = engine.getProcessEngineConfiguration().isTenantCheckEnabled();
    try {
      engine.getProcessEngineConfiguration().setTenantCheckEnabled(true);
      engine.getIdentityService().setAuthentication("other-tenant-user", null, List.of("tenantB"));
      assertThatThrownBy(() -> runtimeService.newMigration(plan).processInstanceIds(process.getId()).execute())
          .isInstanceOf(ProcessEngineException.class).hasMessageContaining("no authenticated tenant");
    } finally {
      engine.getIdentityService().clearAuthentication();
      engine.getProcessEngineConfiguration().setTenantCheckEnabled(previous);
    }
    assertSource(process, source);
    assertThat(scope(process, "adhoc")).isEqualTo(scope);
    assertThat(enabled(process, "taskB")).extracting(Execution::getId).containsExactly(token);
    assertThat(task(process, "taskC").getId()).isEqualTo(task);
    assertThat(runtimeService.getVariableLocal(scope, NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).isEqualTo(1);
  }


  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void discardUnstartedMultiInstanceBodyParkedOnOrdinaryWrapper(boolean sequential) {
    String pending = "<userTask id=\"taskB\"><extensionElements>"
        + "<operaton:executionListener event=\"end\" expression=\"${execution.setVariable('unstartedEnd', true)}\"/>"
        + "</extensionElements>" + multiInstance(sequential) + "</userTask>";
    String sourceChildren = "<userTask id=\"taskA\"/>" + pending
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
    String targetChildren = "<userTask id=\"taskA\"/><subProcess id=\"inner\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"wrapperLocal\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerFlow\" sourceRef=\"innerStart\" targetRef=\"taskB\"/>"
        + pending + "</subProcess><sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"inner\"/>";
    ProcessDefinition source = deploy(model("Parallel", "${false}", sourceChildren));
    ProcessDefinition target = deploy(model("Parallel", "${false}", targetChildren));
    ProcessInstance process = runtimeService.startProcessInstanceById(source.getId(), Map.of("unstartedEnd", false));
    String scope = scope(process, "adhoc");
    trigger(scope, "taskA");
    complete(process, "taskA");
    String body = "taskB#multiInstanceBody";
    MigrationPlan plan = runtimeService.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities(body, body).mapActivities("taskB", "taskB").build();
    migrate(plan, process);
    assertThat(enabled(process, body)).hasSize(1);
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).count()).isZero();

    runtimeService.completeAdHocSubProcess(scope);

    assertThat(runtimeService.getVariable(process.getId(), "unstartedEnd")).isEqualTo(false);
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(process.getId())
        .activityId(body).count()).isZero();
    assertPendingWrapperCleaned(process);
    finish(process);
  }

  public static class MarkCompleteDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
      execution.setVariable("serviceCompleted", true);
    }
  }

  private String chain() {
    return "<userTask id=\"taskA\"/><userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
  }

  private String multiInstance(boolean sequential) {
    return "<multiInstanceLoopCharacteristics isSequential=\"" + sequential + "\">"
        + "<loopCardinality>2</loopCardinality></multiInstanceLoopCharacteristics>";
  }

  private String model(String ordering, String condition, String children) {
    return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"adhoc-enabled-migration\">"
        + "<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/>"
        + "<adHocSubProcess id=\"adhoc\" ordering=\"" + ordering + "\">" + children
        + (condition.isEmpty() ? "" : "<completionCondition>" + condition + "</completionCondition>")
        + "</adHocSubProcess><sequenceFlow id=\"leave\" sourceRef=\"adhoc\" targetRef=\"after\"/>"
        + "<userTask id=\"after\"/><sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"end\"/>"
        + "<endEvent id=\"end\"/></process></definitions>";
  }

  private ProcessDefinition deploy(String xml) {
    return testHelper.deployAndGetDefinition(Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
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

  private void assertSource(ProcessInstance process, ProcessDefinition source) {
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.getId()).singleResult()
        .getProcessDefinitionId()).isEqualTo(source.getId());
  }

  private String scope(ProcessInstance process, String id) {
    Execution execution = runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId(id).singleResult();
    assertThat(execution).isNotNull();
    return execution.getId();
  }

  private List<Execution> enabled(ProcessInstance process, String id) {
    return runtimeService.createExecutionQuery().processInstanceId(process.getId()).activityId(id).list().stream()
        .filter(execution -> Boolean.TRUE.equals(runtimeService.getVariableLocal(execution.getId(), AD_HOC_ENABLED_ACTIVITY)))
        .toList();
  }

  private void trigger(String scope, String... activities) {
    runtimeService.triggerAdHocActivities(scope, List.of(activities), null);
  }

  private Task task(ProcessInstance process, String activityId) {
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(process.getId()).taskDefinitionKey(activityId).list();
    assertThat(tasks).isNotEmpty();
    return tasks.get(0);
  }

  private void complete(ProcessInstance process, String activityId) {
    taskService.complete(task(process, activityId).getId());
  }

  private void assertNoJobs(ProcessInstance process) {
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId()).count()).isZero();
  }

  private void finish(ProcessInstance process) {
    assertThat(taskService.createTaskQuery().processInstanceId(process.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactly("after");
    complete(process, "after");
    testHelper.assertProcessEnded(process.getId());
  }
}
