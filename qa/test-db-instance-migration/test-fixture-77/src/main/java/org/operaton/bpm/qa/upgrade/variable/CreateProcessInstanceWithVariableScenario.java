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
package org.operaton.bpm.qa.upgrade.variable;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

public class CreateProcessInstanceWithVariableScenario {

  private CreateProcessInstanceWithVariableScenario() {
  }

  @Deployment
  public static String deployProcess() {
    return "org/operaton/bpm/qa/upgrade/variable/simpleProcess.bpmn20.xml";
  }

  @DescribesScenario("initProcessInstance")
  public static ScenarioSetup initProcessInstance() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {
        // given
        ProcessInstance processInstance = engine.getRuntimeService().startProcessInstanceByKey("Process", "process");
        // when
        Execution execution = engine.getRuntimeService().createExecutionQuery().processInstanceId(processInstance.getId()).singleResult();
        engine.getRuntimeService().setVariable(execution.getId(), "foo", "bar");
      }
    };
  }
}
