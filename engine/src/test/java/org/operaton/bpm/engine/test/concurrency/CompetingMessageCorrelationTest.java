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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.MessageCorrelationBuilderImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.CompleteTaskCmd;
import org.operaton.bpm.engine.impl.cmd.MessageEventReceivedCmd;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class CompetingMessageCorrelationTest extends ConcurrencyTestCase {

  @AfterEach
  void tearDown() {
    ((ProcessEngineConfigurationImpl)processEngine.getProcessEngineConfiguration()).getCommandExecutorTxRequiresNew().execute(commandContext -> {

      List<HistoricJobLog> jobLogs = processEngine.getHistoryService().createHistoricJobLogQuery().list();
      for (HistoricJobLog jobLog : jobLogs) {
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogById(jobLog.getId());
      }

      return null;
    });

    assertThat(processEngine.getHistoryService().createHistoricJobLogQuery().list()).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Test
  // H2: Does not support pessimistic locking behavior reliably in test environment
  // MSSQL: Uses READ_COMMITTED_SNAPSHOT isolation which relies on optimistic locking instead of pessimistic locks.
  //        This test specifically requires pessimistic locking behavior (acquiring exclusive locks) which is
  //        incompatible with snapshot isolation. Running this test with MSSQL causes infinite hangs.
  @RequiredDatabase(excludes = {DbSqlSessionFactory.H2, DbSqlSessionFactory.MSSQL})
  void testConcurrentExclusiveCorrelation() throws Exception {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", true));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", true));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // thread one correlates and acquires the exclusive lock
    thread1.makeContinue();
    thread1.waitForSync();

    // the service task was executed once
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(1);

    // thread two attempts to acquire the exclusive lock but can't since thread 1 hasn't released it yet
    thread2.makeContinue();
    Thread.sleep(2000);

    // let the first thread ends its transaction
    thread1.makeContinue();
    assertThat(thread1.getException()).isNull();

    // thread 2 can't continue because the event subscription it tried to lock was deleted
    thread2.waitForSync();
    assertThat(thread2.getException()).isNotNull();
    assertThat(thread2.getException()).isInstanceOf(ProcessEngineException.class);
    assertThat(thread2.getException().getMessage())
        .contains("does not have a subscription to a message event with name 'Message'");

    // the first thread ended successfully without an exception
    thread1.join();
    assertThat(thread1.getException()).isNull();

    // the follow-up task was reached
    Task afterMessageTask = taskService.createTaskQuery().singleResult();
    assertThat(afterMessageTask.getTaskDefinitionKey()).isEqualTo("afterMessageUserTask");

    // the service task was not executed a second time
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Test
  void testConcurrentCorrelationFailsWithOptimisticLockingException() {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", false));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", false));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // both threads correlate
    thread1.makeContinue();
    thread2.makeContinue();

    thread1.waitForSync();
    thread2.waitForSync();

    // the service task was executed twice
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // the first thread ends its transaction
    thread1.waitUntilDone();
    assertThat(thread1.getException()).isNull();

    Task afterMessageTask = taskService.createTaskQuery().singleResult();
    assertThat(afterMessageTask.getTaskDefinitionKey()).isEqualTo("afterMessageUserTask");

    // the second thread ends its transaction and fails with optimistic locking exception
    thread2.waitUntilDone();
    assertThat(thread2.getException()).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Test
  // MSSQL: Uses READ_COMMITTED_SNAPSHOT isolation which relies on optimistic locking instead of pessimistic locks.
  //        This test requires pessimistic locking behavior (acquiring exclusive locks) which is incompatible
  //        with snapshot isolation. Running this test with MSSQL causes infinite hangs.
  @RequiredDatabase(excludes = DbSqlSessionFactory.MSSQL)
  void testConcurrentExclusiveCorrelationToDifferentExecutions() {
    InvocationLogListener.reset();

    // given a process instance
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("testProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel to each of the two instances
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", instance1.getId(), true));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", instance2.getId(), true));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // thread one correlates and acquires the exclusive lock on the event subscription of instance1
    thread1.makeContinue();
    thread1.waitForSync();

    // the service task was executed once
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(1);

    // thread two correlates and acquires the exclusive lock on the event subscription of instance2
    // depending on the database and locking used, this may block thread2
    thread2.makeContinue();

    // thread 1 completes successfully
    thread1.waitUntilDone();
    assertThat(thread1.getException()).isNull();

    // thread2 should be able to continue at least after thread1 has finished and released its lock
    thread2.waitForSync();

    // the service task was executed the second time
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // thread 2 completes successfully
    thread2.waitUntilDone();
    assertThat(thread2.getException()).isNull();

    // the follow-up task was reached in both instances
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterMessageUserTask").count()).isEqualTo(2);
  }

  /**
   * Fails at least on mssql; mssql appears to lock more than the actual event subscription row
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Test
  void testConcurrentExclusiveCorrelationToDifferentExecutionsCase2() {
    InvocationLogListener.reset();

    // given a process instance
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("testProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel to each of the two instances
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", instance1.getId(), true));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", instance2.getId(), true));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // thread one correlates and acquires the exclusive lock on the event subscription of instance1
    thread1.makeContinue();
    thread1.waitForSync();

    // the service task was executed once
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(1);

    // thread two correlates and acquires the exclusive lock on the event subscription of instance2
    thread2.makeContinue();
    // FIXME: this does not return on sql server due to locking
    thread2.waitForSync();

    // the service task was executed the second time
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // thread 2 completes successfully, even though it acquired its lock after thread 1
    thread2.waitUntilDone();
    assertThat(thread2.getException()).isNull();

    // thread 1 completes successfully
    thread1.waitUntilDone();
    assertThat(thread1.getException()).isNull();

    // the follow-up task was reached in both instances
    assertThat(taskService.createTaskQuery().taskDefinitionKey("afterMessageUserTask").count()).isEqualTo(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Test
  void testConcurrentMixedCorrelation() {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel (one exclusive, one non-exclusive)
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", true));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", false));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // thread one correlates and acquires the exclusive lock
    thread1.makeContinue();
    thread1.waitForSync();

    // thread two correlates since it does not need a pessimistic lock
    thread2.makeContinue();
    thread2.waitForSync();

    // the service task was executed twice
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // the first thread ends its transaction and releases the lock; the event subscription is now gone
    thread1.waitUntilDone();
    assertThat(thread1.getException()).isNull();

    Task afterMessageTask = taskService.createTaskQuery().singleResult();
    assertThat(afterMessageTask.getTaskDefinitionKey()).isEqualTo("afterMessageUserTask");

    // thread two attempts to end its transaction and fails with optimistic locking
    thread2.makeContinue();
    thread2.waitForSync();

    assertThat(thread2.getException()).isNotNull();
    assertThat(thread2.getException()).isInstanceOf(OptimisticLockingException.class);
  }

  /**
   * <p>
   *   At least on MySQL, this test case fails with deadlock exceptions.
   *   The reason is the combination of our flush with the locking of the event
   *   subscription documented in the ticket CAM-3636.
   * </p>
   * @throws InterruptedException
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.catchMessageProcess.bpmn20.xml")
  @Ignore("CAM-3636")
  @Test
  void testConcurrentMixedCorrelationCase2() throws Exception {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel (one exclusive, one non-exclusive)
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", false));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("Message", true));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // thread one correlates and acquires no lock
    thread1.makeContinue();
    thread1.waitForSync();

    // thread two acquires a lock and succeeds because thread one hasn't acquired one
    thread2.makeContinue();
    thread2.waitForSync();

    // the service task was executed twice
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // thread one ends its transaction and blocks on flush when it attempts to delete the event subscription
    thread1.makeContinue();
    Thread.sleep(5000);
    assertThat(thread1.getException()).isNull();

    assertThat(taskService.createTaskQuery().count()).isZero();

    // thread 2 flushes successfully and releases the lock
    thread2.waitUntilDone();
    assertThat(thread2.getException()).isNull();

    Task afterMessageTask = taskService.createTaskQuery().singleResult();
    assertThat(afterMessageTask).isNotNull();
    assertThat(afterMessageTask.getTaskDefinitionKey()).isEqualTo("afterMessageUserTask");

    // thread 1 flush fails with optimistic locking
    thread1.join();
    assertThat(thread1.getException()).isNotNull();
    assertThat(thread1.getException()).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.eventSubprocess.bpmn")
  @Test
  void testEventSubprocess() {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    // and two threads correlating in parallel
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageCorrelationCommand("incoming", false));
    thread1.reportInterrupts();
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageCorrelationCommand("incoming", false));
    thread2.reportInterrupts();

    // both threads open a transaction and wait before correlating the message
    thread1.waitForSync();
    thread2.waitForSync();

    // both threads correlate
    thread1.makeContinue();
    thread2.makeContinue();

    thread1.waitForSync();
    thread2.waitForSync();

    // the first thread ends its transaction
    thread1.waitUntilDone();
    assertThat(thread1.getException()).isNull();

    // the second thread ends its transaction and fails with optimistic locking exception
    thread2.waitUntilDone();
    assertThat(thread2.getException()).isNotNull();
    assertThat(thread2.getException()).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment
  @Test
  void testConcurrentMessageCorrelationAndTreeCompaction() {
    runtimeService.startProcessInstanceByKey("process");

    // trigger non-interrupting boundary event and wait before flush
    ThreadControl correlateThread = executeControllableCommand(
        new ControllableMessageCorrelationCommand("Message", false));
    correlateThread.reportInterrupts();

    // stop correlation right before the flush
    correlateThread.waitForSync();
    correlateThread.makeContinueAndWaitForSync();

    // trigger tree compaction
    List<Task> tasks = taskService.createTaskQuery().list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    // flush correlation
    correlateThread.waitUntilDone();

    // the correlation should not have succeeded
    Throwable exception = correlateThread.getException();
    assertThat(exception).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/concurrency/CompetingMessageCorrelationTest.testConcurrentMessageCorrelationAndTreeCompaction.bpmn20.xml")
  @Test
  void testConcurrentTreeCompactionAndMessageCorrelation() {
    runtimeService.startProcessInstanceByKey("process");
    List<Task> tasks = taskService.createTaskQuery().list();

    // trigger tree compaction and wait before flush
    ThreadControl taskCompletionThread = executeControllableCommand(new ControllableCompleteTaskCommand(tasks));
    taskCompletionThread.reportInterrupts();

    // stop task completion right before flush
    taskCompletionThread.waitForSync();

    // perform message correlation to non-interrupting boundary event
    // (i.e. adds another concurrent execution to the scope execution)
    runtimeService.correlateMessage("Message");

    // flush task completion and tree compaction
    taskCompletionThread.waitUntilDone();

    // then it should not have succeeded
    Throwable exception = taskCompletionThread.getException();
    assertThat(exception).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment
  @Test
  void testConcurrentMessageCorrelationTwiceAndTreeCompaction() {
    runtimeService.startProcessInstanceByKey("process");

    // trigger non-interrupting boundary event 1 that ends in a none end event immediately
    runtimeService.correlateMessage("Message2");

    // trigger non-interrupting boundary event 2 and wait before flush
    ThreadControl correlateThread = executeControllableCommand(
        new ControllableMessageCorrelationCommand("Message1", false));
    correlateThread.reportInterrupts();

    // stop correlation right before the flush
    correlateThread.waitForSync();
    correlateThread.makeContinueAndWaitForSync();

    // trigger tree compaction
    List<Task> tasks = taskService.createTaskQuery().list();

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    // flush correlation
    correlateThread.waitUntilDone();

    // the correlation should not have succeeded
    Throwable exception = correlateThread.getException();
    assertThat(exception).isInstanceOf(OptimisticLockingException.class);
  }

  @Deployment
  @Test
  void testConcurrentEndExecutionListener() {
    InvocationLogListener.reset();

    // given a process instance
    runtimeService.startProcessInstanceByKey("testProcess");

    List<Execution> tasks = runtimeService.createExecutionQuery().messageEventSubscriptionName("Message").list();
    // two tasks waiting for the message
    assertThat(tasks).hasSize(2);

    // start first thread and wait in the second execution end listener
    ThreadControl thread1 = executeControllableCommand(new ControllableMessageEventReceivedCommand(tasks.get(0).getId(), "Message", true));
    thread1.reportInterrupts();
    thread1.waitForSync();

    // the counting execution listener was executed on task 1
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(1);

    // start second thread and complete the task
    ThreadControl thread2 = executeControllableCommand(new ControllableMessageEventReceivedCommand(tasks.get(1).getId(), "Message", false));
    thread2.waitForSync();
    thread2.waitUntilDone();

    // the counting execution listener was executed on task 1 and 2
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // continue with thread 1
    thread1.makeContinueAndWaitForSync();

    // the counting execution listener was not executed again
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);

    // try to complete thread 1
    thread1.waitUntilDone();

    // thread 1 was rolled back with an optimistic locking exception
    Throwable exception = thread1.getException();
    assertThat(exception).isInstanceOf(OptimisticLockingException.class);

    // the execution listener was not executed again
    assertThat(InvocationLogListener.getInvocations()).isEqualTo(2);
  }

  public static class InvocationLogListener implements JavaDelegate {

    protected static AtomicInteger invocations = new AtomicInteger(0);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      invocations.incrementAndGet();
    }

    public static void reset() {
      invocations.set(0);
    }

    public static int getInvocations() {
      return invocations.get();
    }
  }

  public static class WaitingListener implements ExecutionListener {

    protected static ThreadControl monitor;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      if (WaitingListener.monitor != null) {
        ThreadControl localMonitor = WaitingListener.monitor;
        WaitingListener.monitor = null;
        localMonitor.sync();
      }
    }

    public static void setMonitor(ThreadControl monitor) {
      WaitingListener.monitor = monitor;
    }
  }

  protected static class ControllableMessageCorrelationCommand extends ControllableCommand<Void> {

    protected String messageName;
    protected boolean exclusive;
    protected String processInstanceId;

    public ControllableMessageCorrelationCommand(String messageName, boolean exclusive) {
      this.messageName = messageName;
      this.exclusive = exclusive;
    }

    public ControllableMessageCorrelationCommand(String messageName, String processInstanceId, boolean exclusive) {
      this(messageName, exclusive);
      this.processInstanceId = processInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      monitor.sync();  // thread will block here until makeContinue() is called from main thread

      MessageCorrelationBuilderImpl correlationBuilder = new MessageCorrelationBuilderImpl(commandContext, messageName);
      if (processInstanceId != null) {
        correlationBuilder.processInstanceId(processInstanceId);
      }

      if (exclusive) {
        correlationBuilder.correlateExclusively();
      }
      else {
        correlationBuilder.correlate();
      }

      monitor.sync();  // thread will block here until waitUntilDone() is called form main thread

      return null;
    }

  }

  protected static class ControllableMessageEventReceivedCommand extends ControllableCommand<Void> {

    protected final String executionId;
    protected final String messageName;
    protected final boolean shouldWaitInListener;

    public ControllableMessageEventReceivedCommand(String executionId, String messageName, boolean shouldWaitInListener) {
      this.executionId = executionId;
      this.messageName = messageName;
      this.shouldWaitInListener = shouldWaitInListener;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      if (shouldWaitInListener) {
        WaitingListener.setMonitor(monitor);
      }

      MessageEventReceivedCmd receivedCmd = new MessageEventReceivedCmd(messageName, executionId, null);

      receivedCmd.execute(commandContext);

      monitor.sync();

      return null;
    }
  }

  public static class ControllableCompleteTaskCommand extends ControllableCommand<Void> {

    protected List<Task> tasks;

    public ControllableCompleteTaskCommand(List<Task> tasks) {
      this.tasks = tasks;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      for (Task task : tasks) {
        CompleteTaskCmd completeTaskCmd = new CompleteTaskCmd(task.getId(), null);
        completeTaskCmd.execute(commandContext);
      }

      monitor.sync();

      return null;
    }

  }

}
