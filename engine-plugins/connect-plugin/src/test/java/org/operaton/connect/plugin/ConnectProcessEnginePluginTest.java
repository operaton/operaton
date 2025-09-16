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
package org.operaton.connect.plugin;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.connect.ConnectorException;
import org.operaton.connect.Connectors;
import org.operaton.connect.httpclient.HttpConnector;
import org.operaton.connect.httpclient.soap.SoapHttpConnector;
import org.operaton.connect.plugin.util.TestConnector;
import org.operaton.connect.spi.Connector;

import static org.operaton.bpm.engine.impl.test.ProcessEngineAssert.assertProcessEnded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConnectProcessEnginePluginTest {

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
  private RepositoryService repositoryService;
  private RuntimeService runtimeService;
  private TaskService taskService;
  private ManagementService managementService;
  private HistoryService historyService;

  @BeforeEach
  void setUp() {
    TestConnector.responseParameters.clear();
    TestConnector.requestParameters = null;

    runtimeService = engineExtension.getRuntimeService();
    repositoryService = engineExtension.getRepositoryService();
    taskService = engineExtension.getTaskService();
    managementService = engineExtension.getManagementService();
    historyService = engineExtension.getHistoryService();
  }

  @Test
  void connectorsRegistered() {
    Connector<?> http = Connectors.getConnector(HttpConnector.ID);
    assertNotNull(http);
    Connector<?> soap = Connectors.getConnector(SoapHttpConnector.ID);
    assertNotNull(soap);
    Connector<?> test = Connectors.getConnector(TestConnector.ID);
    assertNotNull(test);
  }

  @Test
  void connectorIdMissing() {
    var deployment = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorIdMissing.bpmn");
    assertThatThrownBy(deployment::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .isNotInstanceOf(BpmnParseException.class);
  }

  @Deployment
  @Test
  void connectorIdUnknown() {
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(ConnectorException.class);
  }

  @Deployment
  @Test
  void connectorInvoked() {
    String outputParamValue = "someOutputValue";
    String inputVariableValue = "someInputVariableValue";

    TestConnector.responseParameters.put("someOutputParameter", outputParamValue);

    Map<String, Object> vars = new HashMap<>();
    vars.put("someInputVariable", inputVariableValue);
    runtimeService.startProcessInstanceByKey("testProcess", vars);

    // validate input parameter
    assertNotNull(TestConnector.requestParameters.get("reqParam1"));
    assertThat(TestConnector.requestParameters.get("reqParam1")).isEqualTo(inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo(outputParamValue);
  }

  @Deployment
  @Test
  void connectorWithScriptInputOutputMapping() {
    int x = 3;
    Map<String, Object> variables = new HashMap<>();
    variables.put("x", x);
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // validate input parameter
    Object in = TestConnector.requestParameters.get("in");
    assertNotNull(in);
    assertThat(in).isEqualTo(2 * x);

    // validate output parameter
    VariableInstance out = runtimeService.createVariableInstanceQuery().variableName("out").singleResult();
    assertNotNull(out);
    assertThat(out.getValue()).isEqualTo(3 * x);
  }


  @Deployment
  @Test
  void connectorWithSetVariableInOutputMapping() {
    // given process with set variable on connector in output mapping

    // when start process
    runtimeService.startProcessInstanceByKey("testProcess");

    // then variable x is set and no exception is thrown
    VariableInstance out = runtimeService.createVariableInstanceQuery().variableName("x").singleResult();
    assertThat(out.getValue()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptInputOutputMapping.bpmn")
  @Test
  void connectorBpmnErrorThrownInScriptInputMappingIsHandledByBoundaryEvent() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptInputOutputMapping.bpmn")
  @Test
  void connectorRuntimeExceptionThrownInScriptInputMappingIsNotHandledByBoundaryEvent() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new RuntimeException(exceptionMessage));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptInputOutputMapping.bpmn")
  @Test
  void connectorBpmnErrorThrownInScriptOutputMappingIsHandledByBoundaryEvent() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptInputOutputMapping.bpmn")
  @Test
  void connectorRuntimeExceptionThrownInScriptOutputMappingIsNotHandledByBoundaryEvent() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new RuntimeException(exceptionMessage));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptResourceInputOutputMapping.bpmn")
  @Test
  void connectorBpmnErrorThrownInScriptResourceInputMappingIsHandledByBoundaryEvent() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptResourceInputOutputMapping.bpmn")
  @Test
  void connectorRuntimeExceptionThrownInScriptResourceInputMappingIsNotHandledByBoundaryEvent() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new RuntimeException(exceptionMessage));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptResourceInputOutputMapping.bpmn")
  @Test
  void connectorBpmnErrorThrownInScriptResourceOutputMappingIsHandledByBoundaryEvent() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptResourceInputOutputMapping.bpmn")
  @Test
  void connectorRuntimeExceptionThrownInScriptResourceOutputMappingIsNotHandledByBoundaryEvent() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new RuntimeException(exceptionMessage));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @Deployment(resources = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorBpmnErrorThrownInScriptResourceNoAsyncAfterJobIsCreated.bpmn")
  @Test
  void connectorBpmnErrorThrownInScriptResourceNoAsyncAfterJobIsCreated() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new BpmnError("error"));

    // when
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // then
    // we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");

    // no job is created
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  void followingExceptionIsNotHandledByConnector() {
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Invalid format");
  }

  @Deployment
  @Test
  void sendTaskWithConnector() {
    String outputParamValue = "someSendTaskOutputValue";
    String inputVariableValue = "someSendTaskInputVariableValue";

    TestConnector.responseParameters.put("someOutputParameter", outputParamValue);

    Map<String, Object> vars = new HashMap<>();
    vars.put("someInputVariable", inputVariableValue);
    runtimeService.startProcessInstanceByKey("process_sending_with_connector", vars);

    // validate input parameter
    assertNotNull(TestConnector.requestParameters.get("reqParam1"));
    assertThat(TestConnector.requestParameters.get("reqParam1")).isEqualTo(inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo(outputParamValue);
  }

  @Deployment
  @Test
  void intermediateMessageThrowEventWithConnector() {
    String outputParamValue = "someMessageThrowOutputValue";
    String inputVariableValue = "someMessageThrowInputVariableValue";

    TestConnector.responseParameters.put("someOutputParameter", outputParamValue);

    Map<String, Object> vars = new HashMap<>();
    vars.put("someInputVariable", inputVariableValue);
    runtimeService.startProcessInstanceByKey("process_sending_with_connector", vars);

    // validate input parameter
    assertNotNull(TestConnector.requestParameters.get("reqParam1"));
    assertThat(TestConnector.requestParameters.get("reqParam1")).isEqualTo(inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo(outputParamValue);
  }

  @Deployment
  @Test
  void messageEndEventWithConnector() {
    String outputParamValue = "someMessageEndOutputValue";
    String inputVariableValue = "someMessageEndInputVariableValue";

    TestConnector.responseParameters.put("someOutputParameter", outputParamValue);

    Map<String, Object> vars = new HashMap<>();
    vars.put("someInputVariable", inputVariableValue);
    ProcessInstance processInstance = runtimeService
        .startProcessInstanceByKey("process_sending_with_connector", vars);
    assertProcessEnded(engineExtension.getProcessEngine(), processInstance.getId());

    // validate input parameter
    assertNotNull(TestConnector.requestParameters.get("reqParam1"));
    assertThat(TestConnector.requestParameters.get("reqParam1")).isEqualTo(inputVariableValue);

    // validate connector output
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().variableName("out1").singleResult();
    assertNotNull(variable);
    assertThat(variable.getValue()).isEqualTo(outputParamValue);
  }

}
