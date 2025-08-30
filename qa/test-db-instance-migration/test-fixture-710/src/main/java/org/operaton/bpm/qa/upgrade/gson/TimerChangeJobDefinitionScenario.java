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
package org.operaton.bpm.qa.upgrade.gson;

import java.util.Date;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

/**
 * @author Tassilo Weidner
 */
public final class TimerChangeJobDefinitionScenario {
  protected static final Date FIXED_DATE_ONE = new Date(1363607000000L);
  protected static final Date FIXED_DATE_TWO = new Date(1363607500000L);
  protected static final Date FIXED_DATE_THREE = new Date(1363607600000L);
  protected static final Date FIXED_DATE_FOUR = new Date(1363607700000L);

  private TimerChangeJobDefinitionScenario() {
  }

  @DescribesScenario("initTimerChangeJobDefinition")
  public static ScenarioSetup initTimerChangeJobDefinition() {
    return (engine, scenarioName) -> {

      String processDefinitionIdWithoutTenant = engine.getRepositoryService().createDeployment()
        .addClasspathResource("org/operaton/bpm/qa/upgrade/gson/oneTaskProcessTimerJob.bpmn20.xml")
        .deployWithResult()
        .getDeployedProcessDefinitions()
        .get(0)
        .getId();

      engine.getRuntimeService().startProcessInstanceByKey("oneTaskProcessTimerJob_710");

      Job job = engine.getManagementService().createJobQuery()
        .processDefinitionKey("oneTaskProcessTimerJob_710")
        .singleResult();

      engine.getManagementService()
        .updateJobDefinitionSuspensionState()
        .byJobDefinitionId(job.getJobDefinitionId())
        .includeJobs(true)
        .executionDate(FIXED_DATE_ONE)
        .suspend();

      engine.getManagementService()
        .updateJobDefinitionSuspensionState()
        .byProcessDefinitionId(processDefinitionIdWithoutTenant)
        .includeJobs(false)
        .executionDate(FIXED_DATE_TWO)
        .suspend();

      engine.getRepositoryService().createDeployment()
        .addClasspathResource("org/operaton/bpm/qa/upgrade/gson/oneTaskProcessTimerJob.bpmn20.xml")
        .tenantId("aTenantId")
        .deploy();

      engine.getManagementService()
        .updateJobDefinitionSuspensionState()
        .byProcessDefinitionKey("oneTaskProcessTimerJob_710")
        .processDefinitionTenantId("aTenantId")
        .includeJobs(false)
        .executionDate(FIXED_DATE_THREE)
        .suspend();

      engine.getManagementService()
        .updateJobDefinitionSuspensionState()
        .byProcessDefinitionKey("oneTaskProcessTimerJob_710")
        .processDefinitionWithoutTenantId()
        .includeJobs(false)
        .executionDate(FIXED_DATE_FOUR)
        .suspend();
    };
  }
}
