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
package org.operaton.bpm.engine.test.jobexecutor;

import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;

import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Daniel Meyer
 */
public class AcquireJobsCmdTest extends PluggableProcessEngineTest {

  @Deployment(resources={"org/operaton/bpm/engine/test/standalone/jobexecutor/oneJobProcess.bpmn20.xml"})
  @Test
  public void testJobsNotVisisbleToAcquisitionIfInstanceSuspended() {

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(pd.getKey());

    // now there is one job:
    Job job = managementService.createJobQuery()
      .singleResult();
    assertThat(job).isNotNull();

    makeSureJobDue(job);

    // the acquirejobs command sees the job:
    AcquiredJobs acquiredJobs = executeAcquireJobsCommand();
    assertThat(acquiredJobs.size()).isEqualTo(1);

    // suspend the process instance:
    runtimeService.suspendProcessInstanceById(pi.getId());

    // now, the acquirejobs command does not see the job:
    acquiredJobs = executeAcquireJobsCommand();
    assertThat(acquiredJobs.size()).isZero();
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/standalone/jobexecutor/oneJobProcess.bpmn20.xml"})
  @Test
  public void testJobsNotVisisbleToAcquisitionIfDefinitionSuspended() {

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceByKey(pd.getKey());
    // now there is one job:
    Job job = managementService.createJobQuery()
      .singleResult();
    assertThat(job).isNotNull();

    makeSureJobDue(job);

    // the acquirejobs command sees the job:
    AcquiredJobs acquiredJobs = executeAcquireJobsCommand();
    assertThat(acquiredJobs.size()).isEqualTo(1);

    // suspend the process instance:
    repositoryService.suspendProcessDefinitionById(pd.getId());

    // now, the acquirejobs command does not see the job:
    acquiredJobs = executeAcquireJobsCommand();
    assertThat(acquiredJobs.size()).isZero();
  }

  protected void makeSureJobDue(final Job job) {
    processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> {
      Date currentTime = ClockUtil.getCurrentTime();
      commandContext.getJobManager()
          .findJobById(job.getId())
          .setDuedate(new Date(currentTime.getTime() - 10000));
      return null;
    });
  }

  private AcquiredJobs executeAcquireJobsCommand() {
    return processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(new AcquireJobsCmd(processEngineConfiguration.getJobExecutor()));
  }

}
