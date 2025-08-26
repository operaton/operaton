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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.SetTaskVariablesCmd;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class ConcurrentVariableUpdateTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);


  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected TaskService taskService;

  protected static ControllableThread activeThread;


  @BeforeEach
  void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
  }

  class SetTaskVariablesThread extends ControllableThread {

    OptimisticLockingException optimisticLockingException;
    Exception exception;

    protected Object variableValue;
    protected String taskId;
    protected String variableName;

    public SetTaskVariablesThread(String taskId, String variableName, Object variableValue) {
      this.taskId = taskId;
      this.variableName = variableName;
      this.variableValue = variableValue;
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
          .execute(new ControlledCommand(activeThread, new SetTaskVariablesCmd(taskId, Collections.singletonMap(variableName, variableValue), false)));

      } catch (OptimisticLockingException e) {
        this.optimisticLockingException = e;
      } catch (Exception e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }
  }

  // Test is skipped when testing on DB2.
  // Please update the IF condition in #runTest, if the method name is changed.
  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  @Test
  @RequiredDatabase(excludes = DbSqlSessionFactory.DB2)
  void testConcurrentVariableCreate() {

    runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("varName1", "someValue"));

    String variableName = "varName";
    String taskId = taskService.createTaskQuery().singleResult().getId();

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread1.startAndWaitUntilControlIsReturned();

    // this should fail with integrity constraint violation
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    assertThat(thread1.exception).isNull();
    assertThat(thread1.optimisticLockingException).isNull();

    thread2.proceedAndWaitTillDone();
    assertThat(thread2.exception).isNull();
    assertThat(thread2.optimisticLockingException).isNotNull();

    // should not fail with FK violation because one of the variables is not deleted.
    taskService.complete(taskId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  @Test
  void testConcurrentVariableUpdate() {

    runtimeService.startProcessInstanceByKey("testProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();
    String variableName = "varName";

    taskService.setVariable(taskId, variableName, "someValue");

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread1.startAndWaitUntilControlIsReturned();

    // this fails with an optimistic locking exception
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someOtherString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    thread2.proceedAndWaitTillDone();

    assertThat(thread1.optimisticLockingException).isNull();
    assertThat(thread2.optimisticLockingException).isNotNull();

    // succeeds
    taskService.complete(taskId);
  }


  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  @Test
  void testConcurrentVariableUpdateTypeChange() {

    runtimeService.startProcessInstanceByKey("testProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();
    String variableName = "varName";

    taskService.setVariable(taskId, variableName, "someValue");

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, 100l);
    thread1.startAndWaitUntilControlIsReturned();

    // this fails with an optimistic locking exception
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someOtherString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    thread2.proceedAndWaitTillDone();

    assertThat(thread1.optimisticLockingException).isNull();
    assertThat(thread2.optimisticLockingException).isNotNull();

    // succeeds
    taskService.complete(taskId);
  }

}
