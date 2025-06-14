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
package org.operaton.bpm.qa.rolling.update.scenarios.externalTask;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class ProcessWithExternalTaskScenario {
  public static final String PROCESS_DEF_KEY = "processWithExternalTask";
  public static final String EXTERNAL_TASK = "externalTask";
  public static final String EXTERNAL_TASK_TYPE = "external";
  public static final long LOCK_TIME = 5 * 60 * 1000;

  private ProcessWithExternalTaskScenario() {
  }

  /**
   * Deploy a process model, which contains an external task. The topic is
   * given via parameter so the test cases are independent.
   *
   * @param engine the engine which is used to deploy the instance
   * @param topicName the topic name for the external task
   */
  public static void deploy(ProcessEngine engine, String topicName) {
    BpmnModelInstance instance = Bpmn.createExecutableProcess(PROCESS_DEF_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask(EXTERNAL_TASK)
        .operatonType(EXTERNAL_TASK_TYPE)
        .operatonTopic(topicName)
        .endEvent()
        .done();

    engine.getRepositoryService().createDeployment()
        .addModelInstance(ProcessWithExternalTaskScenario.class.getSimpleName() + ".startProcessWithFetch.bpmn20.xml", instance)
        .deploy();
  }

  @DescribesScenario("init")
  @Times(1)
  public static ScenarioSetup startProcess() {
    return new ScenarioSetup() {

      @Override
      public void execute(ProcessEngine engine, String scenarioName) {
        deploy(engine, scenarioName);
        engine.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, scenarioName);
      }
    };
  }


  @DescribesScenario("init.fetch")
  @Times(1)
  public static ScenarioSetup startProcessWithFetch() {
    return new ScenarioSetup() {

      @Override
      public void execute(ProcessEngine engine, String scenarioName) {
        deploy(engine, scenarioName);

        engine.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, scenarioName);
        engine.getExternalTaskService().fetchAndLock(1, scenarioName)
                                       .topic(scenarioName, LOCK_TIME)
                                       .execute();
      }
    };
  }
}
