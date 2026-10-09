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
package org.operaton.bpm.engine.test.bpmn.subprocess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdHocSubProcessLifecycleTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  ManagementService managementService;
  HistoryService historyService;

  @Test
  void completionStopsOutgoingSequenceFlow() {
    String scope = start("cancelRemainingInstances=\"true\"", """
        <userTask id="a"/><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void drainingScopeRejectsNewStartsAndRemainsAddressable() {
    String scope = start("cancelRemainingInstances=\"false\"", """
        <userTask id="a"/><userTask id="b"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    complete("a");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(scope, List.of("a"), null))
        .isInstanceOf(BadUserRequestException.class).hasMessageContaining("completion");
    assertThat(runtimeService.createExecutionQuery().executionId(scope).activityId("adhoc").count()).isEqualTo(1);
    complete("b");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void remainingParallelChildDoesNotReplaceAdHocScope() {
    String scope = start("operaton:autoComplete=\"false\"", "<userTask id=\"a\"/><userTask id=\"b\"/>");
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    complete("a");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).hasSize(2);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    complete("a");
    complete("b");
    runtimeService.completeAdHocSubProcess(scope);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void nestedDescendantCannotBeTriggeredFromOuterScope() {
    String scope = start("", """
        <subProcess id="nested"><startEvent id="nestedStart"/><userTask id="nestedTask"/>
        <sequenceFlow id="nestedFlow" sourceRef="nestedStart" targetRef="nestedTask"/></subProcess>
        """);
    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(scope, List.of("nestedTask"), null))
        .isInstanceOf(BadUserRequestException.class).hasMessageContaining("not startable");
    assertThat(taskService.createTaskQuery().count()).isZero();
  }

  @Test
  void synchronousBatchReservesAllActivitiesBeforeStarting() {
    String scope = start("", "<task id=\"a\"/><userTask id=\"b\"/>");
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isZero();
    complete("b");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void synchronousCompletionCancelsReservedBatchMembers() {
    String scope = start("cancelRemainingInstances=\"true\"", """
        <task id="a"/><userTask id="b"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void completionCancelsAsyncJobsAndLocalVariables() {
    String scope = start("cancelRemainingInstances=\"true\"", """
        <userTask id="a"/><userTask id="b" operaton:asyncBefore="true"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), Map.of("b", Map.of("localValue", "value")));
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    complete("a");
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery().variableName("localValue").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void nestedAdHocActivityCanBeDiscoveredAndTriggered() {
    String outer = start("", "<adHocSubProcess id=\"inner\"><userTask id=\"a\"/></adHocSubProcess>");
    assertThat(runtimeService.getStartableAdHocActivities(outer)).hasSize(1);
    runtimeService.triggerAdHocActivities(outer, List.of("inner"), null);
    Execution inner = runtimeService.createExecutionQuery().activityId("inner").singleResult();
    runtimeService.triggerAdHocActivities(inner.getId(), List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void completionTakesPrecedenceOverFalseOutgoingCondition() {
    String scope = start("", """
        <userTask id="a"/><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b">
          <conditionExpression>${false}</conditionExpression>
        </sequenceFlow>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void outgoingCompletionPreservesAsyncAfterBoundary() {
    String scope = start("", """
        <userTask id="a" operaton:asyncAfter="true"/><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Test
  void outgoingCompletionUsesEndListenerAndOutputMappingExactlyOnce() {
    String scope = start("", """
        <userTask id="a"><extensionElements>
          <operaton:executionListener event="end" expression="${execution.setVariable('endRuns', endRuns + 1)}"/>
          <operaton:inputOutput>
            <operaton:inputParameter name="answer">yes</operaton:inputParameter>
            <operaton:outputParameter name="done">${answer == 'yes'}</operaton:outputParameter>
          </operaton:inputOutput>
        </extensionElements></userTask><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <completionCondition>${done &amp;&amp; endRuns == 1}</completionCondition>
        """, Map.of("done", false, "endRuns", 0));
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    Task after = taskService.createTaskQuery().taskDefinitionKey("after").singleResult();
    assertThat(after).isNotNull();
    assertThat(runtimeService.getVariable(after.getExecutionId(), "endRuns")).isEqualTo(1L);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").finished().count()).isEqualTo(1);
  }

  @Test
  void falseCompletionContinuesOutgoingFlowWithTaskLocalVariables() {
    String scope = start("", """
        <userTask id="a"><extensionElements><operaton:inputOutput>
          <operaton:inputParameter name="route">yes</operaton:inputParameter>
        </operaton:inputOutput></extensionElements></userTask><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b">
          <conditionExpression>${route == 'yes'}</conditionExpression>
        </sequenceFlow>
        <completionCondition>${nrOfCompletedAdHocActivities == 2}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    runtimeService.triggerAdHocActivities(scope, List.of("b"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    complete("b");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void multiInstanceTaskIsActivatedThroughItsBody() {
    String scope = start("", """
        <userTask id="a"><multiInstanceLoopCharacteristics isSequential="false">
          <loopCardinality>3</loopCardinality>
        </multiInstanceLoopCharacteristics></userTask>
        """);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting("activityId").containsExactly("a");
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("a").count()).isEqualTo(3);
    for (Task task : taskService.createTaskQuery().taskDefinitionKey("a").list()) {
      taskService.complete(task.getId());
    }
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void multiInstanceBodyCannotBypassModelActivityValidation() {
    String scope = start("", """
        <userTask id="a"><multiInstanceLoopCharacteristics isSequential="true">
          <loopCardinality>2</loopCardinality>
        </multiInstanceLoopCharacteristics></userTask>
        """);
    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(scope, List.of("a#multiInstanceBody"), null))
        .isInstanceOf(BadUserRequestException.class);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("a").count()).isEqualTo(1);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void terminalAsyncAfterJobSurvivesSynchronousBatchCompletion() {
    String scope = start("", "<task id=\"a\" operaton:asyncAfter=\"true\"/><task id=\"b\"/>");
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().count()).isZero();
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void parserRejectsDirectStartAndEndEvents() {
    assertThatThrownBy(() -> start("", "<startEvent id=\"invalidStart\"/><userTask id=\"a\"/>"))
        .isInstanceOf(ParseException.class).hasMessageContaining("must not contain a direct startEvent");
    assertThatThrownBy(() -> start("", "<userTask id=\"a\"/><endEvent id=\"invalidEnd\"/>"))
        .isInstanceOf(ParseException.class).hasMessageContaining("must not contain a direct endEvent");
  }

  @Test
  void parserRejectsAdHocEventSubprocess() {
    assertThatThrownBy(() -> start("triggeredByEvent=\"true\"", "<userTask id=\"a\"/>"))
        .isInstanceOf(ParseException.class).hasMessageContaining("cannot be an event subprocess");
  }

  @Test
  void falseCompletionPreservesSeparateAsyncAfterAndAsyncBeforeJobs() {
    String scope = start("", """
        <userTask id="a" operaton:asyncAfter="true"/><userTask id="b" operaton:asyncBefore="true"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 2}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    String afterJob = managementService.createJobQuery().singleResult().getId();
    managementService.executeJob(afterJob);
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(managementService.createJobQuery().count()).isZero();
    runtimeService.triggerAdHocActivities(scope, List.of("b"), null);
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().singleResult().getId()).isNotEqualTo(afterJob);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(managementService.createJobQuery().count()).isZero();
    complete("b");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void configuredSynchronousBatchCancelsRemainingWork() {
    String process = deployAndStart("", """
        <extensionElements><operaton:properties>
          <operaton:property name="activeTasksCollection" value="${initial}"/>
        </operaton:properties></extensionElements>
        <task id="a"/>
        <userTask id="b"><extensionElements>
          <operaton:executionListener event="start" expression="${execution.setVariable('wasStarted', true)}"/>
          <operaton:executionListener event="end" expression="${execution.setVariable('unexpectedEnd', true)}"/>
        </extensionElements></userTask>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """, Map.of("initial", List.of("a", "b"), "unexpectedEnd", false, "wasStarted", false));
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().processInstanceId(process).count()).isZero();
    assertThat(runtimeService.getVariable(process, "unexpectedEnd"))
        .isEqualTo(runtimeService.getVariable(process, "wasStarted"));
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count())
        .isEqualTo(Boolean.TRUE.equals(runtimeService.getVariable(process, "wasStarted")) ? 1 : 0);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("adhoc").finished().count()).isEqualTo(1);
  }

  @Test
  void latchedCompletionStopsAlreadyWaitingGatewayContinuation() {
    String scope = start("cancelRemainingInstances=\"false\"", """
        <userTask id="a"/><userTask id="b"/><userTask id="c"/>
        <exclusiveGateway id="gateway" operaton:asyncAfter="true"/>
        <sequenceFlow id="ag" sourceRef="a" targetRef="gateway"/>
        <sequenceFlow id="gc" sourceRef="gateway" targetRef="c"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 2}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    complete("a");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    complete("b");
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(2);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(taskService.createTaskQuery().taskDefinitionKey("c").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void triggeredSynchronousBatchDoesNotEndNeverStartedMembers() {
    String scope = start("", """
        <task id="a"/>
        <userTask id="b"><extensionElements>
          <operaton:executionListener event="end" expression="${execution.setVariable('unexpectedEnd', true)}"/>
        </extensionElements></userTask>
        <completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>
        """, Map.of("unexpectedEnd", false));
    String process = runtimeService.createExecutionQuery().executionId(scope).singleResult().getProcessInstanceId();
    runtimeService.triggerAdHocActivities(scope, List.of("a", "b"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
    assertThat(runtimeService.getVariable(process, "unexpectedEnd")).isEqualTo(false);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("adhoc").finished().count()).isEqualTo(1);
  }

  @Test
  void outgoingFanOutPreservesOriginalAdHocScope() {
    String scope = start("", """
        <userTask id="a"/><userTask id="b"/><userTask id="c"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <sequenceFlow id="ac" sourceRef="a" targetRef="c"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 3}</completionCondition>
        """);
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting("activityId")
        .containsExactlyInAnyOrder("a", "b", "c");
    runtimeService.triggerAdHocActivities(scope, List.of("b", "c"), null);
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    assertThat(runtimeService.createExecutionQuery().executionId(scope).activityId("adhoc").count()).isEqualTo(1);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting("activityId").containsExactly("a");
    complete("b");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).hasSize(1);
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(2);
    complete("c");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void explicitCompletionCancelsPendingTerminalAsyncAfter() {
    String scope = start("", "<task id=\"a\" operaton:asyncAfter=\"true\"/>");
    runtimeService.triggerAdHocActivities(scope, List.of("a"), Map.of("a", Map.of("localValue", "value")));
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    runtimeService.completeAdHocSubProcess(scope);
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery().variableName("localValue").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  @Test
  void routingEventDoesNotReevaluateActivityCompletionCondition() {
    String scope = start("", """
        <userTask id="a"/>
        <intermediateCatchEvent id="timer"><extensionElements>
          <operaton:executionListener event="end" expression="${execution.setVariable('done', true)}"/>
        </extensionElements><timerEventDefinition><timeDuration>PT1S</timeDuration></timerEventDefinition>
        </intermediateCatchEvent>
        <sequenceFlow id="at" sourceRef="a" targetRef="timer"/>
        <completionCondition>${done}</completionCondition>
        """, Map.of("done", false));
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isZero();
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    complete("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

  private String start(String attributes, String contents) {
    return start(attributes, contents, Map.of());
  }

  private String start(String attributes, String contents, Map<String, Object> variables) {
    String process = deployAndStart(attributes, contents, variables);
    return runtimeService.createExecutionQuery().processInstanceId(process).activityId("adhoc").singleResult().getId();
  }

  private String deployAndStart(String attributes, String contents, Map<String, Object> variables) {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="test">
          <process id="adhocLifecycle" isExecutable="true" operaton:historyTimeToLive="180">
            <startEvent id="start"/>
            <sequenceFlow id="enter" sourceRef="start" targetRef="adhoc"/>
            <adHocSubProcess id="adhoc" %s>%s</adHocSubProcess>
            <sequenceFlow id="leave" sourceRef="adhoc" targetRef="after"/>
            <userTask id="after"/>
          </process>
        </definitions>
        """.formatted(attributes, contents);
    testRule.deploy(repositoryService.createDeployment().addString("adhoc.bpmn20.xml", xml));
    return runtimeService.startProcessInstanceByKey("adhocLifecycle", variables).getId();
  }

  private void complete(String activityId) {
    Task task = taskService.createTaskQuery().taskDefinitionKey(activityId).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
  }
}
