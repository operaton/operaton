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
package org.operaton.bpm.engine.test.concurrency;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.ActivityInstanceCancellationCmd;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
public class CompetingActivityInstanceCancellationTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule();
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  public ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;

  @Before
  public void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
  }

  Thread testThread = Thread.currentThread();
  static ControllableThread activeThread;
  static String jobId;

  public class CancelActivityInstance extends ControllableThread {

    String processInstanceId;
    String activityInstanceId;
    OptimisticLockingException exception;

    public CancelActivityInstance(String processInstanceId, String activityInstanceId) {
      this.processInstanceId = processInstanceId;
      this.activityInstanceId = activityInstanceId;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        processEngineConfiguration
          .getCommandExecutorTxRequired()
          .execute(new ControlledCommand(activeThread, new ActivityInstanceCancellationCmd(processInstanceId, activityInstanceId)));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/concurrency/CompetingForkTest.testCompetingFork.bpmn20.xml"})
  @Test
  public void testCompetingCancellation() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);
    ActivityInstance[] children = activityInstance.getChildActivityInstances();

    String task1ActivityInstanceId = null;
    String task2ActivityInstanceId = null;
    String task3ActivityInstanceId = null;

    for (ActivityInstance currentInstance : children) {

      String id = currentInstance.getId();
      String activityId = currentInstance.getActivityId();

      if ("task1".equals(activityId)) {
        task1ActivityInstanceId = id;
      }
      else if ("task2".equals(activityId)) {
        task2ActivityInstanceId = id;
      }
      else if ("task3".equals(activityId)) {
        task3ActivityInstanceId = id;
      }
      else {
        fail("");
      }
    }

    LOG.debug("test thread starts thread one");
    CancelActivityInstance threadOne = new CancelActivityInstance(processInstanceId, task1ActivityInstanceId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread thread two");
    CancelActivityInstance threadTwo = new CancelActivityInstance(processInstanceId, task2ActivityInstanceId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread three");
    CancelActivityInstance threadThree = new CancelActivityInstance(processInstanceId, task3ActivityInstanceId);
    threadThree.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();
    testRule.assertTextPresent("was updated by another transaction concurrently", threadTwo.exception.getMessage());

    LOG.debug("test thread notifies thread 3");
    threadThree.proceedAndWaitTillDone();
    assertThat(threadThree.exception).isNotNull();
    testRule.assertTextPresent("was updated by another transaction concurrently", threadThree.exception.getMessage());
  }

}
