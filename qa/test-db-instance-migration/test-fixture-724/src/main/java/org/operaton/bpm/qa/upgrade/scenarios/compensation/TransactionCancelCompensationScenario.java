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
package org.operaton.bpm.qa.upgrade.scenarios.compensation;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ExtendsScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author Thorben Lindhauer
 *
 */
public final class TransactionCancelCompensationScenario {

  private TransactionCancelCompensationScenario() {
  }

  @Deployment
  public static String deployProcess() {
    return "org/operaton/bpm/qa/upgrade/compensation/transactionCancelCompensationProcess.bpmn20.xml";
  }

  @DescribesScenario("init")
  @Times(3)
  public static ScenarioSetup instantiate() {
    return (engine, scenarioName) -> {
      engine
        .getRuntimeService()
        .startProcessInstanceByKey("TransactionCancelCompensationScenario", scenarioName);

      // create the compensation event subscription and wait before throwing cancel end event
      Task userTask = engine.getTaskService().createTaskQuery()
        .processInstanceBusinessKey(scenarioName).singleResult();
      engine.getTaskService().complete(userTask.getId());
    };
  }

  @DescribesScenario("init.triggerCompensation")
  @ExtendsScenario("init")
  @Times(3)
  public static ScenarioSetup instantiateAndTriggerCompensation() {
    return (engine, scenarioName) -> {
      // throw compensation by executing the cancel end event;
      // the compensation handler for userTask should then be active
      Task beforeCancelTask = engine.getTaskService().createTaskQuery()
        .processInstanceBusinessKey(scenarioName).singleResult();
      engine.getTaskService().complete(beforeCancelTask.getId());
    };
  }
}
