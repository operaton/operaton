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
package org.operaton.bpm.engine.test.api.history;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionOutputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.dmn.businessruletask.TestPojo;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Svetlana Dorokhova
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class BulkHistoryDeleteTest {

  protected static final String ONE_TASK_PROCESS = "oneTaskProcess";

  public static final int PROCESS_INSTANCE_COUNT = 5;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private HistoryService historyService;
  private TaskService taskService;
  private RuntimeService runtimeService;
  private FormService formService;
  private ExternalTaskService externalTaskService;
  private CaseService caseService;
  private IdentityService identityService;

  public static final String USER_ID = "demo";

  @BeforeEach
  void init() {
    identityService.setAuthenticatedUserId(USER_ID);
  }

  @BeforeEach
  void enableDmnFeelLegacyBehavior() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
        .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
  }

  @AfterEach
  void disableDmnFeelLegacyBehavior() {

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoryTaskIdentityLink() {
    //given
    final List<String> ids = prepareHistoricProcesses();
    List<Task> taskList = taskService.createTaskQuery().list();
    taskService.addUserIdentityLink(taskList.get(0).getId(), "someUser", IdentityLinkType.ASSIGNEE);

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricIdentityLinkLogQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoryActivityInstances() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupTaskAttachmentWithContent() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    List<Task> taskList = taskService.createTaskQuery().list();

    String taskWithAttachmentId = taskList.get(0).getId();
    createTaskAttachmentWithContent(taskWithAttachmentId);
    //remember contentId
    final String contentId = findAttachmentContentId(taskService.getTaskAttachments(taskWithAttachmentId));

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(taskService.getTaskAttachments(taskWithAttachmentId)).isEmpty();
    //check that attachment content was removed
    verifyByteArraysWereRemoved(contentId);
  }

  private String findAttachmentContentId(List<Attachment> attachments) {
    assertThat(attachments).hasSize(1);
    return ((AttachmentEntity) attachments.get(0)).getContentId();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupProcessInstanceAttachmentWithContent() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    String processInstanceWithAttachmentId = ids.get(0);
    createProcessInstanceAttachmentWithContent(processInstanceWithAttachmentId);
    //remember contentId
    final String contentId = findAttachmentContentId(taskService.getProcessInstanceAttachments(processInstanceWithAttachmentId));

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(taskService.getProcessInstanceAttachments(processInstanceWithAttachmentId)).isEmpty();
    //check that attachment content was removed
    verifyByteArraysWereRemoved(contentId);
  }

  private void createProcessInstanceAttachmentWithContent(String processInstanceId) {
    taskService
        .createAttachment("web page", null, processInstanceId, "weatherforcast", "temperatures and more", new ByteArrayInputStream("someContent".getBytes()));

    List<Attachment> taskAttachments = taskService.getProcessInstanceAttachments(processInstanceId);
    assertThat(taskAttachments).hasSize(1);
    assertThat(taskService.getAttachmentContent(taskAttachments.get(0).getId())).isNotNull();
  }

  private void createTaskAttachmentWithContent(String taskId) {
    taskService.createAttachment("web page", taskId, null, "weatherforcast", "temperatures and more", new ByteArrayInputStream("someContent".getBytes()));

    List<Attachment> taskAttachments = taskService.getTaskAttachments(taskId);
    assertThat(taskAttachments).hasSize(1);
    assertThat(taskService.getAttachmentContent(taskAttachments.get(0).getId())).isNotNull();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupTaskComment() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    List<Task> taskList = taskService.createTaskQuery().list();

    String taskWithCommentId = taskList.get(2).getId();
    taskService.createComment(taskWithCommentId, null, "Some comment");

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(taskService.getTaskComments(taskWithCommentId)).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupProcessInstanceComment() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    String processInstanceWithCommentId = ids.get(0);
    taskService.createComment(null, processInstanceWithCommentId, "Some comment");

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(taskService.getProcessInstanceComments(processInstanceWithCommentId)).isEmpty();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoricVariableInstancesAndHistoricDetails() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    List<Task> taskList = taskService.createTaskQuery().list();

    taskService.setVariables(taskList.get(0).getId(), getVariables());

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoryTaskForm() {
    //given
    final List<String> ids = prepareHistoricProcesses();

    List<Task> taskList = taskService.createTaskQuery().list();

    formService.submitTaskForm(taskList.get(0).getId(), getVariables());

    for (ProcessInstance processInstance : runtimeService.createProcessInstanceQuery().list()) {
      runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), null);
    }

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  void testCleanupHistoricExternalTaskLog() {
    //given
    final List<String> ids = prepareHistoricProcesses("oneExternalTaskProcess");

    String workerId = "aWrokerId";
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, workerId).topic("externalTaskTopic", 10000L).execute();

    externalTaskService.handleFailure(tasks.get(0).getId(), workerId, "errorMessage", "exceptionStackTrace", 5, 3000L);

    //remember errorDetailsByteArrayId
    final String errorDetailsByteArrayId = findErrorDetailsByteArrayId("errorMessage");

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricExternalTaskLogQuery().count()).isZero();
    //check that ByteArray was removed
    verifyByteArraysWereRemoved(errorDetailsByteArrayId);
  }

  private String findErrorDetailsByteArrayId(String errorMessage) {
    final List<HistoricExternalTaskLog> historicExternalTaskLogs = historyService.createHistoricExternalTaskLogQuery().errorMessage(errorMessage).list();
    assertThat(historicExternalTaskLogs).hasSize(1);

    return ((HistoricExternalTaskLogEntity) historicExternalTaskLogs.get(0)).getErrorDetailsByteArrayId();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  void testCleanupHistoricIncidents() {
    //given
    List<String> ids = prepareHistoricProcesses("failingProcess");

    testRule.executeAvailableJobs();

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("failingProcess").count()).isZero();
    assertThat(historyService.createHistoricIncidentQuery().count()).isZero();

  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/mgmt/IncidentTest.testShouldCreateOneIncident.bpmn"})
  void testCleanupHistoricJobLogs() {
    //given
    List<String> ids = prepareHistoricProcesses("failingProcess", null, 1);

    testRule.executeAvailableJobs();

    runtimeService.deleteProcessInstances(ids, null, true, true);

    List<String> byteArrayIds = findExceptionByteArrayIds();

    //when
    historyService.deleteHistoricProcessInstancesBulk(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("failingProcess").count()).isZero();
    assertThat(historyService.createHistoricJobLogQuery().count()).isZero();

    verifyByteArraysWereRemoved(byteArrayIds.toArray(new String[] {}));
  }

  private List<String> findExceptionByteArrayIds() {
    List<String> exceptionByteArrayIds = new ArrayList<>();
    List<HistoricJobLog> historicJobLogs = historyService.createHistoricJobLogQuery().list();
    for (HistoricJobLog historicJobLog : historicJobLogs) {
      HistoricJobLogEventEntity historicJobLogEventEntity = (HistoricJobLogEventEntity) historicJobLog;
      if (historicJobLogEventEntity.getExceptionByteArrayId() != null) {
        exceptionByteArrayIds.add(historicJobLogEventEntity.getExceptionByteArrayId());
      }
    }
    return exceptionByteArrayIds;
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"})
  void testCleanupHistoryDecisionData() {
    //given
    List<String> ids = prepareHistoricProcesses("testProcess", Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37)));

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //remember input and output ids
    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().includeInputs().includeOutputs().list();
    final List<String> inputIds = new ArrayList<>();
    final List<String> inputByteArrayIds = new ArrayList<>();
    collectHistoricDecisionInputIds(historicDecisionInstances, inputIds, inputByteArrayIds);

    final List<String> outputIds = new ArrayList<>();
    final List<String> outputByteArrayIds = new ArrayList<>();
    collectHistoricDecisionOutputIds(historicDecisionInstances, outputIds, outputByteArrayIds);

    //when
    historyService.deleteHistoricDecisionInstancesBulk(extractIds(historicDecisionInstances));

    //then
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();

    //check that decision inputs and outputs were removed
    assertDataDeleted(inputIds, inputByteArrayIds, outputIds, outputByteArrayIds);


    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery()
      .operationType(OPERATION_TYPE_DELETE_HISTORY)
      .property("nrOfInstances")
      .list();

    assertThat(userOperationLogEntries).hasSize(1);

    UserOperationLogEntry entry = userOperationLogEntries.get(0);
    assertThat(entry.getNewValue()).isEqualTo(String.valueOf(historicDecisionInstances.size()));
    assertThat(entry.getCategory()).isEqualTo(CATEGORY_OPERATOR);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionRef.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"})
  void testCleanupFakeHistoryDecisionData() {
    //given
    List<String> ids = List.of("aFake");

    //when
    historyService.deleteHistoricDecisionInstancesBulk(ids);

    //then expect no exception
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();
  }

  void assertDataDeleted(final List<String> inputIds, final List<String> inputByteArrayIds, final List<String> outputIds,
    final List<String> outputByteArrayIds) {
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      for (String inputId : inputIds) {
        assertThat(commandContext.getDbEntityManager().selectById(HistoricDecisionInputInstanceEntity.class, inputId)).isNull();
      }
      for (String inputByteArrayId : inputByteArrayIds) {
        assertThat(commandContext.getDbEntityManager().selectById(ByteArrayEntity.class, inputByteArrayId)).isNull();
      }
      for (String outputId : outputIds) {
        assertThat(commandContext.getDbEntityManager().selectById(HistoricDecisionOutputInstanceEntity.class, outputId)).isNull();
      }
      for (String outputByteArrayId : outputByteArrayIds) {
        assertThat(commandContext.getDbEntityManager().selectById(ByteArrayEntity.class, outputByteArrayId)).isNull();
      }
      return null;
    });
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/history/testDmnWithPojo.dmn11.xml"})
  void testCleanupHistoryStandaloneDecisionData() {
    //given
    for (int i = 0; i < 5; i++) {
      engineRule.getDecisionService().evaluateDecisionByKey("testDecision").variables(Variables.createVariables().putValue("pojo", new TestPojo("okay", 13.37))).evaluate();
    }

    //remember input and output ids
    List<HistoricDecisionInstance> historicDecisionInstances = historyService.createHistoricDecisionInstanceQuery().includeInputs().includeOutputs().list();
    final List<String> inputIds = new ArrayList<>();
    final List<String> inputByteArrayIds = new ArrayList<>();
    collectHistoricDecisionInputIds(historicDecisionInstances, inputIds, inputByteArrayIds);

    final List<String> outputIds = new ArrayList<>();
    final List<String> outputByteArrayIds = new ArrayList<>();
    collectHistoricDecisionOutputIds(historicDecisionInstances, outputIds, outputByteArrayIds);

    List<String> decisionInstanceIds = extractIds(historicDecisionInstances);

    //when
    historyService.deleteHistoricDecisionInstancesBulk(decisionInstanceIds);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("testProcess").count()).isZero();
    assertThat(historyService.createHistoricDecisionInstanceQuery().count()).isZero();

    //check that decision inputs and outputs were removed
    assertDataDeleted(inputIds, inputByteArrayIds, outputIds, outputByteArrayIds);

  }

  private List<String> extractIds(List<HistoricDecisionInstance> historicDecisionInstances) {
    List<String> decisionInstanceIds = new ArrayList<>();
    for (HistoricDecisionInstance historicDecisionInstance: historicDecisionInstances) {
      decisionInstanceIds.add(historicDecisionInstance.getId());
    }
    return decisionInstanceIds;
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoryEmptyProcessIdsException() {
    //given
    final List<String> ids = prepareHistoricProcesses();
    runtimeService.deleteProcessInstances(ids, null, true, true);

    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesBulk(null),
        "Empty process instance ids exception was expected")
            .isInstanceOf(BadUserRequestException.class)
            .hasMessageContaining("processInstanceIds is null");

    List<String> emptyProcessInstanceIds = emptyList();
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesBulk(emptyProcessInstanceIds),
        "Empty process instance ids exception was expected")
            .isInstanceOf(BadUserRequestException.class)
            .hasMessageContaining("processInstanceIds is empty");

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testCleanupHistoryProcessesNotFinishedException() {
    //given
    final List<String> ids = prepareHistoricProcesses();
    runtimeService.deleteProcessInstances(ids.subList(1, ids.size()), null, true, true);

    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstancesBulk(ids), "Not all processes are finished exception was expected")
          .isInstanceOf(BadUserRequestException.class)
          .hasMessageContaining("Process instance is still running, cannot delete historic process instance");
  }

  private void collectHistoricDecisionInputIds(List<HistoricDecisionInstance> historicDecisionInstances, List<String> historicDecisionInputIds, List<String> inputByteArrayIds) {
    for (HistoricDecisionInstance historicDecisionInstance : historicDecisionInstances) {
      for (HistoricDecisionInputInstance inputInstanceEntity : historicDecisionInstance.getInputs()) {
        historicDecisionInputIds.add(inputInstanceEntity.getId());
        final String byteArrayValueId = ((HistoricDecisionInputInstanceEntity) inputInstanceEntity).getByteArrayValueId();
        if (byteArrayValueId != null) {
          inputByteArrayIds.add(byteArrayValueId);
        }
      }
    }
    assertThat(historicDecisionInputIds).hasSize(PROCESS_INSTANCE_COUNT);
  }

  private void collectHistoricDecisionOutputIds(List<HistoricDecisionInstance> historicDecisionInstances, List<String> historicDecisionOutputIds, List<String> outputByteArrayId) {
    for (HistoricDecisionInstance historicDecisionInstance : historicDecisionInstances) {
      for (HistoricDecisionOutputInstance outputInstanceEntity : historicDecisionInstance.getOutputs()) {
        historicDecisionOutputIds.add(outputInstanceEntity.getId());
        final String byteArrayValueId = ((HistoricDecisionOutputInstanceEntity) outputInstanceEntity).getByteArrayValueId();
        if (byteArrayValueId != null) {
          outputByteArrayId.add(byteArrayValueId);
        }
      }
    }
    assertThat(historicDecisionOutputIds).hasSize(PROCESS_INSTANCE_COUNT);
  }

  private List<String> prepareHistoricProcesses() {
    return prepareHistoricProcesses(ONE_TASK_PROCESS);
  }

  private List<String> prepareHistoricProcesses(String businessKey) {
    return prepareHistoricProcesses(businessKey, null);
  }

  private List<String> prepareHistoricProcesses(String businessKey, VariableMap variables) {
    return prepareHistoricProcesses(businessKey, variables, PROCESS_INSTANCE_COUNT);
  }

  private List<String> prepareHistoricProcesses(String businessKey, VariableMap variables, Integer processInstanceCount) {
    List<String> processInstanceIds = new ArrayList<>();

    for (int i = 0; i < processInstanceCount; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(businessKey, variables);
      processInstanceIds.add(processInstance.getId());
    }

    return processInstanceIds;
  }

  private void verifyByteArraysWereRemoved(final String... errorDetailsByteArrayIds) {
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      for (String errorDetailsByteArrayId : errorDetailsByteArrayIds) {
        assertThat(commandContext.getDbEntityManager().selectOne("selectByteArray", errorDetailsByteArrayId)).isNull();
      }
      return null;
    });
  }

  private VariableMap getVariables() {
    return Variables.createVariables()
        .putValue("aVariableName", "aVariableValue")
        .putValue("pojoVariableName", new TestPojo("someValue", 111.));
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstance() {
    // given
    // create case instances
    int instanceCount = 10;
    List<String> caseInstanceIds = prepareHistoricCaseInstance(instanceCount);

    // assume
    List<HistoricCaseInstance> caseInstanceList = historyService.createHistoricCaseInstanceQuery().list();
    assertThat(caseInstanceList).hasSize(instanceCount);

    // when
    historyService.deleteHistoricCaseInstancesBulk(caseInstanceIds);

    // then
    assertThat(historyService.createHistoricCaseInstanceQuery().count()).isZero();
    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseActivityInstance() {
    // given
    // create case instance
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();
    terminateAndCloseCaseInstance(caseInstanceId, null);

    // assume
    List<HistoricCaseActivityInstance> activityInstances = historyService.createHistoricCaseActivityInstanceQuery().list();
    assertThat(activityInstances).hasSize(1);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstanceId));

    // then
    activityInstances = historyService.createHistoricCaseActivityInstanceQuery().list();
    assertThat(activityInstances).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTask() {
    // given
    // create case instance
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();
    terminateAndCloseCaseInstance(caseInstanceId, null);

    // assume
    List<HistoricTaskInstance> taskInstances = historyService.createHistoricTaskInstanceQuery().list();
    assertThat(taskInstances).hasSize(1);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstanceId));

    // then
    taskInstances = historyService.createHistoricTaskInstanceQuery().list();
    assertThat(taskInstances).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTaskComment() {
    // given
    // create case instance
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    Task task = taskService.createTaskQuery().singleResult();
    taskService.createComment(task.getId(), null, "This is a comment...");

    // assume
    List<Comment> comments = taskService.getTaskComments(task.getId());
    assertThat(comments).hasSize(1);
    terminateAndCloseCaseInstance(caseInstanceId, null);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstanceId));

    // then
    comments = taskService.getTaskComments(task.getId());
    assertThat(comments).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTaskDetails() {
    // given
    // create case instance
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    Task task = taskService.createTaskQuery().singleResult();

    taskService.setVariable(task.getId(), "boo", new TestPojo("foo", 123.0));
    taskService.setVariable(task.getId(), "goo", 9);
    taskService.setVariable(task.getId(), "boo", new TestPojo("foo", 321.0));


    // assume
    List<HistoricDetail> detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).hasSize(3);
    terminateAndCloseCaseInstance(caseInstance.getId(), taskService.getVariables(task.getId()));

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstance.getId()));

    // then
    detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTaskIdentityLink() {
    // given
    // create case instance
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    Task task = taskService.createTaskQuery().singleResult();

    // assume
    taskService.addGroupIdentityLink(task.getId(), "accounting", IdentityLinkType.CANDIDATE);
    int identityLinksForTask = taskService.getIdentityLinksForTask(task.getId()).size();
    assertThat(identityLinksForTask).isEqualTo(1);
    terminateAndCloseCaseInstance(caseInstanceId, null);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstanceId));

    // then
    List<HistoricIdentityLinkLog> historicIdentityLinkLog = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinkLog).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTaskAttachmentByteArray() {
    // given
    // create case instance
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    taskService.createAttachment("foo", taskId, null, "something", null, new ByteArrayInputStream("someContent".getBytes()));

    // assume
    List<Attachment> attachments = taskService.getTaskAttachments(taskId);
    assertThat(attachments).hasSize(1);
    String contentId = findAttachmentContentId(attachments);
    terminateAndCloseCaseInstance(caseInstance.getId(), null);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstance.getId()));

    // then
    attachments = taskService.getTaskAttachments(taskId);
    assertThat(attachments).isEmpty();
    verifyByteArraysWereRemoved(contentId);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceTaskAttachmentUrl() {
    // given
    // create case instance
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    Task task = taskService.createTaskQuery().singleResult();
    taskService.createAttachment("foo", task.getId(), null, "something", null, "http://operaton.org");

    // assume
    List<Attachment> attachments = taskService.getTaskAttachments(task.getId());
    assertThat(attachments).hasSize(1);
    terminateAndCloseCaseInstance(caseInstanceId, null);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstanceId));

    // then
    attachments = taskService.getTaskAttachments(task.getId());
    assertThat(attachments).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceVariables() {
    // given
    // create case instances
    List<String> caseInstanceIds = new ArrayList<>();
    int instanceCount = 10;
    for (int i = 0; i < instanceCount; i++) {
      VariableMap variables = Variables.createVariables();
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", variables.putValue("name" + i, "theValue"));
      caseInstanceIds.add(caseInstance.getId());
      terminateAndCloseCaseInstance(caseInstance.getId(), variables);
    }
    // assume
    List<HistoricVariableInstance> variablesInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variablesInstances).hasSize(instanceCount);

    // when
    historyService.deleteHistoricCaseInstancesBulk(caseInstanceIds);

    // then
    variablesInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variablesInstances).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceComplexVariable() {
    // given
    // create case instances
    VariableMap variables = Variables.createVariables();
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", variables.putValue("pojo", new TestPojo("okay", 13.37)));

    caseService.setVariable(caseInstance.getId(), "pojo", "theValue");

    // assume
    List<HistoricVariableInstance> variablesInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variablesInstances).hasSize(1);
    List<HistoricDetail> detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).hasSize(2);
    terminateAndCloseCaseInstance(caseInstance.getId(), variables);

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstance.getId()));

    // then
    variablesInstances = historyService.createHistoricVariableInstanceQuery().list();
    assertThat(variablesInstances).isEmpty();
    detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceDetails() {
    // given
    // create case instances
    String variableNameCase1 = "varName1";
    CaseInstance caseInstance1 = caseService.createCaseInstanceByKey("oneTaskCase", Variables.createVariables().putValue(variableNameCase1, "value1"));
    CaseInstance caseInstance2 = caseService.createCaseInstanceByKey("oneTaskCase", Variables.createVariables().putValue("varName2", "value2"));

    caseService.setVariable(caseInstance1.getId(), variableNameCase1, "theValue");

    // assume
    List<HistoricDetail> detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).hasSize(3);
    caseService.terminateCaseExecution(caseInstance1.getId(), caseService.getVariables(caseInstance1.getId()));
    caseService.terminateCaseExecution(caseInstance2.getId(), caseService.getVariables(caseInstance2.getId()));
    caseService.closeCaseInstance(caseInstance1.getId());
    caseService.closeCaseInstance(caseInstance2.getId());

    // when
    historyService.deleteHistoricCaseInstancesBulk(List.of(caseInstance1.getId(), caseInstance2.getId()));

    // then
    detailsList = historyService.createHistoricDetailQuery().list();
    assertThat(detailsList).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  void testCleanupHistoryCaseInstanceOperationLog() {
    // given
    // create case instances
    int instanceCount = 10;
    List<String> caseInstanceIds = prepareHistoricCaseInstance(instanceCount);

    // assume
    List<HistoricCaseInstance> caseInstanceList = historyService.createHistoricCaseInstanceQuery().list();
    assertThat(caseInstanceList).hasSize(instanceCount);

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    historyService.deleteHistoricCaseInstancesBulk(caseInstanceIds);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().operationType(OPERATION_TYPE_DELETE_HISTORY).count()).isOne();
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().operationType(OPERATION_TYPE_DELETE_HISTORY).singleResult();
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.CASE_INSTANCE);
    assertThat(entry.getOperationType()).isEqualTo(OPERATION_TYPE_DELETE_HISTORY);
    assertThat(entry.getCaseInstanceId()).isNull();
    assertThat(entry.getProperty()).isEqualTo("nrOfInstances");
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(String.valueOf(instanceCount));
  }

  private List<String> prepareHistoricCaseInstance(int instanceCount) {
    List<String> caseInstanceIds = new ArrayList<>();
    for (int i = 0; i < instanceCount; i++) {
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");
      String caseInstanceId = caseInstance.getId();
      caseInstanceIds.add(caseInstanceId);
      terminateAndCloseCaseInstance(caseInstanceId, null);
    }
    return caseInstanceIds;
  }

  private void terminateAndCloseCaseInstance(String caseInstanceId, Map<String, Object> variables) {
    if (variables==null) {
      caseService.terminateCaseExecution(caseInstanceId, variables);
    }else {
      caseService.terminateCaseExecution(caseInstanceId);
    }
    caseService.closeCaseInstance(caseInstanceId);
  }
}
