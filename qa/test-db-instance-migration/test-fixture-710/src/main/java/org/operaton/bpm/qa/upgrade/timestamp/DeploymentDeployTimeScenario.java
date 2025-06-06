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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author Nikola Koevski
 */
public class DeploymentDeployTimeScenario extends AbstractTimestampMigrationScenario {

  protected static final String PROCESS_DEFINITION_KEY = "deploymentDeployTimeProcess";
  protected static final String DEPLOYMENT_NAME = "DeployTimeDeploymentTest";

  protected static final BpmnModelInstance DEPLOYMENT_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .operatonHistoryTimeToLive(180)
      .startEvent("start")
    .serviceTask("task")
      .operatonDelegateExpression("${true}")
    .endEvent("end")
    .done();

  @DescribesScenario("initDeploymentDeployTime")
  @Times(1)
  public static ScenarioSetup initDeploymentDeployTime() {
    return new ScenarioSetup() {
      @Override
      public void execute(ProcessEngine processEngine, String s) {

        ClockUtil. setCurrentTime(TIMESTAMP);

        deployModel(processEngine, DEPLOYMENT_NAME, PROCESS_DEFINITION_KEY, DEPLOYMENT_MODEL);

        ClockUtil.reset();
      }
    };
  }
}
