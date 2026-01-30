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
package org.operaton.bpm.engine.test.history;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricFormField;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.util.CustomSerializable;
import org.operaton.bpm.engine.test.api.runtime.util.FailingSerializable;
import org.operaton.bpm.engine.test.cmmn.decisiontask.TestPojo;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.propertyComparator;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Christian Lipphardt (Camunda)
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoricVariableInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;
  CaseService caseService;
  RepositoryService repositoryService;
  FormService formService;

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/orderProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/checkCreditProcess.bpmn20.xml"
  })
  @Test
  void testOrderProcessWithCallActivity() {
    // After the process has started, the 'verify credit history' task should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task verifyCreditTask = taskQuery.singleResult();
    assertThat(verifyCreditTask.getName()).isEqualTo("Verify credit history");

    // Verify with Query API
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(pi.getId());

    // Completing the task with approval, will end the subprocess and continue the original process
    taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
    Task prepareAndShipTask = taskQuery.singleResult();
    assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");
  }

  @Deployment
  @Test
  void testSimple() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task userTask = taskQuery.singleResult();
    assertThat(userTask.getName()).isEqualTo("userTask1");

    taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);

    HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
    assertThat(historicVariable.getTextValue()).isEqualTo("test456");

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(5);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(3);
    }
  }

  @Deployment
  @Test
  void testSimpleNoWaitState() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);

    HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
    assertThat(historicVariable.getTextValue()).isEqualTo("test456");

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(4);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
    }
  }

  @Deployment
  @Test
  void testParallel() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task userTask = taskQuery.singleResult();
    assertThat(userTask.getName()).isEqualTo("userTask1");

    taskService.complete(userTask.getId(), CollectionUtil.singletonMap("myVar", "test789"));

    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().list();
    assertThat(variables).hasSize(2);

    HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
    assertThat(historicVariable.getName()).isEqualTo("myVar");
    assertThat(historicVariable.getTextValue()).isEqualTo("test789");

    HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
    assertThat(historicVariable1.getName()).isEqualTo("myVar1");
    assertThat(historicVariable1.getTextValue()).isEqualTo("test456");

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(8);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(5);
    }
  }

  @Deployment
  @Test
  void testParallelNoWaitState() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).hasSize(1);

    HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
    assertThat(historicVariable.getTextValue()).isEqualTo("test456");

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(7);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
    }
  }

  @Deployment
  @Test
  void testTwoSubProcessInParallelWithinSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcessInParallelWithinSubProcess");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().list();
    assertThat(variables).hasSize(2);

    HistoricVariableInstanceEntity historicVariable = (HistoricVariableInstanceEntity) variables.get(0);
    assertThat(historicVariable.getName()).isEqualTo("myVar");
    assertThat(historicVariable.getTextValue()).isEqualTo("test101112");
    assertThat(historicVariable.getTypeName()).isEqualTo("string");

    HistoricVariableInstanceEntity historicVariable1 = (HistoricVariableInstanceEntity) variables.get(1);
    assertThat(historicVariable1.getName()).isEqualTo("myVar1");
    assertThat(historicVariable1.getTextValue()).isEqualTo("test789");
    assertThat(historicVariable1.getTypeName()).isEqualTo("string");

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(18);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(7);
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testCallSimpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testHistoricVariableInstanceQuery() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    testRule.assertProcessEnded(processInstance.getId());

    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().list()).hasSize(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().count()).isEqualTo(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableName().asc().list()).hasSize(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByTenantId().asc().count()).isEqualTo(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByTenantId().asc().list()).hasSize(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableId().asc().count()).isEqualTo(5);
    assertThat(historyService.createHistoricVariableInstanceQuery().orderByVariableId().asc().list()).hasSize(5);

    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("myVar").count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("myVar").list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("myVar1").list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("my\\_Var%").count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableNameLike("my\\_Var%").list()).hasSize(1);
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variables).hasSize(5);

    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test123").list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test456").list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar", "test666").list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").count()).isOne();
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myVar1", "test666").list()).hasSize(1);

    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(8);

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(6);
    }

    // non-existing id:
    assertThat(historyService.createHistoricVariableInstanceQuery().variableId("non-existing").count()).isZero();

    // existing-id
    List<HistoricVariableInstance> variable = historyService.createHistoricVariableInstanceQuery().listPage(0, 1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableId(variable.get(0).getId()).count()).isOne();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testCallSubProcessSettingVariableOnStart.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/subProcessSetVariableOnStart.bpmn20.xml"
  })
  @Test
  void testCallSubProcessSettingVariableOnStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSubProcess");
    testRule.assertProcessEnded(processInstance.getId());

    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();

    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("aVariable", "aValue").count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testHistoricProcessVariableOnDeletion() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("testVar", "Hallo Christian");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    runtimeService.deleteProcessInstance(processInstance.getId(), "deleted");
    testRule.assertProcessEnded(processInstance.getId());

    // check that process variable is set even if the process is canceled and not ended normally
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableValueEquals("testVar", "Hallo Christian").count()).isOne();
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/history/FullHistoryTest.testVariableUpdatesAreLinkedToActivity.bpmn20.xml"})
  @Test
  void testVariableUpdatesLinkedToActivity() {
    if (isFullHistoryEnabled()) {
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("ProcessWithSubProcess");

      Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
      Map<String, Object> variables = new HashMap<>();
      variables.put("test", "1");
      taskService.complete(task.getId(), variables);

      // now we are in the subprocess
      task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
      variables.clear();
      variables.put("test", "2");
      taskService.complete(task.getId(), variables);

      // now we are ended
      testRule.assertProcessEnded(pi.getId());

      // check history
      List<HistoricDetail> updates = historyService.createHistoricDetailQuery().variableUpdates().list();
      assertThat(updates).hasSize(2);

      Map<String, HistoricVariableUpdate> updatesMap = new HashMap<>();
      HistoricVariableUpdate update = (HistoricVariableUpdate) updates.get(0);
      updatesMap.put((String) update.getValue(), update);
      update = (HistoricVariableUpdate) updates.get(1);
      updatesMap.put((String) update.getValue(), update);

      HistoricVariableUpdate update1 = updatesMap.get("1");
      HistoricVariableUpdate update2 = updatesMap.get("2");

      assertThat(update1.getActivityInstanceId()).isNotNull();
      assertThat(update1.getExecutionId()).isNotNull();
      HistoricActivityInstance historicActivityInstance1 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update1.getActivityInstanceId()).singleResult();
      assertThat(update1.getExecutionId()).isEqualTo(historicActivityInstance1.getExecutionId());
      assertThat(historicActivityInstance1.getActivityId()).isEqualTo("usertask1");

      assertThat(update2.getActivityInstanceId()).isNotNull();
      HistoricActivityInstance historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery().activityInstanceId(update2.getActivityInstanceId()).singleResult();
      assertThat(historicActivityInstance2.getActivityId()).isEqualTo("usertask2");

      /*
       * This is OK! The variable is set on the root execution, on a execution never run through the activity, where the process instances
       * stands when calling the set Variable. But the ActivityId of this flow node is used. So the execution id's doesn't have to be equal.
       *
       * execution id: On which execution it was set
       * activity id: in which activity was the process instance when setting the variable
       */
      assertThat(update2.getExecutionId()).isNotEqualTo(historicActivityInstance2.getExecutionId());
    }
  }

  // Test for ACT-1528, which (correctly) reported that deleting any
  // historic process instance would remove ALL historic variables.
  // Yes. Real serious bug.
  @Deployment
  @Test
  void testHistoricProcessInstanceDeleteCascadesCorrectly() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("var1", "value1");
    variables.put("var2", "value2");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess", variables);
    assertThat(processInstance).isNotNull();

    variables = new HashMap<>();
    variables.put("var3", "value3");
    variables.put("var4", "value4");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("myProcess", variables);
    assertThat(processInstance2).isNotNull();

    // check variables
    long count = historyService.createHistoricVariableInstanceQuery().count();
    assertThat(count).isEqualTo(4);

    // delete runtime execution of ONE process instance
    runtimeService.deleteProcessInstance(processInstance.getId(), "reason 1");
    historyService.deleteHistoricProcessInstance(processInstance.getId());

    // recheck variables
    // this is a bug: all variables was deleted after delete a history processinstance
    count = historyService.createHistoricVariableInstanceQuery().count();
    assertThat(count).isEqualTo(2);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testParallel.bpmn20.xml"})
  @Test
  void testHistoricVariableInstanceQueryByTaskIds() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc");

    TaskQuery taskQuery = taskService.createTaskQuery();
    Task userTask = taskQuery.singleResult();
    assertThat(userTask.getName()).isEqualTo("userTask1");

    // set local variable on user task
    taskService.setVariableLocal(userTask.getId(), "taskVariable", "aCustomValue");

    // complete user task to finish process instance
    taskService.complete(userTask.getId());

    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
    assertThat(tasks).hasSize(1);

    // check existing variables
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(3);

    // check existing variables for task ID
    assertThat(historyService.createHistoricVariableInstanceQuery().taskIdIn(tasks.get(0).getId()).list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().taskIdIn(tasks.get(0).getId()).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testParallel.bpmn20.xml"})
  @Test
  void testHistoricVariableInstanceQueryByProcessIdIn() {
    // given
    Map<String, Object> vars = new HashMap<>();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc",vars);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("myProc",vars);

    // check existing variables for process instance ID
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceIdIn(processInstance.getProcessInstanceId(), processInstance2.getProcessInstanceId()).count()).isEqualTo(4);
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceIdIn(processInstance.getProcessInstanceId(), processInstance2.getProcessInstanceId()).list()).hasSize(4);

    //add check with not existing search
    String notExistingSearch = processInstance.getProcessInstanceId() + "-notExisting";
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceIdIn(notExistingSearch, processInstance2.getProcessInstanceId()).count()).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testParallel.bpmn20.xml"})
  @Test
  void testHistoricVariableInstanceQueryByInvalidProcessIdIn() {
    // given
    Map<String, Object> vars = new HashMap<>();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProc",vars);
    var historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
    var processInstanceId = processInstance.getProcessInstanceId();

    // check existing variables for task ID
    assertThatThrownBy(() -> historicVariableInstanceQuery.processInstanceIdIn(processInstanceId, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> historicVariableInstanceQuery.processInstanceIdIn(null, processInstanceId)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testHistoricVariableInstanceQueryByExecutionIds() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("myVar", "test123");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().executionIdIn(processInstance1.getId());
    assertThat(query.count()).isEqualTo(2);
    List<HistoricVariableInstance> variableInstances = query.list();
    assertThat(variableInstances).hasSize(2);
    for (HistoricVariableInstance variableInstance : variableInstances) {
      assertThat(variableInstance.getExecutionId()).isEqualTo(processInstance1.getId());
    }

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("myVar", "test123");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables2);

    query = historyService.createHistoricVariableInstanceQuery().executionIdIn(processInstance1.getId(), processInstance2.getId());
    assertThat(query.list()).hasSize(3);
    assertThat(query.count()).isEqualTo(3);
  }

  @Test
  void testQueryByInvalidExecutionIdIn() {
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().executionIdIn("invalid");
    assertThat(query.count()).isZero();
    var historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();

    assertThatThrownBy(() -> historicVariableInstanceQuery.executionIdIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> historicVariableInstanceQuery.executionIdIn((String) null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByInvalidTaskIdIn() {
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().taskIdIn("invalid");
    assertThat(query.count()).isZero();
    var historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();

    assertThatThrownBy(() -> historicVariableInstanceQuery.taskIdIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> historicVariableInstanceQuery.taskIdIn((String) null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByActivityInstanceIdIn() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("myVar", "test123");
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    query.activityInstanceIdIn(processInstance1.getId());

    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    Map<String, Object> variables2 = new HashMap<>();
    variables2.put("myVar", "test123");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables2);

    query.activityInstanceIdIn(processInstance1.getId(), processInstance2.getId());

    assertThat(query.list()).hasSize(3);
    assertThat(query.count()).isEqualTo(3);
  }

  @Test
  void testQueryByInvalidActivityInstanceIdIn() {
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    query.taskIdIn("invalid");
    assertThat(query.count()).isZero();

    assertThatThrownBy(() -> query.taskIdIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.taskIdIn((String) null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByVariableTypeIn() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery().variableTypeIn("string");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
    assertThat(query.list().get(0).getName()).isEqualTo("stringVar");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByVariableTypeInWithCapitalLetter() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    query.variableTypeIn("Boolean");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
    assertThat(query.list().get(0).getName()).isEqualTo("boolVar");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByVariableTypeInWithSeveralTypes() {
    // given
    Map<String, Object> variables1 = new HashMap<>();
    variables1.put("stringVar", "test");
    variables1.put("boolVar", true);
    variables1.put("intVar", 5);
    variables1.put("nullVar", null);
    variables1.put("pojoVar", new TestPojo("str", .0));
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables1);

    // when
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    query.variableTypeIn("BooLEAN", "string", "Serializable");

    // then
    assertThat(query.list()).hasSize(3);
    assertThat(query.count()).isEqualTo(3);
  }

  @Test
  void testQueryByInvalidVariableTypeIn() {
    // given
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    // when
    query.variableTypeIn("invalid");

    // then
    assertThat(query.count()).isZero();

    assertThatThrownBy(() -> query.variableTypeIn((String[]) null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> query.variableTypeIn((String) null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testBinaryFetchingEnabled() {

    // by default, binary fetching is enabled

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "binaryVariableName";
    taskService.setVariable(newTask.getId(), variableName, "some bytes".getBytes());

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
      .variableName(variableName)
      .singleResult();

    assertThat(variableInstance.getValue()).isNotNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testBinaryFetchingDisabled() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "binaryVariableName";
    taskService.setVariable(newTask.getId(), variableName, "some bytes".getBytes());

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
      .variableName(variableName)
      .disableBinaryFetching()
      .singleResult();

    assertThat(variableInstance.getValue()).isNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
  @Test
  void testDisableBinaryFetchingForFileValues() {
    // given
    String fileName = "text.txt";
    String encoding = "crazy-encoding";
    String mimeType = "martini/dry";

    FileValue fileValue = Variables
        .fileValue(fileName)
        .file("ABC".getBytes())
        .encoding(encoding)
        .mimeType(mimeType)
        .create();

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValueTyped("fileVar", fileValue));

    // when enabling binary fetching
    HistoricVariableInstance fileVariableInstance =
        historyService.createHistoricVariableInstanceQuery().singleResult();

    // then the binary value is accessible
    assertThat(fileVariableInstance.getValue()).isNotNull();

    // when disabling binary fetching
    fileVariableInstance =
        historyService.createHistoricVariableInstanceQuery().disableBinaryFetching().singleResult();

    // then the byte value is not fetched
    assertThat(fileVariableInstance).isNotNull();
    assertThat(fileVariableInstance.getName()).isEqualTo("fileVar");

    assertThat(fileVariableInstance.getValue()).isNull();

    FileValue typedValue = (FileValue) fileVariableInstance.getTypedValue();
    assertThat(typedValue.getValue()).isNull();

    // but typed value metadata is accessible
    assertThat(typedValue.getType()).isEqualTo(ValueType.FILE);
    assertThat(typedValue.getFilename()).isEqualTo(fileName);
    assertThat(typedValue.getEncoding()).isEqualTo(encoding);
    assertThat(typedValue.getMimeType()).isEqualTo(mimeType);

  }

  @Test
  void testDisableCustomObjectDeserialization() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    Map<String, Object> variables = new HashMap<>();
    variables.put("customSerializable", new CustomSerializable());
    variables.put("failingSerializable", new FailingSerializable());
    taskService.setVariables(newTask.getId(), variables);

    // when
    List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
      .disableCustomObjectDeserialization()
      .list();

    // then
    assertThat(variableInstances).hasSize(2);

    for (HistoricVariableInstance variableInstance : variableInstances) {
      assertThat(variableInstance.getErrorMessage()).isNull();

      ObjectValue typedValue = (ObjectValue) variableInstance.getTypedValue();
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.isDeserialized()).isFalse();
      // cannot access the deserialized value
      assertThatThrownBy(typedValue::getValue)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Object is not deserialized");
      assertThat(typedValue.getValueSerialized()).isNotNull();
    }

    taskService.deleteTask(newTask.getId(), true);

  }

  @Test
  void testDisableCustomObjectDeserializationNativeQuery() {
    // given
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    Map<String, Object> variables = new HashMap<>();
    variables.put("customSerializable", new CustomSerializable());
    variables.put("failingSerializable", new FailingSerializable());
    taskService.setVariables(newTask.getId(), variables);

    // when
    List<HistoricVariableInstance> variableInstances = historyService.createNativeHistoricVariableInstanceQuery()
      .sql("SELECT * from " + managementService.getTableName(HistoricVariableInstance.class))
      .disableCustomObjectDeserialization()
      .list();

    // then
    assertThat(variableInstances).hasSize(2);

    for (HistoricVariableInstance variableInstance : variableInstances) {
      assertThat(variableInstance.getErrorMessage()).isNull();

      ObjectValue typedValue = (ObjectValue) variableInstance.getTypedValue();
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.isDeserialized()).isFalse();
      // cannot access the deserialized value
      assertThatThrownBy(typedValue::getValue)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Object is not deserialized");
      assertThat(typedValue.getValueSerialized()).isNotNull();
    }

    taskService.deleteTask(newTask.getId(), true);
  }

  @Test
  void testErrorMessage() {

    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);

    String variableName = "failingSerializable";
    taskService.setVariable(newTask.getId(), variableName, new FailingSerializable());

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
      .variableName(variableName)
      .singleResult();

    assertThat(variableInstance.getValue()).isNull();
    assertThat(variableInstance.getErrorMessage()).isNotNull();

    taskService.deleteTask(newTask.getId(), true);
  }

  @Deployment
  @Test
  void testHistoricVariableInstanceRevision() {
    // given:
    // a finished process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    testRule.assertProcessEnded(processInstance.getId());

    // when

    // then
    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();

    assertThat(variable).isNotNull();

    HistoricVariableInstanceEntity variableEntity = (HistoricVariableInstanceEntity) variable;

    // the revision has to be 0
    assertThat(variableEntity.getRevision()).isZero();

    if (isFullHistoryEnabled()) {
      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .orderByVariableRevision()
        .asc()
        .list();

      for (HistoricDetail detail : details) {
        HistoricVariableUpdate variableDetail = (HistoricVariableUpdate) detail;
        assertThat(variableDetail.getRevision()).isZero();
      }
    }
  }

  @Deployment
  @Test
  void testHistoricVariableInstanceRevisionAsync() {
    // given:
    // a finished process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    testRule.executeAvailableJobs();

    // then
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();

    assertThat(variable).isNotNull();

    HistoricVariableInstanceEntity variableEntity = (HistoricVariableInstanceEntity) variable;

    // the revision has to be 2
    assertThat(variableEntity.getRevision()).isEqualTo(2);

    if (isFullHistoryEnabled()) {
      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .orderByVariableRevision()
        .asc()
        .list();

      int i = 0;
      for (HistoricDetail detail : details) {
        HistoricVariableUpdate variableDetail = (HistoricVariableUpdate) detail;
        assertThat(variableDetail.getRevision()).isEqualTo(i);
        i++;
      }
    }

  }

  /**
   * CAM-3442
   */
  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  void testImplicitVariableUpdate() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new UpdateValueDelegate()));

    List<String> list = (List<String>) runtimeService.getVariable(instance.getId(), "listVar");
    assertThat(list)
            .isNotNull()
            .hasSize(1);
    assertThat(list.get(0)).isEqualTo(UpdateValueDelegate.NEW_ELEMENT);

    HistoricVariableInstance historicVariableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("listVar").singleResult();

    List<String> historicList = (List<String>) historicVariableInstance.getValue();
    assertThat(historicList)
            .isNotNull()
            .hasSize(1);
    assertThat(historicList.get(0)).isEqualTo(UpdateValueDelegate.NEW_ELEMENT);

    if (isFullHistoryEnabled()) {
      List<HistoricDetail> historicDetails = historyService
          .createHistoricDetailQuery()
          .variableUpdates()
          .variableInstanceId(historicVariableInstance.getId())
          .orderPartiallyByOccurrence().asc()
          .list();

      assertThat(historicDetails).hasSize(2);

      HistoricVariableUpdate update1 = (HistoricVariableUpdate) historicDetails.get(0);
      HistoricVariableUpdate update2 = (HistoricVariableUpdate) historicDetails.get(1);

      List<String> value1 = (List<String>) update1.getValue();

      assertThat(value1).isNotNull();
      assertThat(value1).isEmpty();

      List<String> value2 = (List<String>) update2.getValue();

      assertThat(value2)
              .isNotNull()
              .hasSize(1);
      assertThat(value2.get(0)).isEqualTo(UpdateValueDelegate.NEW_ELEMENT);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testImplicitVariableUpdate.bpmn20.xml")
  @Disabled("Historic variable's activity is not the historicServiceTask")
  @Test
  void testImplicitVariableUpdateActivityInstanceId() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new UpdateValueDelegate()));

    HistoricActivityInstance historicServiceTask = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("task")
        .singleResult();

    @SuppressWarnings("unchecked")
    List<String> list = (List<String>) runtimeService.getVariable(instance.getId(), "listVar");
    assertThat(list)
            .isNotNull()
            .hasSize(1);
    assertThat(list.get(0)).isEqualTo(UpdateValueDelegate.NEW_ELEMENT);

    // when
    HistoricVariableInstance historicVariableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("listVar").singleResult();

    // then
    assertThat(historicVariableInstance.getActivityInstanceId()).isEqualTo(historicServiceTask.getId());
  }

  @SuppressWarnings("unchecked")
  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testImplicitVariableUpdate.bpmn20.xml")
  @Disabled("historicDetails has just 2 instead of 3 expected entries")
  @Test
  void testImplicitVariableUpdateAndReplacementInOneTransaction() {
    // given
    runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new UpdateAndReplaceValueDelegate()));

    HistoricVariableInstance historicVariableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("listVar").singleResult();

    List<String> historicList = (List<String>) historicVariableInstance.getValue();
    assertThat(historicList).isEmpty();

    if (isFullHistoryEnabled()) {
      List<HistoricDetail> historicDetails = historyService
          .createHistoricDetailQuery()
          .variableUpdates()
          .variableInstanceId(historicVariableInstance.getId())
          .orderPartiallyByOccurrence().asc()
          .list();

      assertThat(historicDetails).hasSize(3);

      HistoricVariableUpdate update1 = (HistoricVariableUpdate) historicDetails.get(0);
      HistoricVariableUpdate update2 = (HistoricVariableUpdate) historicDetails.get(1);
      HistoricVariableUpdate update3 = (HistoricVariableUpdate) historicDetails.get(2);

      List<String> value1 = (List<String>) update1.getValue();

      assertThat(value1).isNotNull();
      assertThat(value1).isEmpty();

      List<String> value2 = (List<String>) update2.getValue();

      assertThat(value2)
              .isNotNull()
              .hasSize(1);
      assertThat(value2.get(0)).isEqualTo(UpdateValueDelegate.NEW_ELEMENT);

      List<String> value3 = (List<String>) update3.getValue();

      assertThat(value3).isNotNull();
      assertThat(value3).isEmpty();
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testImplicitVariableUpdateAndScopeDestroyedInOneTransaction() {
   testRule.deploy(Bpmn.createExecutableProcess("process1")
      .startEvent("start")
      .serviceTask("task1").operatonExpression("${var.setValue(\"newValue\")}")
      .endEvent("end")
      .done());

    processEngine.getRuntimeService().startProcessInstanceByKey("process1", Variables.createVariables().putValue("var", new CustomVar("initialValue")));

    final HistoricVariableInstance historicVariableInstance = processEngine.getHistoryService().createHistoricVariableInstanceQuery().list().get(0);
    CustomVar customVar = (CustomVar) historicVariableInstance.getTypedValue().getValue();

    assertThat(customVar.getValue()).isEqualTo("newValue");

    final List<HistoricDetail> historicDetails = processEngine.getHistoryService().createHistoricDetailQuery().orderPartiallyByOccurrence().desc().list();
    HistoricDetail historicDetail = historicDetails.get(0);
    final CustomVar typedValue = (CustomVar) ((HistoricVariableUpdate) historicDetail).getTypedValue().getValue();
    assertThat(typedValue.getValue()).isEqualTo("newValue");
  }

  public static class CustomVar implements Serializable {
    private String value;

    public CustomVar(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @SuppressWarnings("unchecked")
  @Deployment
  @Test
  void testNoImplicitUpdateOnHistoricValues() {
    //given
    runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new UpdateHistoricValueDelegate()));

    // a task before the delegate ensures that the variables have actually been persisted
    // and can be fetched by querying
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // then
    HistoricVariableInstance historicVariableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("listVar").singleResult();

    List<String> historicList = (List<String>) historicVariableInstance.getValue();
    assertThat(historicList).isEmpty();

    if (isFullHistoryEnabled()) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);

      List<HistoricDetail> historicDetails = historyService
          .createHistoricDetailQuery()
          .variableUpdates()
          .variableInstanceId(historicVariableInstance.getId())
          .list();

      assertThat(historicDetails).hasSize(1);

      HistoricVariableUpdate update1 = (HistoricVariableUpdate) historicDetails.get(0);

      List<String> value1 = (List<String>) update1.getValue();

      assertThat(value1).isNotNull();
      assertThat(value1).isEmpty();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testImplicitVariableUpdate.bpmn20.xml")
  @Test
  void testImplicitVariableRemoveAndUpdateInOneTransaction() {
    // given
    runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new RemoveAndUpdateValueDelegate()));

    if (isFullHistoryEnabled()) {
      List<HistoricDetail> historicDetails = historyService
          .createHistoricDetailQuery()
          .variableUpdates()
          .orderPartiallyByOccurrence().asc()
          .list();

      historicDetails.removeIf(historicDetail -> !"listVar".equals(((HistoricVariableUpdate) historicDetail).getVariableName()));

      // one for creation, one for deletion, none for update
      assertThat(historicDetails).hasSize(2);

      HistoricVariableUpdate update1 = (HistoricVariableUpdate) historicDetails.get(0);

      @SuppressWarnings("unchecked")
      List<String> value1 = (List<String>) update1.getValue();

      assertThat(value1).isNotNull();
      assertThat(value1).isEmpty();

      HistoricVariableUpdate update2 = (HistoricVariableUpdate) historicDetails.get(1);
      assertThat(update2.getValue()).isNull();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testNoImplicitUpdateOnHistoricValues.bpmn20.xml")
  @Test
  void testNoImplicitUpdateOnHistoricDetailValues() {
    if (!isFullHistoryEnabled()) {
      return;
    }

    // given
    runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValue("listVar", new ArrayList<String>())
          .putValue("delegate", new UpdateHistoricDetailValueDelegate()));

    // a task before the delegate ensures that the variables have actually been persisted
    // and can be fetched by querying
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // then
    HistoricVariableInstance historicVariableInstance = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("listVar").singleResult();

    // One for "listvar", one for "delegate"
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);

    List<HistoricDetail> historicDetails = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(historicVariableInstance.getId())
        .list();

    assertThat(historicDetails).hasSize(1);

    HistoricVariableUpdate update1 = (HistoricVariableUpdate) historicDetails.get(0);

    @SuppressWarnings("unchecked")
    List<String> value1 = (List<String>) update1.getValue();

    assertThat(value1).isEmpty();
  }

  protected boolean isFullHistoryEnabled() {
    return processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testHistoricVariableInstanceRevision.bpmn20.xml"})
  @Test
  void testVariableUpdateOrder() {
    // given:
    // a finished process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    testRule.assertProcessEnded(processInstance.getId());

    // when

    // then
    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variable.getId())
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(3);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testHistoricVariableInstanceRevisionAsync.bpmn20.xml"})
  @Test
  void testVariableUpdateOrderAsync() {
    // given:
    // a finished process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    testRule.executeAvailableJobs();

    // then
    testRule.assertProcessEnded(processInstance.getId());

    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variable.getId())
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(3);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testTaskVariableUpdateOrder() {
    // given:
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when (1)
    taskService.setVariableLocal(taskId, "myVariable", 1);
    taskService.setVariableLocal(taskId, "myVariable", 2);
    taskService.setVariableLocal(taskId, "myVariable", 3);

    // then (1)
    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    String variableInstanceId = variable.getId();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(3);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());
    }

    // when (2)
    taskService.setVariableLocal(taskId, "myVariable", "abc");

    // then (2)
    variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(4);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());

      HistoricVariableUpdate fourthUpdate = (HistoricVariableUpdate) details.get(3);
      assertThat(fourthUpdate.getValue()).isEqualTo("abc");
      assertThat(((HistoryEvent) fourthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) thirdUpdate).getSequenceCounter());
    }

    // when (3)
    taskService.removeVariable(taskId, "myVariable");

    // then (3)
    variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(5);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());

      HistoricVariableUpdate fourthUpdate = (HistoricVariableUpdate) details.get(3);
      assertThat(fourthUpdate.getValue()).isEqualTo("abc");
      assertThat(((HistoryEvent) fourthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) thirdUpdate).getSequenceCounter());

      HistoricVariableUpdate fifthUpdate = (HistoricVariableUpdate) details.get(4);
      assertThat(fifthUpdate.getValue()).isNull();
      assertThat(((HistoryEvent) fifthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) fourthUpdate).getSequenceCounter());
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCaseVariableUpdateOrder() {
    // given:
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when (1)
    caseService.setVariable(caseInstanceId, "myVariable", 1);
    caseService.setVariable(caseInstanceId, "myVariable", 2);
    caseService.setVariable(caseInstanceId, "myVariable", 3);

    // then (1)
    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    String variableInstanceId = variable.getId();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(3);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());
    }

    // when (2)
    caseService.setVariable(caseInstanceId, "myVariable", "abc");

    // then (2)
    variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(4);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());

      HistoricVariableUpdate fourthUpdate = (HistoricVariableUpdate) details.get(3);
      assertThat(fourthUpdate.getValue()).isEqualTo("abc");
      assertThat(((HistoryEvent) fourthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) thirdUpdate).getSequenceCounter());
    }

    // when (3)
    caseService.removeVariable(caseInstanceId, "myVariable");

    // then (3)
    variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNull();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(5);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(3);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());

      HistoricVariableUpdate fourthUpdate = (HistoricVariableUpdate) details.get(3);
      assertThat(fourthUpdate.getValue()).isEqualTo("abc");
      assertThat(((HistoryEvent) fourthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) thirdUpdate).getSequenceCounter());

      HistoricVariableUpdate fifthUpdate = (HistoricVariableUpdate) details.get(4);
      assertThat(fifthUpdate.getValue()).isNull();
      assertThat(((HistoryEvent) fifthUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) fourthUpdate).getSequenceCounter());
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testSetSameVariableUpdateOrder() {
    // given:
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariable(taskId, "myVariable", 1);
    taskService.setVariable(taskId, "myVariable", 1);
    taskService.setVariable(taskId, "myVariable", 2);

    // then
    HistoricVariableInstance variable = historyService
      .createHistoricVariableInstanceQuery()
      .singleResult();
    assertThat(variable).isNotNull();

    String variableInstanceId = variable.getId();

    if (isFullHistoryEnabled()) {

      List<HistoricDetail> details = historyService
        .createHistoricDetailQuery()
        .variableInstanceId(variableInstanceId)
        .orderPartiallyByOccurrence()
        .asc()
        .list();

      assertThat(details).hasSize(3);

      HistoricVariableUpdate firstUpdate = (HistoricVariableUpdate) details.get(0);
      assertThat(firstUpdate.getValue()).isEqualTo(1);

      HistoricVariableUpdate secondUpdate = (HistoricVariableUpdate) details.get(1);
      assertThat(secondUpdate.getValue()).isEqualTo(1);
      assertThat(((HistoryEvent) secondUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) firstUpdate).getSequenceCounter());

      HistoricVariableUpdate thirdUpdate = (HistoricVariableUpdate) details.get(2);
      assertThat(thirdUpdate.getValue()).isEqualTo(2);
      assertThat(((HistoryEvent) thirdUpdate).getSequenceCounter()).isGreaterThan(((HistoryEvent) secondUpdate).getSequenceCounter());
    }

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionProperty() {
    // given
    String key = "oneTaskProcess";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key);

    String processInstanceId = processInstance.getId();
    String taskId = taskService.createTaskQuery().singleResult().getId();

    runtimeService.setVariable(processInstanceId, "aVariable", "aValue");
    taskService.setVariableLocal(taskId, "aLocalVariable", "anotherValue");

    // when (1)
    HistoricVariableInstance instance = historyService
        .createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .variableName("aVariable")
        .singleResult();

    // then (1)
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(instance.getProcessDefinitionId()).isNotNull();
    assertThat(instance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();

    // when (2)
    instance = historyService
        .createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .variableName("aLocalVariable")
        .singleResult();

    // then (2)
    assertThat(instance.getProcessDefinitionKey()).isNotNull();
    assertThat(instance.getProcessDefinitionKey()).isEqualTo(key);

    assertThat(instance.getProcessDefinitionId()).isNotNull();
    assertThat(instance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());

    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testCaseDefinitionProperty() {
    // given
    String key = "oneTaskCase";
    CaseInstance caseInstance = caseService.createCaseInstanceByKey(key);

    String caseInstanceId = caseInstance.getId();

    caseService.createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();
    String taskId = taskService.createTaskQuery().singleResult().getId();

    caseService.setVariable(caseInstanceId, "aVariable", "aValue");
    taskService.setVariableLocal(taskId, "aLocalVariable", "anotherValue");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().caseInstanceIdIn(caseInstanceId).variableName("aVariable").singleResult();
    assertThat(variable).isNotNull();

    // when (1)
    HistoricVariableInstance instance = historyService
        .createHistoricVariableInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .variableName("aVariable")
        .singleResult();

    // then (1)
    assertCaseVariable(key, caseInstance, instance);

    // when (2)
    instance = historyService
        .createHistoricVariableInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .variableName("aLocalVariable")
        .singleResult();

    // then (2)
    assertCaseVariable(key, caseInstance, instance);

    // when (3)
    instance = historyService
        .createHistoricVariableInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .variableId(variable.getId())
        .singleResult();

    // then (4)
    assertThat(instance).isNotNull();
    assertCaseVariable(key, caseInstance, instance);
  }

  protected void assertCaseVariable(String key, CaseInstance caseInstance, HistoricVariableInstance instance) {
    assertThat(instance.getCaseDefinitionKey()).isNotNull();
    assertThat(instance.getCaseDefinitionKey()).isEqualTo(key);

    assertThat(instance.getCaseDefinitionId()).isNotNull();
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());

    assertThat(instance.getProcessDefinitionKey()).isNull();
    assertThat(instance.getProcessDefinitionId()).isNull();
  }

  @Test
  void testStandaloneTaskDefinitionProperties() {
    // given
    String taskId = "myTask";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    taskService.setVariable(taskId, "aVariable", "anotherValue");

    // when (1)
    HistoricVariableInstance instance = historyService
        .createHistoricVariableInstanceQuery()
        .taskIdIn(taskId)
        .variableName("aVariable")
        .singleResult();

    // then (1)
    assertThat(instance.getProcessDefinitionKey()).isNull();
    assertThat(instance.getProcessDefinitionId()).isNull();
    assertThat(instance.getCaseDefinitionKey()).isNull();
    assertThat(instance.getCaseDefinitionId()).isNull();

    taskService.deleteTask(taskId, true);
  }

  @Test
  void testTaskIdProperty() {
    // given
    String taskId = "myTask";
    Task task = taskService.newTask(taskId);
    taskService.saveTask(task);

    taskService.setVariable(taskId, "aVariable", "anotherValue");

    // when
    HistoricVariableInstance instance = historyService
        .createHistoricVariableInstanceQuery()
        .taskIdIn(taskId)
        .variableName("aVariable")
        .singleResult();

    // then
    assertThat(instance.getTaskId()).isEqualTo(taskId);

    taskService.deleteTask(taskId, true);
  }

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testJoinParallelGatewayLocalVariableOnLastJoiningExecution.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testNestedJoinParallelGatewayLocalVariableOnLastJoiningExecution.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testJoinInclusiveGatewayLocalVariableOnLastJoiningExecution.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testNestedJoinInclusiveGatewayLocalVariableOnLastJoiningExecution.bpmn20.xml"
  })
  void shouldSetHistoricVariable (String bpmnResource) {
    // when
    testRule.deploy(bpmnResource);
    runtimeService.startProcessInstanceByKey("process");

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getName()).isEqualTo("testVar");
  }

  @Deployment
  @Test
  void testForkParallelGatewayTreeCompaction() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    Task task1 = taskService
        .createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();

    // when
    runtimeService.setVariableLocal(task2Execution.getId(), "foo", "bar");
    taskService.complete(task1.getId());

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isOne();

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getName()).isEqualTo("foo");
  }

  @Deployment
  @Test
  void testNestedForkParallelGatewayTreeCompaction() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    Task task1 = taskService
        .createTaskQuery()
        .taskDefinitionKey("task1")
        .singleResult();

    Execution task2Execution = runtimeService
        .createExecutionQuery()
        .activityId("task2")
        .singleResult();

    // when
    runtimeService.setVariableLocal(task2Execution.getId(), "foo", "bar");
    taskService.complete(task1.getId());

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isOne();

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getName()).isEqualTo("foo");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testQueryByCaseActivityId() {
    // given
    caseService.createCaseInstanceByKey("oneTaskCase", Variables.putValue("foo", "bar"));

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    caseService.setVariableLocal(caseExecution.getId(), "bar", "foo");

    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery()
        .caseActivityIdIn("PI_HumanTask_1");

    // then
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getName()).isEqualTo("bar");
    assertThat(query.singleResult().getValue()).isEqualTo("foo");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn")
  @Test
  void testQueryByCaseActivityIds() {
    // given
    caseService.createCaseInstanceByKey("twoTaskCase");

    CaseExecution caseExecution1 = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    caseService.setVariableLocal(caseExecution1.getId(), "foo", "bar");

    CaseExecution caseExecution2 = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2")
        .singleResult();
    caseService.setVariableLocal(caseExecution2.getId(), "bar", "foo");

    // when
    HistoricVariableInstanceQuery query = historyService
        .createHistoricVariableInstanceQuery()
        .caseActivityIdIn("PI_HumanTask_1", "PI_HumanTask_2");

    // then
    assertThat(query.count()).isEqualTo(2);
  }

  @Test
  void testQueryByInvalidCaseActivityIds() {
    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();

    query.caseActivityIdIn("invalid");
    assertThat(query.count()).isZero();
    String[] values = { "a", null, "b" };

    assertThatThrownBy(() -> query.caseActivityIdIn((String[]) null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> query.caseActivityIdIn((String) null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> query.caseActivityIdIn(values)).isInstanceOf(NullValueException.class);
  }

  @Test
  void testSetVariableInSubProcessStartEventWithEndListener() {
    //given
    BpmnModelInstance topProcess = Bpmn.createExecutableProcess("topProcess")
        .startEvent()
        .callActivity()
        .calledElement("subProcess")
        .operatonIn("executionListenerCounter","executionListenerCounter")
        .endEvent()
        .done();

    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subProcess")
        .startEvent()
        .operatonAsyncBefore()
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, "org.operaton.bpm.engine.test.history.SubProcessActivityStartListener")
        .endEvent()
        .done();
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addModelInstance("process.bpmn", topProcess)
        .addModelInstance("subProcess.bpmn", subProcess)
        .deploy();

    //when
    runtimeService.startProcessInstanceByKey("topProcess", Variables.createVariables().putValue("executionListenerCounter",1));
    managementService.executeJob(managementService.createJobQuery().active().singleResult().getId());

    //then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(3L);
    repositoryService.deleteDeployment(deployment.getId(),true);
  }

  @Test
  void testSetVariableInEndListenerOfAsyncStartEvent() {
    //given
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("process")
      .startEvent()
      .operatonAsyncBefore()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, SubProcessActivityStartListener.class.getName())
      .endEvent()
      .done();

    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
      .addModelInstance("process.bpmn", subProcess)
      .deploy();

    //when
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("executionListenerCounter",1));
    managementService.executeJob(managementService.createJobQuery().active().singleResult().getId());

    //then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2L);
    repositoryService.deleteDeployment(deployment.getId(),true);
  }

  @Test
  void testSetVariableInStartListenerOfAsyncStartEvent() {
    //given
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("process")
      .startEvent()
      .operatonAsyncBefore()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, SubProcessActivityStartListener.class.getName())
      .endEvent()
      .done();

    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
      .addModelInstance("process.bpmn", subProcess)
      .deploy();

    //when
    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("executionListenerCounter",1));
    managementService.executeJob(managementService.createJobQuery().active().singleResult().getId());

    //then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(2L);
    repositoryService.deleteDeployment(deployment.getId(),true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEvent.bpmn20.xml")
  @Test
  void testAsyncStartEventHistory() {
    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      runtimeService.startProcessInstanceByKey("asyncStartEvent");

      HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
      assertThat(historicInstance).isNotNull();
      assertThat(historicInstance.getStartTime()).isNotNull();

      HistoricActivityInstance historicStartEvent = historyService.createHistoricActivityInstanceQuery().singleResult();
      assertThat(historicStartEvent).isNull();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEvent.bpmn20.xml")
  @Test
  void testAsyncStartEventVariableHistory() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    String processInstanceId = runtimeService.startProcessInstanceByKey("asyncStartEvent", variables).getId();

    VariableInstance variableFoo = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableFoo).isNotNull();
    assertThat(variableFoo.getName()).isEqualTo("foo");
    assertThat(variableFoo.getValue()).isEqualTo("bar");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    testRule.executeAvailableJobs();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId());

    // assert process instance is ended
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
      assertThat(variable).isNotNull();
      assertThat(variable.getName()).isEqualTo("foo");
      assertThat(variable.getValue()).isEqualTo("bar");
      assertThat(variable.getActivityInstanceId()).isEqualTo(processInstanceId);

      if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {

        HistoricDetail historicDetail = historyService
          .createHistoricDetailQuery()
          .singleResult();

        assertThat(historicDetail).isNotNull();
        assertThat(historicDetail.getActivityInstanceId()).isEqualTo(historicDetail.getProcessInstanceId());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testMultipleAsyncStartEvents.bpmn20.xml"})
  @Test
  void testMultipleAsyncStartEventsVariableHistory() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<>(), variables);

    VariableInstance variableFoo = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableFoo).isNotNull();
    assertThat(variableFoo.getName()).isEqualTo("foo");
    assertThat(variableFoo.getValue()).isEqualTo("bar");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    testRule.executeAvailableJobs();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    // assert process instance is ended
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      String processInstanceId = historyService
        .createHistoricProcessInstanceQuery()
        .singleResult()
        .getId();

      HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
      assertThat(variable).isNotNull();
      assertThat(variable.getName()).isEqualTo("foo");
      assertThat(variable.getValue()).isEqualTo("bar");
      assertThat(variable.getActivityInstanceId()).isEqualTo(processInstanceId);

      if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
        HistoricDetail historicDetail = historyService
          .createHistoricDetailQuery()
          .singleResult();

        assertThat(historicDetail).isNotNull();
        assertThat(historicDetail.getActivityInstanceId()).isEqualTo(historicDetail.getProcessInstanceId());
      }
    }
  }

  @Test
  void testAsyncStartEventWithAddedVariable() {
    // given a process definition with asynchronous start event
   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .operatonAsyncBefore()
      .endEvent()
      .done());

    // when create an instance with a variable
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess",
      Variables.putValue("var1", "foo"));

    // and add a variable before the instance is created
    runtimeService.setVariable(processInstance.getId(), "var2", "bar");

    testRule.executeAvailableJobs();

    testRule.assertProcessEnded(processInstance.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      // then the history contains one entry for each variable
      HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
      assertThat(query.count()).isEqualTo(2);

      HistoricVariableInstance firstVariable = query.variableName("var1").singleResult();
      assertThat(firstVariable).isNotNull();
      assertThat(firstVariable.getValue()).isEqualTo("foo");
      assertThat(firstVariable.getActivityInstanceId()).isNotNull();

      HistoricVariableInstance secondVariable = query.variableName("var2").singleResult();
      assertThat(secondVariable).isNotNull();
      assertThat(secondVariable.getValue()).isEqualTo("bar");
      assertThat(secondVariable.getActivityInstanceId()).isNotNull();
    }
  }


  @Test
  void testAsyncStartEventWithChangedVariable() {
    // given a process definition with asynchronous start event
   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .operatonAsyncBefore()
      .endEvent()
      .done());

    // when create an instance with a variable
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess",
      Variables.putValue("var", "foo"));

    // and update this variable before the instance is created
    runtimeService.setVariable(processInstance.getId(), "var", "bar");

    testRule.executeAvailableJobs();

    testRule.assertProcessEnded(processInstance.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      // then the history contains only one entry for the latest update (value = "bar")
      // - the entry for the initial value (value = "foo") is lost because of current limitations
      HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
      assertThat(query.count()).isOne();

      HistoricVariableInstance variable = query.singleResult();
      assertThat(variable.getValue()).isEqualTo("bar");
      assertThat(variable.getActivityInstanceId()).isNotNull();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEvent.bpmn20.xml")
  @Test
  void testSubmitForm() {

    String processDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("asyncStartEvent")
      .singleResult()
      .getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("foo", "bar");

    formService.submitStartForm(processDefinitionId, properties);

    VariableInstance variableFoo = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(variableFoo).isNotNull();
    assertThat(variableFoo.getName()).isEqualTo("foo");
    assertThat(variableFoo.getValue()).isEqualTo("bar");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    testRule.executeAvailableJobs();

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    // assert process instance is ended
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      String processInstanceId = historyService
        .createHistoricProcessInstanceQuery()
        .singleResult()
        .getId();

      HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
      assertThat(variable).isNotNull();
      assertThat(variable.getName()).isEqualTo("foo");
      assertThat(variable.getValue()).isEqualTo("bar");
      assertThat(variable.getActivityInstanceId()).isEqualTo(processInstanceId);

      if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {

        HistoricFormField historicFormUpdate = (HistoricFormField) historyService
          .createHistoricDetailQuery()
          .formFields()
          .singleResult();

        assertThat(historicFormUpdate).isNotNull();
        assertThat(historicFormUpdate.getFieldValue()).isEqualTo("bar");

        HistoricVariableUpdate historicVariableUpdate = (HistoricVariableUpdate) historyService
          .createHistoricDetailQuery()
          .variableUpdates()
          .singleResult();

        assertThat(historicVariableUpdate).isNotNull();
        assertThat(historicVariableUpdate.getActivityInstanceId()).isEqualTo(historicVariableUpdate.getProcessInstanceId());
        assertThat(historicVariableUpdate.getValue()).isEqualTo("bar");

      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/async/AsyncStartEventTest.testAsyncStartEvent.bpmn20.xml")
  @Disabled("CAM-2828")
  @Test
  void testSubmitFormHistoricUpdates() {

    String processDefinitionId = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("asyncStartEvent")
      .singleResult()
      .getId();

    Map<String, Object> properties = new HashMap<>();
    properties.put("foo", "bar");

    formService.submitStartForm(processDefinitionId, properties);
    testRule.executeAvailableJobs();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {

      String theStartActivityInstanceId = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("startEvent")
        .singleResult()
        .getId();

      HistoricDetail historicFormUpdate = historyService
        .createHistoricDetailQuery()
        .formFields()
        .singleResult();

      assertThat(historicFormUpdate).isNotNull();
      assertThat(historicFormUpdate.getActivityInstanceId()).isEqualTo(theStartActivityInstanceId);

    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testSetDifferentStates() {
    //given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "foo"));
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariables(task.getId(), Variables.createVariables().putValue("bar", "abc"));
    taskService.complete(task.getId());

    //when
    runtimeService.removeVariable(processInstance.getId(), "bar");

    //then
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().includeDeleted().list();
    assertThat(variables).hasSize(2);

    int createdCounter = 0;
    int deletedCounter = 0;

    for (HistoricVariableInstance variable : variables) {
      if ("initial".equals(variable.getName())) {
        assertThat(variable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
        createdCounter += 1;
      } else if ("bar".equals(variable.getName())) {
        assertThat(variable.getState()).isEqualTo(HistoricVariableInstance.STATE_DELETED);
        deletedCounter += 1;
      }
    }

    assertThat(createdCounter).isEqualTo(1);
    assertThat(deletedCounter).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testQueryNotIncludeDeleted() {
    //given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "foo"));
    Task task = taskService.createTaskQuery().singleResult();
    taskService.setVariables(task.getId(), Variables.createVariables().putValue("bar", "abc"));
    taskService.complete(task.getId());

    //when
    runtimeService.removeVariable(processInstance.getId(), "bar");

    //then
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(variable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
    assertThat(variable.getName()).isEqualTo("initial");
    assertThat(variable.getValue()).isEqualTo("foo");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionId() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess",
        Variables.createVariables().putValue("initial", "foo"));

    // when
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
        .processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();

    // then
    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("initial");
    assertThat(variable.getValue()).isEqualTo("foo");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionKey() {
    // given
    runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "foo"));

    // when
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
        .processDefinitionKey("twoTasksProcess").singleResult();

    // then
    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("initial");
    assertThat(variable.getValue()).isEqualTo("foo");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionKeyTwoInstances() {
    // given
    runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "foo").putValue("vegie", "cucumber"));
    runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "bar").putValue("fruit", "marakuia"));

    // when
    List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
    .processDefinitionKey("twoTasksProcess").list();

    // then
    assertThat(variables)
            .isNotNull()
            .hasSize(4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml", "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessDefinitionKeyTwoDefinitions() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.createVariables().putValue("initial", "bar"));
    runtimeService.startProcessInstanceByKey("twoTasksProcess", Variables.createVariables().putValue("initial", "foo"));

    // when
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
        .processDefinitionKey("twoTasksProcess").singleResult();

    // then
    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("initial");
    assertThat(variable.getValue()).isEqualTo("foo");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryByProcessInstanceIdAndVariableId() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.createVariables().putValue("initial", "bar"));

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("initial").singleResult();
    assertThat(variable).isNotNull();

    // when
    HistoricVariableInstance historyVariable = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstance.getId())
        .variableId(variable.getId())
        .singleResult();

    // then
    assertThat(historyVariable).isNotNull();
    assertThat(historyVariable.getName()).isEqualTo("initial");
    assertThat(historyVariable.getValue()).isEqualTo("bar");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableCreateTime() throws Exception {
    // given
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    Date fixedDate = sdf.parse("01/01/2001 01:01:01.000");
    ClockUtil.setCurrentTime(fixedDate);
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "test");
    // when
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // then
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(variable.getCreateTime()).isEqualTo(fixedDate);

    // clean up
    ClockUtil.setCurrentTime(new Date());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableNameEqualsIgnoreCase() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String variableName = "variableName";
    variables.put(variableName, "variableValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().variableName(variableName).singleResult();
    HistoricVariableInstance instanceIgnoreCase = historyService.createHistoricVariableInstanceQuery().variableName(variableName.toLowerCase()).singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchIgnoreCase = historyService.createHistoricVariableInstanceQuery().variableName(variableName.toLowerCase())
        .matchVariableNamesIgnoreCase().singleResult();

    // then
    assertThat(instance).isNotNull();
    assertThat(instanceIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchIgnoreCase).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableValueEqualsIgnoreCase() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String variableName = "variableName";
    String variableValue = "variableValue";
    variables.put(variableName, variableValue);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().variableValueEquals(variableName, variableValue).singleResult();
    HistoricVariableInstance instanceIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName, variableValue.toLowerCase()).singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName, variableValue.toLowerCase()).matchVariableValuesIgnoreCase().singleResult();

    // then
    assertThat(instance).isNotNull();
    assertThat(instanceIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchIgnoreCase).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableNameAndValueEqualsIgnoreCase() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String variableName = "variableName";
    String variableValue = "variableValue";
    variables.put(variableName, variableValue);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().variableValueEquals(variableName, variableValue).singleResult();
    HistoricVariableInstance instanceIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName.toLowerCase(), variableValue.toLowerCase()).singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchNameIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName.toLowerCase(), variableValue.toLowerCase()).matchVariableNamesIgnoreCase().singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchValueIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName.toLowerCase(), variableValue.toLowerCase()).matchVariableValuesIgnoreCase().singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchNameAndValueIgnoreCase = historyService.createHistoricVariableInstanceQuery()
        .variableValueEquals(variableName.toLowerCase(), variableValue.toLowerCase()).matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase()
        .singleResult();

    // then
    assertThat(instance).isNotNull();
    assertThat(instanceIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchNameIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchValueIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchNameAndValueIgnoreCase).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableNameAndValueEqualsEmptyString() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String variableName = "variableName";
    String variableValue = "";
    variables.put(variableName, variableValue);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().variableValueEquals(variableName, variableValue).singleResult();

    // then
    assertThat(instance).isNotNull();
    assertThat(instance.getValue()).isEqualTo("");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testVariableNameLikeIgnoreCase() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String variableName = "variableName";
    String variableValue = "variableValue";
    variables.put(variableName, variableValue);
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    HistoricVariableInstance instance = historyService.createHistoricVariableInstanceQuery().variableNameLike("variableN%").singleResult();
    HistoricVariableInstance instanceIgnoreCase = historyService.createHistoricVariableInstanceQuery().variableNameLike("variablen%").singleResult();
    HistoricVariableInstance instanceIgnoreCaseMatchNameIgnoreCase = historyService.createHistoricVariableInstanceQuery().variableNameLike("variablen%").matchVariableNamesIgnoreCase().singleResult();

    // then
    assertThat(instance).isNotNull();
    assertThat(instanceIgnoreCase).isNull();
    assertThat(instanceIgnoreCaseMatchNameIgnoreCase).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldQueryByVariableNamesWithOneVariableName() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("my-variable-name-one", "my-variable-value-one");
    variables.put("my-variable-name-two", "my-variable-value-two");
    variables.put("my-variable-name-three", "my-variable-value-three");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    List<HistoricVariableInstance> instances = historyService.createHistoricVariableInstanceQuery()
        .variableNameIn("my-variable-name-one")
        .list();

    // then
    assertThat(instances).extracting("name", "value")
        .containsExactly(tuple("my-variable-name-one", "my-variable-value-one"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldQueryByVariableNamesWithTwoVariableNames() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("my-variable-name-one", "my-variable-value-one");
    variables.put("my-variable-name-two", "my-variable-value-two");
    variables.put("my-variable-name-three", "my-variable-value-three");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // when
    List<HistoricVariableInstance> instances = historyService.createHistoricVariableInstanceQuery()
        .variableNameIn("my-variable-name-one", "my-variable-name-two")
        .list();

    // then
    assertThat(instances).extracting("name", "value")
        .containsExactlyInAnyOrder(
            tuple("my-variable-name-one", "my-variable-value-one"),
            tuple("my-variable-name-two", "my-variable-value-two"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldThrowExceptionWhenQueryByVariableNamesWithNullString() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ThrowingCallable throwingCallable =
        () -> historyService.createHistoricVariableInstanceQuery().variableNameIn((String) null);

    // then
    assertThatThrownBy(throwingCallable)
        .isInstanceOf(NullValueException.class)
        .hasMessage("Variable names contains null value");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldThrowExceptionWhenQueryByVariableNamesWithNullArrayString() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ThrowingCallable throwingCallable =
        () -> historyService.createHistoricVariableInstanceQuery().variableNameIn((String[]) null);

    // then
    assertThatThrownBy(throwingCallable)
        .isInstanceOf(NullValueException.class)
        .hasMessage("Variable names is null");
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testCallSimpleSubProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/history/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void shouldBeCorrectlySortedWhenSortingByVariableCreationTime() {
  // given
  runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

  // when
  List<HistoricVariableInstance> historicVariableInstancesAsc =
      historyService.createHistoricVariableInstanceQuery().orderByCreationTime().asc().list();
  List<HistoricVariableInstance> historicVariableInstancesDesc =
      historyService.createHistoricVariableInstanceQuery().orderByCreationTime().desc().list();

  // then
  assertThat(historicVariableInstancesAsc).hasSize(5);
  assertThat(historicVariableInstancesDesc).hasSize(5);
  verifySorting(historicVariableInstancesAsc, propertyComparator(HistoricVariableInstance::getCreateTime));
  verifySorting(historicVariableInstancesDesc, inverted(propertyComparator(HistoricVariableInstance::getCreateTime)));
}

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testCallSimpleSubProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/history/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void shouldQueryByCreatedAfter() {
  // given
  Calendar creationDate = Calendar.getInstance();
  ClockUtil.setCurrentTime(creationDate.getTime());
  runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

  creationDate.add(Calendar.HOUR, 1);
  ClockUtil.setCurrentTime(creationDate.getTime());
  runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

  // when
  List<HistoricVariableInstance> variablesCreatedAfter = historyService.createHistoricVariableInstanceQuery()
      .createdAfter(creationDate.getTime())
      .list();
  List<HistoricVariableInstance> allVariables = historyService.createHistoricVariableInstanceQuery().list();

  // then
  assertThat(variablesCreatedAfter).hasSize(5);
  assertThat(allVariables).hasSize(10);
}



}
