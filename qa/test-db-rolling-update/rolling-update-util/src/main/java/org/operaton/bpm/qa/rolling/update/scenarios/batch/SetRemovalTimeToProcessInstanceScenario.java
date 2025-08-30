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
package org.operaton.bpm.qa.rolling.update.scenarios.batch;

import java.util.Date;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

public final class SetRemovalTimeToProcessInstanceScenario {

  public static final String PROCESS_DEF_KEY = "oneTaskProcess";

  private SetRemovalTimeToProcessInstanceScenario() {
  }

  @Deployment
  public static String deploy() {
    return "org/operaton/bpm/qa/rolling/update/oneTaskProcess.bpmn20.xml";
  }

  @DescribesScenario("createSetRemovalTimeToProcessInstanceBatch")
  @Times(1)
  public static ScenarioSetup createBatch() {
    return (engine, scenarioName) -> {
      Date removalTime = new Date(1363609000000L);
      String processInstanceId = engine.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, "SetRemovalTimeToProcessInstance.batch").getId();
      Batch batch = engine.getHistoryService().setRemovalTimeToHistoricProcessInstances().absoluteRemovalTime(removalTime).byIds(processInstanceId).executeAsync();
      engine.getManagementService().setProperty("SetRemovalTimeToProcessInstance.batch.batchId", batch.getId());
    };
  }

  @DescribesScenario("createSetRemovalTimeToProcessInstanceBatchJob")
  @Times(1)
  public static ScenarioSetup createBatchJob() {
    return (engine, scenarioName) -> {
      Date removalTime = new Date(1363609000000L);
      String processInstanceId = engine.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, "SetRemovalTimeToProcessInstance.batchJob").getId();
      Batch batch = engine.getHistoryService().setRemovalTimeToHistoricProcessInstances().absoluteRemovalTime(removalTime).byIds(processInstanceId).executeAsync();
      String seedJobDefinitionId = batch.getSeedJobDefinitionId();
      Job seedJob = engine.getManagementService().createJobQuery().jobDefinitionId(seedJobDefinitionId).singleResult();

      engine.getManagementService().executeJob(seedJob.getId());
      engine.getManagementService().setProperty("SetRemovalTimeToProcessInstance.batchJob.batchId", batch.getId());
    };
  }
}
