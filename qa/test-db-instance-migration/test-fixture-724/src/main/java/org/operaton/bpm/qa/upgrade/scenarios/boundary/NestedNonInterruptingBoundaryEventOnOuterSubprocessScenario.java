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
package org.operaton.bpm.qa.upgrade.scenarios.boundary;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author Thorben Lindhauer
 *
 */
public final class NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario {

  private NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario() {
  }

  @Deployment
  public static String deployTimerBoundary() {
    return "org/operaton/bpm/qa/upgrade/boundary/nestedNonInterruptingTimerBoundaryEventOnOuterSubprocess.bpmn20.xml";
  }

  @Deployment
  public static String deployMessageBoundary() {
    return "org/operaton/bpm/qa/upgrade/boundary/nestedNonInterruptingMessageBoundaryEventOnOuterSubprocess.bpmn20.xml";
  }

  @DescribesScenario("initMessage")
  @Times(7)
  public static ScenarioSetup initMessage() {
    return (engine, scenarioName) -> {
      engine
        .getRuntimeService()
        .startProcessInstanceByKey("NestedNonInterruptingMessageBoundaryEventOnOuterSubprocessScenario", scenarioName);

      engine.getRuntimeService().correlateMessage("BoundaryEventMessage", scenarioName);
    };
  }

  @DescribesScenario("initTimer")
  @Times(7)
  public static ScenarioSetup initTimer() {
    return (engine, scenarioName) -> {
      ProcessInstance instance = engine
        .getRuntimeService()
        .startProcessInstanceByKey("NestedNonInterruptingTimerBoundaryEventOnOuterSubprocessScenario", scenarioName);

      Job job = engine.getManagementService()
        .createJobQuery().processInstanceId(instance.getId()).singleResult();
      engine.getManagementService().executeJob(job.getId());
    };
  }
}
