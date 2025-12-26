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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

class ConnectProcessEnginePluginTest {
  private static final String CONNECTOR_ID_MISSING = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorIdMissing.bpmn";
  private static final String CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptInputOutputMapping.bpmn";
  private static final String CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_RESOURCE_INPUT_OUTPUT_MAPPING = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorWithThrownExceptionInScriptResourceInputOutputMapping.bpmn";
  private static final String CONNECTOR_BPMN_ERROR_THROWN_IN_SCRIPT_RESOURCE_NO_ASYNC_AFTER_JOB_IS_CREATED = "org/operaton/connect/plugin/ConnectProcessEnginePluginTest.connectorBpmnErrorThrownInScriptResourceNoAsyncAfterJobIsCreated.bpmn";

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
    assertThat(http).isNotNull();
    Connector<?> soap = Connectors.getConnector(SoapHttpConnector.ID);
    assertThat(soap).isNotNull();
    Connector<?> test = Connectors.getConnector(TestConnector.ID);
    assertThat(test).isNotNull();
  }

  @Test
  void connectorIdMissing() {
    var deployment = repositoryService.createDeployment()
        .addClasspathResource(CONNECTOR_ID_MISSING);
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
    assertThat(TestConnector.requestParameters.get("reqParam1")).isNotNull();
    assertThat(TestConnector.requestParameters).containsEntry("reqParam1", inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertThat(variable).isNotNull();
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
    assertThat(in)
            .isNotNull()
            .isEqualTo(2 * x);

    // validate output parameter
    VariableInstance out = runtimeService.createVariableInstanceQuery().variableName("out").singleResult();
    assertThat(out).isNotNull();
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

  @Deployment(resources = CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING)
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

  @ParameterizedTest
  @CsvSource({
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING + ", in",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING + ", out",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_RESOURCE_INPUT_OUTPUT_MAPPING + ", in",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_RESOURCE_INPUT_OUTPUT_MAPPING + ", out"
  })
  void connectorRuntimeExceptionThrownInInputOutputMappingIsNotHandledByBoundaryEvent (String bpmnResource, String throwInMapping) {
    // given
    var deployment = repositoryService.createDeployment().addClasspathResource(bpmnResource).deploy();
    engineExtension.manageDeployment(deployment);

    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", throwInMapping);
    variables.put("exception", new RuntimeException(exceptionMessage));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @ParameterizedTest
  @CsvSource({
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING + ", in",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_INPUT_OUTPUT_MAPPING + ", out",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_RESOURCE_INPUT_OUTPUT_MAPPING + ", in",
    CONNECTOR_WITH_THROWN_EXCEPTION_IN_SCRIPT_RESOURCE_INPUT_OUTPUT_MAPPING + ", out"
  })
  void connectorBpmnErrorThrownInInputOutputMappingIsHandledByBoundaryEvent (String bpmnResource, String throwInMapping) {
    // given
    var deployment = repositoryService.createDeployment().addClasspathResource(bpmnResource).deploy();
    engineExtension.manageDeployment(deployment);

    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", throwInMapping);
    variables.put("exception", new BpmnError("error"));

    // when
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // then
    // we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = CONNECTOR_BPMN_ERROR_THROWN_IN_SCRIPT_RESOURCE_NO_ASYNC_AFTER_JOB_IS_CREATED)
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
    assertThat(TestConnector.requestParameters.get("reqParam1")).isNotNull();
    assertThat(TestConnector.requestParameters).containsEntry("reqParam1", inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertThat(variable).isNotNull();
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
    assertThat(TestConnector.requestParameters.get("reqParam1")).isNotNull();
    assertThat(TestConnector.requestParameters).containsEntry("reqParam1", inputVariableValue);

    // validate connector output
    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("out1").singleResult();
    assertThat(variable).isNotNull();
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
    assertThat(TestConnector.requestParameters.get("reqParam1")).isNotNull();
    assertThat(TestConnector.requestParameters).containsEntry("reqParam1", inputVariableValue);

    // validate connector output
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().variableName("out1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(outputParamValue);
  }

}
