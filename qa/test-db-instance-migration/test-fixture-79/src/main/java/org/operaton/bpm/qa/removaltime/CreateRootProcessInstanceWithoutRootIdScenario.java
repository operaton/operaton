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
package org.operaton.bpm.qa.removaltime;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

/**
 * @author Tassilo Weidner
 */
public final class CreateRootProcessInstanceWithoutRootIdScenario {

  private CreateRootProcessInstanceWithoutRootIdScenario() {
  }

  @DescribesScenario("initRootProcessInstanceWithoutRootId")
  public static ScenarioSetup initRootProcessInstance() {
    return (engine, scenarioName) -> {

      engine.getRepositoryService().createDeployment()
        .addModelInstance("process.bpmn", Bpmn.createExecutableProcess("process")
            .operatonHistoryTimeToLive(180)
          .startEvent()
          .userTask()
          .endEvent().done())
        .deploy();

      engine.getRepositoryService().createDeployment()
        .addModelInstance("rootProcess.bpmn", Bpmn.createExecutableProcess("rootProcess")
            .operatonHistoryTimeToLive(180)
          .startEvent()
          .callActivity()
            .calledElement("process")
          .endEvent().done())
        .deploy();

      engine.getRuntimeService().startProcessInstanceByKey("rootProcess", "rootProcessInstance");
    };
  }
}
