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

import static org.operaton.bpm.engine.repository.ResourceTypes.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
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
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class RuntimeByteArrayTest {
  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_TIME = 10000L;
  protected static final String TOPIC_NAME = "externalTaskTopic";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);
  protected MigrationTestRule migrationRule = new MigrationTestRule(engineRule);
  protected BatchMigrationHelper helper = new BatchMigrationHelper(engineRule, migrationRule);


  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(migrationRule).around(testRule);

  protected ProcessEngineConfigurationImpl configuration;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected TaskService taskService;
  protected RepositoryService repositoryService;
  protected ExternalTaskService externalTaskService;

  protected String id;

  @Before
  public void initServices() {
    configuration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    taskService = engineRule.getTaskService();
    repositoryService = engineRule.getRepositoryService();
    externalTaskService = engineRule.getExternalTaskService();
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @After
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
