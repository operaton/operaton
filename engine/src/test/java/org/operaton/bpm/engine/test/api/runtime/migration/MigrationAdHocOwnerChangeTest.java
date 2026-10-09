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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class MigrationAdHocOwnerChangeTest {

  @RegisterExtension
  static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension helper = new MigrationTestExtension(engine);

  @ParameterizedTest
  @CsvSource({"Parallel,false", "Parallel,true", "Sequential,false", "Sequential,true"})
  void addFreshOwnerAroundRunningTaskWithoutStartingConfiguredActivities(String ordering, boolean scopedChild) {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("taskA", task(scopedChild), "taskA"));
    ProcessDefinition target = deploy(process("owner", adhoc(ordering, "${false}", configuredInitialExtra() + task(scopedChild)
        + "<userTask id=\"extra\"/>", ""), "owner"));
    assertFreshTargetStartsConfiguredExtra(target);
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    Task before = task(instance, "taskA");
    runtime.setVariableLocal(before.getExecutionId(), "childLocal", "retained");
    if (scopedChild) {
      runtime.setVariableLocal(before.getExecutionId(), "localInput", "changed after start");
    }
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("taskA", "taskA").build();

    migrate(plan, instance);

    String owner = scope(instance, "owner");
    assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    if (scopedChild) {
      assertThat(runtime.getVariableLocal(task(instance, "taskA").getExecutionId(), "childLocal")).isEqualTo("retained");
      assertThat(runtime.getVariableLocal(task(instance, "taskA").getExecutionId(), "localInput"))
          .isEqualTo("changed after start");
    } else {
      // A non-scoped task initially shares the process execution, so this local belongs to the process.
      assertThat(runtime.getVariableLocal(instance.getId(), "childLocal")).isEqualTo("retained");
    }
    assertThat(runtime.getVariable(task(instance, "taskA").getExecutionId(), "childLocal")).isEqualTo("retained");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    assertThat(runtime.getVariableLocal(owner, "nrOfActiveAdHocActivities")).isEqualTo(1);
    assertThat(runtime.getVariableLocal(owner, "adHocCompletionConditionSatisfied")).isEqualTo(false);
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).count()).isEqualTo(1);
    complete(instance, "taskA");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void convertOrdinaryOwnerToAdHocPreservingCompactedOrScopedChild(boolean scopedChild) {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("owner", ordinary(task(scopedChild)), "owner"));
    ProcessDefinition target = deploy(process("owner", adhoc("Sequential", "${false}", configuredInitialExtra() + task(scopedChild)
        + "<userTask id=\"extra\"/>", ""), "owner"));
    assertFreshTargetStartsConfiguredExtra(target);
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    Task before = task(instance, "taskA");
    HistoricActivityInstance historyBefore = ownerHistory(instance);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").build();

    migrate(plan, instance);

    String owner = scope(instance, "owner");
    assertOwnerHistory(instance, historyBefore, target, "owner", "adHocSubProcess", "Ad-hoc owner");
    assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    if (scopedChild) {
      assertThat(runtime.getVariableLocal(before.getExecutionId(), "localInput")).isEqualTo("retained");
    }
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    assertThat(runtime.getStartableAdHocActivities(owner)).isEmpty();
    complete(instance, "taskA");
    assertThat(runtime.getStartableAdHocActivities(owner)).extracting(AdHocActivity::getActivityId).contains("extra");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void convertRunningAdHocOwnerToOrdinaryKeepingLocalsAndCompletionHistory(boolean scopedChild) {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", task(scopedChild), ""), "owner"));
    String targetOwner = scopedChild ? "renamedOwner" : "owner";
    ProcessDefinition target = deploy(process("owner", ordinary(task(scopedChild)), "owner")
        .replace("\"owner\"", "\"" + targetOwner + "\""));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    String owner = scope(instance, "owner");
    runtime.setVariableLocal(owner, "scopeLocal", "retained");
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    complete(instance, "taskA");
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    Task before = task(instance, "taskA");
    HistoricActivityInstance historyBefore = ownerHistory(instance);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", targetOwner).mapActivities("taskA", "taskA").build();

    migrate(plan, instance);

    assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    assertOwnerHistory(instance, historyBefore, target, targetOwner, "subProcess", "Ordinary owner");
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(instance.getId())
        .activityId("taskA").finished().singleResult().getProcessDefinitionId()).isEqualTo(source.getId());
    assertThat(runtime.getVariableLocal(owner, "scopeLocal")).isEqualTo("retained");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    assertThat(runtime.getVariableLocal(owner, "adHocCompletedActivityIds")).isEqualTo(List.of("taskA"));
    complete(instance, "taskA");
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void removeRunningOwnerUsingGenericWrapperOutputAndListenerPolicy(boolean skipMappingsAndListeners) {
    RuntimeService runtime = engine.getRuntimeService();
    String extensions = "<extensionElements><operaton:executionListener event=\"end\" "
        + "expression=\"${execution.setVariable('removedListener', true)}\"/><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"scopeLocal\">retained</operaton:inputParameter>"
        + "<operaton:outputParameter name=\"removedOutput\">${scopeLocal}</operaton:outputParameter>"
        + "</operaton:inputOutput></extensionElements>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", extensions + task(false), ""), "owner"));
    ProcessDefinition target = deploy(process("taskA", task(false), "taskA"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId(), Map.of("removedListener", false));
    String owner = scope(instance, "owner");
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    Task before = task(instance, "taskA");
    runtime.setVariableLocal(before.getExecutionId(), "childLocal", "retained");
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("taskA", "taskA").build();
    var migration = runtime.newMigration(plan).processInstanceIds(instance.getId());
    if (skipMappingsAndListeners) {
      migration.skipIoMappings().skipCustomListeners();
    }

    migration.execute();

    assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    assertThat(runtime.getVariableLocal(task(instance, "taskA").getExecutionId(), "childLocal")).isEqualTo("retained");
    assertThat(runtime.createExecutionQuery().executionId(owner).count()).isZero();
    assertThat(runtime.getVariable(instance.getId(), "removedOutput")).isEqualTo(skipMappingsAndListeners ? null : "retained");
    assertThat(runtime.getVariable(instance.getId(), "removedListener")).isEqualTo(!skipMappingsAndListeners);
    complete(instance, "taskA");
    finish(instance);
  }

  @ParameterizedTest
  @CsvSource({"idle,true", "idle,false", "enabled,true", "enabled,false", "latched,true", "latched,false"})
  void rejectOwnerRemovalOrConversionWhenNoOrdinaryEquivalentExists(String state, boolean preserveWrapper) {
    RuntimeService runtime = engine.getRuntimeService();
    String children = task(false) + "<userTask id=\"taskB\"/><userTask id=\"taskC\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${done}", children,
        "cancelRemainingInstances=\"false\""), "owner"));
    ProcessDefinition target = deploy(process(preserveWrapper ? "owner" : "taskA",
        preserveWrapper ? ordinary(children) : children, preserveWrapper ? "owner" : "taskC"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId(), Map.of("done", false));
    String owner = scope(instance, "owner");
    if (!"idle".equals(state)) {
      runtime.triggerAdHocActivities(owner, "latched".equals(state) ? List.of("taskA", "taskC") : List.of("taskA"), null);
      runtime.setVariable(instance.getId(), "done", "latched".equals(state));
      complete(instance, "taskA");
    }
    var plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").mapActivities("taskC", "taskC");
    if (preserveWrapper) {
      plan.mapActivities("owner", "owner");
    }
    MigrationPlan migrationPlan = plan.build();
    List<String> tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list()
        .stream().map(Task::getId).toList();

    assertThatThrownBy(() -> migrate(migrationPlan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("idle".equals(state) ? "idle entered" : state);

    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(source.getId());
    assertThat(scope(instance, "owner")).isEqualTo(owner);
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list()).extracting(Task::getId)
        .containsExactlyInAnyOrderElementsOf(tasks);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Parallel", "Sequential"})
  void validateRunningCountBeforeAddingOwnerAroundConcurrentTasks(String ordering) {
    RuntimeService runtime = engine.getRuntimeService();
    String children = "<userTask id=\"taskA\"/><userTask id=\"taskB\"/>";
    String fork = "<parallelGateway id=\"fork\"/><sequenceFlow id=\"forkA\" sourceRef=\"fork\" targetRef=\"taskA\"/>"
        + "<sequenceFlow id=\"forkB\" sourceRef=\"fork\" targetRef=\"taskB\"/>";
    ProcessDefinition source = deploy(process("fork", fork + children, null));
    ProcessDefinition target = deploy(process("owner", adhoc(ordering, "${false}", children, ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").build();
    if ("Sequential".equals(ordering)) {
      assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
          .hasMessageContaining("more than one open child activity");
      assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
          .isEqualTo(source.getId());
    } else {
      migrate(plan, instance);
      String owner = scope(instance, "owner");
      assertThat(runtime.getVariableLocal(owner, "nrOfActiveAdHocActivities")).isEqualTo(2);
      complete(instance, "taskA");
      complete(instance, "taskB");
      runtime.completeAdHocSubProcess(owner);
      finish(instance);
    }
  }

  @Test
  void enabledActivityCannotAcquireANewActivationOwner() {
    RuntimeService runtime = engine.getRuntimeService();
    String children = task(false) + "<userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", children, ""), "owner"));
    String targetChildren = "<adHocSubProcess id=\"nested\"><userTask id=\"taskB\"/></adHocSubProcess>";
    ProcessDefinition target = deploy(process("owner", adhoc("Parallel", "${false}", targetChildren, ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    runtime.triggerAdHocActivities(scope(instance, "owner"), List.of("taskA"), null);
    complete(instance, "taskA");
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("taskB", "taskB").build();
    assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("original mapped activation owner");
  }

  @Test
  void addSequentialOwnerIndependentlyInsideEachMultiInstanceIteration() {
    RuntimeService runtime = engine.getRuntimeService();
    String loop = "<multiInstanceLoopCharacteristics isSequential=\"false\"><loopCardinality>2</loopCardinality>"
        + "</multiInstanceLoopCharacteristics>";
    String sourceOuter = ordinary(task(false)).replace("<subProcess id=\"owner\" name=\"Ordinary owner\">", "<subProcess id=\"outer\">" + loop);
    String targetOuter = "<subProcess id=\"outer\">" + loop + "<startEvent id=\"outerStart\"/>"
        + "<sequenceFlow id=\"outerEnter\" sourceRef=\"outerStart\" targetRef=\"owner\"/>"
        + adhoc("Sequential", "${nrOfCompletedAdHocActivities >= 1}", task(false), "") + "</subProcess>";
    ProcessDefinition source = deploy(process("outer", sourceOuter, "outer"));
    ProcessDefinition target = deploy(process("outer", targetOuter, "outer"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    List<String> taskIds = engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list()
        .stream().map(Task::getId).toList();
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapEqualActivities().mapActivities("taskA", "taskA").build();

    migrate(plan, instance);

    assertThat(runtime.createExecutionQuery().processInstanceId(instance.getId()).activityId("owner").list()).hasSize(2);
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list()).extracting(Task::getId)
        .containsExactlyInAnyOrderElementsOf(taskIds);
    taskIds.forEach(id -> engine.getTaskService().complete(id));
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveTimerRoutingWaitWhenConvertingOrRemovingOwner(boolean preserveWrapper) {
    RuntimeService runtime = engine.getRuntimeService();
    String children = task(false) + "<intermediateCatchEvent id=\"timer\"><timerEventDefinition>"
        + "<timeDuration>PT1H</timeDuration></timerEventDefinition></intermediateCatchEvent>"
        + "<userTask id=\"taskB\"/><sequenceFlow id=\"toTimer\" sourceRef=\"taskA\" targetRef=\"timer\"/>"
        + "<sequenceFlow id=\"fromTimer\" sourceRef=\"timer\" targetRef=\"taskB\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", children, ""), "owner"));
    ProcessDefinition target = deploy(process(preserveWrapper ? "owner" : "taskA",
        preserveWrapper ? ordinary(children) : children, preserveWrapper ? "owner" : "taskB"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    runtime.triggerAdHocActivities(scope(instance, "owner"), List.of("taskA"), null);
    complete(instance, "taskA");
    Job before = engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).timers().singleResult();
    var builder = runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("timer", "timer");
    if (preserveWrapper) {
      builder.mapActivities("owner", "owner");
    }

    migrate(builder.build(), instance);

    Job after = engine.getManagementService().createJobQuery().jobId(before.getId()).singleResult();
    assertThat(after.getDuedate()).isEqualTo(before.getDuedate());
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
    engine.getManagementService().executeJob(after.getId());
    complete(instance, "taskB");
    finish(instance);
  }

  @Test
  void convertOrdinaryWrapperContainingAsyncBeforeAdHocWithoutLosingJobOrToken() {
    RuntimeService runtime = engine.getRuntimeService();
    String inner = "<adHocSubProcess id=\"inner\" operaton:asyncBefore=\"true\"><userTask id=\"taskA\"/></adHocSubProcess>";
    String ordinary = "<subProcess id=\"owner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"inner\"/>" + inner + "</subProcess>";
    ProcessDefinition source = deploy(process("owner", ordinary, "owner"));
    ProcessDefinition target = deploy(process("owner", adhoc("Sequential", "${false}", inner, ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    Job before = engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).singleResult();
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("inner", "inner").build();

    migrate(plan, instance);

    assertThat(engine.getManagementService().createJobQuery().jobId(before.getId()).count()).isEqualTo(1);
    String owner = scope(instance, "owner");
    assertThat(runtime.getVariableLocal(owner, "nrOfActiveAdHocActivities")).isEqualTo(1);
    engine.getManagementService().executeJob(before.getId());
    runtime.triggerAdHocActivities(scope(instance, "inner"), List.of("taskA"), null);
    complete(instance, "taskA");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @Test
  void mixedWrapperExpansionPreservesReadyAdHocAndRunningSiblingBeforeLatchedDrain() {
    RuntimeService runtime = engine.getRuntimeService();
    String pending = "<adHocSubProcess id=\"pending\"><userTask id=\"taskB\"/></adHocSubProcess>";
    String sourceChildren = task(false) + pending + "<userTask id=\"taskC\"/><userTask id=\"taskD\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"pending\"/>";
    String wrapper = "<subProcess id=\"wrapper\"><startEvent id=\"wrapperStart\"/>"
        + "<sequenceFlow id=\"wrapperEnter\" sourceRef=\"wrapperStart\" targetRef=\"pending\"/>"
        + pending + "<userTask id=\"taskC\"/></subProcess><userTask id=\"taskD\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${done}", sourceChildren,
        "cancelRemainingInstances=\"false\""), "owner"));
    ProcessDefinition target = deploy(process("owner", adhoc("Parallel", "${done}", wrapper,
        "cancelRemainingInstances=\"false\""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId(), Map.of("done", false));
    String owner = scope(instance, "owner");
    runtime.triggerAdHocActivities(owner, List.of("taskA", "taskC", "taskD"), null);
    complete(instance, "taskA");
    String running = task(instance, "taskC").getId();
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("pending", "pending")
        .mapActivities("taskC", "taskC").mapActivities("taskD", "taskD").build();

    migrate(plan, instance);

    assertThat(runtime.getVariableLocal(owner, "nrOfActiveAdHocActivities")).isEqualTo(2);
    assertThat(runtime.getStartableAdHocActivities(owner)).extracting(AdHocActivity::getActivityId).contains("pending");
    runtime.setVariable(instance.getId(), "done", true);
    complete(instance, "taskD");
    assertThat(task(instance, "taskC").getId()).isEqualTo(running);
    assertThat(runtime.createExecutionQuery().processInstanceId(instance.getId()).activityId("pending").count()).isZero();
    assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(instance.getId())
        .activityId("pending").count()).isZero();
    complete(instance, "taskC");
    finish(instance);
  }

  @Test
  void roundTripThroughOrdinaryOwnerPreservesTrackedProgressWithoutCountingOrdinaryCompletions() {
    RuntimeService runtime = engine.getRuntimeService();
    String children = task(false) + "<userTask id=\"taskB\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", children, ""), "owner"));
    ProcessDefinition ordinaryDefinition = deploy(process("owner", ordinary(children
        + "<sequenceFlow id=\"ordinaryNext\" sourceRef=\"taskA\" targetRef=\"taskB\"/>"), "owner"));
    ProcessDefinition returned = deploy(process("owner", adhoc("Sequential", "${false}", configuredInitialExtra()
        + task(false) + "<userTask id=\"renamedB\"/><userTask id=\"extra\"/>", ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    String owner = scope(instance, "owner");
    HistoricActivityInstance originalHistory = ownerHistory(instance);
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    complete(instance, "taskA");
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    migrate(runtime.createMigrationPlan(source.getId(), ordinaryDefinition.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").build(), instance);
    complete(instance, "taskA");
    String taskB = task(instance, "taskB").getId();
    String retiredMarkerId = runtime.createVariableInstanceQuery().executionIdIn(owner)
        .variableName("adHocRetiredContext").singleResult().getId();
    HistoricVariableInstance retiredMarker = engine.getHistoryService().createHistoricVariableInstanceQuery()
        .variableId(retiredMarkerId).singleResult();
    assertThat(retiredMarker.getProcessDefinitionId()).isEqualTo(ordinaryDefinition.getId());
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);

    migrate(runtime.createMigrationPlan(ordinaryDefinition.getId(), returned.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").mapActivities("taskB", "renamedB").build(), instance);

    assertThat(task(instance, "renamedB").getId()).isEqualTo(taskB);
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).count()).isEqualTo(1);
    assertThat(runtime.getVariablesLocal(owner)).doesNotContainKey("adHocRetiredContext");
    HistoricVariableInstance deletedMarker = engine.getHistoryService().createHistoricVariableInstanceQuery()
        .variableId(retiredMarkerId).includeDeleted().singleResult();
    assertThat(deletedMarker.getState()).isEqualTo(HistoricVariableInstance.STATE_DELETED);
    assertThat(deletedMarker.getProcessDefinitionId()).isEqualTo(retiredMarker.getProcessDefinitionId());
    assertThat(deletedMarker.getExecutionId()).isEqualTo(owner);
    assertThat(runtime.createVariableInstanceQuery().variableId(retiredMarkerId).count()).isZero();
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    assertThat(runtime.getVariableLocal(owner, "adHocActiveActivityIds")).isEqualTo(List.of("renamedB"));
    assertOwnerHistory(instance, originalHistory, returned, "owner", "adHocSubProcess", "Ad-hoc owner");
    complete(instance, "renamedB");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(2);
    assertThat(runtime.getVariableLocal(owner, "adHocCompletedActivityIds")).isEqualTo(List.of("taskA", "renamedB"));
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @Test
  void failedSequentialReturnLeavesRetiredContextIntactForLaterValidReturn() {
    RuntimeService runtime = engine.getRuntimeService();
    String children = task(false) + "<userTask id=\"taskB\"/>";
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", children, ""), "owner"));
    ProcessDefinition ordinaryDefinition = deploy(process("owner", ordinary(children), "owner"));
    ProcessDefinition returned = deploy(process("owner", adhoc("Sequential", "${false}", children, ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    String owner = scope(instance, "owner");
    runtime.triggerAdHocActivities(owner, List.of("taskA", "taskB"), null);
    migrate(runtime.createMigrationPlan(source.getId(), ordinaryDefinition.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").build(), instance);
    Object marker = runtime.getVariableLocal(owner, "adHocRetiredContext");
    assertThat(marker).isNotNull();
    MigrationPlan back = runtime.createMigrationPlan(ordinaryDefinition.getId(), returned.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").mapActivities("taskB", "taskB").build();

    assertThatThrownBy(() -> migrate(back, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("more than one open child activity");

    assertThat(runtime.getVariableLocal(owner, "adHocRetiredContext")).isEqualTo(marker);
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(ordinaryDefinition.getId());
    assertThat(ownerHistory(instance).getActivityType()).isEqualTo("subProcess");
    complete(instance, "taskB");
    migrate(back, instance);
    assertThat(runtime.getVariablesLocal(owner)).doesNotContainKey("adHocRetiredContext");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    complete(instance, "taskA");
    assertThat(runtime.getVariableLocal(owner, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(strings = {"nrOfCompletedAdHocActivities", "adHocRetiredContext", "adHocEnabledActivity"})
  void rejectUnmarkedOrdinaryBusinessVariableConflictsWithoutOverwriting(String variableName) {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("owner", ordinary(task(false)), "owner"));
    ProcessDefinition target = deploy(process("owner", adhoc("Parallel", "${false}", task(false), ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    Task before = task(instance, "taskA");
    Object value = "nrOfCompletedAdHocActivities".equals(variableName) ? 42 : Boolean.TRUE;
    runtime.setVariableLocal(before.getExecutionId(), variableName, value);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").build();

    assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("reserved variable '" + variableName + "'");

    assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    assertThat(runtime.getVariableLocal(before.getExecutionId(), variableName)).isEqualTo(value);
    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(source.getId());
    complete(instance, "taskA");
    finish(instance);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectChildMarkerCollisionWhenAddingOwnerWithoutOverwriting(boolean asyncAfter) {
    RuntimeService runtime = engine.getRuntimeService();
    String child = task(true).replace("<userTask id=\"taskA\"",
        "<userTask id=\"taskA\" operaton:asyncAfter=\"" + asyncAfter + "\"");
    ProcessDefinition source = deploy(process("taskA", child, null));
    ProcessDefinition target = deploy(process("owner", adhoc("Parallel", "${false}", child, ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    Task before = task(instance, "taskA");
    runtime.setVariableLocal(before.getExecutionId(), "adHocEnabledActivity", true);
    if (asyncAfter) {
      complete(instance, "taskA");
    }
    Job job = engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).singleResult();
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("taskA", "taskA").build();

    assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("reserved variable 'adHocEnabledActivity'");

    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(source.getId());
    assertThat(runtime.getVariableLocal(before.getExecutionId(), "adHocEnabledActivity")).isEqualTo(true);
    if (asyncAfter) {
      assertThat(engine.getManagementService().createJobQuery().processInstanceId(instance.getId()).singleResult().getId())
          .isEqualTo(job.getId());
    } else {
      assertThat(task(instance, "taskA").getId()).isEqualTo(before.getId());
    }
  }

  @Test
  void addingOwnerLeavesInheritedBusinessMarkerOutsideAdHocContext() {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("taskA", task(false), "taskA"));
    ProcessDefinition target = deploy(process("owner", adhoc("Parallel", "${false}", task(false), ""), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    runtime.setVariableLocal(instance.getId(), "adHocEnabledActivity", true);

    migrate(runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("taskA", "taskA").build(), instance);

    String owner = scope(instance, "owner");
    assertThat(runtime.getVariableLocal(instance.getId(), "adHocEnabledActivity")).isEqualTo(true);
    assertThat(runtime.getVariablesLocal(owner)).doesNotContainKey("adHocEnabledActivity");
    assertThat(runtime.getVariablesLocal(task(instance, "taskA").getExecutionId())).doesNotContainKey("adHocEnabledActivity");
    assertThat(runtime.getVariableLocal(owner, "nrOfActiveAdHocActivities")).isEqualTo(1);
    complete(instance, "taskA");
    runtime.completeAdHocSubProcess(owner);
    finish(instance);
  }

  @Test
  void addingOuterOwnerRejectsOrdinaryCarrierMarkerOfEnteredInnerOwner() {
    RuntimeService runtime = engine.getRuntimeService();
    String inner = adhoc("Parallel", "${false}", task(false), "");
    String preparation = "<parallelGateway id=\"fork\"/>"
        + "<sequenceFlow id=\"toPrepare\" sourceRef=\"fork\" targetRef=\"prepare\"/>"
        + "<sequenceFlow id=\"toKeep\" sourceRef=\"fork\" targetRef=\"keep\"/>"
        + "<userTask id=\"prepare\"/><userTask id=\"keep\"/>"
        + "<sequenceFlow id=\"toOwner\" sourceRef=\"prepare\" targetRef=\"owner\"/>";
    ProcessDefinition source = deploy(process("fork", preparation + inner, "owner"));
    String outer = adhoc("Parallel", "${false}", inner, "").replaceFirst("id=\"owner\"", "id=\"outer\"");
    ProcessDefinition target = deploy(process("outer", outer + "<userTask id=\"keep\"/>", "outer"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    String carrier = task(instance, "prepare").getExecutionId();
    runtime.setVariableLocal(carrier, "adHocEnabledActivity", true);
    complete(instance, "prepare");
    String innerOwner = scope(instance, "owner");
    runtime.triggerAdHocActivities(innerOwner, List.of("taskA"), null);
    String runningTask = task(instance, "taskA").getId();
    assertThat(runtime.getVariablesLocal(innerOwner)).doesNotContainKey("adHocEnabledActivity");
    assertThat(runtime.getVariableLocal(carrier, "adHocEnabledActivity")).isEqualTo(true);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").mapActivities("keep", "keep").build();

    assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("reserved variable 'adHocEnabledActivity'");

    assertThat(task(instance, "taskA").getId()).isEqualTo(runningTask);
    assertThat(scope(instance, "owner")).isEqualTo(innerOwner);
    assertThat(runtime.getVariableLocal(carrier, "adHocEnabledActivity")).isEqualTo(true);
    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(source.getId());
  }

  @Test
  void retiringOwnerRejectsAnExistingMarkerRatherThanOverwritingIt() {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(process("owner", adhoc("Parallel", "${false}", task(false), ""), "owner"));
    ProcessDefinition target = deploy(process("owner", ordinary(task(false)), "owner"));
    ProcessInstance instance = runtime.startProcessInstanceById(source.getId());
    String owner = scope(instance, "owner");
    runtime.triggerAdHocActivities(owner, List.of("taskA"), null);
    runtime.setVariableLocal(owner, "adHocRetiredContext", "application value");
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("owner", "owner").mapActivities("taskA", "taskA").build();

    assertThatThrownBy(() -> migrate(plan, instance)).isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("Cannot retire an ad-hoc owner over reserved variable");

    assertThat(runtime.getVariableLocal(owner, "adHocRetiredContext")).isEqualTo("application value");
    assertThat(runtime.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult().getProcessDefinitionId())
        .isEqualTo(source.getId());
  }

  private String configuredInitialExtra() {
    return "<extensionElements><operaton:properties><operaton:property name=\"activeTasksCollection\" value=\"extra\"/>"
        + "</operaton:properties></extensionElements>";
  }

  private void assertFreshTargetStartsConfiguredExtra(ProcessDefinition definition) {
    ProcessInstance control = engine.getRuntimeService().startProcessInstanceById(definition.getId());
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(control.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactly("extra");
    complete(control, "extra");
    engine.getRuntimeService().completeAdHocSubProcess(scope(control, "owner"));
    finish(control);
  }

  private HistoricActivityInstance ownerHistory(ProcessInstance instance) {
    return engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(instance.getId())
        .activityId("owner").unfinished().singleResult();
  }

  private void assertOwnerHistory(ProcessInstance instance, HistoricActivityInstance before,
      ProcessDefinition target, String activityId, String type, String name) {
    HistoricActivityInstance after = engine.getHistoryService().createHistoricActivityInstanceQuery()
        .activityInstanceId(before.getId()).singleResult();
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.getActivityId()).isEqualTo(activityId);
    assertThat(after.getActivityName()).isEqualTo(name);
    assertThat(after.getActivityType()).isEqualTo(type);
    assertThat(after.getProcessDefinitionId()).isEqualTo(target.getId());
  }

  private String task(boolean scoped) {
    return scoped ? "<userTask id=\"taskA\"><extensionElements><operaton:inputOutput>"
        + "<operaton:inputParameter name=\"localInput\">retained</operaton:inputParameter>"
        + "</operaton:inputOutput></extensionElements></userTask>" : "<userTask id=\"taskA\"/>";
  }

  private String ordinary(String children) {
    return "<subProcess id=\"owner\" name=\"Ordinary owner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"taskA\"/>" + children + "</subProcess>";
  }

  private String adhoc(String ordering, String condition, String children, String attributes) {
    return "<adHocSubProcess id=\"owner\" name=\"Ad-hoc owner\" ordering=\"" + ordering + "\" " + attributes + ">" + children
        + "<completionCondition>" + condition + "</completionCondition></adHocSubProcess>";
  }

  private String process(String entry, String children, String leave) {
    return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"adhoc-owner-migration\">"
        + "<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"" + entry + "\"/>" + children
        + (leave == null ? "" : "<sequenceFlow id=\"leave\" sourceRef=\"" + leave + "\" targetRef=\"after\"/>")
        + "<userTask id=\"after\"/></process></definitions>";
  }

  private ProcessDefinition deploy(String xml) {
    return helper.deployAndGetDefinition(Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  private void migrate(MigrationPlan plan, ProcessInstance instance) {
    engine.getRuntimeService().newMigration(plan).processInstanceIds(instance.getId()).execute();
  }

  private String scope(ProcessInstance instance, String activity) {
    return engine.getRuntimeService().createExecutionQuery().processInstanceId(instance.getId()).activityId(activity).singleResult().getId();
  }

  private Task task(ProcessInstance instance, String activity) {
    Task task = engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).taskDefinitionKey(activity).singleResult();
    assertThat(task).isNotNull();
    return task;
  }

  private void complete(ProcessInstance instance, String activity) {
    engine.getTaskService().complete(task(instance, activity).getId());
  }

  private void finish(ProcessInstance instance) {
    assertThat(engine.getTaskService().createTaskQuery().processInstanceId(instance.getId()).list())
        .extracting(Task::getTaskDefinitionKey).containsExactly("after");
    complete(instance, "after");
    helper.assertProcessEnded(instance.getId());
  }
}
