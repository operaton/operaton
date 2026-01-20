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
package org.operaton.bpm.engine.test.api.externaltask;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskById;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByLockExpirationTime;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByProcessDefinitionId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByProcessDefinitionKey;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByProcessInstanceId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class ExternalTaskQueryTest {

  protected static final String WORKER_ID = "aWorkerId";
  protected static final String TOPIC_NAME = "externalTaskTopic";
  protected static final String ERROR_MESSAGE = "error";
  // The range of Oracle's NUMBER field is limited to ~10e+125
  // which is below Double.MAX_VALUE, so we only test with the following
  // max value
  protected static final double MAX_DOUBLE_VALUE = 10E+124;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ExternalTaskService externalTaskService;

  @BeforeEach
  void setUp() {
    ClockUtil.setCurrentTime(new Date());
  }

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSingleResult() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();

    // then
    assertThat(externalTask.getId()).isNotNull();

    assertThat(externalTask.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(externalTask.getActivityId()).isEqualTo("externalTask");
    assertThat(externalTask.getActivityInstanceId()).isNotNull();
    assertThat(externalTask.getExecutionId()).isNotNull();
    assertThat(externalTask.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(externalTask.getProcessDefinitionKey()).isEqualTo("oneExternalTaskProcess");
    assertThat(externalTask.getTopicName()).isEqualTo(TOPIC_NAME);
    assertThat(externalTask.getWorkerId()).isNull();
    assertThat(externalTask.getLockExpirationTime()).isNull();
    assertThat(externalTask.isSuspended()).isFalse();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testList() {
    startInstancesByKey("oneExternalTaskProcess", 5);
    assertThat(externalTaskService.createExternalTaskQuery().list()).hasSize(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testCount() {
    startInstancesByKey("oneExternalTaskProcess", 5);
    assertThat(externalTaskService.createExternalTaskQuery().count()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByLockState() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 5);
    lockInstances(TOPIC_NAME, 10000L, 3, WORKER_ID);

    // when
    List<ExternalTask> lockedTasks = externalTaskService.createExternalTaskQuery().locked().list();
    List<ExternalTask> nonLockedTasks = externalTaskService.createExternalTaskQuery().notLocked().list();

    // then
    assertThat(lockedTasks).hasSize(3);
    for (ExternalTask task : lockedTasks) {
      assertThat(task.getLockExpirationTime()).isNotNull();
    }

    assertThat(nonLockedTasks).hasSize(2);
    for (ExternalTask task : nonLockedTasks) {
      assertThat(task.getLockExpirationTime()).isNull();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByProcessDefinitionId() {
    // given
    org.operaton.bpm.engine.repository.Deployment secondDeployment = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
      .deploy();

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    startInstancesById(processDefinitions.get(0).getId(), 3);
    startInstancesById(processDefinitions.get(1).getId(), 2);

    // when
    List<ExternalTask> definition1Tasks = externalTaskService
        .createExternalTaskQuery()
        .processDefinitionId(processDefinitions.get(0).getId())
        .list();
    List<ExternalTask> definition2Tasks = externalTaskService
        .createExternalTaskQuery()
        .processDefinitionId(processDefinitions.get(1).getId())
        .list();

    // then
    assertThat(definition1Tasks).hasSize(3);
    for (ExternalTask task : definition1Tasks) {
      assertThat(task.getProcessDefinitionId()).isEqualTo(processDefinitions.get(0).getId());
    }

    assertThat(definition2Tasks).hasSize(2);
    for (ExternalTask task : definition2Tasks) {
      assertThat(task.getProcessDefinitionId()).isEqualTo(processDefinitions.get(1).getId());
    }

    // cleanup
    repositoryService.deleteDeployment(secondDeployment.getId(), true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByActivityId() {
    // given
    startInstancesByKey("parallelExternalTaskProcess", 3);

    // when
    List<ExternalTask> tasks = externalTaskService
        .createExternalTaskQuery()
        .activityId("externalTask2")
        .list();

    // then
    assertThat(tasks).hasSize(3);
    for (ExternalTask task : tasks) {
      assertThat(task.getActivityId()).isEqualTo("externalTask2");
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByActivityIdIn() {
    // given
    startInstancesByKey("parallelExternalTaskProcess", 3);

    List<String> activityIds = List.of("externalTask1", "externalTask2");

    // when
    List<ExternalTask> tasks = externalTaskService
        .createExternalTaskQuery()
        .activityIdIn(activityIds.toArray(new String[0]))
        .list();

    // then
    assertThat(tasks).hasSize(6);
    for (ExternalTask task : tasks) {
      assertThat(activityIds).contains(task.getActivityId());
    }
  }

  @Test
  void testFailQueryByActivityIdInNull() {
    // given
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();

    // when
    assertThatThrownBy(() -> externalTaskQuery.activityIdIn((String) null))
    // then
        .isInstanceOf(NullValueException.class)
        .hasMessageContaining("activityIdIn contains null value");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByTopicName() {
    // given
    startInstancesByKey("parallelExternalTaskProcess", 3);

    // when
    List<ExternalTask> topic1Tasks = externalTaskService
        .createExternalTaskQuery()
        .topicName("topic1")
        .list();

    // then
    assertThat(topic1Tasks).hasSize(3);
    for (ExternalTask task : topic1Tasks) {
      assertThat(task.getTopicName()).isEqualTo("topic1");
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByProcessInstanceId() {
    // given
    List<ProcessInstance> processInstances = startInstancesByKey("oneExternalTaskProcess", 3);

    // when
    ExternalTask task = externalTaskService
      .createExternalTaskQuery()
      .processInstanceId(processInstances.get(0).getId())
      .singleResult();

    // then
    assertThat(task).isNotNull();
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstances.get(0).getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByLargeListOfProcessInstanceIdIn() {
    // given
    List<String> processInstances = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      processInstances.add(runtimeService.startProcessInstanceByKey("oneExternalTaskProcess").getProcessInstanceId());
    }

    // when
    List<ExternalTask> tasks = externalTaskService
      .createExternalTaskQuery()
      .processInstanceIdIn(processInstances.toArray(new String[processInstances.size()]))
      .list();

    // then
    assertThat(tasks)
            .isNotNull()
            .hasSize(1001);
    for (ExternalTask task : tasks) {
      assertThat(processInstances).contains(task.getProcessInstanceId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByProcessInstanceIdIn() {
    // given
    List<ProcessInstance> processInstances = startInstancesByKey("oneExternalTaskProcess", 3);

    List<String> processInstanceIds = List.of(processInstances.get(0).getId(), processInstances.get(1).getId());

    // when
    List<ExternalTask> tasks = externalTaskService
      .createExternalTaskQuery()
      .processInstanceIdIn(processInstances.get(0).getId(), processInstances.get(1).getId())
      .list();

    // then
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);
    for (ExternalTask task : tasks) {
      assertThat(processInstanceIds).contains(task.getProcessInstanceId());
    }
  }

  @Test
  void testQueryByNonExistingProcessInstanceId() {
    ExternalTaskQuery query = externalTaskService
        .createExternalTaskQuery()
        .processInstanceIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testQueryByProcessInstanceIdNull() {
    // given
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    // when
    assertThatThrownBy(() -> externalTaskQuery.processInstanceIdIn((String) null))
      // then
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processInstanceIdIn contains null value");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByExecutionId() {
    // given
    List<ProcessInstance> processInstances = startInstancesByKey("oneExternalTaskProcess", 3);

    ProcessInstance firstInstance = processInstances.get(0);

    ActivityInstance externalTaskActivityInstance = runtimeService
      .getActivityInstance(firstInstance.getId())
      .getActivityInstances("externalTask")[0];
    String executionId = externalTaskActivityInstance.getExecutionIds()[0];

    // when
    ExternalTask externalTask = externalTaskService
      .createExternalTaskQuery()
      .executionId(executionId)
      .singleResult();

    // then
    assertThat(externalTask).isNotNull();
    assertThat(externalTask.getExecutionId()).isEqualTo(executionId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByWorkerId() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 10);
    lockInstances(TOPIC_NAME, 10000L, 3, "worker1");
    lockInstances(TOPIC_NAME, 10000L, 4, "worker2");

    // when
    List<ExternalTask> tasks = externalTaskService
      .createExternalTaskQuery()
      .workerId("worker1")
      .list();

    // then
    assertThat(tasks).hasSize(3);
    for (ExternalTask task : tasks) {
      assertThat(task.getWorkerId()).isEqualTo("worker1");
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByLockExpirationTime() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 10);
    lockInstances(TOPIC_NAME, 5000L, 3, WORKER_ID);
    lockInstances(TOPIC_NAME, 10000L, 4, WORKER_ID);

    // when
    Date lockDate = new Date(ClockUtil.getCurrentTime().getTime() + 7000L);
    List<ExternalTask> lockedExpirationBeforeTasks = externalTaskService
        .createExternalTaskQuery()
        .lockExpirationBefore(lockDate)
        .list();

    List<ExternalTask> lockedExpirationAfterTasks = externalTaskService
        .createExternalTaskQuery()
        .lockExpirationAfter(lockDate)
        .list();

    // then
    assertThat(lockedExpirationBeforeTasks).hasSize(3);
    for (ExternalTask task : lockedExpirationBeforeTasks) {
      assertThat(task.getLockExpirationTime()).isNotNull();
      assertThat(task.getLockExpirationTime().getTime()).isLessThan(lockDate.getTime());
    }

    assertThat(lockedExpirationAfterTasks).hasSize(4);
    for (ExternalTask task : lockedExpirationAfterTasks) {
      assertThat(task.getLockExpirationTime()).isNotNull();
      assertThat(task.getLockExpirationTime().getTime()).isGreaterThan(lockDate.getTime());
    }
  }

  @Test
  void testQueryWithNullValues() {
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    assertThatThrownBy(() -> externalTaskQuery.externalTaskId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("externalTaskId is null");

    assertThatThrownBy(() -> externalTaskQuery.activityId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("activityId is null");

    assertThatThrownBy(() -> externalTaskQuery.executionId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("executionId is null");

    assertThatThrownBy(() -> externalTaskQuery.lockExpirationAfter(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("lockExpirationAfter is null");

    assertThatThrownBy(() -> externalTaskQuery.lockExpirationBefore(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("lockExpirationBefore is null");

    assertThatThrownBy(() -> externalTaskQuery.processDefinitionId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processDefinitionId is null");

    assertThatThrownBy(() -> externalTaskQuery.processInstanceId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("processInstanceId is null");

    assertThatThrownBy(() -> externalTaskQuery.topicName(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("topicName is null");

    assertThatThrownBy(() -> externalTaskQuery.workerId(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @Test
  void testQuerySorting() {

    startInstancesByKey("oneExternalTaskProcess", 5);
    startInstancesByKey("twoExternalTaskProcess", 5);

    lockInstances(TOPIC_NAME, 5000L, 5, WORKER_ID);
    lockInstances(TOPIC_NAME, 10000L, 5, WORKER_ID);

    // asc
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().orderById().asc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, externalTaskById());

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessInstanceId().asc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, externalTaskByProcessInstanceId());

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessDefinitionId().asc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, externalTaskByProcessDefinitionId());

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessDefinitionKey().asc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, externalTaskByProcessDefinitionKey());

    tasks = externalTaskService.createExternalTaskQuery().orderByLockExpirationTime().asc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, externalTaskByLockExpirationTime());

    // desc
    tasks = externalTaskService.createExternalTaskQuery().orderById().desc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, inverted(externalTaskById()));

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessInstanceId().desc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, inverted(externalTaskByProcessInstanceId()));

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessDefinitionId().desc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, inverted(externalTaskByProcessDefinitionId()));

    tasks = externalTaskService.createExternalTaskQuery().orderByProcessDefinitionKey().desc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, inverted(externalTaskByProcessDefinitionKey()));

    tasks = externalTaskService.createExternalTaskQuery().orderByLockExpirationTime().desc().list();
    assertThat(tasks).hasSize(10);
    verifySorting(tasks, inverted(externalTaskByLockExpirationTime()));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryBySuspensionState() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 5);
    suspendInstances(3);

    // when
    List<ExternalTask> suspendedTasks = externalTaskService.createExternalTaskQuery().suspended().list();
    List<ExternalTask> activeTasks = externalTaskService.createExternalTaskQuery().active().list();

    // then
    assertThat(suspendedTasks).hasSize(3);
    for (ExternalTask task : suspendedTasks) {
      assertThat(task.isSuspended()).isTrue();
    }

    assertThat(activeTasks).hasSize(2);
    for (ExternalTask task : activeTasks) {
      assertThat(task.isSuspended()).isFalse();
      assertThat(suspendedTasks).doesNotContain(task);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByRetries() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 5);

    List<LockedExternalTask> tasks = lockInstances(TOPIC_NAME, 10000L, 3, WORKER_ID);
    failInstances(tasks.subList(0, 2), ERROR_MESSAGE, 0, 5000L);  // two tasks have no retries left
    failInstances(tasks.subList(2, 3), ERROR_MESSAGE, 4, 5000L);  // one task has retries left

    // when
    List<ExternalTask> tasksWithRetries = externalTaskService
        .createExternalTaskQuery().withRetriesLeft().list();
    List<ExternalTask> tasksWithoutRetries = externalTaskService
        .createExternalTaskQuery().noRetriesLeft().list();

    // then
    assertThat(tasksWithRetries).hasSize(3);
    for (ExternalTask task : tasksWithRetries) {
      assertThat(task.getRetries() == null || task.getRetries() > 0).isTrue();
    }

    assertThat(tasksWithoutRetries).hasSize(2);
    for (ExternalTask task : tasksWithoutRetries) {
      assertThat((int) task.getRetries()).isZero();
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryById() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 2);
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    ExternalTask firstTask = tasks.get(0);

    // when
    ExternalTask resultTask =
        externalTaskService.createExternalTaskQuery()
          .externalTaskId(firstTask.getId())
          .singleResult();

    // then
    assertThat(resultTask.getId()).isEqualTo(firstTask.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByIds() {
    // given
    startInstancesByKey("oneExternalTaskProcess", 3);
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, tasks.get(0).getId(), tasks.get(1).getId());
    // when
    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery().externalTaskIdIn(ids);
    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByIdsWithNull() {
    // given
    Set<String> ids = null;
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    // when
    assertThatThrownBy(() -> externalTaskQuery.externalTaskIdIn(ids))
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of external task ids is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByIdsWithEmptyList() {
    // given
    Set<String> ids = new HashSet<>();
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    // when
    assertThatThrownBy(() -> externalTaskQuery.externalTaskIdIn(ids))
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of external task ids is empty");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByBusinessKey() {
    // given
    String businessKey = "theUltimateKey";
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", businessKey);

    // when
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();

    // then
    assertThat(externalTask).isNotNull();
    assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryListByBusinessKey() {
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", "businessKey" + i);
    }

    assertThat(externalTaskService.createExternalTaskQuery().count()).isEqualTo(5);
    List<ExternalTask> list = externalTaskService.createExternalTaskQuery().list();
    for (ExternalTask externalTask : list) {
      assertThat(externalTask.getBusinessKey()).isNotNull();
    }
  }

  @Test
  void shouldCheckPresenceOfVersionTag() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonVersionTag("1.2.3.4")
        .startEvent()
        .serviceTask()
          .operatonExternalTask("my-topic")
        .endEvent()
        .done();

    testRule.deploy(process);

    startInstancesByKey("process", 1);

    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();

    assertThat(task.getProcessDefinitionVersionTag()).isEqualTo("1.2.3.4");
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionKey() {
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionKey("oneExternalTaskProcess").count()).isZero();
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionKey("oneExternalTaskProcess").count()).isOne();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionKeyIn() {
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionKeyIn("oneExternalTaskProcess").count()).isZero();
    var processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    assertThat(processInstance).isNotNull();
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionKeyIn("oneExternalTaskProcess").count()).isOne();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessDefinitionName() {
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionName("One external task process").count()).isZero();
    var processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    assertThat(processInstance).isNotNull();
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionName("One external task process").count()).isOne();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void processDefinitionNameLike() {
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionNameLike("One external task proc%").count()).isZero();
    var processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    assertThat(processInstance).isNotNull();
    assertThat(externalTaskService.createExternalTaskQuery().processDefinitionNameLike("One external task proc%").count()).isOne();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEquals() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    // Start process-instance with all types of variables
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    // Test query matches
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("longVar", 928374L).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("shortVar", (short) 123).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("integerVar", 1234).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("stringVar", "stringValue").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("booleanVar", true).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("dateVar", date).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("nullVar", null).count()).isOne();

    // Test query for other values on existing variables
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("longVar", 999L).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("shortVar", (short) 999).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("integerVar", 999).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("stringVar", "999").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("booleanVar", false).count()).isZero();
    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("nullVar", "999").count()).isZero();

    // Test querying for task variables not equals
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("longVar", 999L).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("shortVar", (short) 999).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("integerVar", 999).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("stringVar", "999").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("booleanVar", false).count()).isOne();

    // and query for the existing variable with NOT should result in nothing found:
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("longVar", 928374L).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableNameEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, variableValue);

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    // query for case-insensitive variable name should only return a result if case-insensitive search is used
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableNamesIgnoreCase().processVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals(variableName.toLowerCase(), variableValue).matchVariableNamesIgnoreCase().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, variableValue);

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    // query for existing variable should return one result
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals(variableName, variableValue).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for non existing variable should return zero results
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();

    // query for existing variable with different value should return zero results
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals(variableName, "nonExistentValue").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, "nonExistentValue".toLowerCase()).count()).isZero();

    // query for case-insensitive variable value should only return a result when case-insensitive search is used
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for case-insensitive variable with not equals operator should only return a result when case-sensitive search is used
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueLike() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "stringVal%").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "%ngValue").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "%ngVal%").count()).isOne();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "stringVar%").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "%ngVar").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "%ngVar%").count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "stringVal").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("nonExistingVar", "string%").count()).isZero();

    // test with null value
    ExternalTaskQuery externalTaskQuery = externalTaskService.createExternalTaskQuery();

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(() -> externalTaskQuery.processVariableValueLike("stringVar", null));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueLikeIgnoreCase() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isZero();
    ExternalTaskQuery externalTaskQuery = externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase();

    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isOne();
    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "%ngValue".toLowerCase()).count()).isOne();
    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "%ngVal%".toLowerCase()).count()).isOne();

    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "stringVar%".toLowerCase()).count()).isZero();
    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "%ngVar".toLowerCase()).count()).isZero();
    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "%ngVar%".toLowerCase()).count()).isZero();

    assertThat(externalTaskQuery.processVariableValueLike("stringVar", "stringVal".toLowerCase()).count()).isZero();
    assertThat(externalTaskQuery.processVariableValueLike("nonExistingVar", "stringVal%".toLowerCase()).count()).isZero();

    // test with null value
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueLike("stringVar", null));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNotLike() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "stringVal%").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "%ngValue").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "%ngVal%").count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "stringVar%").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "%ngVar").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "%ngVar%").count()).isOne();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "stringVal").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("nonExistingVar", "string%").count()).isZero();

    // test with null value
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueNotLike("stringVar", null));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNotLikeIgnoreCase() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotLike("stringVar", "stringVal%".toLowerCase()).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVal%".toLowerCase()).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngValue".toLowerCase()).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVal%".toLowerCase()).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVar%".toLowerCase()).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVar".toLowerCase()).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVar%".toLowerCase()).count()).isOne();

    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVal".toLowerCase()).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("nonExistingVar", "stringVal%".toLowerCase()).count()).isZero();

    // test with null value
    var externalTaskQuery = externalTaskService.createExternalTaskQuery().matchVariableValuesIgnoreCase();
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueNotLike("stringVar", null));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueCompare() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("numericVar", 928374);
    Date date = new GregorianCalendar(2014, 2, 2, 2, 2, 2).getTime();
    variables.put("dateVar", date);
    variables.put("stringVar", "ab");
    variables.put("nullVar", null);

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", variables);

    // test compare methods with numeric values
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("numericVar", 928373).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("numericVar", 928374).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("numericVar", 928375).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("numericVar", 928373).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("numericVar", 928375).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("numericVar", 928375).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("numericVar", 928374).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("numericVar", 928373).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("numericVar", 928375).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("numericVar", 928373).count()).isZero();

    // test compare methods with date values
    Date before = new GregorianCalendar(2014, 2, 2, 2, 2, 1).getTime();
    Date after = new GregorianCalendar(2014, 2, 2, 2, 2, 3).getTime();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("dateVar", before).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("dateVar", date).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("dateVar", after).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("dateVar", before).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("dateVar", date).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("dateVar", after).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("dateVar", after).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("dateVar", date).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("dateVar", before).count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("dateVar", after).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("dateVar", date).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("dateVar", before).count()).isZero();

    //test with string values
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("stringVar", "aa").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("stringVar", "ab").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("stringVar", "ba").count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("stringVar", "aa").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("stringVar", "ba").count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("stringVar", "ba").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("stringVar", "ab").count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("stringVar", "aa").count()).isZero();

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("stringVar", "ba").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("stringVar", "aa").count()).isZero();

    // test with null value
    var externalTaskQuery = externalTaskService.createExternalTaskQuery();
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
        externalTaskQuery.processVariableValueGreaterThan("nullVar", null));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueGreaterThanOrEquals("nullVar", null));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
        externalTaskQuery.processVariableValueLessThan("nullVar", null));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueLessThanOrEquals("nullVar", null));

    // test with boolean value
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueGreaterThan("nullVar", true));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueGreaterThanOrEquals("nullVar", false));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
        externalTaskQuery.processVariableValueLessThan("nullVar", true));

    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(()->
            externalTaskQuery.processVariableValueLessThanOrEquals("nullVar", false));

    // test non existing variable
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("nonExisting", 123).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", "123"));

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNumberComparison() {
    // long
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 123));

    // untyped null
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", null));

    // typed null
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", "123"));

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThan("var", Variables.numberValue(123)).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueGreaterThanOrEquals("var", Variables.numberValue(123)).count()).isEqualTo(5);
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThan("var", Variables.numberValue(123)).count()).isZero();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueLessThanOrEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberMax() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", MAX_DOUBLE_VALUE));
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", Long.MAX_VALUE));

    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).count()).isOne();
    assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(Long.MAX_VALUE)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberLongValueOverflow() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", MAX_DOUBLE_VALUE));

    // this results in an overflow
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", (long) MAX_DOUBLE_VALUE));

    // the query should not find the long variable
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberNonIntegerDoubleShouldNotMatchInteger() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Variables.createVariables().putValue("var", 42).putValue("var2", 52.4d));

    // querying by 42.4 should not match the integer variable 42
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(42.4d)).count()).isZero();

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess",
            Collections.<String, Object>singletonMap("var", 42.4d));

    // querying by 52 should not find the double variable 52.4
      assertThat(externalTaskService.createExternalTaskQuery().processVariableValueEquals("var", Variables.numberValue(52)).count()).isZero();
  }

  protected List<ProcessInstance> startInstancesByKey(String processDefinitionKey, int number) {
    List<ProcessInstance> processInstances = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      processInstances.add(runtimeService.startProcessInstanceByKey(processDefinitionKey));
    }

    return processInstances;
  }

  protected List<ProcessInstance> startInstancesById(String processDefinitionId, int number) {
    List<ProcessInstance> processInstances = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      processInstances.add(runtimeService.startProcessInstanceById(processDefinitionId));
    }

    return processInstances;
  }

  protected void suspendInstances(int number) {
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().listPage(0, number);

    for (ProcessInstance processInstance : processInstances) {
      runtimeService.suspendProcessInstanceById(processInstance.getId());
    }
  }

  protected List<LockedExternalTask> lockInstances(String topic, long duration, int number, String workerId) {
    return externalTaskService.fetchAndLock(number, workerId).topic(topic, duration).execute();
  }

  protected void failInstances(List<LockedExternalTask> tasks, String errorMessage, int retries, long retryTimeout) {
    this.failInstances(tasks,errorMessage,null,retries,retryTimeout);
  }

  protected void failInstances(List<LockedExternalTask> tasks, String errorMessage, String errorDetails, int retries, long retryTimeout) {
    for (LockedExternalTask task : tasks) {
      externalTaskService.handleFailure(task.getId(), task.getWorkerId(), errorMessage, errorDetails, retries, retryTimeout);
    }
  }

}
