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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.operaton.bpm.engine.impl.cmd.SetJobDefinitionPriorityCmd;
import org.operaton.bpm.engine.impl.cmd.SuspendJobCmd;
import org.operaton.bpm.engine.impl.cmd.SuspendJobDefinitionCmd;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.ExecuteJobHelper;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobFailureCollector;
import org.operaton.bpm.engine.impl.management.UpdateJobDefinitionSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.management.UpdateJobSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;

/**
 * @author Thorben Lindhauer
 *
 */
public class ConcurrentJobExecutorTest {

  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected static ControllableThread activeThread;

  protected static final BpmnModelInstance SIMPLE_ASYNC_PROCESS = Bpmn.createExecutableProcess("simpleAsyncProcess")
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
        .operatonAsyncBefore()
      .endEvent()
      .done();

  @Before
  public void initServices() {
    runtimeService = engineRule.getRuntimeService();
    repositoryService = engineRule.getRepositoryService();
    managementService = engineRule.getManagementService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void tearDown() {
    ClockUtil.reset();
    for(final Job job : managementService.createJobQuery().list()) {

      processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
        ((JobEntity) job).delete();
        return null;
      });
    }
  }

  @Test
  public void testCompetingJobExecutionDeleteJobDuringExecution() {
    //given a simple process with a async service task
    testRule.deploy(Bpmn
            .createExecutableProcess("process")
              .startEvent()
              .serviceTask("task")
                .operatonAsyncBefore()
                .operatonExpression("${true}")
              .endEvent()
            .done());
    runtimeService.startProcessInstanceByKey("process");
    Job currentJob = managementService.createJobQuery().singleResult();

    // when a job is executed
    JobExecutionThread threadOne = new JobExecutionThread(currentJob.getId());
    threadOne.startAndWaitUntilControlIsReturned();
    //and deleted in parallel
    managementService.deleteJob(currentJob.getId());

    // then the job fails with a OLE and the failed job listener throws no NPE
    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception instanceof OptimisticLockingException).isTrue();
  }

  @Test
  public void shouldCompleteTimeoutRetryWhenTimeoutedJobCompletesInbetween() {
    // given a simple process with an async service task
    testRule.deploy(Bpmn
        .createExecutableProcess("process")
          .startEvent()
          .serviceTask("task")
            .operatonAsyncBefore()
            .operatonExpression("${true}")
          .endEvent()
        .done());
    runtimeService.startProcessInstanceByKey("process");
    Job currentJob = managementService.createJobQuery().singleResult();

    // and a job is executed until before the command context is closed
    JobExecutionThread threadOne = new JobExecutionThread(currentJob.getId());
    threadOne.startAndWaitUntilControlIsReturned();

    // and lock is expiring in the meantime
    ClockUtil.offset(engineRule.getProcessEngineConfiguration().getJobExecutor().getLockTimeInMillis() + 10_000L);

    // and job is acquired again
    JobAcquisitionThread acquisitionThread = new JobAcquisitionThread();
    acquisitionThread.startAndWaitUntilControlIsReturned();
    acquisitionThread.proceedAndWaitTillDone();

    // and the job is executed again until before the command context is closed
    JobExecutionThread threadTwo = new JobExecutionThread(currentJob.getId());
    threadTwo.startAndWaitUntilControlIsReturned();

    // and the first execution finishes
    threadOne.proceedAndWaitTillDone();

    // when
    threadTwo.proceedAndWaitTillDone();

    // then
    assertThat(threadOne.exception)
      .isInstanceOf(OptimisticLockingException.class)
      .hasMessageContaining("DELETE MessageEntity")
      .hasMessageContaining("Entity was updated by another transaction concurrently");
    assertThat(threadTwo.exception).isNull();
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Test
  @Deployment
  public void testCompetingJobExecutionDefaultRetryStrategy() {
    // given an MI subprocess with two instances
    runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    List<Job> currentJobs = managementService.createJobQuery().list();
    assertThat(currentJobs).hasSize(2);

    // when the jobs are executed in parallel
    JobExecutionThread threadOne = new JobExecutionThread(currentJobs.get(0).getId());
    threadOne.startAndWaitUntilControlIsReturned();

    JobExecutionThread threadTwo = new JobExecutionThread(currentJobs.get(1).getId());
    threadTwo.startAndWaitUntilControlIsReturned();

    // then the first committing thread succeeds
    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    // then the second committing thread fails with an OptimisticLockingException
    // and the job retries have not been decremented
    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    Job remainingJob = managementService.createJobQuery().singleResult();
    assertThat(remainingJob.getRetries()).isEqualTo(currentJobs.get(1).getRetries());

    assertThat(remainingJob.getExceptionMessage()).isNotNull();

    JobEntity jobEntity = (JobEntity) remainingJob;
    assertThat(jobEntity.getLockOwner()).isNull();

    // and there is no lock expiration time due to the default retry strategy
    assertThat(jobEntity.getLockExpirationTime()).isNull();
  }

  @Test
  @Deployment
  public void testCompetingJobExecutionFoxRetryStrategy() {
    // given an MI subprocess with two instances
    runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    List<Job> currentJobs = managementService.createJobQuery().list();
    assertThat(currentJobs).hasSize(2);

    // when the jobs are executed in parallel
    JobExecutionThread threadOne = new JobExecutionThread(currentJobs.get(0).getId());
    threadOne.startAndWaitUntilControlIsReturned();

    JobExecutionThread threadTwo = new JobExecutionThread(currentJobs.get(1).getId());
    threadTwo.startAndWaitUntilControlIsReturned();

    // then the first committing thread succeeds
    LOG.debug("test thread notifies thread 1");
    threadOne.proceedAndWaitTillDone();
    assertThat(threadOne.exception).isNull();

    // then the second committing thread fails with an OptimisticLockingException
    // and the job retries have not been decremented
    LOG.debug("test thread notifies thread 2");
    threadTwo.proceedAndWaitTillDone();
    assertThat(threadTwo.exception).isNotNull();

    Job remainingJob = managementService.createJobQuery().singleResult();
    // retries are configured as R5/PT5M, so no decrement means 5 retries left
    assertThat(remainingJob.getRetries()).isEqualTo(5);

    assertThat(remainingJob.getExceptionMessage()).isNotNull();

    JobEntity jobEntity = (JobEntity) remainingJob;
    assertThat(jobEntity.getLockOwner()).isNull();

    // and there is a due date time set
    assertThat(jobEntity.getDuedate()).isNotNull();
  }

  @Test
  public void testCompletingJobExecutionSuspendDuringExecution() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    Job job = managementService.createJobQuery().singleResult();

    // given a waiting execution and a waiting suspension
    JobExecutionThread executionthread = new JobExecutionThread(job.getId());
    executionthread.startAndWaitUntilControlIsReturned();

    JobSuspensionThread jobSuspensionThread = new JobSuspensionThread("simpleAsyncProcess");
    jobSuspensionThread.startAndWaitUntilControlIsReturned();

    // first complete suspension:
    jobSuspensionThread.proceedAndWaitTillDone();
    executionthread.proceedAndWaitTillDone();

    // then the execution will fail with optimistic locking
    assertThat(jobSuspensionThread.exception).isNull();
    assertThat(executionthread.exception).isNotNull();

    //--------------------------------------------

    // given a waiting execution and a waiting suspension
    executionthread = new JobExecutionThread(job.getId());
    executionthread.startAndWaitUntilControlIsReturned();

    jobSuspensionThread = new JobSuspensionThread("simpleAsyncProcess");
    jobSuspensionThread.startAndWaitUntilControlIsReturned();

    // first complete execution:
    executionthread.proceedAndWaitTillDone();
    jobSuspensionThread.proceedAndWaitTillDone();

    // then there are no optimistic locking exceptions
    assertThat(jobSuspensionThread.exception).isNull();
    assertThat(executionthread.exception).isNull();
  }

  @Test
  public void testCompletingSuspendJobDuringAcquisition() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // given a waiting acquisition and a waiting suspension
    JobAcquisitionThread acquisitionThread = new JobAcquisitionThread();
    acquisitionThread.startAndWaitUntilControlIsReturned();

    JobSuspensionThread jobSuspensionThread = new JobSuspensionThread("simpleAsyncProcess");
    jobSuspensionThread.startAndWaitUntilControlIsReturned();

    // first complete suspension:
    jobSuspensionThread.proceedAndWaitTillDone();
    acquisitionThread.proceedAndWaitTillDone();

    // then the acquisition will not fail with optimistic locking
    assertThat(jobSuspensionThread.exception).isNull();

    assertThat(acquisitionThread.exception).isNull();
    // but the job will also not be acquired
    assertThat(acquisitionThread.acquiredJobs.size()).isEqualTo(0);

    //--------------------------------------------

    // given a waiting acquisition and a waiting suspension
    acquisitionThread = new JobAcquisitionThread();
    acquisitionThread.startAndWaitUntilControlIsReturned();

    jobSuspensionThread = new JobSuspensionThread("simpleAsyncProcess");
    jobSuspensionThread.startAndWaitUntilControlIsReturned();

    // first complete acquisition:
    acquisitionThread.proceedAndWaitTillDone();
    jobSuspensionThread.proceedAndWaitTillDone();

    // then there are no optimistic locking exceptions
    assertThat(jobSuspensionThread.exception).isNull();
    assertThat(acquisitionThread.exception).isNull();
  }

  @Test
  public void testCompletingSuspendedJobDuringRunningInstance() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .receiveTask()
        .intermediateCatchEvent()
          .timerWithDuration("PT0M")
        .endEvent()
        .done());

    // given
    // a process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

    // suspend the process definition (and the job definitions)
    repositoryService.suspendProcessDefinitionById(processDefinition.getId());

    // assert that there still exists a running and active process instance
    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(1);

    // when
    runtimeService.signal(processInstance.getId());

    // then
    // there should be one suspended job
    assertThat(managementService.createJobQuery().suspended().count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().active().count()).isEqualTo(0);

    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isEqualTo(1);

  }

  @Test
  public void testCompletingUpdateJobDefinitionPriorityDuringExecution() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    // given
    // two running instances
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // and a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // and two jobs
    List<Job> jobs = managementService.createJobQuery().list();

    // when the first job is executed but has not yet committed
    JobExecutionThread executionThread = new JobExecutionThread(jobs.get(0).getId());
    executionThread.startAndWaitUntilControlIsReturned();

    // and the job priority is updated
    JobDefinitionPriorityThread priorityThread = new JobDefinitionPriorityThread(jobDefinition.getId(), 42L, true);
    priorityThread.startAndWaitUntilControlIsReturned();

    // and the priority threads commits first
    priorityThread.proceedAndWaitTillDone();

    // then both jobs priority has changed
    List<Job> currentJobs = managementService.createJobQuery().list();
    for (Job job : currentJobs) {
      assertThat(job.getPriority()).isEqualTo(42);
    }

    // and the execution thread can nevertheless successfully finish job execution
    executionThread.proceedAndWaitTillDone();

    long remainingJobCount = managementService.createJobQuery().count();
    assertThat(executionThread.exception).isNull();

    // and ultimately only one job with an updated priority is left
    assertThat(remainingJobCount).isEqualTo(1L);
  }

  @Test
  public void testCompletingSuspensionJobDuringPriorityUpdate() {
    testRule.deploy(SIMPLE_ASYNC_PROCESS);

    // given
    // two running instances (ie two jobs)
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    // a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when suspending the jobs is attempted
    JobSuspensionByJobDefinitionThread suspensionThread = new JobSuspensionByJobDefinitionThread(jobDefinition.getId());
    suspensionThread.startAndWaitUntilControlIsReturned();

    // and updating the priority is attempted
    JobDefinitionPriorityThread priorityUpdateThread = new JobDefinitionPriorityThread(jobDefinition.getId(), 42L, true);
    priorityUpdateThread.startAndWaitUntilControlIsReturned();

    // and both commands overlap each other
    suspensionThread.proceedAndWaitTillDone();
    priorityUpdateThread.proceedAndWaitTillDone();

    // then both updates have been performed
    List<Job> updatedJobs = managementService.createJobQuery().list();
    assertThat(updatedJobs).hasSize(2);
    for (Job job : updatedJobs) {
      assertThat(job.getPriority()).isEqualTo(42);
      assertThat(job.isSuspended()).isTrue();
    }
  }


  public class JobExecutionThread extends ControllableThread {

    OptimisticLockingException exception;
    String jobId;

    JobExecutionThread(String jobId) {
      this.jobId = jobId;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        JobFailureCollector jobFailureCollector = new JobFailureCollector(jobId);
        ExecuteJobHelper.executeJob(jobId, processEngineConfiguration.getCommandExecutorTxRequired(),jobFailureCollector,
            new ControlledCommand<>(activeThread, new ExecuteJobsCmd(jobId, jobFailureCollector)));

      }
      catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug(getName() + " ends");
    }
  }

  public class JobAcquisitionThread extends ControllableThread {
    OptimisticLockingException exception;
    AcquiredJobs acquiredJobs;
    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }
    @Override
    public void run() {
      try {
        JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
        acquiredJobs = processEngineConfiguration.getCommandExecutorTxRequired()
          .execute(new ControlledCommand<>(activeThread, new AcquireJobsCmd(jobExecutor)));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }
  }

  public class JobSuspensionThread extends ControllableThread {
    OptimisticLockingException exception;
    String processDefinitionKey;

    public JobSuspensionThread(String processDefinitionKey) {
      this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        processEngineConfiguration.getCommandExecutorTxRequired()
          .execute(new ControlledCommand<>(activeThread, createSuspendJobCommand()));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }

    protected Command<Void> createSuspendJobCommand() {
      UpdateJobDefinitionSuspensionStateBuilderImpl builder = new UpdateJobDefinitionSuspensionStateBuilderImpl()
        .byProcessDefinitionKey(processDefinitionKey)
        .includeJobs(true);

      return new SuspendJobDefinitionCmd(builder);
    }
  }

  public class JobSuspensionByJobDefinitionThread extends ControllableThread {
    OptimisticLockingException exception;
    String jobDefinitionId;

    public JobSuspensionByJobDefinitionThread(String jobDefinitionId) {
      this.jobDefinitionId = jobDefinitionId;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        processEngineConfiguration.getCommandExecutorTxRequired()
          .execute(new ControlledCommand<>(activeThread, createSuspendJobCommand()));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }

    protected SuspendJobCmd createSuspendJobCommand() {
      UpdateJobSuspensionStateBuilderImpl builder = new UpdateJobSuspensionStateBuilderImpl().byJobDefinitionId(jobDefinitionId);
      return new SuspendJobCmd(builder);
    }
  }

  public class JobDefinitionPriorityThread extends ControllableThread {
    OptimisticLockingException exception;
    String jobDefinitionId;
    Long priority;
    boolean cascade;

    public JobDefinitionPriorityThread(String jobDefinitionId, Long priority, boolean cascade) {
      this.jobDefinitionId = jobDefinitionId;
      this.priority = priority;
      this.cascade = cascade;
    }

    @Override
    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    @Override
    public void run() {
      try {
        processEngineConfiguration.getCommandExecutorTxRequired()
          .execute(new ControlledCommand<>(activeThread, new SetJobDefinitionPriorityCmd(jobDefinitionId, priority, cascade)));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
    }
  }
}
