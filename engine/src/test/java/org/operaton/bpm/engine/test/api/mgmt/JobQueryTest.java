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
package org.operaton.bpm.engine.test.api.mgmt;

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
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
@RunWith(Parameterized.class)
public class JobQueryTest {

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

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

  @Parameterized.Parameter
  public boolean ensureJobDueDateSet;

  @Parameterized.Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return Arrays.asList(new Object[][] {
      { false },
      { true }
    });
  }

  /**
   * Setup will create
   *   - 3 process instances, each with one timer, each firing at t1/t2/t3 + 1 hour (see process)
   *   - 1 message
   */
  @Before
  public void setUp() {
    processEngineConfiguration = rule.getProcessEngineConfiguration();
    runtimeService = rule.getRuntimeService();
    repositoryService = rule.getRepositoryService();
    managementService = rule.getManagementService();
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

  @After
  public void tearDown() {
    repositoryService.deleteDeployment(deploymentId, true);
    commandExecutor.execute(new DeleteJobsCmd(messageId, true));
    processEngineConfiguration.setEnsureJobDueDateNotNull(defaultEnsureJobDueDateSet);
  }

  @Test
  public void testQueryByNoCriteria() {
    JobQuery query = managementService.createJobQuery();
    verifyQueryResults(query, 4);
  }

  @Test
  public void testQueryByActivityId(){
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    JobQuery query = managementService.createJobQuery().activityId(jobDefinition.getActivityId());
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByInvalidActivityId(){
    JobQuery query = managementService.createJobQuery().activityId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.activityId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided activity id is null");
    }
  }

  @Test
  public void testByJobDefinitionId() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    JobQuery query = managementService.createJobQuery().jobDefinitionId(jobDefinition.getId());
    verifyQueryResults(query, 3);
  }

  @Test
  public void testByInvalidJobDefinitionId() {
    JobQuery query = managementService.createJobQuery().jobDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.jobDefinitionId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided job definition id is null");
    }
  }

  @Test
  public void testQueryByProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByInvalidProcessInstanceId() {
    JobQuery query = managementService.createJobQuery().processInstanceId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.processInstanceId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided process instance id is null");
    }
  }

  @Test
  public void testQueryByExecutionId() {
    Job job = managementService.createJobQuery().processInstanceId(processInstanceIdOne).singleResult();
    JobQuery query = managementService.createJobQuery().executionId(job.getExecutionId());
    assertThat(job.getId()).isEqualTo(query.singleResult().getId());
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByInvalidExecutionId() {
    JobQuery query = managementService.createJobQuery().executionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.executionId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided execution id is null");
    }
  }

  @Test
  public void testQueryByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().list().get(0);

    JobQuery query = managementService.createJobQuery().processDefinitionId(processDefinition.getId());
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByInvalidProcessDefinitionId() {
    JobQuery query = managementService.createJobQuery().processDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.processDefinitionId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided process definition id is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobQueryTest.testTimeCycleQueryByProcessDefinitionId.bpmn20.xml"})
  public void testTimeCycleQueryByProcessDefinitionId() {
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
    assertNotEquals(jobId, anotherJobId);
  }

  @Test
  public void testQueryByProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("timerOnTask");
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByInvalidProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.processDefinitionKey(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided process instance key is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobQueryTest.testTimeCycleQueryByProcessDefinitionId.bpmn20.xml"})
  public void testTimeCycleQueryByProcessDefinitionKey() {
    JobQuery query = managementService.createJobQuery().processDefinitionKey("process");

    verifyQueryResults(query, 1);

    String jobId = query.singleResult().getId();
    managementService.executeJob(jobId);

    verifyQueryResults(query, 1);

    String anotherJobId = query.singleResult().getId();
    assertNotEquals(jobId, anotherJobId);
  }

  @Test
  public void testQueryByRetriesLeft() {
    JobQuery query = managementService.createJobQuery().withRetriesLeft();
    verifyQueryResults(query, 4);

    setRetries(processInstanceIdOne, 0);
    // Re-running the query should give only 3 jobs now, since one job has retries=0
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByExecutable() {
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

  @Test
  public void testQueryByOnlyTimers() {
    JobQuery query = managementService.createJobQuery().timers();
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByOnlyMessages() {
    JobQuery query = managementService.createJobQuery().messages();
    verifyQueryResults(query, 1);
  }

  @Test
  public void testInvalidOnlyTimersUsage() {
    var jobQuery = managementService.createJobQuery().timers();
    try {
      jobQuery.messages();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Cannot combine onlyTimers() with onlyMessages() in the same query");
    }
  }

  @Test
  public void testQueryByDuedateLowerThen() {
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

  @Test
  public void testQueryByDuedateLowerThenOrEqual() {
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

  @Test
  public void testQueryByDuedateHigherThen() {
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

  @Test
  public void testQueryByDuedateHigherThenOrEqual() {
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

  @Test
  public void testQueryByDuedateCombinations() {
    JobQuery query = managementService.createJobQuery()
        .duedateHigherThan(testStartTime)
        .duedateLowerThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND));
    verifyQueryResults(query, 3);

    query = managementService.createJobQuery()
        .duedateHigherThan(new Date(timerThreeFireTime.getTime() + ONE_SECOND))
        .duedateLowerThan(testStartTime);
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryByCreateTimeCombinations() {
    JobQuery query = managementService.createJobQuery()
            .processInstanceId(processInstanceIdOne);
    List<Job> jobs = query.list();
    assertThat(jobs.size()).isEqualTo(1);
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

  @Test
  public void shouldReturnNoJobDueToExcludingCriteria() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);

    List<Job> jobs = query.list();
    assertThat(jobs.size()).isEqualTo(1);

    query = query.createdBefore(new Date(0)).createdAfter(new Date());

    verifyQueryResults(query, 0);
  }

  @Test
  public void shouldReturnJobDueToIncludingCriteria() {
    JobQuery query = managementService.createJobQuery().processInstanceId(processInstanceIdOne);

    List<Job> jobs = query.list();
    assertThat(jobs.size()).isEqualTo(1);

    query = query.createdBefore(new Date()).createdAfter(new Date(0));

    verifyQueryResults(query, 1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  public void testQueryByException() {
    JobQuery query = managementService.createJobQuery().withException();
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().withException();
    verifyFailedJob(query, processInstance);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  public void testQueryByExceptionMessage() {
    JobQuery query = managementService.createJobQuery().exceptionMessage(EXCEPTION_MESSAGE);
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    query = managementService.createJobQuery().exceptionMessage(job.getExceptionMessage());
    verifyFailedJob(query, processInstance);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  public void testQueryByExceptionMessageEmpty() {
    JobQuery query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);

    startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().exceptionMessage("");
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryByExceptionMessageNull() {
    var jobQuery = managementService.createJobQuery();
    try {
      jobQuery.exceptionMessage(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided exception message is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  public void testQueryByFailedActivityId(){
    JobQuery query = managementService.createJobQuery().failedActivityId("theScriptTask");
    verifyQueryResults(query, 0);

    ProcessInstance processInstance = startProcessInstanceWithFailingJob();

    query = managementService.createJobQuery().failedActivityId("theScriptTask");
    verifyFailedJob(query, processInstance);
  }

  @Test
  public void testQueryByInvalidFailedActivityId(){
    JobQuery query = managementService.createJobQuery().failedActivityId("invalid");
    verifyQueryResults(query, 0);
    var jobQuery = managementService.createJobQuery();

    try {
      jobQuery.failedActivityId(null);
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Provided activity id is null");
    }
  }


  @Test
  public void testJobQueryWithExceptions() {

    createJobWithoutExceptionMsg();

    Job job = managementService.createJobQuery().jobId(timerEntity.getId()).singleResult();

    assertNotNull(job);

    List<Job> list = managementService.createJobQuery().withException().list();
    assertThat(list.size()).isEqualTo(1);

    deleteJobInDatabase();

    createJobWithoutExceptionStacktrace();

    job = managementService.createJobQuery().jobId(timerEntity.getId()).singleResult();

    assertNotNull(job);

    list = managementService.createJobQuery().withException().list();
    assertThat(list.size()).isEqualTo(1);

    deleteJobInDatabase();

  }

  @Test
  public void testQueryByNoRetriesLeft() {
    JobQuery query = managementService.createJobQuery().noRetriesLeft();
    verifyQueryResults(query, 0);

    setRetries(processInstanceIdOne, 0);
    // Re-running the query should give only one jobs now, since three job has retries>0
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByActive() {
    JobQuery query = managementService.createJobQuery().active();
    verifyQueryResults(query, 4);
  }

  @Test
  public void testQueryBySuspended() {
    JobQuery query = managementService.createJobQuery().suspended();
    verifyQueryResults(query, 0);

    managementService.suspendJobDefinitionByProcessDefinitionKey("timerOnTask", true);
    verifyQueryResults(query, 3);
  }

  @Test
  public void testQueryByJobIdsWithOneId() {
    // given
    String id = managementService.createJobQuery().processInstanceId(processInstanceIdOne).singleResult().getId();
    // when
    JobQuery query = managementService.createJobQuery().jobIds(Collections.singleton(id));
    // then
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByJobIdsWithMultipleIds() {
    // given
    Set<String> ids = managementService.createJobQuery().list().stream()
        .map(Job::getId).collect(Collectors.toSet());
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 4);
  }

  @Test
  public void testQueryByJobIdsWithMultipleIdsIncludingFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    ids.addAll(managementService.createJobQuery().list().stream().map(Job::getId).collect(Collectors.toSet()));
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 4);
  }

  @Test
  public void testQueryByJobIdsWithEmptyList() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = Collections.emptySet();

    // when/then
    assertThatThrownBy(() -> jobQuery.jobIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of job ids is empty");
  }

  @Test
  public void testQueryByJobIdsWithNull() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = null;

    // when/then
    assertThatThrownBy(() -> jobQuery.jobIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of job ids is null");
  }

  @Test
  public void testQueryByJobIdsWithFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().jobIds(ids);
    // then
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryByProcessInstanceIdsWithOneId() {
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(Collections.singleton(processInstanceIdOne));
    // then
    verifyQueryResults(query, 1);
  }

  @Test
  public void testQueryByProcessInstanceIdsWithMultipleIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, processInstanceIdOne, processInstanceIdThree);
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 2);
  }

  @Test
  public void testQueryByProcessInstanceIdsWithMultipleIdsIncludingFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, processInstanceIdOne, processInstanceIdThree, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 2);
  }

  @Test
  public void testQueryByProcessInstanceIdsWithEmptyList() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = Collections.emptySet();

    // when/then
    assertThatThrownBy(() -> jobQuery.processInstanceIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is empty");
  }

  @Test
  public void testQueryByProcessInstanceIdsWithNull() {
    // given
    var jobQuery = managementService.createJobQuery();
    Set<String> ids = null;

    // when/then
    assertThatThrownBy(() -> jobQuery.processInstanceIds(ids))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is null");
  }

  @Test
  public void testQueryByProcessInstanceIdsWithFakeIds() {
    // given
    Set<String> ids = new HashSet<>();
    Collections.addAll(ids, "fakeIdOne", "fakeIdTwo");
    // when
    JobQuery query = managementService.createJobQuery().processInstanceIds(ids);
    // then
    verifyQueryResults(query, 0);
  }

  //sorting //////////////////////////////////////////

  @Test
  public void testQuerySorting() {
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
    assertThat(jobs.size()).isEqualTo(3);

    assertThat(jobs.get(0).getRetries()).isEqualTo(2);
    assertThat(jobs.get(1).getRetries()).isEqualTo(3);
    assertThat(jobs.get(2).getRetries()).isEqualTo(3);

    assertThat(jobs.get(0).getProcessInstanceId()).isEqualTo(processInstanceIdTwo);
    assertThat(jobs.get(1).getProcessInstanceId()).isEqualTo(processInstanceIdThree);
    assertThat(jobs.get(2).getProcessInstanceId()).isEqualTo(processInstanceIdOne);
  }

  @Test
  public void testQueryInvalidSortingUsage() {
    var jobQuery = managementService.createJobQuery().orderByJobId();
    try {
      jobQuery.list();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("call asc() or desc() after using orderByXX()");
    }

    var jobQuery2 = managementService.createJobQuery();
    try {
      jobQuery2.asc();
      fail();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("You should call any of the orderBy methods first before specifying a direction");
    }
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

  private ProcessInstance startProcessInstanceWithFailingJob() {
    // start a process with a failing job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event which we will execute manual for testing purposes.
    Job timerJob = managementService.createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    assertNotNull("No job found for process instance", timerJob);
    String timerJobId = timerJob.getId();

    try {
      managementService.executeJob(timerJobId);
      fail("RuntimeException from within the script task expected");
    } catch(RuntimeException re) {
      assertThat(re.getMessage()).contains(EXCEPTION_MESSAGE);
    }
    return processInstance;
  }

  private void verifyFailedJob(JobQuery query, ProcessInstance processInstance) {
    verifyQueryResults(query, 1);

    Job failedJob = query.singleResult();
    assertNotNull(failedJob);
    assertThat(failedJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
    assertNotNull(failedJob.getExceptionMessage());
    assertThat(failedJob.getExceptionMessage()).contains(EXCEPTION_MESSAGE);
  }

  private void verifyQueryResults(JobQuery query, int countExpected) {
    assertThat(query.list().size()).isEqualTo(countExpected);
    assertThat(query.count()).isEqualTo(countExpected);

    if (countExpected == 1) {
      assertNotNull(query.singleResult());
    } else if (countExpected > 1){
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertNull(query.singleResult());
    }
  }

  private void verifySingleResultFails(JobQuery query) {
    try {
      query.singleResult();
      fail();
    } catch (ProcessEngineException e) {
      // expected
    }
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

      assertNotNull(timerEntity.getId());

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

      assertNotNull(timerEntity.getId());

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
