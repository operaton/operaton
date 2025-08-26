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
package org.operaton.bpm.qa.upgrade.customretries;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

import static org.junit.Assert.fail;

public final class FailingIntermediateBoundaryTimerJobScenario {

  private FailingIntermediateBoundaryTimerJobScenario() {
  }

  @Deployment
  public static String deploy() {
    return "org/operaton/bpm/qa/upgrade/customretries/failingTimerJob.bpmn20.xml";
  }

  @DescribesScenario("failingTimerJob")
  public static ScenarioSetup createFailingTimerJob() {
    return new ScenarioSetup() {
      @Override
      public void execute(ProcessEngine engine, String scenarioName) {
        try {
          ManagementService managementService = engine.getManagementService();

          SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
          Date startDate = simpleDateFormat.parse("2019-01-01T10:00:00");
          ClockUtil.setCurrentTime(startDate);

          engine.getRuntimeService().startProcessInstanceByKey("failingTimer");

          ClockUtil.setCurrentTime(simpleDateFormat.parse("2019-01-01T11:00:01"));

          Job firstJob = managementService.createJobQuery().processDefinitionKey("failingTimer").singleResult();
          try {
            managementService.executeJob(firstJob.getId());
          } catch (Exception e) {
            // ignore
          }
        } catch (ParseException e) {
          fail("Unexpected Exception: " + e.getMessage());
        } finally {
          ClockUtil.reset();
        }
      }
    };
  }
}
