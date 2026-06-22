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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.ExecuteJobHelper;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tom Baeyens
 */
class JobExecutorTestCase {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected TweetHandler tweetHandler = new TweetHandler();

  @BeforeEach
  void setUp() {
    processEngineConfiguration.getJobHandlers().put(tweetHandler.getType(), tweetHandler);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getJobHandlers().remove(tweetHandler.getType());
  }

  protected MessageEntity createTweetMessage(String msg) {
    MessageEntity message = new MessageEntity();
    message.setJobHandlerType("tweet");
    message.setJobHandlerConfigurationRaw(msg);
    return message;
  }

  protected TimerEntity createTweetTimer(String msg, Date duedate) {
    TimerEntity timer = new TimerEntity();
    timer.setJobHandlerType("tweet");
    timer.setJobHandlerConfigurationRaw(msg);
    timer.setDuedate(duedate);
    return timer;
  }

  @Nested
  class JobExecutorTest {

    @Test
    void testBasicJobExecutorOperation() {
      CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
      commandExecutor.execute(commandContext -> {
        JobManager jobManager = commandContext.getJobManager();
        jobManager.send(createTweetMessage("message-one"));
        jobManager.send(createTweetMessage("message-two"));
        jobManager.send(createTweetMessage("message-three"));
        jobManager.send(createTweetMessage("message-four"));

        jobManager.schedule(createTweetTimer("timer-one", new Date()));
        jobManager.schedule(createTweetTimer("timer-two", new Date()));
        return null;
      });

      testRule.executeAvailableJobs();

      Set<String> messages = new HashSet<>(tweetHandler.getMessages());
      Set<String> expectedMessages = new HashSet<>();
      expectedMessages.add("message-one");
      expectedMessages.add("message-two");
      expectedMessages.add("message-three");
      expectedMessages.add("message-four");
      expectedMessages.add("timer-one");
      expectedMessages.add("timer-two");

      assertThat(new TreeSet<String>(messages)).isEqualTo(new TreeSet<String>(expectedMessages));

      commandExecutor.execute(commandContext -> {
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

    @Test
    void testJobExecutorHintConfiguration() {
      ProcessEngineConfiguration engineConfig1 =
              ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();

      assertThat(engineConfig1.isHintJobExecutor()).as("default setting is true").isTrue();

      ProcessEngineConfiguration engineConfig2 =
              ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().setHintJobExecutor(false);

      assertThat(engineConfig2.isHintJobExecutor()).isFalse();

      ProcessEngineConfiguration engineConfig3 =
              ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().setHintJobExecutor(true);

      assertThat(engineConfig3.isHintJobExecutor()).isTrue();
    }

    @Test
    void testAcquiredJobs() {
      List<String> firstBatch = new ArrayList<>(List.of("a", "b", "c"));
      List<String> secondBatch = new ArrayList<>(List.of("d", "e", "f"));
      List<String> thirdBatch = new ArrayList<>(List.of("g"));

      AcquiredJobs acquiredJobs = new AcquiredJobs(0);
      acquiredJobs.addJobIdBatch(firstBatch);
      acquiredJobs.addJobIdBatch(secondBatch);
      acquiredJobs.addJobIdBatch(thirdBatch);

      assertThat(acquiredJobs.getJobIdBatches().get(0)).isEqualTo(firstBatch);
      assertThat(acquiredJobs.getJobIdBatches().get(1)).isEqualTo(secondBatch);
      assertThat(acquiredJobs.getJobIdBatches().get(2)).isEqualTo(thirdBatch);

      acquiredJobs.removeJobId("a");
      assertThat(acquiredJobs.getJobIdBatches().get(0)).isEqualTo(List.of("b", "c"));
      assertThat(acquiredJobs.getJobIdBatches().get(1)).isEqualTo(secondBatch);
      assertThat(acquiredJobs.getJobIdBatches().get(2)).isEqualTo(thirdBatch);

      assertThat(acquiredJobs.getJobIdBatches()).hasSize(3);
      acquiredJobs.removeJobId("g");
      assertThat(acquiredJobs.getJobIdBatches()).hasSize(2);
    }
  }

  @Nested
  class JobExecutorCmdHappyTest {
    @Test
    void testJobCommandsWithMessage() {
      CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
      JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
      String jobId = commandExecutor.execute(commandContext -> {
        MessageEntity message = createTweetMessage("i'm coding a test");
        commandContext.getJobManager().send(message);
        return message.getId();
      });

      AcquiredJobs acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(jobExecutor));
      List<List<String>> jobIdsList = acquiredJobs.getJobIdBatches();
      assertThat(jobIdsList).hasSize(1);

      List<String> jobIds = jobIdsList.get(0);

      List<String> expectedJobIds = new ArrayList<>();
      expectedJobIds.add(jobId);

      assertThat(new ArrayList<String>(jobIds)).isEqualTo(expectedJobIds);
      assertThat(tweetHandler.getMessages()).isEmpty();

      ExecuteJobHelper.executeJob(jobId, commandExecutor);

      assertThat(tweetHandler.getMessages().get(0)).isEqualTo("i'm coding a test");
      assertThat(tweetHandler.getMessages()).hasSize(1);

      clearDatabase();
    }

    static final long SOME_TIME = 928374923546L;
    static final long SECOND = 1000;

    @Test
    void testJobCommandsWithTimer() {
      // clock gets automatically reset in LogTestCase.runTest
      ClockUtil.setCurrentTime(new Date(SOME_TIME));

      CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
      JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();

      String jobId = commandExecutor.execute(commandContext -> {
        TimerEntity timer = createTweetTimer("i'm coding a test", new Date(SOME_TIME + (10 * SECOND)));
        commandContext.getJobManager().schedule(timer);
        return timer.getId();
      });

      AcquiredJobs acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(jobExecutor));
      List<List<String>> jobIdsList = acquiredJobs.getJobIdBatches();
      assertThat(jobIdsList).isEmpty();

      List<String> expectedJobIds = new ArrayList<>();

      ClockUtil.setCurrentTime(new Date(SOME_TIME + (20 * SECOND)));

      acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(jobExecutor, jobExecutor.getMaxJobsPerAcquisition()));
      jobIdsList = acquiredJobs.getJobIdBatches();
      assertThat(jobIdsList).hasSize(1);

      List<String> jobIds = jobIdsList.get(0);

      expectedJobIds.add(jobId);
      assertThat(new ArrayList<String>(jobIds)).isEqualTo(expectedJobIds);

      assertThat(tweetHandler.getMessages()).isEmpty();

      ExecuteJobHelper.executeJob(jobId, commandExecutor);

      assertThat(tweetHandler.getMessages().get(0)).isEqualTo("i'm coding a test");
      assertThat(tweetHandler.getMessages()).hasSize(1);

      clearDatabase();
    }

    protected void clearDatabase() {
      processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

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
}