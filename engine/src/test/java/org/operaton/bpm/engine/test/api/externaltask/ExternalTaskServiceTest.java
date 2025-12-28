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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.engine.test.util.AssertUtil;
import org.operaton.bpm.engine.test.util.ClockTestUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Thorben Lindhauer
 *
 */
class ExternalTaskServiceTest {

  protected static final String WORKER_ID = "aWorkerId";
  protected static final long LOCK_TIME = 10000L;
  protected static final String TOPIC_NAME = "externalTaskTopic";
  protected static final String ERROR_MESSAGE = "error message";
  protected static final String ERROR_DETAILS = "error details";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected ExternalTaskService externalTaskService;
  protected TaskService taskService;
  protected HistoryService historyService;

  @BeforeEach
  void setUp() throws Exception {
    // get rid of the milliseconds because of MySQL datetime precision
    Date now = formatter.parse(formatter.format(new Date()));
    ClockUtil.setCurrentTime(now);
  }

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
  }

  @Test
  void testFailOnMalformedPriorityInput() {
    var deploymentBuilder = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/externaltask/externalTaskInvalidPriority.bpmn20.xml");

    // when
    assertThatThrownBy(deploymentBuilder::deploy)
      // then
      .withFailMessage("deploying a process with malformed priority should not succeed")
      .isInstanceOf(ParseException.class)
      .hasMessageContaining("Value 'NOTaNumber' for attribute 'taskPriority' is not a valid number")
      .extracting(e -> ((ParseException)e).getResourceReports().get(0).getErrors().get(0).getMainElementId())
      .isEqualTo("externalTaskWithPrio");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetch() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    assertThat(task.getId()).isNotNull();
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(task.getActivityId()).isEqualTo("externalTask");
    assertThat(task.getProcessDefinitionKey()).isEqualTo("oneExternalTaskProcess");
    assertThat(task.getTopicName()).isEqualTo(TOPIC_NAME);

    ActivityInstance activityInstance = runtimeService
      .getActivityInstance(processInstance.getId())
      .getActivityInstances("externalTask")[0];

    assertThat(task.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(task.getExecutionId()).isEqualTo(activityInstance.getExecutionIds()[0]);

    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), task.getLockExpirationTime());

    assertThat(task.getWorkerId()).isEqualTo(WORKER_ID);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml")
  @Test
  void testFetchWithPriority() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    assertThat(task.getId()).isNotNull();
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(task.getActivityId()).isEqualTo("externalTaskWithPrio");
    assertThat(task.getProcessDefinitionKey()).isEqualTo("twoExternalTaskWithPriorityProcess");
    assertThat(task.getTopicName()).isEqualTo(TOPIC_NAME);
    assertThat(task.getPriority()).isEqualTo(7);

    ActivityInstance activityInstance = runtimeService
      .getActivityInstance(processInstance.getId())
      .getActivityInstances("externalTaskWithPrio")[0];

    assertThat(task.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(task.getExecutionId()).isEqualTo(activityInstance.getExecutionIds()[0]);

    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), task.getLockExpirationTime());

    assertThat(task.getWorkerId()).isEqualTo(WORKER_ID);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldFetchWithCreateTimeDESCAndPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock()
        .maxTasks(5)
        .workerId(WORKER_ID)

        .orderByCreateTime().desc()
        .usePriority(true)

        .subscribe()
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);

    assertThat(result.get(0).getPriority()).isEqualTo(7);
    assertThat(result.get(1).getPriority()).isEqualTo(7);
    assertThat(result.get(2).getPriority()).isZero();
    assertThat(result.get(3).getPriority()).isZero();
    assertThat(result.get(4).getPriority()).isZero();


    // given the same priority, DESC date is applied
    assertThat(result.get(0).getCreateTime()).isAfterOrEqualTo(result.get(1).getCreateTime());

    // given the rest of priorities, DESC date should apply between them
    assertThat(result.get(2).getCreateTime()).isAfterOrEqualTo(result.get(3).getCreateTime());
    assertThat(result.get(3).getCreateTime()).isAfterOrEqualTo(result.get(4).getCreateTime());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldFetchWithCreateTimeASCAndPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock()
        .maxTasks(5)
        .workerId(WORKER_ID)

        .orderByCreateTime().asc()
        .usePriority(true)

        .subscribe()
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);

    assertThat(result.get(0).getPriority()).isEqualTo(7);
    assertThat(result.get(1).getPriority()).isEqualTo(7);

    assertThat(result.get(2).getPriority()).isZero();
    assertThat(result.get(3).getPriority()).isZero();
    assertThat(result.get(4).getPriority()).isZero();


    // given the same priority, ASC date is applied
    assertThat(result.get(0).getCreateTime()).isBeforeOrEqualTo(result.get(1).getCreateTime());

    // given the rest of priorities, ASC date should apply between them
    assertThat(result.get(2).getCreateTime()).isBeforeOrEqualTo(result.get(3).getCreateTime());
    assertThat(result.get(3).getCreateTime()).isBeforeOrEqualTo(result.get(4).getCreateTime());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldFetchWithCreateTimeASCWithoutPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock()
        .maxTasks(5)
        .workerId(WORKER_ID)

        .orderByCreateTime().asc()
        .usePriority(false)

        .subscribe()
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);
    assertThat(result).extracting("createTime", Date.class).isSorted();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldFetchWithCreateTimeDESCWithoutPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock()
        .maxTasks(5)
        .workerId(WORKER_ID)

        .orderByCreateTime().desc()
        .usePriority(false)

        .subscribe()
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);
    assertThat(result).extracting("createTime", Date.class).isSortedAccordingTo(reverseOrder());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldIgnoreCreateOrderingWhenCreateTimeIsNotConfigured() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock()
        .maxTasks(5)
        .workerId(WORKER_ID)

        // create time ordering is omitted
        .usePriority(true)

        .subscribe()
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);
    // create time ordering will be ignored, only priority will be used
    assertThat(result).extracting("priority", Long.class).isSortedAccordingTo(reverseOrder());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml"
  })
  @Test
  void shouldIgnoreCreateTimeConfigWhenOrderIsNull() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess"); // priority 7 & null
    ClockTestUtil.incrementClock(60_000);
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // null priority

    // when
    var result = externalTaskService.fetchAndLock(6, WORKER_ID, true)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    assertThat(result).hasSize(5);
    // create time ordering will be ignored, only priority will be used
    assertThat(result).extracting("priority", Long.class).isSortedAccordingTo(reverseOrder());
  }

  @Test
  void shouldThrowExceptionOnSubscribeWithInvalidOrderConfig() {
    // given
    var externalTaskQueryTopicBuilder = externalTaskService.fetchAndLock().orderByCreateTime();
    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::subscribe)
        // then
        .isInstanceOf(NotValidException.class)
        .hasMessage("Invalid query: call asc() or desc() after using orderByXX(): direction is null");
  }

  @Test
  void shouldThrowExceptionOnChainedSortingConfigs() {
    // given
    var fetchAndLockBuilder = externalTaskService.fetchAndLock().orderByCreateTime().desc();
    // when
    assertThatThrownBy(fetchAndLockBuilder::desc)
        // then
        .isInstanceOf(NotValidException.class)
        .hasMessage("Invalid query: can specify only one direction desc() or asc() for an ordering constraint: direction is not null");
  }

  @Test
  void shouldThrowExceptionOnUnspecifiedSortingField() {
    // given
    var fetchAndLockBuilder = externalTaskService.fetchAndLock();
    // when
    assertThatThrownBy(fetchAndLockBuilder::desc)
        // then
        .isInstanceOf(NotValidException.class)
        .hasMessage("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml")
  @Test
  void testFetchProcessWithPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(2, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).hasSize(2);

    //task with no prio gets prio defined by process
    assertThat(externalTasks.get(0).getPriority()).isEqualTo(9);
    //task with own prio overrides prio defined by process
    assertThat(externalTasks.get(1).getPriority()).isEqualTo(7);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpressionProcess.bpmn20.xml")
  @Test
  void testFetchProcessWithPriorityExpression() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
                                             Variables.createVariables().putValue("priority", 18));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(2, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    assertThat(externalTasks).hasSize(2);

    // then
    //task with no prio gets prio defined by process
    assertThat(externalTasks.get(0).getPriority()).isEqualTo(18);
    //task with own prio overrides prio defined by process
    assertThat(externalTasks.get(1).getPriority()).isEqualTo(7);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testFetchWithPriorityExpression() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
                                                        Variables.createVariables().putValue("priority", 18));
    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    assertThat(task.getId()).isNotNull();
    assertThat(task.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(task.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(task.getActivityId()).isEqualTo("externalTaskWithPrio");
    assertThat(task.getProcessDefinitionKey()).isEqualTo("twoExternalTaskWithPriorityProcess");
    assertThat(task.getTopicName()).isEqualTo(TOPIC_NAME);
    assertThat(task.getPriority()).isEqualTo(18);

    ActivityInstance activityInstance = runtimeService
      .getActivityInstance(processInstance.getId())
      .getActivityInstances("externalTaskWithPrio")[0];

    assertThat(task.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(task.getExecutionId()).isEqualTo(activityInstance.getExecutionIds()[0]);

    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), task.getLockExpirationTime());

    assertThat(task.getWorkerId()).isEqualTo(WORKER_ID);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml")
  @Test
  void testFetchWithPriorityOrdering() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(2, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).hasSize(2);
    assertThat(externalTasks.get(0).getPriority()).isGreaterThan(externalTasks.get(1).getPriority());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml")
  @Test
  void testFetchNextWithPriority() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then the task is locked
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    long firstPrio = task.getPriority();
    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), task.getLockExpirationTime());

    // another task with next higher priority can be claimed
    externalTasks = externalTaskService.fetchAndLock(1, "anotherWorkerId", true)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(externalTasks).hasSize(1);
    assertThat(firstPrio).isGreaterThanOrEqualTo(externalTasks.get(0).getPriority());

    // the expiration time expires
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    //first can be claimed
    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getPriority()).isEqualTo(firstPrio);
  }

  @Deployment
  @Test
  void testFetchTopicSelection() {
    // given
    runtimeService.startProcessInstanceByKey("twoTopicsProcess");

    // when
    List<LockedExternalTask> topic1Tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic("topic1", LOCK_TIME)
        .execute();

    List<LockedExternalTask> topic2Tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic("topic2", LOCK_TIME)
        .execute();

    // then
    assertThat(topic1Tasks).hasSize(1);
    assertThat(topic1Tasks.get(0).getTopicName()).isEqualTo("topic1");

    assertThat(topic2Tasks).hasSize(1);
    assertThat(topic2Tasks.get(0).getTopicName()).isEqualTo("topic2");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchWithoutTopicName() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    var externalTaskQueryTopicBuilder = externalTaskService
      .fetchAndLock(1, WORKER_ID)
      .topic(null, LOCK_TIME);

    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("topicName is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchNullWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    var externalTaskQueryTopicBuilder = externalTaskService
      .fetchAndLock(1, null)
      .topic(TOPIC_NAME, LOCK_TIME);

    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchNegativeNumberOfTasks() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    var externalTaskQueryTopicBuilder = externalTaskService
      .fetchAndLock(-1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME);

    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("maxResults is not greater than or equal to 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchLessTasksThanExist() {
    // given
    for (int i = 0; i < 10; i++) {
      runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    }

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    assertThat(externalTasks).hasSize(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchNegativeLockTime() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    var externalTaskQueryTopicBuilder = externalTaskService
      .fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, -1L);

    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("lockTime is not greater than 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchZeroLockTime() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    var externalTaskQueryTopicBuilder = externalTaskService
      .fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, 0L);

    // when
    assertThatThrownBy(externalTaskQueryTopicBuilder::execute)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("lockTime is not greater than 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchNoTopics() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .execute();

    // then
    assertThat(tasks).isEmpty();
  }

  @Deployment
  @Test
  void testFetchVariables() {
    // given
    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
          Variables.createVariables().putValue("processVar1", 42).putValue("processVar2", 43));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .variables("processVar1", "subProcessVar", "taskVar")
      .execute();

    // then
    LockedExternalTask task = externalTasks.get(0);
    VariableMap variables = task.getVariables();
    assertThat(variables)
            .hasSize(3)
            .containsEntry("processVar1", 42)
            .containsEntry("subProcessVar", 44L)
            .containsEntry("taskVar", 45L);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchVariables.bpmn20.xml")
  @Test
  void testShouldNotFetchSerializedVariables() {
    // given
    ExternalTaskCustomValue customValue = new ExternalTaskCustomValue();
    customValue.setTestValue("value1");
    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
        Variables.createVariables().putValue("processVar1", customValue));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .variables("processVar1")
        .execute();

    // then
    LockedExternalTask task = externalTasks.get(0);
    VariableMap variables = task.getVariables();
    assertThat(variables).hasSize(1);

    // when
    assertThatThrownBy(() -> variables.get("processVar1"))
      // then
      .withFailMessage("did not receive an exception although variable was serialized")
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Object is not deserialized.");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchVariables.bpmn20.xml")
  @Test
  void testFetchSerializedVariables() {
    // given
    ExternalTaskCustomValue customValue = new ExternalTaskCustomValue();
    customValue.setTestValue("value1");
    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
        Variables.createVariables().putValue("processVar1", customValue));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .variables("processVar1")
        .enableCustomObjectDeserialization()
        .execute();

    // then
    LockedExternalTask task = externalTasks.get(0);
    VariableMap variables = task.getVariables();
    assertThat(variables).hasSize(1);

    final ExternalTaskCustomValue receivedCustomValue = (ExternalTaskCustomValue) variables.get("processVar1");
    assertThat(receivedCustomValue).isNotNull();
    assertThat(receivedCustomValue.getTestValue()).isNotNull();
    assertThat(receivedCustomValue.getTestValue()).isEqualTo("value1");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskVariablesTest.testExternalTaskVariablesLocal.bpmn20.xml"})
  @Test
  void testFetchOnlyLocalVariables() {

    VariableMap globalVars = Variables.putValue("globalVar", "globalVal");

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", globalVars);

    final String workerId = "workerId";
    final String topicName = "testTopic";

    List<LockedExternalTask> lockedExternalTasks = externalTaskService.fetchAndLock(10, workerId)
      .topic(topicName, 60000)
      .execute();

    assertThat(lockedExternalTasks).hasSize(1);

    LockedExternalTask lockedExternalTask = lockedExternalTasks.get(0);
    VariableMap variables = lockedExternalTask.getVariables();
    assertThat(variables).hasSize(2);
    assertThat(variables.getValue("globalVar", String.class)).isEqualTo("globalVal");
    assertThat(variables.getValue("localVar", String.class)).isEqualTo("localVal");

    externalTaskService.unlock(lockedExternalTask.getId());

    lockedExternalTasks = externalTaskService.fetchAndLock(10, workerId)
      .topic(topicName, 60000)
      .variables("globalVar", "localVar")
      .localVariables()
      .execute();

    assertThat(lockedExternalTasks).hasSize(1);

    lockedExternalTask = lockedExternalTasks.get(0);
    variables = lockedExternalTask.getVariables();
    assertThat(variables).hasSize(1);
    assertThat(variables.getValue("localVar", String.class)).isEqualTo("localVal");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskVariablesTest.testExternalTaskVariablesLocal.bpmn20.xml"})
  @Test
  void testFetchNonExistingLocalVariables() {

    VariableMap globalVars = Variables.putValue("globalVar", "globalVal");

    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess", globalVars);

    final String workerId = "workerId";
    final String topicName = "testTopic";

    List<LockedExternalTask> lockedExternalTasks = externalTaskService.fetchAndLock(10, workerId)
      .topic(topicName, 60000)
      .variables("globalVar", "nonExistingLocalVar")
      .localVariables()
      .execute();

    assertThat(lockedExternalTasks).hasSize(1);

    LockedExternalTask lockedExternalTask = lockedExternalTasks.get(0);
    VariableMap variables = lockedExternalTask.getVariables();
    assertThat(variables).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchVariables.bpmn20.xml")
  @Test
  void testFetchAllVariables() {
    // given
    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
        Variables.createVariables()
            .putValue("processVar1", 42)
            .putValue("processVar2", 43));

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // then
    LockedExternalTask task = externalTasks.get(0);
    verifyVariables(task);

    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
        Variables.createVariables()
            .putValue("processVar1", 42)
            .putValue("processVar2", 43));

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .variables((String[]) null)
        .execute();

    task = externalTasks.get(0);
    verifyVariables(task);

    runtimeService.startProcessInstanceByKey("subProcessExternalTask",
        Variables.createVariables()
            .putValue("processVar1", 42)
            .putValue("processVar2", 43));

    List<String> list = null;
    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .variables(list)
        .execute();

    task = externalTasks.get(0);
    verifyVariables(task);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchNonExistingVariable() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .variables("nonExistingVariable")
      .execute();

    LockedExternalTask task = tasks.get(0);

    // then
    assertThat(task.getVariables()).isEmpty();
  }

  @Deployment
  @Test
  void testFetchMultipleTopics() {
    // given a process instance with external tasks for topics "topic1", "topic2", and "topic3"
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");

    // when fetching tasks for two topics
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("topic1", LOCK_TIME)
      .topic("topic2", LOCK_TIME * 2)
      .execute();

    // then those two tasks are locked
    assertThat(tasks).hasSize(2);
    LockedExternalTask topic1Task = "topic1".equals(tasks.get(0).getTopicName()) ? tasks.get(0) : tasks.get(1);
    LockedExternalTask topic2Task = "topic2".equals(tasks.get(0).getTopicName()) ? tasks.get(0) : tasks.get(1);

    assertThat(topic1Task.getTopicName()).isEqualTo("topic1");
    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), topic1Task.getLockExpirationTime());

    assertThat(topic2Task.getTopicName()).isEqualTo("topic2");
    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME * 2), topic2Task.getLockExpirationTime());

    // and the third task can still be fetched
    tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("topic1", LOCK_TIME)
      .topic("topic2", LOCK_TIME * 2)
      .topic("topic3", LOCK_TIME * 3)
      .execute();

    assertThat(tasks).hasSize(1);

    LockedExternalTask topic3Task = tasks.get(0);
    assertThat(topic3Task.getTopicName()).isEqualTo("topic3");
    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME * 3), topic3Task.getLockExpirationTime());
  }

  @Deployment
  @Test
  void testFetchMultipleTopicsWithVariables() {
    // given a process instance with external tasks for topics "topic1" and "topic2"
    // both have local variables "var1" and "var2"
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess",
        Variables.createVariables().putValue("var1", 0).putValue("var2", 0));

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic("topic1", LOCK_TIME).variables("var1", "var2")
      .topic("topic2", LOCK_TIME).variables("var1")
      .execute();

    LockedExternalTask topic1Task = "topic1".equals(tasks.get(0).getTopicName()) ? tasks.get(0) : tasks.get(1);
    LockedExternalTask topic2Task = "topic2".equals(tasks.get(0).getTopicName()) ? tasks.get(0) : tasks.get(1);

    assertThat(topic1Task.getTopicName()).isEqualTo("topic1");
    assertThat(topic2Task.getTopicName()).isEqualTo("topic2");

    // then the correct variables have been fetched
    VariableMap topic1Variables = topic1Task.getVariables();
    assertThat(topic1Variables)
            .hasSize(2)
            .containsEntry("var1", 1L)
            .containsEntry("var2", 1L);

    VariableMap topic2Variables = topic2Task.getVariables();
    assertThat(topic2Variables)
            .hasSize(1)
            .containsEntry("var1", 2L);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml")
  @Test
  void testFetchMultipleTopicsMaxTasks() {
    // given
    for (int i = 0; i < 10; i++) {
      runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");
    }

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic("topic1", LOCK_TIME)
        .topic("topic2", LOCK_TIME)
        .topic("topic3", LOCK_TIME)
        .execute();

    // then 5 tasks were returned in total, not per topic
    assertThat(tasks).hasSize(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchSuspendedTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when suspending the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // then the external task cannot be fetched
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    assertThat(externalTasks).isEmpty();

    // when activating the process instance
    runtimeService.activateProcessInstanceById(processInstance.getId());

    // then the task can be fetched
    externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(externalTasks).hasSize(1);
  }

  /**
   * Note: this does not test a hard API guarantee, i.e. the test is stricter than the API (Javadoc).
   * Its purpose is to ensure that the API implementation is less error-prone to use.
   * <p>
   * Bottom line: if there is good reason to change behavior such that this test breaks, it may
   * be ok to change the test.
   * </p>
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchAndLockWithInitialBuilder() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    ExternalTaskQueryBuilder initialBuilder = externalTaskService.fetchAndLock(1, WORKER_ID);
    initialBuilder.topic(TOPIC_NAME, LOCK_TIME);

    // should execute regardless whether the initial builder is used or the builder returned by the
    // #topic invocation
    List<LockedExternalTask> tasks = initialBuilder.execute();

    // then
    assertThat(tasks).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml"})
  @Test
  void testFetchByProcessDefinitionId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");
    String processDefinitionId2 = processInstance2.getProcessDefinitionId();

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .processDefinitionId(processDefinitionId2)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getProcessDefinitionId()).isEqualTo(processDefinitionId2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchByProcessDefinitionIdCombination() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);
    String processDefinitionId2 = processInstance2.getProcessDefinitionId();

    //when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
        .topic(topicName2, lockDuration)
          .processDefinitionId(processDefinitionId2)
        .execute();

    //then
    assertThat(topicTasks).hasSize(3);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(pi.getProcessDefinitionId());
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchByProcessDefinitionIdIn() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";
    String topicName3 = "topic3";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";
    String businessKey3 = "testBusinessKey3";

    long lockDuration = 60L * 1000L;

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    String processDefinitionId1 = processInstance1.getProcessDefinitionId();
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);
    String processDefinitionId2 = processInstance2.getProcessDefinitionId();
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey3);
    String processDefinitionId3 = processInstance3.getProcessDefinitionId();

    // when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(2, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .processDefinitionIdIn(processDefinitionId1)
          .processDefinitionKey("parallelExternalTaskProcess")
        .topic(topicName2, lockDuration)
          .processDefinitionId(processDefinitionId2)
          .businessKey(businessKey2)
        .topic(topicName3, lockDuration)
          .processDefinitionId(processDefinitionId3)
          .processDefinitionKeyIn("unexisting")
        .execute();

    // then
    assertThat(topicTasks).hasSize(2);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(pi.getProcessDefinitionId()).isEqualTo(processDefinitionId1);
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(processDefinitionId1);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml"})
  @Test
  void testFetchByProcessDefinitionIds() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    String processDefinitionId1 = processInstance1.getProcessDefinitionId();
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");
    String processDefinitionId2 = processInstance2.getProcessDefinitionId();

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .processDefinitionId(processDefinitionId2)
      .processDefinitionIdIn(processDefinitionId1)
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml"})
  @Test
  void testFetchByProcessDefinitionKey() {
    // given
    String processDefinitionKey1 = "oneExternalTaskProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey1);
    String processDefinitionKey2 = "twoExternalTaskWithPriorityProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey2);

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .processDefinitionKey(processDefinitionKey2)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getProcessDefinitionKey()).isEqualTo(processDefinitionKey2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml"})
  @Test
  void testFetchByProcessDefinitionKeyIn() {
    // given
    String processDefinitionKey1 = "oneExternalTaskProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey1);
    String processDefinitionKey2 = "twoExternalTaskWithPriorityProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey2);

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .processDefinitionKeyIn(processDefinitionKey2)
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getProcessDefinitionKey()).isEqualTo(processDefinitionKey2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityProcess.bpmn20.xml"})
  @Test
  void testFetchByProcessDefinitionKeys() {
    // given
    String processDefinitionKey1 = "oneExternalTaskProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey1);
    String processDefinitionKey2 = "twoExternalTaskWithPriorityProcess";
    runtimeService.startProcessInstanceByKey(processDefinitionKey2);

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .processDefinitionKey(processDefinitionKey1)
      .processDefinitionKeyIn(processDefinitionKey2)
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchByProcessDefinitionIdAndKey() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";
    String businessKey3 = "testBusinessKey3";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);
    String processDefinitionId2 = processInstance2.getProcessDefinitionId();
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey3);
    String processDefinitionId3 = processInstance3.getProcessDefinitionId();

    //when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
        .topic(topicName2, lockDuration)
          .processDefinitionIdIn(processDefinitionId2, processDefinitionId3)
        .topic("topic3", lockDuration)
          .processDefinitionIdIn("unexisting")
        .execute();

    //then
    assertThat(topicTasks).hasSize(3);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(pi.getProcessDefinitionId());
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
        assertThat(externalTask.getProcessDefinitionId()).isEqualTo(processDefinitionId2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void testFetchWithoutTenant() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .withoutTenantId()
      .execute();

    // then
    assertThat(externalTasks).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldLockExternalTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().notLocked().singleResult();

    // when
    externalTaskService.lock(externalTask.getId(), WORKER_ID, LOCK_TIME);

    // then
    List<ExternalTask> lockedExternalTasks = externalTaskService.createExternalTaskQuery().locked().list();
    assertThat(lockedExternalTasks).hasSize(1);

    Date lockExpirationTime = new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME).toDate();
    ExternalTask lockedExternalTask = lockedExternalTasks.get(0);
    assertThat(lockedExternalTask.getWorkerId()).isEqualToIgnoringCase(WORKER_ID);
    assertThat(lockedExternalTask.getLockExpirationTime())
        .isEqualTo(lockExpirationTime);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldLockExternalTaskWithExpiredLock() throws Exception {
    // given
    // a second worker
    String aSecondWorkerId = "aSecondWorkerId";
    // and a process with an external task
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().notLocked().singleResult();
    // which is locked
    externalTaskService.lock(externalTask.getId(), WORKER_ID, LOCK_TIME);
    // and the lock expires
    // we eliminate milliseconds due to MySQL/MariaDB datetime precision
    Date lockExpiredTime = formatter.parse(
        formatter.format(DateUtils.addMilliseconds(ClockUtil.getCurrentTime(), (int) (LOCK_TIME + 1000L))));
    ClockUtil.setCurrentTime(lockExpiredTime);

    // when
    // the external task is locked again
    externalTaskService.lock(externalTask.getId(), aSecondWorkerId, LOCK_TIME);

    // then
    List<ExternalTask> lockedExternalTasks = externalTaskService.createExternalTaskQuery().locked().list();
    assertThat(lockedExternalTasks).hasSize(1);

    Date lockExpirationTime = new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME).toDate();
    ExternalTask lockedExternalTask = lockedExternalTasks.get(0);
    assertThat(lockedExternalTask.getWorkerId()).isEqualToIgnoringCase(aSecondWorkerId);
    assertThat(lockedExternalTask.getLockExpirationTime())
        .isEqualTo(lockExpirationTime);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldLockAlreadyLockedExternalTaskWithSameWorker() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().notLocked().singleResult();
    externalTaskService.lock(externalTask.getId(), WORKER_ID, LOCK_TIME);

    ExternalTask externalTaskFirstLock = externalTaskService.createExternalTaskQuery().locked().singleResult();
    Date firstLockExpirationTime = externalTaskFirstLock.getLockExpirationTime();

    // when
    externalTaskService.lock(externalTaskFirstLock.getId(), WORKER_ID, LOCK_TIME * 10);

    // then
    ExternalTask externalTaskSecondLock = externalTaskService.createExternalTaskQuery().locked().singleResult();
    Date secondLockExpirationTime = externalTaskSecondLock.getLockExpirationTime();
    assertThat(firstLockExpirationTime).isBefore(secondLockExpirationTime);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldFailToLockAlreadyLockedExternalTask() {
    // given
    String aSecondWorkerId = "aSecondWorkerId";
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().notLocked().singleResult();
    String externalTaskId = externalTask.getId();
    externalTaskService.lock(externalTaskId, WORKER_ID, LOCK_TIME);

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock(externalTaskId, aSecondWorkerId, LOCK_TIME))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("External Task " + externalTaskId
      + " cannot be locked by worker '" + aSecondWorkerId
      + "'. It is locked by worker '" + WORKER_ID + "'.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldReportMissingWorkerIdOnLockExternalTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().notLocked().singleResult();
    String externalTaskId = externalTask.getId();

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock(externalTaskId, null, LOCK_TIME))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldReportMissingExternalTaskIdOnLockExternalTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock(null, WORKER_ID, LOCK_TIME))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find external task with id null: externalTask is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldReportNonexistentExternalTaskIdOnLockExternalTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock("fakeExternalTaskId", WORKER_ID, LOCK_TIME))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find external task with id fakeExternalTaskId: " +
          "externalTask is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldFailToLockExternalTaskWithNullLockDuration() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock("fakeExternalTaskId", WORKER_ID, 0))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("lockDuration is not greater than 0");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml"})
  @Test
  void shouldFailToLockExternalTaskWithNegativeLockDuration() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when/then
    assertThatThrownBy(() -> externalTaskService.lock("fakeExternalTaskId", WORKER_ID, -1))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("lockDuration is not greater than 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testComplete() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    externalTaskService.complete(externalTasks.get(0).getId(), WORKER_ID);

    // then
    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstanceAssert.assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("afterExternalTask")
        .done());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteWithVariables() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    externalTaskService.complete(
        externalTasks.get(0).getId(),
        WORKER_ID,
        Variables.createVariables().putValue("var", 42));

    // then
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstanceAssert.assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("afterExternalTask")
        .done());

    assertThat(runtimeService.getVariable(processInstance.getId(), "var")).isEqualTo(42);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteWithWrongWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, "someCrazyWorkerId"))
      // then
      .withFailMessage("it is not possible to complete the task with a different worker id")
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("cannot be completed by worker 'someCrazyWorkerId'. It is locked by worker '" + WORKER_ID + "'.");
  }

  @Test
  void testCompleteNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.complete("nonExistingTaskId", WORKER_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find external task with id nonExistingTaskId");
  }

  @Test
  void testCompleteNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.complete(null, WORKER_ID))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find external task with id null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteNullWorkerId() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    String externalTaskId = tasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, null))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteSuspendedTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    String externalTaskId = externalTasks.get(0).getId();

    // when suspending the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // when
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
      .withFailMessage("the external task cannot be completed")
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ExternalTask with id '" + externalTaskId + "' is suspended");

    testRule.assertProcessNotEnded(processInstance.getId());

    // when activating the process instance again
    runtimeService.activateProcessInstanceById(processInstance.getId());

    // then the task can be completed
    externalTaskService.complete(externalTaskId, WORKER_ID);

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinition.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then
    // expression was evaluated to true
    // error was thrown and caught
    // flow continued to user task
    List<Task> list = taskService.createTaskQuery().list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnCompleteWithVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // expression was evaluated to true using a variable
    // error was thrown and caught
    // flow continued to user task
    List<Task> list = taskService.createTaskQuery().list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnCompleteWithLocalVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.complete(task.getId(), WORKER_ID, null, vars);

    // then
    // expression was evaluated to true using a local variable
    // error was thrown and caught
    // flow continued to user task
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml")
  void shouldFailNestedErrorEventDefinitionsWhenVariableWasNotProvidedByClientOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    String externalTaskId = externalTasks.get(0).getId();

    // then
    // expression evaluation failed due to missing variable
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Unknown property used in expression");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldKeepVariablesAfterEvaluateNestedErrorEventDefinitionsOnCompleteWithVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // expression was evaluated to true using a variable
    // error was caught
    // flow continued to user task
    // variable is still available without output mapping
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldNotKeepVariablesAfterEvaluateNestedErrorEventDefinitionsOnCompleteWithLocalVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.complete(task.getId(), WORKER_ID, null, vars);

    // then
    // expression was evaluated to true using a local variable
    // error was caught
    // flow continued to user task
    // variable is not available due to missing output mapping
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> list = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(list).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionTrueAndOutputMapping.bpmn20.xml"})
  void shouldNotFailOutputMappingAfterNestedErrorEventDefinitionsOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // expression was evaluated to true
    // error was caught
    // flow continued to user task
    // variables were mapped successfully
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionTrueAndOutputMapping.bpmn20.xml")
  void shouldFailOutputMappingAfterNestedErrorEventDefinitionsWhenVariableWasNotProvidedByClientOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when mapping variable does not exist
    // then output mapping fails due to missing variables
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Propagation of bpmn error errorCode failed.");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinition.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnFailure() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, 3000L);

    // then
    // expression was evaluated to true
    // error was thrown and caught
    // flow continued to user task
    List<Task> list = taskService.createTaskQuery().list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnFailWithVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, vars, null);

    // then
    // expression was evaluated to true using a variable
    // error was thrown and caught
    // flow continued to user task
    List<Task> list = taskService.createTaskQuery().list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldEvaluateNestedErrorEventDefinitionsOnFailWithLocalVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, null, vars);

    // then
    // expression was evaluated to true using a local variable
    // error was thrown and caught
    // flow continued to user task
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml")
  void shouldNotFailNestedErrorEventDefinitionsWhenVariableWasNotProvidedByClientOnFail() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, null, null);

    // then
    // expression evaluation failed due to missing variable
    // initial handleFailure was executed
    Incident incident = runtimeService.createIncidentQuery().singleResult();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
      assertThat(historicIncident.getId()).isEqualTo(incident.getId());
      assertThat(historicIncident.isOpen()).isTrue();
    }

    assertThat(incident).isNotNull();
    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentMessage()).isEqualTo(ERROR_MESSAGE);
    assertThat(incident.getExecutionId()).isEqualTo(task.getExecutionId());
    assertThat(incident.getActivityId()).isEqualTo("externalTask");
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getIncidentType()).isEqualTo("failedExternalTask");
    assertThat(incident.getProcessDefinitionId()).isEqualTo(task.getProcessDefinitionId());
    assertThat(incident.getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getConfiguration()).isEqualTo(task.getId());
    assertThat(incident.getJobDefinitionId()).isNull();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldKeepVariablesAfterEvaluateNestedErrorEventDefinitionsOnFailWithVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, vars, null);

    // then
    // expression was evaluated to true using a variable
    // error was caught
    // flow continued to user task
    // variable is still available without output mapping
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionVariableExpression.bpmn20.xml"})
  void shouldNotKeepVariablesAfterEvaluateNestedErrorEventDefinitionsOnFailWithLocalVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, null, vars);

    // then
    // expression was evaluated to true using a local variable
    // error was caught
    // flow continued to user task
    // variable is not available due to missing output mapping
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> list = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(list).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionTrueAndOutputMapping.bpmn20.xml"})
  void shouldNotFailOutputMappingAfterNestedErrorEventDefinitionsOnFail() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when
    Map<String, Object> vars = new HashMap<>();
    vars.put("foo", "bar");
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, ERROR_DETAILS, 0, 3000L, vars, null);

    // then
    // expression was evaluated to true
    // error was caught
    // flow continued to user task
    // variables were mapped successfully
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    List<VariableInstance> variables = runtimeService.createVariableInstanceQuery().variableName("foo").list();
    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionTrueAndOutputMapping.bpmn20.xml")
  void shouldFailOutputMappingAfterNestedErrorEventDefinitionsWhenVariableWasNotProvidedByClientOnFail() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);
    String taskId = task.getId();

    // when mapping variable does not exist
    // then output mapping fails due to missing variables
    assertThatThrownBy(() -> externalTaskService.handleFailure(taskId, WORKER_ID, ERROR_MESSAGE, 0, 3000L))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Propagation of bpmn error errorCode failed.");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionExpressionIncludesErrorMessage.bpmn20.xml"})
  void shouldResolveExpressionWithErrorMessageInNestedErrorEventDefinitionOnFailure() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(externalTasks).hasSize(1);
    LockedExternalTask task = externalTasks.get(0);

    // when
    externalTaskService.handleFailure(task.getId(), WORKER_ID, "myErrorMessage", "myErrorDetails", 0, 3000L);

    // then
    // expression was evaluated to true
    // error was caught
    // flow continued to user task
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionExpressionIncludesNullErrorMessage.bpmn20.xml"})
  void shouldResolveExpressionWithErrorMessageInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> externalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(externalTasks).hasSize(1);
    LockedExternalTask task = externalTasks.get(0);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then
    // expression was evaluated to true
    // error was caught
    // flow continued to user task
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionExpressionIncludesNullErrorMessage.bpmn20.xml"})
  void shouldResolveExpressionWithErrorMessageInNestedErrorEventDefinitionOnCompleteWithMultipleActivities() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().activityId(task.getActivityId()).list();
    assertThat(externalTasks).hasSize(2);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then
    // correct external task was completed
    // expression was evaluated to true
    // error was caught
    // flow continued to user task
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask");
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithTwoNestedErrorEventDefinitionExpressions.bpmn20.xml"})
  void shouldResolveFirstOfTwoExpressionsInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);

    // when
    // expression for error A is true
    Map<String, Object> vars = new HashMap<>();
    vars.put("a", true);
    vars.put("b", false);
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // error A is thrown
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask A");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithTwoNestedErrorEventDefinitionExpressions.bpmn20.xml"})
  void shouldResolveSecondOfTwoExpressionsInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);

    // when
    // expression for error B is true
    Map<String, Object> vars = new HashMap<>();
    vars.put("a", false);
    vars.put("b", true);
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // error B is thrown
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask B");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithTwoNestedErrorEventDefinitionExpressions.bpmn20.xml"})
  void shouldResolveBothOfTwoExpressionsInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);

    // when
    // expressions for both errors are true
    Map<String, Object> vars = new HashMap<>();
    vars.put("a", true);
    vars.put("b", true);
    externalTaskService.complete(task.getId(), WORKER_ID, vars);

    // then
    // error A is thrown as it is defined first
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("userTask A");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionEmptyExpression.bpmn20.xml"})
  void shouldIgnoreEmptyExpressionInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then
    // no error is thrown
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.externalTaskWithNestedErrorEventDefinitionNullExpression.bpmn20.xml"})
  @Disabled("Fixed the used resource, but this fails to parse. Is this intended? Then the test should be deleted.")
  void shouldIgnoreNullExpressionInNestedErrorEventDefinitionOnComplete() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithNestedErrorEventDefinition");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(lockedExternalTasks).hasSize(1);
    LockedExternalTask task = lockedExternalTasks.get(0);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then
    // no error is thrown
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).isEmpty();
  }

  @Test
  @Deployment
  void shouldThrowProcessEngineExceptionWhenOtherResourceIsNotFound() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalAndScriptTask");
    List<LockedExternalTask> lockedExternalTasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(lockedExternalTasks).hasSize(1);
    String externalTaskId = lockedExternalTasks.get(0).getId();

    // when NotFoundException occurs in the same transaction
    // then a ProcessEngineException is thrown
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
        .isExactlyInstanceOf(ProcessEngineException.class)
        .isNotInstanceOf(NotFoundException.class)
        .hasMessageContaining("Unable to find resource at path foo");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testLocking() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then the task is locked
    assertThat(externalTasks).hasSize(1);

    LockedExternalTask task = externalTasks.get(0);
    AssertUtil.assertEqualsSecondPrecision(nowPlus(LOCK_TIME), task.getLockExpirationTime());

    // and cannot be retrieved by another query
    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(externalTasks).isEmpty();

    // unless the expiration time expires
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteLockExpiredTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // then the task can successfully be completed
    externalTaskService.complete(externalTasks.get(0).getId(), WORKER_ID);

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testCompleteReclaimedLockExpiredTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // and the lock expires
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // and it is reclaimed by another worker
    List<LockedExternalTask> reclaimedTasks = externalTaskService.fetchAndLock(1, "anotherWorkerId")
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // when
    assertThatThrownBy(() -> externalTaskService.complete(externalTaskId, WORKER_ID))
      // then
      .withFailMessage("the first worker cannot complete the task")
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("cannot be completed by worker '" + WORKER_ID + "'. It is locked by worker 'anotherWorkerId'.");

    // and the second worker can
    externalTaskService.complete(reclaimedTasks.get(0).getId(), "anotherWorkerId");

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testDeleteProcessInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    assertThat(externalTaskService.fetchAndLock(5, WORKER_ID).topic(TOPIC_NAME, LOCK_TIME).execute()).isEmpty();
    testRule.assertProcessEnded(processInstance.getId());
  }


  @Deployment
  @Test
  void testExternalTaskExecutionTreeExpansion() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService
      .fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    LockedExternalTask externalTask = tasks.get(0);

    // when a non-interrupting boundary event is triggered meanwhile
    // such that the execution tree is expanded
    runtimeService.correlateMessage("Message");

    // then the external task can still be completed
    externalTaskService.complete(externalTask.getId(), WORKER_ID);

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstanceAssert.assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(processInstance.getProcessDefinitionId())
          .activity("afterBoundaryTask")
        .done());

    Task afterBoundaryTask = taskService.createTaskQuery().singleResult();
    taskService.complete(afterBoundaryTask.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testExternalTaskExecutionTreeCompaction() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("concurrentExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService
      .fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    LockedExternalTask externalTask = tasks.get(0);

    Task userTask = taskService.createTaskQuery().singleResult();

    // when the user task completes meanwhile, thereby trigger execution tree compaction
    taskService.complete(userTask.getId());

    // then the external task can still be completed
    externalTaskService.complete(externalTask.getId(), WORKER_ID);

    tasks = externalTaskService
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(tasks).isEmpty();

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUnlock() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    LockedExternalTask task = externalTasks.get(0);

    // when unlocking the task
    externalTaskService.unlock(task.getId());

    // then it can be acquired again
    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    assertThat(externalTasks).hasSize(1);
    LockedExternalTask reAcquiredTask = externalTasks.get(0);
    assertThat(reAcquiredTask.getId()).isEqualTo(task.getId());
  }

  @Test
  void testUnlockNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.unlock(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("externalTaskId is null");
  }

  @Test
  void testUnlockNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.unlock("nonExistingId"))
      // not found exception lets client distinguish this from other failures
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find external task with id nonExistingId");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailure() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    LockedExternalTask task = tasks.get(0);

    // when submitting a failure (after a simulated processing time of three seconds)
    ClockUtil.setCurrentTime(nowPlus(3000L));

    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 5, 3000L);

    // then the task cannot be immediately acquired again
    tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(tasks).isEmpty();

    // and no incident exists because there are still retries left
    assertThat(runtimeService.createIncidentQuery().count()).isZero();

    // but when the retry time expires, the task is available again
    ClockUtil.setCurrentTime(nowPlus(4000L));

    tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(tasks).hasSize(1);

    // and the retries and error message are accessible
    task = tasks.get(0);
    assertThat(task.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
    assertThat((int) task.getRetries()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureWithErrorDetails() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = tasks.get(0);

    // when submitting a failure (after a simulated processing time of three seconds)
    ClockUtil.setCurrentTime(nowPlus(3000L));

    String errorMessage;
    String exceptionStackTrace;
    try {
      RuntimeSqlException cause = new RuntimeSqlException("test cause");
      for (int i = 0; i < 10; i++) {
        cause = new RuntimeSqlException(cause);
      }
      throw cause;
    } catch (RuntimeException e) {
      exceptionStackTrace = ExceptionUtils.getStackTrace(e);
      var msg = new StringBuilder(e.getMessage());
      while (msg.length() < 1000) {
        msg.append(":").append(e.getMessage());
      }
      errorMessage = msg.toString();
    }
    assertThat(exceptionStackTrace).isNotNull();

    // Make sure that stack trace is longer than the errorMessage DB field length
    assertThat(exceptionStackTrace.length()).isGreaterThan(4000);

    externalTaskService.handleFailure(task.getId(), WORKER_ID, errorMessage, exceptionStackTrace, 5, 3000L);

    ClockUtil.setCurrentTime(nowPlus(4000L));
    tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(tasks).hasSize(1);

    // Verify that exception is accessible properly
    task = tasks.get(0);

    assertThat(task.getErrorMessage()).isEqualTo(errorMessage.substring(0, 666));
    assertThat(task.getRetries()).isEqualTo(5);
    assertThat(externalTaskService.getExternalTaskErrorDetails(task.getId())).isEqualTo(exceptionStackTrace);
    assertThat(task.getErrorDetails()).isEqualTo(exceptionStackTrace);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureZeroRetries() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    LockedExternalTask task = tasks.get(0);

    // when reporting a failure and setting retries to 0
    ClockUtil.setCurrentTime(nowPlus(3000L));

    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, 3000L);

    // then the task cannot be fetched anymore even when the lock expires
    ClockUtil.setCurrentTime(nowPlus(4000L));

    tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    assertThat(tasks).isEmpty();

    // and an incident has been created
    Incident incident = runtimeService.createIncidentQuery().singleResult();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
      assertThat(historicIncident.getId()).isEqualTo(incident.getId());
      assertThat(historicIncident.isOpen()).isTrue();
      assertThat(historicIncident.getHistoryConfiguration()).isEqualTo(getHistoricTaskLogOrdered(incident.getConfiguration()).get(0).getId());
    }

    assertThat(incident).isNotNull();
    assertThat(incident.getId()).isNotNull();
    assertThat(incident.getIncidentMessage()).isEqualTo(ERROR_MESSAGE);
    assertThat(incident.getExecutionId()).isEqualTo(task.getExecutionId());
    assertThat(incident.getActivityId()).isEqualTo("externalTask");
    assertThat(incident.getCauseIncidentId()).isEqualTo(incident.getId());
    assertThat(incident.getIncidentType()).isEqualTo("failedExternalTask");
    assertThat(incident.getProcessDefinitionId()).isEqualTo(task.getProcessDefinitionId());
    assertThat(incident.getProcessInstanceId()).isEqualTo(task.getProcessInstanceId());
    assertThat(incident.getRootCauseIncidentId()).isEqualTo(incident.getId());
    AssertUtil.assertEqualsSecondPrecision(nowMinus(4000L), incident.getIncidentTimestamp());
    assertThat(incident.getConfiguration()).isEqualTo(task.getId());
    assertThat(incident.getJobDefinitionId()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureZeroRetriesAfterIncidentsAreResolved() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    LockedExternalTask task = tasks.get(0);
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, 3000L);
    externalTaskService.setRetries(task.getId(), 5);
    ClockUtil.setCurrentTime(nowPlus(3000L));
    externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    ClockUtil.setCurrentTime(nowPlus(4000L));
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 1, 3000L);

    // when reporting a failure and setting retries to 0
    ClockUtil.setCurrentTime(nowPlus(5000L));
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, 3000L);

    // another incident has been created
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();
    assertThat(incident.getId()).isNotNull();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      // there are two incidents in the history
      List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery()
          .configuration(task.getId())
          .orderByCreateTime().asc()
          .list();
      assertThat(historicIncidents).hasSize(2);
      // there are 3 failure logs for external tasks
      List<HistoricExternalTaskLog> logs = getHistoricTaskLogOrdered(task.getId());
      assertThat(logs).hasSize(3);
      // the oldest incident is resolved and correlates to the oldest external task log entry
      assertThat(historicIncidents.get(0).isResolved()).isTrue();
      assertThat(historicIncidents.get(0).getHistoryConfiguration()).isEqualTo(logs.get(2).getId());
      // the latest incident is open and correlates to the latest external task log entry
      assertThat(historicIncidents.get(1).isOpen()).isTrue();
      assertThat(historicIncidents.get(1).getHistoryConfiguration()).isEqualTo(logs.get(0).getId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureAndDeleteProcessInstance() {
    // given a failed external task with incident
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    LockedExternalTask task = tasks.get(0);

    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, LOCK_TIME);

    // when
    runtimeService.deleteProcessInstance(processInstance.getId(), null);

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureThenComplete() {
    // given a failed external task with incident
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    LockedExternalTask task = tasks.get(0);

    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 0, LOCK_TIME);

    // when
    externalTaskService.complete(task.getId(), WORKER_ID);

    // then the task has been completed nonetheless
    Task followingTask = taskService.createTaskQuery().singleResult();
    assertThat(followingTask).isNotNull();
    assertThat(followingTask.getTaskDefinitionKey()).isEqualTo("afterExternalTask");

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureWithWrongWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleFailure(externalTaskId, "someCrazyWorkerId", ERROR_MESSAGE, 5, LOCK_TIME))
      // then
      .withFailMessage("it is not possible to complete the task with a different worker id")
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Failure of External Task " + externalTaskId
        + " cannot be reported by worker 'someCrazyWorkerId'. It is locked by worker '" + WORKER_ID + "'.");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.handleFailure("nonExistingTaskId", WORKER_ID, ERROR_MESSAGE, 5, LOCK_TIME))
      .withFailMessage("exception expected")
      // not found exception lets client distinguish this from other failures
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find external task with id nonExistingTaskId");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.handleFailure(null, WORKER_ID, ERROR_MESSAGE, 5, LOCK_TIME))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find external task with id " + null);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNullWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleFailure(externalTaskId, null, ERROR_MESSAGE, 5, LOCK_TIME))
      // then
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNegativeLockDuration() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleFailure(externalTaskId, WORKER_ID, ERROR_MESSAGE, 5, - LOCK_TIME))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("retryDuration is not greater than or equal to 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNegativeRetries() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleFailure(externalTaskId, WORKER_ID, ERROR_MESSAGE, -5, LOCK_TIME))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("retries is not greater than or equal to 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureNullErrorMessage() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // when
    externalTaskService.handleFailure(externalTasks.get(0).getId(), WORKER_ID, null, 5, LOCK_TIME);

    // then the failure was reported successfully and the error message is null
    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();

    assertThat((int) task.getRetries()).isEqualTo(5);
    assertThat(task.getErrorMessage()).isNull();
    assertThat(externalTaskService.getExternalTaskErrorDetails(task.getId())).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleFailureSuspendedTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask task = externalTasks.get(0);
    String externalTaskId = task.getId();

    // when suspending the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // when
    assertThatThrownBy(() -> externalTaskService.handleFailure(externalTaskId, WORKER_ID, ERROR_MESSAGE, 5, LOCK_TIME))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ExternalTask with id '" + task.getId() + "' is suspended");

    testRule.assertProcessNotEnded(processInstance.getId());

    // when activating the process instance again
    runtimeService.activateProcessInstanceById(processInstance.getId());

    // then the failure can be reported successfully
    externalTaskService.handleFailure(externalTaskId, WORKER_ID, ERROR_MESSAGE, 5, LOCK_TIME);

    ExternalTask updatedTask = externalTaskService.createExternalTaskQuery().singleResult();
    assertThat((int) updatedTask.getRetries()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetRetries() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // when
    externalTaskService.setRetries(externalTasks.get(0).getId(), 5);

    // then
    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();

    assertThat((int) task.getRetries()).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetRetriesResolvesFailureIncident() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask lockedTask = externalTasks.get(0);
    externalTaskService.handleFailure(lockedTask.getId(), WORKER_ID, ERROR_MESSAGE, 0, LOCK_TIME);

    Incident incident = runtimeService.createIncidentQuery().singleResult();

    // when
    externalTaskService.setRetries(lockedTask.getId(), 5);

    // then the incident is resolved
    assertThat(runtimeService.createIncidentQuery().count()).isZero();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {

      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
      assertThat(historicIncident.getId()).isEqualTo(incident.getId());
      assertThat(historicIncident.isResolved()).isTrue();
    }

    // and the task can be fetched again
    ClockUtil.setCurrentTime(nowPlus(LOCK_TIME + 3000L));

    externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getId()).isEqualTo(lockedTask.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetRetriesToZero() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    LockedExternalTask lockedTask = externalTasks.get(0);

    // when
    externalTaskService.setRetries(lockedTask.getId(), 0);

    // then
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();
    assertThat(incident.getConfiguration()).isEqualTo(lockedTask.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {

      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
      assertThat(historicIncident.getId()).isEqualTo(incident.getId());
      assertThat(historicIncident.isOpen()).isTrue();
      assertThat(historicIncident.getHistoryConfiguration()).isNull();
    }

    // and resetting the retries removes the incident again
    externalTaskService.setRetries(lockedTask.getId(), 5);

    assertThat(runtimeService.createIncidentQuery().count()).isZero();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetRetriesToZeroAfterFailureWithRetriesLeft() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    LockedExternalTask task = tasks.get(0);
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 2, 3000L);
    ClockUtil.setCurrentTime(nowPlus(3000L));
    externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    ClockUtil.setCurrentTime(nowPlus(5000L));
    externalTaskService.handleFailure(task.getId(), WORKER_ID, ERROR_MESSAGE, 1, 3000L);
    externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();


    // when setting retries to 0
    ClockUtil.setCurrentTime(nowPlus(7000L));
    externalTaskService.setRetries(task.getId(), 0);

    // an incident has been created
    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertThat(incident).isNotNull();
    assertThat(incident.getId()).isNotNull();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      // there are one incident in the history
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().configuration(task.getId()).singleResult();
      assertThat(historicIncident).isNotNull();
      // there are two failure logs for external tasks
      List<HistoricExternalTaskLog> logs = getHistoricTaskLogOrdered(task.getId());
      assertThat(logs).hasSize(2);
      HistoricExternalTaskLog log = logs.get(0);
      // the incident is open and correlates to the oldest external task log entry
      assertThat(historicIncident.isOpen()).isTrue();
      assertThat(historicIncident.getHistoryConfiguration()).isEqualTo(log.getId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetRetriesNegative() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.setRetries(externalTaskId, -5))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("retries is not greater than or equal to 0");
  }

  @Test
  void testSetRetriesNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.setRetries("someExternalTaskId", 5))
      // not found exception lets client distinguish this from other failures
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("externalTask is null");
  }

  @Test
  void testSetRetriesNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.setRetries((String)null, 5))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("externalTaskId is null");
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetPriority() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();

    // when
    externalTaskService.setPriority(externalTasks.get(0).getId(), 5);

    // then
    ExternalTask task = externalTaskService.createExternalTaskQuery().singleResult();

    assertThat((int) task.getPriority()).isEqualTo(5);
  }


  @Test
  void testSetPriorityNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.setPriority("someExternalTaskId", 5))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("externalTask is null");
  }

  @Test
  void testSetPriorityNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.setPriority(null, 5))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("externalTaskId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskWithPriorityProcess.bpmn20.xml")
  @Test
  void testAfterSetPriorityFetchHigherTask() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(2, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(2);
    LockedExternalTask task = externalTasks.get(1);
    assertThat(task.getPriority()).isZero();
    externalTaskService.setPriority(task.getId(), 9);
    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // then
    externalTasks = externalTaskService.fetchAndLock(1, "anotherWorkerId", true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks.get(0).getPriority()).isEqualTo(9);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testSetPriorityLockExpiredTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // then the priority can be set
    externalTaskService.setPriority(externalTasks.get(0).getId(), 9);

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID, true)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);
    assertThat(externalTasks.get(0).getPriority()).isEqualTo(9);
  }

  @Deployment
  @Test
  void testCancelExternalTaskWithBoundaryEvent() {
    // given
    runtimeService.startProcessInstanceByKey("boundaryExternalTaskProcess");
    assertThat(externalTaskService.createExternalTaskQuery().count()).isOne();

    // when the external task is cancelled by a boundary event
    runtimeService.correlateMessage("Message");

    // then the external task instance has been removed
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();

    Task afterBoundaryTask = taskService.createTaskQuery().singleResult();
    assertThat(afterBoundaryTask).isNotNull();
    assertThat(afterBoundaryTask.getTaskDefinitionKey()).isEqualTo("afterBoundaryTask");

  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnError() {
    //given
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    // when
    List<LockedExternalTask> externalTasks = helperHandleBpmnError(1, WORKER_ID, TOPIC_NAME, LOCK_TIME,  "ERROR-OCCURED");
    //then
    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNotNull();
    assertThat(afterBpmnError.getTaskDefinitionKey()).isEqualTo("afterBpmnError");
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorWithoutDefinedBoundary() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    //when
    List<LockedExternalTask> externalTasks = helperHandleBpmnError(1, WORKER_ID, TOPIC_NAME, LOCK_TIME,  "ERROR-OCCURED");

    //then
    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNull();
    testRule.assertProcessEnded(processInstance.getId());
  }

  /**
   * Helper method to handle a bmpn error on an external task, which is fetched with the given parameters.
   *
   * @param taskCount the count of task to fetch
   * @param workerID the worker id
   * @param topicName the topic name of the external task
   * @param lockTime the lock time for the fetch
   * @param errorCode the error code of the bpmn error
   * @return returns the locked external tasks after the bpmn error was handled
   */
  public  List<LockedExternalTask> helperHandleBpmnError(int taskCount, String workerID, String topicName, long lockTime, String errorCode) {
    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(taskCount, workerID)
      .topic(topicName, lockTime)
      .execute();

    externalTaskService.handleBpmnError(externalTasks.get(0).getId(), workerID, errorCode);

    return externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorLockExpiredTask() {
    //given
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    externalTaskService.handleBpmnError(externalTasks.get(0).getId(), WORKER_ID, "ERROR-OCCURED");

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNotNull();
    assertThat(afterBpmnError.getTaskDefinitionKey()).isEqualTo("afterBpmnError");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorReclaimedLockExpiredTaskWithoutDefinedBoundary() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    handleBpmnErrorReclaimedLockExpiredTask(false);
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorReclaimedLockExpiredTaskWithBoundary() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    //then
    handleBpmnErrorReclaimedLockExpiredTask(false);
  }

  /**
   * Helper method which reclaims an external task after the lock is expired.
   * @param includeVariables flag showing if pass or not variables
   */
  public void handleBpmnErrorReclaimedLockExpiredTask(boolean includeVariables) {
    // when
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // and the lock expires
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // and it is reclaimed by another worker
    List<LockedExternalTask> reclaimedTasks = externalTaskService.fetchAndLock(1, "anotherWorkerId")
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // when
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(externalTaskId, WORKER_ID, "ERROR-OCCURED"))
      // then
      .withFailMessage("the first worker cannot complete the task")
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Bpmn error of External Task " + externalTaskId + " cannot be reported by worker '" + WORKER_ID + "'. It is locked by worker 'anotherWorkerId'.")
      .satisfies(e -> {
        if (includeVariables) {
          assertThat(runtimeService.createIncidentQuery().count()).isZero();
          assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
        }
      });

    // and the second worker can
    externalTaskService.complete(reclaimedTasks.get(0).getId(), "anotherWorkerId");

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();
    assertThat(externalTasks).isEmpty();
  }

  @Test
  void testHandleBpmnErrorNonExistingTask() {
    assertThatThrownBy(() -> externalTaskService.handleBpmnError("nonExistingTaskId", WORKER_ID, "ERROR-OCCURED"))
      .isInstanceOf(NotFoundException.class)
      // not found exception lets client distinguish this from other failures
      .hasMessageContaining("Cannot find external task with id nonExistingTaskId");
  }

  @Test
  void testHandleBpmnNullTaskId() {
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(null, WORKER_ID, "ERROR-OCCURED"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find external task with id " + null);
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnNullErrorCode() {
    //given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    //when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    String externalTaskId = tasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(externalTaskId, WORKER_ID, null))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("errorCode is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorNullWorkerId() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    String externalTaskId = tasks.get(0).getId();

    // when
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(externalTaskId, null, "ERROR-OCCURED"))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorSuspendedTask() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(5, WORKER_ID)
        .topic(TOPIC_NAME, LOCK_TIME)
        .execute();
    String externalTaskId = externalTasks.get(0).getId();

    // when suspending the process instance
    runtimeService.suspendProcessInstanceById(processInstance.getId());

    // when
    assertThatThrownBy(() -> externalTaskService.handleBpmnError(externalTaskId, WORKER_ID, "ERROR-OCCURED"))
      // then the external task cannot be completed
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ExternalTask with id '" + externalTaskId + "' is suspended");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorPassVariablesBoundaryEvent() {
    //given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    variables.put("transientVar", Variables.integerValue(1, true));

    // when
    externalTaskService.handleBpmnError(externalTasks.get(0).getId(), WORKER_ID, "ERROR-OCCURED", null, variables);

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNotNull();
    assertThat(afterBpmnError.getTaskDefinitionKey()).isEqualTo("afterBpmnError");
    List<VariableInstance> list = runtimeService.createVariableInstanceQuery().processInstanceIdIn(pi.getId()).list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("foo");
  }

  @Test
  void testHandleBpmnErrorPassVariablesEventSubProcess() {
    // when
    BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .serviceTask("externalTask")
          .operatonType("external")
          .operatonTopic(TOPIC_NAME)
        .endEvent("endEvent")
        .done();

    BpmnModelInstance subProcess = modify(process)
        .addSubProcessTo("process")
          .id("eventSubProcess")
          .triggerByEvent()
          .embeddedSubProcess()
            .startEvent("eventSubProcessStart")
                .error("ERROR-SPEC-10")
            .userTask("afterBpmnError")
            .endEvent()
          .subProcessDone()
          .done();

    BpmnModelInstance targetProcess = modify(subProcess);

    testRule.deploy(targetProcess);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    variables.put("transientVar", Variables.integerValue(1, true));

    // when
    externalTaskService.handleBpmnError(externalTasks.get(0).getId(), WORKER_ID, "ERROR-SPEC-10", null, variables);

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNotNull();
    assertThat(afterBpmnError.getTaskDefinitionKey()).isEqualTo("afterBpmnError");
    List<VariableInstance> list = runtimeService.createVariableInstanceQuery().processInstanceIdIn(pi.getId()).list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("foo");
  }

  @Deployment
  @Test
  void testHandleBpmnErrorPassMessageEventSubProcess() {
    //given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // and the lock expires without the task being reclaimed
    ClockUtil.setCurrentTime(new DateTime(ClockUtil.getCurrentTime()).plus(LOCK_TIME * 2).toDate());

    // when
    String anErrorMessage = "Some meaningful message";
    externalTaskService.handleBpmnError(externalTasks.get(0).getId(), WORKER_ID, "ERROR-SPEC-10", anErrorMessage);

    externalTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, LOCK_TIME)
      .execute();

    // then
    assertThat(externalTasks).isEmpty();
    assertThat(externalTaskService.createExternalTaskQuery().count()).isZero();
    Task afterBpmnError = taskService.createTaskQuery().singleResult();
    assertThat(afterBpmnError).isNotNull();
    assertThat(afterBpmnError.getTaskDefinitionKey()).isEqualTo("afterBpmnError");
    List<VariableInstance> list = runtimeService.createVariableInstanceQuery().processInstanceIdIn(pi.getId()).list();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("errorMessageVariable");
    assertThat(list.get(0).getValue()).isEqualTo(anErrorMessage);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml")
  @Test
  void testHandleBpmnErrorReclaimedLockExpiredTaskWithBoundaryAndPassVariables() {
    // given
    runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    // then
    handleBpmnErrorReclaimedLockExpiredTask(true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByExternalTaskIds() {
    // given
    startProcessInstance("oneExternalTaskProcess", 5);
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    List<String> externalTaskIds = Arrays.asList(
        tasks.get(0).getId(),
        tasks.get(1).getId(),
        tasks.get(2).getId(),
        tasks.get(3).getId(),
        tasks.get(4).getId());

    // when
    externalTaskService.updateRetries().externalTaskIds(externalTaskIds).set(5);

    // then
    tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByExternalTaskIdArray() {
    // given
    startProcessInstance("oneExternalTaskProcess", 5);
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    List<String> externalTaskIds = Arrays.asList(
        tasks.get(0).getId(),
        tasks.get(1).getId(),
        tasks.get(2).getId(),
        tasks.get(3).getId(),
        tasks.get(4).getId());

    // when
    externalTaskService.updateRetries().externalTaskIds(externalTaskIds.toArray(new String[0])).set(5);

    // then
    tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByProcessInstanceIds() {
    // given
    List<String> processInstances = startProcessInstance("oneExternalTaskProcess", 5);

    // when
    externalTaskService.updateRetries().processInstanceIds(processInstances).set(5);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByProcessInstanceIdArray() {
    // given
    List<String> processInstances = startProcessInstance("oneExternalTaskProcess", 5);

    // when
    externalTaskService.updateRetries().processInstanceIds(processInstances.toArray(new String[0])).set(5);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByExternalTaskQuery() {
    // given
    startProcessInstance("oneExternalTaskProcess", 5);

    ExternalTaskQuery query = externalTaskService.createExternalTaskQuery();

    // when
    externalTaskService.updateRetries().externalTaskQuery(query).set(5);

    // then
    List<ExternalTask> tasks = query.list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByProcessInstanceQuery() {
    // given
    startProcessInstance("oneExternalTaskProcess", 5);

    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey("oneExternalTaskProcess");

    // when
    externalTaskService.updateRetries().processInstanceQuery(processInstanceQuery).set(5);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByHistoricProcessInstanceQuery() {
    // given
    startProcessInstance("oneExternalTaskProcess", 5);

    HistoricProcessInstanceQuery query = historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("oneExternalTaskProcess");

    // when
    externalTaskService.updateRetries().historicProcessInstanceQuery(query).set(5);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat((int) task.getRetries()).isEqualTo(5);
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testUpdateRetriesByAllParameters() {
    // given
    List<String> ids = startProcessInstance("oneExternalTaskProcess", 5);

    ExternalTask externalTask = externalTaskService
        .createExternalTaskQuery()
        .processInstanceId(ids.get(0))
        .singleResult();

    ExternalTaskQuery externalTaskQuery = externalTaskService
        .createExternalTaskQuery()
        .processInstanceId(ids.get(1));

    ProcessInstanceQuery processInstanceQuery = runtimeService
        .createProcessInstanceQuery()
        .processInstanceId(ids.get(2));

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(ids.get(3));

    // when
    externalTaskService.updateRetries()
      .externalTaskIds(externalTask.getId())
      .externalTaskQuery(externalTaskQuery)
      .processInstanceQuery(processInstanceQuery)
      .historicProcessInstanceQuery(historicProcessInstanceQuery)
      .processInstanceIds(ids.get(4))
      .set(5);

    // then
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().list();
    assertThat(tasks).hasSize(5);

    for (ExternalTask task : tasks) {
      assertThat(task.getRetries()).isEqualTo(Integer.valueOf(5));
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTime() {
    final Date oldCurrentTime = ClockUtil.getCurrentTime();
    try {
      // given
      runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
      ClockUtil.setCurrentTime(nowMinus(1000));
      List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic(TOPIC_NAME, LOCK_TIME).execute();

      // when
      Date extendLockTime = new Date();
      ClockUtil.setCurrentTime(extendLockTime);

      externalTaskService.extendLock(lockedTasks.get(0).getId(), WORKER_ID, LOCK_TIME);

      // then
      ExternalTask taskWithExtendedLock = externalTaskService.createExternalTaskQuery().locked().singleResult();
      assertThat(taskWithExtendedLock).isNotNull();
      AssertUtil.assertEqualsSecondPrecision(new Date(extendLockTime.getTime() + LOCK_TIME), taskWithExtendedLock.getLockExpirationTime());

    } finally {
      ClockUtil.setCurrentTime(oldCurrentTime);
    }

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeThatExpired() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic(TOPIC_NAME, 1L)
      .execute();
    String externalTaskId = lockedTasks.get(0).getId();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    ClockUtil.setCurrentTime(nowPlus(2));
    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, WORKER_ID, 100))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot extend a lock that expired");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithoutLock() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();
    String externalTaskId = externalTask.getId();

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, WORKER_ID, 100))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The lock of the External Task " + externalTaskId + " cannot be extended by worker '" + WORKER_ID + "'");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithNullLockTime() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, 1L)
        .execute();
    String externalTaskId = lockedTasks.get(0).getId();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, WORKER_ID, 0))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("lockTime is not greater than 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithNegativeLockTime() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, 1L)
        .execute();
    String externalTaskId = lockedTasks.get(0).getId();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, WORKER_ID, -1))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("lockTime is not greater than 0");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithNullWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, 1L)
        .execute();
    String externalTaskId = lockedTasks.get(0).getId();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, null, 100))
      // then
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("workerId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithDifferentWorkerId() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, 1L)
        .execute();
    String externalTaskId = lockedTasks.get(0).getId();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(externalTaskId, "anAnotherWorkerId", 100))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("The lock of the External Task " + externalTaskId + " cannot be extended by worker 'anAnotherWorkerId'");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeWithNullExternalTask() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_NAME, 1L)
        .execute();

    assertThat(lockedTasks)
            .isNotNull()
            .hasSize(1);

    // when
    assertThatThrownBy(() -> externalTaskService.extendLock(null, WORKER_ID, 100))
      // then
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot find external task with id null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testExtendLockTimeForUnexistingExternalTask() {
    assertThatThrownBy(() -> externalTaskService.extendLock("unexisting", WORKER_ID, 100))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot find external task with id unexisting");
  }

  @Test
  void testCompleteWithLocalVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process").startEvent().serviceTask("externalTask")
        .operatonType("external").operatonTopic("foo").operatonTaskPriority("100")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ReadLocalVariableListenerImpl.class)
        .userTask("user").endEvent().done();

   testRule.deploy(instance);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic("foo", 1L).execute();

    // when
    externalTaskService.complete(lockedTasks.get(0).getId(), WORKER_ID, null,
        Variables.createVariables().putValue("abc", "bar"));

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId()).singleResult();
    assertThat(variableInstance).isNull();
    if (processEngineConfiguration.getHistoryLevel() == HistoryLevel.HISTORY_LEVEL_AUDIT
        || processEngineConfiguration.getHistoryLevel() == HistoryLevel.HISTORY_LEVEL_FULL) {
      HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
          .activityInstanceIdIn(lockedTasks.get(0).getActivityInstanceId()).singleResult();
      assertThat(historicVariableInstance).isNotNull();
      assertThat(historicVariableInstance.getName()).isEqualTo("abc");
      assertThat(historicVariableInstance.getValue()).isEqualTo("bar");
    }
  }

  @Test
  void testCompleteWithNonLocalVariables() {
    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process").startEvent().serviceTask("externalTask")
        .operatonType("external").operatonTopic("foo").operatonTaskPriority("100")
        .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, ReadLocalVariableListenerImpl.class)
        .userTask("user").endEvent().done();

   testRule.deploy(instance);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process");

    List<LockedExternalTask> lockedTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic("foo", 1L).execute();

    // when
    externalTaskService.complete(lockedTasks.get(0).getId(), WORKER_ID,
        Variables.createVariables().putValue("abc", "bar"), null);

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(processInstance.getId()).singleResult();
    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("bar");
    assertThat(variableInstance.getName()).isEqualTo("abc");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  @Test
  void testFetchWithEmptyListOfVariables() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    // when
    List<LockedExternalTask> tasks = externalTaskService.fetchAndLock(5, WORKER_ID).topic("externalTaskTopic", LOCK_TIME).variables()
      .execute();

    // then
    assertThat(tasks).hasSize(1);

    LockedExternalTask task = tasks.get(0);
    assertThat(task.getId()).isNotNull();
    assertThat(task.getVariables()).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByBusinessKey() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";
    String topicName3 = "topic3";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);

    //when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .businessKey(businessKey1)
        .topic(topicName2, lockDuration)
          .businessKey(businessKey2)
        .topic(topicName3, lockDuration)
          .businessKey("fakeBusinessKey")
        .execute();

    //then
    assertThat(topicTasks).hasSize(2);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey1);
        assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey1);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey2);
        assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByBusinessKeyCombination1() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);

    //when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .businessKey(businessKey1)
        .topic(topicName2, lockDuration)
        .execute();

    //then
    assertThat(topicTasks).hasSize(3);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey1);
        assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey1);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(externalTask.getBusinessKey()).isEqualTo(pi.getBusinessKey());
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByBusinessKeyCombination2() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);

    //when
    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
        .topic(topicName2, lockDuration)
          .businessKey(businessKey2)
        .execute();

    //then
    assertThat(topicTasks).hasSize(3);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(externalTask.getBusinessKey()).isEqualTo(pi.getBusinessKey());
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey2);
        assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/parallelExternalTaskProcess.bpmn20.xml")
  @Test
  void testQueryByBusinessKeyLocking() {
    // given
    String topicName1 = "topic1";
    String topicName2 = "topic2";
    String topicName3 = "topic3";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    long lockDuration = 60L * 1000L;

    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess", businessKey2);

    //when
    List<LockedExternalTask> lockedTopicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .businessKey(businessKey1)
        .topic(topicName2, lockDuration)
          .businessKey(businessKey2)
        .topic(topicName3, lockDuration)
          .businessKey("fakeBusinessKey")
        .execute();

    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .businessKey(businessKey1)
        .topic(topicName2, 2 * lockDuration)
          .businessKey(businessKey2)
        .topic(topicName3, 2 * lockDuration)
          .businessKey(businessKey1)
        .execute();

    //then
    assertThat(lockedTopicTasks).hasSize(2);
    assertThat(topicTasks).hasSize(1);

    LockedExternalTask externalTask = topicTasks.get(0);
    ProcessInstance pi = runtimeService.createProcessInstanceQuery()
      .processInstanceId(externalTask.getProcessInstanceId())
      .singleResult();

    assertThat(pi.getBusinessKey()).isEqualTo(businessKey1);
    assertThat(externalTask.getBusinessKey()).isEqualTo(businessKey1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testVariableValueTopicQuery.bpmn20.xml")
  @Test
  void testTopicQueryByVariableValue() {
    // given
    String topicName1 = "testTopic1";
    String topicName2 = "testTopic2";

    String variableName = "testVariable";
    String variableValue1 = "testValue1";
    String variableValue2 = "testValue2";

    Map<String, Object> variables = new HashMap<>();

    long lockDuration = 60L * 1000L;

    //when
    variables.put(variableName, variableValue1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    variables.put(variableName, variableValue2);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .processInstanceVariableEquals(variableName, variableValue1)
        .topic(topicName2, lockDuration)
          .processInstanceVariableEquals(variableName, variableValue2)
        .execute();

    //then
    assertThat(topicTasks).hasSize(2);

    for (LockedExternalTask externalTask : topicTasks) {
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(externalTask.getVariables()).containsEntry(variableName, variableValue1);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(externalTask.getVariables()).containsEntry(variableName, variableValue2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testVariableValueTopicQuery.bpmn20.xml")
  @Test
  void testTopicQueryByVariableValueLocking() {
    // given
    String topicName1 = "testTopic1";
    String topicName2 = "testTopic2";
    String topicName3 = "testTopic3";

    String variableName = "testVariable";
    String variableValue1 = "testValue1";
    String variableValue2 = "testValue2";

    Map<String, Object> variables = new HashMap<>();

    long lockDuration = 60L * 1000L;

    //when
    variables.put(variableName, variableValue1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    variables.put(variableName, variableValue2);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    List<LockedExternalTask> lockedTopicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .processInstanceVariableEquals(variableName, variableValue1)
        .topic(topicName2, lockDuration)
          .processInstanceVariableEquals(variableName, variableValue2)
        .execute();

    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, 2 * lockDuration)
          .processInstanceVariableEquals(variableName, variableValue1)
        .topic(topicName2, 2 * lockDuration)
          .processInstanceVariableEquals(variableName, variableValue2)
        .topic(topicName3, lockDuration)
          .processInstanceVariableEquals(variableName, variableValue2)
        .execute();

    //then
    assertThat(lockedTopicTasks).hasSize(2);
    assertThat(topicTasks).hasSize(1);

    LockedExternalTask externalTask = topicTasks.get(0);
    assertThat(externalTask.getTopicName()).isEqualTo(topicName3);
    assertThat(externalTask.getVariables()).containsEntry(variableName, variableValue2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testVariableValueTopicQuery.bpmn20.xml")
  @Test
  void testTopicQueryByVariableValues() {
    // given
    String topicName1 = "testTopic1";
    String topicName2 = "testTopic2";
    String topicName3 = "testTopic3";

    String variableName1 = "testVariable1";
    String variableName2 = "testVariable2";
    String variableName3 = "testVariable3";

    String variableValue1 = "testValue1";
    String variableValue2 = "testValue2";
    String variableValue3 = "testValue3";
    String variableValue4 = "testValue4";
    String variableValue5 = "testValue5";
    String variableValue6 = "testValue6";

    Map<String, Object> variables = new HashMap<>();

    long lockDuration = 60L * 1000L;

    //when
    variables.put(variableName1, variableValue1);
    variables.put(variableName2, variableValue2);
    variables.put(variableName3, variableValue3);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    variables.put(variableName1, variableValue4);
    variables.put(variableName2, variableValue5);
    variables.put(variableName3, variableValue6);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", variables);

    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .processInstanceVariableEquals(variableName1, variableValue1)
          .processInstanceVariableEquals(variableName2, variableValue2)
        .topic(topicName2, lockDuration)
          .processInstanceVariableEquals(variableName2, variableValue5)
          .processInstanceVariableEquals(variableName3, variableValue6)
        .topic(topicName3, lockDuration)
          .processInstanceVariableEquals(variableName1, "fakeVariableValue")
        .execute();

    //then
    assertThat(topicTasks).hasSize(2);

    for (LockedExternalTask externalTask : topicTasks) {
      // topic names are not always in the same order
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(externalTask.getVariables()).containsEntry(variableName1, variableValue1);
        assertThat(externalTask.getVariables()).containsEntry(variableName2, variableValue2);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(externalTask.getVariables()).containsEntry(variableName2, variableValue5);
        assertThat(externalTask.getVariables()).containsEntry(variableName3, variableValue6);
      } else {
        fail("No other topic name values should be available!");
      }

    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testVariableValueTopicQuery.bpmn20.xml")
  @Test
  void testTopicQueryByBusinessKeyAndVariableValue() {
    // given
    String topicName1 = "testTopic1";
    String topicName2 = "testTopic2";
    String topicName3 = "testTopic3";

    String businessKey1 = "testBusinessKey1";
    String businessKey2 = "testBusinessKey2";

    String variableName = "testVariable1";
    String variableValue1 = "testValue1";
    String variableValue2 = "testValue2";

    Map<String, Object> variables = new HashMap<>();

    long lockDuration = 60L * 1000L;

    //when
    variables.put(variableName, variableValue1);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", businessKey1, variables);
    variables.put(variableName, variableValue2);
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcessTopicQueryVariableValues", businessKey2, variables);

    List<LockedExternalTask> topicTasks = externalTaskService
        .fetchAndLock(3, "externalWorkerId")
        .topic(topicName1, lockDuration)
          .businessKey(businessKey1)
          .processInstanceVariableEquals(variableName, variableValue1)
        .topic(topicName2, lockDuration)
          .businessKey(businessKey2)
          .processInstanceVariableEquals(variableName, variableValue2)
        .topic(topicName3, lockDuration)
          .businessKey("fakeBusinessKey")
        .execute();

    //then
    assertThat(topicTasks).hasSize(2);

    for (LockedExternalTask externalTask : topicTasks) {
      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
          .processInstanceId(externalTask.getProcessInstanceId())
          .singleResult();
      // topic names are not always in the same order
      if (externalTask.getTopicName().equals(topicName1)) {
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey1);
        assertThat(externalTask.getVariables()).containsEntry(variableName, variableValue1);
      } else if (externalTask.getTopicName().equals(topicName2)){
        assertThat(pi.getBusinessKey()).isEqualTo(businessKey2);
        assertThat(externalTask.getVariables()).containsEntry(variableName, variableValue2);
      } else {
        fail("No other topic name values should be available!");
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchAndLockByProcessDefinitionVersionTag.bpmn20.xml"})
  @Test
  void testFetchAndLockByProcessDefinitionVersionTag() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess"); // no version tag
    runtimeService.startProcessInstanceByKey("testFetchAndLockByProcessDefinitionVersionTag"); // version tag: version X.Y

    // when
    Long totalExternalTasks = externalTaskService.createExternalTaskQuery().count();
    List<LockedExternalTask> fetchedExternalTasks = externalTaskService.fetchAndLock(1, "workerID")
        .topic("externalTaskTopic", 1000L).processDefinitionVersionTag("version X.Y").execute();

    //then
    assertThat(totalExternalTasks).isEqualTo(2);
    assertThat(fetchedExternalTasks).hasSize(1);
    assertThat(fetchedExternalTasks.get(0).getProcessDefinitionKey()).isEqualTo("testFetchAndLockByProcessDefinitionVersionTag");
    assertThat(fetchedExternalTasks.get(0).getProcessDefinitionVersionTag()).isEqualTo("version X.Y");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml"})
  @Test
  void testGetTopicNamesWithLockedTasks(){
    //given
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");
    externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic("topic1", LOCK_TIME)
      .execute();

    //when
    List<String> result = externalTaskService.getTopicNames(true, false, false);

    //then
    assertThat(result).containsExactly("topic1");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml"})
  @Test
  void testGetTopicNamesWithUnlockedTasks(){
    //given
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");
    externalTaskService.fetchAndLock(1, WORKER_ID)
      .topic("topic1", LOCK_TIME)
      .execute();

    //when
    List<String> result = externalTaskService.getTopicNames(false,true,false);

    //then
    assertThat(result).containsExactlyInAnyOrder("topic2", "topic3");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml"})
  @Test
  void testGetTopicNamesWithRetries(){
    //given
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");

    ExternalTask topic1Task = externalTaskService.createExternalTaskQuery().topicName("topic1").singleResult();
    ExternalTask topic2Task = externalTaskService.createExternalTaskQuery().topicName("topic2").singleResult();
    ExternalTask topic3Task = externalTaskService.createExternalTaskQuery().topicName("topic3").singleResult();

    externalTaskService.setRetries(topic1Task.getId(), 3);
    externalTaskService.setRetries(topic2Task.getId(), 0);
    externalTaskService.setRetries(topic3Task.getId(), 0);

    //when
    List<String> result = externalTaskService.getTopicNames(false,false,true);

    //then
    assertThat(result).containsExactly("topic1");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchMultipleTopics.bpmn20.xml"})
  @Test
  void testGetTopicNamesAreDistinct(){
    //given
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");
    runtimeService.startProcessInstanceByKey("parallelExternalTaskProcess");

    // when
    List<String> result = externalTaskService.getTopicNames();

    //then
    assertThat(result).containsExactlyInAnyOrder("topic1", "topic2", "topic3");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchAndLockWithExtensionProperties.bpmn20.xml"})
  @Test
  void testFetchAndLockWithExtensionProperties_shouldReturnProperties() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithCustomProperties");

    // when
    List<LockedExternalTask> lockedExternalTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic(TOPIC_NAME, LOCK_TIME).includeExtensionProperties()
        .execute();

    // then
    assertThat(lockedExternalTasks).hasSize(1);
    assertThat(lockedExternalTasks.get(0).getExtensionProperties()).containsOnly(entry("property1", "value1"), entry("property2", "value2"),
        entry("property3", "value3"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchAndLockWithoutExtensionProperties.bpmn20.xml"})
  @Test
  void testFetchAndLockWithExtensionProperties_shouldReturnEmptyMap() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithoutCustomProperties");

    // when
    List<LockedExternalTask> lockedExternalTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic(TOPIC_NAME, LOCK_TIME).includeExtensionProperties()
        .execute();

    // then
    assertThat(lockedExternalTasks).hasSize(1);
    assertThat(lockedExternalTasks.get(0).getExtensionProperties()).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/ExternalTaskServiceTest.testFetchAndLockWithExtensionProperties.bpmn20.xml"})
  @Test
  void testFetchAndLockWithoutExtensionProperties_shouldReturnEmptyMap() {
    // given
    runtimeService.startProcessInstanceByKey("oneExternalTaskWithCustomProperties");

    // when
    List<LockedExternalTask> lockedExternalTasks = externalTaskService.fetchAndLock(1, WORKER_ID).topic(TOPIC_NAME, LOCK_TIME).execute();

    // then
    assertThat(lockedExternalTasks).hasSize(1);
    assertThat(lockedExternalTasks.get(0).getExtensionProperties()).isEmpty();
  }

  protected Date nowPlus(long millis) {
    return new Date(ClockUtil.getCurrentTime().getTime() + millis);
  }

  protected Date nowMinus(long millis) {
    return new Date(ClockUtil.getCurrentTime().getTime() - millis);
  }

  protected List<String> startProcessInstance(String key, int instances) {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < instances; i++) {
      ids.add(runtimeService.startProcessInstanceByKey(key, String.valueOf(i)).getId());
    }
    return ids;
  }

  protected void verifyVariables(LockedExternalTask task) {
    VariableMap variables = task.getVariables();
    assertThat(variables)
            .hasSize(4)
            .containsEntry("processVar1", 42)
            .containsEntry("processVar2", 43)
            .containsEntry("subProcessVar", 44L)
            .containsEntry("taskVar", 45L);
  }

  protected List<HistoricExternalTaskLog> getHistoricTaskLogOrdered(String taskId) {
    return historyService.createHistoricExternalTaskLogQuery()
        .failureLog()
        .externalTaskId(taskId)
        .orderByTimestamp().desc()
        .list();
  }

  public static class ReadLocalVariableListenerImpl implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      String value = (String) execution.getVariable("abc");
      assertThat(value).isEqualTo("bar");
    }
  }

}
