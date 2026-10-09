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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocStartability;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdHocConditionalEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  HistoryService historyService;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void dispatchTakeListenerVariableEventBeforeParkingEnabledActivity(boolean interrupting) {
    String scope = start(interrupting, "", """
        <extensionElements>
          <operaton:executionListener event="take" expression="${execution.setVariable('flag', true)}"/>
        </extensionElements>
        """, false);

    completeSourceAndAssertConditionalBoundary(scope, interrupting);
  }

  @ParameterizedTest
  @CsvSource({"true,false", "false,false", "true,true", "false,true"})
  void dispatchOutputMappingVariableEventBeforeParkingEnabledActivity(boolean interrupting,
      boolean completionCondition) {
    String scope = start(interrupting, """
        <extensionElements>
          <operaton:inputOutput>
            <operaton:outputParameter name="flag">${true}</operaton:outputParameter>
          </operaton:inputOutput>
        </extensionElements>
        """, "", completionCondition);

    completeSourceAndAssertConditionalBoundary(scope, interrupting);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void dispatchEnabledCountVariableEventAfterParkingActivity(boolean interrupting) {
    String scope = startModel(interrupting, "", """
        <userTask id="a"/><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
        <completionCondition>${false}</completionCondition>
        """, "nrOfEnabledAdHocActivities",
        "${execution.getVariable('nrOfEnabledAdHocActivities') == 1}", Map.of());

    completeSourceAndAssertConditionalBoundary(scope, interrupting);
  }

  @ParameterizedTest
  @CsvSource({"true,true", "false,true", "true,false", "false,false"})
  void dispatchOutputMappingVariableEventWhileAnotherActivityKeepsScopeOpen(boolean interrupting,
      boolean completionCondition) {
    String scope = startModel(interrupting, "cancelRemainingInstances=\"false\"", """
        <userTask id="a"><extensionElements><operaton:inputOutput>
          <operaton:outputParameter name="flag">${true}</operaton:outputParameter>
        </operaton:inputOutput></extensionElements></userTask>
        <userTask id="c"/>
        %s
        """.formatted(completionCondition ? """
            <userTask id="b"/>
            <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
            <completionCondition>${nrOfCompletedAdHocActivities > 0}</completionCondition>
            """ : ""), "flag", "${flag}", Map.of("flag", false));
    runtimeService.triggerAdHocActivities(scope, List.of("a", "c"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isZero();

    taskService.complete(taskService.createTaskQuery().taskDefinitionKey("a").singleResult().getId());

    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertThat(runtimeService.createVariableInstanceQuery()
        .variableName(AdHocStartability.AD_HOC_ENABLED_ACTIVITY).count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isZero();
    if (interrupting) {
      assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isZero();
      assertThat(taskService.createTaskQuery().taskDefinitionKey("c").count()).isZero();
    } else {
      assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isEqualTo(1);
      assertThat(taskService.createTaskQuery().taskDefinitionKey("c").count()).isEqualTo(1);
      if (completionCondition) {
        assertThat(runtimeService.getStartableAdHocActivities(scope)).isEmpty();
      }
      taskService.complete(taskService.createTaskQuery().taskDefinitionKey("c").singleResult().getId());
      assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isZero();
      assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
      assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isEqualTo(1);
    }
  }

  @Test
  void rollBackSourceCompletionAndEnabledStateWhenConditionalHandlerFails() {
    String scope = startModel(true, "", """
        <userTask id="a"/><userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b"><extensionElements>
          <operaton:executionListener event="take" expression="${execution.setVariable('flag', true)}"/>
        </extensionElements></sequenceFlow>
        """, "flag", "${flag}", Map.of("flag", false, "failHandler", true), """
        <extensionElements>
          <operaton:executionListener event="start" class="%s"/>
        </extensionElements>
        """.formatted(FailingConditionalHandler.class.getName()));
    runtimeService.triggerAdHocActivities(scope, List.of("a"), Map.of("a", Map.of("payload", "retained")));
    Task sourceTask = taskService.createTaskQuery().taskDefinitionKey("a").singleResult();
    String sourceExecution = sourceTask.getExecutionId();

    assertThatThrownBy(() -> taskService.complete(sourceTask.getId()))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("conditional handler failed");

    assertThat(taskService.createTaskQuery().taskId(sourceTask.getId()).singleResult().getExecutionId())
        .isEqualTo(sourceExecution);
    assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isEqualTo(1);
    assertThat(runtimeService.getVariable(scope, "flag")).isEqualTo(false);
    assertThat(runtimeService.getVariableLocal(sourceExecution, "payload")).isEqualTo("retained");
    assertThat(runtimeService.createVariableInstanceQuery()
        .variableName(AdHocStartability.AD_HOC_ENABLED_ACTIVITY).count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").unfinished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").finished().count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("handled").count()).isZero();

    runtimeService.setVariable(scope, "failHandler", false);
    taskService.complete(sourceTask.getId());

    assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isZero();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").finished().count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  private void completeSourceAndAssertConditionalBoundary(String scope, boolean interrupting) {
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isZero();

    taskService.complete(taskService.createTaskQuery().taskDefinitionKey("a").singleResult().getId());

    // Variable events must be dispatched in the same command, without a later trigger or variable update.
    assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("a").finished().count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isZero();

    if (interrupting) {
      assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isZero();
      assertThat(runtimeService.createVariableInstanceQuery()
          .variableName(AdHocStartability.AD_HOC_ENABLED_ACTIVITY).count()).isZero();
    } else {
      assertThat(runtimeService.createExecutionQuery().executionId(scope).count()).isEqualTo(1);
      AdHocActivity enabled = runtimeService.getStartableAdHocActivities(scope).stream()
          .filter(activity -> activity.getActivityId().equals("b"))
          .findFirst().orElseThrow();
      assertThat(enabled.getEnabledExecutionIds()).hasSize(1);
      assertThat(runtimeService.createVariableInstanceQuery()
          .variableName(AdHocStartability.AD_HOC_ENABLED_ACTIVITY).count()).isEqualTo(1);
      runtimeService.triggerAdHocActivities(scope, List.of("b"), null);
      assertThat(taskService.createTaskQuery().taskDefinitionKey("b").count()).isEqualTo(1);
      assertThat(taskService.createTaskQuery().taskDefinitionKey("handled").count()).isEqualTo(1);
    }
  }

  private String start(boolean interrupting, String taskExtensions, String flowExtensions,
      boolean completionCondition) {
    String contents = """
        <userTask id="a">%s</userTask>
        <userTask id="b"/>
        <sequenceFlow id="ab" sourceRef="a" targetRef="b">%s</sequenceFlow>
        %s
        """.formatted(taskExtensions, flowExtensions,
            completionCondition ? "<completionCondition>${false}</completionCondition>" : "");
    return startModel(interrupting, "", contents, "flag", "${flag}", Map.of("flag", false));
  }

  private String startModel(boolean interrupting, String attributes, String contents,
      String conditionalVariable, String boundaryCondition, Map<String, Object> variables) {
    return startModel(interrupting, attributes, contents, conditionalVariable, boundaryCondition, variables, "");
  }

  private String startModel(boolean interrupting, String attributes, String contents,
      String conditionalVariable, String boundaryCondition, Map<String, Object> variables, String handlerExtensions) {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="test">
          <process id="adHocConditionalEvents" isExecutable="true" operaton:historyTimeToLive="180">
            <startEvent id="start"/>
            <sequenceFlow id="enter" sourceRef="start" targetRef="adhoc"/>
            <adHocSubProcess id="adhoc" %s>%s</adHocSubProcess>
            <boundaryEvent id="conditionalBoundary" attachedToRef="adhoc" cancelActivity="%s">
              <conditionalEventDefinition operaton:variableName="%s" operaton:variableEvents="update">
                <condition>%s</condition>
              </conditionalEventDefinition>
            </boundaryEvent>
            <sequenceFlow id="conditionalFlow" sourceRef="conditionalBoundary" targetRef="handled"/>
            <userTask id="handled">%s</userTask>
            <sequenceFlow id="leave" sourceRef="adhoc" targetRef="after"/>
            <userTask id="after"/>
          </process>
        </definitions>
        """.formatted(attributes, contents, interrupting, conditionalVariable, boundaryCondition, handlerExtensions);
    testRule.deploy(repositoryService.createDeployment().addString("adHocConditionalEvents.bpmn20.xml", xml));
    runtimeService.startProcessInstanceByKey("adHocConditionalEvents", variables);
    return runtimeService.createExecutionQuery().activityId("adhoc").singleResult().getId();
  }

  public static class FailingConditionalHandler implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
      if (Boolean.TRUE.equals(execution.getVariable("failHandler"))) {
        throw new ProcessEngineException("conditional handler failed");
      }
    }
  }
}
