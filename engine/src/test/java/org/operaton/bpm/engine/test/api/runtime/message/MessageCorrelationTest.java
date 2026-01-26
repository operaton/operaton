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
package org.operaton.bpm.engine.test.api.runtime.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.MessageCorrelationResult;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultType;
import org.operaton.bpm.engine.runtime.MessageCorrelationResultWithVariables;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.variables.FailingJavaSerializable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 */
class MessageCorrelationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> configuration.setJavaSerializationFormatEnabled(true))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  RepositoryService repositoryService;

  @Deployment
  @Test
  void testCatchingMessageEventCorrelation() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    variables = new HashMap<>();
    variables.put("aKey", "anotherValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");
    Map<String, Object> messagePayload = new HashMap<>();
    messagePayload.put("aNewKey", "aNewVariable");

    runtimeService.correlateMessage(messageName, correlationKeys, messagePayload);

    long uncorrelatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "anotherValue").messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertThat(uncorrelatedExecutions).isOne();

    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aKey", "aValue").processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // this time: use the builder ////////////////

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    // use the fluent builder
    runtimeService.createMessageCorrelation(messageName)
      .processInstanceVariableEquals("aKey", "aValue")
      .setVariable("aNewKey", "aNewVariable")
      .correlate();

    uncorrelatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "anotherValue").messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertThat(uncorrelatedExecutions).isOne();

    // the execution that has been correlated should have advanced
    correlatedExecutions = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aKey", "aValue").processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testOneMatchingProcessInstanceUsingFluentCorrelateAll() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    variables = new HashMap<>();
    variables.put("aKey", "anotherValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    String messageName = "newInvoiceMessage";

    // use the fluent builder: correlate to first started process instance
    runtimeService.createMessageCorrelation(messageName)
      .processInstanceVariableEquals("aKey", "aValue")
      .setVariable("aNewKey", "aNewVariable")
      .correlateAll();

    // there exists an uncorrelated executions (the second process instance)
    long uncorrelatedExecutions = runtimeService
        .createExecutionQuery()
        .processVariableValueEquals("aKey", "anotherValue")
        .messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertThat(uncorrelatedExecutions).isOne();

    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .activityId("task")
        .processVariableValueEquals("aKey", "aValue")
        .processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertThat(correlatedExecutions).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testTwoMatchingProcessInstancesCorrelation() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(messageName)
        .processInstanceVariableEquals("aKey", "aValue");

    // when/then
    assertThatThrownBy(() -> runtimeService.correlateMessage(messageName, correlationKeys))
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("2 executions match the correlation keys");

    // fluent builder fails as well
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("2 executions match the correlation keys");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testTwoMatchingProcessInstancesUsingFluentCorrelateAll() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");

    // fluent builder multiple should not fail
    runtimeService.createMessageCorrelation(messageName)
      .processInstanceVariableEquals("aKey", "aValue")
      .setVariable("aNewKey", "aNewVariable")
      .correlateAll();

    long uncorrelatedExecutions = runtimeService
        .createExecutionQuery()
        .messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertThat(uncorrelatedExecutions).isZero();

    // the executions that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .activityId("task")
        .processVariableValueEquals("aKey", "aValue")
        .processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertThat(correlatedExecutions).isEqualTo(2);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationByBusinessKey() {
    String businessKey = "aBusinessKey";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", businessKey);
    runtimeService.correlateMessage("newInvoiceMessage", businessKey);

    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery().activityId("task").count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // use fluent builder //////////////////////

    processInstance = runtimeService.startProcessInstanceByKey("process", businessKey);
    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .correlate();

    // the execution that has been correlated should have advanced
    correlatedExecutions = runtimeService.createExecutionQuery().activityId("task").count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationByBusinessKeyUsingFluentCorrelateAll() {
    String businessKey = "aBusinessKey";
    runtimeService.startProcessInstanceByKey("process", businessKey);
    runtimeService.startProcessInstanceByKey("process", businessKey);

    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .correlateAll();

    // the executions that has been correlated should be in the task
    long correlatedExecutions = runtimeService.createExecutionQuery().activityId("task").count();
    assertThat(correlatedExecutions).isEqualTo(2);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testMessageCorrelateAllResultListWithResultTypeExecution() {
    //given
    ProcessInstance procInstance1 = runtimeService.startProcessInstanceByKey("process");
    ProcessInstance procInstance2 = runtimeService.startProcessInstanceByKey("process");

    //when correlated all with result
    List<MessageCorrelationResult> resultList = runtimeService.createMessageCorrelation("newInvoiceMessage")
                                                              .correlateAllWithResult();

    assertThat(resultList).hasSize(2);
    //then result should contain executions on which messages was correlated
    for (MessageCorrelationResult result : resultList) {
      assertThat(result).isNotNull();
      assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
      assertThat(procInstance1.getId().equalsIgnoreCase(result.getExecution().getProcessInstanceId())
          || procInstance2.getId().equalsIgnoreCase(result.getExecution().getProcessInstanceId())).isTrue();
      ExecutionEntity entity = (ExecutionEntity) result.getExecution();
      assertThat(entity.getActivityId()).isEqualTo("messageCatch");
    }
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageCorrelateAllResultListWithResultTypeProcessDefinition() {
    //when correlated all with result
    List<MessageCorrelationResult> resultList = runtimeService.createMessageCorrelation("newInvoiceMessage")
                                                              .correlateAllWithResult();

    assertThat(resultList).hasSize(1);
    //then result should contain process definitions and start event activity ids on which messages was correlated
    for (MessageCorrelationResult result : resultList) {
      checkProcessDefinitionMessageCorrelationResult(result, "messageStartEvent");
    }
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationByBusinessKeyWithVariables() {
    String businessKey = "aBusinessKey";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", businessKey);

    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.correlateMessage("newInvoiceMessage", businessKey, variables);

    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "aValue").count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // use fluent builder /////////////////////////

    processInstance = runtimeService.startProcessInstanceByKey("process", businessKey);

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .setVariable("aKey", "aValue")
      .correlate();

    // the execution that has been correlated should have advanced
    correlatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "aValue").count();
    assertThat(correlatedExecutions).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationByBusinessKeyWithVariablesUsingFluentCorrelateAll() {
    String businessKey = "aBusinessKey";

    runtimeService.startProcessInstanceByKey("process", businessKey);
    runtimeService.startProcessInstanceByKey("process", businessKey);

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .setVariable("aKey", "aValue")
      .correlateAll();

    // the executions that has been correlated should have advanced
    long correlatedExecutions = runtimeService
        .createExecutionQuery()
        .processVariableValueEquals("aKey", "aValue")
        .count();
    assertThat(correlatedExecutions).isEqualTo(2);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationSetSerializedVariableValue() throws Exception {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

    // but it can be set as a variable:
    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .setVariable("var",
          Variables
            .serializedObjectValue(serializedObject)
            .objectTypeName(FailingJavaSerializable.class.getName())
            .serializationDataFormat(SerializationDataFormats.JAVA)
            .create())
      .correlate();

    // then
    ObjectValue variableTyped = runtimeService.getVariableTyped(processInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testExecutionCorrelationSetSerializedVariableValues() throws Exception {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

    // but it can be set as a variable:
    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .setVariables(
          Variables.createVariables().putValueTyped("var",
            Variables
              .serializedObjectValue(serializedObject)
              .objectTypeName(FailingJavaSerializable.class.getName())
              .serializationDataFormat(SerializationDataFormats.JAVA)
              .create()))
      .correlate();

    // then
    ObjectValue variableTyped = runtimeService.getVariableTyped(processInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment
  @Test
  void testMessageStartEventCorrelation() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");

    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<>(), variables);

    long instances = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue").count();
    assertThat(instances).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationUsingFluentCorrelateStartMessage() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .correlateStartMessage();

    long instances = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue").count();
    assertThat(instances).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationUsingFluentCorrelateSingle() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .correlate();

    long instances = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue").count();
    assertThat(instances).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationUsingFluentCorrelateAll() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .correlateAll();

    long instances = runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue")
        .count();
    assertThat(instances).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml"})
  @Test
  void testMessageStartEventCorrelationWithBusinessKey() {
    final String businessKey = "aBusinessKey";

    runtimeService.correlateMessage("newInvoiceMessage", businessKey);

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo(businessKey);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml"})
  @Test
  void testMessageStartEventCorrelationWithBusinessKeyUsingFluentCorrelateStartMessage() {
    final String businessKey = "aBusinessKey";

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .correlateStartMessage();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo(businessKey);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml"})
  @Test
  void testMessageStartEventCorrelationWithBusinessKeyUsingFluentCorrelateSingle() {
    final String businessKey = "aBusinessKey";

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .correlate();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo(businessKey);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml"})
  @Test
  void testMessageStartEventCorrelationWithBusinessKeyUsingFluentCorrelateAll() {
    final String businessKey = "aBusinessKey";

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceBusinessKey(businessKey)
      .correlateAll();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo(businessKey);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationSetSerializedVariableValue() throws Exception {

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

    // but it can be set as a variable:
    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .setVariable("var",
          Variables
            .serializedObjectValue(serializedObject)
            .objectTypeName(FailingJavaSerializable.class.getName())
            .serializationDataFormat(SerializationDataFormats.JAVA)
            .create())
      .correlate();

    // then
    ProcessInstance startedInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(startedInstance).isNotNull();

    ObjectValue variableTyped = runtimeService.getVariableTyped(startedInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationSetSerializedVariableValues() throws Exception {

    // when
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());

    // then it is not possible to deserialize the object
    try {
      new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
    } catch (RuntimeException e) {
      testRule.assertTextPresent("Exception while deserializing object.", e.getMessage());
    }

    // but it can be set as a variable:
    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .setVariables(
          Variables.createVariables().putValueTyped("var",
            Variables
              .serializedObjectValue(serializedObject)
              .objectTypeName(FailingJavaSerializable.class.getName())
              .serializationDataFormat(SerializationDataFormats.JAVA)
              .create()))
      .correlate();

    // then
    ProcessInstance startedInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(startedInstance).isNotNull();

    ObjectValue variableTyped = runtimeService.getVariableTyped(startedInstance.getId(), "var", false);
    assertThat(variableTyped).isNotNull();
    assertThat(variableTyped.isDeserialized()).isFalse();
    assertThat(variableTyped.getValueSerialized()).isEqualTo(serializedObject);
    assertThat(variableTyped.getObjectTypeName()).isEqualTo(FailingJavaSerializable.class.getName());
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(SerializationDataFormats.JAVA.getName());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationWithVariablesUsingFluentCorrelateStartMessage() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariables(Variables.createVariables()
          .putValue("var1", "a")
          .putValue("var2", "b"))
      .correlateStartMessage();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("var1", "a")
        .variableValueEquals("var2", "b");
    assertThat(query.count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationWithVariablesUsingFluentCorrelateSingleMessage() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariables(Variables.createVariables()
          .putValue("var1", "a")
          .putValue("var2", "b"))
      .correlate();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("var1", "a")
        .variableValueEquals("var2", "b");
    assertThat(query.count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testMessageStartEventCorrelationWithVariablesUsingFluentCorrelateAll() {

    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariables(Variables.createVariables()
          .putValue("var1", "a")
          .putValue("var2", "b"))
      .correlateAll();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("var1", "a")
        .variableValueEquals("var2", "b");
    assertThat(query.count()).isOne();
  }

  /**
   * this test assures the right start event is selected
   */
  @Deployment
  @Test
  void testMultipleMessageStartEventsCorrelation() {

    runtimeService.correlateMessage("someMessage");
    // verify the right start event was selected:
    Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task2").singleResult()).isNull();
    taskService.complete(task.getId());

    runtimeService.correlateMessage("someOtherMessage");
    // verify the right start event was selected:
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task1").singleResult()).isNull();
    taskService.complete(task.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMultipleMessageStartEventsCorrelation.bpmn20.xml"})
  @Test
  void testMultipleMessageStartEventsCorrelationUsingFluentCorrelateStartMessage() {

    runtimeService.createMessageCorrelation("someMessage").correlateStartMessage();
    // verify the right start event was selected:
    Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task2").singleResult()).isNull();
    taskService.complete(task.getId());

    runtimeService.createMessageCorrelation("someOtherMessage").correlateStartMessage();
    // verify the right start event was selected:
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task1").singleResult()).isNull();
    taskService.complete(task.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMultipleMessageStartEventsCorrelation.bpmn20.xml"})
  @Test
  void testMultipleMessageStartEventsCorrelationUsingFluentCorrelateSingle() {

    runtimeService.createMessageCorrelation("someMessage").correlate();
    // verify the right start event was selected:
    Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task2").singleResult()).isNull();
    taskService.complete(task.getId());

    runtimeService.createMessageCorrelation("someOtherMessage").correlate();
    // verify the right start event was selected:
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task1").singleResult()).isNull();
    taskService.complete(task.getId());
  }

  /**
   * this test assures the right start event is selected
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMultipleMessageStartEventsCorrelation.bpmn20.xml"})
  @Test
  void testMultipleMessageStartEventsCorrelationUsingFluentCorrelateAll() {

    runtimeService.createMessageCorrelation("someMessage").correlateAll();
    // verify the right start event was selected:
    Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task2").singleResult()).isNull();
    taskService.complete(task.getId());

    runtimeService.createMessageCorrelation("someOtherMessage").correlateAll();
    // verify the right start event was selected:
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertThat(task).isNotNull();
    assertThat(taskService.createTaskQuery().taskDefinitionKey("task1").singleResult()).isNull();
    taskService.complete(task.getId());
  }

  @Deployment
  @Test
  void testMatchingStartEventAndExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").singleResult()).isNotNull();
    // correlate message -> this will trigger the execution
    runtimeService.correlateMessage("newInvoiceMessage");
    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").singleResult()).isNull();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // fluent builder //////////////////////

    processInstance = runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").singleResult()).isNotNull();
    // correlate message -> this will trigger the execution
    runtimeService.createMessageCorrelation("newInvoiceMessage").correlate();
    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").singleResult()).isNull();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMatchingStartEventAndExecution.bpmn20.xml"})
  @Test
  void testMessageCorrelationResultWithResultTypeProcessDefinition() {
    //given
    String msgName = "newInvoiceMessage";

    //when
    //correlate message with result
    MessageCorrelationResult result = runtimeService.createMessageCorrelation(msgName).correlateWithResult();

    //then
    //message correlation result contains information from receiver
    checkProcessDefinitionMessageCorrelationResult(result, "process");
  }

  protected void checkProcessDefinitionMessageCorrelationResult(MessageCorrelationResult result, String processDefinitionId) {
    assertThat(result).isNotNull();
    assertThat(result.getProcessInstance().getId()).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.ProcessDefinition);
    assertThat(result.getProcessInstance().getProcessDefinitionId()).contains(processDefinitionId);
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMatchingStartEventAndExecution.bpmn20.xml"})
  @Test
  void testMessageCorrelationResultWithResultTypeExecution() {
    //given
    String msgName = "newInvoiceMessage";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName(msgName).singleResult()).isNotNull();

    //when
    //correlate message with result
    MessageCorrelationResult result = runtimeService.createMessageCorrelation(msgName).correlateWithResult();

    //then
    //message correlation result contains information from receiver
    checkExecutionMessageCorrelationResult(result, processInstance, "messageCatch");
  }

  protected void checkExecutionMessageCorrelationResult(MessageCorrelationResult result, ProcessInstance processInstance, String activityId) {
    assertThat(result).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
    assertThat(result.getExecution().getProcessInstanceId()).isEqualTo(processInstance.getId());
    ExecutionEntity entity = (ExecutionEntity) result.getExecution();
    assertThat(entity.getActivityId()).isEqualTo(activityId);
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMatchingStartEventAndExecution.bpmn20.xml"})
  @Test
  void testMatchingStartEventAndExecutionUsingFluentCorrelateAll() {
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").count()).isEqualTo(2);
    // correlate message -> this will trigger the executions AND start a new process instance
    runtimeService.createMessageCorrelation("newInvoiceMessage").correlateAll();
    assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoiceMessage").singleResult()).isNotNull();

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMatchingStartEventAndExecution.bpmn20.xml"})
  @Test
  void testMatchingStartEventAndExecutionCorrelateAllWithResult() {
    //given
    ProcessInstance procInstance1 = runtimeService.startProcessInstanceByKey("process");
    ProcessInstance procInstance2 = runtimeService.startProcessInstanceByKey("process");

    //when correlated all with result
    List<MessageCorrelationResult> resultList = runtimeService.createMessageCorrelation("newInvoiceMessage")
            .correlateAllWithResult();

    //then result should contain three entries
    //two of type execution und one of type process definition
    assertThat(resultList).hasSize(3);
    int executionResultCount = 0;
    int procDefResultCount = 0;
    for (MessageCorrelationResult result : resultList) {
      if (result.getResultType().equals(MessageCorrelationResultType.Execution)) {
        assertThat(result).isNotNull();
        assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
        assertThat(procInstance1.getId().equalsIgnoreCase(result.getExecution().getProcessInstanceId())
            || procInstance2.getId().equalsIgnoreCase(result.getExecution().getProcessInstanceId())).isTrue();
        ExecutionEntity entity = (ExecutionEntity) result.getExecution();
        assertThat(entity.getActivityId()).isEqualTo("messageCatch");
        executionResultCount++;
      } else {
        checkProcessDefinitionMessageCorrelationResult(result, "process");
        procDefResultCount++;
      }
    }
    assertThat(executionResultCount).isEqualTo(2);
    assertThat(procDefResultCount).isEqualTo(1);
  }

  @Test
  void testMessageStartEventCorrelationWithNonMatchingDefinition() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("aMessageName");

    // when/then
    assertThatThrownBy(() -> runtimeService.correlateMessage("aMessageName"))
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message");

    // fluent builder //////////////////
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message");

    // fluent builder with multiple correlation //////////////////
    // This should not fail
    runtimeService.createMessageCorrelation("aMessageName").correlateAll();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKeyAndVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", "anotherBusinessKey", variables);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("aProcessVariable", "aVariableValue");
    runtimeService.correlateMessage(messageName, "aBusinessKey", correlationKeys, processVariables);

    Execution correlatedExecution = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aProcessVariable", "aVariableValue")
        .singleResult();

    assertThat(correlatedExecution).isNotNull();

    ProcessInstance correlatedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId()).singleResult();

    assertThat(correlatedProcessInstance.getBusinessKey()).isEqualTo("aBusinessKey");

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // fluent builder /////////////////////////////

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    processInstance = runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);

    runtimeService.createMessageCorrelation(messageName)
      .processInstanceBusinessKey("aBusinessKey")
      .processInstanceVariableEquals("aKey", "aValue")
      .setVariable("aProcessVariable", "aVariableValue")
      .correlate();

    correlatedExecution = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aProcessVariable", "aVariableValue")
        .singleResult();

    assertThat(correlatedExecution).isNotNull();

    correlatedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId()).singleResult();

    assertThat(correlatedProcessInstance.getBusinessKey()).isEqualTo("aBusinessKey");

    runtimeService.deleteProcessInstance(processInstance.getId(), null);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKeyAndVariablesUsingFluentCorrelateAll() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);

    String messageName = "newInvoiceMessage";
    runtimeService.createMessageCorrelation(messageName)
      .processInstanceBusinessKey("aBusinessKey")
      .processInstanceVariableEquals("aKey", "aValue")
      .setVariable("aProcessVariable", "aVariableValue")
      .correlateAll();

    List<Execution> correlatedExecutions = runtimeService
        .createExecutionQuery()
        .activityId("task")
        .processVariableValueEquals("aProcessVariable", "aVariableValue")
        .list();

    assertThat(correlatedExecutions).hasSize(2);

    // Instance 1
    Execution correlatedExecution = correlatedExecutions.get(0);
    ProcessInstance correlatedProcessInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId())
        .singleResult();

    assertThat(correlatedProcessInstance.getBusinessKey()).isEqualTo("aBusinessKey");

    // Instance 2
    correlatedExecution = correlatedExecutions.get(1);
    correlatedProcessInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId())
        .singleResult();

    assertThat(correlatedProcessInstance.getBusinessKey()).isEqualTo("aBusinessKey");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByProcessInstanceId() {

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("process");

    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("process");
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("aMessageName");

    // correlation with only the name is ambiguous:
    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message");

    // use process instance id as well
    runtimeService.createMessageCorrelation("newInvoiceMessage")
      .processInstanceId(processInstance1.getId())
      .correlate();

    Execution correlatedExecution = runtimeService.createExecutionQuery()
        .activityId("task")
        .processInstanceId(processInstance1.getId())
        .singleResult();
    assertThat(correlatedExecution).isNotNull();

    Execution uncorrelatedExecution = runtimeService.createExecutionQuery()
        .activityId("task")
        .processInstanceId(processInstance2.getId())
        .singleResult();
    assertThat(uncorrelatedExecution).isNull();

    runtimeService.deleteProcessInstance(processInstance1.getId(), null);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByProcessInstanceIdUsingFluentCorrelateAll() {
    // correlate by name
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process")).doesNotThrowAnyException();

    // correlation with only the name is ambiguous:
    runtimeService
      .createMessageCorrelation("aMessageName")
      .correlateAll();

    assertThat(runtimeService.createExecutionQuery().activityId("task").count()).isZero();

    // correlate process instance id
    var processInstance1 = runtimeService.startProcessInstanceByKey("process");

    var processInstance2 = runtimeService.startProcessInstanceByKey("process");

    // use process instance id as well
    runtimeService
      .createMessageCorrelation("newInvoiceMessage")
      .processInstanceId(processInstance1.getId())
      .correlateAll();

    Execution correlatedExecution = runtimeService
        .createExecutionQuery()
        .activityId("task")
        .processInstanceId(processInstance1.getId())
        .singleResult();
    assertThat(correlatedExecution).isNotNull();

    Execution uncorrelatedExecution = runtimeService
        .createExecutionQuery()
        .activityId("task")
        .processInstanceId(processInstance2.getId())
        .singleResult();
    assertThat(uncorrelatedExecution).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKeyAndNullVariableUsingFluentCorrelateAll() {
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey");

    String messageName = "newInvoiceMessage";
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(messageName)
        .processInstanceBusinessKey("aBusinessKey");

    // when/then
    assertThatThrownBy(() -> messageCorrelationBuilder.setVariable(null, "aVariableValue"))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("null");

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKeyAndNullVariableEqualsUsingFluentCorrelateAll() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);

    String messageName = "newInvoiceMessage";
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(messageName)
        .processInstanceBusinessKey("aBusinessKey");

    // when/then
    assertThatThrownBy(() -> messageCorrelationBuilder.processInstanceVariableEquals(null, "bar"))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("null");

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKeyAndNullVariablesUsingFluentCorrelateAll() {
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey");

    String messageName = "newInvoiceMessage";

    runtimeService.createMessageCorrelation(messageName)
      .processInstanceBusinessKey("aBusinessKey")
      .setVariables(null)
      .setVariable("foo", "bar")
      .correlateAll();

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .processVariableValueEquals("foo", "bar")
      .list();

    assertThat(correlatedExecutions).isNotEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByVariablesOnly() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("variable", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);

    variables.put("variable", "value2");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", variables);

    runtimeService.correlateMessage(null, variables);

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .list();

    assertThat(correlatedExecutions).hasSize(1);
    assertThat(correlatedExecutions.get(0).getId()).isEqualTo(instance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByBusinessKey() {
    runtimeService.startProcessInstanceByKey("process", "businessKey1");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", "businessKey2");

    runtimeService.correlateMessage(null, "businessKey2");

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .list();

    assertThat(correlatedExecutions).hasSize(1);
    assertThat(correlatedExecutions.get(0).getId()).isEqualTo(instance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationByProcessInstanceIdOnly() {
    runtimeService.startProcessInstanceByKey("process");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    runtimeService
      .createMessageCorrelation(null)
      .processInstanceId(instance.getId())
      .correlate();

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .list();

    assertThat(correlatedExecutions).hasSize(1);
    assertThat(correlatedExecutions.get(0).getId()).isEqualTo(instance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationWithoutMessageNameFluent() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("variable", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);

    variables.put("variable", "value2");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", variables);

    runtimeService.createMessageCorrelation(null)
      .processInstanceVariableEquals("variable", "value2")
      .correlate();

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .list();

    assertThat(correlatedExecutions).hasSize(1);
    assertThat(correlatedExecutions.get(0).getId()).isEqualTo(instance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCorrelateAllWithoutMessage.bpmn20.xml"})
  @Test
  void testCorrelateAllWithoutMessage() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("variable", "value1");
    runtimeService.startProcessInstanceByKey("process", variables);
    runtimeService.startProcessInstanceByKey("secondProcess", variables);

    variables.put("variable", "value2");
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("process", variables);
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("secondProcess", variables);

    runtimeService.createMessageCorrelation(null)
      .processInstanceVariableEquals("variable", "value2")
      .correlateAll();

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .orderByProcessDefinitionKey()
      .asc()
      .list();

    assertThat(correlatedExecutions).hasSize(2);
    assertThat(correlatedExecutions.get(0).getId()).isEqualTo(instance1.getId());
    assertThat(correlatedExecutions.get(1).getId()).isEqualTo(instance2.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationWithoutMessageDoesNotMatchStartEvent() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(null)
        .processInstanceVariableEquals("variable", "value2");
    assertThatThrownBy(messageCorrelationBuilder::correlate).isInstanceOf(MismatchingMessageCorrelationException.class);

    List<Execution> correlatedExecutions = runtimeService
      .createExecutionQuery()
      .activityId("task")
      .list();

    assertThat(correlatedExecutions).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelationWithoutCorrelationPropertiesFails() {

    runtimeService.startProcessInstanceByKey("process");
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(null);

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(NullValueException.class)
      .hasMessage("At least one of the following correlation criteria has to be present: messageName, businessKey, correlationKeys, processInstanceId");

    assertThatThrownBy(() -> runtimeService.correlateMessage(null))
      .isInstanceOf(NullValueException.class)
      .hasMessage("At least one of the following correlation criteria has to be present: messageName, businessKey, correlationKeys, processInstanceId");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/twoBoundaryEventSubscriptions.bpmn20.xml")
  @Test
  void testCorrelationToExecutionWithMultipleSubscriptionsFails() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation(null)
        .processInstanceId(instance.getId());

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("More than one matching message subscription found for execution");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testSuspendedProcessInstance() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    String processInstance = runtimeService.startProcessInstanceByKey("process", variables).getId();

    // suspend process instance
    runtimeService.suspendProcessInstanceById(processInstance);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");

    assertThatThrownBy(() -> runtimeService.correlateMessage(messageName, correlationKeys)).isInstanceOf(MismatchingMessageCorrelationException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testOneMatchingAndOneSuspendedProcessInstance() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    String firstProcessInstance = runtimeService.startProcessInstanceByKey("process", variables).getId();

    variables = new HashMap<>();
    variables.put("aKey", "aValue");
    String secondProcessInstance = runtimeService.startProcessInstanceByKey("process", variables).getId();

    // suspend second process instance
    runtimeService.suspendProcessInstanceById(secondProcessInstance);

    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<>();
    correlationKeys.put("aKey", "aValue");

    Map<String, Object> messagePayload = new HashMap<>();
    messagePayload.put("aNewKey", "aNewVariable");

    runtimeService.correlateMessage(messageName, correlationKeys, messagePayload);

    // there exists an uncorrelated executions (the second process instance)
    long uncorrelatedExecutions = runtimeService
        .createExecutionQuery()
        .processInstanceId(secondProcessInstance)
        .processVariableValueEquals("aKey", "aValue")
        .messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertThat(uncorrelatedExecutions).isOne();

    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService
        .createExecutionQuery()
        .processInstanceId(firstProcessInstance)
        .activityId("task")
        .processVariableValueEquals("aKey", "aValue")
        .processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertThat(correlatedExecutions).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testSuspendedProcessDefinition() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    repositoryService.suspendProcessDefinitionById(processDefinitionId);

    Map<String, Object> variables = new HashMap<>();
    variables.put("aKey", "aValue");
    var processVariables = new HashMap<String, Object>();

    assertThatThrownBy(() -> runtimeService.correlateMessage("newInvoiceMessage", processVariables, variables)).isInstanceOf(MismatchingMessageCorrelationException.class);
  }

  @Test
  void testCorrelateMessageStartEventWithProcessDefinitionId() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("a")
        .userTask()
        .endEvent()
        .done());

    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("b")
        .userTask()
        .endEvent()
        .done());

    ProcessDefinition firstProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).singleResult();
    ProcessDefinition secondProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();

    runtimeService.createMessageCorrelation("a")
      .processDefinitionId(firstProcessDefinition.getId())
      .processInstanceBusinessKey("first")
      .correlateStartMessage();

    runtimeService.createMessageCorrelation("b")
      .processDefinitionId(secondProcessDefinition.getId())
      .processInstanceBusinessKey("second")
      .correlateStartMessage();

    assertThat(runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey("first").processDefinitionId(firstProcessDefinition.getId()).count()).isOne();
    assertThat(runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey("second").processDefinitionId(secondProcessDefinition.getId()).count()).isOne();
  }

  @Test
  void testFailCorrelateMessageStartEventWithWrongProcessDefinitionId() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("a")
        .userTask()
        .endEvent()
        .done());

    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("b")
        .userTask()
        .endEvent()
        .done());

    ProcessDefinition latestProcessDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().singleResult();
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processDefinitionId(latestProcessDefinition.getId());

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message 'a'");
  }

  @Test
  void testFailCorrelateMessageStartEventWithNonExistingProcessDefinitionId() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processDefinitionId("not existing");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed process definition found");
  }

  @Test
  void testFailCorrelateMessageWithProcessDefinitionId() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processDefinitionId("id");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a process definition id");
  }

  @Test
  void testFailCorrelateMessagesWithProcessDefinitionId() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processDefinitionId("id");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateAll)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a process definition id");
  }

  @Test
  void testFailCorrelateMessageStartEventWithCorrelationVariable() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processInstanceVariableEquals("var", "value");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify correlation variables ");
  }

  @Test
  void testFailCorrelateMessageStartEventWithCorrelationVariables() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .processInstanceVariablesEqual(Variables
              .createVariables()
              .putValue("var1", "b")
              .putValue("var2", "c"));

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify correlation variables ");
  }

  @Test
  void testCorrelationWithResultBySettingLocalVariables() {
    // given
    String outputVarName = "localVar";
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .intermediateCatchEvent("message_1")
            .message("1")
            .operatonOutputParameter(outputVarName, "${testLocalVar}")
          .userTask("UserTask_1")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messageLocalPayload = new HashMap<>();
    String outputValue = "outputValue";
    String localVarName = "testLocalVar";
    messageLocalPayload.put(localVarName, outputValue);

    // when
    MessageCorrelationResultWithVariables messageCorrelationResult = runtimeService
        .createMessageCorrelation("1")
        .setVariablesLocal(messageLocalPayload)
        .correlateWithResultAndVariables(true);

    // then
    checkExecutionMessageCorrelationResult(messageCorrelationResult, processInstance, "message_1");

    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .variableName(outputVarName)
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
    assertThat(variable.getExecutionId()).isEqualTo(processInstance.getId());

    VariableInstance variableNonExisting = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .variableName(localVarName)
        .singleResult();
    assertThat(variableNonExisting).isNull();

    VariableMap variablesInReturn = messageCorrelationResult.getVariables();
    assertThat(variablesInReturn.<StringValue>getValueTyped(outputVarName)).isEqualTo(variable.getTypedValue());
    assertThat(variablesInReturn.getValue("processInstanceVar", String.class)).isEqualTo("processInstanceVarValue");
  }

  @Test
  void testCorrelationBySettingLocalVariables() {
    // given
    String outputVarName = "localVar";
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .intermediateCatchEvent("message_1")
            .message("1")
            .operatonOutputParameter(outputVarName, "${testLocalVar}")
          .userTask("UserTask_1")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messageLocalPayload = new HashMap<>();
    String outputValue = "outputValue";
    String localVarName = "testLocalVar";
    messageLocalPayload.put(localVarName, outputValue);

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesLocal(messageLocalPayload)
        .correlate();

    // then
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .variableName(outputVarName)
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
    assertThat(variable.getExecutionId()).isEqualTo(processInstance.getId());

    VariableInstance variableNonExisting = runtimeService
        .createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId())
        .variableName(localVarName)
        .singleResult();
    assertThat(variableNonExisting).isNull();
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testCorrelationWithTransientLocalVariables() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateCatchEvent("message_1").message("message")
        .userTask("UserTask_1")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey("process");

    StringValue transientValue = Variables.stringValue("value", true);

    Map<String, Object> messageLocalPayload = new HashMap<>();
    messageLocalPayload.put("var", transientValue);

    // when
    runtimeService
        .createMessageCorrelation("message")
        .setVariablesLocal(messageLocalPayload)
        .correlate();

    // then
    long numHistoricVariables =
        engineRule.getHistoryService()
          .createHistoricVariableInstanceQuery()
          .count();

    assertThat(numHistoricVariables).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.waitForMessageProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.sendMessageProcess.bpmn20.xml"})
  @Test
  void testCorrelateWithResultTwoTimesInSameTransaction() {
    // start process that waits for message
    Map<String, Object> variables = new HashMap<>();
    variables.put("correlationKey", "someCorrelationKey");
    ProcessInstance messageWaitProcess = runtimeService.startProcessInstanceByKey("waitForMessageProcess", variables);

    Execution waitingProcess = runtimeService.createExecutionQuery().executionId(messageWaitProcess.getProcessInstanceId()).singleResult();
    assertThat(waitingProcess).isNotNull();

    VariableMap switchScenarioFlag = Variables.createVariables().putValue("allFlag", false);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("sendMessageProcess", switchScenarioFlag))
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message 'waitForCorrelationKeyMessage'");

    // waiting process has not finished
    waitingProcess = runtimeService.createExecutionQuery().executionId(messageWaitProcess.getProcessInstanceId()).singleResult();
    assertThat(waitingProcess).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.waitForMessageProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.sendMessageProcess.bpmn20.xml"})
  @Test
  void testCorrelateAllWithResultTwoTimesInSameTransaction() {
    // start process that waits for message
    Map<String, Object> variables = new HashMap<>();
    variables.put("correlationKey", "someCorrelationKey");
    ProcessInstance messageWaitProcess = runtimeService.startProcessInstanceByKey("waitForMessageProcess", variables);

    Execution waitingProcess = runtimeService.createExecutionQuery().executionId(messageWaitProcess.getProcessInstanceId()).singleResult();
    assertThat(waitingProcess).isNotNull();

    // start process that sends two messages with the same correlationKey
    VariableMap switchScenarioFlag = Variables.createVariables().putValue("allFlag", true);
    runtimeService.startProcessInstanceByKey("sendMessageProcess", switchScenarioFlag);

    // waiting process must be finished
    waitingProcess = runtimeService.createExecutionQuery().executionId(messageWaitProcess.getProcessInstanceId()).singleResult();
    assertThat(waitingProcess).isNull();
  }

  @Test
  void testMessageStartEventCorrelationWithLocalVariables() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .message("1")
        .userTask("userTask1")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String localVarName = "testLocalVar";
    messagePayload.put(localVarName, outputValue);

    // when
    MessageCorrelationResult result = runtimeService
        .createMessageCorrelation("1")
        .setVariablesLocal(messagePayload)
        .correlateWithResult();

    // then
    assertThat(result).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.ProcessDefinition);
  }

  @Test
  void testMessageStartEventCorrelationWithVariablesInResult() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .message("1")
        .userTask("UserTask_1")
        .endEvent()
        .done();

    testRule.deploy(model);

    // when
    MessageCorrelationResultWithVariables result = runtimeService
        .createMessageCorrelation("1")
        .setVariable("foo", "bar")
        .correlateWithResultAndVariables(true);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.ProcessDefinition);
    assertThat(result.getVariables().getValue("foo", String.class)).isEqualTo("bar");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  @Test
  void testCorrelateAllWithResultVariables() {
    //given
    ProcessInstance procInstance1 = runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("var1", "foo"));
    ProcessInstance procInstance2 = runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("var2", "bar"));

    //when correlated all with result and variables
    List<MessageCorrelationResultWithVariables> resultList = runtimeService
        .createMessageCorrelation("newInvoiceMessage")
        .correlateAllWithResultAndVariables(true);

    assertThat(resultList).hasSize(2);
    //then result should contain executions on which messages was correlated
    for (MessageCorrelationResultWithVariables result : resultList) {
      assertThat(result).isNotNull();
      assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
      ExecutionEntity execution = (ExecutionEntity) result.getExecution();
      VariableMap variables = result.getVariables();
      assertThat(variables).hasSize(1);
      if (procInstance1.getId().equalsIgnoreCase(execution.getProcessInstanceId())) {
        assertThat(variables.getValue("var1", String.class)).isEqualTo("foo");
      } else if (procInstance2.getId().equalsIgnoreCase(execution.getProcessInstanceId())) {
        assertThat(variables.getValue("var2", String.class)).isEqualTo("bar");
      } else {
        assertThat(execution.getProcessInstanceId()).as("Only those process instances should exist").isIn(procInstance1.getId(), procInstance2.getId());
      }
    }
  }

  @Test
  void testCorrelationWithModifiedVariablesInResult() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .intermediateCatchEvent("Message_1")
        .message("1")
        .serviceTask()
        .operatonClass(ChangeVariableDelegate.class.getName())
        .userTask("UserTask_1")
        .endEvent()
        .done();

    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey("Process_1",
        Variables.createVariables()
        .putValue("a", 40)
        .putValue("b", 2));

    // when
    MessageCorrelationResultWithVariables result = runtimeService
        .createMessageCorrelation("1")
        .correlateWithResultAndVariables(true);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getResultType()).isEqualTo(MessageCorrelationResultType.Execution);
    assertThat(result.getVariables()).hasSize(3);
    assertThat(result.getVariables()).containsEntry("a", "foo");
    assertThat(result.getVariables()).containsEntry("b", 2);
    assertThat(result.getVariables()).containsEntry("sum", 42);
  }

  @Test
  void testCorrelateWithVariablesInReturnShouldDeserializeObjectValue()
  {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
      .startEvent()
      .intermediateCatchEvent("Message_1")
      .message("1")
      .userTask("UserTask_1")
      .endEvent()
      .done();

    testRule.deploy(model);

    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    MessageCorrelationResultWithVariables result = runtimeService.createMessageCorrelation("1")
        .correlateWithResultAndVariables(true);

    // then
    VariableMap resultVariables = result.getVariables();

    ObjectValue returnedValue = resultVariables.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isTrue();
    assertThat(returnedValue.getValue()).isEqualTo("value");
  }

  @Test
  void testCorrelateWithVariablesInReturnShouldNotDeserializeObjectValue()
  {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
      .startEvent()
      .intermediateCatchEvent("Message_1")
      .message("1")
      .userTask("UserTask_1")
      .endEvent()
      .done();

    testRule.deploy(model);

    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", variables);
    String serializedValue = ((ObjectValue) runtimeService.getVariableTyped(instance.getId(), "var")).getValueSerialized();

    // when
    MessageCorrelationResultWithVariables result = runtimeService.createMessageCorrelation("1")
        .correlateWithResultAndVariables(false);

    // then
    VariableMap resultVariables = result.getVariables();

    ObjectValue returnedValue = resultVariables.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isFalse();
    assertThat(returnedValue.getValueSerialized()).isEqualTo(serializedValue);
  }

  @Test
  void testCorrelateAllWithVariablesInReturnShouldDeserializeObjectValue()
  {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
      .startEvent()
      .intermediateCatchEvent("Message_1")
      .message("1")
      .userTask("UserTask_1")
      .endEvent()
      .done();

    testRule.deploy(model);

    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    runtimeService.startProcessInstanceByKey("process", variables);

    // when
    List<MessageCorrelationResultWithVariables> result = runtimeService.createMessageCorrelation("1")
        .correlateAllWithResultAndVariables(true);

    // then
    assertThat(result).hasSize(1);

    VariableMap resultVariables = result.get(0).getVariables();

    ObjectValue returnedValue = resultVariables.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isTrue();
    assertThat(returnedValue.getValue()).isEqualTo("value");
  }

  @Test
  void testCorrelateAllWithVariablesInReturnShouldNotDeserializeObjectValue()
  {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
      .startEvent()
      .intermediateCatchEvent("Message_1")
      .message("1")
      .userTask("UserTask_1")
      .endEvent()
      .done();

    testRule.deploy(model);

    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process", variables);
    String serializedValue = ((ObjectValue) runtimeService.getVariableTyped(instance.getId(), "var")).getValueSerialized();

    // when
    List<MessageCorrelationResultWithVariables> result = runtimeService.createMessageCorrelation("1")
        .correlateAllWithResultAndVariables(false);

    // then
    assertThat(result).hasSize(1);

    VariableMap resultVariables = result.get(0).getVariables();

    ObjectValue returnedValue = resultVariables.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isFalse();
    assertThat(returnedValue.getValueSerialized()).isEqualTo(serializedValue);
  }

  @Test
  void testStartMessageOnlyFlag() {
    deployTwoVersionsWithStartMessageEvent();

    ProcessDefinition firstProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).singleResult();
    ProcessDefinition secondProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();

    runtimeService.createMessageCorrelation("a")
      .startMessageOnly()
      .processDefinitionId(firstProcessDefinition.getId())
      .processInstanceBusinessKey("first")
      .correlate();

    runtimeService.createMessageCorrelation("a")
      .startMessageOnly()
      .processDefinitionId(secondProcessDefinition.getId())
      .processInstanceBusinessKey("second")
      .correlate();

    assertTwoInstancesAreStarted(firstProcessDefinition, secondProcessDefinition);
  }

  @Test
  void testStartMessageOnlyFlagAll() {
    deployTwoVersionsWithStartMessageEvent();

    ProcessDefinition firstProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).singleResult();
    ProcessDefinition secondProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2).singleResult();

    runtimeService.createMessageCorrelation("a")
      .startMessageOnly()
      .processDefinitionId(firstProcessDefinition.getId())
      .processInstanceBusinessKey("first")
      .correlateAll();

    runtimeService.createMessageCorrelation("a")
      .startMessageOnly()
      .processDefinitionId(secondProcessDefinition.getId())
      .processInstanceBusinessKey("second")
      .correlateAll();

    assertTwoInstancesAreStarted(firstProcessDefinition, secondProcessDefinition);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testStartMessageOnlyFlagWithResult() {
    MessageCorrelationResult result = runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .startMessageOnly()
      .correlateWithResult();

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue");
    assertThat(processInstanceQuery.count()).isOne();
    assertThat(result.getProcessInstance().getId()).isEqualTo(processInstanceQuery.singleResult().getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testStartMessageOnlyFlagWithVariablesInResult() {

    MessageCorrelationResultWithVariables result = runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .startMessageOnly()
      .correlateWithResultAndVariables(false);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue");
    assertThat(processInstanceQuery.count()).isOne();
    assertThat(result.getVariables()).hasSize(1);
    assertThat(result.getVariables().getValueTyped("aKey").getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testStartMessageOnlyFlagAllWithResult() {
    List<MessageCorrelationResult> result = runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .startMessageOnly()
      .correlateAllWithResult();

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue");
    assertThat(processInstanceQuery.count()).isOne();
    assertThat(result.get(0).getProcessInstance().getId()).isEqualTo(processInstanceQuery.singleResult().getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/message/MessageCorrelationTest.testMessageStartEventCorrelation.bpmn20.xml")
  @Test
  void testStartMessageOnlyFlagAllWithVariablesInResult() {

    List<MessageCorrelationResultWithVariables> results = runtimeService.createMessageCorrelation("newInvoiceMessage")
      .setVariable("aKey", "aValue")
      .startMessageOnly()
      .correlateAllWithResultAndVariables(false);

    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue");
    assertThat(processInstanceQuery.count()).isOne();
    MessageCorrelationResultWithVariables result = results.get(0);
    assertThat(result.getVariables()).hasSize(1);
    assertThat(result.getVariables().getValueTyped("aKey").getValue()).isEqualTo("aValue");
  }

  @Test
  void testFailStartMessageOnlyFlagWithCorrelationVariable() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .startMessageOnly()
        .processInstanceVariableEquals("var", "value");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify correlation variables ");
  }

  @Test
  void testFailStartMessageOnlyFlagWithCorrelationVariables() {
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("a")
        .startMessageOnly()
        .processInstanceVariablesEqual(Variables
              .createVariables()
              .putValue("var1", "b")
              .putValue("var2", "c"));

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateAll)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify correlation variables ");
  }

  @Test
  void shouldCorrelateNonInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = createModelWithBoundaryEvent(false, false);

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = createModelWithBoundaryEvent(true, false);

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();

    // then the scope is the PI
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(processInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateEventSubprocessNonInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance targetModel = createModelWithEventSubprocess(false, false);
    testRule.deploy(targetModel);
    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    String outputValue = "outputValue";
    String variableName = "testVar";

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariableToTriggeredScope(variableName, outputValue)
        .correlate();

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateEventSubprocessInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance targetModel = createModelWithEventSubprocess(true, false);
    testRule.deploy(targetModel);
    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    String outputValue = "outputValue";
    String variableName = "testVar";

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariableToTriggeredScope(variableName, outputValue)
        .correlate();

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateStartWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .message("1")
        .userTask("afterMessage")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    // when
    MessageCorrelationResult result = runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlateWithResult();

    // then the scope is the PI
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableScopeIdIn(result.getProcessInstance().getId())
        .variableName(variableName)
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldCorrelateIntermediateCatchMessageWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .intermediateCatchEvent("Message_1")
            .message("1")
        .userTask("afterMessage")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();

    // the scope was "Message" activity
    HistoryService historyService = engineRule.getHistoryService();
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName(variableName)
        .singleResult();
    assertThat(historicVariable).isNotNull();
    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("Message_1").singleResult();
    assertThat(historicActivity).isNotNull();
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(historicActivity.getId());

  }

  @Test
  void shouldFailCorrelateWithNullVariableNameInNewScope() {
    // given
    BpmnModelInstance targetModel = createModelWithBoundaryEvent(true, false);
    testRule.deploy(targetModel);
    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("1");

    // when/then
    assertThatThrownBy(() -> messageCorrelationBuilder.setVariableToTriggeredScope(null, "outputValue"))
    .isInstanceOf(NullValueException.class)
    .hasMessageContaining("variableName");
  }

  @Test
  void shouldCorrelateAsyncNonInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = createModelWithBoundaryEvent(false, true);

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);


    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();
    Job asyncJob = engineRule.getManagementService().createJobQuery().list().get(0);
    assertThat(asyncJob).isNotNull();
    engineRule.getManagementService().executeJob(asyncJob.getId());

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateAsyncInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = createModelWithBoundaryEvent(true, true);

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();
    Job asyncJob = engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(asyncJob).isNotNull();
    engineRule.getManagementService().executeJob(asyncJob.getId());

    // then the scope is the PI
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(processInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateEventSubprocessAsyncNonInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance targetModel = createModelWithEventSubprocess(false, true);
    testRule.deploy(targetModel);
    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    String outputValue = "outputValue";
    String variableName = "testVar";

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariableToTriggeredScope(variableName, outputValue)
        .correlate();
    Job asyncJob = engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(asyncJob).isNotNull();
    engineRule.getManagementService().executeJob(asyncJob.getId());

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  void shouldCorrelateEventSubprocessAsyncInterruptingWithVariablesInNewScope() {
    // given
    BpmnModelInstance targetModel = createModelWithEventSubprocess(true, true);
    testRule.deploy(targetModel);
    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    String outputValue = "outputValue";
    String variableName = "testVar";

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariableToTriggeredScope(variableName, outputValue)
        .correlate();
    Job asyncJob = engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(asyncJob).isNotNull();
    engineRule.getManagementService().executeJob(asyncJob.getId());

    // then the scope is "afterMessage" activity
    Execution activityInstance = runtimeService.createExecutionQuery().activityId("afterMessage").singleResult();
    assertThat(activityInstance).isNotNull();
    VariableInstance variable = runtimeService
        .createVariableInstanceQuery()
        .variableName(variableName)
        .variableScopeIdIn(activityInstance.getId())
        .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputValue);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldCorrelateAsyncIntermediateCatchMessageWithVariablesInNewScope() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .intermediateCatchEvent("Message_1")
          .operatonAsyncBefore(true)
            .message("1")
        .userTask("afterMessage")
        .endEvent()
        .done();

    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("processInstanceVar", "processInstanceVarValue");
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", variables);

    Map<String, Object> messagePayload = new HashMap<>();
    String outputValue = "outputValue";
    String variableName = "testVar";
    messagePayload.put(variableName, outputValue);

    Job asyncJob = engineRule.getManagementService().createJobQuery().singleResult();
    assertThat(asyncJob).isNotNull();
    engineRule.getManagementService().executeJob(asyncJob.getId());

    // when
    runtimeService
        .createMessageCorrelation("1")
        .setVariablesToTriggeredScope(messagePayload)
        .correlate();

    // the scope was "Message" activity
    HistoryService historyService = engineRule.getHistoryService();
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName(variableName)
        .singleResult();
    assertThat(historicVariable).isNotNull();
    HistoricActivityInstance historicActivity = historyService.createHistoricActivityInstanceQuery()
        .activityId("Message_1").singleResult();
    assertThat(historicActivity).isNotNull();
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(historicActivity.getId());

  }

  // helpers ------------------------------------------------------------------

  protected void deployTwoVersionsWithStartMessageEvent() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("a")
        .userTask("ut1")
        .endEvent()
        .done());

    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
          .message("a")
        .userTask("ut2")
        .endEvent()
        .done());
  }

  protected void assertTwoInstancesAreStarted(ProcessDefinition firstProcessDefinition, ProcessDefinition secondProcessDefinition) {
    assertThat(runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey("first")
      .processDefinitionId(firstProcessDefinition.getId())
      .count()).isOne();
    assertThat(runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey("second")
      .processDefinitionId(secondProcessDefinition.getId())
      .count()).isOne();
  }

  protected BpmnModelInstance createModelWithEventSubprocess(boolean isInterrupting, boolean isAsync) {
    return modify(Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .subProcess("Subprocess_1")
          .embeddedSubProcess()
          .startEvent()
          .userTask()
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done())
        .addSubProcessTo("Subprocess_1")
        .triggerByEvent()
        .embeddedSubProcess()
          .startEvent("Message_1")
          .operatonAsyncBefore(isAsync)
          .interrupting(isInterrupting)
          .message("1")
          .exclusiveGateway("Gateway_1")
          .condition("Condition_1", "${testVar == 'outputValue'}")
            .userTask("afterMessage")
            .endEvent("happyEnd")
          .moveToLastGateway()
          .condition("Condition_2", "${testVar != 'outputValue'}")
            .userTask("wrongOutcome")
            .endEvent("unhappyEnd")
            .done();
  }

  protected BpmnModelInstance createModelWithBoundaryEvent(boolean isInterrupting, boolean isAsync) {
    return Bpmn.createExecutableProcess("Process_1")
        .startEvent()
        .userTask("UserTask_1")
          .boundaryEvent("Message_1")
          .operatonAsyncBefore(isAsync)
          .cancelActivity(isInterrupting)
          .message("1")
            .exclusiveGateway("Gateway_1")
            .condition("Condition_1", "${testVar == 'outputValue'}")
              .userTask("afterMessage")
              .endEvent("happyEnd")
            .moveToLastGateway()
            .condition("Condition_2", "${testVar != 'outputValue'}")
              .userTask("wrongOutcome")
              .endEvent("unhappyEnd")
        .done();
  }


  public static class ChangeVariableDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Integer a = (Integer) execution.getVariable("a");
      Integer b = (Integer) execution.getVariable("b");
      execution.setVariable("sum", a + b);
      execution.setVariable("a", "foo");
    }
  }
}
