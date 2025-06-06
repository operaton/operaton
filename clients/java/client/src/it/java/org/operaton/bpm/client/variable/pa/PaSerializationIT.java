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
package org.operaton.bpm.client.variable.pa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.dto.HistoricProcessInstanceDto;
import org.operaton.bpm.client.dto.ProcessInstanceDto;
import org.operaton.bpm.client.rule.ClientRule;
import org.operaton.bpm.client.rule.EngineRule;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskService;
import org.operaton.bpm.client.util.RecordingExternalTaskHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler.RecordedInvocation;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.qa.Bean;

import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.client.util.PropertyUtil.CAMUNDA_ENGINE_NAME;
import static org.operaton.bpm.client.util.PropertyUtil.CAMUNDA_ENGINE_REST;
import static org.operaton.bpm.client.util.PropertyUtil.DEFAULT_PROPERTIES_PATH;
import static org.operaton.bpm.client.util.PropertyUtil.loadProperties;

/**
 * @author Tassilo Weidner
 */
public class PaSerializationIT {

  protected static final String ENGINE_NAME = "/engine/another-engine";

  private static final Variables.SerializationDataFormats JSON_DATAFORMAT_NAME = Variables.SerializationDataFormats.JSON;
  private static final Variables.SerializationDataFormats XML_DATAFORMAT_NAME = Variables.SerializationDataFormats.XML;
  private static final Variables.SerializationDataFormats JAVA_DATAFORMAT_NAME = Variables.SerializationDataFormats.JAVA;

  private static final String VARIABLE_NAME = "bean";
  private static final Bean VARIABLE_VALUE_BEAN_FOO = new Bean("foo");
  private static final Bean VARIABLE_VALUE_BEAN_BAR = new Bean("bar");

  private static final String EXTERNAL_TASK_TOPIC_NAME = "qYeiKGuhqXGx3ate";
  private static final String PROCESS_DEFINITION_KEY = "KYsKNUbyVawGRt6H";

  @RegisterExtension
  static ClientRule clientRule = new ClientRule(() -> {
    Properties properties = loadProperties(DEFAULT_PROPERTIES_PATH);
    String baseUrl = properties.getProperty(CAMUNDA_ENGINE_REST) + ENGINE_NAME;
    return ExternalTaskClient.create()
      .baseUrl(baseUrl)
      .disableAutoFetching();
  });

  @RegisterExtension
  static EngineRule engineRule = new EngineRule(() -> {
    Properties properties = loadProperties(DEFAULT_PROPERTIES_PATH);
    properties.put(CAMUNDA_ENGINE_NAME, ENGINE_NAME);
    return properties;
  });

  protected ExternalTaskClient client;

  protected ProcessInstanceDto processInstance;

  protected RecordingExternalTaskHandler handler = new RecordingExternalTaskHandler();
  protected RecordingInvocationHandler invocationHandler = new RecordingInvocationHandler();

  @BeforeEach
  public void setup() {
    client = clientRule.client();

    processInstance = engineRule.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    handler.clear();
    invocationHandler.clear();
  }

  @AfterEach
  public void tearDown() {
    HistoricProcessInstanceDto historicProcessInstance = engineRule.getHistoricProcessInstanceById(processInstance.getId());
    if (historicProcessInstance.getEndTime() == null) {
      engineRule.deleteProcessInstance(processInstance.getId());
    }
  }

  @Test
  public void shouldSelectSequenceFlowAndCompleteProcessInstance_JsonSerialization() {
    // given
    ObjectValue objectValueJsonSerialization = Variables
      .objectValue(VARIABLE_VALUE_BEAN_FOO)
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

    client.subscribe(EXTERNAL_TASK_TOPIC_NAME)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTaskService service = invocation.getExternalTaskService();
    ExternalTask task = invocation.getExternalTask();

    // when
    service.complete(task, Collections.singletonMap(VARIABLE_NAME, objectValueJsonSerialization));

    // then
    HistoricProcessInstanceDto processInstanceDto = engineRule.getHistoricProcessInstanceById(processInstance.getId());
    assertThat(processInstanceDto.getEndTime()).isNotNull();
  }

  @Test
  public void shouldSelectSequenceFlowAndCompleteProcessInstance_XmlSerialization() {
    // given
    ObjectValue objectValueXmlSerialization = Variables
      .objectValue(VARIABLE_VALUE_BEAN_BAR)
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .create();

    client.subscribe(EXTERNAL_TASK_TOPIC_NAME)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTaskService service = invocation.getExternalTaskService();
    ExternalTask task = invocation.getExternalTask();

    // when
    service.complete(task, Collections.singletonMap(VARIABLE_NAME, objectValueXmlSerialization));

    // then
    HistoricProcessInstanceDto processInstanceDto = engineRule.getHistoricProcessInstanceById(processInstance.getId());
    assertThat(processInstanceDto.getEndTime()).isNotNull();
  }

  @Test
  public void shouldSelectSequenceFlowAndCompleteProcessInstance_JavaSerialization() {
    // given
    ObjectValue objectValueJavaSerialization = Variables
      .objectValue(VARIABLE_VALUE_BEAN_FOO)
      .serializationDataFormat(JAVA_DATAFORMAT_NAME)
      .create();

    client.subscribe(EXTERNAL_TASK_TOPIC_NAME)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTaskService service = invocation.getExternalTaskService();
    ExternalTask task = invocation.getExternalTask();

    // when
    service.complete(task, Collections.singletonMap(VARIABLE_NAME, objectValueJavaSerialization));

    // then
    HistoricProcessInstanceDto processInstanceDto = engineRule.getHistoricProcessInstanceById(processInstance.getId());
    assertThat(processInstanceDto.getEndTime()).isNotNull();
  }

}
