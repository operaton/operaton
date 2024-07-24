/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.qa.rolling.update.scenarios.task;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * Starts the process with a user task on the old engine.
 *
 * @author Christopher Zell <christopher.zell@operaton.com>
 */
public class ProcessWithUserTaskAndTimerScenario {

  public static final String PROCESS_DEF_KEY = "processWithUserTaskAndTimer";

  @Deployment
  public static String deploy() {
    return "org/operaton/bpm/qa/rolling/update/processWithUserTaskAndTimer.bpmn20.xml";
  }

  @DescribesScenario("init")
  @Times(1)
  public static ScenarioSetup startProcess() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {
        engine.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, scenarioName);
      }
    };
  }
}
