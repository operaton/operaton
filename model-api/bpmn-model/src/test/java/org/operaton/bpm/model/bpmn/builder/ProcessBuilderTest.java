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
package org.operaton.bpm.model.bpmn.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.*;
import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.instance.Error;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.operaton.*;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;

import static org.operaton.bpm.model.bpmn.BpmnTestConstants.*;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Sebastian Menski
 */
public class ProcessBuilderTest {

  public static final String TIMER_DATE = "2011-03-11T12:13:14Z";
  public static final String TIMER_DURATION = "P10D";
  public static final String TIMER_CYCLE = "R3/PT10H";
  public static final String FAILED_JOB_RETRY_TIME_CYCLE = "R5/PT1M";

  private BpmnModelInstance modelInstance;
  private static ModelElementType taskType;
  private static ModelElementType gatewayType;
  private static ModelElementType eventType;
  private static ModelElementType processType;

  @BeforeAll
  static void getElementTypes() {
    Model model = Bpmn.createEmptyModel().getModel();
    taskType = model.getType(Task.class);
    gatewayType = model.getType(Gateway.class);
    eventType = model.getType(Event.class);
    processType = model.getType(Process.class);
  }

  @AfterEach
  void validateModel() {
    if (modelInstance != null) {
      Bpmn.validateModel(modelInstance);
    }
  }

  @Test
  void testCreateEmptyProcess() {
    modelInstance = Bpmn.createProcess()
      .done();

    Definitions definitions = modelInstance.getDefinitions();
    assertThat(definitions).isNotNull();
    assertThat(definitions.getTargetNamespace()).isEqualTo(BPMN20_NS);

    Collection<ModelElementInstance> processes = modelInstance.getModelElementsByType(processType);
    assertThat(processes)
      .hasSize(1);

    Process process = (Process) processes.iterator().next();

    assertThat(process.getId()).isNotNull();
  }

  @Test
  void emptyProcessShouldHaveDefaultHTTL() {
    modelInstance = Bpmn.createProcess().done();

    var process = (Process) modelInstance.getModelElementsByType(processType)
        .iterator()
        .next();

    assertThat(process.getOperatonHistoryTimeToLiveString())
        .isEqualTo("P180D");
  }

  @Test
  void shouldHaveNullHTTLValueOnCreateProcessWithSkipHTTL() {
    modelInstance = Bpmn.createProcess().operatonHistoryTimeToLive(null).done();

    var process = (Process) modelInstance.getModelElementsByType(processType)
        .iterator()
        .next();

    assertThat(process.getOperatonHistoryTimeToLiveString())
        .isNull();
  }

  @Test
  void shouldHaveNullHTTLValueOnCreateProcessIdWithoutSkipHTTL(){
    modelInstance = Bpmn.createProcess(PROCESS_ID).done();

    var process = (Process) modelInstance.getModelElementById(PROCESS_ID);

    assertThat(process.getOperatonHistoryTimeToLiveString())
        .isEqualTo("P180D");
  }

  @Test
  void shouldHaveNullHTTLValueOnCreateProcessIdWithSkipHTTL(){
    modelInstance = Bpmn.createProcess(PROCESS_ID).operatonHistoryTimeToLive(null).done();

    var process = (Process) modelInstance.getModelElementById(PROCESS_ID);

    assertThat(process.getOperatonHistoryTimeToLiveString())
        .isNull();
  }

  @Test
  void testGetElement() {
    // Make sure this method is publicly available
    Process process = Bpmn.createProcess().getElement();
    assertThat(process).isNotNull();
  }

