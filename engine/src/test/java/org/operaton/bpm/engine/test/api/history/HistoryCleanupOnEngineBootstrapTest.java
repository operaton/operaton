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
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.cfg.BatchWindowConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandlerConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.junit.Test;

/**
 * @author Nikola Koevski
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoryCleanupOnEngineBootstrapTest {

  private static final String ENGINE_NAME = "engineWithHistoryCleanupBatchWindow";

  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @Test
  public void testConsecutiveEngineBootstrapHistoryCleanupJobReconfiguration() {

    // given
    // create history cleanup job
    ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/batchwindow.operaton.cfg.xml")
      .buildProcessEngine()
      .close();

    // when
    // suspend history cleanup job
    ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/no-batchwindow.operaton.cfg.xml")
      .buildProcessEngine()
      .close();

    // then
    // reconfigure history cleanup job
    ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/batchwindow.operaton.cfg.xml");
    processEngineConfiguration.setProcessEngineName(ENGINE_NAME);
    ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

    assertThat(ProcessEngines.getProcessEngine(ENGINE_NAME)).isNotNull();

    closeProcessEngine(processEngine);
  }

  @Test
  public void testDecreaseNumberOfHistoryCleanupJobs() {
    // given
    // create history cleanup job
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-default.operaton.cfg.xml")
      .buildProcessEngine();

    // assume
    ManagementService managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().list()).hasSize(4);

    engine.close();

    // when
    engine = ProcessEngineConfiguration
    .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-less.operaton.cfg.xml")
      .buildProcessEngine();

    // then
    // reconfigure history cleanup job
    managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().list()).hasSize(1);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(getHistoryCleanupJobHandlerConfiguration(job).getMinuteFrom()).isEqualTo(0);
    assertThat(getHistoryCleanupJobHandlerConfiguration(job).getMinuteTo()).isEqualTo(59);

    closeProcessEngine(engine);
  }

  @Test
  public void testIncreaseNumberOfHistoryCleanupJobs() {
    // given
    // create history cleanup job
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-default.operaton.cfg.xml")
      .buildProcessEngine();

    // assume
    ManagementService managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().count()).isEqualTo(4);

    engine.close();

    // when
    engine = ProcessEngineConfiguration
    .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-more.operaton.cfg.xml")
      .buildProcessEngine();

    // then
    // reconfigure history cleanup job
    managementService = engine.getManagementService();
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(8);

    for (Job job : jobs) {
      int minuteTo = getHistoryCleanupJobHandlerConfiguration(job).getMinuteTo();
      int minuteFrom = getHistoryCleanupJobHandlerConfiguration(job).getMinuteFrom();

      if (minuteFrom == 0) {
        assertThat(minuteTo).isEqualTo(6);
      }
      else if (minuteFrom == 7) {
        assertThat(minuteTo).isEqualTo(13);
      }
      else if (minuteFrom == 14) {
        assertThat(minuteTo).isEqualTo(20);
      }
      else if (minuteFrom == 21) {
        assertThat(minuteTo).isEqualTo(27);
      }
      else if (minuteFrom == 28) {
        assertThat(minuteTo).isEqualTo(34);
      }
      else if (minuteFrom == 35) {
        assertThat(minuteTo).isEqualTo(41);
      }
      else if (minuteFrom == 42) {
        assertThat(minuteTo).isEqualTo(48);
      }
      else if (minuteFrom == 49) {
        assertThat(minuteTo).isEqualTo(59);
      }
      else {
        fail("unexpected minute from " + minuteFrom);
      }
    }

    closeProcessEngine(engine);
  }

  @Test
  public void testBatchWindowXmlConfigParsingException() {
    // when/then
    assertThatThrownBy(() -> ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-map-wrong-values.operaton.cfg.xml")
      .buildProcessEngine())
    .isInstanceOf(Exception.class)
    .hasMessageContaining("startTime");
  }

  @Test
  public void testBatchWindowMapInXmlConfig() throws ParseException {
    // given
    //we're on Monday
    ClockUtil.setCurrentTime(sdf.parse("2018-05-14T22:00:00"));

    //when
    //we configure batch window only for Wednesday and start the server
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-map.operaton.cfg.xml")
      .buildProcessEngine();

    //then
    //history cleanup is scheduled for Wednesday
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-16T23:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    engine.close();

    //when
    //we reconfigure batch window with default values
    engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-default.operaton.cfg.xml")
      .buildProcessEngine();

    //then
    //history cleanup is scheduled for today
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-14T23:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    closeProcessEngine(engine);
  }

  @Test
  public void testHistoryCleanupJobScheduled() {

    final ProcessEngineConfigurationImpl standaloneInMemProcessEngineConfiguration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    standaloneInMemProcessEngineConfiguration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testHistoryCleanupJobScheduled");

    ProcessEngine engine = standaloneInMemProcessEngineConfiguration
      .buildProcessEngine();

    try {
      final List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
      assertThat(historyCleanupJobs).isNotEmpty();
      final ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();
      for (Job historyCleanupJob : historyCleanupJobs) {
        assertThat(historyCleanupJob.getDuedate()).isEqualTo(processEngineConfiguration.getBatchWindowManager().getCurrentOrNextBatchWindow(ClockUtil.getCurrentTime(), processEngineConfiguration).getStart());
      }
    } finally {
      closeProcessEngine(engine);
    }
  }

  @Test
  public void shouldCreateHistoryCleanupJobLogs() {

    final ProcessEngineConfigurationImpl standaloneInMemProcessEngineConfiguration =
        (ProcessEngineConfigurationImpl)ProcessEngineConfiguration
            .createStandaloneInMemProcessEngineConfiguration();
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    standaloneInMemProcessEngineConfiguration
        .setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testHistoryCleanupJobScheduled");

    ProcessEngine engine = standaloneInMemProcessEngineConfiguration.buildProcessEngine();
    try {
      List<HistoricJobLog> historicJobLogs = engine.getHistoryService()
                                                   .createHistoricJobLogQuery()
                                                   .jobDefinitionType(HistoryCleanupJobHandler.TYPE)
                                                   .list();
      for (HistoricJobLog historicJobLog : historicJobLogs) {
        assertThat(historicJobLog.getHostname()).isNotNull();
      }
    } finally {
      closeProcessEngine(engine);
    }
  }

  @Test
  public void testBatchWindowOneDayOfWeek() throws ParseException {
    ClockUtil.setCurrentTime(sdf.parse("2018-05-14T22:00:00"));       //monday
    //given
    final ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    //we have batch window only once per week - Monday afternoon
    configuration.getHistoryCleanupBatchWindows().put(Calendar.MONDAY, new BatchWindowConfiguration("18:00", "20:00"));
    configuration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testBatchWindowOneDayOfWeek");

    //when
    //we're on Monday evening
    //and we bootstrap the engine
    ProcessEngine engine = configuration.buildProcessEngine();

    //then
    //job is scheduled for next week Monday
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-21T18:00:00"));     //monday next week
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    //when
    //we're on Monday evening next week, right aftre the end of batch window
    ClockUtil.setCurrentTime(sdf.parse("2018-05-21T20:00:01"));       //monday
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is scheduled for next week Monday
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-28T18:00:00"));     //monday next week
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    closeProcessEngine(engine);
  }

  @Test
  public void testBatchWindow24Hours() throws ParseException {
    //given
    final ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    //we have batch window for 24 hours
    configuration.getHistoryCleanupBatchWindows().put(Calendar.MONDAY, new BatchWindowConfiguration("06:00", "06:00"));
    configuration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testBatchWindow24Hours");

    //when
    //we're on Monday early morning
    ClockUtil.setCurrentTime(sdf.parse("2018-05-14T05:00:00"));       //monday
    //and we bootstrap the engine
    ProcessEngine engine = configuration.buildProcessEngine();

    //then
    //job is scheduled for Monday 06 AM
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-14T06:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    //when
    //we're on Monday afternoon
    ClockUtil.setCurrentTime(sdf.parse("2018-05-14T15:00:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is still within current batch window
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(sdf.parse("2018-05-15T06:00:00").after(historyCleanupJobs.get(0).getDuedate())).isTrue();
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    //when
    //we're on Tuesday early morning close to the end of batch window
    ClockUtil.setCurrentTime(sdf.parse("2018-05-15T05:59:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is still within current batch window
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(sdf.parse("2018-05-15T06:00:00").after(historyCleanupJobs.get(0).getDuedate())).isTrue();
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    //when
    //we're on Tuesday early morning shortly after the end of batch window
    ClockUtil.setCurrentTime(sdf.parse("2018-05-15T06:01:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is rescheduled till next Monday
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs).isNotEmpty();
    assertThat(historyCleanupJobs).hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(sdf.parse("2018-05-21T06:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isEqualTo(false);

    closeProcessEngine(engine);
  }

  protected HistoryCleanupJobHandlerConfiguration getHistoryCleanupJobHandlerConfiguration(Job job) {
    return HistoryCleanupJobHandlerConfiguration
          .fromJson(JsonUtil.asObject(((JobEntity) job).getJobHandlerConfigurationRaw()));
  }

  protected void closeProcessEngine(ProcessEngine processEngine) {
    ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    final HistoryService historyService = processEngine.getHistoryService();
    configuration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {

      List<Job> jobs = historyService.findHistoryCleanupJobs();
      for (Job job: jobs) {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }

      //cleanup "detached" historic job logs
      final List<HistoricJobLog> list = historyService.createHistoricJobLogQuery().list();
      for (HistoricJobLog jobLog: list) {
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobLog.getJobId());
      }

      commandContext.getMeterLogManager().deleteAll();

      return null;
    });

    processEngine.close();
  }

}
