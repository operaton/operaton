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
package org.operaton.bpm.qa.upgrade.job;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

public class SetJobRetriesWithDueDateScenario {

  private SetJobRetriesWithDueDateScenario() {
  }

  @Deployment
  public static String deployOneTask() {
    return "org/operaton/bpm/qa/upgrade/job/SetJobRetriesWithDueDateScenario.oneTaskProcess.bpmn20.xml";
  }

  @DescribesScenario("createSetRetriesBatch")
  public static ScenarioSetup createSetRetriesBatch() {
    return (engine, scenarioName) -> {
      // services
      RuntimeService runtimeService = engine.getRuntimeService();
      RepositoryService repositoryService = engine.getRepositoryService();
      ManagementService managementService = engine.getManagementService();

      // definition
      ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
          .processDefinitionKey("createProcessForSetRetriesWithDueDate_718").singleResult();

      // create set retries batch
      List<String> instanceIds = new ArrayList<>();
      instanceIds.add(runtimeService.startProcessInstanceByKey(definition.getKey()).getId());

      Batch batch = managementService.setJobRetriesAsync(instanceIds, (ProcessInstanceQuery) null, 5);
      managementService.setProperty("SetJobRetriesWithDueDateTest.retries.batchId", batch.getId());
    };
  }
}
