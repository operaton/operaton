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
package org.operaton.bpm.engine.test.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.FetchExternalTasksCmd;
import org.operaton.bpm.engine.impl.externaltask.TopicFetchInstruction;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;


/**
 * @author Thorben Lindhauer
 *
 */
class CompetingExternalTaskFetchingTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;

  @BeforeEach
  void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
  }

  public class ExternalTaskFetcherThread extends ControllableThread {

    protected String workerId;
    protected int results;
    protected String topic;

    protected List<LockedExternalTask> fetchedTasks = Collections.emptyList();
    protected OptimisticLockingException exception;

    public ExternalTaskFetcherThread(String workerId, int results, String topic) {
      this.workerId = workerId;
      this.results = results;
      this.topic = topic;
    }

    @Override
    public void run() {
      Map<String, TopicFetchInstruction> instructions = new HashMap<>();

      TopicFetchInstruction instruction = new TopicFetchInstruction(topic, 10000L);
      instructions.put(topic, instruction);

      ControlledCommand<List<LockedExternalTask>> cmd = new ControlledCommand<>(
          (ControllableThread) Thread.currentThread(),
          new FetchExternalTasksCmd(workerId, results, instructions));

      try {
        fetchedTasks = processEngineConfiguration.getCommandExecutorTxRequired().execute(cmd);
      } catch (OptimisticLockingException e) {
        exception = e;
      }
    }
  }

  @Deployment
  @Test
  void testCompetingExternalTaskFetching() {
    runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");

    ExternalTaskFetcherThread thread1 = new ExternalTaskFetcherThread("thread1", 5, "externalTaskTopic");
    ExternalTaskFetcherThread thread2 = new ExternalTaskFetcherThread("thread2", 5, "externalTaskTopic");

    // both threads fetch the same task and wait before flushing the lock
    thread1.startAndWaitUntilControlIsReturned();
    thread2.startAndWaitUntilControlIsReturned();

    // thread1 succeeds
    thread1.proceedAndWaitTillDone();
    assertThat(thread1.exception).isNull();
    assertThat(thread1.fetchedTasks).hasSize(1);

    // thread2 does not succeed in locking the job
    thread2.proceedAndWaitTillDone();
    assertThat(thread2.fetchedTasks).isEmpty();
    // but does not fail with an OptimisticLockingException
    assertThat(thread2.exception).isNull();
  }
}
