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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.persistence.entity.JobManager;

/**
 * @author Tom Baeyens
 */
class JobExecutorTest extends JobExecutorTestCase {

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
    List<String> firstBatch = new ArrayList<>(Arrays.asList("a", "b", "c"));
    List<String> secondBatch = new ArrayList<>(Arrays.asList("d", "e", "f"));
    List<String> thirdBatch = new ArrayList<>(Arrays.asList("g"));

    AcquiredJobs acquiredJobs = new AcquiredJobs(0);
    acquiredJobs.addJobIdBatch(firstBatch);
    acquiredJobs.addJobIdBatch(secondBatch);
    acquiredJobs.addJobIdBatch(thirdBatch);

    assertThat(acquiredJobs.getJobIdBatches().get(0)).isEqualTo(firstBatch);
    assertThat(acquiredJobs.getJobIdBatches().get(1)).isEqualTo(secondBatch);
    assertThat(acquiredJobs.getJobIdBatches().get(2)).isEqualTo(thirdBatch);

    acquiredJobs.removeJobId("a");
    assertThat(acquiredJobs.getJobIdBatches().get(0)).isEqualTo(Arrays.asList("b", "c"));
    assertThat(acquiredJobs.getJobIdBatches().get(1)).isEqualTo(secondBatch);
    assertThat(acquiredJobs.getJobIdBatches().get(2)).isEqualTo(thirdBatch);

    assertThat(acquiredJobs.getJobIdBatches()).hasSize(3);
    acquiredJobs.removeJobId("g");
    assertThat(acquiredJobs.getJobIdBatches()).hasSize(2);
  }
}
