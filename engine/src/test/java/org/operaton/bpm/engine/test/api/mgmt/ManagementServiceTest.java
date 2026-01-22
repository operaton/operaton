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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentManager;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.TableMetaData;
import org.operaton.bpm.engine.management.TablePage;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobExpectingException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Saeid Mizaei
 * @author Joram Barrez
 */
class ManagementServiceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected TaskService taskService;

  protected boolean tearDownEnsureJobDueDateNotNull;

  private static final Date TEST_DUE_DATE = new Date(1675752840000L);
  private static final int SECOND = 1000;

  @AfterEach
  void tearDown() {
    if(tearDownEnsureJobDueDateNotNull) {
      processEngineConfiguration.setEnsureJobDueDateNotNull(false);
    }
  }

  @Test
  void testGetMetaDataForUnexistingTable() {
    TableMetaData metaData = managementService.getTableMetaData("unexistingtable");
    assertThat(metaData).isNull();
  }

  @Test
  void testGetMetaDataNullTableName() {
    // when/then
    assertThatThrownBy(() -> managementService.getTableMetaData(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("tableName is null");
  }

  @Test
  void testExecuteJobNullJobId() {
    assertThatCode(() -> executeJobExpectingException(managementService, null, "jobId is null")).doesNotThrowAnyException();
  }

  @Test
  void testExecuteJobUnexistingJob() {
    assertThatCode(() -> executeJobExpectingException(managementService, "unexistingjob", "No job found with id")).doesNotThrowAnyException();
  }


  @Deployment
  @Test
  void testGetJobExceptionStacktrace() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundry
    // timer event which we will execute manual for testing purposes.
    Job timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    var timerJobId = timerJob.getId();

    executeJobExpectingException(managementService, timerJobId, "This is an exception thrown from scriptTask");

    // Fetch the task to see that the exception that occurred is persisted
    timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).isNotNull();
    assertThat(timerJob.getExceptionMessage()).isNotNull();
    testRule.assertTextPresent("This is an exception thrown from scriptTask", timerJob.getExceptionMessage());

    // Get the full stacktrace using the managementService
    String exceptionStack = managementService.getJobExceptionStacktrace(timerJob.getId());
    assertThat(exceptionStack).isNotNull();
    testRule.assertTextPresent("This is an exception thrown from scriptTask", exceptionStack);
  }

  @Test
  void testgetJobExceptionStacktraceUnexistingJobId() {
    // given
    String jobId = "unexistingjob";

    // when/then
    assertThatThrownBy(() -> managementService.getJobExceptionStacktrace(jobId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No job found with id unexistingjob");
  }

  @Test
  void testgetJobExceptionStacktraceNullJobId() {
    // when/then
    assertThatThrownBy(() -> managementService.getJobExceptionStacktrace(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("jobId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobRetries() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event.
    Job timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    assertThat(timerJob.getRetries()).isEqualTo(JobEntity.DEFAULT_RETRIES);

    managementService.setJobRetries(timerJob.getId(), 5);

    timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();
    assertThat(timerJob.getRetries()).isEqualTo(5);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetMultipleJobRetries() {
    //given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");
    List<String> allJobIds = getAllJobIds();

    //when
    managementService.setJobRetries(allJobIds, 5);

    //then
    assertRetries(allJobIds, 5);
  }

  @Test
  void shouldThrowExceptionOnSetJobRetriesWithNull() {
    assertThatThrownBy(() -> managementService.setJobRetries((List<String>) null, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("job ids is null");
  }

  @Test
  void shouldThrowExceptionOnSetJobRetriesWithNoJobReference() {
    // given
    var setJobRetriesBuilder = managementService.setJobRetries(5);

    // when/then
    assertThatThrownBy(setJobRetriesBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("052")
      .hasMessageContaining("You must specify exactly one of jobId, jobIds or jobDefinitionId as parameter.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetJobRetriesWithDuedateByJobIds() {
    // given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<String> jobIds = getAllJobIds();

    // when
    managementService.setJobRetries(5).jobIds(jobIds).dueDate(TEST_DUE_DATE).execute();

    // then
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      assertThat(job.getRetries()).isEqualTo(5);
      assertThat(job.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetJobRetriesWithDuedateByJobId() {
    // given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<String> jobIds = getAllJobIds();
    String jobId = jobIds.get(0);

    // when
    managementService.setJobRetries(5).jobId(jobId).dueDate(TEST_DUE_DATE).execute();

    // then
    Job job = managementService.createJobQuery().jobId(jobId).singleResult();
    assertThat(job.getRetries()).isEqualTo(5);
    assertThat(job.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetJobRetriesWithNullDuedateByJobId() {
    // given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<Job> jobs = managementService.createJobQuery().list();
    Job job = jobs.get(0);
    String jobId = job.getId();

    // when
    managementService.setJobRetries(5).jobId(jobId).dueDate(null).execute();

    // then
    Job updatedJob = managementService.createJobQuery().jobId(jobId).singleResult();
    assertThat(updatedJob.getRetries()).isEqualTo(5);
    assertThat(updatedJob.getDuedate()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetJobRetriesWithDuedateByJobDefinitionId() {
    // given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<Job> list = managementService.createJobQuery().list();
    Job job = list.get(0);
    managementService.setJobRetries(job.getId(), 0);

    // when
    managementService.setJobRetries(5).jobDefinitionId(job.getJobDefinitionId()).dueDate(TEST_DUE_DATE).execute();

    // then
    job = managementService.createJobQuery().jobDefinitionId(job.getJobDefinitionId()).singleResult();
    assertThat(job.getRetries()).isEqualTo(5);
    assertThat(job.getDuedate()).isCloseTo(TEST_DUE_DATE, 1000);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetJobRetriesWithNullDuedateByJobDefinitionId() {
    // given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<Job> list = managementService.createJobQuery().list();
    Job job = list.get(0);
    managementService.setJobRetries(job.getId(), 0);

    // when
    managementService.setJobRetries(5).jobDefinitionId(job.getJobDefinitionId()).dueDate(null).execute();

    // then
    Job updatedJob = managementService.createJobQuery().jobDefinitionId(job.getJobDefinitionId()).singleResult();
    assertThat(updatedJob.getRetries()).isEqualTo(5);
    assertThat(updatedJob.getDuedate()).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetDueDateOnSetJobRetriesWithNullDuedateWhenEnsureDueDateNotNull() {
    // given
    tearDownEnsureJobDueDateNotNull = true;
    processEngineConfiguration.setEnsureJobDueDateNotNull(true);
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<Job> list = managementService.createJobQuery().list();
    Job job = list.get(0);
    managementService.setJobRetries(job.getId(), 0);

    // when
    managementService.setJobRetries(5).jobDefinitionId(job.getJobDefinitionId()).dueDate(null).execute();

    // then
    job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getDuedate()).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void shouldSetDueDateNullOnSetJobRetriesWithNullDuedateWhenNotEnsureDueDateNotNull() {
    // given
    tearDownEnsureJobDueDateNotNull = true;
    processEngineConfiguration.setEnsureJobDueDateNotNull(false);
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<Job> list = managementService.createJobQuery().list();
    Job job = list.get(0);
    managementService.setJobRetries(job.getId(), 0);

    // when
    managementService.setJobRetries(5).jobDefinitionId(job.getJobDefinitionId()).dueDate(null).execute();

    // then
    job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getDuedate()).isNull();
  }

  @Test
  void shouldThrowExceptionOnSetJobRetriesWithNegativeRetries() {
    assertThatThrownBy(() -> managementService.setJobRetries("aFake", -1))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("54")
      .hasMessageContaining("The number of job retries must be a non-negative Integer, but '-1' has been provided.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobRetriesWithFake() {
    //given
    runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    List<String> allJobIds = getAllJobIds();
    allJobIds.add("aFake");
    assertThatThrownBy(() -> managementService.setJobRetries(allJobIds, 5)).isInstanceOf(ProcessEngineException.class);

    assertRetries(getAllJobIds(), JobEntity.DEFAULT_RETRIES);
  }

  protected void assertRetries(List<String> allJobIds, int i) {
    for (String id : allJobIds) {
      assertThat(managementService.createJobQuery().jobId(id).singleResult().getRetries()).isEqualTo(i);
    }
  }

  protected List<String> getAllJobIds() {
    ArrayList<String> result = new ArrayList<>();
    for (Job job : managementService.createJobQuery().list()) {
      result.add(job.getId());
    }
    return result;
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobRetriesNullCreatesIncident() {

    // initially there is no incident
    assertThat(runtimeService.createIncidentQuery().count()).isZero();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event.
    Job timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    assertThat(timerJob.getRetries()).isEqualTo(JobEntity.DEFAULT_RETRIES);

    managementService.setJobRetries(timerJob.getId(), 0);

    timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();
    assertThat(timerJob.getRetries()).isZero();

    assertThat(runtimeService.createIncidentQuery().count()).isOne();

  }

  @Test
  void shouldThrowExceptionOnSetJobRetriesWithUnexistingJobId() {
    assertThatThrownBy(() -> managementService.setJobRetries("unexistingjob", 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("053")
      .hasMessageContaining("No job found with id 'unexistingjob'.");
  }

  @Test
  void shouldThrowExceptionOnSetJobRetriesWithEmptyJobId() {
    assertThatThrownBy(() -> managementService.setJobRetries("", 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("052")
      .hasMessageContaining("You must specify exactly one of jobId, jobIds or jobDefinitionId as parameter.");
  }

  @Test
  void testSetJobRetriesJobIdNull() {
    assertThatThrownBy(() -> managementService.setJobRetries((String) null, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("052")
      .hasMessageContaining("You must specify exactly one of jobId, jobIds or jobDefinitionId as parameter.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobRetriesByJobDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");
    testRule.executeAvailableJobs();

    JobQuery query = managementService.createJobQuery()
        .processInstanceId(processInstance.getId());

    JobDefinition jobDefinition = managementService
        .createJobDefinitionQuery()
        .singleResult();

    Job timerJob = query.singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    assertThat(timerJob.getRetries()).isZero();

    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 5);

    timerJob = query.singleResult();
    assertThat(timerJob.getRetries()).isEqualTo(5);
  }

  @Test
  void testSetJobRetriesByJobDefinitionIdEmptyJobDefinitionId() {
      assertThatThrownBy(() -> managementService.setJobRetriesByJobDefinitionId("", 5))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("052")
        .hasMessageContaining("You must specify exactly one of jobId, jobIds or jobDefinitionId as parameter.");
  }

  @Test
  void testSetJobRetriesByJobDefinitionIdNull() {
    assertThatThrownBy(() -> managementService.setJobRetriesByJobDefinitionId(null, 5))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("052")
      .hasMessageContaining("You must specify exactly one of jobId, jobIds or jobDefinitionId as parameter.");
  }

  @Test
  void testSetJobRetriesUnlocksInconsistentJob() {
    // case 1
    // given an inconsistent job that is never again picked up by a job executor
    createJob(0, "owner", ClockUtil.getCurrentTime());

    // when the job retries are reset
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 3);

    // then the job can be picked up again
    job = (JobEntity) managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getLockOwner()).isNull();
    assertThat(job.getLockExpirationTime()).isNull();
    assertThat(job.getRetries()).isEqualTo(3);

    deleteJobAndIncidents(job);

    // case 2
    // given an inconsistent job that is never again picked up by a job executor
    createJob(2, "owner", null);

    // when the job retries are reset
    job = (JobEntity) managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 3);

    // then the job can be picked up again
    job = (JobEntity) managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getLockOwner()).isNull();
    assertThat(job.getLockExpirationTime()).isNull();
    assertThat(job.getRetries()).isEqualTo(3);

    deleteJobAndIncidents(job);

    // case 3
    // given a consistent job
    createJob(2, "owner", ClockUtil.getCurrentTime());

    // when the job retries are reset
    job = (JobEntity) managementService.createJobQuery().singleResult();
    managementService.setJobRetries(job.getId(), 3);

    // then the lock owner and expiration should not change
    job = (JobEntity) managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getLockOwner()).isNotNull();
    assertThat(job.getLockExpirationTime()).isNotNull();
    assertThat(job.getRetries()).isEqualTo(3);

    deleteJobAndIncidents(job);
  }

  protected void createJob(final int retries, final String owner, final Date lockExpirationTime) {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      JobManager jobManager = commandContext.getJobManager();
      MessageEntity job = new MessageEntity();
      job.setJobHandlerType("any");
      job.setLockOwner(owner);
      job.setLockExpirationTime(lockExpirationTime);
      job.setRetries(retries);

      jobManager.send(job);
      return null;
    });
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobRetriesByDefinitionUnlocksInconsistentJobs() {
    // given a job definition
    final JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // and an inconsistent job that is never again picked up by a job executor
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(new Command<Void>() {
      @Override
      public Void execute(CommandContext commandContext) {
        JobManager jobManager = commandContext.getJobManager();
        MessageEntity job = new MessageEntity();
        job.setJobDefinitionId(jobDefinition.getId());
        job.setJobHandlerType("any");
        job.setLockOwner("owner");
        job.setLockExpirationTime(ClockUtil.getCurrentTime());
        job.setRetries(0);

        jobManager.send(job);
        return null;
      }
    });

    // when the job retries are reset
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 3);

    // then the job can be picked up again
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getLockOwner()).isNull();
    assertThat(job.getLockExpirationTime()).isNull();
    assertThat(job.getRetries()).isEqualTo(3);

    deleteJobAndIncidents(job);
  }

  protected void deleteJobAndIncidents(final Job job) {
    final List<HistoricIncident> incidents =
        historyService.createHistoricIncidentQuery()
            .incidentType(Incident.FAILED_JOB_HANDLER_TYPE).list();

    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      ((JobEntity) job).delete();

      HistoricIncidentManager historicIncidentManager = commandContext.getHistoricIncidentManager();
      for (HistoricIncident incident : incidents) {
        HistoricIncidentEntity incidentEntity = (HistoricIncidentEntity) incident;
        historicIncidentManager.delete(incidentEntity);
      }

      commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      return null;
    });
  }

  @Test
  void testDeleteJobNullJobId() {
    // when/then
    assertThatThrownBy(() -> managementService.deleteJob(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("jobId is null");
  }

  @Test
  void testDeleteJobUnexistingJob() {
    // given
    String jobId = "unexistingjob";

    // when/then
    assertThatThrownBy(() -> managementService.deleteJob(jobId))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("No job found with id");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/timerOnTask.bpmn20.xml"})
  @Test
  void testDeleteJobDeletion() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnTask");
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(timerJob).as("Task timer should be there").isNotNull();
    managementService.deleteJob(timerJob.getId());

    timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(timerJob).as("There should be no job now. It was deleted").isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/timerOnTask.bpmn20.xml"})
  @Test
  void testDeleteJobThatWasAlreadyAcquired() {
    ClockUtil.setCurrentTime(new Date());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnTask");
    Job timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
    var timerJobId = timerJob.getId();

    // We need to move time at least one hour to make the timer executable
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 7200000L));

    // Acquire job by running the acquire command manually
    ProcessEngineImpl processEngineImpl = (ProcessEngineImpl) processEngine;
    JobExecutor jobExecutor = processEngineImpl.getProcessEngineConfiguration().getJobExecutor();
    AcquireJobsCmd acquireJobsCmd = new AcquireJobsCmd(jobExecutor);
    CommandExecutor commandExecutor = processEngineImpl.getProcessEngineConfiguration().getCommandExecutorTxRequired();
    commandExecutor.execute(acquireJobsCmd);

    // Try to delete the job. This should fail.
    assertThatThrownBy(() -> managementService.deleteJob(timerJobId)).isInstanceOf(ProcessEngineException.class);

    // Clean up
    managementService.executeJob(timerJob.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobDuedate() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event.
    Job timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    assertThat(timerJob.getDuedate()).isNotNull();

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, 3); // add 3 days on the actual date
    managementService.setJobDuedate(timerJob.getId(), cal.getTime());

    Job newTimerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // normalize date for mysql dropping fractional seconds in time values
    assertThat((newTimerJob.getDuedate().getTime() / SECOND) * SECOND).isEqualTo((cal.getTime().getTime() / SECOND) * SECOND);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml"})
  @Test
  void testSetJobDuedateDateNull() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

    // The execution is waiting in the first usertask. This contains a boundary
    // timer event.
    Job timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob).as("No job found for process instance").isNotNull();
    assertThat(timerJob.getDuedate()).isNotNull();

    managementService.setJobDuedate(timerJob.getId(), null);

    timerJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(timerJob.getDuedate()).isNull();
  }


  @Test
  void testSetJobDuedateJobIdNull() {
    // given
    Date duedate = new Date();

    // when/then
    assertThatThrownBy(() -> managementService.setJobDuedate(null, duedate))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The job id is mandatory, but 'null' has been provided.");
  }

  @Test
  void testSetJobDuedateEmptyJobId() {
    // given
    Date duedate = new Date();
    String jobId = "";

    // when/then
    assertThatThrownBy(() -> managementService.setJobDuedate(jobId, duedate))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The job id is mandatory, but '' has been provided.");
  }

  @Test
  void testSetJobDuedateUnexistingJobId() {
    // given
    Date duedate = new Date();
    String jobId = "unexistingjob";

    // when/then
    assertThatThrownBy(() -> managementService.setJobDuedate(jobId, duedate))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No job found with id 'unexistingjob'.");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/job/oneTaskProcess.bpmn20.xml")
  @Test
  void testSetJobDuedateNonTimerJob(){
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Job job = managementService.createJobQuery().processDefinitionKey("oneTaskProcess").singleResult();
    assertThat(job).isNotNull();
    managementService.setJobDuedate(job.getId(), new Date());
    job = managementService.createJobQuery().processDefinitionKey("oneTaskProcess").singleResult();
    assertThat(job.getDuedate()).isNotNull();
  }

  @Test
  void testGetProperties() {
    Map<String, String> properties = managementService.getProperties();
    assertThat(properties)
            .isNotNull()
            .isNotEmpty();
  }

  @Test
  void testSetProperty() {
    final String name = "testProp";
    final String value = "testValue";
    managementService.setProperty(name, value);

    Map<String, String> properties = managementService.getProperties();
    assertThat(properties).containsKey(name);
    String storedValue = properties.get(name);
    assertThat(storedValue).isEqualTo(value);

    managementService.deleteProperty(name);
  }

  @Test
  void testDeleteProperty() {
    final String name = "testProp";
    final String value = "testValue";
    managementService.setProperty(name, value);

    Map<String, String> properties = managementService.getProperties();
    assertThat(properties).containsKey(name);
    String storedValue = properties.get(name);
    assertThat(storedValue).isEqualTo(value);

    managementService.deleteProperty(name);
    properties = managementService.getProperties();
    assertThat(properties.containsKey(name)).isFalse();

  }

  @Test
  void testDeleteNonexistingProperty() {
    assertThatCode(() -> managementService.deleteProperty("non existing"))
      .doesNotThrowAnyException();
  }

  @Test
  void testGetHistoryLevel() {
    int historyLevel = managementService.getHistoryLevel();
    assertThat(historyLevel).isEqualTo(processEngineConfiguration.getHistoryLevel().getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testSetJobPriority() {
    // given
    runtimeService
        .createProcessInstanceByKey("asyncTaskProcess")
        .startBeforeActivity("task")
        .execute();

    Job job = managementService.createJobQuery().singleResult();

    // when
    managementService.setJobPriority(job.getId(), 42);

    // then
    job = managementService.createJobQuery().singleResult();

    assertThat(job.getPriority()).isEqualTo(42);
  }

  @Test
  void testSetJobPriorityForNonExistingJob() {
    // given
    String jobId = "nonExistingJob";
    int priority = 42;

    // when/then
    assertThatThrownBy(() -> managementService.setJobPriority(jobId, priority))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No job found with id 'nonExistingJob'");
  }

  @Test
  void testSetJobPriorityForNullJob() {
    // given
    int priority = 42;

    // when/then
    assertThatThrownBy(() -> managementService.setJobPriority(null, priority))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("job id must not be null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testSetJobPriorityToExtremeValues() {
    runtimeService
        .createProcessInstanceByKey("asyncTaskProcess")
        .startBeforeActivity("task")
        .execute();

    Job job = managementService.createJobQuery().singleResult();

    // it is possible to set the max integer value
    managementService.setJobPriority(job.getId(), Long.MAX_VALUE);
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getPriority()).isEqualTo(Long.MAX_VALUE);

    // it is possible to set the min integer value
    managementService.setJobPriority(job.getId(), Long.MIN_VALUE + 1); // +1 for informix
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getPriority()).isEqualTo(Long.MIN_VALUE + 1);
  }

  @Test
  void testGetTableMetaData() {

    TableMetaData tableMetaData = managementService.getTableMetaData("ACT_RU_TASK");
    assertThat(tableMetaData.getColumnNames()).hasSize(tableMetaData.getColumnTypes().size());
    assertThat(tableMetaData.getColumnNames()).contains("ID_", "REV_","NAME_", "PARENT_TASK_ID_",
        "PRIORITY_", "CREATE_TIME_", "LAST_UPDATED_", "OWNER_", "ASSIGNEE_", "DELEGATION_", "EXECUTION_ID_",
        "PROC_DEF_ID_", "PROC_INST_ID_", "CASE_EXECUTION_ID_","CASE_INST_ID_", "CASE_DEF_ID_", "TASK_DEF_KEY_",
        "DESCRIPTION_", "DUE_DATE_", "FOLLOW_UP_DATE_", "SUSPENSION_STATE_", "TENANT_ID_");

    int assigneeIndex = tableMetaData.getColumnNames().indexOf("ASSIGNEE_");
    int createTimeIndex = tableMetaData.getColumnNames().indexOf("CREATE_TIME_");

    assertThat(assigneeIndex).isPositive();
    assertThat(createTimeIndex).isPositive();

    assertThat(tableMetaData.getColumnTypes().get(assigneeIndex)).isIn("CHARACTER VARYING", "VARCHAR", "NVARCHAR2", "nvarchar", "NVARCHAR");
    assertThat(tableMetaData.getColumnTypes().get(createTimeIndex)).isIn("TIMESTAMP", "TIMESTAMP(6)", "datetime", "DATETIME", "DATETIME2");
  }

  @Test
  void testGetTablePage() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    List<String> taskIds = generateDummyTasks(20);

    TablePage tablePage = managementService.createTablePageQuery()
        .tableName(tablePrefix + "ACT_RU_TASK")
        .listPage(0, 5);

    assertThat(tablePage.getFirstResult()).isZero();
    assertThat(tablePage.getSize()).isEqualTo(5);
    assertThat(tablePage.getRows()).hasSize(5);
    assertThat(tablePage.getTotal()).isEqualTo(20);

    tablePage = managementService.createTablePageQuery()
        .tableName(tablePrefix + "ACT_RU_TASK")
        .listPage(14, 10);

    assertThat(tablePage.getFirstResult()).isEqualTo(14);
    assertThat(tablePage.getSize()).isEqualTo(6);
    assertThat(tablePage.getRows()).hasSize(6);
    assertThat(tablePage.getTotal()).isEqualTo(20);

    taskService.deleteTasks(taskIds, true);
  }

  @Test
  void testGetSortedTablePage() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    List<String> taskIds = generateDummyTasks(15);

    // With an ascending sort
    TablePage tablePage = managementService.createTablePageQuery()
        .tableName(tablePrefix + "ACT_RU_TASK")
        .orderAsc("NAME_")
        .listPage(1, 7);
    String[] expectedTaskNames = new String[]{"B", "C", "D", "E", "F", "G", "H"};
    verifyTaskNames(expectedTaskNames, tablePage.getRows());

    // With a descending sort
    tablePage = managementService.createTablePageQuery()
        .tableName(tablePrefix + "ACT_RU_TASK")
        .orderDesc("NAME_")
        .listPage(6, 8);
    expectedTaskNames = new String[]{"I", "H", "G", "F", "E", "D", "C", "B"};
    verifyTaskNames(expectedTaskNames, tablePage.getRows());

    taskService.deleteTasks(taskIds, true);
  }

  @Test
  void shouldAlwaysReturnFalseWhenFetchingIsTelemetryEnabled() {
    // given default configuration

    // then
    assertThat(managementService.isTelemetryEnabled()).isFalse();
  }

  @Test
  void shouldReturnFalseWhenToggleTelemetry() {
    // given default configuration

    // when
    managementService.toggleTelemetry(true);

    // then
    assertThat(managementService.isTelemetryEnabled()).isFalse();
  }

  private void verifyTaskNames(String[] expectedTaskNames, List<Map<String, Object>> rowData) {
    assertThat(rowData).hasSize(expectedTaskNames.length);
    String columnKey = "NAME_";

    for (int i = 0; i < expectedTaskNames.length; i++) {
      Object o = rowData.get(i).get(columnKey);
      if (o == null) {
        o = rowData.get(i).get(columnKey.toLowerCase());
      }
      assertThat(o).isEqualTo(expectedTaskNames[i]);
    }
  }

  private List<String> generateDummyTasks(int nrOfTasks) {
    ArrayList<String> taskIds = new ArrayList<>();
    for (int i = 0; i < nrOfTasks; i++) {
      Task task = taskService.newTask();
      task.setName(((char) ('A' + i)) + "");
      taskService.saveTask(task);
      taskIds.add(task.getId());
    }
    return taskIds;
  }

}
