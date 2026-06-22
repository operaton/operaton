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
package org.operaton.bpm.client.variable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.dto.ProcessDefinitionDto;
import org.operaton.bpm.client.dto.ProcessInstanceDto;
import org.operaton.bpm.client.rule.ClientRule;
import org.operaton.bpm.client.rule.EngineRule;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskService;
import org.operaton.bpm.client.util.RecordingExternalTaskHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler.RecordedInvocation;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_BAR;
import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_FOO;
import static org.operaton.bpm.client.util.ProcessModels.TWO_EXTERNAL_TASK_PROCESS;
import static org.operaton.bpm.engine.variable.type.ValueType.*;
import static org.assertj.core.api.Assertions.assertThat;

class PrimitiveVariableIT {

  protected static final String VARIABLE_NAME = "foo";

  protected static final String VARIABLE_NAME_INT = "intValue";
  protected static final String VARIABLE_NAME_LONG = "longValue";
  protected static final String VARIABLE_NAME_SHORT = "shortValue";
  protected static final String VARIABLE_NAME_DOUBLE = "doubleValue";
  protected static final String VARIABLE_NAME_STRING = "stringValue";
  protected static final String VARIABLE_NAME_BOOLEAN = "booleanValue";
  protected static final String VARIABLE_NAME_DATE = "dateValue";
  protected static final String VARIABLE_NAME_BYTES = "bytesValue";
  protected static final String VARIABLE_NAME_NULL = "nullValue";

  protected static final int VARIABLE_VALUE_INT = 123;
  protected static final long VARIABLE_VALUE_LONG = 123L;
  protected static final short VARIABLE_VALUE_SHORT = (short) 123;
  protected static final double VARIABLE_VALUE_DOUBLE = 12.34;
  protected static final String VARIABLE_VALUE_STRING = "bar";
  protected static final boolean VARIABLE_VALUE_BOOLEAN = true;
  protected static final Date VARIABLE_VALUE_DATE = new Date(1514790000000L);
  protected static final byte[] VARIABLE_VALUE_BYTES = VARIABLE_VALUE_STRING.getBytes();
//  protected static final InputStream VARIABLE_VALUE_BYTES_INPUTSTREAM = new ByteArrayInputStream(VARIABLE_VALUE_STRING.getBytes());

  @RegisterExtension
  static ClientRule clientRule = new ClientRule();
  @RegisterExtension
  static EngineRule engineRule = new EngineRule();

  protected ExternalTaskClient client;

  protected ProcessDefinitionDto processDefinition;
  protected ProcessInstanceDto processInstance;

  protected RecordingExternalTaskHandler handler = new RecordingExternalTaskHandler();
  protected RecordingInvocationHandler invocationHandler = new RecordingInvocationHandler();

  @BeforeEach
  void setup() {
    client = clientRule.client();
    processDefinition = engineRule.deploy(TWO_EXTERNAL_TASK_PROCESS).get(0);

    handler.clear();
    invocationHandler.clear();
  }

