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
 * @author Anna Pazola
 *
 */
@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoryCleanupBatchWindowForEveryDayTest {

  protected String defaultStartTime;
  protected String defaultEndTime;
  protected int defaultBatchSize;

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .cacheForConfigurationResource(false)
    .configurator(configuration -> {
      configuration.setHistoryCleanupBatchSize(20);
      configuration.setHistoryCleanupBatchThreshold(10);
      configuration.setDefaultNumberOfRetries(5);
    }).build();
  @RegisterExtension
  protected static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private HistoryService historyService;
  private ManagementService managementService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @Parameter(0)
  public String startTime;

  @Parameter(1)
  public String endTime;

  @Parameter(2)
  public Date startDateForCheck;

  @Parameter(3)
  public Date endDateForCheck;

  @Parameter(4)
  public Date currentDate;

  @Parameters
  public static Collection<Object[]> scenarios() throws ParseException {
    return Arrays.asList(new Object[][] {
        // inside the batch window on the same day
        { "22:00", "23:00", sdf.parse("2017-09-06T22:00:00"), sdf.parse("2017-09-06T23:00:00"), sdf.parse("2017-09-06T22:15:00")},
        // inside the batch window on the next day
        { "23:00", "01:00", sdf.parse("2017-09-06T23:00:00"), sdf.parse("2017-09-07T01:00:00"), sdf.parse("2017-09-07T00:15:00")},
        // batch window 24h
        { "00:00", "00:00", sdf.parse("2017-09-06T00:00:00"), sdf.parse("2017-09-07T00:00:00"), sdf.parse("2017-09-06T15:00:00")},
        // batch window 24h
        { "00:00", "00:00", sdf.parse("2017-09-06T00:00:00"), sdf.parse("2017-09-07T00:00:00"), sdf.parse("2017-09-06T00:00:00")},
        // before the batch window on the same day
        { "22:00", "23:00", sdf.parse("2017-09-06T22:00:00"), sdf.parse("2017-09-06T23:00:00"), sdf.parse("2017-09-06T21:15:00")},
        // after the batch window on the same day
        { "22:00", "23:00", sdf.parse("2017-09-07T22:00:00"), sdf.parse("2017-09-07T23:00:00"), sdf.parse("2017-09-06T23:15:00")},
        // after the batch window on the next day
        { "22:00", "23:00", sdf.parse("2017-09-07T22:00:00"), sdf.parse("2017-09-07T23:00:00"), sdf.parse("2017-09-07T00:15:00")} });
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
  public void testScheduleJobForBatchWindow() {
    ClockUtil.setCurrentTime(currentDate);

    processEngineConfiguration.setHistoryCleanupBatchWindowStartTime(startTime);
    processEngineConfiguration.setHistoryCleanupBatchWindowEndTime(endTime);

    processEngineConfiguration.initHistoryCleanup();

    Job job = historyService.cleanUpHistoryAsync();

    assertThat(startDateForCheck.after(job.getDuedate())).isFalse(); // job due date is not before start date
    assertThat(endDateForCheck.after(job.getDuedate())).isTrue();
  }
}