  @Test
  void testCreateProcessWithStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
  }

  @Test
  void testCreateProcessWithServiceTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .serviceTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithSendTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .sendTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithUserTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithBusinessRuleTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .businessRuleTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithScriptTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .scriptTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithReceiveTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .receiveTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithManualTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .manualTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithParallelGateway() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .parallelGateway()
        .scriptTask()
        .endEvent()
      .moveToLastGateway()
        .userTask()
        .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(3);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(gatewayType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithExclusiveGateway() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .exclusiveGateway()
        .condition("approved", "${approved}")
        .serviceTask()
        .endEvent()
      .moveToLastGateway()
        .condition("not approved", "${!approved}")
        .scriptTask()
        .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(3);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(3);
    assertThat(modelInstance.getModelElementsByType(gatewayType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithInclusiveGateway() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .inclusiveGateway()
        .condition("approved", "${approved}")
        .serviceTask()
        .endEvent()
      .moveToLastGateway()
        .condition("not approved", "${!approved}")
        .scriptTask()
        .endEvent()
      .done();

    ModelElementType inclusiveGwType = modelInstance.getModel().getType(InclusiveGateway.class);

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(3);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(3);
    assertThat(modelInstance.getModelElementsByType(inclusiveGwType))
      .hasSize(1);
  }

  @Test
  void testCreateProcessWithForkAndJoin() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .parallelGateway()
        .serviceTask()
        .parallelGateway()
        .id("join")
      .moveToLastGateway()
        .scriptTask()
      .connectTo("join")
      .userTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(4);
    assertThat(modelInstance.getModelElementsByType(gatewayType))
      .hasSize(2);
  }

  @Test
  void testCreateProcessWithMultipleParallelTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .parallelGateway("fork")
        .userTask()
        .parallelGateway("join")
      .moveToNode("fork")
        .serviceTask()
        .connectTo("join")
      .moveToNode("fork")
        .userTask()
        .connectTo("join")
      .moveToNode("fork")
        .scriptTask()
        .connectTo("join")
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(4);
    assertThat(modelInstance.getModelElementsByType(gatewayType))
      .hasSize(2);
  }

  @Test
  void testBaseElementDocumentation() {
    modelInstance = Bpmn.createProcess("process")
            .documentation("processDocumentation")
            .startEvent("startEvent")
            .documentation("startEventDocumentation_1")
            .documentation("startEventDocumentation_2")
            .documentation("startEventDocumentation_3")
            .userTask("task")
            .documentation("taskDocumentation")
            .businessRuleTask("businessruletask")
            .subProcess("subprocess")
            .documentation("subProcessDocumentation")
            .embeddedSubProcess()
            .startEvent("subprocessStartEvent")
            .endEvent("subprocessEndEvent")
            .subProcessDone()
            .endEvent("endEvent")
            .documentation("endEventDocumentation")
            .done();

    assertThat(((Process) modelInstance.getModelElementById("process")).getDocumentations().iterator().next().getTextContent()).isEqualTo("processDocumentation");
    assertThat(((UserTask) modelInstance.getModelElementById("task")).getDocumentations().iterator().next().getTextContent()).isEqualTo("taskDocumentation");
    assertThat(((SubProcess) modelInstance.getModelElementById("subprocess")).getDocumentations().iterator().next().getTextContent()).isEqualTo("subProcessDocumentation");
    assertThat(((EndEvent) modelInstance.getModelElementById("endEvent")).getDocumentations().iterator().next().getTextContent()).isEqualTo("endEventDocumentation");

    final Documentation[] startEventDocumentations = ((StartEvent) modelInstance.getModelElementById("startEvent")).getDocumentations().toArray(new Documentation[]{});
    assertThat(startEventDocumentations).hasSize(3);
    for (int i = 1; i <=3; i++) {
      assertThat(startEventDocumentations[i - 1].getTextContent()).isEqualTo("startEventDocumentation_" + i);
    }
  }

  @Test
  void testExtend() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
        .id("task1")
      .serviceTask()
      .endEvent()
      .done();

    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(2);

    UserTask userTask = modelInstance.getModelElementById("task1");
    SequenceFlow outgoingSequenceFlow = userTask.getOutgoing().iterator().next();
    FlowNode serviceTask = outgoingSequenceFlow.getTarget();
    userTask.getOutgoing().remove(outgoingSequenceFlow);
    userTask.builder()
      .scriptTask()
      .userTask()
      .connectTo(serviceTask.getId());

    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(4);
  }

  @Test
  void testCreateInvoiceProcess() {
    modelInstance = Bpmn.createProcess()
      .executable()
      .startEvent()
        .name("Invoice received")
        .operatonFormKey("embedded:app:forms/start-form.html")
      .userTask()
        .name("Assign Approver")
        .operatonFormKey("embedded:app:forms/assign-approver.html")
        .operatonAssignee("demo")
      .userTask("approveInvoice")
        .name("Approve Invoice")
        .operatonFormKey("embedded:app:forms/approve-invoice.html")
        .operatonAssignee("${approver}")
      .exclusiveGateway()
        .name("Invoice approved?")
        .gatewayDirection(GatewayDirection.Diverging)
      .condition("yes", "${approved}")
      .userTask()
        .name("Prepare Bank Transfer")
        .operatonFormKey("embedded:app:forms/prepare-bank-transfer.html")
        .operatonCandidateGroups("accounting")
      .serviceTask()
        .name("Archive Invoice")
        .operatonClass("org.operaton.bpm.example.invoice.service.ArchiveInvoiceService" )
      .endEvent()
        .name("Invoice processed")
      .moveToLastGateway()
      .condition("no", "${!approved}")
      .userTask()
        .name("Review Invoice")
        .operatonFormKey("embedded:app:forms/review-invoice.html" )
        .operatonAssignee("demo")
       .exclusiveGateway()
        .name("Review successful?")
        .gatewayDirection(GatewayDirection.Diverging)
      .condition("no", "${!clarified}")
      .endEvent()
        .name("Invoice not processed")
      .moveToLastGateway()
      .condition("yes", "${clarified}")
      .connectTo("approveInvoice")
      .done();

    assertThat(modelInstance.getModelElementsByType(processType))
      .hasSize(1);
    assertThat(modelInstance.getModelElementsByType(taskType))
      .hasSize(5);
    assertThat(modelInstance.getModelElementsByType(gatewayType))
      .hasSize(2);
    assertThat(modelInstance.getModelElementsByType(eventType))
      .hasSize(3);

  }

  @Test
  @SuppressWarnings("deprecation")
  void testProcessOperatonExtensions() {
    modelInstance = Bpmn.createProcess(PROCESS_ID)
      .operatonJobPriority("${somePriority}")
      .operatonTaskPriority(TEST_PROCESS_TASK_PRIORITY)
      .operatonHistoryTimeToLive(TEST_HISTORY_TIME_TO_LIVE)
      .operatonStartableInTasklist(TEST_STARTABLE_IN_TASKLIST)
      .operatonVersionTag(TEST_VERSION_TAG)
      .startEvent()
      .endEvent()
      .done();

    Process process = modelInstance.getModelElementById(PROCESS_ID);
    assertThat(process.getOperatonJobPriority()).isEqualTo("${somePriority}");
    assertThat(process.getOperatonTaskPriority()).isEqualTo(TEST_PROCESS_TASK_PRIORITY);
    assertThat(process.getOperatonHistoryTimeToLiveString()).isEqualTo(TEST_HISTORY_TIME_TO_LIVE.toString());
    assertThat(process.isOperatonStartableInTasklist()).isEqualTo(TEST_STARTABLE_IN_TASKLIST);
    assertThat(process.getOperatonVersionTag()).isEqualTo(TEST_VERSION_TAG);
  }

  @Test
  void testProcessStartableInTasklist() {
    modelInstance = Bpmn.createProcess(PROCESS_ID)
      .startEvent()
      .endEvent()
      .done();

    Process process = modelInstance.getModelElementById(PROCESS_ID);
    assertThat(process.isOperatonStartableInTasklist()).isTrue();
  }

  @Test
  void testTaskOperatonExternalTask() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
        .serviceTask(EXTERNAL_TASK_ID)
          .operatonExternalTask(TEST_EXTERNAL_TASK_TOPIC)
        .endEvent()
        .done();

    ServiceTask serviceTask = modelInstance.getModelElementById(EXTERNAL_TASK_ID);
    assertThat(serviceTask.getOperatonType()).isEqualTo("external");
    assertThat(serviceTask.getOperatonTopic()).isEqualTo(TEST_EXTERNAL_TASK_TOPIC);
  }

  @Test
  void testTaskOperatonExternalTaskErrorEventDefinition() {
    modelInstance = Bpmn.createProcess()
    .startEvent()
    .serviceTask(EXTERNAL_TASK_ID)
    .operatonExternalTask(TEST_EXTERNAL_TASK_TOPIC)
      .operatonErrorEventDefinition().id("id").error("myErrorCode", "errorMessage").expression("expression").errorEventDefinitionDone()
    .endEvent()
    .moveToActivity(EXTERNAL_TASK_ID)
    .boundaryEvent("boundary").error("myErrorCode", "errorMessage")
    .endEvent("boundaryEnd")
    .done();

    ServiceTask externalTask = modelInstance.getModelElementById(EXTERNAL_TASK_ID);
    ExtensionElements extensionElements = externalTask.getExtensionElements();
    Collection<OperatonErrorEventDefinition> errorEventDefinitions = extensionElements.getChildElementsByType(OperatonErrorEventDefinition.class);
    assertThat(errorEventDefinitions).hasSize(1);
    OperatonErrorEventDefinition operatonErrorEventDefinition = errorEventDefinitions.iterator().next();
    assertThat(operatonErrorEventDefinition).isNotNull();
    assertThat(operatonErrorEventDefinition.getId()).isEqualTo("id");
    assertThat(operatonErrorEventDefinition.getOperatonExpression()).isEqualTo("expression");
    assertErrorEventDefinition("boundary", "myErrorCode", "errorMessage");
  }

  @Test
  void testTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .serviceTask(TASK_ID)
        .operatonAsyncBefore()
        .notOperatonExclusive()
        .operatonJobPriority("${somePriority}")
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(serviceTask.isOperatonAsyncBefore()).isTrue();
    assertThat(serviceTask.isOperatonExclusive()).isFalse();
    assertThat(serviceTask.getOperatonJobPriority()).isEqualTo("${somePriority}");
    assertThat(serviceTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);

    assertOperatonFailedJobRetryTimeCycle(serviceTask);
  }

  @Test
  void testServiceTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .serviceTask(TASK_ID)
        .operatonClass(TEST_CLASS_API)
        .operatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
        .operatonExpression(TEST_EXPRESSION_API)
        .operatonResultVariable(TEST_STRING_API)
        .operatonTopic(TEST_STRING_API)
        .operatonType(TEST_STRING_API)
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .done();

    ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(serviceTask.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(serviceTask.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertThat(serviceTask.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(serviceTask.getOperatonResultVariable()).isEqualTo(TEST_STRING_API);
    assertThat(serviceTask.getOperatonTopic()).isEqualTo(TEST_STRING_API);
    assertThat(serviceTask.getOperatonType()).isEqualTo(TEST_STRING_API);
    assertThat(serviceTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);

    assertOperatonFailedJobRetryTimeCycle(serviceTask);
  }

  @Test
  void testServiceTaskOperatonClass() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .serviceTask(TASK_ID)
        .operatonClass(getClass().getName())
      .done();

    ServiceTask serviceTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(serviceTask.getOperatonClass()).isEqualTo(getClass().getName());
  }


  @Test
  void testSendTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .sendTask(TASK_ID)
        .operatonClass(TEST_CLASS_API)
        .operatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
        .operatonExpression(TEST_EXPRESSION_API)
        .operatonResultVariable(TEST_STRING_API)
        .operatonTopic(TEST_STRING_API)
        .operatonType(TEST_STRING_API)
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    SendTask sendTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(sendTask.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(sendTask.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertThat(sendTask.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(sendTask.getOperatonResultVariable()).isEqualTo(TEST_STRING_API);
    assertThat(sendTask.getOperatonTopic()).isEqualTo(TEST_STRING_API);
    assertThat(sendTask.getOperatonType()).isEqualTo(TEST_STRING_API);
    assertThat(sendTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);

    assertOperatonFailedJobRetryTimeCycle(sendTask);
  }

  @Test
  void testSendTaskOperatonClass() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .sendTask(TASK_ID)
        .operatonClass(this.getClass())
      .endEvent()
      .done();

    SendTask sendTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(sendTask.getOperatonClass()).isEqualTo(this.getClass().getName());
  }

  @Test
  void testUserTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask(TASK_ID)
        .operatonAssignee(TEST_STRING_API)
        .operatonCandidateGroups(TEST_GROUPS_API)
        .operatonCandidateUsers(TEST_USERS_LIST_API)
        .operatonDueDate(TEST_DUE_DATE_API)
        .operatonFollowUpDate(TEST_FOLLOW_UP_DATE_API)
        .operatonFormHandlerClass(TEST_CLASS_API)
        .operatonFormKey(TEST_STRING_API)
        .operatonFormRef(FORM_ID)
        .operatonFormRefBinding(TEST_STRING_FORM_REF_BINDING)
        .operatonFormRefVersion(TEST_STRING_FORM_REF_VERSION)
        .operatonPriority(TEST_PRIORITY_API)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(userTask.getOperatonAssignee()).isEqualTo(TEST_STRING_API);
    assertThat(userTask.getOperatonCandidateGroups()).isEqualTo(TEST_GROUPS_API);
    assertThat(userTask.getOperatonCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_API);
    assertThat(userTask.getOperatonCandidateUsers()).isEqualTo(TEST_USERS_API);
    assertThat(userTask.getOperatonCandidateUsersList()).containsAll(TEST_USERS_LIST_API);
    assertThat(userTask.getOperatonDueDate()).isEqualTo(TEST_DUE_DATE_API);
    assertThat(userTask.getOperatonFollowUpDate()).isEqualTo(TEST_FOLLOW_UP_DATE_API);
    assertThat(userTask.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_API);
    assertThat(userTask.getOperatonFormKey()).isEqualTo(TEST_STRING_API);
    assertThat(userTask.getOperatonFormRef()).isEqualTo(FORM_ID);
    assertThat(userTask.getOperatonFormRefBinding()).isEqualTo(TEST_STRING_FORM_REF_BINDING);
    assertThat(userTask.getOperatonFormRefVersion()).isEqualTo(TEST_STRING_FORM_REF_VERSION);
    assertThat(userTask.getOperatonPriority()).isEqualTo(TEST_PRIORITY_API);

    assertOperatonFailedJobRetryTimeCycle(userTask);
  }

  @Test
  void testBusinessRuleTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .businessRuleTask(TASK_ID)
        .operatonClass(TEST_CLASS_API)
        .operatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
        .operatonExpression(TEST_EXPRESSION_API)
        .operatonResultVariable("resultVar")
        .operatonTopic("topic")
        .operatonType("type")
        .operatonDecisionRef("decisionRef")
        .operatonDecisionRefBinding("latest")
        .operatonDecisionRefVersion("7")
        .operatonDecisionRefVersionTag("0.1.0")
        .operatonDecisionRefTenantId("tenantId")
        .operatonMapDecisionResult("singleEntry")
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    BusinessRuleTask businessRuleTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(businessRuleTask.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(businessRuleTask.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertThat(businessRuleTask.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(businessRuleTask.getOperatonResultVariable()).isEqualTo("resultVar");
    assertThat(businessRuleTask.getOperatonTopic()).isEqualTo("topic");
    assertThat(businessRuleTask.getOperatonType()).isEqualTo("type");
    assertThat(businessRuleTask.getOperatonDecisionRef()).isEqualTo("decisionRef");
    assertThat(businessRuleTask.getOperatonDecisionRefBinding()).isEqualTo("latest");
    assertThat(businessRuleTask.getOperatonDecisionRefVersion()).isEqualTo("7");
    assertThat(businessRuleTask.getOperatonDecisionRefVersionTag()).isEqualTo("0.1.0");
    assertThat(businessRuleTask.getOperatonDecisionRefTenantId()).isEqualTo("tenantId");
    assertThat(businessRuleTask.getOperatonMapDecisionResult()).isEqualTo("singleEntry");
    assertThat(businessRuleTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);

    assertOperatonFailedJobRetryTimeCycle(businessRuleTask);
  }

  @Test
  void testBusinessRuleTaskOperatonClass() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .businessRuleTask(TASK_ID)
        .operatonClass(Bpmn.class)
      .endEvent()
      .done();

    BusinessRuleTask businessRuleTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(businessRuleTask.getOperatonClass()).isEqualTo("org.operaton.bpm.model.bpmn.Bpmn");
  }

  @Test
  void testScriptTaskOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .scriptTask(TASK_ID)
        .operatonResultVariable(TEST_STRING_API)
        .operatonResource(TEST_STRING_API)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    ScriptTask scriptTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(scriptTask.getOperatonResultVariable()).isEqualTo(TEST_STRING_API);
    assertThat(scriptTask.getOperatonResource()).isEqualTo(TEST_STRING_API);

    assertOperatonFailedJobRetryTimeCycle(scriptTask);
  }

  @Test
  void testStartEventOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent(START_EVENT_ID)
        .operatonAsyncBefore()
        .notOperatonExclusive()
        .operatonFormHandlerClass(TEST_CLASS_API)
        .operatonFormKey(TEST_STRING_API)
        .operatonFormRef(FORM_ID)
        .operatonFormRefBinding(TEST_STRING_FORM_REF_BINDING)
        .operatonFormRefVersion(TEST_STRING_FORM_REF_VERSION)
        .operatonInitiator(TEST_STRING_API)
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .done();

    StartEvent startEvent = modelInstance.getModelElementById(START_EVENT_ID);
    assertThat(startEvent.isOperatonAsyncBefore()).isTrue();
    assertThat(startEvent.isOperatonExclusive()).isFalse();
    assertThat(startEvent.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_API);
    assertThat(startEvent.getOperatonFormKey()).isEqualTo(TEST_STRING_API);
    assertThat(startEvent.getOperatonFormRef()).isEqualTo(FORM_ID);
    assertThat(startEvent.getOperatonFormRefBinding()).isEqualTo(TEST_STRING_FORM_REF_BINDING);
    assertThat(startEvent.getOperatonFormRefVersion()).isEqualTo(TEST_STRING_FORM_REF_VERSION);
    assertThat(startEvent.getOperatonInitiator()).isEqualTo(TEST_STRING_API);

    assertOperatonFailedJobRetryTimeCycle(startEvent);
  }

  @Test
  void testErrorDefinitionsForStartEvent() {
    modelInstance = Bpmn.createProcess()
    .startEvent("start")
      .errorEventDefinition("event")
        .errorCodeVariable("errorCodeVariable")
        .errorMessageVariable("errorMessageVariable")
        .error("errorCode", "errorMessage")
      .errorEventDefinitionDone()
     .endEvent().done();

    assertErrorEventDefinition("start", "errorCode", "errorMessage");
    assertErrorEventDefinitionForErrorVariables("start", "errorCodeVariable", "errorMessageVariable");
  }

  @Test
  void testErrorDefinitionsForStartEventWithoutEventDefinitionId() {
    modelInstance = Bpmn.createProcess()
    .startEvent("start")
      .errorEventDefinition()
        .errorCodeVariable("errorCodeVariable")
        .errorMessageVariable("errorMessageVariable")
        .error("errorCode", "errorMessage")
      .errorEventDefinitionDone()
     .endEvent().done();

    assertErrorEventDefinition("start", "errorCode", "errorMessage");
    assertErrorEventDefinitionForErrorVariables("start", "errorCodeVariable", "errorMessageVariable");
  }

  @Test
  void testCallActivityOperatonExtension() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .callActivity(CALL_ACTIVITY_ID)
        .calledElement(TEST_STRING_API)
        .operatonAsyncBefore()
        .operatonCalledElementBinding("version")
        .operatonCalledElementVersion("1.0")
        .operatonCalledElementVersionTag("ver-1.0")
        .operatonCalledElementTenantId("t1")
        .operatonCaseRef("case")
        .operatonCaseBinding("deployment")
        .operatonCaseVersion("2")
        .operatonCaseTenantId("t2")
        .operatonIn("in-source", "in-target")
        .operatonOut("out-source", "out-target")
        .operatonVariableMappingClass(TEST_CLASS_API)
        .operatonVariableMappingDelegateExpression(TEST_DELEGATE_EXPRESSION_API)
        .notOperatonExclusive()
        .operatonFailedJobRetryTimeCycle(FAILED_JOB_RETRY_TIME_CYCLE)
      .endEvent()
      .done();

    CallActivity callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
    assertThat(callActivity.getCalledElement()).isEqualTo(TEST_STRING_API);
    assertThat(callActivity.isOperatonAsyncBefore()).isTrue();
    assertThat(callActivity.getOperatonCalledElementBinding()).isEqualTo("version");
    assertThat(callActivity.getOperatonCalledElementVersion()).isEqualTo("1.0");
    assertThat(callActivity.getOperatonCalledElementVersionTag()).isEqualTo("ver-1.0");
    assertThat(callActivity.getOperatonCalledElementTenantId()).isEqualTo("t1");
    assertThat(callActivity.getOperatonCaseRef()).isEqualTo("case");
    assertThat(callActivity.getOperatonCaseBinding()).isEqualTo("deployment");
    assertThat(callActivity.getOperatonCaseVersion()).isEqualTo("2");
    assertThat(callActivity.getOperatonCaseTenantId()).isEqualTo("t2");
    assertThat(callActivity.isOperatonExclusive()).isFalse();

    OperatonIn operatonIn = (OperatonIn) callActivity.getExtensionElements().getUniqueChildElementByType(OperatonIn.class);
    assertThat(operatonIn.getOperatonSource()).isEqualTo("in-source");
    assertThat(operatonIn.getOperatonTarget()).isEqualTo("in-target");

    OperatonOut operatonOut = (OperatonOut) callActivity.getExtensionElements().getUniqueChildElementByType(OperatonOut.class);
    assertThat(operatonOut.getOperatonSource()).isEqualTo("out-source");
    assertThat(operatonOut.getOperatonTarget()).isEqualTo("out-target");

    assertThat(callActivity.getOperatonVariableMappingClass()).isEqualTo(TEST_CLASS_API);
    assertThat(callActivity.getOperatonVariableMappingDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertOperatonFailedJobRetryTimeCycle(callActivity);
  }

  @Test
  void testCallActivityOperatonBusinessKey() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .callActivity(CALL_ACTIVITY_ID)
        .operatonInBusinessKey("business-key")
      .endEvent()
      .done();

    CallActivity callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
    OperatonIn operatonIn = (OperatonIn) callActivity.getExtensionElements().getUniqueChildElementByType(OperatonIn.class);
    assertThat(operatonIn.getOperatonBusinessKey()).isEqualTo("business-key");
  }

  @Test
  void testCallActivityOperatonVariableMappingClass() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .callActivity(CALL_ACTIVITY_ID)
        .operatonVariableMappingClass(this.getClass())
      .endEvent()
      .done();

    CallActivity callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
    assertThat(callActivity.getOperatonVariableMappingClass()).isEqualTo(this.getClass().getName());
  }

  @Test
  void testSubProcessBuilder() {
    BpmnModelInstance bpmnModelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
        .operatonAsyncBefore()
        .embeddedSubProcess()
          .startEvent()
          .userTask()
          .endEvent()
        .subProcessDone()
      .serviceTask(SERVICE_TASK_ID)
      .endEvent()
      .done();

    SubProcess subProcess = bpmnModelInstance.getModelElementById(SUB_PROCESS_ID);
    ServiceTask serviceTask = bpmnModelInstance.getModelElementById(SERVICE_TASK_ID);
    assertThat(subProcess.isOperatonAsyncBefore()).isTrue();
    assertThat(subProcess.isOperatonExclusive()).isTrue();
    assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(1);
    assertThat(subProcess.getFlowElements()).hasSize(5);
    assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
  }

  @Test
  void testSubProcessBuilderDetached() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess(SUB_PROCESS_ID)
      .serviceTask(SERVICE_TASK_ID)
      .endEvent()
      .done();

    SubProcess subProcess = modelInstance.getModelElementById(SUB_PROCESS_ID);

    subProcess.builder()
      .operatonAsyncBefore()
      .embeddedSubProcess()
        .startEvent()
        .userTask()
        .endEvent();

    ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
    assertThat(subProcess.isOperatonAsyncBefore()).isTrue();
    assertThat(subProcess.isOperatonExclusive()).isTrue();
    assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(1);
    assertThat(subProcess.getFlowElements()).hasSize(5);
    assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
  }

  @Test
  void testSubProcessBuilderNested() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess(SUB_PROCESS_ID + 1)
        .operatonAsyncBefore()
        .embeddedSubProcess()
          .startEvent()
          .userTask()
          .subProcess(SUB_PROCESS_ID + 2)
            .operatonAsyncBefore()
            .notOperatonExclusive()
            .embeddedSubProcess()
              .startEvent()
              .userTask()
              .endEvent()
            .subProcessDone()
          .serviceTask(SERVICE_TASK_ID + 1)
          .endEvent()
        .subProcessDone()
      .serviceTask(SERVICE_TASK_ID + 2)
      .endEvent()
      .done();

    SubProcess subProcess = modelInstance.getModelElementById(SUB_PROCESS_ID + 1);
    ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID + 2);
    assertThat(subProcess.isOperatonAsyncBefore()).isTrue();
    assertThat(subProcess.isOperatonExclusive()).isTrue();
    assertThat(subProcess.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(subProcess.getChildElementsByType(Task.class)).hasSize(2);
    assertThat(subProcess.getChildElementsByType(SubProcess.class)).hasSize(1);
    assertThat(subProcess.getFlowElements()).hasSize(9);
    assertThat(subProcess.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);

    SubProcess nestedSubProcess = modelInstance.getModelElementById(SUB_PROCESS_ID + 2);
    ServiceTask nestedServiceTask = modelInstance.getModelElementById(SERVICE_TASK_ID + 1);
    assertThat(nestedSubProcess.isOperatonAsyncBefore()).isTrue();
    assertThat(nestedSubProcess.isOperatonExclusive()).isFalse();
    assertThat(nestedSubProcess.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(nestedSubProcess.getChildElementsByType(Task.class)).hasSize(1);
    assertThat(nestedSubProcess.getFlowElements()).hasSize(5);
    assertThat(nestedSubProcess.getSucceedingNodes().singleResult()).isEqualTo(nestedServiceTask);
  }

  @Test
  void testSubProcessBuilderWrongScope() {
    var endEventBuilder = Bpmn.createProcess().startEvent();
    assertThatThrownBy(endEventBuilder::subProcessDone).isInstanceOf(BpmnModelException.class);
  }

  @Test
  void testTransactionBuilder() {
    BpmnModelInstance bpmnModelInstance = Bpmn.createProcess()
      .startEvent()
      .transaction(TRANSACTION_ID)
        .operatonAsyncBefore()
        .method(TransactionMethod.Image)
        .embeddedSubProcess()
          .startEvent()
          .userTask()
          .endEvent()
        .transactionDone()
      .serviceTask(SERVICE_TASK_ID)
      .endEvent()
      .done();

    Transaction transaction = bpmnModelInstance.getModelElementById(TRANSACTION_ID);
    ServiceTask serviceTask = bpmnModelInstance.getModelElementById(SERVICE_TASK_ID);
    assertThat(transaction.isOperatonAsyncBefore()).isTrue();
    assertThat(transaction.isOperatonExclusive()).isTrue();
    assertThat(transaction.getMethod()).isEqualTo(TransactionMethod.Image);
    assertThat(transaction.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(transaction.getChildElementsByType(Task.class)).hasSize(1);
    assertThat(transaction.getFlowElements()).hasSize(5);
    assertThat(transaction.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
  }

  @Test
  void testTransactionBuilderDetached() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .transaction(TRANSACTION_ID)
      .serviceTask(SERVICE_TASK_ID)
      .endEvent()
      .done();

    Transaction transaction = modelInstance.getModelElementById(TRANSACTION_ID);

    transaction.builder()
      .operatonAsyncBefore()
      .embeddedSubProcess()
        .startEvent()
        .userTask()
        .endEvent();

    ServiceTask serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
    assertThat(transaction.isOperatonAsyncBefore()).isTrue();
    assertThat(transaction.isOperatonExclusive()).isTrue();
    assertThat(transaction.getChildElementsByType(Event.class)).hasSize(2);
    assertThat(transaction.getChildElementsByType(Task.class)).hasSize(1);
    assertThat(transaction.getFlowElements()).hasSize(5);
    assertThat(transaction.getSucceedingNodes().singleResult()).isEqualTo(serviceTask);
  }

  @Test
  void testScriptText() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .scriptTask("script")
        .scriptFormat("groovy")
        .scriptText("println \"hello, world\";")
      .endEvent()
      .done();

    ScriptTask scriptTask = modelInstance.getModelElementById("script");
    assertThat(scriptTask.getScriptFormat()).isEqualTo("groovy");
    assertThat(scriptTask.getScript().getTextContent()).isEqualTo("println \"hello, world\";");
  }

  @Test
  void testEventBasedGatewayAsyncAfter() {
    var eventBasedGatewayBuilder = Bpmn.createProcess()
      .startEvent()
      .eventBasedGateway();
    assertThatThrownBy(eventBasedGatewayBuilder::operatonAsyncAfter).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> eventBasedGatewayBuilder.operatonAsyncAfter(true)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testMessageStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").message("message")
      .done();

    assertMessageEventDefinition("start", "message");
  }

  @Test
  void testMessageStartEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").message("message")
        .subProcess().triggerByEvent()
         .embeddedSubProcess()
         .startEvent("subStart").message("message")
         .subProcessDone()
      .done();

    Message message = assertMessageEventDefinition("start", "message");
    Message subMessage = assertMessageEventDefinition("subStart", "message");

    assertThat(message).isEqualTo(subMessage);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testIntermediateMessageCatchEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch").message("message")
      .done();

    assertMessageEventDefinition("catch", "message");
  }

  @Test
  void testIntermediateMessageCatchEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch1").message("message")
      .intermediateCatchEvent("catch2").message("message")
      .done();

    Message message1 = assertMessageEventDefinition("catch1", "message");
    Message message2 = assertMessageEventDefinition("catch2", "message");

    assertThat(message1).isEqualTo(message2);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testMessageEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end").message("message")
      .done();

    assertMessageEventDefinition("end", "message");
  }

  @Test
  void testMessageEventDefinitionEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end")
      .messageEventDefinition()
        .message("message")
      .done();

    assertMessageEventDefinition("end", "message");
  }

  @Test
  void testMessageEndEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .parallelGateway()
      .endEvent("end1").message("message")
      .moveToLastGateway()
      .endEvent("end2").message("message")
      .done();

    Message message1 = assertMessageEventDefinition("end1", "message");
    Message message2 = assertMessageEventDefinition("end2", "message");

    assertThat(message1).isEqualTo(message2);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testMessageEventDefinitionEndEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .parallelGateway()
      .endEvent("end1")
      .messageEventDefinition()
        .message("message")
        .messageEventDefinitionDone()
      .moveToLastGateway()
      .endEvent("end2")
      .messageEventDefinition()
        .message("message")
      .done();

    Message message1 = assertMessageEventDefinition("end1", "message");
    Message message2 = assertMessageEventDefinition("end2", "message");

    assertThat(message1).isEqualTo(message2);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testIntermediateMessageThrowEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw").message("message")
      .done();

    assertMessageEventDefinition("throw", "message");
  }

  @Test
  void testIntermediateMessageEventDefinitionThrowEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw")
      .messageEventDefinition()
        .message("message")
      .done();

    assertMessageEventDefinition("throw", "message");
  }

  @Test
  void testIntermediateMessageThrowEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1").message("message")
      .intermediateThrowEvent("throw2").message("message")
      .done();

    Message message1 = assertMessageEventDefinition("throw1", "message");
    Message message2 = assertMessageEventDefinition("throw2", "message");

    assertThat(message1).isEqualTo(message2);
    assertOnlyOneMessageExists("message");
  }


  @Test
  void testIntermediateMessageEventDefinitionThrowEventWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1")
      .messageEventDefinition()
        .message("message")
        .messageEventDefinitionDone()
      .intermediateThrowEvent("throw2")
      .messageEventDefinition()
        .message("message")
        .messageEventDefinitionDone()
      .done();

    Message message1 = assertMessageEventDefinition("throw1", "message");
    Message message2 = assertMessageEventDefinition("throw2", "message");

    assertThat(message1).isEqualTo(message2);
    assertOnlyOneMessageExists("message");
  }

  @Test
  void testIntermediateMessageThrowEventWithMessageDefinition() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1")
      .messageEventDefinition()
        .id("messageEventDefinition")
        .message("message")
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
        .operatonType("external")
        .operatonTopic("TOPIC")
      .done();

    MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
    assertThat(event.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);
    assertThat(event.getOperatonTopic()).isEqualTo("TOPIC");
    assertThat(event.getOperatonType()).isEqualTo("external");
    assertThat(event.getMessage().getName()).isEqualTo("message");
  }

  @Test
  void testIntermediateMessageThrowEventWithTaskPriority() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1")
      .messageEventDefinition("messageEventDefinition")
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
      .done();

    MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
    assertThat(event.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);
  }

  @Test
  void testEndEventWithTaskPriority() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end")
      .messageEventDefinition("messageEventDefinition")
        .operatonTaskPriority(TEST_SERVICE_TASK_PRIORITY)
      .done();

    MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
    assertThat(event.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);
  }

  @Test
  void testMessageEventDefinitionWithID() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1")
      .messageEventDefinition("messageEventDefinition")
      .done();

    MessageEventDefinition event = modelInstance.getModelElementById("messageEventDefinition");
    assertThat(event).isNotNull();

    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw2")
      .messageEventDefinition().id("messageEventDefinition1")
      .done();

    //========================================
    //==============end event=================
    //========================================
    event = modelInstance.getModelElementById("messageEventDefinition1");
    assertThat(event).isNotNull();
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end1")
      .messageEventDefinition("messageEventDefinition")
      .done();

    event = modelInstance.getModelElementById("messageEventDefinition");
    assertThat(event).isNotNull();

    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end2")
      .messageEventDefinition().id("messageEventDefinition1")
      .done();

    event = modelInstance.getModelElementById("messageEventDefinition1");
    assertThat(event).isNotNull();
  }

  @Test
  void testReceiveTaskMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .receiveTask("receive").message("message")
      .done();

    ReceiveTask receiveTask = modelInstance.getModelElementById("receive");

    Message message = receiveTask.getMessage();
    assertThat(message).isNotNull();
    assertThat(message.getName()).isEqualTo("message");
  }

  @Test
  void testReceiveTaskWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .receiveTask("receive1").message("message")
      .receiveTask("receive2").message("message")
      .done();

    ReceiveTask receiveTask1 = modelInstance.getModelElementById("receive1");
    Message message1 = receiveTask1.getMessage();

    ReceiveTask receiveTask2 = modelInstance.getModelElementById("receive2");
    Message message2 = receiveTask2.getMessage();

    assertThat(message1).isEqualTo(message2);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testSendTaskMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .sendTask("send").message("message")
      .done();

    SendTask sendTask = modelInstance.getModelElementById("send");

    Message message = sendTask.getMessage();
    assertThat(message).isNotNull();
    assertThat(message.getName()).isEqualTo("message");
  }

  @Test
  void testSendTaskWithExistingMessage() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .sendTask("send1").message("message")
      .sendTask("send2").message("message")
      .done();

    SendTask sendTask1 = modelInstance.getModelElementById("send1");
    Message message1 = sendTask1.getMessage();

    SendTask sendTask2 = modelInstance.getModelElementById("send2");
    Message message2 = sendTask2.getMessage();

    assertThat(message1).isEqualTo(message2);

    assertOnlyOneMessageExists("message");
  }

  @Test
  void testSignalStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").signal("signal")
      .done();

    assertSignalEventDefinition("start", "signal");
  }

  @Test
  void testSignalStartEventWithExistingSignal() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").signal("signal")
      .subProcess().triggerByEvent()
      .embeddedSubProcess()
      .startEvent("subStart").signal("signal")
      .subProcessDone()
      .done();

    Signal signal = assertSignalEventDefinition("start", "signal");
    Signal subSignal = assertSignalEventDefinition("subStart", "signal");

    assertThat(signal).isEqualTo(subSignal);

    assertOnlyOneSignalExists("signal");
  }

  @Test
  void testIntermediateSignalCatchEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch").signal("signal")
      .done();

    assertSignalEventDefinition("catch", "signal");
  }

  @Test
  void testIntermediateSignalCatchEventWithExistingSignal() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch1").signal("signal")
      .intermediateCatchEvent("catch2").signal("signal")
      .done();

    Signal signal1 = assertSignalEventDefinition("catch1", "signal");
    Signal signal2 = assertSignalEventDefinition("catch2", "signal");

    assertThat(signal1).isEqualTo(signal2);

    assertOnlyOneSignalExists("signal");
  }

  @Test
  void testSignalEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end").signal("signal")
      .done();

    assertSignalEventDefinition("end", "signal");
  }

  @Test
  void testSignalEndEventWithExistingSignal() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .parallelGateway()
      .endEvent("end1").signal("signal")
      .moveToLastGateway()
      .endEvent("end2").signal("signal")
      .done();

    Signal signal1 = assertSignalEventDefinition("end1", "signal");
    Signal signal2 = assertSignalEventDefinition("end2", "signal");

    assertThat(signal1).isEqualTo(signal2);

    assertOnlyOneSignalExists("signal");
  }

  @Test
  void testIntermediateSignalThrowEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw").signal("signal")
      .done();

    assertSignalEventDefinition("throw", "signal");
  }

  @Test
  void testIntermediateSignalThrowEventWithExistingSignal() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw1").signal("signal")
      .intermediateThrowEvent("throw2").signal("signal")
      .done();

    Signal signal1 = assertSignalEventDefinition("throw1", "signal");
    Signal signal2 = assertSignalEventDefinition("throw2", "signal");

    assertThat(signal1).isEqualTo(signal2);

    assertOnlyOneSignalExists("signal");
  }

  @Test
  void testIntermediateSignalThrowEventWithPayloadLocalVar() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw")
        .signalEventDefinition("signal")
          .operatonInSourceTarget("source", "target1")
          .operatonInSourceExpressionTarget("${'sourceExpression'}", "target2")
          .operatonInAllVariables("all", true)
          .operatonInBusinessKey("aBusinessKey")
          .throwEventDefinitionDone()
      .endEvent()
      .done();

    assertSignalEventDefinition("throw", "signal");
    SignalEventDefinition signalEventDefinition = assertAndGetSingleEventDefinition("throw", SignalEventDefinition.class);

    assertThat(signalEventDefinition.getSignal().getName()).isEqualTo("signal");

    List<OperatonIn> operatonInParams = signalEventDefinition.getExtensionElements().getElementsQuery().filterByType(OperatonIn.class).list();
    assertThat(operatonInParams).hasSize(4);

    int paramCounter = 0;
    for (OperatonIn inParam : operatonInParams) {
      if (inParam.getOperatonVariables() != null) {
        assertThat(inParam.getOperatonVariables()).isEqualTo("all");
        if (inParam.getOperatonLocal()) {
          paramCounter++;
        }
      } else if (inParam.getOperatonBusinessKey() != null) {
        assertThat(inParam.getOperatonBusinessKey()).isEqualTo("aBusinessKey");
        paramCounter++;
      } else if (inParam.getOperatonSourceExpression() != null) {
        assertThat(inParam.getOperatonSourceExpression()).isEqualTo("${'sourceExpression'}");
        assertThat(inParam.getOperatonTarget()).isEqualTo("target2");
        paramCounter++;
      } else if (inParam.getOperatonSource() != null) {
        assertThat(inParam.getOperatonSource()).isEqualTo("source");
        assertThat(inParam.getOperatonTarget()).isEqualTo("target1");
        paramCounter++;
      }
    }
    assertThat(paramCounter).isEqualTo(operatonInParams.size());
  }

  @Test
  void testIntermediateSignalThrowEventWithPayload() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw")
        .signalEventDefinition("signal")
          .operatonInAllVariables("all")
          .throwEventDefinitionDone()
      .endEvent()
      .done();

    SignalEventDefinition signalEventDefinition = assertAndGetSingleEventDefinition("throw", SignalEventDefinition.class);

    List<OperatonIn> operatonInParams = signalEventDefinition.getExtensionElements().getElementsQuery().filterByType(OperatonIn.class).list();
    assertThat(operatonInParams).hasSize(1);

    assertThat(operatonInParams.get(0).getOperatonVariables()).isEqualTo("all");
  }

  @Test
  void testMessageBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task") // jump back to user task and attach a boundary event
      .boundaryEvent("boundary").message("message")
      .endEvent("boundaryEnd")
      .done();

    assertMessageEventDefinition("boundary", "message");

    UserTask userTask = modelInstance.getModelElementById("task");
    BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
    EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

    // boundary event is attached to the user task
    assertThat(boundaryEvent.getAttachedTo()).isEqualTo(userTask);

    // boundary event has no incoming sequence flows
    assertThat(boundaryEvent.getIncoming()).isEmpty();

    // the next flow node is the boundary end event
    List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
    assertThat(succeedingNodes).containsOnly(boundaryEnd);
  }

  @Test
  void testMultipleBoundaryEvents() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task") // jump back to user task and attach a boundary event
      .boundaryEvent("boundary1").message("message")
      .endEvent("boundaryEnd1")
      .moveToActivity("task") // jump back to user task and attach another boundary event
      .boundaryEvent("boundary2").signal("signal")
      .endEvent("boundaryEnd2")
      .done();

    assertMessageEventDefinition("boundary1", "message");
    assertSignalEventDefinition("boundary2", "signal");

    UserTask userTask = modelInstance.getModelElementById("task");
    BoundaryEvent boundaryEvent1 = modelInstance.getModelElementById("boundary1");
    EndEvent boundaryEnd1 = modelInstance.getModelElementById("boundaryEnd1");
    BoundaryEvent boundaryEvent2 = modelInstance.getModelElementById("boundary2");
    EndEvent boundaryEnd2 = modelInstance.getModelElementById("boundaryEnd2");

    // boundary events are attached to the user task
    assertThat(boundaryEvent1.getAttachedTo()).isEqualTo(userTask);
    assertThat(boundaryEvent2.getAttachedTo()).isEqualTo(userTask);

    // boundary events have no incoming sequence flows
    assertThat(boundaryEvent1.getIncoming()).isEmpty();
    assertThat(boundaryEvent2.getIncoming()).isEmpty();

    // the next flow node is the boundary end event
    List<FlowNode> succeedingNodes = boundaryEvent1.getSucceedingNodes().list();
    assertThat(succeedingNodes).containsOnly(boundaryEnd1);
    succeedingNodes = boundaryEvent2.getSucceedingNodes().list();
    assertThat(succeedingNodes).containsOnly(boundaryEnd2);
  }

  @Test
  void testOperatonTaskListenerByClassName() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClass("start", "aClass")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo("aClass");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonTaskListenerByClass() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClass("start", this.getClass())
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo(this.getClass().getName());
    assertThat(taskListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonTaskListenerByExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerExpression("start", "anExpression")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonExpression()).isEqualTo("anExpression");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonTaskListenerByDelegateExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerDelegateExpression("start", "aDelegate")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo("aDelegate");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonTimeoutCycleTaskListenerByClassName() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithCycle("timeout-1", "aClass", "R/PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo("aClass");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNotNull();
    assertThat(timeout.getTimeCycle().getRawTextContent()).isEqualTo("R/PT1H");
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDateTaskListenerByClassName() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithDate("timeout-1", "aClass", "2019-09-09T12:12:12")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo("aClass");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNotNull();
    assertThat(timeout.getTimeDate().getRawTextContent()).isEqualTo("2019-09-09T12:12:12");
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDurationTaskListenerByClassName() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithDuration("timeout-1", "aClass", "PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo("aClass");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNotNull();
    assertThat(timeout.getTimeDuration().getRawTextContent()).isEqualTo("PT1H");
  }

  @Test
  void testOperatonTimeoutDurationTaskListenerByClass() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithDuration("timeout-1", this.getClass(), "PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo(this.getClass().getName());
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNotNull();
    assertThat(timeout.getTimeDuration().getRawTextContent()).isEqualTo("PT1H");
  }

  @Test
  void testOperatonTimeoutCycleTaskListenerByClass() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithCycle("timeout-1", this.getClass(), "R/PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo(this.getClass().getName());
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNotNull();
    assertThat(timeout.getTimeCycle().getRawTextContent()).isEqualTo("R/PT1H");
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDateTaskListenerByClass() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerClassTimeoutWithDate("timeout-1", this.getClass(), "2019-09-09T12:12:12")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonClass()).isEqualTo(this.getClass().getName());
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNotNull();
    assertThat(timeout.getTimeDate().getRawTextContent()).isEqualTo("2019-09-09T12:12:12");
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutCycleTaskListenerByExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerExpressionTimeoutWithCycle("timeout-1", "anExpression", "R/PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonExpression()).isEqualTo("anExpression");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNotNull();
    assertThat(timeout.getTimeCycle().getRawTextContent()).isEqualTo("R/PT1H");
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDateTaskListenerByExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerExpressionTimeoutWithDate("timeout-1", "anExpression", "2019-09-09T12:12:12")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonExpression()).isEqualTo("anExpression");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNotNull();
    assertThat(timeout.getTimeDate().getRawTextContent()).isEqualTo("2019-09-09T12:12:12");
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDurationTaskListenerByExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerExpressionTimeoutWithDuration("timeout-1", "anExpression", "PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonExpression()).isEqualTo("anExpression");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNotNull();
    assertThat(timeout.getTimeDuration().getRawTextContent()).isEqualTo("PT1H");
  }

  @Test
  void testOperatonTimeoutCycleTaskListenerByDelegateExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerDelegateExpressionTimeoutWithCycle("timeout-1", "aDelegate", "R/PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo("aDelegate");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNotNull();
    assertThat(timeout.getTimeCycle().getRawTextContent()).isEqualTo("R/PT1H");
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDateTaskListenerByDelegateExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerDelegateExpressionTimeoutWithDate("timeout-1", "aDelegate", "2019-09-09T12:12:12")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo("aDelegate");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNotNull();
    assertThat(timeout.getTimeDate().getRawTextContent()).isEqualTo("2019-09-09T12:12:12");
    assertThat(timeout.getTimeDuration()).isNull();
  }

  @Test
  void testOperatonTimeoutDurationTaskListenerByDelegateExpression() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
          .userTask("task")
            .operatonTaskListenerDelegateExpressionTimeoutWithDuration("timeout-1", "aDelegate", "PT1H")
        .endEvent()
        .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonTaskListener> taskListeners = extensionElements.getChildElementsByType(OperatonTaskListener.class);
    assertThat(taskListeners).hasSize(1);

    OperatonTaskListener taskListener = taskListeners.iterator().next();
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo("aDelegate");
    assertThat(taskListener.getOperatonEvent()).isEqualTo("timeout");

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNotNull();
    assertThat(timeout.getTimeDuration().getRawTextContent()).isEqualTo("PT1H");
  }

  @Test
  void testOperatonExecutionListenerByClassName() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .operatonExecutionListenerClass("start", "aClass")
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonExecutionListener> executionListeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    assertThat(executionListeners).hasSize(1);

    OperatonExecutionListener executionListener = executionListeners.iterator().next();
    assertThat(executionListener.getOperatonClass()).isEqualTo("aClass");
    assertThat(executionListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonExecutionListenerByClass() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .operatonExecutionListenerClass("start", this.getClass())
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonExecutionListener> executionListeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    assertThat(executionListeners).hasSize(1);

    OperatonExecutionListener executionListener = executionListeners.iterator().next();
    assertThat(executionListener.getOperatonClass()).isEqualTo(this.getClass().getName());
    assertThat(executionListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonExecutionListenerByExpression() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .operatonExecutionListenerExpression("start", "anExpression")
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonExecutionListener> executionListeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    assertThat(executionListeners).hasSize(1);

    OperatonExecutionListener executionListener = executionListeners.iterator().next();
    assertThat(executionListener.getOperatonExpression()).isEqualTo("anExpression");
    assertThat(executionListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testOperatonExecutionListenerByDelegateExpression() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .operatonExecutionListenerDelegateExpression("start", "aDelegateExpression")
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    ExtensionElements extensionElements = userTask.getExtensionElements();
    Collection<OperatonExecutionListener> executionListeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    assertThat(executionListeners).hasSize(1);

    OperatonExecutionListener executionListener = executionListeners.iterator().next();
    assertThat(executionListener.getOperatonDelegateExpression()).isEqualTo("aDelegateExpression");
    assertThat(executionListener.getOperatonEvent()).isEqualTo("start");
  }

  @Test
  void testMultiInstanceLoopCharacteristicsSequential() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
        .multiInstance()
          .sequential()
          .cardinality("card")
          .completionCondition("compl")
          .operatonCollection("coll")
          .operatonElementVariable("element")
        .multiInstanceDone()
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
        userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

    assertThat(miCharacteristics).hasSize(1);

    MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
    assertThat(miCharacteristic.isSequential()).isTrue();
    assertThat(miCharacteristic.getLoopCardinality().getTextContent()).isEqualTo("card");
    assertThat(miCharacteristic.getCompletionCondition().getTextContent()).isEqualTo("compl");
    assertThat(miCharacteristic.getOperatonCollection()).isEqualTo("coll");
    assertThat(miCharacteristic.getOperatonElementVariable()).isEqualTo("element");

  }

  @Test
  void testMultiInstanceLoopCharacteristicsParallel() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
        .multiInstance()
          .parallel()
        .multiInstanceDone()
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
      userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

    assertThat(miCharacteristics).hasSize(1);

    MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
    assertThat(miCharacteristic.isSequential()).isFalse();
  }

  @Test
  void testTaskWithOperatonInputOutput() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
        .operatonInputParameter("foo", "bar")
        .operatonInputParameter("yoo", "hoo")
        .operatonOutputParameter("one", "two")
        .operatonOutputParameter("three", "four")
      .endEvent()
      .done();

    UserTask task = modelInstance.getModelElementById("task");
    assertOperatonInputOutputParameter(task);
  }

  @Test
  void testMultiInstanceLoopCharacteristicsAsynchronousMultiInstanceAsyncBeforeElement() {
    modelInstance = Bpmn.createProcess()
            .startEvent()
            .userTask("task")
            .multiInstance()
            .operatonAsyncBefore()
            .parallel()
            .multiInstanceDone()
            .endEvent()
            .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
            userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

    assertThat(miCharacteristics).hasSize(1);

    MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
    assertThat(miCharacteristic.isSequential()).isFalse();
    assertThat(miCharacteristic.isOperatonAsyncAfter()).isFalse();
    assertThat(miCharacteristic.isOperatonAsyncBefore()).isTrue();
  }

  @Test
  void testMultiInstanceLoopCharacteristicsAsynchronousMultiInstanceAsyncAfterElement() {
    modelInstance = Bpmn.createProcess()
            .startEvent()
            .userTask("task")
            .multiInstance()
            .operatonAsyncAfter()
            .parallel()
            .multiInstanceDone()
            .endEvent()
            .done();

    UserTask userTask = modelInstance.getModelElementById("task");
    Collection<MultiInstanceLoopCharacteristics> miCharacteristics =
            userTask.getChildElementsByType(MultiInstanceLoopCharacteristics.class);

    assertThat(miCharacteristics).hasSize(1);

    MultiInstanceLoopCharacteristics miCharacteristic = miCharacteristics.iterator().next();
    assertThat(miCharacteristic.isSequential()).isFalse();
    assertThat(miCharacteristic.isOperatonAsyncAfter()).isTrue();
    assertThat(miCharacteristic.isOperatonAsyncBefore()).isFalse();
  }

  @Test
  void testTaskWithOperatonInputOutputWithExistingExtensionElements() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
        .operatonExecutionListenerExpression("end", "${true}")
        .operatonInputParameter("foo", "bar")
        .operatonInputParameter("yoo", "hoo")
        .operatonOutputParameter("one", "two")
        .operatonOutputParameter("three", "four")
      .endEvent()
      .done();

    UserTask task = modelInstance.getModelElementById("task");
    assertOperatonInputOutputParameter(task);
  }

  @Test
  void testTaskWithOperatonInputOutputWithExistingOperatonInputOutput() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
        .operatonInputParameter("foo", "bar")
        .operatonOutputParameter("one", "two")
      .endEvent()
      .done();

    UserTask task = modelInstance.getModelElementById("task");

    task.builder()
      .operatonInputParameter("yoo", "hoo")
      .operatonOutputParameter("three", "four");

    assertOperatonInputOutputParameter(task);
  }

  @Test
  void testSubProcessWithOperatonInputOutput() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess("subProcess")
        .operatonInputParameter("foo", "bar")
        .operatonInputParameter("yoo", "hoo")
        .operatonOutputParameter("one", "two")
        .operatonOutputParameter("three", "four")
        .embeddedSubProcess()
          .startEvent()
          .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    SubProcess subProcess = modelInstance.getModelElementById("subProcess");
    assertOperatonInputOutputParameter(subProcess);
  }

  @Test
  void testSubProcessWithOperatonInputOutputWithExistingExtensionElements() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess("subProcess")
        .operatonExecutionListenerExpression("end", "${true}")
        .operatonInputParameter("foo", "bar")
        .operatonInputParameter("yoo", "hoo")
        .operatonOutputParameter("one", "two")
        .operatonOutputParameter("three", "four")
        .embeddedSubProcess()
          .startEvent()
          .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    SubProcess subProcess = modelInstance.getModelElementById("subProcess");
    assertOperatonInputOutputParameter(subProcess);
  }

  @Test
  void testSubProcessWithOperatonInputOutputWithExistingOperatonInputOutput() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess("subProcess")
        .operatonInputParameter("foo", "bar")
        .operatonOutputParameter("one", "two")
        .embeddedSubProcess()
          .startEvent()
          .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    SubProcess subProcess = modelInstance.getModelElementById("subProcess");

    subProcess.builder()
      .operatonInputParameter("yoo", "hoo")
      .operatonOutputParameter("three", "four");

    assertOperatonInputOutputParameter(subProcess);
  }

  @Test
  void testTimerStartEventWithDate() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").timerWithDate(TIMER_DATE)
      .done();

    assertTimerWithDate("start", TIMER_DATE);
  }

  @Test
  void testTimerStartEventWithDuration() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").timerWithDuration(TIMER_DURATION)
      .done();

    assertTimerWithDuration("start", TIMER_DURATION);
  }

  @Test
  void testTimerStartEventWithCycle() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start").timerWithCycle(TIMER_CYCLE)
      .done();

    assertTimerWithCycle("start", TIMER_CYCLE);
  }

  @Test
  void testIntermediateTimerCatchEventWithDate() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch").timerWithDate(TIMER_DATE)
      .done();

    assertTimerWithDate("catch", TIMER_DATE);
  }

  @Test
  void testIntermediateTimerCatchEventWithDuration() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch").timerWithDuration(TIMER_DURATION)
      .done();

    assertTimerWithDuration("catch", TIMER_DURATION);
  }

  @Test
  void testIntermediateTimerCatchEventWithCycle() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent("catch").timerWithCycle(TIMER_CYCLE)
      .done();

    assertTimerWithCycle("catch", TIMER_CYCLE);
  }

  @Test
  void testTimerBoundaryEventWithDate() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").timerWithDate(TIMER_DATE)
      .done();

    assertTimerWithDate("boundary", TIMER_DATE);
  }

  @Test
  void testTimerBoundaryEventWithDuration() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").timerWithDuration(TIMER_DURATION)
      .done();

    assertTimerWithDuration("boundary", TIMER_DURATION);
  }

  @Test
  void testTimerBoundaryEventWithCycle() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").timerWithCycle(TIMER_CYCLE)
      .done();

    assertTimerWithCycle("boundary", TIMER_CYCLE);
  }

  @Test
  void testNotCancelingBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .boundaryEvent("boundary").cancelActivity(false)
      .done();

    BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
    assertThat(boundaryEvent.cancelActivity()).isFalse();
  }

  @Test
  void testCatchAllErrorBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").error()
      .endEvent("boundaryEnd")
      .done();

    ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition("boundary", ErrorEventDefinition.class);
    assertThat(errorEventDefinition.getError()).isNull();
  }

  @Test
  void testCompensationTask() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .boundaryEvent("boundary")
        .compensateEventDefinition().compensateEventDefinitionDone()
        .compensationStart()
        .userTask("compensate").name("compensate")
        .compensationDone()
      .endEvent("theend")
      .done();

    // Checking Association
    Collection<Association> associations = modelInstance.getModelElementsByType(Association.class);
    assertThat(associations).hasSize(1);
    Association association = associations.iterator().next();
    assertThat(association.getSource().getId()).isEqualTo("boundary");
    assertThat(association.getTarget().getId()).isEqualTo("compensate");
    assertThat(association.getAssociationDirection()).isEqualTo(AssociationDirection.One);

    // Checking Sequence flow
    UserTask task = modelInstance.getModelElementById("task");
    Collection<SequenceFlow> outgoing = task.getOutgoing();
    assertThat(outgoing).hasSize(1);
    SequenceFlow flow = outgoing.iterator().next();
    assertThat(flow.getSource().getId()).isEqualTo("task");
    assertThat(flow.getTarget().getId()).isEqualTo("theend");

  }

  @Test
  void testOnlyOneCompensateBoundaryEventAllowed() {
    // given
    UserTaskBuilder builder = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .boundaryEvent("boundary")
      .compensateEventDefinition().compensateEventDefinitionDone()
      .compensationStart()
      .userTask("compensate").name("compensate");
    // when
    assertThatThrownBy(builder::userTask)
      .isInstanceOf(BpmnModelException.class)
      .hasMessageContaining("Only single compensation handler allowed. Call compensationDone() to continue main flow.");
  }

  @Test
  void testInvalidCompensationStartCall() {
    // given
    StartEventBuilder builder = Bpmn.createProcess().startEvent();

    // when
    assertThatThrownBy(builder::compensationStart)
      .isInstanceOf(BpmnModelException.class)
      .hasMessageContaining("Compensation can only be started on a boundary event with a compensation event definition");
  }

  @Test
  void testInvalidCompensationDoneCall() {
    // given
    var builder = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .boundaryEvent("boundary")
      .compensateEventDefinition().compensateEventDefinitionDone();

    // when
    assertThatThrownBy(builder::compensationDone)
      .isInstanceOf(BpmnModelException.class)
      .hasMessageContaining("No compensation in progress. Call compensationStart() first.");
  }

  @Test
  void testErrorBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").error("myErrorCode", "errorMessage")
      .endEvent("boundaryEnd")
      .done();

    assertErrorEventDefinition("boundary", "myErrorCode", "errorMessage");

    UserTask userTask = modelInstance.getModelElementById("task");
    BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
    EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

    // boundary event is attached to the user task
    assertThat(boundaryEvent.getAttachedTo()).isEqualTo(userTask);

    // boundary event has no incoming sequence flows
    assertThat(boundaryEvent.getIncoming()).isEmpty();

    // the next flow node is the boundary end event
    List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
    assertThat(succeedingNodes).containsOnly(boundaryEnd);
  }

  @Test
  void testErrorBoundaryEventWithoutErrorMessage() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
        .userTask("task")
        .endEvent()
        .moveToActivity("task")
        .boundaryEvent("boundary").error("myErrorCode")
        .endEvent("boundaryEnd")
        .done();

    assertErrorEventDefinition("boundary", "myErrorCode", null);
  }

  @Test
  void testErrorDefinitionForBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary")
        .errorEventDefinition("event")
          .errorCodeVariable("errorCodeVariable")
          .errorMessageVariable("errorMessageVariable")
          .error("errorCode", "errorMessage")
        .errorEventDefinitionDone()
      .endEvent("boundaryEnd")
      .done();

    assertErrorEventDefinition("boundary", "errorCode", "errorMessage");
    assertErrorEventDefinitionForErrorVariables("boundary", "errorCodeVariable", "errorMessageVariable");
  }

  @Test
  void testErrorDefinitionForBoundaryEventWithoutEventDefinitionId() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary")
        .errorEventDefinition()
          .errorCodeVariable("errorCodeVariable")
          .errorMessageVariable("errorMessageVariable")
          .error("errorCode", "errorMessage")
        .errorEventDefinitionDone()
      .endEvent("boundaryEnd")
      .done();

    Bpmn.writeModelToStream(System.out, modelInstance);

    assertErrorEventDefinition("boundary", "errorCode", "errorMessage");
    assertErrorEventDefinitionForErrorVariables("boundary", "errorCodeVariable", "errorMessageVariable");
  }

  @Test
  void testErrorEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end").error("myErrorCode", "errorMessage")
      .done();

    assertErrorEventDefinition("end", "myErrorCode", "errorMessage");
  }

  @Test
  void testErrorEndEventWithoutErrorMessage() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
        .endEvent("end").error("myErrorCode")
        .done();

    assertErrorEventDefinition("end", "myErrorCode", null);
  }

  @Test
  void testErrorEndEventWithExistingError() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent("end").error("myErrorCode", "errorMessage")
      .moveToActivity("task")
      .boundaryEvent("boundary").error("myErrorCode")
      .endEvent("boundaryEnd")
      .done();

    Error boundaryError = assertErrorEventDefinition("boundary", "myErrorCode", "errorMessage");
    Error endError = assertErrorEventDefinition("end", "myErrorCode", "errorMessage");

    assertThat(boundaryError).isEqualTo(endError);

    assertOnlyOneErrorExists("myErrorCode");
  }

  @Test
  void testErrorStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
        .error("myErrorCode", "errorMessage")
        .endEvent()
      .done();

    assertErrorEventDefinition("subProcessStart", "myErrorCode", "errorMessage");
  }

  @Test
  void testErrorStartEventWithoutErrorMessage() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
        .endEvent()
        .subProcess()
          .triggerByEvent()
          .embeddedSubProcess()
            .startEvent("subProcessStart")
            .error("myErrorCode")
            .endEvent()
        .done();

    assertErrorEventDefinition("subProcessStart", "myErrorCode", null);
  }

  @Test
  void testCatchAllErrorStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
        .error()
        .endEvent()
      .done();

    ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition("subProcessStart", ErrorEventDefinition.class);
    assertThat(errorEventDefinition.getError()).isNull();
  }

  @Test
  void testCatchAllEscalationBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .moveToActivity("task")
      .boundaryEvent("boundary").escalation()
      .endEvent("boundaryEnd")
      .done();

    EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition("boundary", EscalationEventDefinition.class);
    assertThat(escalationEventDefinition.getEscalation()).isNull();
  }

  @Test
  void testEscalationBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .subProcess("subProcess")
      .endEvent()
      .moveToActivity("subProcess")
      .boundaryEvent("boundary").escalation("myEscalationCode")
      .endEvent("boundaryEnd")
      .done();

    assertEscalationEventDefinition("boundary", "myEscalationCode");

    SubProcess subProcess = modelInstance.getModelElementById("subProcess");
    BoundaryEvent boundaryEvent = modelInstance.getModelElementById("boundary");
    EndEvent boundaryEnd = modelInstance.getModelElementById("boundaryEnd");

    // boundary event is attached to the sub process
    assertThat(boundaryEvent.getAttachedTo()).isEqualTo(subProcess);

    // boundary event has no incoming sequence flows
    assertThat(boundaryEvent.getIncoming()).isEmpty();

    // the next flow node is the boundary end event
    List<FlowNode> succeedingNodes = boundaryEvent.getSucceedingNodes().list();
    assertThat(succeedingNodes).containsOnly(boundaryEnd);
  }

  @Test
  void testEscalationEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent("end").escalation("myEscalationCode")
      .done();

    assertEscalationEventDefinition("end", "myEscalationCode");
  }

  @Test
  void testEscalationStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
        .escalation("myEscalationCode")
        .endEvent()
      .done();

    assertEscalationEventDefinition("subProcessStart", "myEscalationCode");
  }

  @Test
  void testCatchAllEscalationStartEvent() {
    modelInstance = Bpmn.createProcess()
        .startEvent()
        .endEvent()
        .subProcess()
          .triggerByEvent()
          .embeddedSubProcess()
          .startEvent("subProcessStart")
          .escalation()
          .endEvent()
        .done();

    EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition("subProcessStart", EscalationEventDefinition.class);
    assertThat(escalationEventDefinition.getEscalation()).isNull();
  }

  @Test
  void testIntermediateEscalationThrowEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateThrowEvent("throw").escalation("myEscalationCode")
      .endEvent()
      .done();

    assertEscalationEventDefinition("throw", "myEscalationCode");
  }

  @Test
  void testEscalationEndEventWithExistingEscalation() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("task")
      .endEvent("end").escalation("myEscalationCode")
      .moveToActivity("task")
      .boundaryEvent("boundary").escalation("myEscalationCode")
      .endEvent("boundaryEnd")
      .done();

    Escalation boundaryEscalation = assertEscalationEventDefinition("boundary", "myEscalationCode");
    Escalation endEscalation = assertEscalationEventDefinition("end", "myEscalationCode");

    assertThat(boundaryEscalation).isEqualTo(endEscalation);

    assertOnlyOneEscalationExists("myEscalationCode");

  }

  @Test
  void testCompensationStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
        .compensation()
        .endEvent()
      .done();

    assertCompensationEventDefinition("subProcessStart");
  }

  @Test
  void testInterruptingStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
          .interrupting(true)
          .error()
        .endEvent()
      .done();

    StartEvent startEvent = modelInstance.getModelElementById("subProcessStart");
    assertThat(startEvent).isNotNull();
    assertThat(startEvent.isInterrupting()).isTrue();
  }

  @Test
  void testNonInterruptingStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent("subProcessStart")
          .interrupting(false)
          .error()
        .endEvent()
      .done();

    StartEvent startEvent = modelInstance.getModelElementById("subProcessStart");
    assertThat(startEvent).isNotNull();
    assertThat(startEvent.isInterrupting()).isFalse();
  }

  @Test
  void testUserTaskOperatonFormField() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask(TASK_ID)
        .operatonFormField()
          .operatonId("myFormField_1")
          .operatonLabel("Form Field One")
          .operatonType("string")
          .operatonDefaultValue("myDefaultVal_1")
         .operatonFormFieldDone()
        .operatonFormField()
          .operatonId("myFormField_2")
          .operatonLabel("Form Field Two")
          .operatonType("integer")
          .operatonDefaultValue("myDefaultVal_2")
         .operatonFormFieldDone()
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById(TASK_ID);
    assertOperatonFormField(userTask);
  }

  @Test
  void testUserTaskOperatonFormFieldWithExistingOperatonFormData() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask(TASK_ID)
        .operatonFormField()
          .operatonId("myFormField_1")
          .operatonLabel("Form Field One")
          .operatonType("string")
          .operatonDefaultValue("myDefaultVal_1")
         .operatonFormFieldDone()
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById(TASK_ID);

    userTask.builder()
      .operatonFormField()
        .operatonId("myFormField_2")
        .operatonLabel("Form Field Two")
        .operatonType("integer")
        .operatonDefaultValue("myDefaultVal_2")
       .operatonFormFieldDone();

    assertOperatonFormField(userTask);
  }

  @Test
  void testStartEventOperatonFormField() {
    modelInstance = Bpmn.createProcess()
      .startEvent(START_EVENT_ID)
        .operatonFormField()
          .operatonId("myFormField_1")
          .operatonLabel("Form Field One")
          .operatonType("string")
          .operatonDefaultValue("myDefaultVal_1")
         .operatonFormFieldDone()
         .operatonFormField()
         .operatonId("myFormField_2")
          .operatonLabel("Form Field Two")
          .operatonType("integer")
          .operatonDefaultValue("myDefaultVal_2")
         .operatonFormFieldDone()
      .endEvent()
      .done();

    StartEvent startEvent = modelInstance.getModelElementById(START_EVENT_ID);
    assertOperatonFormField(startEvent);
  }

  @Test
  void testUserTaskOperatonFormRef() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask(TASK_ID)
        .operatonFormRef(FORM_ID)
        .operatonFormRefBinding(TEST_STRING_FORM_REF_BINDING)
        .operatonFormRefVersion(TEST_STRING_FORM_REF_VERSION)
      .endEvent()
      .done();

    UserTask userTask = modelInstance.getModelElementById(TASK_ID);
    assertThat(userTask.getOperatonFormRef()).isEqualTo(FORM_ID);
    assertThat(userTask.getOperatonFormRefBinding()).isEqualTo(TEST_STRING_FORM_REF_BINDING);
    assertThat(userTask.getOperatonFormRefVersion()).isEqualTo(TEST_STRING_FORM_REF_VERSION);
  }

  @Test
  void testStartEventOperatonFormRef() {
    modelInstance = Bpmn.createProcess()
        .startEvent(START_EVENT_ID)
          .operatonFormRef(FORM_ID)
          .operatonFormRefBinding(TEST_STRING_FORM_REF_BINDING)
          .operatonFormRefVersion(TEST_STRING_FORM_REF_VERSION)
        .userTask()
        .endEvent()
        .done();

    StartEvent startEvent = modelInstance.getModelElementById(START_EVENT_ID);
    assertThat(startEvent.getOperatonFormRef()).isEqualTo(FORM_ID);
    assertThat(startEvent.getOperatonFormRefBinding()).isEqualTo(TEST_STRING_FORM_REF_BINDING);
    assertThat(startEvent.getOperatonFormRefVersion()).isEqualTo(TEST_STRING_FORM_REF_VERSION);
  }

  @Test
  void testCompensateEventDefinitionCatchStartEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent("start")
        .compensateEventDefinition()
        .waitForCompletion(false)
        .compensateEventDefinitionDone()
      .userTask("userTask")
      .endEvent("end")
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("start", CompensateEventDefinition.class);
    Activity activity = eventDefinition.getActivity();
    assertThat(activity).isNull();
    assertThat(eventDefinition.isWaitForCompletion()).isFalse();
  }


  @Test
  void testCompensateEventDefinitionCatchBoundaryEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .boundaryEvent("catch")
        .compensateEventDefinition()
        .waitForCompletion(false)
        .compensateEventDefinitionDone()
      .endEvent("end")
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("catch", CompensateEventDefinition.class);
    Activity activity = eventDefinition.getActivity();
    assertThat(activity).isNull();
    assertThat(eventDefinition.isWaitForCompletion()).isFalse();
  }

  @Test
  void testCompensateEventDefinitionCatchBoundaryEventWithId() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .boundaryEvent("catch")
        .compensateEventDefinition("foo")
        .waitForCompletion(false)
        .compensateEventDefinitionDone()
      .endEvent("end")
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("catch", CompensateEventDefinition.class);
    assertThat(eventDefinition.getId()).isEqualTo("foo");
  }

  @Test
  void testCompensateEventDefinitionThrowEndEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .endEvent("end")
        .compensateEventDefinition()
        .activityRef("userTask")
        .waitForCompletion(true)
        .compensateEventDefinitionDone()
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("end", CompensateEventDefinition.class);
    Activity activity = eventDefinition.getActivity();
    assertThat(activity).isEqualTo(modelInstance.getModelElementById("userTask"));
    assertThat(eventDefinition.isWaitForCompletion()).isTrue();
  }

  @Test
  void testCompensateEventDefinitionThrowIntermediateEvent() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .intermediateThrowEvent("throw")
        .compensateEventDefinition()
        .activityRef("userTask")
        .waitForCompletion(true)
        .compensateEventDefinitionDone()
      .endEvent("end")
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("throw", CompensateEventDefinition.class);
    Activity activity = eventDefinition.getActivity();
    assertThat(activity).isEqualTo(modelInstance.getModelElementById("userTask"));
    assertThat(eventDefinition.isWaitForCompletion()).isTrue();
  }

  @Test
  void testCompensateEventDefinitionThrowIntermediateEventWithId() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .intermediateCatchEvent("throw")
        .compensateEventDefinition("foo")
        .activityRef("userTask")
        .waitForCompletion(true)
        .compensateEventDefinitionDone()
      .endEvent("end")
      .done();

    CompensateEventDefinition eventDefinition = assertAndGetSingleEventDefinition("throw", CompensateEventDefinition.class);
    assertThat(eventDefinition.getId()).isEqualTo("foo");
  }

  @Test
  void testCompensateEventDefinitionReferencesNonExistingActivity() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .endEvent("end")
      .done();

    UserTask userTask = modelInstance.getModelElementById("userTask");
    UserTaskBuilder userTaskBuilder = userTask.builder();
    var compensateEventDefinitionBuilder = userTaskBuilder
      .boundaryEvent()
      .compensateEventDefinition();

    assertThatThrownBy(() -> compensateEventDefinitionBuilder.activityRef("nonExistingTask"))
        .isInstanceOf(BpmnModelException.class)
        .hasMessageContaining("Activity with id 'nonExistingTask' does not exist");
  }

  @Test
  void testCompensateEventDefinitionReferencesActivityInDifferentScope() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask("userTask")
      .subProcess()
        .embeddedSubProcess()
        .startEvent()
        .userTask("subProcessTask")
        .endEvent()
        .subProcessDone()
      .endEvent("end")
      .done();

    UserTask userTask = modelInstance.getModelElementById("userTask");
    UserTaskBuilder userTaskBuilder = userTask.builder();
    var compensateEventDefinitionBuilder = userTaskBuilder
      .boundaryEvent("boundary")
      .compensateEventDefinition();

    assertThatThrownBy(() -> compensateEventDefinitionBuilder.activityRef("subProcessTask"))
        .isInstanceOf(BpmnModelException.class)
        .hasMessageContaining("Activity with id 'subProcessTask' must be in the same scope as 'boundary'");
  }

  @Test
  void testConditionalEventDefinitionOperatonExtensions() {
    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent()
      .conditionalEventDefinition(CONDITION_ID)
        .condition(TEST_CONDITION)
        .operatonVariableEvents(TEST_CONDITIONAL_VARIABLE_EVENTS)
        .operatonVariableEvents(TEST_CONDITIONAL_VARIABLE_EVENTS_LIST)
        .operatonVariableName(TEST_CONDITIONAL_VARIABLE_NAME)
      .conditionalEventDefinitionDone()
      .endEvent()
      .done();

    ConditionalEventDefinition conditionalEventDef = modelInstance.getModelElementById(CONDITION_ID);
    assertThat(conditionalEventDef.getOperatonVariableEvents()).isEqualTo(TEST_CONDITIONAL_VARIABLE_EVENTS);
    assertThat(conditionalEventDef.getOperatonVariableEventsList()).containsAll(TEST_CONDITIONAL_VARIABLE_EVENTS_LIST);
    assertThat(conditionalEventDef.getOperatonVariableName()).isEqualTo(TEST_CONDITIONAL_VARIABLE_NAME);
  }

  @Test
  void testIntermediateConditionalEventDefinition() {

    modelInstance = Bpmn.createProcess()
      .startEvent()
      .intermediateCatchEvent(CATCH_ID)
        .conditionalEventDefinition(CONDITION_ID)
            .condition(TEST_CONDITION)
        .conditionalEventDefinitionDone()
      .endEvent()
      .done();

    ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(CATCH_ID, ConditionalEventDefinition.class);
    assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
    assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
  }

  @Test
  void testIntermediateConditionalEventDefinitionShortCut() {

    modelInstance = Bpmn.createProcess()
      .startEvent()
        .intermediateCatchEvent(CATCH_ID)
        .condition(TEST_CONDITION)
      .endEvent()
      .done();

    ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(CATCH_ID, ConditionalEventDefinition.class);
    assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
  }

  @Test
  void testBoundaryConditionalEventDefinition() {

    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask(USER_TASK_ID)
      .endEvent()
        .moveToActivity(USER_TASK_ID)
          .boundaryEvent(BOUNDARY_ID)
            .conditionalEventDefinition(CONDITION_ID)
              .condition(TEST_CONDITION)
            .conditionalEventDefinitionDone()
          .endEvent()
      .done();

    ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(BOUNDARY_ID, ConditionalEventDefinition.class);
    assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
    assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
  }

  @Test
  void testEventSubProcessConditionalStartEvent() {

    modelInstance = Bpmn.createProcess()
      .startEvent()
      .userTask()
      .endEvent()
      .subProcess()
        .triggerByEvent()
        .embeddedSubProcess()
        .startEvent(START_EVENT_ID)
          .conditionalEventDefinition(CONDITION_ID)
            .condition(TEST_CONDITION)
          .conditionalEventDefinitionDone()
        .endEvent()
      .done();

    ConditionalEventDefinition eventDefinition = assertAndGetSingleEventDefinition(START_EVENT_ID, ConditionalEventDefinition.class);
    assertThat(eventDefinition.getId()).isEqualTo(CONDITION_ID);
    assertThat(eventDefinition.getCondition().getTextContent()).isEqualTo(TEST_CONDITION);
  }

  protected Message assertMessageEventDefinition(String elementId, String messageName) {
    MessageEventDefinition messageEventDefinition = assertAndGetSingleEventDefinition(elementId, MessageEventDefinition.class);
    Message message = messageEventDefinition.getMessage();
    assertThat(message).isNotNull();
    assertThat(message.getName()).isEqualTo(messageName);

    return message;
  }

  protected void assertOnlyOneMessageExists(String messageName) {
    Collection<Message> messages = modelInstance.getModelElementsByType(Message.class);
    assertThat(messages).extracting("name").containsOnlyOnce(messageName);
  }

  protected Signal assertSignalEventDefinition(String elementId, String signalName) {
    SignalEventDefinition signalEventDefinition = assertAndGetSingleEventDefinition(elementId, SignalEventDefinition.class);
    Signal signal = signalEventDefinition.getSignal();
    assertThat(signal).isNotNull();
    assertThat(signal.getName()).isEqualTo(signalName);

    return signal;
  }

  protected void assertOnlyOneSignalExists(String signalName) {
    Collection<Signal> signals = modelInstance.getModelElementsByType(Signal.class);
    assertThat(signals).extracting("name").containsOnlyOnce(signalName);
  }

  protected Error assertErrorEventDefinition(String elementId, String errorCode, String errorMessage) {
    ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition(elementId, ErrorEventDefinition.class);
    Error error = errorEventDefinition.getError();
    assertThat(error).isNotNull();
    assertThat(error.getErrorCode()).isEqualTo(errorCode);
    assertThat(error.getOperatonErrorMessage()).isEqualTo(errorMessage);

    return error;
  }

  protected void assertErrorEventDefinitionForErrorVariables(String elementId, String errorCodeVariable, String errorMessageVariable) {
    ErrorEventDefinition errorEventDefinition = assertAndGetSingleEventDefinition(elementId, ErrorEventDefinition.class);
    assertThat(errorEventDefinition).isNotNull();
    if(errorCodeVariable != null) {
      assertThat(errorEventDefinition.getOperatonErrorCodeVariable()).isEqualTo(errorCodeVariable);
    }
    if(errorMessageVariable != null) {
      assertThat(errorEventDefinition.getOperatonErrorMessageVariable()).isEqualTo(errorMessageVariable);
    }
  }

  protected void assertOnlyOneErrorExists(String errorCode) {
    Collection<Error> errors = modelInstance.getModelElementsByType(Error.class);
    assertThat(errors).extracting("errorCode").containsOnlyOnce(errorCode);
  }

  protected Escalation assertEscalationEventDefinition(String elementId, String escalationCode) {
    EscalationEventDefinition escalationEventDefinition = assertAndGetSingleEventDefinition(elementId, EscalationEventDefinition.class);
    Escalation escalation = escalationEventDefinition.getEscalation();
    assertThat(escalation).isNotNull();
    assertThat(escalation.getEscalationCode()).isEqualTo(escalationCode);

    return escalation;
  }

  protected void assertOnlyOneEscalationExists(String escalationCode) {
    Collection<Escalation> escalations = modelInstance.getModelElementsByType(Escalation.class);
    assertThat(escalations).extracting("escalationCode").containsOnlyOnce(escalationCode);
  }

  protected void assertCompensationEventDefinition(String elementId) {
    assertAndGetSingleEventDefinition(elementId, CompensateEventDefinition.class);
  }

  protected void assertOperatonInputOutputParameter(BaseElement element) {
    OperatonInputOutput operatonInputOutput = element.getExtensionElements().getElementsQuery().filterByType(OperatonInputOutput.class).singleResult();
    assertThat(operatonInputOutput).isNotNull();

    List<OperatonInputParameter> operatonInputParameters = new ArrayList<>(operatonInputOutput.getOperatonInputParameters());
    assertThat(operatonInputParameters).hasSize(2);

    OperatonInputParameter operatonInputParameter = operatonInputParameters.get(0);
    assertThat(operatonInputParameter.getOperatonName()).isEqualTo("foo");
    assertThat(operatonInputParameter.getTextContent()).isEqualTo("bar");

    operatonInputParameter = operatonInputParameters.get(1);
    assertThat(operatonInputParameter.getOperatonName()).isEqualTo("yoo");
    assertThat(operatonInputParameter.getTextContent()).isEqualTo("hoo");

    List<OperatonOutputParameter> operatonOutputParameters = new ArrayList<>(operatonInputOutput.getOperatonOutputParameters());
    assertThat(operatonOutputParameters).hasSize(2);

    OperatonOutputParameter operatonOutputParameter = operatonOutputParameters.get(0);
    assertThat(operatonOutputParameter.getOperatonName()).isEqualTo("one");
    assertThat(operatonOutputParameter.getTextContent()).isEqualTo("two");

    operatonOutputParameter = operatonOutputParameters.get(1);
    assertThat(operatonOutputParameter.getOperatonName()).isEqualTo("three");
    assertThat(operatonOutputParameter.getTextContent()).isEqualTo("four");
  }

  protected void assertTimerWithDate(String elementId, String timerDate) {
    TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
    TimeDate timeDate = timerEventDefinition.getTimeDate();
    assertThat(timeDate).isNotNull();
    assertThat(timeDate.getTextContent()).isEqualTo(timerDate);
  }

  protected void assertTimerWithDuration(String elementId, String timerDuration) {
    TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
    TimeDuration timeDuration = timerEventDefinition.getTimeDuration();
    assertThat(timeDuration).isNotNull();
    assertThat(timeDuration.getTextContent()).isEqualTo(timerDuration);
  }

  protected void assertTimerWithCycle(String elementId, String timerCycle) {
    TimerEventDefinition timerEventDefinition = assertAndGetSingleEventDefinition(elementId, TimerEventDefinition.class);
    TimeCycle timeCycle = timerEventDefinition.getTimeCycle();
    assertThat(timeCycle).isNotNull();
    assertThat(timeCycle.getTextContent()).isEqualTo(timerCycle);
  }

  @SuppressWarnings("unchecked")
  protected <T extends EventDefinition> T assertAndGetSingleEventDefinition(String elementId, Class<T> eventDefinitionType) {
    BpmnModelElementInstance element = modelInstance.getModelElementById(elementId);
    assertThat(element).isNotNull();
    Collection<EventDefinition> eventDefinitions = element.getChildElementsByType(EventDefinition.class);
    assertThat(eventDefinitions).hasSize(1);

    EventDefinition eventDefinition = eventDefinitions.iterator().next();
    assertThat(eventDefinition)
      .isNotNull()
      .isInstanceOf(eventDefinitionType);
    return (T) eventDefinition;
  }

  protected void assertOperatonFormField(BaseElement element) {
    assertThat(element.getExtensionElements()).isNotNull();

    OperatonFormData operatonFormData = element.getExtensionElements().getElementsQuery().filterByType(OperatonFormData.class).singleResult();
    assertThat(operatonFormData).isNotNull();

    List<OperatonFormField> operatonFormFields = new ArrayList<>(operatonFormData.getOperatonFormFields());
    assertThat(operatonFormFields).hasSize(2);

    OperatonFormField operatonFormField = operatonFormFields.get(0);
    assertThat(operatonFormField.getOperatonId()).isEqualTo("myFormField_1");
    assertThat(operatonFormField.getOperatonLabel()).isEqualTo("Form Field One");
    assertThat(operatonFormField.getOperatonType()).isEqualTo("string");
    assertThat(operatonFormField.getOperatonDefaultValue()).isEqualTo("myDefaultVal_1");

    operatonFormField = operatonFormFields.get(1);
    assertThat(operatonFormField.getOperatonId()).isEqualTo("myFormField_2");
    assertThat(operatonFormField.getOperatonLabel()).isEqualTo("Form Field Two");
    assertThat(operatonFormField.getOperatonType()).isEqualTo("integer");
    assertThat(operatonFormField.getOperatonDefaultValue()).isEqualTo("myDefaultVal_2");

  }

  protected void assertOperatonFailedJobRetryTimeCycle(BaseElement element) {
    assertThat(element.getExtensionElements()).isNotNull();

    OperatonFailedJobRetryTimeCycle operatonFailedJobRetryTimeCycle = element.getExtensionElements().getElementsQuery().filterByType(OperatonFailedJobRetryTimeCycle.class).singleResult();
    assertThat(operatonFailedJobRetryTimeCycle).isNotNull();
    assertThat(operatonFailedJobRetryTimeCycle.getTextContent()).isEqualTo(FAILED_JOB_RETRY_TIME_CYCLE);
  }

  @Test
  void testCreateEventSubProcess() {
    ProcessBuilder process = Bpmn.createProcess();
    modelInstance = process
      .startEvent()
      .sendTask()
      .endEvent()
      .done();

    EventSubProcessBuilder eventSubProcess = process.eventSubProcess();
    eventSubProcess
      .startEvent()
      .userTask()
      .endEvent();

    SubProcess subProcess = eventSubProcess.getElement();

    // no input or output from the sub process
    assertThat(subProcess.getIncoming()).isEmpty();
    assertThat(subProcess.getOutgoing()).isEmpty();

    // subProcess was triggered by event
    assertThat(eventSubProcess.getElement().triggeredByEvent()).isTrue();

    // subProcess contains startEvent, sendTask and endEvent
    assertThat(subProcess.getChildElementsByType(StartEvent.class)).isNotNull();
    assertThat(subProcess.getChildElementsByType(UserTask.class)).isNotNull();
    assertThat(subProcess.getChildElementsByType(EndEvent.class)).isNotNull();
  }


  @Test
  void testCreateEventSubProcessInSubProcess() {
    ProcessBuilder process = Bpmn.createProcess();
    modelInstance = process
      .startEvent()
      .subProcess("mysubprocess")
        .embeddedSubProcess()
        .startEvent()
        .userTask()
        .endEvent()
        .subProcessDone()
      .userTask()
      .endEvent()
      .done();

    SubProcess subprocess = modelInstance.getModelElementById("mysubprocess");
    subprocess
      .builder()
      .embeddedSubProcess()
        .eventSubProcess("myeventsubprocess")
        .startEvent()
        .userTask()
        .endEvent()
        .subProcessDone();

    SubProcess eventSubProcess = modelInstance.getModelElementById("myeventsubprocess");

    // no input or output from the sub process
    assertThat(eventSubProcess.getIncoming()).isEmpty();
    assertThat(eventSubProcess.getOutgoing()).isEmpty();

    // subProcess was triggered by event
    assertThat(eventSubProcess.triggeredByEvent()).isTrue();

    // subProcess contains startEvent, sendTask and endEvent
    assertThat(eventSubProcess.getChildElementsByType(StartEvent.class)).isNotNull();
    assertThat(eventSubProcess.getChildElementsByType(UserTask.class)).isNotNull();
    assertThat(eventSubProcess.getChildElementsByType(EndEvent.class)).isNotNull();
  }

  @Test
  void testCreateEventSubProcessError() {
    ProcessBuilder process = Bpmn.createProcess();
    modelInstance = process
      .startEvent()
      .sendTask()
      .endEvent()
      .done();

    EventSubProcessBuilder eventSubProcess = process.eventSubProcess();
    eventSubProcess
      .startEvent()
      .userTask()
      .endEvent();

    assertThatThrownBy(eventSubProcess::subProcessDone, "eventSubProcess has returned a builder after completion")
        .isInstanceOf(BpmnModelException.class)
        .hasMessageContaining("Unable to find a parent subProcess.");
  }

  @Test
  void testSetIdAsDefaultNameForFlowElements() {
    BpmnModelInstance instance = Bpmn.createExecutableProcess("process")
        .startEvent("start")
        .userTask("user")
        .endEvent("end")
          .name("name")
        .done();

    String startName = ((FlowElement) instance.getModelElementById("start")).getName();
    assertThat(startName).isEqualTo("start");
    String userName = ((FlowElement) instance.getModelElementById("user")).getName();
    assertThat(userName).isEqualTo("user");
    String endName = ((FlowElement) instance.getModelElementById("end")).getName();
    assertThat(endName).isEqualTo("name");
  }

}