  @Test
  void shoudGetVariable_Integer() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.integerValue(VARIABLE_VALUE_INT));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    int variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_INT);
  }

  @Test
  void shoudGetVariableTyped_Integer() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.integerValue(VARIABLE_VALUE_INT));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_INT);
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudGetVariable_NullInteger() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.integerValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Integer variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullInteger() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.integerValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudSetVariable_Integer() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_INT);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    int variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_INT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_INT);
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudSetVariableTyped_Integer() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.integerValue(VARIABLE_VALUE_INT));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    int variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_INT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_INT);
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudSetVariableUntyped_Integer() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_INT));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    int variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_INT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_INT);
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudSetVariableTyped_NullInteger() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.integerValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Integer variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(INTEGER);
  }

  @Test
  void shoudGetVariable_Long() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.longValue(VARIABLE_VALUE_LONG));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_LONG);
  }

  @Test
  void shoudGetVariableTyped_Long() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.longValue(VARIABLE_VALUE_LONG));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_LONG);
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudGetVariable_NullLong() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.longValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullLong() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.longValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudSetVariable_Long() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_LONG);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_LONG);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_LONG);
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudSetVariableTyped_Long() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.longValue(VARIABLE_VALUE_LONG));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_LONG);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_LONG);
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudSetVariableUntyped_Long() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_LONG));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_INT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_LONG);
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudSetVariableTyped_NullLong() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.longValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Long variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(LONG);
  }

  @Test
  void shoudGetVariable_Short() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.shortValue(VARIABLE_VALUE_SHORT));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_SHORT);
  }

  @Test
  void shoudGetVariableTyped_Short() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.shortValue(VARIABLE_VALUE_SHORT));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_SHORT);
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudGetVariable_NullShort() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.shortValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullShort() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.shortValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudSetVariable_Short() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_SHORT);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_SHORT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_SHORT);
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudSetVariableTyped_Short() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.shortValue(VARIABLE_VALUE_SHORT));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_SHORT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_SHORT);
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudSetVariableUntyped_Short() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_SHORT));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_SHORT);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_SHORT);
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudSetVariableTyped_NullShort() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.shortValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Short variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(SHORT);
  }

  @Test
  void shoudGetVariable_Double() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.doubleValue(VARIABLE_VALUE_DOUBLE));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DOUBLE);
  }

  @Test
  void shoudGetVariableTyped_Double() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.doubleValue(VARIABLE_VALUE_DOUBLE));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DOUBLE);
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudGetVariable_NullDouble() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.doubleValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullDouble() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.doubleValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudSetVariable_Double() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_DOUBLE);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DOUBLE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DOUBLE);
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudSetVariableTyped_Double() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.doubleValue(VARIABLE_VALUE_DOUBLE));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DOUBLE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DOUBLE);
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudSetVariableUntyped_Double() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_DOUBLE));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DOUBLE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DOUBLE);
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudSetVariableTyped_NullDouble() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.doubleValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Double variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(DOUBLE);
  }

  @Test
  void shoudGetVariable_String() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.stringValue(VARIABLE_VALUE_STRING));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_STRING);
  }

  @Test
  void shoudGetVariableTyped_String() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.stringValue(VARIABLE_VALUE_STRING));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_STRING);
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudGetVariable_NullString() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.stringValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullString() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.stringValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudSetVariable_String() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_STRING);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_STRING);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_STRING);
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudSetVariableTyped_String() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.stringValue(VARIABLE_VALUE_STRING));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_STRING);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_STRING);
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudSetVariableUntyped_String() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_STRING));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_STRING);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_STRING);
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudSetVariableTyped_NullString() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.stringValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    String variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(STRING);
  }

  @Test
  void shoudGetVariable_Boolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.booleanValue(VARIABLE_VALUE_BOOLEAN));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isTrue();
  }

  @Test
  void shoudGetVariableTyped_Boolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.booleanValue(VARIABLE_VALUE_BOOLEAN));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat((Boolean) typedValue.getValue()).isTrue();
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudGetVariable_NullBoolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.booleanValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullBoolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.booleanValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudSetVariable_Boolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_BOOLEAN);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BOOLEAN);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BOOLEAN);
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudSetVariableTyped_Boolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.booleanValue(VARIABLE_VALUE_BOOLEAN));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BOOLEAN);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BOOLEAN);
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudSetVariableUntyped_Boolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_BOOLEAN));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BOOLEAN);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BOOLEAN);
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudSetVariableTyped_NullBoolean() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.booleanValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Boolean variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(BOOLEAN);
  }

  @Test
  void shoudGetVariable_Date() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.dateValue(VARIABLE_VALUE_DATE));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DATE);
  }

  @Test
  void shoudGetVariableTyped_Date() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.dateValue(VARIABLE_VALUE_DATE));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat((Date) typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DATE);
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudGetVariable_NullDate() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.dateValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullDate() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.dateValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudSetVariable_Date() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_DATE);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DATE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DATE);
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudSetVariableTyped_Date() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.dateValue(VARIABLE_VALUE_DATE));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DATE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DATE);
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudSetVariableUntyped_Date() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_DATE));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_DATE);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_DATE);
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudSetVariableTyped_NullDate() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.dateValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Date variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(DATE);
  }

  @Test
  void shoudGetVariable_Bytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.byteArrayValue(VARIABLE_VALUE_BYTES));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);
  }

  @Test
  void shoudGetVariableTyped_Bytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.byteArrayValue(VARIABLE_VALUE_BYTES));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudGetVariable_NullBytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.byteArrayValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_NullBytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.byteArrayValue(null));

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariable_Bytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE_BYTES);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariableTyped_Bytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.byteArrayValue(VARIABLE_VALUE_BYTES));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariableUntyped_Bytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(VARIABLE_VALUE_BYTES));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariableTyped_NullBytes() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.byteArrayValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariable_Bytes_InputStream() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    InputStream inputStream = new ByteArrayInputStream(VARIABLE_VALUE_STRING.getBytes());
    variables.put(VARIABLE_NAME, inputStream);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudSetVariableUntyped_Bytes_InputStream() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    InputStream inputStream = new ByteArrayInputStream(VARIABLE_VALUE_STRING.getBytes());
    variables.put(VARIABLE_NAME, Variables.untypedValue(inputStream));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    byte[] variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_BYTES);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(typedValue.getType()).isEqualTo(BYTES);
  }

  @Test
  void shoudGetVariable_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.untypedNullValue());

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Object variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudGetVariableTyped_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME, Variables.untypedNullValue());

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(NULL);
  }

  @Test
  void shoudSetVariable_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, null);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Object variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(NULL);
  }

  @Test
  void shoudSetVariableTyped_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedNullValue());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Object variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(NULL);
  }

  @Test
  void shoudSetVariableUntyped_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME, Variables.untypedValue(null));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Object variableValue = task.getVariable(VARIABLE_NAME);
    assertThat(variableValue).isNull();

    TypedValue typedValue = task.getVariableTyped(VARIABLE_NAME);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getType()).isEqualTo(NULL);
  }

  @Test
  void shoudGetAllVariable() {
    // given
    Map<String, TypedValue> variables = new HashMap<>();
    variables.put(VARIABLE_NAME_INT, Variables.integerValue(VARIABLE_VALUE_INT));
    variables.put(VARIABLE_NAME_LONG, Variables.longValue(VARIABLE_VALUE_LONG));
    variables.put(VARIABLE_NAME_SHORT, Variables.shortValue(VARIABLE_VALUE_SHORT));
    variables.put(VARIABLE_NAME_DOUBLE, Variables.doubleValue(VARIABLE_VALUE_DOUBLE));
    variables.put(VARIABLE_NAME_STRING, Variables.stringValue(VARIABLE_VALUE_STRING));
    variables.put(VARIABLE_NAME_BOOLEAN, Variables.booleanValue(VARIABLE_VALUE_BOOLEAN));
    variables.put(VARIABLE_NAME_DATE, Variables.dateValue(VARIABLE_VALUE_DATE));
    variables.put(VARIABLE_NAME_BYTES, Variables.byteArrayValue(VARIABLE_VALUE_BYTES));
    variables.put(VARIABLE_NAME_NULL, Variables.untypedNullValue());
    engineRule.startProcessInstance(processDefinition.getId(), variables);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    Map<String, Object> fetchedVariables = task.getAllVariables();
    assertThat(fetchedVariables)
      .hasSize(variables.size())
      .containsEntry(VARIABLE_NAME_INT, VARIABLE_VALUE_INT)
      .containsEntry(VARIABLE_NAME_LONG, VARIABLE_VALUE_LONG)
      .containsEntry(VARIABLE_NAME_SHORT, VARIABLE_VALUE_SHORT)
      .containsEntry(VARIABLE_NAME_DOUBLE, VARIABLE_VALUE_DOUBLE)
      .containsEntry(VARIABLE_NAME_STRING, VARIABLE_VALUE_STRING)
      .containsEntry(VARIABLE_NAME_BOOLEAN, VARIABLE_VALUE_BOOLEAN)
      .containsEntry(VARIABLE_NAME_DATE, VARIABLE_VALUE_DATE)
      .containsEntry(VARIABLE_NAME_BYTES, VARIABLE_VALUE_BYTES);
    assertThat(fetchedVariables.get(VARIABLE_NAME_NULL)).isNull();
  }

  @Test
  void shoudGetAllVariableTyped() {
    // given
    Map<String, TypedValue> variables = new HashMap<>();
    variables.put(VARIABLE_NAME_INT, Variables.integerValue(VARIABLE_VALUE_INT));
    variables.put(VARIABLE_NAME_LONG, Variables.longValue(VARIABLE_VALUE_LONG));
    variables.put(VARIABLE_NAME_SHORT, Variables.shortValue(VARIABLE_VALUE_SHORT));
    variables.put(VARIABLE_NAME_DOUBLE, Variables.doubleValue(VARIABLE_VALUE_DOUBLE));
    variables.put(VARIABLE_NAME_STRING, Variables.stringValue(VARIABLE_VALUE_STRING));
    variables.put(VARIABLE_NAME_BOOLEAN, Variables.booleanValue(VARIABLE_VALUE_BOOLEAN));
    variables.put(VARIABLE_NAME_DATE, Variables.dateValue(VARIABLE_VALUE_DATE));
    variables.put(VARIABLE_NAME_BYTES, Variables.byteArrayValue(VARIABLE_VALUE_BYTES));
    variables.put(VARIABLE_NAME_NULL, Variables.untypedNullValue());
    engineRule.startProcessInstance(processDefinition.getId(), variables);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    VariableMap fetchedVariables = task.getAllVariablesTyped();
    assertThat(fetchedVariables)
      .hasSize(variables.size())
      .containsEntry(VARIABLE_NAME_INT, VARIABLE_VALUE_INT)
      .containsEntry(VARIABLE_NAME_LONG, VARIABLE_VALUE_LONG)
      .containsEntry(VARIABLE_NAME_SHORT, VARIABLE_VALUE_SHORT)
      .containsEntry(VARIABLE_NAME_DOUBLE, VARIABLE_VALUE_DOUBLE)
      .containsEntry(VARIABLE_NAME_STRING, VARIABLE_VALUE_STRING)
      .containsEntry(VARIABLE_NAME_BOOLEAN, VARIABLE_VALUE_BOOLEAN)
      .containsEntry(VARIABLE_NAME_DATE, VARIABLE_VALUE_DATE)
      .containsEntry(VARIABLE_NAME_BYTES, VARIABLE_VALUE_BYTES);

    TypedValue intTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_INT);
    assertThat(intTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_INT);
    assertThat(intTypedValue.getType()).isEqualTo(INTEGER);

    TypedValue longTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_LONG);
    assertThat(longTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_LONG);
    assertThat(longTypedValue.getType()).isEqualTo(LONG);

    TypedValue shortTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_SHORT);
    assertThat(shortTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_SHORT);
    assertThat(shortTypedValue.getType()).isEqualTo(SHORT);

    TypedValue doubleTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_DOUBLE);
    assertThat(doubleTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_DOUBLE);
    assertThat(doubleTypedValue.getType()).isEqualTo(DOUBLE);

    TypedValue stringTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_STRING);
    assertThat(stringTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_STRING);
    assertThat(stringTypedValue.getType()).isEqualTo(STRING);

    TypedValue booleanTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_BOOLEAN);
    assertThat(booleanTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_BOOLEAN);
    assertThat(booleanTypedValue.getType()).isEqualTo(BOOLEAN);

    TypedValue dateTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_DATE);
    assertThat(dateTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_DATE);
    assertThat(dateTypedValue.getType()).isEqualTo(DATE);

    TypedValue bytesTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_BYTES);
    assertThat(bytesTypedValue.getValue()).isEqualTo(VARIABLE_VALUE_BYTES);
    assertThat(bytesTypedValue.getType()).isEqualTo(BYTES);

    TypedValue nullTypedValue = fetchedVariables.getValueTyped(VARIABLE_NAME_NULL);
    assertThat(nullTypedValue.getValue()).isNull();
    assertThat(nullTypedValue.getType()).isEqualTo(NULL);
  }

}
