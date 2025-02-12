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
package org.operaton.bpm.engine.test.api.runtime;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.operaton.bpm.engine.impl.util.CollectionUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.util.SimpleSerializableBean;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.history.SerializableVariable;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.test.util.TestExecutionListener;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.SubProcessBuilder;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutableProcessUtil.USER_TASK_PROCESS;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.objectValue;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class RuntimeServiceTest {

  public static final String TESTING_INSTANCE_DELETION = "testing instance deletion";

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  private RuntimeService runtimeService;
  private TaskService taskService;
  private ManagementService managementService;
  private RepositoryService repositoryService;
  private HistoryService historyService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    managementService = engineRule.getManagementService();
    repositoryService = engineRule.getRepositoryService();
    historyService = engineRule.getHistoryService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @Test
  public void testStartProcessInstanceByKeyNullKey() {
    try {
      runtimeService.startProcessInstanceByKey(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      // Expected exception
    }
  }

  @Test
  public void testStartProcessInstanceByKeyUnexistingKey() {
    try {
      runtimeService.startProcessInstanceByKey("unexistingkey");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("no processes deployed with key", ae.getMessage());
    }
  }

  @Test
  public void testStartProcessInstanceByIdNullId() {
    try {
      runtimeService.startProcessInstanceById(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      // Expected exception
    }
  }

  @Test
  public void testStartProcessInstanceByIdUnexistingId() {
    try {
      runtimeService.startProcessInstanceById("unexistingId");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("no deployed process definition found with id", ae.getMessage());
    }
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testStartProcessInstanceByIdNullVariables() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess", (Map<String, Object>) null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);
  }

  @Test
  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void startProcessInstanceWithBusinessKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // by key
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "123");
    assertNotNull(processInstance);
    assertThat(processInstance.getBusinessKey()).isEqualTo("123");
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

    // by key with variables
    processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "456", CollectionUtil.singletonMap("var", "value"));
    assertNotNull(processInstance);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(2);
    assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value");

    // by id
    processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "789");
    assertNotNull(processInstance);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(3);

    // by id with variables
    processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "101123", CollectionUtil.singletonMap("var", "value2"));
    assertNotNull(processInstance);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(4);
    assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo("value2");
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

    runtimeService.deleteProcessInstance(processInstance.getId(), TESTING_INSTANCE_DELETION);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(0);

    // test that the delete reason of the process instance shows up as delete reason of the task in history
    // ACT-848
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {

      HistoricTaskInstance historicTaskInstance = historyService
              .createHistoricTaskInstanceQuery()
              .processInstanceId(processInstance.getId())
              .singleResult();

      assertThat(historicTaskInstance.getDeleteReason()).isEqualTo(TESTING_INSTANCE_DELETION);
    }
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstances() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // if we skip the custom listeners,
    runtimeService.deleteProcessInstances(Arrays.asList(processInstance.getId(),processInstance2.getId()), null, false, false);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testDeleteProcessInstanceWithListeners() {
    RecorderExecutionListener.clear();

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedParallelGatewayScopeTasks");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), "");

    // then
    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertThat(recordedEvents).hasSize(10);

    Set<RecordedEvent> startEvents = new HashSet<>();
    Set<RecordedEvent> endEvents = new HashSet<>();
    for (RecordedEvent event : recordedEvents) {
      if(event.getEventName().equals(ExecutionListener.EVENTNAME_START)){
        startEvents.add(event);
      } else if(event.getEventName().equals(ExecutionListener.EVENTNAME_END)){
        endEvents.add(event);
      }
    }

    assertThat(startEvents).hasSize(5);
    assertThat(endEvents).hasSize(5);
    for (RecordedEvent startEvent : startEvents) {
      assertThat(startEvent.getActivityId()).isIn("innerTask1",
          "innerTask2", "outerTask", "subProcess", "theStart");
      for (RecordedEvent endEvent : endEvents) {
        if(startEvent.getActivityId().equals(endEvent.getActivityId())){
          assertThat(startEvent.getActivityInstanceId()).isEqualTo(endEvent.getActivityInstanceId());
          assertThat(startEvent.getExecutionId()).isEqualTo(endEvent.getExecutionId());
        }
      }
    }
    for (RecordedEvent recordedEvent : endEvents) {
      assertThat(recordedEvent.getActivityId()).isIn("innerTask1",
          "innerTask2", "outerTask", "subProcess", null);
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstanceSkipCustomListenersEnsureHistoryWritten() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // if we skip the custom listeners,
    runtimeService.deleteProcessInstance(processInstance.getId(), null, true);

    // built-in listeners are still invoked and thus history is written
    if(!ProcessEngineConfiguration.HISTORY_NONE.equals(processEngineConfiguration.getHistory())) {
      // verify that all historic activity instances are ended
      List<HistoricActivityInstance> hais = historyService.createHistoricActivityInstanceQuery().list();
      for (HistoricActivityInstance hai : hais) {
        assertNotNull(hai.getEndTime());
      }
    }
  }

  @Deployment
  @Test
  public void testDeleteProcessInstanceSkipCustomListeners() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if we do not skip the custom listeners,
    runtimeService.deleteProcessInstance(processInstance.getId(), null, false);
    // the custom listener is invoked
    assertThat(TestExecutionListener.collectedEvents).hasSize(1);
    TestExecutionListener.reset();

    processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if we DO skip the custom listeners,
    runtimeService.deleteProcessInstance(processInstance.getId(), null, true);
    // the custom listener is not invoked
    assertTrue(TestExecutionListener.collectedEvents.isEmpty());
    TestExecutionListener.reset();
  }

  @Deployment
  @Test
  public void testDeleteProcessInstanceSkipCustomListenersScope() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if we do not skip the custom listeners,
    runtimeService.deleteProcessInstance(processInstance.getId(), null, false);
    // the custom listener is invoked
    assertThat(TestExecutionListener.collectedEvents).hasSize(1);
    TestExecutionListener.reset();

    processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if we DO skip the custom listeners,
    runtimeService.deleteProcessInstance(processInstance.getId(), null, true);
    // the custom listener is not invoked
    assertTrue(TestExecutionListener.collectedEvents.isEmpty());
    TestExecutionListener.reset();
  }

  @Deployment
  @Test
  public void testDeleteProcessInstanceSkipCustomTaskListeners() {

    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // and an empty task listener invocation storage
    RecorderTaskListener.clear();

    // if we do not skip the custom listeners
    runtimeService.deleteProcessInstance(instance.getId(), null, false);

    // then the custom listener is invoked
    assertThat(RecorderTaskListener.getRecordedEvents()).hasSize(1);
    assertThat(RecorderTaskListener.getRecordedEvents().get(0).getEvent()).isEqualTo(TaskListener.EVENTNAME_DELETE);

    // if we do skip the custom listeners
    instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    RecorderTaskListener.clear();

    runtimeService.deleteProcessInstance(instance.getId(), null, true);

    // then the custom listener is not invoked
    assertTrue(RecorderTaskListener.getRecordedEvents().isEmpty());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/oneTaskProcessWithIoMappings.bpmn20.xml" })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testDeleteProcessInstanceSkipIoMappings() {

    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // when the process instance is deleted and we do skip the io mappings
    runtimeService.deleteProcessInstance(instance.getId(), null, false, true, true);

    // then
    testRule.assertProcessEnded(instance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcessWithIoMappings.bpmn20.xml" })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testDeleteProcessInstanceWithoutSkipIoMappings() {

    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // when the process instance is deleted and we do not skip the io mappings
    runtimeService.deleteProcessInstance(instance.getId(), null, false, true, false);

    // then
    testRule.assertProcessEnded(instance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instance.getId()).list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("outputMappingExecuted").count()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testCascadingDeleteSubprocessInstanceSkipIoMappings.Calling.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testCascadingDeleteSubprocessInstanceSkipIoMappings.Called.bpmn20.xml" })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testCascadingDeleteSubprocessInstanceSkipIoMappings() {

    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callingProcess");

    ProcessInstance instance2 = runtimeService.createProcessInstanceQuery().superProcessInstanceId(instance.getId()).singleResult();

    // when the process instance is deleted and we do skip the io mappings
    runtimeService.deleteProcessInstance(instance.getId(), "test_purposes", false, true, true);

    // then
    testRule.assertProcessEnded(instance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instance2.getId()).list()).hasSize(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testCascadingDeleteSubprocessInstanceSkipIoMappings.Calling.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testCascadingDeleteSubprocessInstanceSkipIoMappings.Called.bpmn20.xml" })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testCascadingDeleteSubprocessInstanceWithoutSkipIoMappings() {

    // given a process instance
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("callingProcess");

    ProcessInstance instance2 = runtimeService.createProcessInstanceQuery().superProcessInstanceId(instance.getId()).singleResult();

    // when the process instance is deleted and we do not skip the io mappings
    runtimeService.deleteProcessInstance(instance.getId(), "test_purposes", false, true, false);

    // then
    testRule.assertProcessEnded(instance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(instance2.getId()).list()).hasSize(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("inputMappingExecuted").count()).isEqualTo(1);
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("outputMappingExecuted").count()).isEqualTo(1);
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstanceNullReason() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);

    // Deleting without a reason should be possible
    runtimeService.deleteProcessInstance(processInstance.getId(), null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(0);
  }

  /**
   * CAM-8005 - StackOverflowError must not happen.
   * Note: debug at CommandContextInterceptor -> context.close()
   */
  @Test
  public void testDeleteProcessInstancesManyParallelSubprocesses() {
    final BpmnModelInstance multiInstanceWithSubprocess =
      Bpmn.createExecutableProcess("multiInstanceWithSubprocess")
        .startEvent()
          .subProcess()
          .embeddedSubProcess()
          .startEvent()
            .userTask("userTask")
          .endEvent()
          .subProcessDone()
          .multiInstance().cardinality("300").multiInstanceDone()
        .endEvent()
      .done();

    testRule.deploy(multiInstanceWithSubprocess);

    final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multiInstanceWithSubprocess");

    runtimeService.deleteProcessInstance(processInstance.getId(), "some reason");
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
  }

  @Test
  public void testDeleteProcessInstanceWithFake() {
    try {
      runtimeService.deleteProcessInstance("aFake", null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("No process instance found for id", e.getMessage());
      assertTrue(e instanceof NotFoundException);
    }
  }

  @Test
  public void testDeleteProcessInstanceIfExistsWithFake() {
      runtimeService.deleteProcessInstanceIfExists("aFake", null, false, false, false, false);
      //don't expect exception
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstancesWithFake() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    var processInstanceIds = Arrays.asList(instance.getId(), "aFake");

    try {
      runtimeService.deleteProcessInstances(processInstanceIds, "test", false, false, false, false);
      fail("ProcessEngineException expected");
    }catch (ProcessEngineException e) {
      //expected
      assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(1);
      assertTrue(e instanceof NotFoundException);
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testDeleteProcessInstancesIfExistsWithFake() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.deleteProcessInstancesIfExists(Arrays.asList(instance.getId(), "aFake"), "test", false, false, false);
    //dont't expect exception, existing instances are deleted
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count()).isEqualTo(0);
  }

  @Test
  public void testDeleteProcessInstanceNullId() {
    try {
      runtimeService.deleteProcessInstance(null, "test null id delete");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("processInstanceId is null", ae.getMessage());
      assertTrue(ae instanceof BadUserRequestException);
    }
  }

  @Deployment
  @Test
  public void testDeleteProcessInstanceWithActiveCompensation() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("compensationProcess");

    Task innerTask = taskService.createTaskQuery().singleResult();
    taskService.complete(innerTask.getId());

    Task afterSubProcessTask = taskService.createTaskQuery().singleResult();
    assertThat(afterSubProcessTask.getTaskDefinitionKey()).isEqualTo("taskAfterSubprocess");
    taskService.complete(afterSubProcessTask.getId());

    // when
    // there are two compensation tasks
    assertThat(taskService.createTaskQuery().taskDefinitionKey("outerAfterBoundaryTask").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("innerAfterBoundaryTask").count()).isEqualTo(1);

    // when the process instance is deleted
    runtimeService.deleteProcessInstance(instance.getId(), "");

    // then
    testRule.assertProcessEnded(instance.getId());
  }


  @Deployment
  @Test
  public void testDeleteProcessInstanceWithVariableOnScopeAndConcurrentExecution() {

    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task")
      .execute();

    List<Execution> executions = runtimeService.createExecutionQuery().list();

    for (Execution execution : executions) {
      runtimeService.setVariableLocal(execution.getId(), "foo", "bar");
    }

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfigurationImpl.HISTORY_AUDIT)
  public void testDeleteCalledSubprocess() {

    // given
    BpmnModelInstance callingInstance = ProcessModels.newModel("oneTaskProcess")
      .startEvent()
      .callActivity()
      .calledElement("called")
      .endEvent()
      .done();

    BpmnModelInstance calledInstance = ProcessModels.newModel("called")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

    testRule.deploy(callingInstance, calledInstance);
    final String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getProcessInstanceId();

    String subprocessId = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("called").singleResult().getId();

    runtimeService.deleteProcessInstance(subprocessId, TESTING_INSTANCE_DELETION);

    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(subprocessId).singleResult().getDeleteReason()).isEqualTo(TESTING_INSTANCE_DELETION);
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult().getDeleteReason()).isEqualTo(TESTING_INSTANCE_DELETION);
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testFindActiveActivityIds() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<String> activities = runtimeService.getActiveActivityIds(processInstance.getId());
    assertNotNull(activities);
    assertThat(activities).hasSize(1);
  }

  @Test
  public void testFindActiveActivityIdsUnexistingExecutionId() {
    try {
      runtimeService.getActiveActivityIds("unexistingExecutionId");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
    }
  }

  @Test
  public void testFindActiveActivityIdsNullExecutionId() {
    try {
      runtimeService.getActiveActivityIds(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  /**
   * Testcase to reproduce ACT-950 (https://jira.codehaus.org/browse/ACT-950)
   */
  @Deployment
  @Test
  public void testFindActiveActivityIdProcessWithErrorEventAndSubProcess() {
    ProcessInstance processInstance = engineRule.getProcessEngine().getRuntimeService().startProcessInstanceByKey("errorEventSubprocess");

    List<String> activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivities).hasSize(3);

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    Task parallelUserTask = null;
    for (Task task : tasks) {
      if (!task.getName().equals("ParallelUserTask") && !task.getName().equals("MainUserTask")) {
        fail("Expected: <ParallelUserTask> or <MainUserTask> but was <" + task.getName() + ">.");
      }
      if (task.getName().equals("ParallelUserTask")) {
        parallelUserTask = task;
      }
    }
    assertNotNull(parallelUserTask);

    taskService.complete(parallelUserTask.getId());

    Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subprocess1WaitBeforeError").singleResult();
    runtimeService.signal(execution.getId());

    activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivities).hasSize(2);

    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    Task beforeErrorUserTask = null;
    for (Task task : tasks) {
      if (!task.getName().equals("BeforeError") && !task.getName().equals("MainUserTask")) {
        fail("Expected: <BeforeError> or <MainUserTask> but was <" + task.getName() + ">.");
      }
      if (task.getName().equals("BeforeError")) {
        beforeErrorUserTask = task;
      }
    }
    assertNotNull(beforeErrorUserTask);

    taskService.complete(beforeErrorUserTask.getId());

    activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivities).hasSize(2);

    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    Task afterErrorUserTask = null;
    for (Task task : tasks) {
      if (!task.getName().equals("AfterError") && !task.getName().equals("MainUserTask")) {
        fail("Expected: <AfterError> or <MainUserTask> but was <" + task.getName() + ">.");
      }
      if (task.getName().equals("AfterError")) {
        afterErrorUserTask = task;
      }
    }
    assertNotNull(afterErrorUserTask);

    taskService.complete(afterErrorUserTask.getId());

    tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("MainUserTask");

    activeActivities = runtimeService.getActiveActivityIds(processInstance.getId());
    assertThat(activeActivities).hasSize(1);
    assertThat(activeActivities.get(0)).isEqualTo("MainUserTask");

    taskService.complete(tasks.get(0).getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  public void testSignalUnexistingExecutionId() {
    try {
      runtimeService.signal("unexistingExecutionId");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
      assertTrue(ae instanceof BadUserRequestException);
    }
  }

  @Test
  public void testSignalNullExecutionId() {
    try {
      runtimeService.signal(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
      assertTrue(ae instanceof BadUserRequestException);
    }
  }

  @Deployment
  @Test
  public void testSignalWithProcessVariables() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSignalWithProcessVariables");
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("variable", "value");

    // signal the execution while passing in the variables
    runtimeService.signal(processInstance.getId(), processVariables);

    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(processVariables).isEqualTo(variables);

  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testSignalWithProcessVariables.bpmn20.xml"})
  @Test
  public void testSignalWithSignalNameAndData() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSignalWithProcessVariables");
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("variable", "value");

    // signal the execution while passing in the variables
    runtimeService.signal(processInstance.getId(), "dummySignalName", "SignalData", processVariables);

    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(processVariables).isEqualTo(variables);

  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testSignalWithProcessVariables.bpmn20.xml"})
  @Test
  public void testSignalWithoutSignalNameAndData() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSignalWithProcessVariables");
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("variable", "value");

    // signal the execution while passing in the variables
    runtimeService.signal(processInstance.getId(), null, null, processVariables);

    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables).isEqualTo(processVariables);

  }

  @Deployment
  @Test
  public void testSignalInactiveExecution() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("testSignalInactiveExecution");
    var instanceId = instance.getId();

    // there exist two executions: the inactive parent (the process instance) and the child that actually waits in the receive task
    try {
      runtimeService.signal(instanceId);
      fail();
    } catch(ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("cannot signal execution " + instance.getId() + ": it has no current activity", e.getMessage());
    } catch (Exception e) {
      fail("Signalling an inactive execution that has no activity should result in a ProcessEngineException");
    }

  }

  @Test
  public void testGetVariablesUnexistingExecutionId() {
    try {
      runtimeService.getVariables("unexistingExecutionId");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
    }
  }

  @Test
  public void testGetVariablesNullExecutionId() {
    try {
      runtimeService.getVariables(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Test
  public void testGetVariableUnexistingExecutionId() {
    try {
      runtimeService.getVariables("unexistingExecutionId");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
    }
  }

  @Test
  public void testGetVariableNullExecutionId() {
    try {
      runtimeService.getVariables(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariableUnexistingVariableName() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Object variableValue = runtimeService.getVariable(processInstance.getId(), "unexistingVariable");
    assertNull(variableValue);
  }

  @Test
  public void testSetVariableUnexistingExecutionId() {
    try {
      runtimeService.setVariable("unexistingExecutionId", "variableName", "value");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingExecutionId doesn't exist", ae.getMessage());
    }
  }

  @Test
  public void testSetVariableNullExecutionId() {
    try {
      runtimeService.setVariable(null, "variableName", "variableValue");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSetVariableNullVariableName() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    var processInstanceId = processInstance.getId();
    try {
      runtimeService.setVariable(processInstanceId, null, "variableValue");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("variableName is null", ae.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSetVariables() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariables(processInstance.getId(), vars);

    assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isEqualTo("value1");
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesTyped() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    VariableMap variablesTyped = runtimeService.getVariablesTyped(processInstance.getId());
    assertThat(variablesTyped).isEqualTo(vars);
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesTypedDeserialize() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables()
          .putValue("broken", Variables.serializedObjectValue("broken")
              .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
              .objectTypeName("unexisting").create()));

    // this works
    VariableMap variablesTyped = runtimeService.getVariablesTyped(processInstance.getId(), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));
    variablesTyped = runtimeService.getVariablesTyped(processInstance.getId(), List.of("broken"), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));

    // this does not
    try {
      runtimeService.getVariablesTyped(processInstance.getId());
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

    // this does not
    try {
      runtimeService.getVariablesTyped(processInstance.getId(), List.of("broken"), true);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesLocalTyped() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    VariableMap variablesTyped = runtimeService.getVariablesLocalTyped(processInstance.getId());
    assertThat(variablesTyped).isEqualTo(vars);
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesLocalTypedDeserialize() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables()
          .putValue("broken", Variables.serializedObjectValue("broken")
              .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
              .objectTypeName("unexisting").create()));

    // this works
    VariableMap variablesTyped = runtimeService.getVariablesLocalTyped(processInstance.getId(), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));
    variablesTyped = runtimeService.getVariablesLocalTyped(processInstance.getId(), List.of("broken"), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));

    // this does not
    try {
      runtimeService.getVariablesLocalTyped(processInstance.getId());
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

    // this does not
    try {
      runtimeService.getVariablesLocalTyped(processInstance.getId(), List.of("broken"), true);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetVariablesUnexistingExecutionId() {
    try {
      runtimeService.setVariables("unexistingexecution", Collections.EMPTY_MAP);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("execution unexistingexecution doesn't exist", ae.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetVariablesNullExecutionId() {
    try {
      runtimeService.setVariables(null, Collections.EMPTY_MAP);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }


  @Test
  public void setVariablesSyncOnCompletedProcessInstance() {
    // given completed process instance
    testRule.deploy(USER_TASK_PROCESS);
    String id = runtimeService.startProcessInstanceByKey("process").getId();
    Task task = engineRule.getTaskService().createTaskQuery().processInstanceId(id).singleResult();
    engineRule.getTaskService().complete(task.getId());
    VariableMap variables = createVariables().putValue("foo", "bar");

    // when setting variables then exception is thrown
    assertThatThrownBy(() -> runtimeService.setVariables(id, variables))
        .isInstanceOf(NullValueException.class)
        .hasMessage("execution " + id + " doesn't exist: execution is null");
  }

  private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      boolean deletedVariableUpdateFound = false;

      List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
      for (HistoricDetail currentHistoricDetail : resultSet) {
        assertTrue(currentHistoricDetail instanceof HistoricDetailVariableInstanceUpdateEntity);
        HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = (HistoricDetailVariableInstanceUpdateEntity) currentHistoricDetail;

        if (historicVariableUpdate.getName().equals(variableName) && historicVariableUpdate.getValue() == null) {
          if (deletedVariableUpdateFound) {
            fail("Mismatch: A HistoricVariableUpdateEntity with a null value already found");
          } else {
            deletedVariableUpdateFound = true;
          }
        }
      }

      assertTrue(deletedVariableUpdateFound);
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.setVariables(processInstance.getId(), vars);

    runtimeService.removeVariable(processInstance.getId(), "variable1");

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariableInParentScope() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
    Task currentTask = taskService.createTaskQuery().singleResult();

    runtimeService.removeVariable(currentTask.getExecutionId(), "variable1");

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
  }


  @Test
  public void testRemoveVariableNullExecutionId() {
    try {
      runtimeService.removeVariable(null, "variable");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariableLocal() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.removeVariableLocal(processInstance.getId(), "variable1");

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariableLocalWithParentScope() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
    Task currentTask = taskService.createTaskQuery().singleResult();
    runtimeService.setVariableLocal(currentTask.getExecutionId(), "localVariable", "local value");

    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable")).isEqualTo("local value");

    runtimeService.removeVariableLocal(currentTask.getExecutionId(), "localVariable");

    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "localVariable"));
    assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "localVariable"));

    assertThat(runtimeService.getVariable(processInstance.getId(), "variable1")).isEqualTo("value1");
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isEqualTo("value1");
    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable2")).isEqualTo("value2");

    checkHistoricVariableUpdateEntity("localVariable", processInstance.getId());
  }


  @Test
  public void testRemoveLocalVariableNullExecutionId() {
    try {
      runtimeService.removeVariableLocal(null, "variable");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariables() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

    runtimeService.removeVariables(processInstance.getId(), vars.keySet());

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariable(processInstance.getId(), "variable2"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable2"));

    assertThat(runtimeService.getVariable(processInstance.getId(), "variable3")).isEqualTo("value3");
    assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable3")).isEqualTo("value3");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariablesWithParentScope() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);
    runtimeService.setVariable(processInstance.getId(), "variable3", "value3");

    Task currentTask = taskService.createTaskQuery().singleResult();

    runtimeService.removeVariables(currentTask.getExecutionId(), vars.keySet());

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable1"));
    assertNull(runtimeService.getVariable(processInstance.getId(), "variable2"));
    assertNull(runtimeService.getVariableLocal(processInstance.getId(), "variable2"));

    assertThat(runtimeService.getVariable(processInstance.getId(), "variable3")).isEqualTo("value3");
    assertThat(runtimeService.getVariableLocal(processInstance.getId(), "variable3")).isEqualTo("value3");

    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable1"));
    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable2"));

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRemoveVariablesNullExecutionId() {
    try {
      runtimeService.removeVariables(null, Collections.EMPTY_LIST);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testRemoveVariablesLocalWithParentScope() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", vars);

    Task currentTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> varsToDelete = new HashMap<>();
    varsToDelete.put("variable3", "value3");
    varsToDelete.put("variable4", "value4");
    varsToDelete.put("variable5", "value5");
    runtimeService.setVariablesLocal(currentTask.getExecutionId(), varsToDelete);
    runtimeService.setVariableLocal(currentTask.getExecutionId(), "variable6", "value6");

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");
    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3")).isEqualTo("value3");
    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable4")).isEqualTo("value4");
    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4")).isEqualTo("value4");
    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable5")).isEqualTo("value5");
    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5")).isEqualTo("value5");
    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");
    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");

    runtimeService.removeVariablesLocal(currentTask.getExecutionId(), varsToDelete.keySet());

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isEqualTo("value1");
    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable2")).isEqualTo("value2");

    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable3"));
    assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable3"));
    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable4"));
    assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable4"));
    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable5"));
    assertNull(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable5"));

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");
    assertThat(runtimeService.getVariableLocal(currentTask.getExecutionId(), "variable6")).isEqualTo("value6");

    checkHistoricVariableUpdateEntity("variable3", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable4", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable5", processInstance.getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRemoveVariablesLocalNullExecutionId() {
    try {
      runtimeService.removeVariablesLocal(null, Collections.EMPTY_LIST);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("executionId is null", ae.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testUpdateVariables() {
    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "value1");
    modifications.put("variable2", "value2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable1");

    Map<String, Object> initialVariables = new HashMap<>();
    initialVariables.put("variable1", "initialValue");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", initialVariables);
    ((RuntimeServiceImpl) runtimeService).updateVariables(processInstance.getId(), modifications, deletions);

    assertNull(runtimeService.getVariable(processInstance.getId(), "variable1"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable2")).isEqualTo("value2");
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testUpdateVariablesLocal() {
    Map<String, Object> globalVars = new HashMap<>();
    globalVars.put("variable4", "value4");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", globalVars);

    Task currentTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> localVars = new HashMap<>();
    localVars.put("variable1", "value1");
    localVars.put("variable2", "value2");
    localVars.put("variable3", "value3");
    runtimeService.setVariablesLocal(currentTask.getExecutionId(), localVars);

    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    ((RuntimeServiceImpl) runtimeService).updateVariablesLocal(currentTask.getExecutionId(), modifications, deletions);

    assertThat(runtimeService.getVariable(currentTask.getExecutionId(), "variable1")).isEqualTo("anotherValue1");
    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable2"));
    assertNull(runtimeService.getVariable(currentTask.getExecutionId(), "variable3"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable4")).isEqualTo("value4");
  }

  @Deployment(resources={
          "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.catchPanicSignal.bpmn20.xml"
  })
  @Test
  public void testSignalEventReceived() {

    //////  test  signalEventReceived(String)

    startSignalCatchProcesses();
    // 12, because the signal catch is a scope
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(12);
    runtimeService.signalEventReceived("alert");
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(6);
    runtimeService.signalEventReceived("panic");
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

    //////  test  signalEventReceived(String, String)
    startSignalCatchProcesses();

    // signal the executions one at a time:
    for (int executions = 3; executions > 0; executions--) {
      List<Execution> page = runtimeService.createExecutionQuery()
        .signalEventSubscriptionName("alert")
        .listPage(0, 1);
      runtimeService.signalEventReceived("alert", page.get(0).getId());

      assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").count()).isEqualTo(executions - 1);
    }

    for (int executions = 3; executions > 0; executions-- ) {
      List<Execution> page = runtimeService.createExecutionQuery()
        .signalEventSubscriptionName("panic")
        .listPage(0, 1);
      runtimeService.signalEventReceived("panic", page.get(0).getId());

      assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("panic").count()).isEqualTo(executions - 1);
    }

  }

  @Deployment(resources={
          "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.catchAlertMessage.bpmn20.xml",
          "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.catchPanicMessage.bpmn20.xml"
  })
  @Test
  public void testMessageEventReceived() {

    startMessageCatchProcesses();
    // 12, because the signal catch is a scope
    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(12);

    // signal the executions one at a time:
    for (int executions = 3; executions > 0; executions--) {
      List<Execution> page = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("alert")
        .listPage(0, 1);
      runtimeService.messageEventReceived("alert", page.get(0).getId());

      assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("alert").count()).isEqualTo(executions - 1);
    }

    for (int executions = 3; executions > 0; executions-- ) {
      List<Execution> page = runtimeService.createExecutionQuery()
        .messageEventSubscriptionName("panic")
        .listPage(0, 1);
      runtimeService.messageEventReceived("panic", page.get(0).getId());

      assertThat(runtimeService.createExecutionQuery().messageEventSubscriptionName("panic").count()).isEqualTo(executions - 1);
    }

  }

 @Test
  public void testSignalEventReceivedNonExistingExecution() {
   try {
     runtimeService.signalEventReceived("alert", "nonexistingExecution");
     fail("exeception expected");
   }catch (ProcessEngineException e) {
     // this is good
     assertTrue(e.getMessage().contains("Cannot find execution with id 'nonexistingExecution'"));
   }
  }

 @Test
  public void testMessageEventReceivedNonExistingExecution() {
   try {
     runtimeService.messageEventReceived("alert", "nonexistingExecution");
     fail("exeception expected");
   }catch (ProcessEngineException e) {
     // this is good
     assertTrue(e.getMessage().contains("Execution with id 'nonexistingExecution' does not have a subscription to a message event with name 'alert'"));
   }
  }

 @Deployment(resources={
         "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.catchAlertSignal.bpmn20.xml"
 })
 @Test
  public void testExecutionWaitingForDifferentSignal() {
   runtimeService.startProcessInstanceByKey("catchAlertSignal");
   Execution execution = runtimeService.createExecutionQuery()
     .signalEventSubscriptionName("alert")
     .singleResult();
   var executionId = execution.getId();
   try {
     runtimeService.signalEventReceived("bogusSignal", executionId);
     fail("exeception expected");
   }catch (ProcessEngineException e) {
     // this is good
     assertTrue(e.getMessage().contains("has not subscribed to a signal event with name 'bogusSignal'"));
   }
  }

  private void startSignalCatchProcesses() {
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("catchAlertSignal");
      runtimeService.startProcessInstanceByKey("catchPanicSignal");
    }
  }

  private void startMessageCatchProcesses() {
    for (int i = 0; i < 3; i++) {
      runtimeService.startProcessInstanceByKey("catchAlertMessage");
      runtimeService.startProcessInstanceByKey("catchPanicMessage");
    }
  }

  // getActivityInstance Tests //////////////////////////////////

  @Test
  public void testActivityInstanceForNonExistingProcessInstanceId() {
    assertNull(runtimeService.getActivityInstance("some-nonexisting-id"));
  }

  @Test
  public void testActivityInstanceForNullProcessInstanceId() {
    try {
      runtimeService.getActivityInstance(null);
      fail("PEE expected!");
    } catch (ProcessEngineException engineException) {
      assertTrue(engineException.getMessage().contains("processInstanceId is null"));
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testActivityInstancePopulated() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "business-key");

    // validate properties of root
    ActivityInstance rootActInstance = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(rootActInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(rootActInstance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(rootActInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(processInstance.getId()).isEqualTo(rootActInstance.getExecutionIds()[0]);
    assertThat(rootActInstance.getActivityId()).isEqualTo(rootActInstance.getProcessDefinitionId());
    assertNull(rootActInstance.getParentActivityInstanceId());
    assertThat(rootActInstance.getActivityType()).isEqualTo("processDefinition");

    // validate properties of child:
    Task task = taskService.createTaskQuery().singleResult();
    ActivityInstance childActivityInstance = rootActInstance.getChildActivityInstances()[0];
    assertThat(childActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(childActivityInstance.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(childActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getExecutionId()).isEqualTo(childActivityInstance.getExecutionIds()[0]);
    assertThat(childActivityInstance.getActivityId()).isEqualTo("theTask");
    assertThat(childActivityInstance.getParentActivityInstanceId()).isEqualTo(rootActInstance.getId());
    assertThat(childActivityInstance.getActivityType()).isEqualTo("userTask");
    assertNotNull(childActivityInstance.getChildActivityInstances());
    assertNotNull(childActivityInstance.getChildTransitionInstances());
    assertThat(childActivityInstance.getChildActivityInstances().length).isEqualTo(0);
    assertThat(childActivityInstance.getChildTransitionInstances().length).isEqualTo(0);

  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForAsyncBeforeTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("theTask")
        .done());

    TransitionInstance asyncBeforeTransitionInstance = tree.getChildTransitionInstances()[0];
    assertThat(asyncBeforeTransitionInstance.getExecutionId()).isEqualTo(processInstance.getId());
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForConcurrentAsyncBeforeTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentTasksProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("theTask")
          .transition("asyncTask")
        .done());

    TransitionInstance asyncBeforeTransitionInstance = tree.getChildTransitionInstances()[0];
    String asyncExecutionId = managementService.createJobQuery().singleResult().getExecutionId();
    assertThat(asyncBeforeTransitionInstance.getExecutionId()).isEqualTo(asyncExecutionId);
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForAsyncBeforeStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("theStart")
        .done());

    TransitionInstance asyncBeforeTransitionInstance = tree.getChildTransitionInstances()[0];
    assertThat(asyncBeforeTransitionInstance.getExecutionId()).isEqualTo(processInstance.getId());
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForAsyncAfterTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());


    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("theTask")
        .done());

    TransitionInstance asyncAfterTransitionInstance = tree.getChildTransitionInstances()[0];
    assertThat(asyncAfterTransitionInstance.getExecutionId()).isEqualTo(processInstance.getId());
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForConcurrentAsyncAfterTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentTasksProcess");

    Task asyncTask = taskService.createTaskQuery().taskDefinitionKey("asyncTask").singleResult();
    assertNotNull(asyncTask);
    taskService.complete(asyncTask.getId());

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("theTask")
          .transition("asyncTask")
        .done());

    TransitionInstance asyncBeforeTransitionInstance = tree.getChildTransitionInstances()[0];
    String asyncExecutionId = managementService.createJobQuery().singleResult().getExecutionId();
    assertThat(asyncBeforeTransitionInstance.getExecutionId()).isEqualTo(asyncExecutionId);
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForAsyncAfterEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncEndEventProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .transition("theEnd")
        .done());

    TransitionInstance asyncAfterTransitionInstance = tree.getChildTransitionInstances()[0];
    assertThat(asyncAfterTransitionInstance.getExecutionId()).isEqualTo(processInstance.getId());
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForNestedAsyncBeforeTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .transition("theTask")
        .done());

    TransitionInstance asyncBeforeTransitionInstance = tree.getChildActivityInstances()[0]
        .getChildTransitionInstances()[0];
    String asyncExecutionId = managementService.createJobQuery().singleResult().getExecutionId();
    assertThat(asyncBeforeTransitionInstance.getExecutionId()).isEqualTo(asyncExecutionId);
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForNestedAsyncBeforeStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .transition("theSubProcessStart")
        .done());
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForNestedAsyncAfterTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());


    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .transition("theTask")
        .done());

    TransitionInstance asyncAfterTransitionInstance = tree.getChildActivityInstances()[0]
        .getChildTransitionInstances()[0];
    String asyncExecutionId = managementService.createJobQuery().singleResult().getExecutionId();
    assertThat(asyncAfterTransitionInstance.getExecutionId()).isEqualTo(asyncExecutionId);
  }

  @Deployment
  @Test
  public void testActivityInstanceTreeForNestedAsyncAfterEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncEndEventProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .beginScope("subProcess")
            .transition("theSubProcessEnd")
        .done());

    TransitionInstance asyncAfterTransitionInstance = tree.getChildActivityInstances()[0]
        .getChildTransitionInstances()[0];
    String asyncExecutionId = managementService.createJobQuery().singleResult().getExecutionId();
    assertThat(asyncAfterTransitionInstance.getExecutionId()).isEqualTo(asyncExecutionId);
  }

  /**
   * Test for CAM-3572
   */
  @Deployment
  @Test
  public void testActivityInstanceForConcurrentSubprocess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentSubProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    assertThat(tree).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("outerTask")
          .beginScope("subProcess")
            .activity("innerTask")
        .done());
  }

  @Deployment
  @Test
  public void testGetActivityInstancesForActivity() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // then
    ActivityInstance[] processActivityInstances = tree.getActivityInstances(definition.getId());
    assertThat(processActivityInstances.length).isEqualTo(1);
    assertThat(processActivityInstances[0].getId()).isEqualTo(tree.getId());
    assertThat(processActivityInstances[0].getActivityId()).isEqualTo(definition.getId());

    assertActivityInstances(tree.getActivityInstances("subProcess#multiInstanceBody"), 1, "subProcess#multiInstanceBody");
    assertActivityInstances(tree.getActivityInstances("subProcess"), 3, "subProcess");
    assertActivityInstances(tree.getActivityInstances("innerTask"), 3, "innerTask");

    ActivityInstance subProcessInstance = tree.getChildActivityInstances()[0].getChildActivityInstances()[0];
    assertActivityInstances(subProcessInstance.getActivityInstances("subProcess"), 1, "subProcess");

    ActivityInstance[] childInstances = subProcessInstance.getActivityInstances("innerTask");
    assertThat(childInstances.length).isEqualTo(1);
    assertThat(childInstances[0].getId()).isEqualTo(subProcessInstance.getChildActivityInstances()[0].getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testGetActivityInstancesForActivity.bpmn20.xml")
  @Test
  public void testGetInvalidActivityInstancesForActivity() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    try {
      tree.getActivityInstances(null);
      fail("exception expected");
    } catch (NullValueException e) {
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testGetActivityInstancesForActivity.bpmn20.xml")
  @Test
  public void testGetActivityInstancesForNonExistingActivity() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    ActivityInstance[] instances = tree.getActivityInstances("aNonExistingActivityId");
    assertNotNull(instances);
    assertThat(instances.length).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testGetTransitionInstancesForActivity() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");

    // complete one async task
    Job job = managementService.createJobQuery().listPage(0, 1).get(0);
    managementService.executeJob(job.getId());
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    // then
    assertThat(tree.getTransitionInstances("subProcess").length).isEqualTo(0);
    TransitionInstance[] asyncBeforeInstances = tree.getTransitionInstances("innerTask");
    assertThat(asyncBeforeInstances.length).isEqualTo(2);

    assertThat(asyncBeforeInstances[0].getActivityId()).isEqualTo("innerTask");
    assertThat(asyncBeforeInstances[1].getActivityId()).isEqualTo("innerTask");
    assertNotEquals(asyncBeforeInstances[0].getId(), asyncBeforeInstances[1].getId());

    TransitionInstance[] asyncEndEventInstances = tree.getTransitionInstances("theSubProcessEnd");
    assertThat(asyncEndEventInstances.length).isEqualTo(1);
    assertThat(asyncEndEventInstances[0].getActivityId()).isEqualTo("theSubProcessEnd");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testGetTransitionInstancesForActivity.bpmn20.xml")
  @Test
  public void testGetInvalidTransitionInstancesForActivity() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    try {
      tree.getTransitionInstances(null);
      fail("exception expected");
    } catch (NullValueException e) {
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testGetTransitionInstancesForActivity.bpmn20.xml")
  @Test
  public void testGetTransitionInstancesForNonExistingActivity() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSubprocess");

    ActivityInstance tree = runtimeService.getActivityInstance(instance.getId());

    TransitionInstance[] instances = tree.getTransitionInstances("aNonExistingActivityId");
    assertNotNull(instances);
    assertThat(instances.length).isEqualTo(0);
  }


  protected void assertActivityInstances(ActivityInstance[] instances, int expectedAmount, String expectedActivityId) {
    assertThat(instances.length).isEqualTo(expectedAmount);

    Set<String> instanceIds = new HashSet<>();

    for (ActivityInstance instance : instances) {
      assertThat(instance.getActivityId()).isEqualTo(expectedActivityId);
      instanceIds.add(instance.getId());
    }

    // ensure that all instances are unique
    assertThat(instanceIds).hasSize(expectedAmount);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testActivityInstanceNoIncidents() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    // when
    assertNotNull(tree);

    Incident[] incidents = tree.getActivityInstances("theTask")[0].getIncidents();

    // then
    assertThat(incidents.length).isEqualTo(0);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testActivityInstanceIncidents() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String executionId = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).active().singleResult().getId();
    Incident incident = runtimeService.createIncident("foo", executionId, "bar");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // assume
    assertThat(tree).hasTotalIncidents(1);

    Incident[] incidents = tree.getActivityInstances("theTask")[0].getIncidents();

    // then
    assertThat(incidents.length).isEqualTo(1);
    assertThat(incidents[0]).isEqualTo(incident);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testActivityInstanceNoIncidentIds() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // then
    String[] incidentIds = tree.getActivityInstances("theTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(0);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testActivityInstanceIncidentIds() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String executionId = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).active().singleResult().getId();
    Incident incident = runtimeService.createIncident("foo", executionId, "bar");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // then
    assertThat(tree).hasTotalIncidents(1);

    assertThat(tree.getActivityInstances("theTask").length).isEqualTo(1);
    String[] incidentIds = tree.getActivityInstances("theTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(1);
    assertThat(incidentIds[0]).isEqualTo(incident.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testActivityInstanceTreeForConcurrentAsyncAfterTask.bpmn20.xml"})
  public void testActivityInstanceIncidentConcurrentTasksProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentTasksProcess");
    String executionId = runtimeService.createExecutionQuery().activityId("theTask").active().singleResult().getId();
    Incident theTaskIncident = runtimeService.createIncident("foo", executionId, "bar");

    executionId = runtimeService.createExecutionQuery().activityId("asyncTask").active().singleResult().getId();
    Incident asyncTaskIncident = runtimeService.createIncident("foo", executionId, "bar");
    Incident anotherIncident = runtimeService.createIncident("foo", executionId, "bar");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // then
    assertThat(tree).hasTotalIncidents(3);

    assertThat(tree.getActivityInstances("theTask").length).isEqualTo(1);
    String[] incidentIds = tree.getActivityInstances("theTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(1);
    assertThat(incidentIds[0]).isEqualTo(theTaskIncident.getId());

    assertThat(tree.getActivityInstances("asyncTask").length).isEqualTo(1);
    incidentIds = tree.getActivityInstances("asyncTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(2);
    for (String incidentId : incidentIds) {
      if (!incidentId.equals(asyncTaskIncident.getId())
          && !incidentId.equals(anotherIncident.getId())) {
        fail("Expected: " + asyncTaskIncident.getId() + " or " + anotherIncident.getId()
            + " but it was " + incidentId);
      }
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testActivityInstanceForConcurrentSubprocess.bpmn20.xml"})
  public void testActivityInstanceIncidentConcurrentSubProcess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentSubProcess");
    String executionId = runtimeService.createExecutionQuery().activityId("outerTask").active().singleResult().getId();
    Incident outerTaskIncident = runtimeService.createIncident("foo", executionId, "bar");
    executionId = runtimeService.createExecutionQuery().activityId("innerTask").active().singleResult().getId();
    Incident innerTaskIncident = runtimeService.createIncident("foo", executionId, "bar");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // then
    assertThat(tree).hasTotalIncidents(2);

    assertThat(tree.getActivityInstances("outerTask").length).isEqualTo(1);
    String[] incidentIds = tree.getActivityInstances("outerTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(1);
    assertThat(incidentIds[0]).isEqualTo(outerTaskIncident.getId());

    assertThat(tree.getActivityInstances("innerTask").length).isEqualTo(1);
    incidentIds = tree.getActivityInstances("innerTask")[0].getIncidentIds();
    assertThat(incidentIds.length).isEqualTo(1);
    assertThat(incidentIds[0]).isEqualTo(innerTaskIncident.getId());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testGetActivityInstancesForActivity.bpmn20.xml"})
  public void testActivityInstanceIncidentMultiInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSubprocess");
    Execution execution = runtimeService.createExecutionQuery().activityId("innerTask").active().list().get(0);
    String executionId = execution.getId();
    Incident incident = runtimeService.createIncident("foo", executionId, "bar");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(tree);

    // then
    assertThat(tree).hasTotalIncidents(1);

    boolean innerTaskMatched = false;

    for (ActivityInstance activityInstance : tree.getActivityInstances("innerTask")) {
      if(activityInstance.getExecutionIds()[0].equals(executionId)) {
        String[] incidentIds = activityInstance.getIncidentIds();
        assertThat(incidentIds.length).isEqualTo(1);
        assertThat(incidentIds[0]).isEqualTo(incident.getId());
        innerTaskMatched = true;
      } else {
        assertThat(activityInstance.getIncidentIds().length).isEqualTo(0);
      }
    }

    assertTrue(innerTaskMatched);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneAsyncTask.bpmn")
  public void testActivityInstanceIncidentFailedJob() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasTotalIncidents(1);

    TransitionInstance transitionInstance = activityInstance.getTransitionInstances("theTask")[0];
    String[] incidents = transitionInstance.getIncidentIds();

    assertThat(incidents).containsExactly(incident.getId());
  }


  @Test
  public void testActivityInstanceIncidentFailedJobInSubProcess() {
    // given
    BpmnModelInstance process = ProcessModels.newModel("process")
      .startEvent()
      .subProcess("subProcess")
      .embeddedSubProcess()
      .startEvent()
      .userTask("task")
      .operatonAsyncBefore()
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 0);

    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasTotalIncidents(1);

    TransitionInstance transitionInstance = activityInstance.getTransitionInstances("task")[0];
    String[] incidents = transitionInstance.getIncidentIds();

    assertThat(incidents).containsExactly(incident.getId());
  }

  /**
   * It is debatable if the premise of this test (incident on non-leaf execution) is
   * intended behavior; if we decide that it is not (and enforce it),
   * then it is perfectly fine to change this test or remove it
   */
  @Test
  public void testActivityInstanceIncidentOnNonLeafWithScopeActivity() {
    // given
    BpmnModelInstance process = ProcessModels.newModel("process")
      .startEvent()
      .subProcess("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, CreateIncidentDelegate.class)
      .embeddedSubProcess()
      .startEvent()
      .userTask("task")
      .operatonInputParameter("foo", "bar")
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasTotalIncidents(1);

    ActivityInstance subProcessInstance = activityInstance.getActivityInstances("subProcess")[0];
    String[] incidents = subProcessInstance.getIncidentIds();

    assertThat(incidents).containsExactly(incident.getId());
  }

  @Test
  public void testActivityInstanceIncidentOnNonLeafWithNonScopeActivity() {
    // given
    BpmnModelInstance process = ProcessModels.newModel("process")
      .startEvent()
      .subProcess("subProcess")
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, CreateIncidentDelegate.class)
      .embeddedSubProcess()
      .startEvent()
      .userTask("task")
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasTotalIncidents(1);

    // while the incident references the sub process, the sub process
    // execution represents both the sub process and the user task (compacted tree).
    // In that case, incidents are always assigned to the leaf instance
    ActivityInstance subProcessInstance = activityInstance.getActivityInstances("task")[0];
    String[] incidents = subProcessInstance.getIncidentIds();

    assertThat(incidents).containsExactly(incident.getId());
  }

  public static class CreateIncidentDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
      runtimeService.createIncident("foo", execution.getId(), null);
    }

  }

  @Test
  public void testActivityInstanceOnAsyncAfterSequentialMultiInstance() {
    // given
    String processDefinitionKey = "process";
    SubProcessBuilder subprocessBuilder = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .subProcess("subprocess")
        .multiInstance()
          .sequential()
          .operatonAsyncAfter()
          .cardinality("3")
        .multiInstanceDone();

    BpmnModelInstance process = subprocessBuilder
        .embeddedSubProcess()
        .startEvent()
        .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("subprocess")
          .transition("subprocess")
      .done());
  }

  @Test
  public void testActivityInstanceOnAsyncAfterSequentialMultiInstance_NonScopeActivity() {
    // given
    String processDefinitionKey = "process";
    BpmnModelInstance process = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .manualTask("task")
        .multiInstance()
          .sequential()
          .operatonAsyncAfter()
          .cardinality("3")
        .multiInstanceDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("task")
          .transition("task")
      .done());
  }

  @Test
  public void testActivityInstanceOnAsyncAfterParallelMultiInstance() {
    // given
    String processDefinitionKey = "process";
    SubProcessBuilder subprocessBuilder = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .subProcess("subprocess")
        .multiInstance()
          .operatonAsyncAfter()
          .cardinality("3")
        .multiInstanceDone();

    BpmnModelInstance process = subprocessBuilder
        .embeddedSubProcess()
        .startEvent()
        .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
      .beginMiBody("subprocess")
        .transition("subprocess")
        .transition("subprocess")
        .transition("subprocess")
      .done());
  }


  @Test
  public void testActivityInstanceOnAsyncAfterScopeActivityWithOutgoingTransition() {
    // given
    String processDefinitionKey = "process";
    BpmnModelInstance process = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .subProcess("subprocess")
        .embeddedSubProcess()
        .startEvent()
        .manualTask("task")
          .operatonAsyncAfter()
          .operatonInputParameter("foo", "bar")
        .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
      .beginScope("subprocess")
        .transition("task")
      .done());
  }

  @Test
  public void testActivityInstanceOnAsyncAfterScopeActivityWithoutOutgoingTransition() {
    // given
    String processDefinitionKey = "process";
    BpmnModelInstance process = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .subProcess("subprocess")
        .embeddedSubProcess()
        .startEvent()
        .manualTask("task")
          .operatonAsyncAfter()
          .operatonInputParameter("foo", "bar")
        .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
      .beginScope("subprocess")
        .transition("task")
      .done());
  }

  @Test
  public void testActivityInstanceOnAsyncBeforeScopeActivity() {
    // given
    String processDefinitionKey = "process";
    BpmnModelInstance process = Bpmn.createExecutableProcess(processDefinitionKey)
      .startEvent()
      .subProcess("subprocess")
        .embeddedSubProcess()
        .startEvent()
        .manualTask("task")
          .operatonAsyncBefore()
          .operatonInputParameter("foo", "bar")
        .endEvent()
        .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
      .beginScope("subprocess")
        .transition("task")
      .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testChangeVariableType() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    DummySerializable dummy = new DummySerializable();
    runtimeService.setVariable(instance.getId(), "dummy", dummy);

    runtimeService.setVariable(instance.getId(), "dummy", 47);

    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variableInstance.getValue()).isEqualTo(47);
    assertThat(variableInstance.getTypeName()).isEqualTo(ValueType.INTEGER.getName());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testStartByKeyWithCaseInstanceId() {
    String caseInstanceId = "aCaseInstanceId";

    ProcessInstance firstInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", null, caseInstanceId);

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // load process instance from db
    firstInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(firstInstance.getId())
        .singleResult();

    assertNotNull(firstInstance);

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // the second possibility to start a process instance /////////////////////////////////////////////

    ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", null, caseInstanceId, null);

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // load process instance from db
    secondInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(secondInstance.getId())
        .singleResult();

    assertNotNull(secondInstance);

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testStartByIdWithCaseInstanceId() {
    String processDefinitionId = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("oneTaskProcess")
        .singleResult()
        .getId();

    String caseInstanceId = "aCaseInstanceId";
    ProcessInstance firstInstance = runtimeService.startProcessInstanceById(processDefinitionId, null, caseInstanceId);

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // load process instance from db
    firstInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(firstInstance.getId())
        .singleResult();

    assertNotNull(firstInstance);

    assertThat(firstInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // the second possibility to start a process instance /////////////////////////////////////////////

    ProcessInstance secondInstance = runtimeService.startProcessInstanceById(processDefinitionId, null, caseInstanceId, null);

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    // load process instance from db
    secondInstance = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(secondInstance.getId())
        .singleResult();

    assertNotNull(secondInstance);

    assertThat(secondInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testSetAbstractNumberValueFails() {
    var variables = Variables.createVariables().putValueTyped("var", Variables.numberValue(42));
    var variableMap = Variables.numberValue(42);
    try {
      runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
      fail("exception expected");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresentIgnoreCase("cannot serialize value of abstract type number", e.getMessage());
    }

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    var processInstanceId = processInstance.getId();

    try {
      runtimeService.setVariable(processInstanceId, "var", variableMap);
      fail("exception expected");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresentIgnoreCase("cannot serialize value of abstract type number", e.getMessage());
    }
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/messageStartEvent.bpmn20.xml")
  @Test
  public void testStartProcessInstanceByMessageWithEarlierVersionOfProcessDefinition() {
	  String deploymentId = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/api/runtime/messageStartEvent_version2.bpmn20.xml").deploy().getId();
	  ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).singleResult();

	  ProcessInstance processInstance = runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("startMessage", processDefinition.getId());

	  assertThat(processInstance).isNotNull();
	  assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

	  // clean up
	  repositoryService.deleteDeployment(deploymentId, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/messageStartEvent.bpmn20.xml")
  @Test
  public void testStartProcessInstanceByMessageWithLastVersionOfProcessDefinition() {
	  String deploymentId = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/api/runtime/messageStartEvent_version2.bpmn20.xml").deploy().getId();
	  ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().singleResult();

	  ProcessInstance processInstance = runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("newStartMessage", processDefinition.getId());

	  assertThat(processInstance).isNotNull();
	  assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

	  // clean up
	  repositoryService.deleteDeployment(deploymentId, true);
   }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/messageStartEvent.bpmn20.xml")
  @Test
  public void testStartProcessInstanceByMessageWithNonExistingMessageStartEvent() {
	  String deploymentId = null;
	  deploymentId = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/api/runtime/messageStartEvent_version2.bpmn20.xml").deploy().getId();
	  ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1).singleResult();
	  var processDefinitionId = processDefinition.getId();

    try {
		 runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("newStartMessage", processDefinitionId);

		 fail("exeception expected");
	 } catch(ProcessEngineException e) {
		 assertThat(e.getMessage()).contains("Cannot correlate message 'newStartMessage'");
	 }
	 finally {
		 // clean up
		 if(deploymentId != null){
			 repositoryService.deleteDeployment(deploymentId, true);
		 }
	 }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testActivityInstanceActivityNameProperty() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    ActivityInstance[] activityInstances = tree.getActivityInstances("theTask");
    assertThat(activityInstances.length).isEqualTo(1);

    ActivityInstance task = activityInstances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("my task");
  }

  @Deployment
  @Test
  public void testTransitionInstanceActivityNamePropertyBeforeTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("firstServiceTask");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("First Service Task");

    instances = tree.getTransitionInstances("secondServiceTask");
    task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("Second Service Task");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testTransitionInstanceActivityNamePropertyBeforeTask.bpmn20.xml")
  @Test
  public void testTransitionInstanceActivityTypePropertyBeforeTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("firstServiceTask");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("serviceTask");

    instances = tree.getTransitionInstances("secondServiceTask");
    task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("serviceTask");
  }

  @Deployment
  @Test
  public void testTransitionInstanceActivityNamePropertyAfterTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("firstServiceTask");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("First Service Task");

    instances = tree.getTransitionInstances("secondServiceTask");
    task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("Second Service Task");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testTransitionInstanceActivityNamePropertyAfterTask.bpmn20.xml")
  @Test
  public void testTransitionInstanceActivityTypePropertyAfterTask() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("firstServiceTask");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("serviceTask");

    instances = tree.getTransitionInstances("secondServiceTask");
    task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("serviceTask");
  }

  @Deployment
  @Test
  public void testTransitionInstanceActivityNamePropertyBeforeStartEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("start");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("The Start Event");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testTransitionInstanceActivityNamePropertyBeforeStartEvent.bpmn20.xml")
  @Test
  public void testTransitionInstanceActivityTypePropertyBeforeStartEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("start");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("startEvent");
  }

  @Deployment
  @Test
  public void testTransitionInstanceActivityNamePropertyAfterStartEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("start");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityName());
    assertThat(task.getActivityName()).isEqualTo("The Start Event");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testTransitionInstanceActivityNamePropertyAfterStartEvent.bpmn20.xml")
  @Test
  public void testTransitionInstanceActivityTypePropertyAfterStartEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);

    // then
    TransitionInstance[] instances = tree.getTransitionInstances("start");
    TransitionInstance task = instances[0];
    assertNotNull(task);
    assertNotNull(task.getActivityType());
    assertThat(task.getActivityType()).isEqualTo("startEvent");
  }

  //Test for a bug: when the process engine is rebooted the
  // cache is cleaned and the deployed process definition is
  // removed from the process cache. This led to problems because
  // the id wasn't fetched from the DB after a redeploy.
  @Test
  public void testStartProcessInstanceByIdAfterReboot() {

    // In case this test is run in a test suite, previous engines might
    // have been initialized and cached.  First we close the
    // existing process engines to make sure that the db is clean
    // and that there are no existing process engines involved.
    ProcessEngines.destroy();

    // Creating the DB schema (without building a process engine)
    ProcessEngineConfigurationImpl processEngineCfg = new StandaloneInMemProcessEngineConfiguration();
    processEngineCfg.setProcessEngineName("reboot-test-schema");
    processEngineCfg.setJdbcUrl("jdbc:h2:mem:activiti-reboot-test;DB_CLOSE_DELAY=1000");
    ProcessEngine schemaProcessEngine = processEngineCfg.buildProcessEngine();

    // Create process engine and deploy test process
    ProcessEngine processEngine = new StandaloneProcessEngineConfiguration()
      .setProcessEngineName("reboot-test")
      .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
      .setJdbcUrl("jdbc:h2:mem:activiti-reboot-test;DB_CLOSE_DELAY=1000")
      .setJobExecutorActivate(false)
      .buildProcessEngine();

    processEngine.getRepositoryService()
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
      .deploy();
      // verify existence of process definition
    List<ProcessDefinition> processDefinitions = processEngine
      .getRepositoryService()
      .createProcessDefinitionQuery()
      .list();

    assertThat(processDefinitions).hasSize(1);

    // Start a new Process instance
    ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitions.get(0).getId());
    String processInstanceId = processInstance.getId();
    assertNotNull(processInstance);

    // Close the process engine
    processEngine.close();
    assertNotNull(processEngine.getRuntimeService());

    // Reboot the process engine
    processEngine = new StandaloneProcessEngineConfiguration()
      .setProcessEngineName("reboot-test")
      .setDatabaseSchemaUpdate(org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
      .setJdbcUrl("jdbc:h2:mem:activiti-reboot-test;DB_CLOSE_DELAY=1000")
      .setJobExecutorActivate(false)
      .buildProcessEngine();

    // Check if the existing process instance is still alive
    processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    assertNotNull(processInstance);

    // Complete the task.  That will end the process instance
    TaskService processEngineTaskService = processEngine.getTaskService();
    Task task = processEngineTaskService
      .createTaskQuery()
      .list()
      .get(0);
    processEngineTaskService.complete(task.getId());

    // Check if the process instance has really ended.  This means that the process definition has
    // re-loaded into the process definition cache
    processInstance = processEngine
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult();
    assertNull(processInstance);

    // Extra check to see if a new process instance can be started as well
    processInstance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitions.get(0).getId());
    assertNotNull(processInstance);

    // close the process engine
    processEngine.close();

    // Cleanup schema
    schemaProcessEngine.close();
  }

  @Deployment
  @Test
  public void testVariableScope() {

    // After starting the process, the task in the subprocess should be active
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("test", "test");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", varMap);
    Task subProcessTask = taskService.createTaskQuery()
        .processInstanceId(pi.getId())
        .singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // get variables for execution id user task, should return the new value of variable test --> test2
    assertThat(runtimeService.getVariable(subProcessTask.getExecutionId(), "test")).isEqualTo("test2");
    assertThat(runtimeService.getVariables(subProcessTask.getExecutionId())).containsEntry("test", "test2");

    // get variables for process instance id, should return the initial value of variable test --> test
    assertThat(runtimeService.getVariable(pi.getId(), "test")).isEqualTo("test");
    assertThat(runtimeService.getVariables(pi.getId())).containsEntry("test", "test");

    runtimeService.setVariableLocal(subProcessTask.getExecutionId(), "test", "test3");

    // get variables for execution id user task, should return the new value of variable test --> test3
    assertThat(runtimeService.getVariable(subProcessTask.getExecutionId(), "test")).isEqualTo("test3");
    assertThat(runtimeService.getVariables(subProcessTask.getExecutionId())).containsEntry("test", "test3");

    // get variables for process instance id, should still return the initial value of variable test --> test
    assertThat(runtimeService.getVariable(pi.getId(), "test")).isEqualTo("test");
    assertThat(runtimeService.getVariables(pi.getId())).containsEntry("test", "test");

    runtimeService.setVariable(pi.getId(), "test", "test4");

    // get variables for execution id user task, should return the old value of variable test --> test3
    assertThat(runtimeService.getVariable(subProcessTask.getExecutionId(), "test")).isEqualTo("test3");
    assertThat(runtimeService.getVariables(subProcessTask.getExecutionId())).containsEntry("test", "test3");

    // get variables for process instance id, should also return the initial value of variable test --> test4
    assertThat(runtimeService.getVariable(pi.getId(), "test")).isEqualTo("test4");
    assertThat(runtimeService.getVariables(pi.getId())).containsEntry("test", "test4");

    // After completing the task in the subprocess,
    // the subprocess scope is destroyed and the complete process ends
    taskService.complete(subProcessTask.getId());
  }

  @Deployment
  @Test
  public void testBasicVariableOperations() {

    Date now = new Date();
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");
    byte[] bytes = "somebytes".getBytes();
    byte[] streamBytes = "morebytes".getBytes();

    // Start process instance with different types of variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "coca-cola");
    variables.put("dateVar", now);
    variables.put("nullVar", null);
    variables.put("serializableVar", serializable);
    variables.put("bytesVar", bytes);
    variables.put("byteStreamVar", new ByteArrayInputStream(streamBytes));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

    variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables)
            .containsEntry("stringVar", "coca-cola")
            .containsEntry("longVar", 928374L)
            .containsEntry("shortVar", (short) 123)
            .containsEntry("integerVar", 1234)
            .containsEntry("dateVar", now);
    assertNull(variables.get("nullVar"));
    assertThat(variables).containsEntry("serializableVar", serializable);
    assertArrayEquals(bytes, (byte[]) variables.get("bytesVar"));
    assertArrayEquals(streamBytes, (byte[]) variables.get("byteStreamVar"));
    assertThat(variables).hasSize(9);

    // Set all existing variables values to null
    runtimeService.setVariable(processInstance.getId(), "longVar", null);
    runtimeService.setVariable(processInstance.getId(), "shortVar", null);
    runtimeService.setVariable(processInstance.getId(), "integerVar", null);
    runtimeService.setVariable(processInstance.getId(), "stringVar", null);
    runtimeService.setVariable(processInstance.getId(), "dateVar", null);
    runtimeService.setVariable(processInstance.getId(), "nullVar", null);
    runtimeService.setVariable(processInstance.getId(), "serializableVar", null);
    runtimeService.setVariable(processInstance.getId(), "bytesVar", null);
    runtimeService.setVariable(processInstance.getId(), "byteStreamVar", null);

    variables = runtimeService.getVariables(processInstance.getId());
    assertNull(variables.get("longVar"));
    assertNull(variables.get("shortVar"));
    assertNull(variables.get("integerVar"));
    assertNull(variables.get("stringVar"));
    assertNull(variables.get("dateVar"));
    assertNull(variables.get("nullVar"));
    assertNull(variables.get("serializableVar"));
    assertNull(variables.get("bytesVar"));
    assertNull(variables.get("byteStreamVar"));
    assertThat(variables).hasSize(9);

    // Update existing variable values again, and add a new variable
    runtimeService.setVariable(processInstance.getId(), "new var", "hi");
    runtimeService.setVariable(processInstance.getId(), "longVar", 9987L);
    runtimeService.setVariable(processInstance.getId(), "shortVar", (short) 456);
    runtimeService.setVariable(processInstance.getId(), "integerVar", 4567);
    runtimeService.setVariable(processInstance.getId(), "stringVar", "colgate");
    runtimeService.setVariable(processInstance.getId(), "dateVar", now);
    runtimeService.setVariable(processInstance.getId(), "serializableVar", serializable);
    runtimeService.setVariable(processInstance.getId(), "bytesVar", bytes);
    runtimeService.setVariable(processInstance.getId(), "byteStreamVar", new ByteArrayInputStream(streamBytes));

    variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables)
            .containsEntry("new var", "hi")
            .containsEntry("longVar", 9987L)
            .containsEntry("shortVar", (short) 456)
            .containsEntry("integerVar", 4567)
            .containsEntry("stringVar", "colgate")
            .containsEntry("dateVar", now);
    assertNull(variables.get("nullVar"));
    assertThat(variables).containsEntry("serializableVar", serializable);
    assertArrayEquals(bytes, (byte[]) variables.get("bytesVar"));
    assertArrayEquals(streamBytes, (byte[]) variables.get("byteStreamVar"));
    assertThat(variables).hasSize(10);

    Collection<String> varFilter = new ArrayList<>(2);
    varFilter.add("stringVar");
    varFilter.add("integerVar");

    Map<String, Object> filteredVariables = runtimeService.getVariables(processInstance.getId(), varFilter);
    assertThat(filteredVariables).hasSize(2);
    assertTrue(filteredVariables.containsKey("stringVar"));
    assertTrue(filteredVariables.containsKey("integerVar"));

    // Try setting the value of the variable that was initially created with value 'null'
    runtimeService.setVariable(processInstance.getId(), "nullVar", "a value");
    Object newValue = runtimeService.getVariable(processInstance.getId(), "nullVar");
    assertNotNull(newValue);
    assertThat(newValue).isEqualTo("a value");

    // Try setting the value of the serializableVar to an integer value
    runtimeService.setVariable(processInstance.getId(), "serializableVar", 100);
    variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables).containsEntry("serializableVar", 100);

    // Try setting the value of the serializableVar back to a serializable value
    runtimeService.setVariable(processInstance.getId(), "serializableVar", serializable);
    variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables).containsEntry("serializableVar", serializable);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testBasicVariableOperations.bpmn20.xml"})
  @Test
  public void testOnlyChangeType() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", 1234);
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableName("aVariable");

    VariableInstance variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.INTEGER.getName());

    runtimeService.setVariable(pi.getId(), "aVariable", 1234L);
    variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.LONG.getName());

    runtimeService.setVariable(pi.getId(), "aVariable", (short)1234);
    variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.SHORT.getName());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testBasicVariableOperations.bpmn20.xml"})
  @Test
  public void testChangeTypeFromSerializableUsingApi() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", new SerializableVariable("foo"));
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableName("aVariable");

    VariableInstance variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.OBJECT.getName());

    runtimeService.setVariable(pi.getId(), "aVariable", null);
    variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.NULL.getName());

  }

  @Deployment
  @Test
  public void testChangeSerializableInsideEngine() {

    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();

    SerializableVariable variable = (SerializableVariable) taskService.getVariable(task.getId(), "variableName");
    assertNotNull(variable);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testBasicVariableOperations.bpmn20.xml"})
  @Test
  public void testChangeToSerializableUsingApi() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "test");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskAssigneeProcess", variables);

    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery().variableName("aVariable");

    VariableInstance variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.STRING.getName());

    runtimeService.setVariable(processInstance.getId(), "aVariable", new SerializableVariable("foo"));
    variable = query.singleResult();
    assertThat(variable.getTypeName()).isEqualTo(ValueType.OBJECT.getName());

  }

  @Deployment
  @Test
  public void testGetVariableInstancesFromVariableScope() {

    VariableMap variables = createVariables()
      .putValue("anIntegerVariable", 1234)
      .putValue("anObjectValue", objectValue(new SimpleSerializableBean(10)).serializationDataFormat(Variables.SerializationDataFormats.JAVA))
      .putValue("anUntypedObjectValue", new SimpleSerializableBean(30));

    runtimeService.startProcessInstanceByKey("testProcess", variables);

    // assertions are part of the java delegate AssertVariableInstancesDelegate
    // only there we can access the VariableScope methods
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testSetVariableInScope.bpmn20.xml")
  @Test
  public void testSetVariableInScopeExplicitUpdate() {
    // when a process instance is started and the task after the subprocess reached
    runtimeService.startProcessInstanceByKey("testProcess",
        Collections.singletonMap("shouldExplicitlyUpdateVariable", true));

    // then there should be only the "shouldExplicitlyUpdateVariable" variable
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();
    assertNotNull(variableInstance);
    assertThat(variableInstance.getName()).isEqualTo("shouldExplicitlyUpdateVariable");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/runtime/RuntimeServiceTest.testSetVariableInScope.bpmn20.xml")
  @Test
  public void testSetVariableInScopeImplicitUpdate() {
    // when a process instance is started and the task after the subprocess reached
    runtimeService.startProcessInstanceByKey("testProcess",
        Collections.singletonMap("shouldExplicitlyUpdateVariable", true));

    // then there should be only the "shouldExplicitlyUpdateVariable" variable
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();
    assertNotNull(variableInstance);
    assertThat(variableInstance.getName()).isEqualTo("shouldExplicitlyUpdateVariable");
  }

  @Deployment
  @Test
  public void testUpdateVariableInProcessWithoutWaitstate() {
    // when a process instance is started
    runtimeService.startProcessInstanceByKey("oneScriptTaskProcess",
        Collections.singletonMap("var", new SimpleSerializableBean(10)));

    // then it should succeeds successfully
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertNull(processInstance);
  }

  @Deployment
  @Test
  public void testSetUpdateAndDeleteComplexVariable() {
    // when a process instance is started
    runtimeService.startProcessInstanceByKey("oneUserTaskProcess",
        Collections.singletonMap("var", new SimpleSerializableBean(10)));

    // then it should wait at the user task
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertNotNull(processInstance);
  }

  @Deployment
  @Test
  public void testRollback() {
    try {
      runtimeService.startProcessInstanceByKey("RollbackProcess");

      fail("Starting the process instance should throw an exception");

    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Buzzz");
    }

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/trivial.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/rollbackAfterSubProcess.bpmn20.xml"})
  @Test
  public void testRollbackAfterSubProcess() {
    try {
      runtimeService.startProcessInstanceByKey("RollbackAfterSubProcess");

      fail("Starting the process instance should throw an exception");

    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Buzzz");
    }

    assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(0);

  }

  @Test
  public void testGetActivityInstanceForCompletedInstanceInDelegate() {
    // given
    BpmnModelInstance deletingProcess = Bpmn.createExecutableProcess("process1")
        .startEvent()
        .userTask()
        .serviceTask()
        .operatonClass(DeleteInstanceDelegate.class.getName())
        .userTask()
        .endEvent()
        .done();
    BpmnModelInstance processToDelete = Bpmn.createExecutableProcess("process2")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(deletingProcess, processToDelete);

    ProcessInstance instanceToDelete = runtimeService.startProcessInstanceByKey("process2");
    ProcessInstance deletingInstance = runtimeService.startProcessInstanceByKey("process1",
        Variables.createVariables().putValue("instanceToComplete", instanceToDelete.getId()));

    Task deleteTrigger = taskService.createTaskQuery().processInstanceId(deletingInstance.getId()).singleResult();

    // when
    taskService.complete(deleteTrigger.getId());

    // then
    boolean activityInstanceNull =
        (Boolean) runtimeService.getVariable(deletingInstance.getId(), "activityInstanceNull");
    assertTrue(activityInstanceNull);
  }

  public static class DeleteInstanceDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
      TaskService taskService = execution.getProcessEngineServices().getTaskService();

      String instanceToDelete = (String) execution.getVariable("instanceToComplete");
      Task taskToTrigger = taskService.createTaskQuery().processInstanceId(instanceToDelete).singleResult();
      taskService.complete(taskToTrigger.getId());

      ActivityInstance activityInstance = runtimeService.getActivityInstance(instanceToDelete);
      execution.setVariable("activityInstanceNull", activityInstance == null);
    }

  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testDeleteProcessInstanceWithSubprocessInstances() {
    // given a process instance with subprocesses
    BpmnModelInstance calling = prepareComplexProcess("A", "B", "A");

    BpmnModelInstance calledA = prepareSimpleProcess("A");
    BpmnModelInstance calledB = prepareSimpleProcess("B");

    testRule.deploy(calling, calledA, calledB);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("calling");
    List<ProcessInstance> subInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(instance.getId()).list();

    // when the process instance is deleted and we do not skip sub processes
    String id = instance.getId();
    runtimeService.deleteProcessInstance(id, "test_purposes", false, true, false, false);

    // then
    testRule.assertProcessEnded(id);

    for (ProcessInstance subInstance : subInstances) {
      testRule.assertProcessEnded(subInstance.getId());
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  public void testDeleteProcessInstanceWithoutSubprocessInstances() {
    // given a process instance with subprocesses
    BpmnModelInstance calling = prepareComplexProcess("A", "B", "C");

    BpmnModelInstance calledA = prepareSimpleProcess("A");
    BpmnModelInstance calledB = prepareSimpleProcess("B");
    BpmnModelInstance calledC = prepareSimpleProcess("C");

    testRule.deploy(calling, calledA, calledB, calledC);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("calling");
    List<ProcessInstance> subInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();

    // when the process instance is deleted and we do skip sub processes
    String id = processInstance.getId();
    runtimeService.deleteProcessInstance(id, "test_purposes", false, true, false, true);

    // then
    testRule.assertProcessEnded(id);

    for (ProcessInstance subInstance : subInstances) {
      testRule.assertProcessNotEnded(subInstance.getId());
    }
  }

  @Test
  public void testDeleteProcessInstancesWithoutSubprocessInstances() {
    // given a process instance with subprocess
    String callingProcessKey = "calling";
    String calledProcessKey = "called";
    BpmnModelInstance calling = prepareCallingProcess(callingProcessKey, calledProcessKey);
    BpmnModelInstance called = prepareSimpleProcess(calledProcessKey);
    testRule.deploy(calling, called);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(callingProcessKey);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(callingProcessKey);

    List<ProcessInstance> subprocessList = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
    subprocessList.addAll(runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance2.getId()).list());

    // when
    runtimeService.deleteProcessInstances(Arrays.asList(processInstance.getId(), processInstance2.getId()), null, false, false, true, false);

    // then
    testRule.assertProcessEnded(processInstance.getId());
    testRule.assertProcessEnded(processInstance2.getId());

    for (ProcessInstance instance : subprocessList) {
      testRule.assertProcessNotEnded(instance.getId());
    }
  }

  @Test
  public void testDeleteProcessInstancesWithSubprocessInstances() {
    // given a process instance with subprocess
    String callingProcessKey = "calling";
    String calledProcessKey = "called";
    BpmnModelInstance calling = prepareCallingProcess(callingProcessKey, calledProcessKey);
    BpmnModelInstance called = prepareSimpleProcess(calledProcessKey);
    testRule.deploy(calling, called);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(callingProcessKey);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(callingProcessKey);

    List<ProcessInstance> subprocessList = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
    subprocessList.addAll(runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance2.getId()).list());

    // when
    runtimeService.deleteProcessInstances(Arrays.asList(processInstance.getId(), processInstance2.getId()), null, false, false, false, false);

    // then
    testRule.assertProcessEnded(processInstance.getId());
    testRule.assertProcessEnded(processInstance2.getId());

    for (ProcessInstance subprocess : subprocessList) {
      testRule.assertProcessEnded(subprocess.getId());
    }
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    Map<String, Object> variables = runtimeService.getVariables(processInstanceId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesTypedByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    Map<String, Object> variables = runtimeService.getVariablesTyped(processInstanceId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesLocalByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    Map<String, Object> variables = runtimeService.getVariablesLocal(processInstanceId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesLocalTypedByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    Map<String, Object> variables = runtimeService.getVariablesLocalTyped(processInstanceId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  private BpmnModelInstance prepareComplexProcess(String calledProcessA,String calledProcessB,String calledProcessC) {
    return Bpmn.createExecutableProcess("calling")
          .startEvent()
          .parallelGateway("fork1")
            .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .parallelGateway("fork2")
                .callActivity("callingA")
                  .calledElement(calledProcessA)
              .endEvent("endA")

              .moveToNode("fork2")
              .callActivity("callingB")
                .calledElement(calledProcessB)
              .endEvent()
            .subProcessDone()

          .moveToNode("fork1")
            .callActivity("callingC")
              .calledElement(calledProcessC)
            .endEvent()

        .done();
  }

  private BpmnModelInstance prepareSimpleProcess(String name) {
    return Bpmn.createExecutableProcess(name)
        .startEvent()
        .userTask("Task" + name)
        .endEvent()
        .done();
  }

  private BpmnModelInstance prepareCallingProcess(String callingProcess, String calledProcess) {
    return Bpmn.createExecutableProcess(callingProcess)
          .startEvent()
          .callActivity()
            .calledElement(calledProcess)
          .endEvent()
          .done();
  }

}
