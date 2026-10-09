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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

class AdHocActivityEndSemanticsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;
  ManagementService managementService;
  HistoryService historyService;
  ExternalTaskService externalTaskService;

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void ordinaryUnhandledErrorDoesNotTakeOutgoingFlow(boolean scoped, boolean asyncAfter) {
    String process = start(false, scoped, asyncAfter, false, true);
    taskService.handleBpmnError(task().getId(), "unhandled");
    finishAsyncAfter(asyncAfter);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process).count()).isZero();
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void adHocUnhandledErrorDoesNotCountCompletionOrEnableOutgoingFlow(boolean scoped, boolean asyncAfter) {
    String process = start(true, scoped, asyncAfter, false, true);
    String scope = scope(process);
    taskService.handleBpmnError(task().getId(), "unhandled");
    finishAsyncAfter(asyncAfter);
    assertIdleWithoutSuccessfulCompletion(scope);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void ordinaryEndListenerErrorDoesNotTakeOutgoingFlow(boolean asyncAfter) {
    String process = start(false, true, asyncAfter, true, true);
    taskService.complete(task().getId());
    finishAsyncAfter(asyncAfter);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process).count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void adHocEndListenerErrorDoesNotReuseNormalCompletionPhase(boolean asyncAfter) {
    String process = start(true, true, asyncAfter, true, true);
    String scope = scope(process);
    taskService.complete(task().getId());
    finishAsyncAfter(asyncAfter);
    assertIdleWithoutSuccessfulCompletion(scope);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void noConditionAdHocUnhandledErrorRetiresWithoutEnablingOutgoingFlow(boolean asyncAfter) {
    String process = start(true, true, asyncAfter, false, false);
    taskService.handleBpmnError(task().getId(), "unhandled");
    finishAsyncAfter(asyncAfter);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(process).activityId("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void externalTaskUnhandledErrorDoesNotBecomeSuccessfulCompletion(boolean adHoc, boolean asyncAfter) {
    String process = start(adHoc, true, asyncAfter, false, true, true, false);
    String scope = adHoc ? scope(process) : null;
    LockedExternalTask task = externalTaskService.fetchAndLock(1, "worker").topic("work", 60000).execute().get(0);
    externalTaskService.handleBpmnError(task.getId(), "worker", "unhandled");
    finishAsyncAfter(asyncAfter);
    if (adHoc) {
      assertIdleWithoutSuccessfulCompletion(scope);
    } else {
      assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process).count()).isZero();
      assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    }
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void interruptingTerminationDoesNotResumeSuccessfulCompletion(boolean pendingAsyncAfter) {
    String process = start(true, true, pendingAsyncAfter, false, true, false, true);
    if (pendingAsyncAfter) {
      taskService.complete(task().getId());
      assertThat(managementService.createJobQuery().count()).isEqualTo(1);
    }

    runtimeService.correlateMessage("stop");

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process).count()).isZero();
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("after").count()).isZero();
  }

  @Test
  void cancelAdHocActivityDoesNotCountSuccessfulCompletion() {
    String process = start(true, true, false, false, true);
    String scope = scope(process);
    // Modification intentionally removes empty ancestors. Keep another running
    // instance so this cancellation retires only one activity, not its owner.
    runtimeService.triggerAdHocActivities(scope, List.of("a"), null);
    String activityInstance = runtimeService.getActivityInstance(process).getActivityInstances("a")[0].getId();
    runtimeService.createProcessInstanceModification(process).cancelActivityInstance(activityInstance).execute();
    assertThat(runtimeService.createExecutionQuery().executionId(scope).activityId("adhoc").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    assertThat(runtimeService.getVariableLocal(scope, "adHocCompletedActivityIds")).isEqualTo(List.of());
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting("activityId").containsExactly("a");
    assertThat(taskService.createTaskQuery().taskDefinitionKey("a").count()).isEqualTo(1);
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  private void assertIdleWithoutSuccessfulCompletion(String scope) {
    assertThat(runtimeService.createExecutionQuery().executionId(scope).activityId("adhoc").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(scope, "nrOfCompletedAdHocActivities")).isEqualTo(0);
    assertThat(runtimeService.getVariableLocal(scope, "adHocCompletedActivityIds")).isEqualTo(List.of());
    assertThat(runtimeService.getStartableAdHocActivities(scope)).extracting("activityId").containsExactly("a");
    assertThat(taskService.createTaskQuery().count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().activityId("b").count()).isZero();
  }

  private void finishAsyncAfter(boolean asyncAfter) {
    if (asyncAfter) {
      assertThat(managementService.createJobQuery().count()).isEqualTo(1);
      managementService.executeJob(managementService.createJobQuery().singleResult().getId());
    }
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  private Task task() {
    Task task = taskService.createTaskQuery().taskDefinitionKey("a").singleResult();
    assertThat(task).isNotNull();
    return task;
  }

  private String scope(String process) {
    return runtimeService.createExecutionQuery().processInstanceId(process).activityId("adhoc").singleResult().getId();
  }

  private String start(boolean adHoc, boolean scoped, boolean asyncAfter, boolean listenerError, boolean condition) {
    return start(adHoc, scoped, asyncAfter, listenerError, condition, false, false);
  }

  private String start(boolean adHoc, boolean scoped, boolean asyncAfter, boolean listenerError, boolean condition,
      boolean externalTask, boolean terminatingEventSubprocess) {
    String extensions = scoped || listenerError ? "<extensionElements>" : "";
    if (scoped) {
      // Input mapping alone establishes an ordinary activity scope. Keep its
      // preexisting process-end output-cleanup defect outside this regression.
      String output = adHoc
          ? "<operaton:outputParameter name=\"mappedValue\">${localValue}</operaton:outputParameter>" : "";
      extensions += """
          <operaton:inputOutput><operaton:inputParameter name="localValue">value</operaton:inputParameter>
            %s
          </operaton:inputOutput>
          """.formatted(output);
    }
    if (listenerError) {
      extensions += "<operaton:executionListener event=\"end\" class=\"" + ThrowOnceBpmnError.class.getName() + "\"/>";
    }
    if (scoped || listenerError) {
      extensions += "</extensionElements>";
    }
    String activityType = externalTask ? "serviceTask" : "userTask";
    String externalAttributes = externalTask ? " operaton:type=\"external\" operaton:topic=\"work\"" : "";
    String activities = "<" + activityType + " id=\"a\" operaton:asyncAfter=\"" + asyncAfter + "\""
        + externalAttributes + ">" + extensions + "</" + activityType + ">"
        + "<sequenceFlow id=\"ab\" sourceRef=\"a\" targetRef=\"b\"/><userTask id=\"b\"/>";
    String body;
    if (adHoc) {
      body = "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"adhoc\"/>"
          + "<adHocSubProcess id=\"adhoc\">" + activities
          + (condition ? "<completionCondition>${nrOfCompletedAdHocActivities == 1}</completionCondition>" : "")
          + "</adHocSubProcess><sequenceFlow id=\"leave\" sourceRef=\"adhoc\" targetRef=\"after\"/>"
          + "<userTask id=\"after\"/>";
    } else {
      body = "<sequenceFlow id=\"enter\" sourceRef=\"start\" targetRef=\"a\"/>" + activities
          + "<sequenceFlow id=\"leave\" sourceRef=\"b\" targetRef=\"end\"/><endEvent id=\"end\"/>";
    }
    if (terminatingEventSubprocess) {
      body += """
          <subProcess id="stopHandler" triggeredByEvent="true">
            <startEvent id="stopStart" isInterrupting="true"><messageEventDefinition messageRef="stop"/></startEvent>
            <sequenceFlow id="stopFlow" sourceRef="stopStart" targetRef="stopEnd"/>
            <endEvent id="stopEnd"><terminateEventDefinition/></endEvent>
          </subProcess>
          """;
    }
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="test">
          <message id="stop" name="stop"/>
          <process id="activityEndSemantics" isExecutable="true" operaton:historyTimeToLive="180">
            <startEvent id="start"/>%s
          </process>
        </definitions>
        """.formatted(body);
    testRule.deploy(repositoryService.createDeployment().addString("activity-end.bpmn20.xml", xml));
    String process = runtimeService.startProcessInstanceByKey("activityEndSemantics").getId();
    if (adHoc) {
      runtimeService.triggerAdHocActivities(scope(process), List.of("a"), null);
    }
    return process;
  }

  public static class ThrowOnceBpmnError implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) {
      if (!Boolean.TRUE.equals(execution.getVariable("listenerErrorThrown"))) {
        execution.setVariable("listenerErrorThrown", true);
        throw new BpmnError("unhandled");
      }
    }
  }
}
