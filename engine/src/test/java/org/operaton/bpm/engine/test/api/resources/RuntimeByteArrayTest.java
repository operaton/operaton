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
import static org.operaton.bpm.engine.repository.ResourceTypes.RUNTIME;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

public class RuntimeByteArrayTest {
  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_TIME = 10000L;
  protected static final String TOPIC_NAME = "externalTaskTopic";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  static MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);

  ProcessEngineConfigurationImpl configuration;
  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;
  RepositoryService repositoryService;
  ExternalTaskService externalTaskService;

  String id;

  @AfterEach
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @AfterEach
  public void tearDown() {
    if (id != null) {
      // delete task
      taskService.deleteTask(id, true);
    }
    ClockUtil.setCurrentTime(new Date());
  }

  @Test
  public void testVariableBinaryForFileValues() {
    // given
    BpmnModelInstance instance = createProcess();

    testRule.deploy(instance);
    FileValue fileValue = createFile();

    runtimeService.startProcessInstanceByKey("Process", Variables.createVariables().putValueTyped("fileVar", fileValue));

    String byteArrayValueId = ((VariableInstanceEntity)runtimeService.createVariableInstanceQuery().singleResult()).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  public void testVariableBinary() {
    byte[] binaryContent = "some binary content".getBytes();

    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("binaryVariable", binaryContent);
    Task task = taskService.newTask();
    taskService.saveTask(task);
    id = task.getId();
    taskService.setVariablesLocal(id, variables);

    String byteArrayValueId = ((VariableInstanceEntity)runtimeService.createVariableInstanceQuery().singleResult()).getByteArrayValueId();

    // when
    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  public void testBatchBinary() {
    // when
    helper.migrateProcessInstancesAsync(15);

    String byteArrayValueId = ((BatchEntity) managementService.createBatchQuery().singleResult()).getConfiguration();

    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired()
        .execute(new GetByteArrayCommand(byteArrayValueId));

    checkBinary(byteArrayEntity);
  }

  @Test
  public void testExceptionStacktraceBinary() {
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

    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(job.getExceptionByteArrayId()));

    checkBinary(byteArrayEntity);
  }

  @Test
  public void testExternalTaskStacktraceBinary() {
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

    ExternalTaskEntity externalTask = (ExternalTaskEntity) externalTaskService.createExternalTaskQuery().singleResult();

    ByteArrayEntity byteArrayEntity = configuration.getCommandExecutorTxRequired().execute(new GetByteArrayCommand(externalTask.getErrorDetailsByteArrayId()));

    // then
    checkBinary(byteArrayEntity);
  }

  protected void checkBinary(ByteArrayEntity byteArrayEntity) {
    assertThat(byteArrayEntity).isNotNull();
    assertThat(byteArrayEntity.getCreateTime()).isNotNull();
    assertThat(byteArrayEntity.getType()).isEqualTo(RUNTIME.getValue());
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

  protected Date nowPlus(long millis) {
    return new Date(ClockUtil.getCurrentTime().getTime() + millis);
  }

}
