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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.api.runtime.util.ChangeVariablesDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;

/**
 * @author Tassilo Weidner
 */
class JobEntityTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected List<String> jobIds = new ArrayList<>();

  protected HistoryService historyService;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;

  protected static final Date CREATE_DATE = new Date(1363607000000L);

  protected String activityIdLoggingProperty;

  @BeforeEach
  void setUp() {
    jobIds = new ArrayList<>();

    activityIdLoggingProperty = engineRule.getProcessEngineConfiguration().getLoggingContextActivityId();
  }

  @BeforeEach
  void setClock() {
    ClockUtil.setCurrentTime(CREATE_DATE);
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void cleanup() {
    for (String jobId : jobIds) {
      managementService.deleteJob(jobId);
    }

    if (!testRule.isHistoryLevelNone()) {
      cleanupJobLog();
    }

    engineRule.getProcessEngineConfiguration().setLoggingContextActivityId(activityIdLoggingProperty);
  }

  @Test
  void shouldCheckCreateTimeOnMessage() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .operatonAsyncBefore()
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process");

    // when
    Job messageJob = managementService.createJobQuery().singleResult();

    // then
    assertThat(messageJob.getCreateTime()).isEqualTo(CREATE_DATE);
    assertThat(messageJob.getClass().getSimpleName()).isEqualTo("MessageEntity");

    // cleanup
    jobIds.add(messageJob.getId());
  }

  @Test
  void shouldCheckCreateTimeOnTimer() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .timerWithDuration("PT5S")
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process");

    // when
    Job timerJob = managementService.createJobQuery().singleResult();

    // then
    assertThat(timerJob.getCreateTime()).isEqualTo(CREATE_DATE);
    assertThat(timerJob.getClass().getSimpleName()).isEqualTo("TimerEntity");

    // cleanup
    jobIds.add(timerJob.getId());
  }

  @Test
  void shouldCheckCreateTimeOnEverLivingJob() {
    // given
    historyService.cleanUpHistoryAsync(true);

    // when
    Job everLivingJob = managementService.createJobQuery().singleResult();

    // then
    assertThat(everLivingJob.getCreateTime()).isEqualTo(CREATE_DATE);
    assertThat(everLivingJob.getClass().getSimpleName()).isEqualTo("EverLivingJobEntity");

    // cleanup
    jobIds.add(everLivingJob.getId());
  }

  @Test
  void shouldShowFailedActivityIdPropertyForFailingAsyncTask() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(FailingDelegate.class)
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    var jobId = job.getId();

    // when
    try {
      managementService.executeJob(jobId);
      fail("Exception expected");
    } catch (Exception e) {
      // exception expected
    }

    // then
    job = (JobEntity) managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getFailedActivityId()).isEqualTo("theTask");
  }

  @Test
  void shouldShowFailedActivityIdIfActivityIdLoggingIsDisabled() {
    // given
    engineRule.getProcessEngineConfiguration().setLoggingContextActivityId(null);

    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(FailingDelegate.class)
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    var jobId = job.getId();

    // when
    try {
      managementService.executeJob(jobId);
      fail("Exception expected");
    } catch (Exception e) {
      // exception expected
    }

    // then
    job = (JobEntity) managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getFailedActivityId()).isEqualTo("theTask");
  }

  @Test
  void shouldShowFailedActivityIdPropertyForAsyncTaskWithFailingFollowUp() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask("theTask")
        .operatonAsyncBefore()
        .operatonClass(ChangeVariablesDelegate.class)
        .serviceTask("theTask2")
        .operatonClass(ChangeVariablesDelegate.class)
        .serviceTask("theTask3")
        .operatonClass(FailingDelegate.class)
        .endEvent()
        .done());

    runtimeService.startProcessInstanceByKey("process", Variables.createVariables().putValue("fail", true));
    JobEntity job = (JobEntity) managementService.createJobQuery().singleResult();
    var jobId = job.getId();

    // when
    try {
      managementService.executeJob(jobId);
      fail("Exception expected");
    } catch (Exception e) {
      // exception expected
    }

    // then
    job = (JobEntity) managementService.createJobQuery().jobId(job.getId()).singleResult();
    assertThat(job.getFailedActivityId()).isEqualTo("theTask3");
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected void cleanupJobLog() {
    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
      .execute(commandContext -> {
      for (String jobId : jobIds) {
        commandContext.getHistoricJobLogManager()
            .deleteHistoricJobLogByJobId(jobId);
      }

      return null;
    });
  }

}
