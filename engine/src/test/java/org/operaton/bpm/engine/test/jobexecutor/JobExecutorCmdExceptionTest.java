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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.DeleteJobCmd;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

/**
 * @author Tom Baeyens
 * @author Thorben Lindhauer
 */
class JobExecutorCmdExceptionTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      // XXX disabled caching because tests got flaky. see https://github.com/operaton/operaton/issues/671
    .cacheForConfigurationResource(false)
    .build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected TweetExceptionHandler tweetExceptionHandler = new TweetExceptionHandler();
  protected TweetNestedCommandExceptionHandler nestedCommandExceptionHandler = new TweetNestedCommandExceptionHandler();
  
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;

  @BeforeEach
  void setUp() {
    processEngineConfiguration.getJobHandlers().put(tweetExceptionHandler.getType(), tweetExceptionHandler);
    processEngineConfiguration.getJobHandlers().put(nestedCommandExceptionHandler.getType(), nestedCommandExceptionHandler);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getJobHandlers().remove(tweetExceptionHandler.getType());
    processEngineConfiguration.getJobHandlers().remove(nestedCommandExceptionHandler.getType());
    managementService.createJobQuery().active().list().forEach(job -> managementService.deleteJob(job.getId()));
    clearDatabase();
  }

  @Test
  void testJobCommandsWith2Exceptions() {
    // create a job
    createJob(TweetExceptionHandler.TYPE);

    // execute the existing job
    testRule.executeAvailableJobs();

    // the job was successfully executed
    JobQuery query = managementService.createJobQuery().noRetriesLeft();
    assertThat(query.count()).isZero();
  }

  @Test
  void testJobCommandsWith3Exceptions() {
    // set the exceptionsRemaining to 3 so that
    // the created job will fail 3 times and a failed
    // job exists
    tweetExceptionHandler.setExceptionsRemaining(3);

    // create a job
    createJob(TweetExceptionHandler.TYPE);

    // execute the existing job
    testRule.executeAvailableJobs();

    // the job execution failed (job.retries = 0)
    Job job = managementService.createJobQuery().noRetriesLeft().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
  }

  @Test
  void testMultipleFailingJobs() {
    // set the exceptionsRemaining to 600 so that
    // each created job will fail 3 times and 40 failed
    // job exists
    tweetExceptionHandler.setExceptionsRemaining(600);

    // create 40 jobs
    for(int i = 0; i < 40; i++) {
      createJob(TweetExceptionHandler.TYPE);
    }

    // execute the existing jobs
    testRule.executeAvailableJobs();

    // now there are 40 jobs with retries = 0:
    List<Job> jobList = managementService.createJobQuery().list();
    assertThat(jobList).hasSize(40);

    for (Job job : jobList) {
      // all jobs have retries exhausted
      assertThat(job.getRetries()).isZero();
    }
  }

  @Test
  void testJobCommandsWithNestedFailingCommand() {
    // create a job
    createJob(TweetNestedCommandExceptionHandler.TYPE);

    // execute the existing job
    Job job = managementService.createJobQuery().singleResult();
    var jobId = job.getId();

    assertThat(job.getRetries()).isEqualTo(3);

    try {
      managementService.executeJob(jobId);
      fail("Exception expected");
    } catch (Exception e) {
      // expected
    }

    job = managementService.createJobQuery().singleResult();
    assertThat(job.getRetries()).isEqualTo(2);

    testRule.executeAvailableJobs();

    // the job execution failed (job.retries = 0)
    job = managementService.createJobQuery().noRetriesLeft().singleResult();
    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/jobFailingOnFlush.bpmn20.xml")
  @Test
  void testJobRetriesDecrementedOnFailedFlush() {

    runtimeService.startProcessInstanceByKey("testProcess");

    // there should be 1 job created:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    // with 3 retries
    assertThat(job.getRetries()).isEqualTo(3);

    // if we execute the job
    testRule.waitForJobExecutorToProcessAllJobs();

    // the job is still present
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    // but has no more retires
    assertThat(job.getRetries()).isZero();
  }

  @Test
  void testFailingTransactionListener() {

   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingTransactionListenerDelegate.class.getName())
          .operatonAsyncBefore()
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("testProcess");

    // there should be 1 job created:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    // with 3 retries
    assertThat(job.getRetries()).isEqualTo(3);

    // if we execute the job
    testRule.waitForJobExecutorToProcessAllJobs();

    // the job is still present
    job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    // but has no more retires
    assertThat(job.getRetries()).isZero();
    assertThat(job.getExceptionMessage()).isEqualTo("exception in transaction listener");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull().contains("java.lang.RuntimeException: exception in transaction listener");
  }

  protected void createJob(final String handlerType) {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      MessageEntity message = createMessage(handlerType);
      commandContext.getJobManager().send(message);
      return message.getId();
    });
  }

  protected MessageEntity createMessage(String handlerType) {
    MessageEntity message = new MessageEntity();
    message.setJobHandlerType(handlerType);
    return message;
  }

  protected void clearDatabase() {
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      List<Job> jobs = processEngineConfiguration
          .getManagementService()
          .createJobQuery()
          .list();

      for (Job job : jobs) {
        new DeleteJobCmd(job.getId()).execute(commandContext);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }

      List<HistoricIncident> historicIncidents = processEngineConfiguration
          .getHistoryService()
          .createHistoricIncidentQuery()
          .list();

      for (HistoricIncident historicIncident : historicIncidents) {
        commandContext
            .getDbEntityManager()
            .delete((DbEntity) historicIncident);
      }

      List<HistoricJobLog> historicJobLogs = processEngineConfiguration
          .getHistoryService()
          .createHistoricJobLogQuery()
          .list();

      for (HistoricJobLog historicJobLog : historicJobLogs) {
        commandContext
            .getHistoricJobLogManager()
            .deleteHistoricJobLogById(historicJobLog.getId());
      }

      return null;
    });
  }

}
