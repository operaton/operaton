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
package org.operaton.bpm.engine.test.api.history;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.factory.BeanCreationException;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Nikola Koevski
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoryCleanupOnEngineBootstrapTest {

  private static final String ENGINE_NAME = "engineWithHistoryCleanupBatchWindow";
  private static final String TEMP_ENGINE_NAME = "tempEngine";

  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private static Date parseDate(String dateString) {
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(dateString, sdf);
      return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse date: " + dateString, e);
    }
  }

  @Test
  void testConsecutiveEngineBootstrapHistoryCleanupJobReconfiguration() {

    // given
    // create history cleanup job
    ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/batchwindow.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine()
      .close();

    // when
    // suspend history cleanup job
    ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/no-batchwindow.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
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
  void testDecreaseNumberOfHistoryCleanupJobs() {
    // given
    // create history cleanup job
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-default.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine();

    // assume
    ManagementService managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().list()).hasSize(4);

    engine.close();

    // when
    engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-less.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine();

    // then
    // reconfigure history cleanup job
    managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().list()).hasSize(1);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(getHistoryCleanupJobHandlerConfiguration(job).getMinuteFrom()).isZero();
    assertThat(getHistoryCleanupJobHandlerConfiguration(job).getMinuteTo()).isEqualTo(59);

    closeProcessEngine(engine);
  }

  @Test
  void testIncreaseNumberOfHistoryCleanupJobs() {
    // given
    // create history cleanup job
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-default.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine();

    // assume
    ManagementService managementService = engine.getManagementService();
    assertThat(managementService.createJobQuery().count()).isEqualTo(4);

    engine.close();

    // when
    engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-parallelism-more.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
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
  void testBatchWindowXmlConfigParsingException() {
    // when/then
    assertThatThrownBy(() -> ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-map-wrong-values.operaton.cfg.xml"))
      .isInstanceOf(BeanCreationException.class)
      .hasRootCauseInstanceOf(PropertyBatchUpdateException.class)
      .rootCause().hasMessageContaining("startTime");
  }

  @Test
  void testBatchWindowMapInXmlConfig() throws Exception {
    // given
    //we're on Monday
    ClockUtil.setCurrentTime(parseDate("2018-05-14T22:00:00"));

    //when
    //we configure batch window only for Wednesday and start the server
    ProcessEngine engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-map.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine();

    //then
    //history cleanup is scheduled for Wednesday
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-16T23:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    engine.close();

    //when
    //we reconfigure batch window with default values
    engine = ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/history/history-cleanup-batch-window-default.operaton.cfg.xml")
      .setProcessEngineName(TEMP_ENGINE_NAME)
      .buildProcessEngine();

    //then
    //history cleanup is scheduled for today
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-14T23:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    closeProcessEngine(engine);
  }

  @Test
  void testHistoryCleanupJobScheduled() {

    final ProcessEngineConfigurationImpl standaloneInMemProcessEngineConfiguration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    standaloneInMemProcessEngineConfiguration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testHistoryCleanupJobScheduled");
    standaloneInMemProcessEngineConfiguration.setProcessEngineName(TEMP_ENGINE_NAME);

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
  void shouldCreateHistoryCleanupJobLogs() {

    final ProcessEngineConfigurationImpl standaloneInMemProcessEngineConfiguration =
        (ProcessEngineConfigurationImpl)ProcessEngineConfiguration
            .createStandaloneInMemProcessEngineConfiguration();
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    standaloneInMemProcessEngineConfiguration.setHistoryCleanupBatchWindowEndTime("01:00");
    standaloneInMemProcessEngineConfiguration
        .setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testHistoryCleanupJobScheduled");
    standaloneInMemProcessEngineConfiguration.setProcessEngineName(TEMP_ENGINE_NAME);

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
  void testBatchWindowOneDayOfWeek() throws Exception {
    ClockUtil.setCurrentTime(parseDate("2018-05-14T22:00:00"));       //monday
    //given
    final ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    //we have batch window only once per week - Monday afternoon
    configuration.getHistoryCleanupBatchWindows().put(Calendar.MONDAY, new BatchWindowConfiguration("18:00", "20:00"));
    configuration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testBatchWindowOneDayOfWeek");
    configuration.setProcessEngineName(TEMP_ENGINE_NAME);

    //when
    //we're on Monday evening
    //and we bootstrap the engine
    ProcessEngine engine = configuration.buildProcessEngine();

    //then
    //job is scheduled for next week Monday
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-21T18:00:00"));     //monday next week
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    //when
    //we're on Monday evening next week, right aftre the end of batch window
    ClockUtil.setCurrentTime(parseDate("2018-05-21T20:00:01"));       //monday
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is scheduled for next week Monday
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-28T18:00:00"));     //monday next week
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    closeProcessEngine(engine);
  }

  @Test
  void testBatchWindow24Hours() throws Exception {
    //given
    final ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl)ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    //we have batch window for 24 hours
    configuration.getHistoryCleanupBatchWindows().put(Calendar.MONDAY, new BatchWindowConfiguration("06:00", "06:00"));
    configuration.setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName() + "testBatchWindow24Hours");
    configuration.setProcessEngineName(TEMP_ENGINE_NAME);

    //when
    //we're on Monday early morning
    ClockUtil.setCurrentTime(parseDate("2018-05-14T05:00:00"));       //monday
    //and we bootstrap the engine
    ProcessEngine engine = configuration.buildProcessEngine();

    //then
    //job is scheduled for Monday 06 AM
    List<Job> historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-14T06:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    //when
    //we're on Monday afternoon
    ClockUtil.setCurrentTime(parseDate("2018-05-14T15:00:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is still within current batch window
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(parseDate("2018-05-15T06:00:00").after(historyCleanupJobs.get(0).getDuedate())).isTrue();
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    //when
    //we're on Tuesday early morning close to the end of batch window
    ClockUtil.setCurrentTime(parseDate("2018-05-15T05:59:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is still within current batch window
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(parseDate("2018-05-15T06:00:00").after(historyCleanupJobs.get(0).getDuedate())).isTrue();
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

    //when
    //we're on Tuesday early morning shortly after the end of batch window
    ClockUtil.setCurrentTime(parseDate("2018-05-15T06:01:00"));
    //we force history job to be rescheduled
    engine.getManagementService().executeJob(historyCleanupJobs.get(0).getId());

    //then
    //job is rescheduled till next Monday
    historyCleanupJobs = engine.getHistoryService().findHistoryCleanupJobs();
    assertThat(historyCleanupJobs)
            .isNotEmpty()
            .hasSize(1);
    assertThat(historyCleanupJobs.get(0).getDuedate()).isEqualTo(parseDate("2018-05-21T06:00:00"));
    assertThat(historyCleanupJobs.get(0).isSuspended()).isFalse();

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
