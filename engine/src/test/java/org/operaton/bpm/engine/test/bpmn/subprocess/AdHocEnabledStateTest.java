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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.SuspendedEntityInteractionException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdHocEnabledStateTest {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  ManagementService managementService;
  HistoryService historyService;

  @ParameterizedTest
  @ValueSource(strings = {"Sequential", "Parallel"})
  void downstreamActivityIsEnabledWithoutBeingStarted(String ordering) {
    String scope = start("ordering=\"" + ordering + "\"", chain(), Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    assertThat(enabled(scope, "b").isStarterActivity()).isFalse();
    assertThat(enabled(scope, "a").isStarterActivity()).isTrue();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    trigger(scope, "b");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isEqualTo(1);
    complete("b");
    assertAfter();
  }

  @Test
  void sequentialFanOutOffersChoicesAndRejectsSecondRunningActivity() {
    String scope = start("ordering=\"Sequential\"", fanOut(), Map.of());
    trigger(scope, "a");
    complete("a");
    String pendingC = enabled(scope, "c").getEnabledExecutionIds().get(0);
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "b");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    assertThatThrownBy(() -> trigger(scope, "c")).isInstanceOf(BadUserRequestException.class);
    assertThat(runtimeService.createExecutionQuery().executionId(pendingC).count()).isEqualTo(1);
    complete("b");
    trigger(scope, "c");
    complete("c");
    assertAfter();
  }

  @Test
  void parallelFanOutCanActivateEnabledBatch() {
    String scope = start("", fanOut(), Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(taskService.createTaskQuery().count()).isZero();
    runtimeService.triggerAdHocActivities(scope, List.of("b", "c"), null);
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    complete("b");
    complete("c");
    assertAfter();
  }

  @Test
  void parallelJoinWaitsForBothTokensBeforeEnablingTarget() {
    String scope = start("ordering=\"Sequential\"", """
        <userTask id="a"/><userTask id="b"/><userTask id="c"/>
        <parallelGateway id="join"/>
        <sequenceFlow id="aj" sourceRef="a" targetRef="join"/>
        <sequenceFlow id="bj" sourceRef="b" targetRef="join"/>
        <sequenceFlow id="jc" sourceRef="join" targetRef="c"/>
        """, Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .doesNotContain("c");
    trigger(scope, "b");
    complete("b");
    assertThat(enabled(scope, "c").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "c");
    complete("c");
    assertAfter();
  }

  @Test
  void loopReenablesActivityWithPersistedToken() {
    String scope = start("ordering=\"Sequential\"", """
        <task id="seed"/><userTask id="a"/><userTask id="b"/><exclusiveGateway id="choice" default="done"/>
        <sequenceFlow id="seedA" sourceRef="seed" targetRef="a"/>
        <sequenceFlow id="aChoice" sourceRef="a" targetRef="choice"/>
        <sequenceFlow id="again" sourceRef="choice" targetRef="a"><conditionExpression>${again}</conditionExpression></sequenceFlow>
        <sequenceFlow id="done" sourceRef="choice" targetRef="b"/>
        """, Map.of("again", true));
    trigger(scope, "seed");
    assertThat(enabled(scope, "a").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "a");
    complete("a");
    assertThat(enabled(scope, "a").getEnabledExecutionIds()).hasSize(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").finished().count()).isEqualTo(1);
    runtimeService.setVariable(scope, "again", false);
    trigger(scope, "a");
    complete("a");
    trigger(scope, "b");
    complete("b");
    assertAfter();
  }

  @Test
  void asyncBeforeIsCreatedOnlyWhenEnabledActivityIsActivated() {
    String scope = start("ordering=\"Sequential\"", chain().replace("id=\"b\"", "id=\"b\" operaton:asyncBefore=\"true\""), Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "b");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    complete("b");
    assertAfter();
  }

  @Test
  void repeatedArrivalsRetainSeparateTokensAndConsumeDeterministically() {
    String scope = start("", chain(), Map.of());
    trigger(scope, "a");
    trigger(scope, "a");
    for (Task task : taskService.createTaskQuery().taskDefinitionKey("a").list()) {
      taskService.complete(task.getId());
    }
    List<String> tokens = enabled(scope, "b").getEnabledExecutionIds();
    assertThat(tokens).hasSize(2).isSorted();
    trigger(scope, "b");
    assertThat(taskService.createTaskQuery().singleResult().getExecutionId()).isEqualTo(tokens.get(0));
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).containsExactly(tokens.get(1));
    complete("b");
    trigger(scope, "b");
    complete("b");
    assertAfter();
  }

  @Test
  void invalidBatchDoesNotConsumeEnabledTokensOrWriteVariables() {
    String scope = start("", chain(), Map.of());
    trigger(scope, "a");
    complete("a");
    List<String> tokens = enabled(scope, "b").getEnabledExecutionIds();
    assertThatThrownBy(() -> runtimeService.triggerAdHocActivities(scope, List.of("b", "missing"),
        Map.of("b", Map.of("unwanted", true)))).isInstanceOf(BadUserRequestException.class);
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).isEqualTo(tokens);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("unwanted").count()).isZero();
    assertThat(taskService.createTaskQuery().count()).isZero();
  }

  @Test
  void suspendedActivationPreservesReadyToken() {
    String scope = start("", chain(), Map.of());
    trigger(scope, "a");
    complete("a");
    List<String> tokens = enabled(scope, "b").getEnabledExecutionIds();
    String process = runtimeService.createExecutionQuery().executionId(scope).singleResult().getProcessInstanceId();
    runtimeService.suspendProcessInstanceById(process);
    assertThatThrownBy(() -> trigger(scope, "b")).isInstanceOf(SuspendedEntityInteractionException.class);
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).isEqualTo(tokens);
    runtimeService.activateProcessInstanceById(process);
    trigger(scope, "b");
    complete("b");
    assertAfter();
  }

  @Test
  void explicitCompletionDiscardsEnabledButNotRunningWorkWithoutCancellation() {
    String scope = start("cancelRemainingInstances=\"false\"", chain(), Map.of());
    trigger(scope, "a");
    complete("a");
    runtimeService.completeAdHocSubProcess(scope);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertAfter();
  }

  @Test
  void completionDecisionDiscardsEnabledChoicesWhileDraining() {
    String scope = start("cancelRemainingInstances=\"false\"", chain() + """
        <userTask id="c"/>
        <completionCondition>${nrOfCompletedAdHocActivities == 2}</completionCondition>
        """, Map.of());
    runtimeService.triggerAdHocActivities(scope, List.of("a", "c"), null);
    complete("a");
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    complete("c");
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertAfter();
  }

  @Test
  void timerRoutesToEnabledActivityWithoutActivatingIt() {
    String scope = start("", """
        <userTask id="a"/><intermediateCatchEvent id="timer"><timerEventDefinition>
          <timeDuration>PT1H</timeDuration></timerEventDefinition></intermediateCatchEvent><userTask id="b"/>
        <sequenceFlow id="at" sourceRef="a" targetRef="timer"/>
        <sequenceFlow id="tb" sourceRef="timer" targetRef="b"/>
        """, Map.of());
    trigger(scope, "a");
    complete("a");
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "b");
    complete("b");
    assertAfter();
  }

  @Test
  void enabledMultiInstanceBodyStartsOnlyOnActivation() {
    String scope = start("", """
        <task id="seed"/><userTask id="a"><multiInstanceLoopCharacteristics isSequential="false">
          <loopCardinality>2</loopCardinality></multiInstanceLoopCharacteristics></userTask>
        <sequenceFlow id="seedA" sourceRef="seed" targetRef="a"/>
        """, Map.of());
    trigger(scope, "seed");
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(enabled(scope, "a").getEnabledExecutionIds()).hasSize(1);
    assertThatThrownBy(() -> trigger(scope, "a#multiInstanceBody"))
        .isInstanceOf(BadUserRequestException.class).hasMessageContaining("not startable");
    assertThat(enabled(scope, "a").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "a");
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }
    assertAfter();
  }

  @Test
  void enabledTokenSurvivesEngineRestart() {
    String jdbc = "jdbc:h2:mem:adhoc-enabled-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    String deployment;
    String scope;
    List<String> tokens;
    ProcessEngine first = newEngine(jdbc);
    try {
      deployment = first.getRepositoryService().createDeployment().addString("enabled.bpmn20.xml", xml("", chain())).deploy().getId();
      first.getRuntimeService().startProcessInstanceByKey("enabledState");
      scope = first.getRuntimeService().createExecutionQuery().activityId("adhoc").singleResult().getId();
      first.getRuntimeService().triggerAdHocActivities(scope, List.of("a"), null);
      first.getTaskService().complete(first.getTaskService().createTaskQuery().singleResult().getId());
      tokens = first.getRuntimeService().getStartableAdHocActivities(scope).stream()
          .filter(activity -> activity.getActivityId().equals("b")).findFirst().orElseThrow().getEnabledExecutionIds();
    } finally {
      first.close();
    }
    ProcessEngine second = newEngine(jdbc);
    try {
      assertThat(second.getRuntimeService().getStartableAdHocActivities(scope).stream()
          .filter(activity -> activity.getActivityId().equals("b")).findFirst().orElseThrow().getEnabledExecutionIds())
          .isEqualTo(tokens);
      second.getRuntimeService().triggerAdHocActivities(scope, List.of("b"), null);
      second.getTaskService().complete(second.getTaskService().createTaskQuery().singleResult().getId());
      assertThat(second.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo("after");
      second.getRepositoryService().deleteDeployment(deployment, true);
    } finally {
      second.close();
    }
  }


  @Test
  void inclusiveJoinWaitsForEnabledBranchUnderSequentialOrdering() {
    String scope = start("ordering=\"Sequential\"", """
        <userTask id="a"/><userTask id="b"/><userTask id="c"/><userTask id="d"/>
        <inclusiveGateway id="split"/><inclusiveGateway id="join"/>
        <sequenceFlow id="as" sourceRef="a" targetRef="split"/>
        <sequenceFlow id="sb" sourceRef="split" targetRef="b"/>
        <sequenceFlow id="sc" sourceRef="split" targetRef="c"/>
        <sequenceFlow id="bj" sourceRef="b" targetRef="join"/>
        <sequenceFlow id="cj" sourceRef="c" targetRef="join"/>
        <sequenceFlow id="jd" sourceRef="join" targetRef="d"/>
        """, Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(enabled(scope, "b").getEnabledExecutionIds()).hasSize(1);
    assertThat(enabled(scope, "c").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "b");
    complete("b");
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting(AdHocActivity::getActivityId)
        .doesNotContain("d");
    trigger(scope, "c");
    complete("c");
    assertThat(enabled(scope, "d").getEnabledExecutionIds()).hasSize(1);
    trigger(scope, "d");
    complete("d");
    assertAfter();
  }

  @Test
  void nestedAdHocEnabledActivitiesRetainTheirOwnActivationOwner() {
    String scope = start("ordering=\"Sequential\"", """
        <userTask id="a"/>
        <adHocSubProcess id="nested" ordering="Sequential">
          <userTask id="innerA"/><userTask id="innerB"/>
          <sequenceFlow id="innerFlow" sourceRef="innerA" targetRef="innerB"/>
        </adHocSubProcess>
        <sequenceFlow id="an" sourceRef="a" targetRef="nested"/>
        """, Map.of());
    trigger(scope, "a");
    complete("a");
    assertThat(enabled(scope, "nested").getEnabledExecutionIds()).hasSize(1);
    String parked = enabled(scope, "nested").getEnabledExecutionIds().get(0);
    assertNotEnteredOwner(parked, "innerA");
    assertThat(enabled(scope, "nested").getEnabledExecutionIds()).containsExactly(parked);
    trigger(scope, "nested");
    String nested = runtimeService.createExecutionQuery().activityId("nested").singleResult().getId();
    trigger(nested, "innerA");
    complete("innerA");
    assertThat(enabled(nested, "innerB").getEnabledExecutionIds()).hasSize(1);
    assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
    assertThatThrownBy(() -> trigger(scope, "innerB")).isInstanceOf(BadUserRequestException.class);
    trigger(nested, "innerB");
    complete("innerB");
    assertAfter();
  }

  @Test
  void enabledTaskDoesNotRunInputMappingsOrCreateBoundarySubscriptions() {
    String scope = start("", """
        <userTask id="a"/>
        <userTask id="b"><extensionElements><operaton:inputOutput>
          <operaton:inputParameter name="selectedValue">${selectionValue}</operaton:inputParameter>
        </operaton:inputOutput></extensionElements></userTask>
        <boundaryEvent id="timer" attachedToRef="b"><timerEventDefinition>
          <timeDuration>PT1H</timeDuration>
        </timerEventDefinition></boundaryEvent>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        """, Map.of("selectionValue", "before"));
    trigger(scope, "a");
    complete("a");
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery().variableName("selectedValue").count()).isZero();
    runtimeService.setVariable(scope, "selectionValue", "selected");
    trigger(scope, "b");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("selectedValue").singleResult().getValue())
        .isEqualTo("selected");
    complete("b");
    assertThat(managementService.createJobQuery().count()).isZero();
    assertAfter();
  }

  @Test
  void asyncBeforeAdHocCannotBeUsedBeforeItsScopeIsEntered() {
    String pending = start("operaton:asyncBefore=\"true\"", chain(), Map.of());
    assertNotEnteredOwner(pending, "a");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    String scope = runtimeService.createExecutionQuery().activityId("adhoc").singleResult().getId();
    trigger(scope, "a");
    complete("a");
    trigger(scope, "b");
    complete("b");
    assertAfter();
  }

  @Test
  void asyncAfterAdHocCannotBeReopenedAfterItsScopeHasEnded() {
    String scope = start("operaton:asyncAfter=\"true\"", chain(), Map.of());
    trigger(scope, "a");
    complete("a");
    trigger(scope, "b");
    complete("b");
    String pending = managementService.createJobQuery().singleResult().getExecutionId();
    assertNotEnteredOwner(pending, "a");
    managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    assertAfter();
  }

  private void assertNotEnteredOwner(String execution, String activity) {
    assertThatThrownBy(() -> runtimeService.getStartableAdHocActivities(execution))
        .isInstanceOf(BadUserRequestException.class);
    assertThatThrownBy(() -> trigger(execution, activity)).isInstanceOf(BadUserRequestException.class);
    assertThatThrownBy(() -> runtimeService.completeAdHocSubProcess(execution, Map.of("mustNotWrite", true)))
        .isInstanceOf(BadUserRequestException.class);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("mustNotWrite").count()).isZero();
  }

  private ProcessEngine newEngine(String jdbc) {
    return ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
        .setProcessEngineName("enabled-restart")
        .setJdbcUrl(jdbc).setDatabaseSchemaUpdate("true").setJobExecutorActivate(false).buildProcessEngine();
  }

  private String start(String attributes, String contents, Map<String, Object> variables) {
    testRule.deploy(repositoryService.createDeployment().addString("enabled.bpmn20.xml", xml(attributes, contents)));
    runtimeService.startProcessInstanceByKey("enabledState", variables);
    return runtimeService.createExecutionQuery().activityId("adhoc").singleResult().getId();
  }

  private String xml(String attributes, String contents) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="test">
          <process id="enabledState" isExecutable="true" operaton:historyTimeToLive="180">
            <startEvent id="start"/><sequenceFlow id="enter" sourceRef="start" targetRef="adhoc"/>
            <adHocSubProcess id="adhoc" %s>%s</adHocSubProcess>
            <sequenceFlow id="leave" sourceRef="adhoc" targetRef="after"/><userTask id="after"/>
          </process>
        </definitions>
        """.formatted(attributes, contents);
  }

  private String chain() {
    return "<userTask id=\"a\"/><userTask id=\"b\"/><sequenceFlow id=\"ab\" sourceRef=\"a\" targetRef=\"b\"/>";
  }

  private String fanOut() {
    return chain() + "<userTask id=\"c\"/><sequenceFlow id=\"ac\" sourceRef=\"a\" targetRef=\"c\"/>";
  }

  private void trigger(String scope, String activity) {
    runtimeService.triggerAdHocActivities(scope, List.of(activity), null);
  }

  private void complete(String activity) {
    taskService.complete(taskService.createTaskQuery().taskDefinitionKey(activity).singleResult().getId());
  }

  private AdHocActivity enabled(String scope, String activity) {
    return runtimeService.getStartableAdHocActivities(scope).stream()
        .filter(candidate -> candidate.getActivityId().equals(activity)).findFirst().orElseThrow();
  }

  private void assertAfter() {
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }
}
