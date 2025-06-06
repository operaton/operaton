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
package org.operaton.bpm.qa.upgrade;


import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.qa.upgrade.scenarios.boundary.NestedNonInterruptingBoundaryEventOnInnerSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.boundary.NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.boundary.NonInterruptingBoundaryEventScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.InterruptingEventSubprocessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SingleActivityCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessParallelCreateCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessParallelThrowCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.TransactionCancelCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.InterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NestedInterruptingErrorEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NestedInterruptingEventSubprocessParallelScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NestedNonInterruptingEventSubprocessNestedSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NestedNonInterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NestedParallelNonInterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.NonInterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.ParallelNestedNonInterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.eventsubprocess.TwoLevelNestedNonInterruptingEventSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.gateway.EventBasedGatewayScenario;
import org.operaton.bpm.qa.upgrade.scenarios.job.AsyncParallelMultiInstanceScenario;
import org.operaton.bpm.qa.upgrade.scenarios.job.AsyncSequentialMultiInstanceScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.MultiInstanceReceiveTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.NestedSequentialMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.ParallelMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.SequentialMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.histperms.HistoricInstancePermissionsWithoutProcDefKeyScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.OneScopeTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.OneTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.ParallelScopeTasksScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.ParallelTasksScenario;

/**
 * Sets up scenarios for migration from 7.3.0
 *
 * @author Thorben Lindhauer
 */
public class TestFixture {

  public static final String ENGINE_VERSION = "7.2.0";

  protected ProcessEngine processEngine;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected TaskService taskService;

  public TestFixture(ProcessEngine processEngine) {
    this.processEngine = processEngine;
    repositoryService = processEngine.getRepositoryService();
    runtimeService = processEngine.getRuntimeService();
    managementService = processEngine.getManagementService();
    taskService = processEngine.getTaskService();
  }

  public static void main(String[] args) {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("operaton.cfg.xml");
    ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

    // register test scenarios
    ScenarioRunner runner = new ScenarioRunner(processEngine, ENGINE_VERSION);

    // event subprocesses
    runner.setupScenarios(InterruptingEventSubprocessScenario.class);
    runner.setupScenarios(NonInterruptingEventSubprocessScenario.class);
    runner.setupScenarios(NestedNonInterruptingEventSubprocessScenario.class);
    runner.setupScenarios(ParallelNestedNonInterruptingEventSubprocessScenario.class);
    runner.setupScenarios(NestedParallelNonInterruptingEventSubprocessScenario.class);
    runner.setupScenarios(NestedNonInterruptingEventSubprocessNestedSubprocessScenario.class);
    runner.setupScenarios(NestedInterruptingErrorEventSubprocessScenario.class);
    runner.setupScenarios(TwoLevelNestedNonInterruptingEventSubprocessScenario.class);
    runner.setupScenarios(NestedInterruptingEventSubprocessParallelScenario.class);

    // multi instance
    runner.setupScenarios(SequentialMultiInstanceSubprocessScenario.class);
    runner.setupScenarios(NestedSequentialMultiInstanceSubprocessScenario.class);
    runner.setupScenarios(MultiInstanceReceiveTaskScenario.class);
    runner.setupScenarios(ParallelMultiInstanceSubprocessScenario.class);

    // async
    runner.setupScenarios(AsyncParallelMultiInstanceScenario.class);
    runner.setupScenarios(AsyncSequentialMultiInstanceScenario.class);

    // boundary event
    runner.setupScenarios(NonInterruptingBoundaryEventScenario.class);
    runner.setupScenarios(NestedNonInterruptingBoundaryEventOnInnerSubprocessScenario.class);
    runner.setupScenarios(NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario.class);

    // compensation
    runner.setupScenarios(SingleActivityCompensationScenario.class);
    runner.setupScenarios(SubprocessCompensationScenario.class);
    runner.setupScenarios(TransactionCancelCompensationScenario.class);
    runner.setupScenarios(InterruptingEventSubprocessCompensationScenario.class);
    runner.setupScenarios(SubprocessParallelThrowCompensationScenario.class);
    runner.setupScenarios(SubprocessParallelCreateCompensationScenario.class);

    // plain tasks
    runner.setupScenarios(OneTaskScenario.class);
    runner.setupScenarios(OneScopeTaskScenario.class);
    runner.setupScenarios(ParallelTasksScenario.class);
    runner.setupScenarios(ParallelScopeTasksScenario.class);

    // event-based gateway
    runner.setupScenarios(EventBasedGatewayScenario.class);

    runner.setupScenarios(HistoricInstancePermissionsWithoutProcDefKeyScenario.class);

    processEngine.close();
  }
}
