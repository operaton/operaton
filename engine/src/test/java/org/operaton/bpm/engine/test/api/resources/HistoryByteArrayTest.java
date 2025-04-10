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
package org.operaton.bpm.engine.test.api.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.repository.ResourceTypes.HISTORY;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionOutputInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricExternalTaskLogEntity;
import org.operaton.bpm.engine.impl.persistence.entity.AttachmentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.api.variables.JavaSerializable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoryByteArrayTest {
  protected static final String DECISION_PROCESS = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml";
  protected static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";
  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_TIME = 10000L;
  protected static final String TOPIC_NAME = "externalTaskTopic";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl configuration;
  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;
  HistoryService historyService;
  ExternalTaskService externalTaskService;

  String taskId;

  @AfterEach
  void tearDown() {
    if (taskId != null) {
      // delete task
      taskService.deleteTask(taskId, true);
    }
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

  @Test
  void testHistoricVariableBinaryForFileValues() {
    // given
    BpmnModelInstance instance = createProcess();

    testRule.deploy(instance);
    FileValue fileValue = createFile();

    runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValueTyped("fileVar", fileValue));

    String byteArrayValueId = ((HistoricVariableInstanceEntity)historyService.createHistoricVariableInstanceQuery().singleResult()).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricVariableBinary() {
    byte[] binaryContent = "some binary content".getBytes();

    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("binaryVariable", binaryContent);
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskId = task.getId();
    taskService.setVariablesLocal(taskId, variables);

    String byteArrayValueId = ((HistoricVariableInstanceEntity)historyService.createHistoricVariableInstanceQuery().singleResult()).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricDetailBinaryForFileValues() {
    // given
    BpmnModelInstance instance = createProcess();

    testRule.deploy(instance);
    FileValue fileValue = createFile();

    runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValueTyped("fileVar", fileValue));

    String byteArrayValueId = ((HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery().singleResult()).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricDecisionInputInstanceBinary() {
    testRule.deploy(DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN);

    startProcessInstanceAndEvaluateDecision(new JavaSerializable("foo"));

    HistoricDecisionInstance historicDecisionInstance = engineRule.getHistoryService().createHistoricDecisionInstanceQuery().includeInputs().singleResult();
    List<HistoricDecisionInputInstance> inputInstances = historicDecisionInstance.getInputs();
    assertThat(inputInstances).hasSize(1);

    String byteArrayValueId = ((HistoricDecisionInputInstanceEntity) inputInstances.get(0)).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricDecisionOutputInstanceBinary() {
    testRule.deploy(DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN);

    startProcessInstanceAndEvaluateDecision(new JavaSerializable("foo"));

    HistoricDecisionInstance historicDecisionInstance = engineRule.getHistoryService().createHistoricDecisionInstanceQuery().includeOutputs().singleResult();
    List<HistoricDecisionOutputInstance> outputInstances = historicDecisionInstance.getOutputs();
    assertThat(outputInstances).hasSize(1);


    String byteArrayValueId = ((HistoricDecisionOutputInstanceEntity) outputInstances.get(0)).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testAttachmentContentBinaries() {
      // create and save task
      Task task = taskService.newTask();
      taskService.saveTask(task);
      taskId = task.getId();

      // when
      AttachmentEntity attachment = (AttachmentEntity) taskService.createAttachment("web page", taskId, "someprocessinstanceid", "weatherforcast", "temperatures and more", new ByteArrayInputStream("someContent".getBytes()));

      ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(attachment.getContentId()));

      checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricExceptionStacktraceBinary() {
    // given
    BpmnModelInstance instance = createFailingProcess();
    testRule.deploy(instance);
    runtimeService.startProcessInstanceByKey("Process");
    String jobId = managementService.createJobQuery().singleResult().getId();

    // when
    try {
      managementService.executeJob(jobId);
      fail("");
    } catch (Exception e) {
      // expected
    }

    HistoricJobLogEventEntity entity = (HistoricJobLogEventEntity) historyService
        .createHistoricJobLogQuery()
        .failureLog()
        .singleResult();
    assertThat(entity).isNotNull();

    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(entity.getExceptionByteArrayId()));

    checkBinary(byteArrayEntity);
  }

  @Test
  void testHistoricExternalTaskJobLogStacktraceBinary() {
    // given
    testRule.deploy("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml");
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = tasks.get(0);

    // submitting a failure (after a simulated processing time of three seconds)
    ClockUtil.setCurrentTime(nowPlus(3000L));

    String errorMessage;
    String exceptionStackTrace;
    try {
      throw new RuntimeSqlException("test cause");
    } catch (RuntimeException e) {
      exceptionStackTrace = ExceptionUtils.getStackTrace(e);
      errorMessage = e.getMessage();
    }
    assertThat(exceptionStackTrace).isNotNull();

    externalTaskService.handleFailure(task.getId(), WORKER_ID, errorMessage, exceptionStackTrace, 5, 3000L);

    HistoricExternalTaskLogEntity entity = (HistoricExternalTaskLogEntity) historyService.createHistoricExternalTaskLogQuery().errorMessage(errorMessage).singleResult();
    assertThat(entity).isNotNull();

    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(entity.getErrorDetailsByteArrayId()));

    // then
    checkBinary(byteArrayEntity);
  }

  protected void checkBinary(ByteArrayEntity byteArrayEntity) {
    assertThat(byteArrayEntity).isNotNull();
    assertThat(byteArrayEntity.getCreateTime()).isNotNull();
    assertThat(byteArrayEntity.getType()).isEqualTo(HISTORY.getValue());
  }

  protected FileValue createFile() {
    String fileName = "text.txt";
    String encoding = "crazy-encoding";
    String mimeType = "martini/dry";

    return Variables
        .fileValue(fileName)
        .file("ABC".getBytes())
        .encoding(encoding)
        .mimeType(mimeType)
        .create();
  }

  protected BpmnModelInstance createProcess() {
    return Bpmn.createExecutableProcess("Process")
      .startEvent()
      .userTask("user")
      .endEvent()
      .done();
  }

  protected BpmnModelInstance createFailingProcess() {
    return Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask("failing")
      .operatonAsyncAfter()
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class)
      .endEvent()
      .done();
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecision(Object input) {
    return engineRule.getRuntimeService().startProcessInstanceByKey("testProcess",
        Variables.createVariables().putValue("input1", input));
  }


  protected Date nowPlus(long millis) {
    return new Date(ClockUtil.getCurrentTime().getTime() + millis);
  }
}
