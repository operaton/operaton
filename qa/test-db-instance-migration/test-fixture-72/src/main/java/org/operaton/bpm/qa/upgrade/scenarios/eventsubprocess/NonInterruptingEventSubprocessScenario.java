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
package org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess;

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
public class NonInterruptingEventSubprocessScenario {

  private NonInterruptingEventSubprocessScenario() {
  }

  @Deployment
  public static String deployProcess() {
    return "org/operaton/bpm/qa/upgrade/eventsubprocess/nonInterruptingMessageEventSubprocess.bpmn20.xml";
  }

  @DescribesScenario("init")
  @Times(4)
  public static ScenarioSetup instantiateAndTriggerSubprocess() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {
        engine
          .getRuntimeService()
          .startProcessInstanceByKey("NonInterruptingEventSubprocessScenario", scenarioName);

        engine.getRuntimeService()
          .createMessageCorrelation("Message")
          .processInstanceBusinessKey(scenarioName)
          .correlate();
      }
    };
  }

  @DescribesScenario("init.outerTask")
  @ExtendsScenario("init")
  @Times(3)
  public static ScenarioSetup completeSubprocessTask() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {
        Task task = engine
          .getTaskService()
          .createTaskQuery()
          .processInstanceBusinessKey(scenarioName)
          .taskDefinitionKey("outerTask")
          .singleResult();

        engine.getTaskService().complete(task.getId());
      }
    };
  }
}
