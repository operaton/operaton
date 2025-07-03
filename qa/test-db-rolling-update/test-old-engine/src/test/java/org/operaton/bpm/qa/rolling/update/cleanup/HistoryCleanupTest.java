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
package org.operaton.bpm.qa.rolling.update.cleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.rolling.update.RollingUpdateConstants;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tassilo Weidner
 */
@ScenarioUnderTest("HistoryCleanupScenario")
@Parameterized
class HistoryCleanupTest extends AbstractRollingUpdateTestCase {

  static final Date FIXED_DATE = new Date(1363608000000L);

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @TestTemplate
  @ScenarioUnderTest("initHistoryCleanup.1")
  void historyCleanup() {

    if (RollingUpdateConstants.OLD_ENGINE_TAG.equals(rule.getTag())) { // test cleanup with old engine

      Date currentDate = addDays(FIXED_DATE, 1);
      ClockUtil.setCurrentTime(currentDate);

      ProcessEngineConfigurationImpl configuration =
        rule.getProcessEngineConfiguration();

      configuration.setHistoryCleanupBatchWindowStartTime("13:00");
      configuration.setHistoryCleanupBatchWindowEndTime("15:00");
      configuration.setHistoryCleanupDegreeOfParallelism(3);
      configuration.initHistoryCleanup();

      List<Job> jobs = rule.getHistoryService().findHistoryCleanupJobs();

      Job jobOne = jobs.get(0);
      rule.getManagementService().executeJob(jobOne.getId());

      Job jobTwo = jobs.get(1);
      rule.getManagementService().executeJob(jobTwo.getId());

      Job jobThree = jobs.get(2);
      rule.getManagementService().executeJob(jobThree.getId());

      jobs = rule.getHistoryService().findHistoryCleanupJobs();

      // assume
      for (Job job : jobs) {
        assertThat(job.getDuedate()).isEqualTo(addSeconds(currentDate, (int)(Math.pow(2., (double)4) * 10)));
      }

      List<HistoricProcessInstance> processInstances = rule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey("HistoryCleanupScenario")
        .list();

      // assume
      assertThat(jobs).hasSize(3);
      assertThat(processInstances).hasSize(15);

      ClockUtil.setCurrentTime(addDays(currentDate, 5));

      // when
      rule.getManagementService().executeJob(jobOne.getId());

      processInstances = rule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey("HistoryCleanupScenario")
        .list();

      // then
      assertThat(processInstances).hasSize(10);

      // when
      rule.getManagementService().executeJob(jobTwo.getId());

      processInstances = rule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey("HistoryCleanupScenario")
        .list();

      // then
      assertThat(processInstances).hasSize(5);

      // when
      rule.getManagementService().executeJob(jobThree.getId());

      processInstances = rule.getHistoryService()
        .createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey("HistoryCleanupScenario")
        .list();

      // then
      assertThat(processInstances).isEmpty();
    }
  }

  protected Date addSeconds(Date initialDate, int seconds) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(initialDate);
    calendar.add(Calendar.SECOND, seconds);
    return calendar.getTime();
  }

  protected Date addDays(Date initialDate, int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(initialDate);
    calendar.add(Calendar.DAY_OF_MONTH, days);
    return calendar.getTime();
  }

}
