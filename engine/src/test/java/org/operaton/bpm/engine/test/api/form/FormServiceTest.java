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
package org.operaton.bpm.engine.test.api.form;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.form.FormField;
import org.operaton.bpm.engine.form.FormProperty;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.form.type.AbstractFormFieldType;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.form.deployment.FindOperatonFormDefinitionsCmd;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.BooleanValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.CollectionUtil;
import org.operaton.commons.utils.IoUtil;

import static java.lang.Boolean.TRUE;
import static org.operaton.bpm.engine.test.util.OperatonFormUtils.findAllOperatonFormDefinitionEntities;
import static org.operaton.bpm.engine.variable.Variables.booleanValue;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.operaton.bpm.engine.variable.Variables.stringValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Tom Baeyens
 * @author Falko Menge (operaton)
 */
class FormServiceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> configuration.setJavaSerializationFormatEnabled(true))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private RuntimeService runtimeService;
  private TaskService taskService;
  private RepositoryService repositoryService;
  private HistoryService historyService;
  private IdentityService identityService;
  private FormService formService;
  private CaseService caseService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void init() {
    identityService.saveUser(identityService.newUser("fozzie"));
    identityService.saveGroup(identityService.newGroup("management"));
    identityService.createMembership("fozzie", "management");
  }

  @AfterEach
  void tearDown() {
    identityService.deleteGroup("management");
    identityService.deleteUser("fozzie");

    VariablesRecordingListener.reset();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/approve.html",
      "org/operaton/bpm/engine/test/api/form/util/request.html",
      "org/operaton/bpm/engine/test/api/form/util/adjustRequest.html"})
  @Test
  void testGetStartFormByProcessDefinitionId() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions).hasSize(1);
    ProcessDefinition processDefinition = processDefinitions.get(0);

    Object startForm = formService.getRenderedStartForm(processDefinition.getId(), "juel");
    assertThat(startForm).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testGetStartFormByProcessDefinitionIdWithoutStartform() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions).hasSize(1);
    ProcessDefinition processDefinition = processDefinitions.get(0);

    Object startForm = formService.getRenderedStartForm(processDefinition.getId());
    assertThat(startForm).isNull();
  }

  @Test
  void testGetStartFormByKeyNullKey() {
    assertThatThrownBy(() -> formService.getRenderedStartForm(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testGetStartFormByIdNullId() {
    assertThatThrownBy(() -> formService.getRenderedStartForm(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testGetStartFormByIdUnexistingProcessDefinitionId() {
    assertThatThrownBy(() -> formService.getRenderedStartForm("unexistingId"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed process definition found with id");
  }

  @Test
  void testGetTaskFormNullTaskId() {
    assertThatThrownBy(() -> formService.getRenderedTaskForm(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testGetTaskFormUnexistingTaskId() {
    assertThatThrownBy(() -> formService.getRenderedTaskForm("unexistingtask"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Task 'unexistingtask' not found");
  }

  @Test
  @SuppressWarnings("deprecation")
  void testTaskFormPropertyDefaultsAndFormRendering() {

    final String deploymentId = testRule.deploy("org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html")
      .getId();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    StartFormData startForm = formService.getStartFormData(procDefId);
    assertThat(startForm).isNotNull();
    assertThat(startForm.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(startForm.getFormKey()).isEqualTo("org/operaton/bpm/engine/test/api/form/start.html");
    assertThat(startForm.getFormProperties()).isEqualTo(new ArrayList<FormProperty>());
    assertThat(startForm.getProcessDefinition().getId()).isEqualTo(procDefId);

    Object renderedStartForm = formService.getRenderedStartForm(procDefId, "juel");
    assertThat(renderedStartForm).isEqualTo("start form content");

    Map<String, String> properties = new HashMap<>();
    properties.put("room", "5b");
    properties.put("speaker", "Mike");
    String processInstanceId = formService.submitStartFormData(procDefId, properties).getId();

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("room", "5b");
    expectedVariables.put("speaker", "Mike");

    Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
    assertThat(variables).isEqualTo(expectedVariables);

    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    TaskFormData taskForm = formService.getTaskFormData(taskId);
    assertThat(taskForm.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(taskForm.getFormKey()).isEqualTo("org/operaton/bpm/engine/test/api/form/task.html");
    assertThat(taskForm.getFormProperties()).isEqualTo(new ArrayList<FormProperty>());
    assertThat(taskForm.getTask().getId()).isEqualTo(taskId);

    assertThat(formService.getRenderedTaskForm(taskId, "juel")).isEqualTo("Mike is speaking in room 5b");

    properties = new HashMap<>();
    properties.put("room", "3f");
    formService.submitTaskFormData(taskId, properties);

    expectedVariables = new HashMap<>();
    expectedVariables.put("room", "3f");
    expectedVariables.put("speaker", "Mike");

    variables = runtimeService.getVariables(processInstanceId);
    assertThat(variables).isEqualTo(expectedVariables);
  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testFormPropertyHandlingDeprecated() {
    Map<String, String> properties = new HashMap<>();
    properties.put("room", "5b"); // default
    properties.put("speaker", "Mike"); // variable name mapping
    properties.put("duration", "45"); // type conversion
    properties.put("free", "true"); // type conversion

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    String processInstanceId = formService.submitStartFormData(procDefId, properties).getId();

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("room", "5b");
    expectedVariables.put("SpeakerName", "Mike");
    expectedVariables.put("duration", 45L);
    expectedVariables.put("free", TRUE);

    Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
    assertThat(variables).isEqualTo(expectedVariables);

    Address address = new Address();
    address.setStreet("broadway");
    runtimeService.setVariable(processInstanceId, "address", address);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    List<FormProperty> formProperties = taskFormData.getFormProperties();
    FormProperty propertyRoom = formProperties.get(0);
    assertThat(propertyRoom.getId()).isEqualTo("room");
    assertThat(propertyRoom.getValue()).isEqualTo("5b");

    FormProperty propertyDuration = formProperties.get(1);
    assertThat(propertyDuration.getId()).isEqualTo("duration");
    assertThat(propertyDuration.getValue()).isEqualTo("45");

    FormProperty propertySpeaker = formProperties.get(2);
    assertThat(propertySpeaker.getId()).isEqualTo("speaker");
    assertThat(propertySpeaker.getValue()).isEqualTo("Mike");

    FormProperty propertyStreet = formProperties.get(3);
    assertThat(propertyStreet.getId()).isEqualTo("street");
    assertThat(propertyStreet.getValue()).isEqualTo("broadway");

    FormProperty propertyFree = formProperties.get(4);
    assertThat(propertyFree.getId()).isEqualTo("free");
    assertThat(propertyFree.getValue()).isEqualTo("true");

    assertThat(formProperties).hasSize(5);

    HashMap<String, String> emptyProperties = new HashMap<>();
    assertThatThrownBy(() -> formService.submitTaskFormData(taskId, emptyProperties))
      .withFailMessage("expected exception about required form property 'street'")
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("form property 'street' is required");

    properties = new HashMap<>();
    properties.put("speaker", "its not allowed to update speaker!");
    var finalProperties = properties;
    assertThatThrownBy(() -> formService.submitTaskFormData(taskId, finalProperties))
      .withFailMessage("expected exception about a non writable form property 'speaker'")
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("form property 'speaker' is not writable");

    properties = new HashMap<>();
    properties.put("street", "rubensstraat");
    formService.submitTaskFormData(taskId, properties);

    expectedVariables = new HashMap<>();
    expectedVariables.put("room", "5b");
    expectedVariables.put("SpeakerName", "Mike");
    expectedVariables.put("duration", 45L);
    expectedVariables.put("free", TRUE);

    variables = runtimeService.getVariables(processInstanceId);
    address = (Address) variables.remove("address");
    assertThat(address.getStreet()).isEqualTo("rubensstraat");
    assertThat(variables).isEqualTo(expectedVariables);
  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testFormPropertyHandling() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("room", "5b"); // default
    properties.put("speaker", "Mike"); // variable name mapping
    properties.put("duration", 45L); // type conversion
    properties.put("free", "true"); // type conversion

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    String processInstanceId = formService.submitStartForm(procDefId, properties).getId();

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("room", "5b");
    expectedVariables.put("SpeakerName", "Mike");
    expectedVariables.put("duration", 45L);
    expectedVariables.put("free", TRUE);

    Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
    assertThat(variables).isEqualTo(expectedVariables);

    Address address = new Address();
    address.setStreet("broadway");
    runtimeService.setVariable(processInstanceId, "address", address);

    String taskId = taskService.createTaskQuery().singleResult().getId();
    TaskFormData taskFormData = formService.getTaskFormData(taskId);

    List<FormProperty> formProperties = taskFormData.getFormProperties();
    FormProperty propertyRoom = formProperties.get(0);
    assertThat(propertyRoom.getId()).isEqualTo("room");
    assertThat(propertyRoom.getValue()).isEqualTo("5b");

    FormProperty propertyDuration = formProperties.get(1);
    assertThat(propertyDuration.getId()).isEqualTo("duration");
    assertThat(propertyDuration.getValue()).isEqualTo("45");

    FormProperty propertySpeaker = formProperties.get(2);
    assertThat(propertySpeaker.getId()).isEqualTo("speaker");
    assertThat(propertySpeaker.getValue()).isEqualTo("Mike");

    FormProperty propertyStreet = formProperties.get(3);
    assertThat(propertyStreet.getId()).isEqualTo("street");
    assertThat(propertyStreet.getValue()).isEqualTo("broadway");

    FormProperty propertyFree = formProperties.get(4);
    assertThat(propertyFree.getId()).isEqualTo("free");
    assertThat(propertyFree.getValue()).isEqualTo("true");

    assertThat(formProperties).hasSize(5);

    HashMap<String, Object> emptyProperties = new HashMap<>();
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, emptyProperties))
      .withFailMessage("expected exception about required form property 'street'")
      .isInstanceOf(ProcessEngineException.class);

    Map<String,Object> finalProperties = Maps.of("speaker", "its not allowed to update speaker!");

    assertThatThrownBy(() -> formService.submitTaskForm(taskId, finalProperties))
      .withFailMessage("expected exception about a non writable form property 'speaker'")
      .isInstanceOf(ProcessEngineException.class);

    properties = new HashMap<>();
    properties.put("street", "rubensstraat");
    formService.submitTaskForm(taskId, properties);

    expectedVariables = new HashMap<>();
    expectedVariables.put("room", "5b");
    expectedVariables.put("SpeakerName", "Mike");
    expectedVariables.put("duration", 45L);
    expectedVariables.put("free", TRUE);

    variables = runtimeService.getVariables(processInstanceId);
    address = (Address) variables.remove("address");
    assertThat(address.getStreet()).isEqualTo("rubensstraat");
    assertThat(variables).isEqualTo(expectedVariables);
  }

  @Deployment
  @Test
  @SuppressWarnings({"unchecked", "deprecation"})
  void testFormPropertyDetails() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    StartFormData startFormData = formService.getStartFormData(procDefId);
    FormProperty property = startFormData.getFormProperties().get(0);
    assertThat(property.getId()).isEqualTo("speaker");
    assertThat(property.getValue()).isNull();
    assertThat(property.isReadable()).isTrue();
    assertThat(property.isWritable()).isTrue();
    assertThat(property.isRequired()).isFalse();
    assertThat(property.getType().getName()).isEqualTo("string");

    property = startFormData.getFormProperties().get(1);
    assertThat(property.getId()).isEqualTo("start");
    assertThat(property.getValue()).isNull();
    assertThat(property.isReadable()).isTrue();
    assertThat(property.isWritable()).isTrue();
    assertThat(property.isRequired()).isFalse();
    assertThat(property.getType().getName()).isEqualTo("date");
    assertThat(property.getType().getInformation("datePattern")).isEqualTo("dd-MMM-yyyy");

    property = startFormData.getFormProperties().get(2);
    assertThat(property.getId()).isEqualTo("direction");
    assertThat(property.getValue()).isNull();
    assertThat(property.isReadable()).isTrue();
    assertThat(property.isWritable()).isTrue();
    assertThat(property.isRequired()).isFalse();
    assertThat(property.getType().getName()).isEqualTo("enum");
    Map<String, String> values = (Map<String, String>) property.getType().getInformation("values");

    Map<String, String> expectedValues = new LinkedHashMap<>();
    expectedValues.put("left", "Go Left");
    expectedValues.put("right", "Go Right");
    expectedValues.put("up", "Go Up");
    expectedValues.put("down", "Go Down");

    // ACT-1023: check if ordering is retained
    Iterator<Entry<String, String>> expectedValuesIterator = expectedValues.entrySet().iterator();
    for(Entry<String, String> entry : values.entrySet()) {
      Entry<String, String> expectedEntryAtLocation = expectedValuesIterator.next();
      assertThat(entry.getKey()).isEqualTo(expectedEntryAtLocation.getKey());
      assertThat(entry.getValue()).isEqualTo(expectedEntryAtLocation.getValue());
    }
    assertThat(values).isEqualTo(expectedValues);
  }

  @Deployment
  @Test
  void testInvalidFormKeyReference() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    assertThatThrownBy(() -> formService.getRenderedStartForm(processDefinitionId, "juel"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Form with formKey 'IDoNotExist' does not exist");
  }

  @Deployment
  @Test
  void testSubmitStartFormDataWithBusinessKey() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("duration", "45");
    properties.put("speaker", "Mike");
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstance processInstance = formService.submitStartForm(procDefId, "123", properties);
    assertThat(processInstance.getBusinessKey()).isEqualTo("123");

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").singleResult().getId()).isEqualTo(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml"})
  @Test
  void testSubmitStartFormDataTypedVariables() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    String stringValue = "some string";
    // Must be BASE64 encoded
    String serializedValue = Base64.getEncoder().encodeToString("some value".getBytes());

    ProcessInstance processInstance = formService.submitStartForm(procDefId,
        createVariables().putValueTyped("boolean", booleanValue(null))
            .putValueTyped("string", stringValue(stringValue))
            .putValueTyped("serializedObject", Variables.serializedObjectValue(serializedValue)
                .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
                .objectTypeName(String.class.getName())
                .create())
            .putValueTyped("object", objectValue(serializedValue).create()));

    VariableMap variables = runtimeService.getVariablesTyped(processInstance.getId(), false);
    assertThat(variables.<BooleanValue>getValueTyped("boolean")).isEqualTo(booleanValue(null));
    assertThat(variables.<StringValue>getValueTyped("string")).isEqualTo(stringValue(stringValue));
    assertThat(variables.<ObjectValue>getValueTyped("serializedObject").getValueSerialized()).isNotNull();
    assertThat(variables.<ObjectValue>getValueTyped("object").getValueSerialized()).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml"})
  @Test
  void testSubmitTaskFormDataTypedVariables() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstance processInstance = formService.submitStartForm(procDefId, createVariables());

    Task task = taskService.createTaskQuery().singleResult();

    String stringValue = "some string";
    String serializedValue = Base64.getEncoder().encodeToString("some value".getBytes());

    formService.submitTaskForm(task.getId(), createVariables()
        .putValueTyped("boolean", booleanValue(null))
        .putValueTyped("string", stringValue(stringValue))
        .putValueTyped("serializedObject", serializedObjectValue(serializedValue)
            .objectTypeName(String.class.getName())
            .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
            .create())
        .putValueTyped("object", objectValue(serializedValue).create()));

    VariableMap variables = runtimeService.getVariablesTyped(processInstance.getId(), false);
    assertThat(variables.<BooleanValue>getValueTyped("boolean")).isEqualTo(booleanValue(null));
    assertThat(variables.<StringValue>getValueTyped("string")).isEqualTo(stringValue(stringValue));
    assertThat(variables.<ObjectValue>getValueTyped("serializedObject").getValueSerialized()).isNotNull();
    assertThat(variables.<ObjectValue>getValueTyped("object").getValueSerialized()).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml"})
  @Test
  void testSubmitFormVariablesNull() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // assert that I can submit the start form with variables null
    formService.submitStartForm(procDefId, null);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // assert that I can submit the task form with variables null
    formService.submitTaskForm(task.getId(), null);
  }

  @Test
  void testSubmitTaskFormForStandaloneTask() {
    // given
    String id = "standaloneTask";
    Task task = taskService.newTask(id);
    taskService.saveTask(task);

    // when
    formService.submitTaskForm(task.getId(), Variables.createVariables().putValue("foo", "bar"));


    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {
      HistoricVariableInstance variableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .taskIdIn(id)
        .singleResult();

      assertThat(variableInstance).isNotNull();
      assertThat(variableInstance.getName()).isEqualTo("foo");
      assertThat(variableInstance.getValue()).isEqualTo("bar");
    }

    taskService.deleteTask(id, true);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSubmitTaskFormForCmmnHumanTask() {
    caseService.createCaseInstanceByKey("oneTaskCase");

    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();

    String stringValue = "some string";
    String serializedValue = Base64.getEncoder().encodeToString("some value".getBytes());

    VariableMap variableMap = createVariables()
      .putValueTyped("boolean", booleanValue(null))
      .putValueTyped("string", stringValue(stringValue))
      .putValueTyped("serializedObject", serializedObjectValue(serializedValue).objectTypeName(String.class.getName())
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
        .create())
      .putValueTyped("object", objectValue(serializedValue).create());

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task).isNotNull();

    // when
    formService.submitTaskForm(taskId, variableMap);

    // then
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task).isNull();
  }


  @Deployment
  @Test
  void testSubmitStartFormWithBusinessKey() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("duration", 45L);
    properties.put("speaker", "Mike");
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstance processInstance = formService.submitStartForm(procDefId, "123", properties);
    assertThat(processInstance.getBusinessKey()).isEqualTo("123");

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").singleResult().getId()).isEqualTo(processInstance.getId());
    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables)
            .containsEntry("SpeakerName", "Mike")
            .containsEntry("duration", 45L);
  }

  @Deployment
  @Test
  void testSubmitStartFormWithoutProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("duration", 45L);
    properties.put("speaker", "Mike");
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    ProcessInstance processInstance = formService.submitStartForm(procDefId, "123", properties);
    assertThat(processInstance.getBusinessKey()).isEqualTo("123");

    assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("123").singleResult().getId()).isEqualTo(processInstance.getId());
    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables)
            .containsEntry("speaker", "Mike")
            .containsEntry("duration", 45L);
  }

  @Test
  void testSubmitStartFormWithExecutionListenerOnStartEvent() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess()
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, VariablesRecordingListener.class)
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, VariablesRecordingListener.class)
        .endEvent()
        .done();

    testRule.deploy(modelInstance);
    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().singleResult();

    VariableMap formData = Variables.createVariables().putValue("foo", "bar");

    // when
    formService.submitStartForm(procDef.getId(), formData);

    // then
    List<VariableMap> variableEvents = VariablesRecordingListener.getVariableEvents();
    assertThat(variableEvents).hasSize(2);
    assertThat(variableEvents.get(0)).containsExactly(entry("foo", "bar"));
    assertThat(variableEvents.get(1)).containsExactly(entry("foo", "bar"));
  }


  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testSubmitStartFormWithAsyncStartEvent() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess()
        .operatonHistoryTimeToLive(180)
        .startEvent().operatonAsyncBefore()
        .endEvent()
        .done();

    testRule.deploy(modelInstance);
    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().singleResult();

    VariableMap formData = Variables.createVariables().putValue("foo", "bar");

    // when
    ProcessInstance processInstance = formService.submitStartForm(procDef.getId(), formData);

    // then
    VariableMap runtimeVariables = runtimeService.getVariablesTyped(processInstance.getId());
    assertThat(runtimeVariables).containsExactly(entry("foo", "bar"));

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getName()).isEqualTo("foo");
    assertThat(historicVariable.getValue()).isEqualTo("bar");
  }


  @Test
  void testSubmitStartFormWithAsyncStartEventExecuteJob() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess()
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .operatonAsyncBefore()
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(modelInstance);
    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery().singleResult();

    VariableMap formData = Variables.createVariables().putValue("foo", "bar");
    ProcessInstance processInstance = formService.submitStartForm(procDef.getId(), formData);

    ManagementService managementService = engineRule.getManagementService();
    Job job = managementService.createJobQuery().singleResult();

    // when
    managementService.executeJob(job.getId());

    // then the job can be executed successfully (e.g. we don't try to insert the variables a second time)
    // and
    VariableMap runtimeVariables = runtimeService.getVariablesTyped(processInstance.getId());
    assertThat(runtimeVariables).containsExactly(entry("foo", "bar"));
  }

  public static class VariablesRecordingListener implements ExecutionListener {

    private static List<VariableMap> variableEvents = new ArrayList<>();

    public static void reset() {
      variableEvents.clear();
    }

    public static List<VariableMap> getVariableEvents() {
      return variableEvents;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      variableEvents.add(execution.getVariablesTyped());
    }
  }

  @Test
  void testGetStartFormKeyEmptyArgument() {
    assertThatThrownBy(() -> formService.getStartFormKey(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The process definition id is mandatory, but 'null' has been provided.");

    assertThatThrownBy(() -> formService.getStartFormKey(""))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The process definition id is mandatory, but '' has been provided.");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml")
  @Test
  void testGetStartFormKey() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    String expectedFormKey = formService.getStartFormData(processDefinitionId).getFormKey();
    String actualFormKey = formService.getStartFormKey(processDefinitionId);
    assertThat(actualFormKey).isEqualTo(expectedFormKey);
  }

  @Test
  void testGetTaskFormKeyEmptyArguments() {
    assertThatThrownBy(() -> formService.getTaskFormKey(null, "23"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The process definition id is mandatory, but 'null' has been provided.");

    assertThatThrownBy(() -> formService.getTaskFormKey("", "23"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The process definition id is mandatory, but '' has been provided.");

    assertThatThrownBy(() -> formService.getTaskFormKey("42", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The task definition key is mandatory, but 'null' has been provided.");

    assertThatThrownBy(() -> formService.getTaskFormKey("42", ""))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The task definition key is mandatory, but '' has been provided.");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/form/FormsProcess.bpmn20.xml")
  @Test
  void testGetTaskFormKey() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    runtimeService.startProcessInstanceById(processDefinitionId);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    String expectedFormKey = formService.getTaskFormData(task.getId()).getFormKey();
    String actualFormKey = formService.getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
    assertThat(actualFormKey).isEqualTo(expectedFormKey);
  }

  @Deployment
  @Test
  void testGetTaskFormKeyWithExpression() {
    runtimeService.startProcessInstanceByKey("FormsProcess", CollectionUtil.singletonMap("dynamicKey", "test"));
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(formService.getTaskFormData(task.getId()).getFormKey()).isEqualTo("test");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormServiceTest.startFormFields.bpmn20.xml"})
  @Test
  void testGetStartFormVariables() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    VariableMap variables = formService.getStartFormVariables(processDefinition.getId());
    assertThat(variables)
            .hasSize(4)
            .containsEntry("stringField", "someString");
    assertThat(variables.getValueTyped("stringField").getValue()).isEqualTo("someString");
    assertThat(variables.getValueTyped("stringField").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables).containsEntry("longField", 5L);
    assertThat(variables.getValueTyped("longField").getValue()).isEqualTo(5L);
    assertThat(variables.getValueTyped("longField").getType()).isEqualTo(ValueType.LONG);

    assertThat(variables.get("customField")).isNull();
    assertThat(variables.getValueTyped("customField").getValue()).isNull();
    assertThat(variables.getValueTyped("customField").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables.get("dateField")).isNotNull();
    assertThat(variables.getValueTyped("dateField").getValue()).isEqualTo(variables.get("dateField"));
    assertThat(variables.getValueTyped("dateField").getType()).isEqualTo(ValueType.STRING);

    AbstractFormFieldType dateFormType = processEngineConfiguration.getFormTypes().getFormType("date");
    Date dateValue = (Date) dateFormType.convertToModelValue(variables.getValueTyped("dateField")).getValue();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateValue);
    assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(10);
    assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
    assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2013);

    // get restricted set of variables:
    variables = formService.getStartFormVariables(processDefinition.getId(), List.of("stringField"), true);
    assertThat(variables)
            .hasSize(1)
            .containsEntry("stringField", "someString");
    assertThat(variables.getValueTyped("stringField").getValue()).isEqualTo("someString");
    assertThat(variables.getValueTyped("stringField").getType()).isEqualTo(ValueType.STRING);

    // request non-existing variable
    variables = formService.getStartFormVariables(processDefinition.getId(), List.of("non-existing!"), true);
    assertThat(variables).isEmpty();

    // null => all
    variables = formService.getStartFormVariables(processDefinition.getId(), null, true);
    assertThat(variables).hasSize(4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormServiceTest.startFormFieldsUnknownType.bpmn20.xml"})
  @Test
  void testGetStartFormVariablesEnumType() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    VariableMap startFormVariables = formService.getStartFormVariables(processDefinition.getId());
    assertThat(startFormVariables).containsEntry("enumField", "a");
    assertThat(startFormVariables.getValueTyped("enumField").getType()).isEqualTo(ValueType.STRING);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/FormServiceTest.taskFormFields.bpmn20.xml"})
  @Test
  void testGetTaskFormVariables() {

    Map<String, Object> processVars = new HashMap<>();
    processVars.put("someString", "initialValue");
    processVars.put("initialBooleanVariable", true);
    processVars.put("initialLongVariable", 1L);
    processVars.put("serializable", List.of("a", "b", "c"));

    runtimeService.startProcessInstanceByKey("testProcess", processVars);

    Task task = taskService.createTaskQuery().singleResult();
    VariableMap variables = formService.getTaskFormVariables(task.getId());
    assertThat(variables)
            .hasSize(7)
            .containsEntry("stringField", "someString");
    assertThat(variables.getValueTyped("stringField").getValue()).isEqualTo("someString");
    assertThat(variables.getValueTyped("stringField").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables).containsEntry("longField", 5L);
    assertThat(variables.getValueTyped("longField").getValue()).isEqualTo(5L);
    assertThat(variables.getValueTyped("longField").getType()).isEqualTo(ValueType.LONG);

    assertThat(variables.get("customField")).isNull();
    assertThat(variables.getValueTyped("customField").getValue()).isNull();
    assertThat(variables.getValueTyped("customField").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables).containsEntry("someString", "initialValue");
    assertThat(variables.getValueTyped("someString").getValue()).isEqualTo("initialValue");
    assertThat(variables.getValueTyped("someString").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables).containsEntry("initialBooleanVariable", true);
    assertThat(variables.getValueTyped("initialBooleanVariable").getValue()).isEqualTo(TRUE);
    assertThat(variables.getValueTyped("initialBooleanVariable").getType()).isEqualTo(ValueType.BOOLEAN);

    assertThat(variables).containsEntry("initialLongVariable", 1L);
    assertThat(variables.getValueTyped("initialLongVariable").getValue()).isEqualTo(1L);
    assertThat(variables.getValueTyped("initialLongVariable").getType()).isEqualTo(ValueType.LONG);

    assertThat(variables.get("serializable")).isNotNull();

    // override the long variable
    taskService.setVariableLocal(task.getId(), "initialLongVariable", 2L);

    variables = formService.getTaskFormVariables(task.getId());
    assertThat(variables)
            .hasSize(7)
            .containsEntry("initialLongVariable", 2L);
    assertThat(variables.getValueTyped("initialLongVariable").getValue()).isEqualTo(2L);
    assertThat(variables.getValueTyped("initialLongVariable").getType()).isEqualTo(ValueType.LONG);

    // get restricted set of variables (form field):
    variables = formService.getTaskFormVariables(task.getId(), List.of("someString"), true);
    assertThat(variables)
            .hasSize(1)
            .containsEntry("someString", "initialValue");
    assertThat(variables.getValueTyped("someString").getValue()).isEqualTo("initialValue");
    assertThat(variables.getValueTyped("someString").getType()).isEqualTo(ValueType.STRING);

    // get restricted set of variables (process variable):
    variables = formService.getTaskFormVariables(task.getId(), List.of("initialBooleanVariable"), true);
    assertThat(variables)
            .hasSize(1)
            .containsEntry("initialBooleanVariable", true);
    assertThat(variables.getValueTyped("initialBooleanVariable").getValue()).isEqualTo(TRUE);
    assertThat(variables.getValueTyped("initialBooleanVariable").getType()).isEqualTo(ValueType.BOOLEAN);

    // request non-existing variable
    variables = formService.getTaskFormVariables(task.getId(), List.of("non-existing!"), true);
    assertThat(variables).isEmpty();

    // null => all
    variables = formService.getTaskFormVariables(task.getId(), null, true);
    assertThat(variables).hasSize(7);

  }

  @Test
  void testGetTaskFormVariables_StandaloneTask() {

    Map<String, Object> processVars = new HashMap<>();
    processVars.put("someString", "initialValue");
    processVars.put("initialBooleanVariable", true);
    processVars.put("initialLongVariable", 1L);
    processVars.put("serializable", List.of("a", "b", "c"));

    // create new standalone task
    Task standaloneTask = taskService.newTask();
    standaloneTask.setName("A Standalone Task");
    taskService.saveTask(standaloneTask);

    Task task = taskService.createTaskQuery().singleResult();

    // set variables
    taskService.setVariables(task.getId(), processVars);

    VariableMap variables = formService.getTaskFormVariables(task.getId());
    assertThat(variables)
            .hasSize(4)
            .containsEntry("someString", "initialValue");
    assertThat(variables.getValueTyped("someString").getValue()).isEqualTo("initialValue");
    assertThat(variables.getValueTyped("someString").getType()).isEqualTo(ValueType.STRING);

    assertThat(variables).containsEntry("initialBooleanVariable", true);
    assertThat(variables.getValueTyped("initialBooleanVariable").getValue()).isEqualTo(TRUE);
    assertThat(variables.getValueTyped("initialBooleanVariable").getType()).isEqualTo(ValueType.BOOLEAN);

    assertThat(variables).containsEntry("initialLongVariable", 1L);
    assertThat(variables.getValueTyped("initialLongVariable").getValue()).isEqualTo(1L);
    assertThat(variables.getValueTyped("initialLongVariable").getType()).isEqualTo(ValueType.LONG);

    assertThat(variables.get("serializable")).isNotNull();

    // override the long variable
    taskService.setVariable(task.getId(), "initialLongVariable", 2L);

    variables = formService.getTaskFormVariables(task.getId());
    assertThat(variables)
            .hasSize(4)
            .containsEntry("initialLongVariable", 2L);
    assertThat(variables.getValueTyped("initialLongVariable").getValue()).isEqualTo(2L);
    assertThat(variables.getValueTyped("initialLongVariable").getType()).isEqualTo(ValueType.LONG);

    // get restricted set of variables
    variables = formService.getTaskFormVariables(task.getId(), List.of("someString"), true);
    assertThat(variables)
            .hasSize(1)
            .containsEntry("someString", "initialValue");
    assertThat(variables.getValueTyped("someString").getValue()).isEqualTo("initialValue");
    assertThat(variables.getValueTyped("someString").getType()).isEqualTo(ValueType.STRING);

    // request non-existing variable
    variables = formService.getTaskFormVariables(task.getId(), List.of("non-existing!"), true);
    assertThat(variables).isEmpty();

    // null => all
    variables = formService.getTaskFormVariables(task.getId(), null, true);
    assertThat(variables).hasSize(4);

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  @SuppressWarnings("unchecked")
  void testSubmitStartFormWithObjectVariables() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when a start form is submitted with an object variable
    Map<String, Object> variables = new HashMap<>();
    variables.put("var", new ArrayList<String>());
    ProcessInstance processInstance = formService.submitStartForm(processDefinition.getId(), variables);

    // then the variable is available as a process variable
    ArrayList<String> variable = (ArrayList<String>) runtimeService.getVariable(processInstance.getId(), "var");
    assertThat(variable).isNotNull().isEmpty();

    // then no historic form property event has been written since this is not supported for custom objects
    if(processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL) {
      assertThat(historyService.createHistoricDetailQuery().formFields().count()).isZero();
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  @SuppressWarnings("unchecked")
  void testSubmitTaskFormWithObjectVariables() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition).isNotNull();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

    // when a task form is submitted with an object variable
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    Map<String, Object> variables = new HashMap<>();
    variables.put("var", new ArrayList<String>());
    formService.submitTaskForm(task.getId(), variables);

    // then the variable is available as a process variable
    ArrayList<String> variable = (ArrayList<String>) runtimeService.getVariable(processInstance.getId(), "var");
    assertThat(variable).isNotNull().isEmpty();

    // then no historic form property event has been written since this is not supported for custom objects
    if(processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL) {
      assertThat(historyService.createHistoricDetailQuery().formFields().count()).isZero();
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskServiceTest.testCompleteTaskWithVariablesInReturn.bpmn20.xml"})
  @Test
  void testSubmitTaskFormWithVariablesInReturn() {
    String processVarName = "processVar";
    String processVarValue = "processVarValue";

    String taskVarName = "taskVar";
    String taskVarValue = "taskVarValue";

    Map<String, Object> variables = new HashMap<>();
    variables.put(processVarName, processVarValue);

    runtimeService.startProcessInstanceByKey("TaskServiceTest.testCompleteTaskWithVariablesInReturn", variables);

    Task firstUserTask = taskService.createTaskQuery().taskName("First User Task").singleResult();
    taskService.setVariable(firstUserTask.getId(), "x", 1);

    Map<String, Object> additionalVariables = new HashMap<>();
    additionalVariables.put(taskVarName, taskVarValue);

    // After completion of firstUserTask a script Task sets 'x' = 5
    VariableMap vars = formService.submitTaskFormWithVariablesInReturn(firstUserTask.getId(), additionalVariables, true);
    assertThat(vars)
            .hasSize(3)
            .containsEntry("x", 5);
    assertThat(vars.getValueTyped("x").getType()).isEqualTo(ValueType.INTEGER);
    assertThat(vars).containsEntry(processVarName, processVarValue);
    assertThat(vars.getValueTyped(processVarName).getType()).isEqualTo(ValueType.STRING);
    assertThat(vars).containsEntry(taskVarName, taskVarValue);

    additionalVariables = new HashMap<>();
    additionalVariables.put("x", 7);
    Task secondUserTask = taskService.createTaskQuery().taskName("Second User Task").singleResult();
    vars = formService.submitTaskFormWithVariablesInReturn(secondUserTask.getId(), additionalVariables, true);
    assertThat(vars)
            .hasSize(3)
            .containsEntry("x", 7)
            .containsEntry(processVarName, processVarValue)
            .containsEntry(taskVarName, taskVarValue);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoParallelTasksProcess.bpmn20.xml"})
  @Test
  void testSubmitTaskFormWithVariablesInReturnParallel() {
    String processVarName = "processVar";
    String processVarValue = "processVarValue";

    String task1VarName = "taskVar1";
    String task2VarName = "taskVar2";
    String task1VarValue = "taskVarValue1";
    String task2VarValue = "taskVarValue2";

    String additionalVar = "additionalVar";
    String additionalVarValue = "additionalVarValue";

    Map<String, Object> variables = new HashMap<>();
    variables.put(processVarName, processVarValue);
    runtimeService.startProcessInstanceByKey("twoParallelTasksProcess", variables);

    Task firstTask = taskService.createTaskQuery().taskName("First Task").singleResult();
    taskService.setVariable(firstTask.getId(), task1VarName, task1VarValue);
    Task secondTask = taskService.createTaskQuery().taskName("Second Task").singleResult();
    taskService.setVariable(secondTask.getId(), task2VarName, task2VarValue);

    Map<String, Object> vars = formService.submitTaskFormWithVariablesInReturn(firstTask.getId(), null, true);

    assertThat(vars)
            .hasSize(3)
            .containsEntry(processVarName, processVarValue)
            .containsEntry(task1VarName, task1VarValue)
            .containsEntry(task2VarName, task2VarValue);

    Map<String, Object> additionalVariables = new HashMap<>();
    additionalVariables.put(additionalVar, additionalVarValue);

    vars = formService.submitTaskFormWithVariablesInReturn(secondTask.getId(), additionalVariables, true);
    assertThat(vars)
            .hasSize(4)
            .containsEntry(processVarName, processVarValue)
            .containsEntry(task1VarName, task1VarValue)
            .containsEntry(task2VarName, task2VarValue)
            .containsEntry(additionalVar, additionalVarValue);
  }

  /**
   * Tests that the variablesInReturn logic is not applied
   * when we call the regular complete API. This is a performance optimization.
   * Loading all variables may be expensive.
   */
  @Test
  void testSubmitTaskFormAndDoNotDeserializeVariables()
  {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .subProcess()
        .embeddedSubProcess()
        .startEvent()
        .userTask("task1")
        .userTask("task2")
        .endEvent()
        .subProcessDone()
        .endEvent()
        .done();

    testRule.deploy(process);

    runtimeService.startProcessInstanceByKey("process", Variables.putValue("var", "val"));

    final Task task = taskService.createTaskQuery().singleResult();

    // when
    final boolean hasLoadedAnyVariables =
      processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
        formService.submitTaskForm(task.getId(), null);
        return !commandContext.getDbEntityManager().getCachedEntitiesByType(VariableInstanceEntity.class).isEmpty();
      });

    // then
    assertThat(hasLoadedAnyVariables).isFalse();
  }


  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml")
  void testSubmitTaskFormWithVariablesInReturnShouldDeserializeObjectValue()
  {
    // given
    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);

    Task task = taskService.createTaskQuery().singleResult();

    // when
    VariableMap result = formService.submitTaskFormWithVariablesInReturn(task.getId(), null, true);

    // then
    ObjectValue returnedValue = result.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isTrue();
    assertThat(returnedValue.getValue()).isEqualTo("value");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml")
  void testSubmitTaskFormWithVariablesInReturnShouldNotDeserializeObjectValue()
  {
    // given
    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);
    String serializedValue = ((ObjectValue) runtimeService.getVariableTyped(instance.getId(), "var")).getValueSerialized();

    Task task = taskService.createTaskQuery().singleResult();

    // when
    VariableMap result = formService.submitTaskFormWithVariablesInReturn(task.getId(), null, false);

    // then
    ObjectValue returnedValue = result.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isFalse();
    assertThat(returnedValue.getValueSerialized()).isEqualTo(serializedValue);
  }

  @Deployment
  @Test
  void testSubmitTaskFormContainingReadonlyVariable() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    formService.submitTaskForm(task.getId(), new HashMap<>());

    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment
  @Test
  void testGetTaskFormWithoutLabels() {
    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();

    // form data can be retrieved
    TaskFormData formData = formService.getTaskFormData(task.getId());

    List<FormField> formFields = formData.getFormFields();
    assertThat(formFields).hasSize(3);

    List<String> formFieldIds = new ArrayList<>();
    for (FormField field : formFields) {
      assertThat(field.getLabel()).isNull();
      formFieldIds.add(field.getId());
    }

    assertThat(formFieldIds).containsAll(List.of("stringField", "customField", "longField"));

    // the form can be rendered
    Object startForm = formService.getRenderedTaskForm(task.getId());
    assertThat(startForm).isNotNull();
  }

  @Test
  void testDeployTaskFormWithoutFieldTypes() {
    var deploymentBuilder = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/form/FormServiceTest.testDeployTaskFormWithoutFieldTypes.bpmn20.xml");

    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("form field must have a 'type' attribute");
  }

  @Deployment
  @Test
  void testGetStartFormWithoutLabels() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());

    // form data can be retrieved
    StartFormData formData = formService.getStartFormData(processDefinition.getId());

    List<FormField> formFields = formData.getFormFields();
    assertThat(formFields).hasSize(3);

    List<String> formFieldIds = new ArrayList<>();
    for (FormField field : formFields) {
      assertThat(field.getLabel()).isNull();
      formFieldIds.add(field.getId());
    }

    assertThat(formFieldIds).containsAll(List.of("stringField", "customField", "longField"));

    // the form can be rendered
    Object startForm = formService.getRenderedStartForm(processDefinition.getId());
    assertThat(startForm).isNotNull();
  }

  @Test
  void testDeployStartFormWithoutFieldTypes() {
    // when
    var deploymentBuilder = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/form/FormServiceTest.testDeployStartFormWithoutFieldTypes.bpmn20.xml");
    // when
    assertThatThrownBy(deploymentBuilder::deploy)
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("form field must have a 'type' attribute");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/util/VacationRequest_deprecated_forms.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/util/approve.html",
      "org/operaton/bpm/engine/test/api/form/util/request.html",
      "org/operaton/bpm/engine/test/api/form/util/adjustRequest.html"})
  @Test
  @SuppressWarnings("deprecation")
  void testTaskFormsWithVacationRequestProcess() {

    // Get start form
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    Object startForm = formService.getRenderedStartForm(procDefId, "juel");
    assertThat(startForm).isNotNull();

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String processDefinitionId = processDefinition.getId();
    assertThat(formService.getStartFormData(processDefinitionId).getFormKey()).isEqualTo("org/operaton/bpm/engine/test/api/form/util/request.html");

    // Define variables that would be filled in through the form
    Map<String, String> formProperties = new HashMap<>();
    formProperties.put("employeeName", "kermit");
    formProperties.put("numberOfDays", "4");
    formProperties.put("vacationMotivation", "I'm tired");
    formService.submitStartFormData(procDefId, formProperties);

    // Management should now have a task assigned to them
    Task task = taskService.createTaskQuery().taskCandidateGroup("management").singleResult();
    assertThat(task.getDescription()).isEqualTo("Vacation request by kermit");
    Object taskForm = formService.getRenderedTaskForm(task.getId(), "juel");
    assertThat(taskForm).isNotNull();

    // Rejecting the task should put the process back to first task
    taskService.complete(task.getId(), CollectionUtil.singletonMap("vacationApproved", "false"));
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Adjust vacation request");
  }

  @Deployment
  @Test
  void testTaskFormUnavailable() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    assertThat(formService.getRenderedStartForm(procDefId)).isNull();

    runtimeService.startProcessInstanceByKey("noStartOrTaskForm");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(formService.getRenderedTaskForm(task.getId())).isNull();
  }

  @Deployment
  @Test
  void testBusinessKey() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    StartFormData startFormData = formService.getStartFormData(procDefId);

    // then
    FormField formField = startFormData.getFormFields().get(0);
    assertThat(formField.isBusinessKey()).isTrue();
  }

  @Deployment
  @Test
  void testSubmitStartFormWithFormFieldMarkedAsBusinessKey() {
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    ProcessInstance pi = formService.submitStartForm(procDefId, "foo", Variables.createVariables().putValue("secondParam", "bar"));

    assertThat(pi.getBusinessKey()).isEqualTo("foo");

    List<VariableInstance> result = runtimeService.createVariableInstanceQuery().list();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("secondParam");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedStartForm() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    InputStream deployedStartForm = formService.getDeployedStartForm(procDefId);

    // then
    assertThat(deployedStartForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/start.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedStartForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/EmbeddedDeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetEmbeddedDeployedStartForm() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    InputStream deployedStartForm = formService.getDeployedStartForm(procDefId);

    // then
    assertThat(deployedStartForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/start.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedStartForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedOperatonFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedOperatonStartForm() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    InputStream deployedStartForm = formService.getDeployedStartForm(procDefId);

    // then
    assertThat(deployedStartForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/start.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedStartForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedCamundaFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedCamundaStartForm() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    InputStream deployedStartForm = formService.getDeployedStartForm(procDefId);

    // then
    assertThat(deployedStartForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/start.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedStartForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Test
  void testGetDeployedStartFormWithNullProcDefId() {
    assertThatThrownBy(() -> formService.getDeployedStartForm(null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Process definition id cannot be null: processDefinitionId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedTaskForm() {
    // given
    runtimeService.startProcessInstanceByKey("FormsProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    InputStream deployedTaskForm = formService.getDeployedTaskForm(taskId);

    // then
    assertThat(deployedTaskForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/task.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedTaskForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedFormsCase.cmmn11.xml",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedTaskForm_Case() {
    // given
    caseService.createCaseInstanceByKey("Case_1");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    InputStream deployedTaskForm = formService.getDeployedTaskForm(taskId);

    // then
    assertThat(deployedTaskForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/task.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedTaskForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/EmbeddedDeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetEmbeddedDeployedTaskForm() {
    // given
    runtimeService.startProcessInstanceByKey("FormsProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    InputStream deployedTaskForm = formService.getDeployedTaskForm(taskId);

    // then
    assertThat(deployedTaskForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/task.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedTaskForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedOperatonFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedOperatonTaskForm() {
    // given
    runtimeService.startProcessInstanceByKey("FormsProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    InputStream deployedTaskForm = formService.getDeployedTaskForm(taskId);

    // then
    assertThat(deployedTaskForm).isNotNull();
    String fileAsString = IoUtil.fileAsString("org/operaton/bpm/engine/test/api/form/task.html");
    String deployedStartFormAsString = IoUtil.inputStreamAsString(deployedTaskForm);
    assertThat(fileAsString).isEqualTo(deployedStartFormAsString);
  }

  @Test
  void testGetDeployedTaskFormWithNullTaskId() {
    assertThatThrownBy(() -> formService.getDeployedTaskForm(null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Task id cannot be null: taskId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/task.html"})
  @Test
  void testGetDeployedStartForm_DeploymentNotFound() {
    // given
    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedStartForm(procDefId))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("The form with the resource name 'org/operaton/bpm/engine/test/api/form/start.html' cannot be found in deployment");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/form/DeployedFormsProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/form/start.html"})
  @Test
  void testGetDeployedTaskForm_DeploymentNotFound() {
    // given
    runtimeService.startProcessInstanceByKey("FormsProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("The form with the resource name 'org/operaton/bpm/engine/test/api/form/task.html' cannot be found in deployment");
  }

  @Test
  void testGetDeployedStartForm_FormKeyNotSet() {
    // given
    testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("One of the attributes 'formKey' and 'operaton:formRef' must be supplied but none were set.");
  }

  @Test
  void testGetDeployedTaskForm_FormKeyNotSet() {
    // given
    testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    runtimeService.startProcessInstanceByKey("Process");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessage("One of the attributes 'formKey' and 'operaton:formRef' must be supplied but none were set.");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.testGetDeployedStartFormWithWrongKeyFormat.bpmn20.xml"})
  @Test
  void testGetDeployedStartFormWithWrongKeyFormat() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedStartForm(processDefinitionId))
    // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The form key 'formKey' does not reference a deployed form.");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.testGetDeployedTaskFormWithWrongKeyFormat.bpmn20.xml"})
  @Test
  void testGetDeployedTaskFormWithWrongKeyFormat() {
    runtimeService.startProcessInstanceByKey("FormsProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    assertThatThrownBy(() -> formService.getDeployedTaskForm(taskId))
    // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The form key 'formKey' does not reference a deployed form.");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.shouldSubmitStartFormUsingFormKeyAndOperatonFormDefinition.bpmn",
      "org/operaton/bpm/engine/test/api/form/start.form"})
  @Test
  void shouldSubmitStartFormUsingFormKeyAndOperatonFormDefinition() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("OperatonStartFormProcess").singleResult();

    // when
    ProcessInstance processInstance = formService.submitStartForm(processDefinition.getId(),
        Variables.createVariables());

    // then
    assertThat(repositoryService.createDeploymentQuery().list()).hasSize(1);
    assertThat(findAllOperatonFormDefinitionEntities(processEngineConfiguration)).hasSize(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.shouldSubmitTaskFormUsingFormKeyAndOperatonFormDefinition.bpmn",
      "org/operaton/bpm/engine/test/api/form/task.form"})
  @Test
  void shouldSubmitTaskFormUsingFormKeyAndOperatonFormDefinition() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("OperatonTaskFormProcess");

    // when
    Task task = taskService.createTaskQuery().singleResult();
    formService.submitTaskForm(task.getId(), Variables.createVariables().putValue("variable", "my variable"));

    // then
    assertThat(repositoryService.createDeploymentQuery().list()).hasSize(1);
    assertThat(findAllOperatonFormDefinitionEntities(processEngineConfiguration)).hasSize(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
    assertThat(taskService.createTaskQuery().list()).isEmpty();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.shouldSubmitStartFormUsingFormRefAndOperatonFormDefinition.bpmn",
      "org/operaton/bpm/engine/test/api/form/start.form"})
  @Test
  void shouldSubmitStartFormUsingFormRefAndOperatonFormDefinition() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("OperatonStartFormProcess").singleResult();

    // when
    ProcessInstance processInstance = formService.submitStartForm(processDefinition.getId(),
        Variables.createVariables());

    // then
    assertThat(repositoryService.createDeploymentQuery().list()).hasSize(1);
    assertThat(engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
        .execute(new FindOperatonFormDefinitionsCmd())).hasSize(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/form/FormServiceTest.shouldSubmitTaskFormUsingFormRefAndOperatonFormDefinition.bpmn",
      "org/operaton/bpm/engine/test/api/form/task.form"})
  @Test
  void shouldSubmitTaskFormUsingFormRefAndOperatonFormDefinition() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("OperatonTaskFormProcess");

    // when
    Task task = taskService.createTaskQuery().singleResult();
    formService.submitTaskForm(task.getId(), Variables.createVariables().putValue("variable", "my variable"));

    // then
    assertThat(repositoryService.createDeploymentQuery().list()).hasSize(1);
    assertThat(engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
        .execute(new FindOperatonFormDefinitionsCmd())).hasSize(1);
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
    assertThat(taskService.createTaskQuery().list()).isEmpty();
  }
}
