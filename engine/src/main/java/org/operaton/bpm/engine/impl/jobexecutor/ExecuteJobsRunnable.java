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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.util.List;

import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.operaton.bpm.engine.impl.cmd.UnlockJobCmd;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.interceptor.ProcessDataContext;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class ExecuteJobsRunnable implements Runnable {

  private static final JobExecutorLogger LOG = ProcessEngineLogger.JOB_EXECUTOR_LOGGER;

  protected final List<String> jobIds;
  protected JobExecutor jobExecutor;
  protected ProcessEngineImpl processEngine;

  public ExecuteJobsRunnable(List<String> jobIds, ProcessEngineImpl processEngine) {
    this.jobIds = jobIds;
    this.processEngine = processEngine;
    this.jobExecutor = processEngine.getProcessEngineConfiguration().getJobExecutor();
  }

  @Override
  public void run() {
    final JobExecutorContext jobExecutorContext = new JobExecutorContext();

    final List<String> currentProcessorJobQueue = jobExecutorContext.getCurrentProcessorJobQueue();
    ProcessEngineConfigurationImpl engineConfiguration = processEngine.getProcessEngineConfiguration();
    CommandExecutor commandExecutor = engineConfiguration.getCommandExecutorTxRequired();

    currentProcessorJobQueue.addAll(jobIds);

    Context.setJobExecutorContext(jobExecutorContext);

    ClassLoader classLoaderBeforeExecution = switchClassLoader();

    try {
      while (!currentProcessorJobQueue.isEmpty()) {

        String nextJobId = currentProcessorJobQueue.remove(0);
        if (jobExecutor.isActive()) {
          JobFailureCollector jobFailureCollector = new JobFailureCollector(nextJobId);
          try {
            executeJob(nextJobId, commandExecutor, jobFailureCollector);
          } catch(Exception t) {
            if (ProcessEngineLogger.shouldLogJobException(engineConfiguration, jobFailureCollector.getJob())) {
              ExecuteJobHelper.loggingHandler.exceptionWhileExecutingJob(nextJobId, t);
            }
          } finally {
            /*
             * clear MDC of potential leftovers from command execution
             * that have not been cleared in Context#removeCommandInvocationContext()
             * in case of exceptions in command execution
             */
            new ProcessDataContext(engineConfiguration).clearMdc();
          }
        } else {
            try {
              unlockJob(nextJobId, commandExecutor);
            }
            catch(Exception t) {
              LOG.exceptionWhileUnlockingJob(nextJobId, t);
            }
        }
      }

      // if there were only exclusive jobs then the job executor
      // does a backoff. In order to avoid too much waiting time
      // we need to tell him to check once more if there were any jobs added.
      jobExecutor.jobWasAdded();

    } finally {
      Context.removeJobExecutorContext();
      ClassLoaderUtil.setContextClassloader(classLoaderBeforeExecution);
    }
  }

  /**
   * Note: this is a hook to be overridden by
   * org.operaton.bpm.container.impl.threading.ra.inflow.JcaInflowExecuteJobsRunnable.executeJob(String, CommandExecutor)
   */
  protected void executeJob(String nextJobId, CommandExecutor commandExecutor, JobFailureCollector jobFailureCollector) {
    ExecuteJobHelper.executeJob(commandExecutor, jobFailureCollector, new ExecuteJobsCmd(nextJobId, jobFailureCollector), processEngine.getProcessEngineConfiguration());
  }

  protected void unlockJob(String nextJobId, CommandExecutor commandExecutor) {
    commandExecutor.execute(new UnlockJobCmd(nextJobId));
  }

  /**
   * Switch the context classloader to the ProcessEngine's
   * to assure the loading of the engine classes during job execution<br>
   *
   * <p>
   * <b>Note</b>: this method is overridden by
   * org.operaton.bpm.container.impl.threading.ra.inflow.JcaInflowExecuteJobsRunnable#switchClassLoader()
   * - where the classloader switch is not required
   * </p>
   *
   * @see https://app.camunda.com/jira/browse/CAM-10379
   *
   * @return the classloader before the switch to return it back after the job execution
   */
  protected ClassLoader switchClassLoader() {
    return ClassLoaderUtil.switchToProcessEngineClassloader();
  }

}
