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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Tom Baeyens
 */
class CompetingSignalsTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected static ControllableThread activeThread;

  @BeforeEach
  void initializeServices() {
    runtimeService = engineRule.getRuntimeService();
  }

  public class SignalThread extends ControllableThread {

    String executionId;
    OptimisticLockingException exception;

    public SignalThread(String executionId) {
      this.executionId = executionId;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        runtimeService.signal(executionId);
      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug("{} ends", getName());
    }
  }

  public static class ControlledConcurrencyBehavior implements ActivityBehavior {
    @Override
    public void execute(ActivityExecution execution) throws Exception {
      activeThread.returnControlToTestThreadAndWait();
    }
  }

  @Deployment
  @Test
  void testCompetingSignals() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("CompetingSignalsProcess");
    String processInstanceId = processInstance.getId();

    LOG.debug("test thread starts thread one");
    SignalThread threadOne = new SignalThread(processInstanceId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    SignalThread threadTwo = new SignalThread(processInstanceId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();
    testRule.assertTextPresent("was updated by another transaction concurrently", threadTwo.exception.getMessage());
  }

}
