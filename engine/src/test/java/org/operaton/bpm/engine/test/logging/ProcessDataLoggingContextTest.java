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
package org.operaton.bpm.engine.test.logging;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.logging.MdcAccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessDataLoggingContextTest {

  private static final String PROCESS = "process";
  private static final String B_KEY = "businessKey1";
  private static final String B_KEY2 = "businessKey2";
  private static final String FAILING_PROCESS = "failing-process";
  private static final String TENANT_ID = "testTenant";

  private static final String CMD_LOGGER = "org.operaton.bpm.engine.cmd";
  private static final String CONTEXT_LOGGER = "org.operaton.bpm.engine.context";
  private static final String JOBEXEC_LOGGER = "org.operaton.bpm.engine.jobexecutor";
  private static final String PVM_LOGGER = "org.operaton.bpm.engine.pvm";

  private static final String LOG_IDENT_FAILURE = "ENGINE-16004";

  private final RuntimeContainerDelegate runtimeContainerDelegate = RuntimeContainerDelegate.INSTANCE.get();
  private boolean defaultEngineRegistered;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setProcessEngineName("reusableEngine");
      configuration.setLoggingContextBusinessKey("businessKey");
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension();

  RuntimeService runtimeService;
  TaskService taskService;

  TestMdcFacade testMDCFacade;

  @BeforeEach
  void setupServices() {
    defaultEngineRegistered = false;
    testMDCFacade = TestMdcFacade.empty();
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
    testMDCFacade.clear();
  }

  @AfterEach
  void tearDown() {
    if (defaultEngineRegistered) {
      runtimeContainerDelegate.unregisterProcessEngine(engineRule.getProcessEngine());
    }
  }

  @AfterEach
  void resetLogConfiguration() {
    engineRule.getProcessEngineConfiguration()
      .setLoggingContextActivityId("activityId")
      .setLoggingContextApplicationName("applicationName")
      .setLoggingContextBusinessKey("businessKey")
      .setLoggingContextProcessDefinitionId("processDefinitionId")
      .setLoggingContextProcessInstanceId("processInstanceId")
      .setLoggingContextTenantId("tenantId")
      .setLoggingContextEngineName("engineName");
  }

  @Test
  @WatchLogger(loggerNames = PVM_LOGGER, level = "DEBUG")
  void shouldNotLogBusinessKeyIfNotConfigured() {
    // given
    engineRule.getProcessEngineConfiguration().setLoggingContextBusinessKey(null);
    manageDeployment(modelOneTaskProcess());
    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then
    assertActivityLogs(instance, "ENGINE-200", List.of("start", "waitState", "end"), true, false, true, true, engineRule.getProcessEngine().getName());
  }

  @Test
  @WatchLogger(loggerNames = PVM_LOGGER, level = "DEBUG")
  void shouldNotLogDisabledProperties() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setLoggingContextActivityId(null)
      .setLoggingContextBusinessKey(null)
      .setLoggingContextProcessDefinitionId("")
      .setLoggingContextEngineName(null);
    manageDeployment(modelOneTaskProcess());
    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then
    assertActivityLogs(instance, "ENGINE-200", null, true, false, false, false, null);
  }

  @Test
  @WatchLogger(loggerNames = {PVM_LOGGER, CMD_LOGGER}, level = "DEBUG")
  void shouldLogMdcPropertiesOnlyInActivityContext() {
    // given
    manageDeployment(modelOneTaskProcess());
    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then activity context logs are present
    assertActivityLogsPresent(instance, List.of("start", "waitState", "end"));
    // other logs do not contain MDC properties
    assertActivityLogsPresentWithoutMdc("ENGINE-130");
  }

  @Test
  @WatchLogger(loggerNames = {PVM_LOGGER, CMD_LOGGER}, level = "DEBUG")
  void shouldPreserveMDCExternalPropertiesAfterJobCompletion() {
    // given

    // a set of custom Logging Context parameters that populate the MDC prior to any process instance execution
    testMDCFacade.withDefaultLoggingContextParameters(
        "customActivityId",
        "customActivityName",
        "customApplicationName",
        "customBusinessKey",
        "customProcessDefinitionId",
        "customProcessDefinitionKey",
        "customProcessInstanceId",
        "customTenantId",
        "customEngineName"
    );

    // and a deployed process
    manageDeployment(modelOneTaskProcess());

    // when a process instance starts and completes
    runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    // then the activity log events should use the process instance specific values (not their external property counterparts)
    assertThat(loggingRule.getFilteredLog("ENGINE-200")).hasSize(13);

    assertActivityLogsAtRange(0, 3, "start", "process:(.*):(.*)", "(\\d)+", "start", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());
    assertActivityLogsAtRange(4, 7, "waitState", "process:(.*):(.*)", "(\\d)+", "waitState", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());
    assertActivityLogsAtRange(8, 12, "end", "process:(.*):(.*)", "(\\d)+", "end", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());

    // And the MDC External Properties are in the same state as prior to the commands execution
    testMDCFacade.assertAllInsertedPropertiesAreInMdc();
  }

  @Test
  @WatchLogger(loggerNames = {NestedLoggingDelegate.LOGGER_NAME}, level = "DEBUG")
  void shouldPreserveMDCExternalPropertiesInFlowsWithInnerCommands() {
    // given

    // a set of custom Logging Context parameters that populate the MDC prior to any process instance execution
    testMDCFacade.withDefaultLoggingContextParameters(
        "customActivityId",
        "customActivityName",
        "customApplicationName",
        "customBusinessKey",
        "customProcessDefinitionId",
        "customProcessDefinitionKey",
        "customProcessInstanceId",
        "customTenantId",
        "customEngineName"
    );

    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .serviceTask("startProcess")
        .operatonClass(NestedLoggingDelegate.class.getName())
        .endEvent("end")
        .done());

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);

    // then
    List<ILoggingEvent> customLogs = loggingRule.getLog();

    // Log events contain the execution specific MDC Properties
    assertThat(customLogs).hasSize(2);

    for (ILoggingEvent logEvent : customLogs) {
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("activityId", "startProcess");
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("businessKey", B_KEY);
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("processDefinitionId", processInstance.getProcessDefinitionId());
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("processInstanceId", processInstance.getId());
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("tenantId", processInstance.getTenantId());
    }

    // And the MDC External Properties are in the same state as prior to the commands execution
    testMDCFacade.assertAllInsertedPropertiesAreInMdc();
  }

  @Test
  @WatchLogger(loggerNames = {PVM_LOGGER, CMD_LOGGER}, level = "DEBUG")
  void shouldPreserveThirdPartyMDCProperties() {
    // given

    // Logging Context Properties
    testMDCFacade.withDefaultLoggingContextParameters(
        "customActivityId",
        "customActivityName",
        "customApplicationName",
        "customBusinessKey",
        "customProcessDefinitionId",
        "customProcessDefinitionKey",
        "customProcessInstanceId",
        "customTenantId",
        "customEngineName"
    );

    // And a custom property that does not belong to the logging context properties
    testMDCFacade.withMDCProperty("thirdPartyProperty", "withAValue");

    // and a deployed process
    manageDeployment(modelOneTaskProcess());

    // when a process instance starts and completes
    runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());

    // then the activity log events should use the process instance specific values (not their external property counterparts)
    assertThat(loggingRule.getFilteredLog("ENGINE-200")).hasSize(13);

    assertActivityLogsAtRange(0, 3, "start", "process:(.*):(.*)", "(\\d)+", "start", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());
    assertActivityLogsAtRange(4, 7, "waitState", "process:(.*):(.*)", "(\\d)+", "waitState", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());
    assertActivityLogsAtRange(8, 12, "end", "process:(.*):(.*)", "(\\d)+", "end", "testTenant", "businessKey1", engineRule.getProcessEngine().getName());

    // and the MDC should contain both the logging context properties & the third party property prior to any command execution
    Map<String, Object> mdcMap = MDC.getMap();

    assertThat(mdcMap).hasSize(10);

    testMDCFacade.assertAllInsertedPropertiesAreInMdc();
  }

  @Test
  @WatchLogger(loggerNames = {PVM_LOGGER, CMD_LOGGER}, level = "DEBUG")
  void shouldLogCustomMdcPropertiesOnlyInActivityContext() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setLoggingContextActivityId("actId")
      .setLoggingContextApplicationName("appName")
      .setLoggingContextBusinessKey("busKey")
      .setLoggingContextProcessDefinitionId("defId")
      .setLoggingContextProcessInstanceId("instId")
      .setLoggingContextTenantId("tenId")
      .setLoggingContextEngineName("engName");
    manageDeployment(modelOneTaskProcess());
    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then activity context logs are present
    assertActivityLogsPresent(instance, List.of("start", "waitState", "end"), "actId", "appName", "busKey", "defId", "instId", "tenId", "engName");
  }

  @Test
  @WatchLogger(loggerNames = {NestedLoggingDelegate.LOGGER_NAME}, level = "DEBUG")
  void shouldLogCustomMdcPropertiesWithNestedCommand() {
    // given
    engineRule.getProcessEngineConfiguration()
      .setLoggingContextActivityId("actId")
      .setLoggingContextBusinessKey("busKey")
      .setLoggingContextProcessDefinitionId("defId")
      .setLoggingContextProcessInstanceId("instId")
      .setLoggingContextTenantId("tenId");

    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
      .startEvent("start")
      .serviceTask("startProcess")
        .operatonClass(NestedLoggingDelegate.class.getName())
      .endEvent("end")
      .done());

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);

    // then
    List<ILoggingEvent> customLogs = loggingRule.getLog();
    assertThat(customLogs).hasSize(2);

    for (ILoggingEvent logEvent : customLogs) {
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("actId", "startProcess");
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("busKey", B_KEY);
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("defId", processInstance.getProcessDefinitionId());
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("instId", processInstance.getId());
      assertThat(logEvent.getMDCPropertyMap()).containsEntry("tenId", processInstance.getTenantId());
    }
  }

  @Test
  @WatchLogger(loggerNames = PVM_LOGGER, level = "DEBUG")
  void shouldLogMdcPropertiesForAsyncBeforeInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState").operatonAsyncBefore()
        .endEvent("end")
        .done());
    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    testRule.waitForJobExecutorToProcessAllJobs();
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then
    assertActivityLogsPresent(pi, List.of("start", "waitState", "end"));
  }

  @Test
  @WatchLogger(loggerNames = PVM_LOGGER, level = "DEBUG")
  void shouldLogMdcPropertiesForAsyncAfterInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState").operatonAsyncAfter()
        .endEvent("end")
        .done());
    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    testRule.waitForJobExecutorToProcessAllJobs();
    // then
    assertActivityLogsPresent(pi, List.of("start", "waitState", "end"));
  }

  @Test
  @WatchLogger(loggerNames = {JOBEXEC_LOGGER, PVM_LOGGER}, level = "DEBUG")
  void shouldLogMdcPropertiesForTimerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .intermediateCatchEvent("timer").timerWithDuration("PT10S")
        .userTask("waitState")
        .endEvent("end")
        .done());
    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(2L));
    testRule.waitForJobExecutorToProcessAllJobs();
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then
    assertActivityLogsPresent(pi, List.of("start", "timer", "waitState", "end"));
    // job executor logs do not contain MDC properties
    assertActivityLogsPresentWithoutMdc("ENGINE-140");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromDelegateInTaskContext() {
    // given
    manageDeployment(modelDelegateFailure());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromDelegateInTaskContextWithChangedBusinessKey() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("bkeyChangingTask")
          .operatonClass(BusinessKeyChangeDelegate.class)
        .serviceTask("failingTask")
          .operatonClass(FailingDelegate.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, LOG_IDENT_FAILURE, "failingTask", null, B_KEY2, 1);
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromCreateTaskListenerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .userTask("failingTask")
          .operatonTaskListenerClass(TaskListener.EVENTNAME_CREATE, FailingTaskListener.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromAssignTaskListenerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("failingTask")
          .operatonTaskListenerClass(TaskListener.EVENTNAME_ASSIGNMENT, FailingTaskListener.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.setAssignee(taskQuery, "testUser")).isInstanceOf(Exception.class);
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromCompleteTaskListenerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("failingTask")
          .operatonTaskListenerClass(TaskListener.EVENTNAME_COMPLETE, FailingTaskListener.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromDeleteTaskListenerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("failingTask")
          .operatonTaskListenerClass(TaskListener.EVENTNAME_DELETE, FailingTaskListener.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var instanceId = instance.getId();
    // when
    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(instanceId, "cancel it")).isInstanceOf(Exception.class);
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromExecutionListenerInTaskContext() {
    // given
    manageDeployment(modelExecutionListenerFailure());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = {CONTEXT_LOGGER, JOBEXEC_LOGGER}, level = "WARN")
  void shouldLogFailureFromTimeoutTaskListenerInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("failingTask")
          .operatonTaskListenerClassTimeoutWithDuration("failure-listener", FailingTaskListener.class, "PT10S")
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(2L));
    testRule.waitForJobExecutorToProcessAllJobs();
    // then
    assertFailureLogPresent(instance, "failingTask", 3);
    assertFailureLogPresent(instance, "ENGINE-14006", "failingTask", null, instance.getBusinessKey(), 3);
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromParallelTasksInCorrectTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .parallelGateway("pSplit")
          .serviceTask("task")
            .operatonClass(NoneDelegate.class)
          .endEvent("end")
        .moveToLastGateway()
          .serviceTask("failingTask")
            .operatonClass(FailingDelegate.class)
          .endEvent("failingEnd")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "DEBUG")
  void shouldLogFailureFromNestedDelegateInOuterContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(FAILING_PROCESS)
        .startEvent("failing_start")
        .serviceTask("failing_task")
          .operatonClass(FailingDelegate.class)
        .endEvent("failing_end")
        .done());
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("startProcess")
          .operatonClass(NestedStartDelegate.class.getName())
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "startProcess");
    assertBpmnStacktraceLogPresent(instance);
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "DEBUG")
  void shouldLogFailureFromNestedExecutionListenerInOuterContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(FAILING_PROCESS)
        .startEvent("failing_start")
        .serviceTask("failing_task")
          .operatonClass(NoneDelegate.class.getName())
          .operatonExecutionListenerClass("end", FailingExecutionListener.class)
        .endEvent("failing_end")
        .done());
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("startProcess")
          .operatonClass(NestedStartDelegate.class.getName())
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "startProcess");
    assertBpmnStacktraceLogPresent(instance);
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromMessageCorrelationListenerInEventContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .intermediateCatchEvent("message")
          .message("testMessage")
          .operatonExecutionListenerClass("end", FailingExecutionListener.class)
        .endEvent("end")
        .done());
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    // when
    assertThatThrownBy(() -> runtimeService.correlateMessage("testMessage")).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "message");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromEventSubprocessInSubprocessTaskContext() {
    // given
    testRule.deployForTenant(TENANT_ID, "org/operaton/bpm/engine/test/logging/ProcessDataLoggingContextTest.shouldLogFailureFromEventSubprocessInSubprocessTaskContext.bpmn20.xml");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS);
    // when
    assertThatThrownBy(() -> runtimeService.correlateMessage("testMessage")).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(instance, "sub_failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogInternalFailureInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("failingTask")
          .operatonDelegateExpression("${foo}")
        .endEvent("end")
        .done());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(pi, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogInputOutputMappingFailureInTaskContext() {
    // given
    manageDeployment(Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("failingTask")
          .operatonClass(NoneDelegate.class)
          .operatonInputParameter("foo", "${foooo}")
        .endEvent("end")
        .done());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    // then
    assertFailureLogPresent(pi, "failingTask");
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromDelegateInTaskContextInPa() {
    // given
    registerProcessEngine();
    TestApplicationReusingExistingEngine application = new TestApplicationReusingExistingEngine() {
      @Override
      public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
        deploymentBuilder.addModelInstance("test.bpmn", modelDelegateFailure()).tenantId(TENANT_ID);
      }
    };
    application.deploy();
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    application.undeploy();
    assertFailureLogInApplication(instance, "failingTask", application.getName());
  }

  @Test
  @WatchLogger(loggerNames = CONTEXT_LOGGER, level = "ERROR")
  void shouldLogFailureFromExecutionListenerInTaskContextInPa() {
    // given
    registerProcessEngine();
    TestApplicationReusingExistingEngine application = new TestApplicationReusingExistingEngine() {
      @Override
      public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
        deploymentBuilder.addModelInstance("test.bpmn", modelExecutionListenerFailure()).tenantId(TENANT_ID);
      }
    };
    application.deploy();
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS, B_KEY);
    var taskQuery = taskService.createTaskQuery().singleResult().getId();
    // when
    assertThatThrownBy(() -> taskService.complete(taskQuery)).isInstanceOf(Exception.class);
    application.undeploy();
    // then
    assertFailureLogInApplication(instance, "failingTask", application.getName());
  }

  protected void assertActivityLogsAtRange(int start, int end, String activityId, String processDefinitionId,
                                           String processInstanceId, String activityName, String tenantId, String businessKey,
                                           String engineName) {
    for (int i=start; i <= end; i++) {
      assertActivityLogsAtIndex(i, activityId, processDefinitionId, processInstanceId, activityName, tenantId, businessKey, engineName);
    }
  }

  protected void assertActivityLogsAtIndex(int index, String activityId, String processDefinitionIdRegex, String processInstanceIdRegex,
                                    String activityName, String tenantId, String businessKey, String engineName) {
    List<ILoggingEvent> logs = loggingRule.getFilteredLog("ENGINE-200");
    ILoggingEvent logEvent = logs.get(index);

    Map<String, String> mdcPropertyMap = logEvent.getMDCPropertyMap();

    assertThat(mdcPropertyMap.get("processDefinitionId")).matches(processDefinitionIdRegex);
    assertThat(mdcPropertyMap.get("processInstanceId")).matches(processInstanceIdRegex);

    assertThat(mdcPropertyMap)
            .containsEntry("activityId", activityId)
            .containsEntry("activityName", activityName)
            .containsEntry("tenantId", tenantId)
            .containsEntry("businessKey", businessKey)
            .containsEntry("engineName", engineName);
  }

  protected void clearMDCFromProperties(String... properties) {
    for (String property : properties) {
      MdcAccess.remove(property);
    }
  }

  protected void assertActivityLogsPresent(ProcessInstance instance, List<String> expectedActivities) {
    assertActivityLogs(instance, "ENGINE-200", expectedActivities, true, true, true, true, engineRule.getProcessEngine().getName());
  }

  protected void assertActivityLogsPresentWithoutMdc(String filter) {
    assertActivityLogs(null, filter, null, false, false, false, false, engineRule.getProcessEngine().getName());
  }

  protected void assertActivityLogs(ProcessInstance instance, String filter, List<String> expectedActivities, boolean isMdcPresent,
      boolean isBusinessKeyPresent, boolean isActivityIdPresent, boolean isDefinitionIdPresent, String engineName) {
    assertActivityLogs(instance, filter, isActivityIdPresent ? expectedActivities : null, null,
        isBusinessKeyPresent ? instance.getBusinessKey() : null, isDefinitionIdPresent ? instance.getProcessDefinitionId() : null, engineName,
        isMdcPresent, null);
  }

  protected void assertActivityLogsPresent(ProcessInstance instance, List<String> expectedActivities, String activityIdProperty,
      String appNameProperty, String businessKeyProperty, String definitionIdProperty, String instanceIdProperty, String tenantIdProperty, String engineNameProperty) {
    assertLogs(instance, "ENGINE-200", expectedActivities, null, instance.getBusinessKey(), instance.getProcessDefinitionId(), engineRule.getProcessEngine().getName(), true, null,
        activityIdProperty, appNameProperty, businessKeyProperty, definitionIdProperty, instanceIdProperty, tenantIdProperty, engineNameProperty);
  }

  protected void assertFailureLogPresent(ProcessInstance instance, String activityId) {
    assertFailureLogPresent(instance, activityId, 1);
  }

  protected void assertFailureLogPresent(ProcessInstance instance, String activityId, int numberOfFailureLogs) {
    assertFailureLogPresent(instance, LOG_IDENT_FAILURE, activityId, null, instance.getBusinessKey(), numberOfFailureLogs);
  }

  protected void assertFailureLogInApplication(ProcessInstance instance, String activityId, String application) {
    assertFailureLogPresent(instance, LOG_IDENT_FAILURE, activityId, application, instance.getBusinessKey(), 1);
  }

  protected void assertFailureLogPresent(ProcessInstance instance, String filter, String activityId, String appName,
      String businessKey, int numberOfFailureLogs) {
    assertActivityLogs(instance, filter, List.of(activityId), appName, businessKey, instance.getProcessDefinitionId(), engineRule.getProcessEngine().getName(), true,
        numberOfFailureLogs);
  }

  protected void assertActivityLogs(ProcessInstance instance, String filter, List<String> expectedActivities, String appName,
      String businessKey, String definitionId, String engineName, boolean isMdcPresent, Integer numberOfLogs) {
    assertLogs(instance, filter, expectedActivities, appName, businessKey, definitionId, engineName, isMdcPresent, numberOfLogs,
        "activityId", "applicationName", "businessKey", "processDefinitionId", "processInstanceId", "tenantId", "engineName");
  }

  protected void assertLogs(ProcessInstance instance, String filter, List<String> expectedActivities, String appName,
      String businessKey, String definitionId, String engineName, boolean isMdcPresent, Integer numberOfLogs, String activityIdProperty, String appNameProperty,
      String businessKeyProperty, String definitionIdProperty, String instanceIdProperty, String tenantIdProperty, String engineNameProperty) {
    boolean foundLogEntries = false;
    Set<String> passedActivities = new HashSet<>();
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog(filter);
    if (numberOfLogs != null) {
      assertThat(numberOfLogs.intValue()).isEqualTo(filteredLog.size());
    }
    for (ILoggingEvent logEvent : filteredLog) {
      Map<String, String> mdcPropertyMap = logEvent.getMDCPropertyMap();
      if (isMdcPresent) {
        // PVM log contains MDC properties
        String activityIdMdc = mdcPropertyMap.get(activityIdProperty);
        String appNameMdc = mdcPropertyMap.get(appNameProperty);
        String businessKeyMdc = mdcPropertyMap.get(businessKeyProperty);
        String definitionIdMdc = mdcPropertyMap.get(definitionIdProperty);
        String instanceIdMdc = mdcPropertyMap.get(instanceIdProperty);
        String tenantIdMdc = mdcPropertyMap.get(tenantIdProperty);
        String engineNameMdc = mdcPropertyMap.get(engineNameProperty);

        if (expectedActivities != null) {
          assertThat(activityIdMdc).isNotNull();
          assertThat(expectedActivities).contains(activityIdMdc);
          passedActivities.add(activityIdMdc);
        } else {
          assertThat(activityIdMdc).isNull();
        }

        if (appName != null) {
          assertThat(appName).isEqualTo(appNameMdc);
        } else {
          assertThat(appNameMdc).isNull();
        }

        if (businessKey != null) {
          assertThat(businessKey).isEqualTo( businessKeyMdc);
        } else {
          assertThat(businessKeyMdc).isNull();
        }

        if (definitionId != null) {
          assertThat(definitionId).isEqualTo( definitionIdMdc);
        } else {
          assertThat(definitionIdMdc).isNull();
        }

        if(engineName != null) {
          assertThat(engineName).isEqualTo( engineNameMdc);
        } else {
          assertThat(engineNameMdc).isNull();
        }

        assertThat(instanceIdMdc).isNotNull();
        assertThat(instance.getId()).isEqualTo( instanceIdMdc);

        assertThat(tenantIdMdc).isNotNull();
        assertThat(instance.getTenantId()).isEqualTo( tenantIdMdc);


      } else {
        assertThat(mdcPropertyMap).isEmpty();
      }
      foundLogEntries = true;
    }
    assertThat(foundLogEntries).isTrue();
    if (expectedActivities != null) {
      assertThat(passedActivities).containsExactlyInAnyOrderElementsOf(expectedActivities);
    }
  }

  protected void assertBpmnStacktraceLogPresent(ProcessInstance instance) {
    List<ILoggingEvent> bpmnStacktraceLog = loggingRule.getFilteredLog("ENGINE-16006");
    assertThat(bpmnStacktraceLog).hasSize(2);
    for (int i = 0; i < bpmnStacktraceLog.size(); i++) {
      ILoggingEvent logEvent = bpmnStacktraceLog.get(i);
      Map<String, String> mdcPropertyMap = logEvent.getMDCPropertyMap();
      assertThat(mdcPropertyMap).containsKey("activityId");
      assertThat(mdcPropertyMap.containsKey("applicationName")).isFalse();
      assertThat(mdcPropertyMap)
              .containsKey("processDefinitionId")
              .containsKey("processInstanceId")
              .containsKey("tenantId");
      if (i == 0) {
        // first BPMN stack trace log corresponds to nested service task
        assertThat(mdcPropertyMap.containsKey("businessKey")).isFalse();
        assertThat(mdcPropertyMap)
                .containsEntry("activityId", "failing_task")
                .doesNotContainEntry("processDefinitionId", instance.getProcessDefinitionId())
                .doesNotContainEntry("processInstanceId", instance.getId());
        assertThat(instance.getTenantId()).isEqualTo(mdcPropertyMap.get("tenantId"));
      } else {
        // second BPMN stack trace log corresponds to outer service task
        assertThat(mdcPropertyMap)
                .containsKey("businessKey")
                .containsEntry("activityId", "startProcess");
        assertThat(instance.getBusinessKey()).isEqualTo(mdcPropertyMap.get("businessKey"));
        assertThat(instance.getProcessDefinitionId()).isEqualTo(mdcPropertyMap.get("processDefinitionId"));
        assertThat(instance.getId()).isEqualTo(mdcPropertyMap.get("processInstanceId"));
        assertThat(instance.getTenantId()).isEqualTo(mdcPropertyMap.get("tenantId"));
      }
    }
  }

  protected void manageDeployment(BpmnModelInstance model) {
    testRule.deployForTenant(TENANT_ID, model);
  }

  protected void registerProcessEngine() {
    runtimeContainerDelegate.registerProcessEngine(engineRule.getProcessEngine());
    defaultEngineRegistered = true;
  }

  protected BpmnModelInstance modelOneTaskProcess() {
    return Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .endEvent("end")
        .done();
  }

  protected BpmnModelInstance modelDelegateFailure() {
    return Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("waitState")
        .serviceTask("failingTask")
          .operatonClass(FailingDelegate.class)
        .endEvent("end")
        .done();
  }

  protected BpmnModelInstance modelExecutionListenerFailure() {
    return Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
        .userTask("failingTask")
          .operatonExecutionListenerClass("end", FailingExecutionListener.class)
        .endEvent("end")
        .done();
  }

  public static class NestedStartDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      RuntimeService runtimeService = execution.getProcessEngine().getRuntimeService();
      runtimeService.startProcessInstanceByKey(FAILING_PROCESS, (String) null);
    }
  }

  public static class NestedLoggingDelegate implements JavaDelegate {

    public static final String LOGGER_NAME = "custom-logger";
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public void execute(DelegateExecution execution) throws Exception {

      RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();

      LOGGER.info("Before API call");
      // to reproduce CAM-12272, it is important to make an API call between the logging statements
      // (regardless if the call is meaningful)
      runtimeService.createProcessInstanceQuery().list();
      LOGGER.info("After API call");
    }

  }

  public static class FailingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      throw new IllegalArgumentException("I am always failing!");
    }
  }

  public static class NoneDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      // nothing to do
    }
  }

  public static class BusinessKeyChangeDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      execution.setProcessBusinessKey(B_KEY2);
    }
  }

  public static class FailingTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      throw new IllegalArgumentException("I am failing!");
    }
  }

  public static class FailingExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
      throw new IllegalArgumentException("I am failing!");
    }
  }
}
