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
package org.operaton.bpm.qa.upgrade.timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author Nikola Koevski
 */
public class JobTimestampsScenario extends AbstractTimestampMigrationScenario {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
  protected static final Date LOCK_EXP_TIME = new Date(TIME + 300_000L);
  protected static final String PROCESS_DEFINITION_KEY = "jobTimestampsMigrationTestProcess";
  protected static final BpmnModelInstance SINGLE_JOB_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent("start")
    .intermediateCatchEvent("catch")
      .timerWithDate(Instant.ofEpochMilli(TIMESTAMP.getTime())
          .atZone(ZoneId.systemDefault())
          .format(DATE_FORMATTER))
    .endEvent("end")
    .done();

  @DescribesScenario("initJobTimestamps")
  @Times(1)
  public static ScenarioSetup initJobTimestamps() {
    return (processEngine, scenarioName) -> {

      ClockUtil.setCurrentTime(TIMESTAMP);

      deployModel(processEngine, PROCESS_DEFINITION_KEY, PROCESS_DEFINITION_KEY, SINGLE_JOB_MODEL);

      final String processInstanceId = processEngine.getRuntimeService()
        .startProcessInstanceByKey(PROCESS_DEFINITION_KEY, scenarioName)
        .getId();

      ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration())
        .getCommandExecutorTxRequired()
        .execute(new Command<Void>() {
          @Override
          public Void execute(CommandContext commandContext) {

            JobEntity job = (JobEntity) processEngine.getManagementService()
              .createJobQuery()
              .processInstanceId(processInstanceId)
              .singleResult();

            job.setLockExpirationTime(LOCK_EXP_TIME);

            commandContext.getJobManager()
              .updateJob(job);

            return null;
          }
        });

      ClockUtil.reset();
    };
  }
}
