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
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationException;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationAdHocScopedActivityEndTest {

  @RegisterExtension
  static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension helper = new MigrationTestExtension(engine);

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void relocateUndisposedScopeIntoOrdinaryWrapperAndOptionallyBack(boolean roundTrip) {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(adHoc(task("${input}", "${localInput}") + "<userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>"));
    String targetTask = task("${missingInputMustNotBeReplayed}", "${localInput.concat('-target')}");
    ProcessDefinition target = deploy(adHoc(wrapper(targetTask).replace(
        "<sequenceFlow id=\"innerNext\" sourceRef=\"taskA\" targetRef=\"taskB\"/>",
        "<sequenceFlow id=\"innerNext\" sourceRef=\"taskA\" targetRef=\"taskB\"><conditionExpression>"
            + "${shadow == 'scope'}</conditionExpression></sequenceFlow>")));
    ProcessInstance process = runtime.startProcessInstanceById(source.getId(), initialVariables());
    String scope = runtime.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").singleResult().getId();
    runtime.triggerAdHocActivities(scope, List.of("taskA"), null);
    complete(process, "taskA");
    Job job = asyncJob(process);
    String scopeExecution = job.getExecutionId();
    runtime.setVariableLocal(parentExecutionId(scopeExecution), "shadow", "carrier");
    runtime.setVariableLocal(scopeExecution, "shadow", "scope");
    assertThat(runtime.getVariable(process.getId(), "endExecutions")).isEqualTo(1L);
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(0);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").build();

    runtime.newMigration(plan).processInstanceIds(process.getId()).execute();

    assertThat(asyncJob(process).getExecutionId()).isEqualTo(scopeExecution);
    assertThat(runtime.getVariableLocal(scopeExecution, "localInput")).isEqualTo("retained");
    assertThat(runtime.getVariableLocal(scopeExecution, "shadow")).isEqualTo("scope");
    assertThat(runtime.getVariableLocal(parentExecutionId(scopeExecution), "shadow")).isEqualTo("carrier");
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(0);
    if (roundTrip) {
      ProcessDefinition direct = deploy(adHoc(targetTask + "<userTask id=\"taskB\"/>"
          + "<sequenceFlow id=\"next\" sourceRef=\"taskA\" targetRef=\"taskB\"/>"));
      MigrationPlan back = runtime.createMigrationPlan(target.getId(), direct.getId())
          .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA").build();
      runtime.newMigration(back).processInstanceIds(process.getId()).execute();
      assertThat(asyncJob(process).getExecutionId()).isEqualTo(scopeExecution);
      assertThat(runtime.getVariableLocal(scopeExecution, "shadow")).isEqualTo("scope");
      assertThat(runtime.getVariableLocal(parentExecutionId(scopeExecution), "shadow")).isEqualTo("carrier");
    }

    engine.getManagementService().executeJob(job.getId());

    assertThat(runtime.getVariable(process.getId(), "mappedOutput")).isEqualTo("retained-target");
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(1L);
    assertThat(runtime.getVariable(process.getId(), "endExecutions")).isEqualTo(1L);
    assertThat(runtime.createVariableInstanceQuery().processInstanceIdIn(process.getId()).variableName("localInput").count())
        .isZero();
    if (roundTrip) {
      assertThat(engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).count()).isZero();
      runtime.triggerAdHocActivities(scope, List.of("taskB"), null);
    }
    complete(process, "taskB");
    runtime.completeAdHocSubProcess(scope);
    complete(process, "after");
    helper.assertProcessEnded(process.getId());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preserveOrRemoveBoundaryDependenciesOnRetainedScope(boolean retainBoundaries) {
    RuntimeService runtime = engine.getRuntimeService();
    String boundaries = "<boundaryEvent id=\"timer\" attachedToRef=\"taskA\" cancelActivity=\"false\">"
        + "<timerEventDefinition><timeDuration>PT1H</timeDuration></timerEventDefinition></boundaryEvent>"
        + "<boundaryEvent id=\"message\" attachedToRef=\"taskA\" cancelActivity=\"false\">"
        + "<messageEventDefinition messageRef=\"notice\"/></boundaryEvent>";
    String sourceChildren = task("${input}", "${localInput}") + boundaries;
    String targetChildren = task("${missingInputMustNotBeReplayed}", "${localInput}")
        + (retainBoundaries ? boundaries : "");
    ProcessDefinition source = deploy(adHoc(sourceChildren));
    ProcessDefinition target = deploy(adHoc(wrapper(targetChildren)));
    ProcessInstance process = runtime.startProcessInstanceById(source.getId(), initialVariables());
    String scope = runtime.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").singleResult().getId();
    runtime.triggerAdHocActivities(scope, List.of("taskA"), null);
    complete(process, "taskA");
    Job job = asyncJob(process);
    Job timer = engine.getManagementService().createJobQuery().processInstanceId(process.getId()).timers().singleResult();
    String subscription = runtime.createEventSubscriptionQuery().processInstanceId(process.getId()).eventName("notice")
        .singleResult().getId();
    var builder = runtime.createMigrationPlan(source.getId(), target.getId())
        .mapActivities("adhoc", "adhoc").mapActivities("taskA", "taskA");
    if (retainBoundaries) {
      builder.mapActivities("timer", "timer").mapActivities("message", "message");
    }

    runtime.newMigration(builder.build()).processInstanceIds(process.getId()).execute();

    if (retainBoundaries) {
      assertThat(engine.getManagementService().createJobQuery().jobId(timer.getId()).singleResult().getDuedate())
          .isEqualTo(timer.getDuedate());
      assertThat(runtime.createEventSubscriptionQuery().eventSubscriptionId(subscription).singleResult().getExecutionId())
          .isEqualTo(job.getExecutionId());
    } else {
      assertThat(engine.getManagementService().createJobQuery().jobId(timer.getId()).count()).isZero();
      assertThat(runtime.createEventSubscriptionQuery().eventSubscriptionId(subscription).count()).isZero();
    }
    engine.getManagementService().executeJob(job.getId());
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId()).count()).isZero();
    assertThat(runtime.createEventSubscriptionQuery().processInstanceId(process.getId()).count()).isZero();
    complete(process, "taskB");
    runtime.completeAdHocSubProcess(scope);
    complete(process, "after");
    helper.assertProcessEnded(process.getId());
  }

  @Test
  void ordinaryTerminalScopedAsyncAfterRetainsLocalsAndSelectsNewTargetFlow() {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(definitions("<process id=\"process\" isExecutable=\"true\">"
        + "<startEvent id=\"start\"/><sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"taskA\"/>"
        + task("${input}", "${localInput}") + "</process>"));
    ProcessDefinition target = deploy(definitions("<process id=\"process\" isExecutable=\"true\">"
        + "<startEvent id=\"start\"/><sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"inner\"/>"
        + wrapper(task("${missingInputMustNotBeReplayed}", "${localInput.concat('-target')}")) + "</process>"));
    ProcessInstance process = runtime.startProcessInstanceById(source.getId(), initialVariables());
    complete(process, "taskA");
    Job job = asyncJob(process);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapActivities("taskA", "taskA").build();

    runtime.newMigration(plan).processInstanceIds(process.getId()).execute();

    assertThat(asyncJob(process).getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(runtime.getVariableLocal(job.getExecutionId(), "localInput")).isEqualTo("retained");
    engine.getManagementService().executeJob(job.getId());
    assertThat(runtime.getVariable(process.getId(), "mappedOutput")).isEqualTo("retained-target");
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(1L);
    assertThat(runtime.getVariable(process.getId(), "endExecutions")).isEqualTo(1L);
    complete(process, "taskB");
    helper.assertProcessEnded(process.getId());
  }

  @Test
  void rejectScopeCollapseWithShadowedLocalsWithoutChangingPendingState() {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(adHoc(task("${input}", "${localInput}")));
    ProcessDefinition target = deploy(adHoc("<userTask id=\"taskA\" operaton:asyncAfter=\"true\"/>"));
    ProcessInstance process = runtime.startProcessInstanceById(source.getId(), initialVariables());
    String scope = runtime.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").singleResult().getId();
    runtime.triggerAdHocActivities(scope, List.of("taskA"), null);
    complete(process, "taskA");
    Job job = asyncJob(process);
    String carrier = parentExecutionId(job.getExecutionId());
    runtime.setVariableLocal(carrier, "shadow", "carrier");
    runtime.setVariableLocal(job.getExecutionId(), "shadow", "scope");
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapEqualActivities().build();

    assertThatThrownBy(() -> runtime.newMigration(plan).processInstanceIds(process.getId()).execute())
        .isInstanceOf(MigratingProcessInstanceValidationException.class)
        .hasMessageContaining("Migrating to a non-scope activity would overwrite one of them");

    assertThat(asyncJob(process).getExecutionId()).isEqualTo(job.getExecutionId());
    assertThat(asyncJob(process).getProcessDefinitionId()).isEqualTo(source.getId());
    assertThat(runtime.getVariableLocal(carrier, "shadow")).isEqualTo("carrier");
    assertThat(runtime.getVariableLocal(job.getExecutionId(), "shadow")).isEqualTo("scope");
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(0);
    engine.getManagementService().executeJob(job.getId());
    runtime.completeAdHocSubProcess(scope);
    complete(process, "after");
    helper.assertProcessEnded(process.getId());
  }

  @Test
  void gainingScopeCreatesTargetBoundaryDependenciesWithoutReplayingInput() {
    RuntimeService runtime = engine.getRuntimeService();
    ProcessDefinition source = deploy(adHoc("<userTask id=\"taskA\" operaton:asyncAfter=\"true\"/>"));
    String targetChildren = task("${inputMustNotBeReplayed}", "${input}")
        + "<boundaryEvent id=\"timer\" attachedToRef=\"taskA\"><timerEventDefinition>"
        + "<timeDuration>PT1H</timeDuration></timerEventDefinition></boundaryEvent>"
        + "<boundaryEvent id=\"message\" attachedToRef=\"taskA\"><messageEventDefinition messageRef=\"notice\"/>"
        + "</boundaryEvent>";
    ProcessDefinition target = deploy(adHoc(targetChildren));
    ProcessInstance process = runtime.startProcessInstanceById(source.getId(), initialVariables());
    String scope = runtime.createExecutionQuery().processInstanceId(process.getId()).activityId("adhoc").singleResult().getId();
    runtime.triggerAdHocActivities(scope, List.of("taskA"), null);
    complete(process, "taskA");
    Job job = asyncJob(process);
    MigrationPlan plan = runtime.createMigrationPlan(source.getId(), target.getId()).mapEqualActivities().build();

    runtime.newMigration(plan).processInstanceIds(process.getId()).execute();

    Job migrated = asyncJob(process);
    assertThat(runtime.getVariableLocal(migrated.getExecutionId(), "localInput")).isNull();
    assertThat(runtime.createEventSubscriptionQuery().processInstanceId(process.getId()).eventName("notice")
        .singleResult().getExecutionId()).isEqualTo(migrated.getExecutionId());
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId()).timers()
        .singleResult().getExecutionId()).isEqualTo(migrated.getExecutionId());
    engine.getManagementService().executeJob(job.getId());
    assertThat(runtime.getVariable(process.getId(), "mappedOutput")).isEqualTo("retained");
    assertThat(runtime.getVariable(process.getId(), "outputExecutions")).isEqualTo(1L);
    assertThat(runtime.getVariable(process.getId(), "endExecutions")).isEqualTo(0);
    assertThat(runtime.createEventSubscriptionQuery().processInstanceId(process.getId()).count()).isZero();
    assertThat(engine.getManagementService().createJobQuery().processInstanceId(process.getId()).count()).isZero();
    runtime.completeAdHocSubProcess(scope);
    complete(process, "after");
    helper.assertProcessEnded(process.getId());
  }

  private String parentExecutionId(String executionId) {
    return engine.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(context -> {
      ExecutionEntity execution = context.getExecutionManager().findExecutionById(executionId);
      return execution.getParentId();
    });
  }

  private Map<String, Object> initialVariables() {
    return Map.of("input", "retained", "mappedOutput", "", "outputExecutions", 0, "endExecutions", 0);
  }

  private String task(String input, String output) {
    return "<userTask id=\"taskA\" operaton:asyncAfter=\"true\"><extensionElements>"
        + "<operaton:executionListener event=\"end\" expression=\"${execution.setVariable('endExecutions', endExecutions + 1)}\"/>"
        + "<operaton:inputOutput><operaton:inputParameter name=\"localInput\">" + input + "</operaton:inputParameter>"
        + "<operaton:outputParameter name=\"mappedOutput\">" + output + "</operaton:outputParameter>"
        + "<operaton:outputParameter name=\"ignored\">${execution.setVariable('outputExecutions', outputExecutions + 1)}"
        + "</operaton:outputParameter></operaton:inputOutput></extensionElements></userTask>";
  }

  private String wrapper(String children) {
    return "<subProcess id=\"inner\"><startEvent id=\"innerStart\"/>"
        + "<sequenceFlow id=\"innerEnter\" sourceRef=\"innerStart\" targetRef=\"taskA\"/>" + children
        + "<sequenceFlow id=\"innerNext\" sourceRef=\"taskA\" targetRef=\"taskB\"/><userTask id=\"taskB\"/>"
        + "<sequenceFlow id=\"innerFinish\" sourceRef=\"taskB\" targetRef=\"innerEnd\"/><endEvent id=\"innerEnd\"/></subProcess>";
  }

  private String adHoc(String children) {
    return definitions("<process id=\"process\" isExecutable=\"true\"><startEvent id=\"start\"/>"
        + "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/><adHocSubProcess id=\"adhoc\">"
        + children + "<completionCondition>${false}</completionCondition></adHocSubProcess>"
        + "<sequenceFlow id=\"leave\" sourceRef=\"adhoc\" targetRef=\"after\"/><userTask id=\"after\"/>"
        + "<sequenceFlow id=\"finish\" sourceRef=\"after\" targetRef=\"end\"/><endEvent id=\"end\"/></process>");
  }

  private String definitions(String process) {
    return "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
        + "xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\" targetNamespace=\"scoped-end-migration\">"
        + "<message id=\"notice\" name=\"notice\"/>" + process + "</definitions>";
  }

  private ProcessDefinition deploy(String xml) {
    return helper.deployAndGetDefinition(Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  private Job asyncJob(ProcessInstance process) {
    return engine.getManagementService().createJobQuery().processInstanceId(process.getId()).messages().singleResult();
  }

  private void complete(ProcessInstance process, String activity) {
    Task task = engine.getTaskService().createTaskQuery().processInstanceId(process.getId()).taskDefinitionKey(activity).singleResult();
    assertThat(task).isNotNull();
    engine.getTaskService().complete(task.getId());
  }
}
