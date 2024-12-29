/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.bpmn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.model.bpmn.instance.Error;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.instance.operaton.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.*;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * @author Sebastian Menski
 * @author Ronny Bräunlich
 */
public class OperatonExtensionsTest {

  private Process process;
  private StartEvent startEvent;
  private SequenceFlow sequenceFlow;
  private UserTask userTask;
  private ServiceTask serviceTask;
  private SendTask sendTask;
  private ScriptTask scriptTask;
  private CallActivity callActivity;
  private BusinessRuleTask businessRuleTask;
  private EndEvent endEvent;
  private MessageEventDefinition messageEventDefinition;
  private ParallelGateway parallelGateway;
  private BpmnModelInstance originalModelInstance;
  private BpmnModelInstance modelInstance;
  private Error error;

  public static Collection<Object[]> parameters(){
    return Arrays.asList(new Object[][]{
        {OPERATON_NS, Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsTest.xml"))},
        //for compatability reasons we gotta check the old namespace, too
        {CAMUNDA_NS, Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatabilityTest.xml"))}
    });
  }

  public void initOperatonExtensionsTest(String namespace, BpmnModelInstance modelInstance) {
    this.originalModelInstance = modelInstance;
    setUp();
  }

  public void setUp(){
    modelInstance = originalModelInstance.clone();
    process = modelInstance.getModelElementById(PROCESS_ID);
    startEvent = modelInstance.getModelElementById(START_EVENT_ID);
    sequenceFlow = modelInstance.getModelElementById(SEQUENCE_FLOW_ID);
    userTask = modelInstance.getModelElementById(USER_TASK_ID);
    serviceTask = modelInstance.getModelElementById(SERVICE_TASK_ID);
    sendTask = modelInstance.getModelElementById(SEND_TASK_ID);
    scriptTask = modelInstance.getModelElementById(SCRIPT_TASK_ID);
    callActivity = modelInstance.getModelElementById(CALL_ACTIVITY_ID);
    businessRuleTask = modelInstance.getModelElementById(BUSINESS_RULE_TASK);
    endEvent = modelInstance.getModelElementById(END_EVENT_ID);
    messageEventDefinition = (MessageEventDefinition) endEvent.getEventDefinitions().iterator().next();
    parallelGateway = modelInstance.getModelElementById("parallelGateway");
    error = modelInstance.getModelElementById("error");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testAssignee(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonAssignee()).isEqualTo(TEST_STRING_XML);
    userTask.setOperatonAssignee(TEST_STRING_API);
    assertThat(userTask.getOperatonAssignee()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testAsync(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.isOperatonAsync()).isFalse();
    assertThat(userTask.isOperatonAsync()).isTrue();
    assertThat(parallelGateway.isOperatonAsync()).isTrue();

    startEvent.setOperatonAsync(true);
    userTask.setOperatonAsync(false);
    parallelGateway.setOperatonAsync(false);

    assertThat(startEvent.isOperatonAsync()).isTrue();
    assertThat(userTask.isOperatonAsync()).isFalse();
    assertThat(parallelGateway.isOperatonAsync()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testAsyncBefore(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.isOperatonAsyncBefore()).isTrue();
    assertThat(endEvent.isOperatonAsyncBefore()).isTrue();
    assertThat(userTask.isOperatonAsyncBefore()).isTrue();
    assertThat(parallelGateway.isOperatonAsyncBefore()).isTrue();

    startEvent.setOperatonAsyncBefore(false);
    endEvent.setOperatonAsyncBefore(false);
    userTask.setOperatonAsyncBefore(false);
    parallelGateway.setOperatonAsyncBefore(false);

    assertThat(startEvent.isOperatonAsyncBefore()).isFalse();
    assertThat(endEvent.isOperatonAsyncBefore()).isFalse();
    assertThat(userTask.isOperatonAsyncBefore()).isFalse();
    assertThat(parallelGateway.isOperatonAsyncBefore()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testAsyncAfter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.isOperatonAsyncAfter()).isTrue();
    assertThat(endEvent.isOperatonAsyncAfter()).isTrue();
    assertThat(userTask.isOperatonAsyncAfter()).isTrue();
    assertThat(parallelGateway.isOperatonAsyncAfter()).isTrue();

    startEvent.setOperatonAsyncAfter(false);
    endEvent.setOperatonAsyncAfter(false);
    userTask.setOperatonAsyncAfter(false);
    parallelGateway.setOperatonAsyncAfter(false);

    assertThat(startEvent.isOperatonAsyncAfter()).isFalse();
    assertThat(endEvent.isOperatonAsyncAfter()).isFalse();
    assertThat(userTask.isOperatonAsyncAfter()).isFalse();
    assertThat(parallelGateway.isOperatonAsyncAfter()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFlowNodeJobPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.getOperatonJobPriority()).isEqualTo(TEST_FLOW_NODE_JOB_PRIORITY);
    assertThat(endEvent.getOperatonJobPriority()).isEqualTo(TEST_FLOW_NODE_JOB_PRIORITY);
    assertThat(userTask.getOperatonJobPriority()).isEqualTo(TEST_FLOW_NODE_JOB_PRIORITY);
    assertThat(parallelGateway.getOperatonJobPriority()).isEqualTo(TEST_FLOW_NODE_JOB_PRIORITY);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testProcessJobPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonJobPriority()).isEqualTo(TEST_PROCESS_JOB_PRIORITY);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testProcessTaskPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonTaskPriority()).isEqualTo(TEST_PROCESS_TASK_PRIORITY);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testHistoryTimeToLive(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonHistoryTimeToLive()).isEqualTo(TEST_HISTORY_TIME_TO_LIVE);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testIsStartableInTasklist(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.isOperatonStartableInTasklist()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testVersionTag(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonVersionTag()).isEqualTo("v1.0.0");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testServiceTaskPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCalledElementBinding(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCalledElementBinding()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCalledElementBinding(TEST_STRING_API);
    assertThat(callActivity.getOperatonCalledElementBinding()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCalledElementVersion(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCalledElementVersion()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCalledElementVersion(TEST_STRING_API);
    assertThat(callActivity.getOperatonCalledElementVersion()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCalledElementVersionTag(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCalledElementVersionTag()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCalledElementVersionTag(TEST_STRING_API);
    assertThat(callActivity.getOperatonCalledElementVersionTag()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCalledElementTenantId(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCalledElementTenantId()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCalledElementTenantId(TEST_STRING_API);
    assertThat(callActivity.getOperatonCalledElementTenantId()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCaseRef(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCaseRef()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCaseRef(TEST_STRING_API);
    assertThat(callActivity.getOperatonCaseRef()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCaseBinding(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCaseBinding()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCaseBinding(TEST_STRING_API);
    assertThat(callActivity.getOperatonCaseBinding()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCaseVersion(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCaseVersion()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCaseVersion(TEST_STRING_API);
    assertThat(callActivity.getOperatonCaseVersion()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCaseTenantId(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonCaseTenantId()).isEqualTo(TEST_STRING_XML);
    callActivity.setOperatonCaseTenantId(TEST_STRING_API);
    assertThat(callActivity.getOperatonCaseTenantId()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDecisionRef(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonDecisionRef()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonDecisionRef(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonDecisionRef()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDecisionRefBinding(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonDecisionRefBinding()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonDecisionRefBinding(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonDecisionRefBinding()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDecisionRefVersion(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonDecisionRefVersion()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonDecisionRefVersion(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonDecisionRefVersion()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDecisionRefVersionTag(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonDecisionRefVersionTag()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonDecisionRefVersionTag(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonDecisionRefVersionTag()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDecisionRefTenantId(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonDecisionRefTenantId()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonDecisionRefTenantId(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonDecisionRefTenantId()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testMapDecisionResult(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonMapDecisionResult()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonMapDecisionResult(TEST_STRING_API);
    assertThat(businessRuleTask.getOperatonMapDecisionResult()).isEqualTo(TEST_STRING_API);
  }


  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testTaskPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(businessRuleTask.getOperatonTaskPriority()).isEqualTo(TEST_STRING_XML);
    businessRuleTask.setOperatonTaskPriority(TEST_SERVICE_TASK_PRIORITY);
    assertThat(businessRuleTask.getOperatonTaskPriority()).isEqualTo(TEST_SERVICE_TASK_PRIORITY);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCandidateGroups(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateGroups()).isEqualTo(TEST_GROUPS_XML);
    assertThat(userTask.getOperatonCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
    userTask.setOperatonCandidateGroups(TEST_GROUPS_API);
    assertThat(userTask.getOperatonCandidateGroups()).isEqualTo(TEST_GROUPS_API);
    assertThat(userTask.getOperatonCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_API);
    userTask.setOperatonCandidateGroupsList(TEST_GROUPS_LIST_XML);
    assertThat(userTask.getOperatonCandidateGroups()).isEqualTo(TEST_GROUPS_XML);
    assertThat(userTask.getOperatonCandidateGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCandidateStarterGroups(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonCandidateStarterGroups()).isEqualTo(TEST_GROUPS_XML);
    assertThat(process.getOperatonCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
    process.setOperatonCandidateStarterGroups(TEST_GROUPS_API);
    assertThat(process.getOperatonCandidateStarterGroups()).isEqualTo(TEST_GROUPS_API);
    assertThat(process.getOperatonCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_API);
    process.setOperatonCandidateStarterGroupsList(TEST_GROUPS_LIST_XML);
    assertThat(process.getOperatonCandidateStarterGroups()).isEqualTo(TEST_GROUPS_XML);
    assertThat(process.getOperatonCandidateStarterGroupsList()).containsAll(TEST_GROUPS_LIST_XML);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCandidateStarterUsers(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(process.getOperatonCandidateStarterUsers()).isEqualTo(TEST_USERS_XML);
    assertThat(process.getOperatonCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_XML);
    process.setOperatonCandidateStarterUsers(TEST_USERS_API);
    assertThat(process.getOperatonCandidateStarterUsers()).isEqualTo(TEST_USERS_API);
    assertThat(process.getOperatonCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_API);
    process.setOperatonCandidateStarterUsersList(TEST_USERS_LIST_XML);
    assertThat(process.getOperatonCandidateStarterUsers()).isEqualTo(TEST_USERS_XML);
    assertThat(process.getOperatonCandidateStarterUsersList()).containsAll(TEST_USERS_LIST_XML);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testCandidateUsers(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateUsers()).isEqualTo(TEST_USERS_XML);
    assertThat(userTask.getOperatonCandidateUsersList()).containsAll(TEST_USERS_LIST_XML);
    userTask.setOperatonCandidateUsers(TEST_USERS_API);
    assertThat(userTask.getOperatonCandidateUsers()).isEqualTo(TEST_USERS_API);
    assertThat(userTask.getOperatonCandidateUsersList()).containsAll(TEST_USERS_LIST_API);
    userTask.setOperatonCandidateUsersList(TEST_USERS_LIST_XML);
    assertThat(userTask.getOperatonCandidateUsers()).isEqualTo(TEST_USERS_XML);
    assertThat(userTask.getOperatonCandidateUsersList()).containsAll(TEST_USERS_LIST_XML);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testClass(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonClass()).isEqualTo(TEST_CLASS_XML);
    assertThat(messageEventDefinition.getOperatonClass()).isEqualTo(TEST_CLASS_XML);

    serviceTask.setOperatonClass(TEST_CLASS_API);
    messageEventDefinition.setOperatonClass(TEST_CLASS_API);

    assertThat(serviceTask.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(messageEventDefinition.getOperatonClass()).isEqualTo(TEST_CLASS_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDelegateExpression(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
    assertThat(messageEventDefinition.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);

    serviceTask.setOperatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
    messageEventDefinition.setOperatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API);

    assertThat(serviceTask.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertThat(messageEventDefinition.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testDueDate(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonDueDate()).isEqualTo(TEST_DUE_DATE_XML);
    userTask.setOperatonDueDate(TEST_DUE_DATE_API);
    assertThat(userTask.getOperatonDueDate()).isEqualTo(TEST_DUE_DATE_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testErrorCodeVariable(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    ErrorEventDefinition errorEventDefinition = startEvent.getChildElementsByType(ErrorEventDefinition.class).iterator().next();
    assertThat(errorEventDefinition.getAttributeValueNs(namespace, OPERATON_ATTRIBUTE_ERROR_CODE_VARIABLE)).isEqualTo("errorVariable");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testErrorMessageVariable(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    ErrorEventDefinition errorEventDefinition = startEvent.getChildElementsByType(ErrorEventDefinition.class).iterator().next();
    assertThat(errorEventDefinition.getAttributeValueNs(namespace, OPERATON_ATTRIBUTE_ERROR_MESSAGE_VARIABLE)).isEqualTo("errorMessageVariable");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testErrorMessage(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(error.getOperatonErrorMessage()).isEqualTo(TEST_STRING_XML);
    error.setOperatonErrorMessage(TEST_STRING_API);
    assertThat(error.getOperatonErrorMessage()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testExclusive(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.isOperatonExclusive()).isTrue();
    assertThat(userTask.isOperatonExclusive()).isFalse();
    userTask.setOperatonExclusive(true);
    assertThat(userTask.isOperatonExclusive()).isTrue();
    assertThat(parallelGateway.isOperatonExclusive()).isTrue();
    parallelGateway.setOperatonExclusive(false);
    assertThat(parallelGateway.isOperatonExclusive()).isFalse();

    assertThat(callActivity.isOperatonExclusive()).isFalse();
    callActivity.setOperatonExclusive(true);
    assertThat(callActivity.isOperatonExclusive()).isTrue();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testExpression(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(messageEventDefinition.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    serviceTask.setOperatonExpression(TEST_EXPRESSION_API);
    messageEventDefinition.setOperatonExpression(TEST_EXPRESSION_API);
    assertThat(serviceTask.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(messageEventDefinition.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFormHandlerClass(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_XML);
    assertThat(userTask.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_XML);
    startEvent.setOperatonFormHandlerClass(TEST_CLASS_API);
    userTask.setOperatonFormHandlerClass(TEST_CLASS_API);
    assertThat(startEvent.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_API);
    assertThat(userTask.getOperatonFormHandlerClass()).isEqualTo(TEST_CLASS_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFormKey(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.getOperatonFormKey()).isEqualTo(TEST_STRING_XML);
    assertThat(userTask.getOperatonFormKey()).isEqualTo(TEST_STRING_XML);
    startEvent.setOperatonFormKey(TEST_STRING_API);
    userTask.setOperatonFormKey(TEST_STRING_API);
    assertThat(startEvent.getOperatonFormKey()).isEqualTo(TEST_STRING_API);
    assertThat(userTask.getOperatonFormKey()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testInitiator(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(startEvent.getOperatonInitiator()).isEqualTo(TEST_STRING_XML);
    startEvent.setOperatonInitiator(TEST_STRING_API);
    assertThat(startEvent.getOperatonInitiator()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testPriority(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonPriority()).isEqualTo(TEST_PRIORITY_XML);
    userTask.setOperatonPriority(TEST_PRIORITY_API);
    assertThat(userTask.getOperatonPriority()).isEqualTo(TEST_PRIORITY_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testResultVariable(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonResultVariable()).isEqualTo(TEST_STRING_XML);
    assertThat(messageEventDefinition.getOperatonResultVariable()).isEqualTo(TEST_STRING_XML);
    serviceTask.setOperatonResultVariable(TEST_STRING_API);
    messageEventDefinition.setOperatonResultVariable(TEST_STRING_API);
    assertThat(serviceTask.getOperatonResultVariable()).isEqualTo(TEST_STRING_API);
    assertThat(messageEventDefinition.getOperatonResultVariable()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testType(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonType()).isEqualTo(TEST_TYPE_XML);
    assertThat(messageEventDefinition.getOperatonType()).isEqualTo(TEST_STRING_XML);
    serviceTask.setOperatonType(TEST_TYPE_API);
    messageEventDefinition.setOperatonType(TEST_STRING_API);
    assertThat(serviceTask.getOperatonType()).isEqualTo(TEST_TYPE_API);
    assertThat(messageEventDefinition.getOperatonType()).isEqualTo(TEST_STRING_API);

  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testTopic(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(serviceTask.getOperatonTopic()).isEqualTo(TEST_STRING_XML);
    assertThat(messageEventDefinition.getOperatonTopic()).isEqualTo(TEST_STRING_XML);
    serviceTask.setOperatonTopic(TEST_TYPE_API);
    messageEventDefinition.setOperatonTopic(TEST_STRING_API);
    assertThat(serviceTask.getOperatonTopic()).isEqualTo(TEST_TYPE_API);
    assertThat(messageEventDefinition.getOperatonTopic()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testVariableMappingClass(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonVariableMappingClass()).isEqualTo(TEST_CLASS_XML);
    callActivity.setOperatonVariableMappingClass(TEST_CLASS_API);
    assertThat(callActivity.getOperatonVariableMappingClass()).isEqualTo(TEST_CLASS_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testVariableMappingDelegateExpression(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(callActivity.getOperatonVariableMappingDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
    callActivity.setOperatonVariableMappingDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
    assertThat(callActivity.getOperatonVariableMappingDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testExecutionListenerExtension(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonExecutionListener processListener = process.getExtensionElements().getElementsQuery().filterByType(OperatonExecutionListener.class).singleResult();
    OperatonExecutionListener startEventListener = startEvent.getExtensionElements().getElementsQuery().filterByType(OperatonExecutionListener.class).singleResult();
    OperatonExecutionListener serviceTaskListener = serviceTask.getExtensionElements().getElementsQuery().filterByType(OperatonExecutionListener.class).singleResult();
    assertThat(processListener.getOperatonClass()).isEqualTo(TEST_CLASS_XML);
    assertThat(processListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
    assertThat(startEventListener.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(startEventListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
    assertThat(serviceTaskListener.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
    assertThat(serviceTaskListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_XML);
    processListener.setOperatonClass(TEST_CLASS_API);
    processListener.setOperatonEvent(TEST_EXECUTION_EVENT_API);
    startEventListener.setOperatonExpression(TEST_EXPRESSION_API);
    startEventListener.setOperatonEvent(TEST_EXECUTION_EVENT_API);
    serviceTaskListener.setOperatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
    serviceTaskListener.setOperatonEvent(TEST_EXECUTION_EVENT_API);
    assertThat(processListener.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(processListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
    assertThat(startEventListener.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(startEventListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
    assertThat(serviceTaskListener.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);
    assertThat(serviceTaskListener.getOperatonEvent()).isEqualTo(TEST_EXECUTION_EVENT_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonScriptExecutionListener(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonExecutionListener sequenceFlowListener = sequenceFlow.getExtensionElements().getElementsQuery().filterByType(OperatonExecutionListener.class).singleResult();

    OperatonScript script = sequenceFlowListener.getOperatonScript();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("groovy");
    assertThat(script.getOperatonResource()).isNull();
    assertThat(script.getTextContent()).isEqualTo("println 'Hello World'");

    OperatonScript newScript = this.modelInstance.newInstance(OperatonScript.class);
    newScript.setOperatonScriptFormat("groovy");
    newScript.setOperatonResource("test.groovy");
    sequenceFlowListener.setOperatonScript(newScript);

    script = sequenceFlowListener.getOperatonScript();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("groovy");
    assertThat(script.getOperatonResource()).isEqualTo("test.groovy");
    assertThat(script.getTextContent()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFailedJobRetryTimeCycleExtension(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonFailedJobRetryTimeCycle timeCycle = sendTask.getExtensionElements().getElementsQuery().filterByType(OperatonFailedJobRetryTimeCycle.class).singleResult();
    assertThat(timeCycle.getTextContent()).isEqualTo(TEST_STRING_XML);
    timeCycle.setTextContent(TEST_STRING_API);
    assertThat(timeCycle.getTextContent()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFieldExtension(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonField field = sendTask.getExtensionElements().getElementsQuery().filterByType(OperatonField.class).singleResult();
    assertThat(field.getOperatonName()).isEqualTo(TEST_STRING_XML);
    assertThat(field.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(field.getOperatonStringValue()).isEqualTo(TEST_STRING_XML);
    assertThat(field.getOperatonExpressionChild().getTextContent()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(field.getOperatonString().getTextContent()).isEqualTo(TEST_STRING_XML);
    field.setOperatonName(TEST_STRING_API);
    field.setOperatonExpression(TEST_EXPRESSION_API);
    field.setOperatonStringValue(TEST_STRING_API);
    field.getOperatonExpressionChild().setTextContent(TEST_EXPRESSION_API);
    field.getOperatonString().setTextContent(TEST_STRING_API);
    assertThat(field.getOperatonName()).isEqualTo(TEST_STRING_API);
    assertThat(field.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(field.getOperatonStringValue()).isEqualTo(TEST_STRING_API);
    assertThat(field.getOperatonExpressionChild().getTextContent()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(field.getOperatonString().getTextContent()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFormData(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonFormData formData = userTask.getExtensionElements().getElementsQuery().filterByType(OperatonFormData.class).singleResult();
    OperatonFormField formField = formData.getOperatonFormFields().iterator().next();
    assertThat(formField.getOperatonId()).isEqualTo(TEST_STRING_XML);
    assertThat(formField.getOperatonLabel()).isEqualTo(TEST_STRING_XML);
    assertThat(formField.getOperatonType()).isEqualTo(TEST_STRING_XML);
    assertThat(formField.getOperatonDatePattern()).isEqualTo(TEST_STRING_XML);
    assertThat(formField.getOperatonDefaultValue()).isEqualTo(TEST_STRING_XML);
    formField.setOperatonId(TEST_STRING_API);
    formField.setOperatonLabel(TEST_STRING_API);
    formField.setOperatonType(TEST_STRING_API);
    formField.setOperatonDatePattern(TEST_STRING_API);
    formField.setOperatonDefaultValue(TEST_STRING_API);
    assertThat(formField.getOperatonId()).isEqualTo(TEST_STRING_API);
    assertThat(formField.getOperatonLabel()).isEqualTo(TEST_STRING_API);
    assertThat(formField.getOperatonType()).isEqualTo(TEST_STRING_API);
    assertThat(formField.getOperatonDatePattern()).isEqualTo(TEST_STRING_API);
    assertThat(formField.getOperatonDefaultValue()).isEqualTo(TEST_STRING_API);

    OperatonProperty property = formField.getOperatonProperties().getOperatonProperties().iterator().next();
    assertThat(property.getOperatonId()).isEqualTo(TEST_STRING_XML);
    assertThat(property.getOperatonValue()).isEqualTo(TEST_STRING_XML);
    property.setOperatonId(TEST_STRING_API);
    property.setOperatonValue(TEST_STRING_API);
    assertThat(property.getOperatonId()).isEqualTo(TEST_STRING_API);
    assertThat(property.getOperatonValue()).isEqualTo(TEST_STRING_API);

    OperatonConstraint constraint = formField.getOperatonValidation().getOperatonConstraints().iterator().next();
    assertThat(constraint.getOperatonName()).isEqualTo(TEST_STRING_XML);
    assertThat(constraint.getOperatonConfig()).isEqualTo(TEST_STRING_XML);
    constraint.setOperatonName(TEST_STRING_API);
    constraint.setOperatonConfig(TEST_STRING_API);
    assertThat(constraint.getOperatonName()).isEqualTo(TEST_STRING_API);
    assertThat(constraint.getOperatonConfig()).isEqualTo(TEST_STRING_API);

    OperatonValue value = formField.getOperatonValues().iterator().next();
    assertThat(value.getOperatonId()).isEqualTo(TEST_STRING_XML);
    assertThat(value.getOperatonName()).isEqualTo(TEST_STRING_XML);
    value.setOperatonId(TEST_STRING_API);
    value.setOperatonName(TEST_STRING_API);
    assertThat(value.getOperatonId()).isEqualTo(TEST_STRING_API);
    assertThat(value.getOperatonName()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testFormProperty(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonFormProperty formProperty = startEvent.getExtensionElements().getElementsQuery().filterByType(OperatonFormProperty.class).singleResult();
    assertThat(formProperty.getOperatonId()).isEqualTo(TEST_STRING_XML);
    assertThat(formProperty.getOperatonName()).isEqualTo(TEST_STRING_XML);
    assertThat(formProperty.getOperatonType()).isEqualTo(TEST_STRING_XML);
    assertThat(formProperty.isOperatonRequired()).isFalse();
    assertThat(formProperty.isOperatonReadable()).isTrue();
    assertThat(formProperty.isOperatonWriteable()).isTrue();
    assertThat(formProperty.getOperatonVariable()).isEqualTo(TEST_STRING_XML);
    assertThat(formProperty.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(formProperty.getOperatonDatePattern()).isEqualTo(TEST_STRING_XML);
    assertThat(formProperty.getOperatonDefault()).isEqualTo(TEST_STRING_XML);
    formProperty.setOperatonId(TEST_STRING_API);
    formProperty.setOperatonName(TEST_STRING_API);
    formProperty.setOperatonType(TEST_STRING_API);
    formProperty.setOperatonRequired(true);
    formProperty.setOperatonReadable(false);
    formProperty.setOperatonWriteable(false);
    formProperty.setOperatonVariable(TEST_STRING_API);
    formProperty.setOperatonExpression(TEST_EXPRESSION_API);
    formProperty.setOperatonDatePattern(TEST_STRING_API);
    formProperty.setOperatonDefault(TEST_STRING_API);
    assertThat(formProperty.getOperatonId()).isEqualTo(TEST_STRING_API);
    assertThat(formProperty.getOperatonName()).isEqualTo(TEST_STRING_API);
    assertThat(formProperty.getOperatonType()).isEqualTo(TEST_STRING_API);
    assertThat(formProperty.isOperatonRequired()).isTrue();
    assertThat(formProperty.isOperatonReadable()).isFalse();
    assertThat(formProperty.isOperatonWriteable()).isFalse();
    assertThat(formProperty.getOperatonVariable()).isEqualTo(TEST_STRING_API);
    assertThat(formProperty.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(formProperty.getOperatonDatePattern()).isEqualTo(TEST_STRING_API);
    assertThat(formProperty.getOperatonDefault()).isEqualTo(TEST_STRING_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testInExtension(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonIn in = callActivity.getExtensionElements().getElementsQuery().filterByType(OperatonIn.class).singleResult();
    assertThat(in.getOperatonSource()).isEqualTo(TEST_STRING_XML);
    assertThat(in.getOperatonSourceExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(in.getOperatonVariables()).isEqualTo(TEST_STRING_XML);
    assertThat(in.getOperatonTarget()).isEqualTo(TEST_STRING_XML);
    assertThat(in.getOperatonBusinessKey()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(in.getOperatonLocal()).isTrue();
    in.setOperatonSource(TEST_STRING_API);
    in.setOperatonSourceExpression(TEST_EXPRESSION_API);
    in.setOperatonVariables(TEST_STRING_API);
    in.setOperatonTarget(TEST_STRING_API);
    in.setOperatonBusinessKey(TEST_EXPRESSION_API);
    in.setOperatonLocal(false);
    assertThat(in.getOperatonSource()).isEqualTo(TEST_STRING_API);
    assertThat(in.getOperatonSourceExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(in.getOperatonVariables()).isEqualTo(TEST_STRING_API);
    assertThat(in.getOperatonTarget()).isEqualTo(TEST_STRING_API);
    assertThat(in.getOperatonBusinessKey()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(in.getOperatonLocal()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOutExtension(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonOut out = callActivity.getExtensionElements().getElementsQuery().filterByType(OperatonOut.class).singleResult();
    assertThat(out.getOperatonSource()).isEqualTo(TEST_STRING_XML);
    assertThat(out.getOperatonSourceExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(out.getOperatonVariables()).isEqualTo(TEST_STRING_XML);
    assertThat(out.getOperatonTarget()).isEqualTo(TEST_STRING_XML);
    assertThat(out.getOperatonLocal()).isTrue();
    out.setOperatonSource(TEST_STRING_API);
    out.setOperatonSourceExpression(TEST_EXPRESSION_API);
    out.setOperatonVariables(TEST_STRING_API);
    out.setOperatonTarget(TEST_STRING_API);
    out.setOperatonLocal(false);
    assertThat(out.getOperatonSource()).isEqualTo(TEST_STRING_API);
    assertThat(out.getOperatonSourceExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(out.getOperatonVariables()).isEqualTo(TEST_STRING_API);
    assertThat(out.getOperatonTarget()).isEqualTo(TEST_STRING_API);
    assertThat(out.getOperatonLocal()).isFalse();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testPotentialStarter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonPotentialStarter potentialStarter = startEvent.getExtensionElements().getElementsQuery().filterByType(OperatonPotentialStarter.class).singleResult();
    Expression expression = potentialStarter.getResourceAssignmentExpression().getExpression();
    assertThat(expression.getTextContent()).isEqualTo(TEST_GROUPS_XML);
    expression.setTextContent(TEST_GROUPS_API);
    assertThat(expression.getTextContent()).isEqualTo(TEST_GROUPS_API);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testTaskListener(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonTaskListener taskListener = userTask.getExtensionElements().getElementsQuery().filterByType(OperatonTaskListener.class).list().get(0);
    assertThat(taskListener.getOperatonEvent()).isEqualTo(TEST_TASK_EVENT_XML);
    assertThat(taskListener.getOperatonClass()).isEqualTo(TEST_CLASS_XML);
    assertThat(taskListener.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_XML);
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_XML);
    taskListener.setOperatonEvent(TEST_TASK_EVENT_API);
    taskListener.setOperatonClass(TEST_CLASS_API);
    taskListener.setOperatonExpression(TEST_EXPRESSION_API);
    taskListener.setOperatonDelegateExpression(TEST_DELEGATE_EXPRESSION_API);
    assertThat(taskListener.getOperatonEvent()).isEqualTo(TEST_TASK_EVENT_API);
    assertThat(taskListener.getOperatonClass()).isEqualTo(TEST_CLASS_API);
    assertThat(taskListener.getOperatonExpression()).isEqualTo(TEST_EXPRESSION_API);
    assertThat(taskListener.getOperatonDelegateExpression()).isEqualTo(TEST_DELEGATE_EXPRESSION_API);

    OperatonField field = taskListener.getOperatonFields().iterator().next();
    assertThat(field.getOperatonName()).isEqualTo(TEST_STRING_XML);
    assertThat(field.getOperatonString().getTextContent()).isEqualTo(TEST_STRING_XML);

    Collection<TimerEventDefinition> timeouts = taskListener.getTimeouts();
    assertThat(timeouts).hasSize(1);

    TimerEventDefinition timeout = timeouts.iterator().next();
    assertThat(timeout.getTimeCycle()).isNull();
    assertThat(timeout.getTimeDate()).isNull();
    assertThat(timeout.getTimeDuration()).isNotNull();
    assertThat(timeout.getTimeDuration().getRawTextContent()).isEqualTo("PT1H");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonScriptTaskListener(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonTaskListener taskListener = userTask.getExtensionElements().getElementsQuery().filterByType(OperatonTaskListener.class).list().get(1);

    OperatonScript script = taskListener.getOperatonScript();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("groovy");
    assertThat(script.getOperatonResource()).isEqualTo("test.groovy");
    assertThat(script.getTextContent()).isEmpty();

    OperatonScript newScript = this.modelInstance.newInstance(OperatonScript.class);
    newScript.setOperatonScriptFormat("groovy");
    newScript.setTextContent("println 'Hello World'");
    taskListener.setOperatonScript(newScript);

    script = taskListener.getOperatonScript();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("groovy");
    assertThat(script.getOperatonResource()).isNull();
    assertThat(script.getTextContent()).isEqualTo("println 'Hello World'");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonModelerProperties(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonProperties operatonProperties = endEvent.getExtensionElements().getElementsQuery().filterByType(OperatonProperties.class).singleResult();
    assertThat(operatonProperties).isNotNull();
    assertThat(operatonProperties.getOperatonProperties()).hasSize(2);

    for (OperatonProperty operatonProperty : operatonProperties.getOperatonProperties()) {
      assertThat(operatonProperty.getOperatonId()).isNull();
      assertThat(operatonProperty.getOperatonName()).startsWith("name");
      assertThat(operatonProperty.getOperatonValue()).startsWith("value");
    }
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testGetNonExistingOperatonCandidateUsers(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    userTask.removeAttributeNs(namespace, "candidateUsers");
    assertThat(userTask.getOperatonCandidateUsers()).isNull();
    assertThat(userTask.getOperatonCandidateUsersList()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testSetNullOperatonCandidateUsers(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateUsers()).isNotEmpty();
    assertThat(userTask.getOperatonCandidateUsersList()).isNotEmpty();
    userTask.setOperatonCandidateUsers(null);
    assertThat(userTask.getOperatonCandidateUsers()).isNull();
    assertThat(userTask.getOperatonCandidateUsersList()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testEmptyOperatonCandidateUsers(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateUsers()).isNotEmpty();
    assertThat(userTask.getOperatonCandidateUsersList()).isNotEmpty();
    userTask.setOperatonCandidateUsers("");
    assertThat(userTask.getOperatonCandidateUsers()).isNull();
    assertThat(userTask.getOperatonCandidateUsersList()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testSetNullOperatonCandidateUsersList(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateUsers()).isNotEmpty();
    assertThat(userTask.getOperatonCandidateUsersList()).isNotEmpty();
    userTask.setOperatonCandidateUsersList(null);
    assertThat(userTask.getOperatonCandidateUsers()).isNull();
    assertThat(userTask.getOperatonCandidateUsersList()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testEmptyOperatonCandidateUsersList(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(userTask.getOperatonCandidateUsers()).isNotEmpty();
    assertThat(userTask.getOperatonCandidateUsersList()).isNotEmpty();
    userTask.setOperatonCandidateUsersList(Collections.<String>emptyList());
    assertThat(userTask.getOperatonCandidateUsers()).isNull();
    assertThat(userTask.getOperatonCandidateUsersList()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testScriptResource(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    assertThat(scriptTask.getScriptFormat()).isEqualTo("groovy");
    assertThat(scriptTask.getOperatonResource()).isEqualTo("test.groovy");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonConnector(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonConnector operatonConnector = serviceTask.getExtensionElements().getElementsQuery().filterByType(OperatonConnector.class).singleResult();
    assertThat(operatonConnector).isNotNull();

    OperatonConnectorId operatonConnectorId = operatonConnector.getOperatonConnectorId();
    assertThat(operatonConnectorId).isNotNull();
    assertThat(operatonConnectorId.getTextContent()).isEqualTo("soap-http-connector");

    OperatonInputOutput operatonInputOutput = operatonConnector.getOperatonInputOutput();

    Collection<OperatonInputParameter> inputParameters = operatonInputOutput.getOperatonInputParameters();
    assertThat(inputParameters).hasSize(1);

    OperatonInputParameter inputParameter = inputParameters.iterator().next();
    assertThat(inputParameter.getOperatonName()).isEqualTo("endpointUrl");
    assertThat(inputParameter.getTextContent()).isEqualTo("http://example.com/webservice");

    Collection<OperatonOutputParameter> outputParameters = operatonInputOutput.getOperatonOutputParameters();
    assertThat(outputParameters).hasSize(1);

    OperatonOutputParameter outputParameter = outputParameters.iterator().next();
    assertThat(outputParameter.getOperatonName()).isEqualTo("result");
    assertThat(outputParameter.getTextContent()).isEqualTo("output");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonInputOutput(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputOutput operatonInputOutput = serviceTask.getExtensionElements().getElementsQuery().filterByType(OperatonInputOutput.class).singleResult();
    assertThat(operatonInputOutput).isNotNull();
    assertThat(operatonInputOutput.getOperatonInputParameters()).hasSize(6);
    assertThat(operatonInputOutput.getOperatonOutputParameters()).hasSize(1);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    // find existing
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeConstant");

    // modify existing
    inputParameter.setOperatonName("hello");
    inputParameter.setTextContent("world");
    inputParameter = findInputParameterByName(serviceTask, "hello");
    assertThat(inputParameter.getTextContent()).isEqualTo("world");

    // add new one
    inputParameter = this.modelInstance.newInstance(OperatonInputParameter.class);
    inputParameter.setOperatonName("abc");
    inputParameter.setTextContent("def");
    serviceTask.getExtensionElements().getElementsQuery().filterByType(OperatonInputOutput.class).singleResult()
      .addChildElement(inputParameter);

    // search for new one
    inputParameter = findInputParameterByName(serviceTask, "abc");
    assertThat(inputParameter.getOperatonName()).isEqualTo("abc");
    assertThat(inputParameter.getTextContent()).isEqualTo("def");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonNullInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeNull");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeNull");
    assertThat(inputParameter.getTextContent()).isEmpty();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonConstantInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeConstant");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeConstant");
    assertThat(inputParameter.getTextContent()).isEqualTo("foo");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonExpressionInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeExpression");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeExpression");
    assertThat(inputParameter.getTextContent()).isEqualTo("${1 + 1}");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonListInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeList");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeList");
    assertThat(inputParameter.getTextContent()).isNotEmpty();
    assertThat(inputParameter.getUniqueChildElementByNameNs(OPERATON_NS, "list")).isNotNull();

    OperatonList list = inputParameter.getValue();
    assertThat(list.getValues()).hasSize(3);
    for (BpmnModelElementInstance values : list.getValues()) {
      assertThat(values.getTextContent()).isIn("a", "b", "c");
    }

    list = this.modelInstance.newInstance(OperatonList.class);
    for (int i = 0; i < 4; i++) {
      OperatonValue value = this.modelInstance.newInstance(OperatonValue.class);
      value.setTextContent("test");
      list.getValues().add(value);
    }
    Collection<OperatonValue> testValues = Arrays.asList(this.modelInstance.newInstance(OperatonValue.class), this.modelInstance.newInstance(OperatonValue.class));
    list.getValues().addAll(testValues);
    inputParameter.setValue(list);

    list = inputParameter.getValue();
    assertThat(list.getValues()).hasSize(6);
    list.getValues().removeAll(testValues);
    ArrayList<BpmnModelElementInstance> operatonValues = new ArrayList<>(list.getValues());
    assertThat(operatonValues).hasSize(4);
    for (BpmnModelElementInstance value : operatonValues) {
      assertThat(value.getTextContent()).isEqualTo("test");
    }

    list.getValues().remove(operatonValues.get(1));
    assertThat(list.getValues()).hasSize(3);

    list.getValues().removeAll(Arrays.asList(operatonValues.get(0), operatonValues.get(3)));
    assertThat(list.getValues()).hasSize(1);

    list.getValues().clear();
    assertThat(list.getValues()).isEmpty();

    // test standard list interactions
    Collection<BpmnModelElementInstance> elements = list.getValues();

    OperatonValue value = this.modelInstance.newInstance(OperatonValue.class);
    elements.add(value);

    List<OperatonValue> newValues = new ArrayList<>();
    newValues.add(this.modelInstance.newInstance(OperatonValue.class));
    newValues.add(this.modelInstance.newInstance(OperatonValue.class));
    elements.addAll(newValues);
    assertThat(elements).hasSize(3);

    assertThat(elements).doesNotContain(this.modelInstance.newInstance(OperatonValue.class));
    assertThat(elements.containsAll(Arrays.asList(this.modelInstance.newInstance(OperatonValue.class)))).isFalse();

    assertThat(elements.remove(this.modelInstance.newInstance(OperatonValue.class))).isFalse();
    assertThat(elements).hasSize(3);

    assertThat(elements.remove(value)).isTrue();
    assertThat(elements).hasSize(2);

    assertThat(elements.removeAll(newValues)).isTrue();
    assertThat(elements).isEmpty();

    elements.add(this.modelInstance.newInstance(OperatonValue.class));
    elements.clear();
    assertThat(elements).isEmpty();

    inputParameter.removeValue();
    assertThat((Object) inputParameter.getValue()).isNull();

  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonMapInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeMap");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeMap");
    assertThat(inputParameter.getTextContent()).isNotEmpty();
    assertThat(inputParameter.getUniqueChildElementByNameNs(OPERATON_NS, "map")).isNotNull();

    OperatonMap map = inputParameter.getValue();
    assertThat(map.getOperatonEntries()).hasSize(2);
    for (OperatonEntry entry : map.getOperatonEntries()) {
      if (entry.getOperatonKey().equals("foo")) {
        assertThat(entry.getTextContent()).isEqualTo("bar");
      }
      else {
        assertThat(entry.getOperatonKey()).isEqualTo("hello");
        assertThat(entry.getTextContent()).isEqualTo("world");
      }
    }

    map = this.modelInstance.newInstance(OperatonMap.class);
    OperatonEntry entry = this.modelInstance.newInstance(OperatonEntry.class);
    entry.setOperatonKey("test");
    entry.setTextContent("value");
    map.getOperatonEntries().add(entry);

    inputParameter.setValue(map);
    map = inputParameter.getValue();
    assertThat(map.getOperatonEntries()).hasSize(1);
    entry = map.getOperatonEntries().iterator().next();
    assertThat(entry.getOperatonKey()).isEqualTo("test");
    assertThat(entry.getTextContent()).isEqualTo("value");

    Collection<OperatonEntry> entries = map.getOperatonEntries();
    entries.add(this.modelInstance.newInstance(OperatonEntry.class));
    assertThat(entries).hasSize(2);

    inputParameter.removeValue();
    assertThat((Object) inputParameter.getValue()).isNull();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonScriptInputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonInputParameter inputParameter = findInputParameterByName(serviceTask, "shouldBeScript");
    assertThat(inputParameter.getOperatonName()).isEqualTo("shouldBeScript");
    assertThat(inputParameter.getTextContent()).isNotEmpty();
    assertThat(inputParameter.getUniqueChildElementByNameNs(OPERATON_NS, "script")).isNotNull();
    assertThat(inputParameter.getUniqueChildElementByType(OperatonScript.class)).isNotNull();

    OperatonScript script = inputParameter.getValue();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("groovy");
    assertThat(script.getOperatonResource()).isNull();
    assertThat(script.getTextContent()).isEqualTo("1 + 1");

    script = this.modelInstance.newInstance(OperatonScript.class);
    script.setOperatonScriptFormat("python");
    script.setOperatonResource("script.py");

    inputParameter.setValue(script);

    script = inputParameter.getValue();
    assertThat(script.getOperatonScriptFormat()).isEqualTo("python");
    assertThat(script.getOperatonResource()).isEqualTo("script.py");
    assertThat(script.getTextContent()).isEmpty();

    inputParameter.removeValue();
    assertThat((Object) inputParameter.getValue()).isNull();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonNestedOutputParameter(String namespace, BpmnModelInstance modelInstance) {
    initOperatonExtensionsTest(namespace, modelInstance);
    OperatonOutputParameter operatonOutputParameter = serviceTask.getExtensionElements().getElementsQuery().filterByType(OperatonInputOutput.class).singleResult().getOperatonOutputParameters().iterator().next();

    assertThat(operatonOutputParameter).isNotNull();
    assertThat(operatonOutputParameter.getOperatonName()).isEqualTo("nested");
    OperatonList list = operatonOutputParameter.getValue();
    assertThat(list).isNotNull();
    assertThat(list.getValues()).hasSize(2);
    Iterator<BpmnModelElementInstance> iterator = list.getValues().iterator();

    // nested list
    OperatonList nestedList = (OperatonList) iterator.next().getUniqueChildElementByType(OperatonList.class);
    assertThat(nestedList).isNotNull();
    assertThat(nestedList.getValues()).hasSize(2);
    for (BpmnModelElementInstance value : nestedList.getValues()) {
      assertThat(value.getTextContent()).isEqualTo("list");
    }

    // nested map
    OperatonMap nestedMap = (OperatonMap) iterator.next().getUniqueChildElementByType(OperatonMap.class);
    assertThat(nestedMap).isNotNull();
    assertThat(nestedMap.getOperatonEntries()).hasSize(2);
    Iterator<OperatonEntry> mapIterator = nestedMap.getOperatonEntries().iterator();

    // nested list in nested map
    OperatonEntry nestedListEntry = mapIterator.next();
    assertThat(nestedListEntry).isNotNull();
    assertThat(nestedListEntry.getOperatonKey()).isEqualTo("list");
    OperatonList nestedNestedList = nestedListEntry.getValue();
    for (BpmnModelElementInstance value : nestedNestedList.getValues()) {
      assertThat(value.getTextContent()).isEqualTo("map");
    }

    // nested map in nested map
    OperatonEntry nestedMapEntry = mapIterator.next();
    assertThat(nestedMapEntry).isNotNull();
    assertThat(nestedMapEntry.getOperatonKey()).isEqualTo("map");
    OperatonMap nestedNestedMap = nestedMapEntry.getValue();
    OperatonEntry entry = nestedNestedMap.getOperatonEntries().iterator().next();
    assertThat(entry.getOperatonKey()).isEqualTo("so");
    assertThat(entry.getTextContent()).isEqualTo("nested");
  }

  protected OperatonInputParameter findInputParameterByName(BaseElement baseElement, String name) {
    Collection<OperatonInputParameter> operatonInputParameters = baseElement.getExtensionElements().getElementsQuery()
      .filterByType(OperatonInputOutput.class).singleResult().getOperatonInputParameters();
    for (OperatonInputParameter operatonInputParameter : operatonInputParameters) {
      if (operatonInputParameter.getOperatonName().equals(name)) {
        return operatonInputParameter;
      }
    }
    throw new BpmnModelException("Unable to find operaton:inputParameter with name '" + name + "' for element with id '" + baseElement.getId() + "'");
  }

  @AfterEach
  void validateModel() {
    Bpmn.validateModel(modelInstance);
  }
}
