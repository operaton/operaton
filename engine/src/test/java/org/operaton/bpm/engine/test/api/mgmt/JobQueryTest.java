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
package org.operaton.bpm.engine.test.api.mgmt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.DeleteJobsCmd;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
@Parameterized
public class JobQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  private CommandExecutor commandExecutor;

  private String deploymentId;
  private String messageId;
  private TimerEntity timerEntity;
  private boolean defaultEnsureJobDueDateSet;

  private Date testStartTime;
  private Date timerOneFireTime;
  private Date timerTwoFireTime;
  private Date timerThreeFireTime;
  private Date messageDueDate;

  private String processInstanceIdOne;
  private String processInstanceIdTwo;
  private String processInstanceIdThree;

  private static final long ONE_HOUR = 60L * 60L * 1000L;
  private static final long ONE_SECOND = 1000L;
  private static final String EXCEPTION_MESSAGE = "java.lang.RuntimeException: This is an exception thrown from scriptTask";

  @Parameter
  public boolean ensureJobDueDateSet;

  @Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { false },
      { true }
    });
  }

  /**
   * Setup will create
   *   - 3 process instances, each with one timer, each firing at t1/t2/t3 + 1 hour (see process)
   *   - 1 message
   */
  @BeforeEach
  void setUp() {
    commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();

    defaultEnsureJobDueDateSet = processEngineConfiguration.isEnsureJobDueDateNotNull();
    processEngineConfiguration.setEnsureJobDueDateNotNull(ensureJobDueDateSet);

    deploymentId = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/timerOnTask.bpmn20.xml")
        .deploy()
        .getId();

    // Create proc inst that has timer that will fire on t1 + 1 hour
    Calendar startTime = Calendar.getInstance();
    startTime.set(Calendar.MILLISECOND, 0);

    Date t1 = startTime.getTime();
    ClockUtil.setCurrentTime(t1);
    processInstanceIdOne = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    testStartTime = t1;
    timerOneFireTime = new Date(t1.getTime() + ONE_HOUR);

    // Create proc inst that has timer that will fire on t2 + 1 hour
    startTime.add(Calendar.HOUR_OF_DAY, 1);
    Date t2 = startTime.getTime();  // t2 = t1 + 1 hour
    ClockUtil.setCurrentTime(t2);
    processInstanceIdTwo = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    timerTwoFireTime = new Date(t2.getTime() + ONE_HOUR);

    // Create proc inst that has timer that will fire on t3 + 1 hour
    startTime.add(Calendar.HOUR_OF_DAY, 1);
    final Date t3 = startTime.getTime(); // t3 = t2 + 1 hour
    ClockUtil.setCurrentTime(t3);
    processInstanceIdThree = runtimeService.startProcessInstanceByKey("timerOnTask").getId();
    timerThreeFireTime = new Date(t3.getTime() + ONE_HOUR);

    // Message.StartTime = Message.DueDate
    startTime.add(Calendar.HOUR_OF_DAY, 2);
    messageDueDate = startTime.getTime();

    // Create one message
    messageId = commandExecutor.execute(commandContext -> {
      MessageEntity message = new MessageEntity();

      if (ensureJobDueDateSet) {
        message.setDuedate(messageDueDate);
      }

      commandContext.getJobManager().send(message);
      return message.getId();
    });
  }

  @AfterEach
  void tearDown() {
    repositoryService.deleteDeployment(deploymentId, true);
    commandExecutor.execute(new DeleteJobsCmd(messageId, true));
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @TestTemplate
  void testQueryByNoCriteria() {
    JobQuery query = managementService.createJobQuery();
    verifyQueryResults(query, 4);
  }

  @TestTemplate
  void testQueryByActivityId(){
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    JobQuery query = managementService.createJobQuery().activityId(jobDefinition.getActivityId());
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByInvalidActivityId(){
    JobQuery query = managementService.createJobQuery().activityId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    assertThatThrownBy(() -> jobQuery.activityId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided activity id is null");
  }

  @TestTemplate
  void testByJobDefinitionId() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    JobQuery query = managementService.createJobQuery().jobDefinitionId(jobDefinition.getId());
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testByInvalidJobDefinitionId() {
    JobQuery query = managementService.createJobQuery().jobDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    assertThatThrownBy(() -> jobQuery.jobDefinitionId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided job definition id is null");
  }

  @TestTemplate
  void testQueryByProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testQueryByInvalidProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();
    assertThatThrownBy(() -> jobQuery.processInstanceId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided process instance id is null");
  }

  @TestTemplate
  void testQueryByExecutionId() {
    Job job = managementService.createJobQuery().processInstanceId(processInstanceIdOne).singleResult();
    JobQuery query = managementService.createJobQuery().executionId(job.getExecutionId());
    assertThat(job.getId()).isEqualTo(query.singleResult().getId());
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testQueryByInvalidExecutionId() {
    JobQuery query = managementService.createJobQuery().executionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();
    assertThatThrownBy(() -> jobQuery.executionId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided execution id is null");
  }

  @TestTemplate
  void testQueryByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().list().get(0);

    JobQuery query = managementService.createJobQuery().processDefinitionId(processDefinition.getId());
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByInvalidProcessDefinitionId() {
    JobQuery query = managementService.createJobQuery().processDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    assertThatThrownBy(() -> jobQuery.processDefinitionId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided process definition id is null");
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobQueryTest.testTimeCycleQueryByProcessDefinitionId.bpmn20.xml"})
  void testTimeCycleQueryByProcessDefinitionId() {
    String processDefinitionId = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process")
        .singleResult()
        .getId();

    JobQuery query = managementService.createJobQuery().processDefinitionId(processDefinitionId);

    verifyQueryResults(query, 1);

    String jobId = query.singleResult().getId();
    managementService.executeJob(jobId);

    verifyQueryResults(query, 1);

    String anotherJobId = query.singleResult().getId();
    assertThat(anotherJobId).isNotEqualTo(jobId);
  }

  @TestTemplate
  void testQueryByProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("timerOnTask");
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByInvalidProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();
    assertThatThrownBy(() -> jobQuery.processDefinitionKey(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided process instance key is null");
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobQueryTest.testTimeCycleQueryByProcessDefinitionId.bpmn20.xml"})
  void testTimeCycleQueryByProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("process");

    verifyQueryResults(query, 1);

    String jobId = query.singleResult().getId();
    managementService.executeJob(jobId);

    verifyQueryResults(query, 1);

    String anotherJobId = query.singleResult().getId();
    assertThat(anotherJobId).isNotEqualTo(jobId);
  }

  @TestTemplate
  void testQueryByRetriesLeft() {
    JobQuery query = managementService.createJobQuery().withRetriesLeft();
    verifyQueryResults(query, 4);

    setRetries(processInstanceIdOne, 0);
    // Re-running the query should give only 3 jobs now, since one job has retries=0
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByExecutable() {
    long testTime = ensureJobDueDateSet? messageDueDate.getTime() : timerThreeFireTime.getTime();
    int expectedCount = ensureJobDueDateSet? 0 : 1;

    ClockUtil.setCurrentTime(new Date(testTime + ONE_SECOND)); // all jobs should be executable at t3 + 1hour.1second
    JobQuery query = managementService.createJobQuery().executable();
    verifyQueryResults(query, 4);

    // Setting retries of one job to 0, makes it non-executable
    setRetries(processInstanceIdOne, 0);
    verifyQueryResults(query, 3);

    // Setting the clock before the start of the process instance, makes none of the jobs executable
    ClockUtil.setCurrentTime(testStartTime);
    verifyQueryResults(query, expectedCount); // 1, since a message is always executable when retries > 0
  }

  @TestTemplate
  void testQueryByOnlyTimers() {
    JobQuery query = managementService.createJobQuery().timers();
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByOnlyMessages() {
    JobQuery query = managementService.createJobQuery().messages();
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testInvalidOnlyTimersUsage() {
    var jobQuery = managementService.createJobQuery().timers();
    assertThatThrownBy(jobQuery::messages)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot combine onlyTimers() with onlyMessages() in the same query");
  }

  @TestTemplate
  @SuppressWarnings("deprecation")
  void testQueryByDuedateLowerThen() {
    JobQuery query = managementService.createJobQuery().duedateLowerThen(testStartTime);
    verifyQueryResults(query, 0);

    query = managementService.createJobQuery().duedateLowerThen(new Date(timerOneFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateLowerThen(new Date(timerTwoFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 2);

    query = managementService.createJobQuery().duedateLowerThen(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 3);

    if (ensureJobDueDateSet) {
      query = managementService.createJobQuery().duedateLowerThen(new Date(messageDueDate.getTime() + ONE_SECOND));
      verifyQueryResults(query, 4);
    }
  }

  @TestTemplate
  @SuppressWarnings("deprecation")
  void testQueryByDuedateLowerThenOrEqual() {
    JobQuery query = managementService.createJobQuery().duedateLowerThenOrEquals(testStartTime);
    verifyQueryResults(query, 0);

    query = managementService.createJobQuery().duedateLowerThenOrEquals(timerOneFireTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery().duedateLowerThenOrEquals(timerTwoFireTime);
    verifyQueryResults(query, 2);

    query = managementService.createJobQuery().duedateLowerThenOrEquals(timerThreeFireTime);
    verifyQueryResults(query, 3);

    if (ensureJobDueDateSet) {
      query = managementService.createJobQuery().duedateLowerThenOrEquals(messageDueDate);
      verifyQueryResults(query, 4);
    }
  }

  @TestTemplate
  @SuppressWarnings("deprecation")
  void testQueryByDuedateHigherThen() {
    int startTimeExpectedCount = ensureJobDueDateSet? 4 : 3;
    int timerOneExpectedCount = ensureJobDueDateSet? 3 : 2;
    int timerTwoExpectedCount = ensureJobDueDateSet? 2 : 1;
    int timerThreeExpectedCount = ensureJobDueDateSet? 1 : 0;

    JobQuery query = managementService.createJobQuery().duedateHigherThen(testStartTime);
    verifyQueryResults(query, startTimeExpectedCount);

    query = managementService.createJobQuery().duedateHigherThen(timerOneFireTime);
    verifyQueryResults(query, timerOneExpectedCount);

    query = managementService.createJobQuery().duedateHigherThen(timerTwoFireTime);
    verifyQueryResults(query, timerTwoExpectedCount);

    query = managementService.createJobQuery().duedateHigherThen(timerThreeFireTime);
    verifyQueryResults(query, timerThreeExpectedCount);

    if (ensureJobDueDateSet) {
      query = managementService.createJobQuery().duedateHigherThen(messageDueDate);
      verifyQueryResults(query, 0);
    }
  }

  @TestTemplate
  @SuppressWarnings("deprecation")
  void testQueryByDuedateHigherThenOrEqual() {
    int startTimeExpectedCount = ensureJobDueDateSet? 4 : 3;
    int timerOneExpectedCount = ensureJobDueDateSet? 3 : 2;
    int timerTwoExpectedCount = ensureJobDueDateSet? 2 : 1;
    int timerThreeExpectedCount = ensureJobDueDateSet? 1 : 0;

    JobQuery query = managementService.createJobQuery().duedateHigherThenOrEquals(testStartTime);
    verifyQueryResults(query, startTimeExpectedCount);

    query = managementService.createJobQuery().duedateHigherThenOrEquals(timerOneFireTime);
    verifyQueryResults(query, startTimeExpectedCount);

    query = managementService.createJobQuery().duedateHigherThenOrEquals(new Date(timerOneFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, timerOneExpectedCount);

    query = managementService.createJobQuery().duedateHigherThenOrEquals(timerThreeFireTime);
    verifyQueryResults(query, timerTwoExpectedCount);

    query = managementService.createJobQuery().duedateHigherThenOrEquals(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, timerThreeExpectedCount);

    if (ensureJobDueDateSet) {
      query = managementService.createJobQuery().duedateHigherThenOrEquals(new Date(messageDueDate.getTime() + ONE_SECOND));
      verifyQueryResults(query, 0);
    }
  }

  @TestTemplate
  void testQueryByDuedateCombinations() {
    JobQuery query = managementService.createJobQuery()
        .duedateHigherThan(testStartTime)
        .duedateLowerThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 3);

    query = managementService.createJobQuery()
        .duedateHigherThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND))
        .duedateLowerThan(testStartTime);
    verifyQueryResults(query, 0);
  }

  @TestTemplate
  void testQueryByCreateTimeCombinations() {
    JobQuery query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne);
    List<Job> jobs = query.list();
    assertThat(jobs).hasSize(1);
    Date jobCreateTime = jobs.get(0).getCreateTime();

    query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne)
            .createdAfter(new Date(jobCreateTime.getTime() - 1));
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne)
            .createdAfter(jobCreateTime);
    verifyQueryResults(query, 0);

    query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne)
            .createdBefore(jobCreateTime);
    verifyQueryResults(query, 1);

    query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne)
            .createdBefore(new Date(jobCreateTime.getTime() - 1));
    verifyQueryResults(query, 0);
  }

  @TestTemplate
  void shouldReturnNoJobDueToExcludingCriteria() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);

    List<Job> jobs = query.list();
    assertThat(jobs).hasSize(1);

    query = query.createdBefore(new Date(0)).createdAfter(new Date());

    verifyQueryResults(query, 0);
  }

  @TestTemplate
  void shouldReturnJobDueToIncludingCriteria() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);

    List<Job> jobs = query.list();
    assertThat(jobs).hasSize(1);

    query = query.createdBefore(new Date()).createdAfter(new Date(0));

    verifyQueryResults(query, 1);
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  void testQueryByException() {
    JobQuery query = managementService.createJobQuery().withException();
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().withException();
    verifyFailedJob(query, processInstance);
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  void testQueryByExceptionMessage() {
    JobQuery query = managementService.createJobQuery().exceptionMessage(EXCEPTION_MESSAGE);
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    query = managementService.createJobQuery().exceptionMessage(job.getExceptionMessage());
    verifyFailedJob(query, processInstance);
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  void testQueryByExceptionMessageEmpty() {
    JobQuery query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);

    startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);
  }

  @TestTemplate
  void testQueryByExceptionMessageNull() {
    var jobQuery = managementService.createJobQuery();
    assertThatThrownBy(() -> jobQuery.exceptionMessage(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided exception message is null");
  }

  @TestTemplate
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  void testQueryByFailedActivityId(){
    JobQuery query = managementService.createJobQuery().failedActivityId("theScriptTask");
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().failedActivityId("theScriptTask");
    verifyFailedJob(query, processInstance);
  }

  @TestTemplate
  void testQueryByInvalidFailedActivityId(){
    JobQuery query = managementService.createJobQuery().failedActivityId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    assertThatThrownBy(() -> jobQuery.failedActivityId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Provided activity id is null");
  }


  @TestTemplate
  void testJobQueryWithExceptions() {

    createJobWithoutExceptionMsg();

    Job job = managementService.createJobQuery().jobId(timerEntity.getId()).singleResult();

    assertThat(job).isNotNull();

    List<Job> list = managementService.createJobQuery().withException().list();
    assertThat(list).hasSize(1);

    deleteJobInDatabase();

    createJobWithoutExceptionStacktrace();

    job = managementService.createJobQuery().jobId(timerEntity.getId()).singleResult();

    assertThat(job).isNotNull();

    list = managementService.createJobQuery().withException().list();
    assertThat(list).hasSize(1);

    deleteJobInDatabase();

  }

  @TestTemplate
  void testQueryByNoRetriesLeft() {
    JobQuery query = managementService.createJobQuery().noRetriesLeft();
    verifyQueryResults(query, 0);

    setRetries(processInstanceIdOne, 0);
    // Re-running the query should give only one jobs now, since three job has retries>0
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testQueryByActive() {
    JobQuery query = managementService.createJobQuery().active();
    verifyQueryResults(query, 4);
  }

  @TestTemplate
  void testQueryBySuspended() {
    JobQuery query = managementService.createJobQuery().suspended();
    verifyQueryResults(query, 0);

    managementService.suspendJobDefinitionByProcessDefinitionKey("timerOnTask", true);
    verifyQueryResults(query, 3);
  }

  @TestTemplate
  void testQueryByJobIdsWithOneId() {
    // given
    String id = managementService.createJobQuery().processInstanceId(processInstanceIdOne).singleResult().getId();
    // when
    JobQuery query = managementService.createJobQuery().jobIds(Collections.singleton(id));
    // then
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testQueryByJobIdsWithMultipleIds() {
    // given
    Set<String> ids = managementService.createJobQuery().list().stream()
        .map(Job::getId).collect(Collectors.toSet());
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 4);
  }

  @TestTemplate
  void testQueryByJobIdsWithMultipleIdsIncludingFakeIds() {
    // given
    Set<String> ids = managementService.createJobQuery().list().stream().map(Job::getId).collect(Collectors.toSet());
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 4);
  }

  @TestTemplate
  void testQueryByJobIdsWithEmptyList() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = Collections.emptySet();

    // when/then
    assertThatThrownBy(() -> jobQuery.jobIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of job ids is empty");
  }

  @TestTemplate
  void testQueryByJobIdsWithNull() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = null;

    // when/then
    assertThatThrownBy(() -> jobQuery.jobIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of job ids is null");
  }

  @TestTemplate
  void testQueryByJobIdsWithFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 0);
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithOneId() {
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(Collections.singleton(processInstanceIdOne));
    // then
    verifyQueryResults(query, 1);
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithMultipleIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, processInstanceIdOne, processInstanceIdThree);
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 2);
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithMultipleIdsIncludingFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, processInstanceIdOne, processInstanceIdThree, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 2);
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithEmptyList() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = Collections.emptySet();

    // when/then
    assertThatThrownBy(() -> jobQuery.processInstanceIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is empty");
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithNull() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = null;

    // when/then
    assertThatThrownBy(() -> jobQuery.processInstanceIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is null");
  }

  @TestTemplate
  void testQueryByProcessInstanceIdsWithFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 0);
  }

  //sorting //////////////////////////////////////////

  @TestTemplate
  void testQuerySorting() {
    // asc
    assertThat(managementService.createJobQuery().orderByJobId().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByJobDuedate().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByExecutionId().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessInstanceId().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByJobRetries().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessDefinitionKey().asc().count()).isEqualTo(4);

    // desc
    assertThat(managementService.createJobQuery().orderByJobId().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByJobDuedate().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByExecutionId().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessInstanceId().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByJobRetries().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(4);
    assertThat(managementService.createJobQuery().orderByProcessDefinitionKey().desc().count()).isEqualTo(4);

    // sorting on multiple fields
    setRetries(processInstanceIdTwo, 2);
    ClockUtil.setCurrentTime(new Date(timerThreeFireTime.getTime() + ONE_SECOND)); // make sure all timers can fire

    JobQuery query = managementService.createJobQuery()
      .timers()
      .executable()
      .orderByJobRetries()
      .asc()
      .orderByJobDuedate()
      .desc();

    List<Job> jobs = query.list();
    assertThat(jobs).hasSize(3);

    assertThat(jobs.get(0).getRetries()).isEqualTo(2);
    assertThat(jobs.get(1).getRetries()).isEqualTo(3);
    assertThat(jobs.get(2).getRetries()).isEqualTo(3);

    assertThat(jobs.get(0).getProcessInstanceId()).isEqualTo(processInstanceIdTwo);
    assertThat(jobs.get(1).getProcessInstanceId()).isEqualTo(processInstanceIdThree);
    assertThat(jobs.get(2).getProcessInstanceId()).isEqualTo(processInstanceIdOne);
  }

  @TestTemplate
  void testQueryInvalidSortingUsage() {
    var jobQuery = managementService.createJobQuery().orderByJobId();
    assertThatThrownBy(jobQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("call asc() or desc() after using orderByXX()");

    var jobQuery2 = managementService.createJobQuery();
    assertThatThrownBy(jobQuery2::asc)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("You should call any of the orderBy methods first before specifying a direction");
  }

  @TestTemplate
  void testQueryByAcquired() {
    Calendar lockExpDate = Calendar.getInstance();
    //given - lock expiration date in future
    lockExpDate.add(Calendar.MILLISECOND, 30000000);

    createJobWithLockExpiration(lockExpDate.getTime());

    Job job = managementService.createJobQuery().jobId(timerEntity.getId()).singleResult();
    assertThat(job).isNotNull();

    List<Job> list = managementService.createJobQuery().acquired().list();
    assertThat(list).hasSize(1);
    deleteJobInDatabase();

    //given - lock expiration date in the past
    lockExpDate.add(Calendar.MILLISECOND, -60000000);
    createJobWithLockExpiration(lockExpDate.getTime());

    list = managementService.createJobQuery().acquired().list();
    assertThat(list).isEmpty();

    deleteJobInDatabase();
  }

  //helper ////////////////////////////////////////////////////////////

  private void setRetries(final String processInstanceId, final int retries) {
    final Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
    commandExecutor.execute(commandContext -> {
      JobEntity timer = commandContext.getDbEntityManager().selectById(JobEntity.class, job.getId());
      timer.setRetries(retries);
      return null;
    });
  }

  private void createJobWithLockExpiration(Date lockDate) {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      timerEntity = new TimerEntity();
      timerEntity.setLockOwner(UUID.randomUUID().toString());
      timerEntity.setDuedate(new Date());
      timerEntity.setRetries(0);
      timerEntity.setLockExpirationTime(lockDate);

      jobManager.insert(timerEntity);

      assertThat(timerEntity.getId()).isNotNull();

      return null;

    });
  }

  private ProcessInstance startProcessInstanceWithFailingJob() {
    // start a process with a failing job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event which we will execute manual for testing purposes.
    Job timerJob = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    String timerJobId = timerJob.getId();

    executeJobExpectingException(managementService, timerJobId, EXCEPTION_MESSAGE);
    return processInstance;
  }

  private void verifyFailedJob(JobQuery query, ProcessInstance processInstance) {
    verifyQueryResults(query, 1);

    Job failedJob = query.singleResult();
    assertThat(failedJob).isNotNull();
    assertThat(failedJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertThat(failedJob.getExceptionMessage()).isNotNull();
    assertThat(failedJob.getExceptionMessage()).contains(EXCEPTION_MESSAGE);
  }

  private void createJobWithoutExceptionMsg() {
    commandExecutor.execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();

      timerEntity = new TimerEntity();
      timerEntity.setLockOwner(UUID.randomUUID().toString());
      timerEntity.setDuedate(new Date());
      timerEntity.setRetries(0);

      StringWriter stringWriter = new StringWriter();
      NullPointerException exception = new NullPointerException();
      exception.printStackTrace(new PrintWriter(stringWriter));
      timerEntity.setExceptionStacktrace(stringWriter.toString());

      jobManager.insert(timerEntity);

      assertThat(timerEntity.getId()).isNotNull();

      return null;

    });

  }

  private void createJobWithoutExceptionStacktrace() {
    commandExecutor.execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();

      timerEntity = new TimerEntity();
      timerEntity.setLockOwner(UUID.randomUUID().toString());
      timerEntity.setDuedate(new Date());
      timerEntity.setRetries(0);
      timerEntity.setExceptionMessage("I'm supposed to fail");

      jobManager.insert(timerEntity);

      assertThat(timerEntity.getId()).isNotNull();

      return null;

    });

  }

  private void deleteJobInDatabase() {
      commandExecutor.execute(commandContext -> {

        timerEntity.delete();

        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(timerEntity.getId());

        List<HistoricIncident> historicIncidents = Context
            .getProcessEngineConfiguration()
            .getHistoryService()
            .createHistoricIncidentQuery()
            .list();

        for (HistoricIncident historicIncident : historicIncidents) {
          commandContext
              .getDbEntityManager()
              .delete((DbEntity) historicIncident);
        }

        return null;
      });
  }

}
