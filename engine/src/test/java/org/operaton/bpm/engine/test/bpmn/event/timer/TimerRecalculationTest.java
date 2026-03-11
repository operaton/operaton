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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandler;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Test timer recalculation
 *
 * @author Tobias Metzke
 */

class TimerRecalculationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  ManagementService managementService;
  RuntimeService runtimeService;
  HistoryService historyService;

  private Set<String> jobIds = new HashSet<>();

  @AfterEach
  void tearDown() {
    clearMeterLog();

    for (String jobId : jobIds) {
      clearJobLog(jobId);
      clearJob(jobId);
    }

    jobIds = new HashSet<>();
  }

  @Test
  void testUnknownId() {
    // when/then
    assertThatThrownBy(() -> managementService.recalculateJobDuedate("unknownID", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No job found with id 'unknownID");
  }

  @Test
  void testEmptyId() {
    // when/then
    assertThatThrownBy(() -> managementService.recalculateJobDuedate("", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The job id is mandatory: jobId is empty");
  }

  @Test
  void testNullId() {
    // when/then
    assertThatThrownBy(() -> managementService.recalculateJobDuedate(null, false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The job id is mandatory: jobId is null");
  }

  @Deployment
  @Test
  void testFinishedJob() {
    // given
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("dueDate", new Date());

    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isOne();

    JobQuery jobQuery = managementService.createJobQuery().executable();
    assertThat(jobQuery.count()).isOne();

    // job duedate can be recalculated, job still exists in runtime
    String jobId = jobQuery.singleResult().getId();
    managementService.recalculateJobDuedate(jobId, false);
    // run the job, finish the process
    managementService.executeJob(jobId);
    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isZero();
    testRule.assertProcessEnded(pi1.getProcessInstanceId());

    // when/then
    assertThatThrownBy(() -> managementService.recalculateJobDuedate(jobId, false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No job found with id '" + jobId);
  }

  @Test
  void testEverLivingJob() {
    // given
    Job job = historyService.cleanUpHistoryAsync(true);
    jobIds.add(job.getId());

    // when & then
    tryRecalculateUnsupported(job, HistoryCleanupJobHandler.TYPE);
  }

  @Deployment
  @Test
  void testMessageJob() {
    // given
    runtimeService.startProcessInstanceByKey("asyncService");
    Job job = managementService.createJobQuery().singleResult();
    jobIds.add(job.getId());

    // when & then
    tryRecalculateUnsupported(job, AsyncContinuationJobHandler.TYPE);
  }


  // helper /////////////////////////////////////////////////////////////////

  protected void tryRecalculateUnsupported(Job job, String type) {
    // given
    String jobId = job.getId();

    // when/then
    assertThatThrownBy(() -> managementService.recalculateJobDuedate(jobId, false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Only timer jobs can be recalculated, but the job with id '%s' is of type '%s".formatted(jobId, type));
  }


  protected void clearMeterLog() {
    processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> {
      commandContext.getMeterLogManager().deleteAll();

      return null;
    });
  }

  protected void clearJobLog(final String jobId) {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
      return null;
    });
  }

  protected void clearJob(final String jobId) {
    processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> {
      JobEntity job = commandContext.getJobManager().findJobById(jobId);
      if (job != null) {
        commandContext.getJobManager().delete(job);
      }
      return null;
    });
  }
}
