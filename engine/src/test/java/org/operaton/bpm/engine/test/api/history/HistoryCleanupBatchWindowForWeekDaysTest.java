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

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 *
 * @author Svetlana Dorokhova
 *
 */
@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoryCleanupBatchWindowForWeekDaysTest {

  protected String defaultStartTime;
  protected String defaultEndTime;
  protected int defaultBatchSize;

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .ensureCleanAfterTest(true)
    .cacheForConfigurationResource(false)
    .configurator(configuration -> {
      configuration.setHistoryCleanupBatchSize(20);
      configuration.setHistoryCleanupBatchThreshold(10);
      configuration.setDefaultNumberOfRetries(5);

      configuration.setMondayHistoryCleanupBatchWindowStartTime("22:00");
      configuration.setMondayHistoryCleanupBatchWindowEndTime("01:00");
      configuration.setTuesdayHistoryCleanupBatchWindowStartTime("22:00");
      configuration.setTuesdayHistoryCleanupBatchWindowEndTime("23:00");
      configuration.setWednesdayHistoryCleanupBatchWindowStartTime("15:00");
      configuration.setWednesdayHistoryCleanupBatchWindowEndTime("20:00");
      configuration.setFridayHistoryCleanupBatchWindowStartTime("22:00");
      configuration.setFridayHistoryCleanupBatchWindowEndTime("01:00");
      configuration.setSundayHistoryCleanupBatchWindowStartTime("10:00");
      configuration.setSundayHistoryCleanupBatchWindowEndTime("20:00");
    }).build();
  @RegisterExtension
  protected static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private HistoryService historyService;
  private ManagementService managementService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @Parameter(0)
  public Date currentDate;

  @Parameter(1)
  public Date startDateForCheck;

  @Parameter(2)
  public Date endDateForCheck;

  @Parameter(3)
  public Date startDateForCheckWithDefaultValues;

  @Parameter(4)
  public Date endDateForCheckWithDefaultValues;

  @Parameters
  public static Collection<Object[]> scenarios() throws ParseException {
    return Arrays.asList(new Object[][] {
        {  sdf.parse("2018-05-14T10:00:00"), sdf.parse("2018-05-14T22:00:00"), sdf.parse("2018-05-15T01:00:00"), null, null},  //monday
        {  sdf.parse("2018-05-14T23:00:00"), sdf.parse("2018-05-14T22:00:00"), sdf.parse("2018-05-15T01:00:00"), null, null},  //monday
        {  sdf.parse("2018-05-15T00:30:00"), sdf.parse("2018-05-14T22:00:00"), sdf.parse("2018-05-15T01:00:00"), null, null},  //tuesday
        {  sdf.parse("2018-05-15T02:00:00"), sdf.parse("2018-05-15T22:00:00"), sdf.parse("2018-05-15T23:00:00"), null, null},  //tuesday
        {  sdf.parse("2018-05-15T23:30:00"), sdf.parse("2018-05-16T15:00:00"), sdf.parse("2018-05-16T20:00:00"), null, null},  //tuesday
        {  sdf.parse("2018-05-16T21:00:00"), sdf.parse("2018-05-18T22:00:00"), sdf.parse("2018-05-19T01:00:00"),
              sdf.parse("2018-05-17T23:00:00"), sdf.parse("2018-05-18T00:00:00") },                                 //wednesday
        {  sdf.parse("2018-05-20T09:00:00"), sdf.parse("2018-05-20T10:00:00"), sdf.parse("2018-05-20T20:00:00"), null, null }} ); //sunday
  }

  @BeforeEach
  void init() {
    defaultStartTime = processEngineConfiguration.getHistoryCleanupBatchWindowStartTime();

    defaultEndTime = processEngineConfiguration.getHistoryCleanupBatchWindowEndTime();
    defaultBatchSize = processEngineConfiguration.getHistoryCleanupBatchSize();
  }

  @AfterEach
  void clearDatabase() {
    //reset configuration changes
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(defaultStartTime);
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(defaultEndTime);
    processEngineConfiguration.setHistoryCleanupBatchSize(defaultBatchSize);
    
    processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {

      List<Job> jobs = managementService.createJobQuery().list();
      if (!jobs.isEmpty()) {
        assertThat(jobs).hasSize(1);
        String jobId = jobs.get(0).getId();
        commandContext.getJobManager().deleteJob((JobEntity) jobs.get(0));
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(jobId);
      }

      return null;
    });
  }

  @TestTemplate
  void testScheduleJobForBatchWindow() {

    ClockUtil.setCurrentTime(currentDate);
    processEngineConfiguration.initHistoryCleanup();
    Job job = historyService.cleanUpHistoryAsync();

    assertThat(startDateForCheck.after(job.getDuedate())).isFalse(); // job due date is not before start date
    assertThat(endDateForCheck.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheck, -1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(startDateForCheck.after(job.getDuedate())).isFalse();
    assertThat(endDateForCheck.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheck, 1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(endDateForCheck.before(job.getDuedate())).isTrue();
  }

  @TestTemplate
  void testScheduleJobForBatchWindowWithDefaultWindowConfigured() {
    ClockUtil.setCurrentTime(currentDate);
    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime("00:00");
    processEngineConfiguration.initHistoryCleanup();


    Job job = historyService.cleanUpHistoryAsync();

    if (startDateForCheckWithDefaultValues == null) {
      startDateForCheckWithDefaultValues = startDateForCheck;
    }
    if (endDateForCheckWithDefaultValues == null) {
      endDateForCheckWithDefaultValues = endDateForCheck;
    }

    assertThat(startDateForCheckWithDefaultValues.after(job.getDuedate())).isFalse(); // job due date is not before start date
    assertThat(endDateForCheckWithDefaultValues.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheckWithDefaultValues, -1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(startDateForCheckWithDefaultValues.after(job.getDuedate())).isFalse();
    assertThat(endDateForCheckWithDefaultValues.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheckWithDefaultValues, 1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(endDateForCheckWithDefaultValues.before(job.getDuedate())).isTrue();
  }

  @TestTemplate
  void testScheduleJobForBatchWindowWithShortcutConfiguration() {
    ClockUtil.setCurrentTime(currentDate);
    processEngineConfiguration.setThursdayHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setThursdayHistoryCleanupBatchWindowEndTime("00:00");
    processEngineConfiguration.setSaturdayHistoryCleanupBatchWindowStartTime("23:00");
    processEngineConfiguration.setSaturdayHistoryCleanupBatchWindowEndTime("00:00");
    processEngineConfiguration.initHistoryCleanup();

    Job job = historyService.cleanUpHistoryAsync();

    if (startDateForCheckWithDefaultValues == null) {
      startDateForCheckWithDefaultValues = startDateForCheck;
    }
    if (endDateForCheckWithDefaultValues == null) {
      endDateForCheckWithDefaultValues = endDateForCheck;
    }

    assertThat(startDateForCheckWithDefaultValues.after(job.getDuedate())).isFalse(); // job due date is not before start date
    assertThat(endDateForCheckWithDefaultValues.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheckWithDefaultValues, -1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(startDateForCheckWithDefaultValues.after(job.getDuedate())).isFalse();
    assertThat(endDateForCheckWithDefaultValues.after(job.getDuedate())).isTrue();

    ClockUtil.setCurrentTime(DateUtils.addMinutes(endDateForCheckWithDefaultValues, 1));

    job = historyService.cleanUpHistoryAsync();

    assertThat(endDateForCheckWithDefaultValues.before(job.getDuedate())).isTrue();

    // reset configuration for test independence
    processEngineConfiguration.setSaturdayHistoryCleanupBatchWindowStartTime(null);
    processEngineConfiguration.setSaturdayHistoryCleanupBatchWindowEndTime(null);
    processEngineConfiguration.setThursdayHistoryCleanupBatchWindowStartTime(null);
    processEngineConfiguration.setThursdayHistoryCleanupBatchWindowEndTime(null);
    processEngineConfiguration.getHistoryCleanupBatchWindows().clear();
    processEngineConfiguration.initHistoryCleanup();
  }

}
