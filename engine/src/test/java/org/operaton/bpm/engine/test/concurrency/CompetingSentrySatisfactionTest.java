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

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.cmd.CompleteCaseExecutionCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.ManualStartCaseExecutionCmd;
import org.operaton.bpm.engine.impl.cmmn.cmd.StateTransitionCaseExecutionCmd;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class CompetingSentrySatisfactionTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);


  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected CaseService caseService;

  protected static ControllableThread activeThread;

  @BeforeEach
  void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    caseService = engineRule.getCaseService();
  }

  public abstract class SingleThread extends ControllableThread {

    String caseExecutionId;
    OptimisticLockingException exception;
    protected StateTransitionCaseExecutionCmd cmd;

    protected SingleThread(String caseExecutionId, StateTransitionCaseExecutionCmd cmd) {
      this.caseExecutionId = caseExecutionId;
      this.cmd = cmd;
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
          .execute(new ControlledCommand(activeThread, cmd));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug("{} ends", getName());
    }
  }

  public class CompletionSingleThread extends SingleThread {

    public CompletionSingleThread(String caseExecutionId) {
      super(caseExecutionId, new CompleteCaseExecutionCmd(caseExecutionId, null, null, null, null));
    }

  }

  public class ManualStartSingleThread extends SingleThread {

    public ManualStartSingleThread(String caseExecutionId) {
      super(caseExecutionId, new ManualStartCaseExecutionCmd(caseExecutionId, null, null, null, null));
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/concurrency/CompetingSentrySatisfactionTest.testEntryCriteriaWithAndSentry.cmmn"})
  @Test
  void testEntryCriteriaWithAndSentry() {
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String firstHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String secondHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    LOG.debug("test thread starts thread one");
    SingleThread threadOne = new ManualStartSingleThread(firstHumanTaskId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    SingleThread threadTwo = new CompletionSingleThread(secondHumanTaskId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    assertThat(threadTwo.exception.getMessage())
        .contains("CaseSentryPartEntity")
        .contains("was updated by another transaction concurrently");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/concurrency/CompetingSentrySatisfactionTest.testExitCriteriaWithAndSentry.cmmn"})
  @Test
  void testExitCriteriaWithAndSentry() {
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String firstHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String secondHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    LOG.debug("test thread starts thread one");
    SingleThread threadOne = new ManualStartSingleThread(firstHumanTaskId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    SingleThread threadTwo = new CompletionSingleThread(secondHumanTaskId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    assertThat(threadTwo.exception.getMessage())
        .contains("CaseSentryPartEntity")
        .contains("was updated by another transaction concurrently");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/concurrency/CompetingSentrySatisfactionTest.testEntryCriteriaWithOrSentry.cmmn"})
  @Test
  void testEntryCriteriaWithOrSentry() {
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String firstHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String secondHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    LOG.debug("test thread starts thread one");
    SingleThread threadOne = new ManualStartSingleThread(firstHumanTaskId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    SingleThread threadTwo = new CompletionSingleThread(secondHumanTaskId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    assertThat(threadTwo.exception.getMessage())
        .contains("CaseExecutionEntity")
        .contains("was updated by another transaction concurrently");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/concurrency/CompetingSentrySatisfactionTest.testExitCriteriaWithOrSentry.cmmn",
    "org/operaton/bpm/engine/test/concurrency/CompetingSentrySatisfactionTest.oneTaskProcess.bpmn20.xml"})
  @Test
  void testExitCriteriaWithOrSentry() {
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    String firstHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String secondHumanTaskId = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    CaseExecution thirdTask = caseService
      .createCaseExecutionQuery()
      .caseInstanceId(caseInstanceId)
      .activityId("ProcessTask_3")
      .singleResult();
    caseService.manuallyStartCaseExecution(thirdTask.getId());

    LOG.debug("test thread starts thread one");
    SingleThread threadOne = new ManualStartSingleThread(firstHumanTaskId);
    threadOne.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread continues to start thread two");
    SingleThread threadTwo = new CompletionSingleThread(secondHumanTaskId);
    threadTwo.startAndWaitUntilControlIsReturned();

    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    assertThat(threadTwo.exception.getMessage())
        .contains("CaseExecutionEntity")
        .contains("was updated by another transaction concurrently");
  }

}
