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
package org.operaton.bpm.qa.upgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.operaton.bpm.qa.upgrade.scenarios720.boundary.NonInterruptingBoundaryEventScenario;
import org.operaton.bpm.qa.upgrade.scenarios720.compensation.InterruptingEventSubprocessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.boundary.NestedNonInterruptingBoundaryEventOnInnerSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.boundary.NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.InterruptingEventSubProcessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.InterruptingEventSubProcessNestedCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.NestedCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.NestedMultiInstanceCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.NonInterruptingEventSubProcessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.ParallelMultiInstanceCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SequentialMultiInstanceCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SingleActivityCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SingleActivityConcurrentCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessParallelCreateCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.SubprocessParallelThrowCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.compensation.TransactionCancelCompensationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.deployment.DeployProcessWithoutIsExecutableAttributeScenario;
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
import org.operaton.bpm.qa.upgrade.scenarios.histperms.HistoricInstancePermissionsWithoutProcDefKeyScenario;
import org.operaton.bpm.qa.upgrade.scenarios.job.AsyncParallelMultiInstanceScenario;
import org.operaton.bpm.qa.upgrade.scenarios.job.AsyncSequentialMultiInstanceScenario;
import org.operaton.bpm.qa.upgrade.scenarios.job.JobMigrationScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.MultiInstanceReceiveTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.NestedSequentialMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.ParallelMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.multiinstance.SequentialMultiInstanceSubprocessScenario;
import org.operaton.bpm.qa.upgrade.scenarios.sentry.SentryScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.OneScopeTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.OneTaskScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.ParallelScopeTasksScenario;
import org.operaton.bpm.qa.upgrade.scenarios.task.ParallelTasksScenario;
import org.operaton.bpm.qa.upgrade.user.creation.DeployUserWithoutSaltForPasswordHashingScenario;
import org.operaton.bpm.qa.upgrade.variable.CreateProcessInstanceWithVariableScenario;
import org.operaton.bpm.qa.upgrade.variable.EmptyStringVariableScenario;
import org.operaton.bpm.qa.upgrade.json.CreateProcessInstanceWithJsonVariablesScenario;
import org.operaton.bpm.qa.upgrade.removaltime.CreateRootProcessInstanceWithoutRootIdScenario;
import org.operaton.bpm.qa.upgrade.gson.ProcessInstanceModificationScenario;
import org.operaton.bpm.qa.upgrade.gson.TaskFilterPropertiesScenario;
import org.operaton.bpm.qa.upgrade.gson.TaskFilterScenario;
import org.operaton.bpm.qa.upgrade.gson.TaskFilterVariablesScenario;
import org.operaton.bpm.qa.upgrade.gson.TimerChangeJobDefinitionScenario;
import org.operaton.bpm.qa.upgrade.gson.TimerChangeProcessDefinitionScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.DeleteHistoricDecisionsBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.DeleteHistoricProcessInstancesBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.DeleteProcessInstancesBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.MigrationBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.ModificationBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.RestartProcessInstanceBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.SetExternalTaskRetriesBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.SetJobRetriesBatchScenario;
import org.operaton.bpm.qa.upgrade.gson.batch.UpdateProcessInstanceSuspendStateBatchScenario;
import org.operaton.bpm.qa.upgrade.timestamp.DeploymentDeployTimeScenario;
import org.operaton.bpm.qa.upgrade.timestamp.EventSubscriptionCreateTimeScenario;
import org.operaton.bpm.qa.upgrade.timestamp.ExternalTaskLockExpTimeScenario;
import org.operaton.bpm.qa.upgrade.timestamp.IncidentTimestampScenario;
import org.operaton.bpm.qa.upgrade.timestamp.JobTimestampsScenario;
import org.operaton.bpm.qa.upgrade.timestamp.MeterLogTimestampScenario;
import org.operaton.bpm.qa.upgrade.timestamp.TaskCreateTimeScenario;
import org.operaton.bpm.qa.upgrade.timestamp.UserLockExpTimeScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.CreateStandaloneTaskDeleteScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.CreateStandaloneTaskScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.SetAssigneeProcessInstanceTaskScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.SuspendProcessDefinitionDeleteScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.annotation.AuthorizationCheckProcessDefinitionScenario;
import org.operaton.bpm.qa.upgrade.useroperationlog.annotation.NoAuthorizationCheckScenario;
import org.operaton.bpm.qa.upgrade.batch.deploymentaware.DeploymentAwareBatchesScenario;
import org.operaton.bpm.qa.upgrade.customretries.FailingIntermediateBoundaryTimerJobScenario;
import org.operaton.bpm.qa.upgrade.restart.SetVariablesScenario;
import org.operaton.bpm.qa.upgrade.restart.StartProcessIntanceWithInitialVariablesScenario;
import org.operaton.bpm.qa.upgrade.batch.CreateSetProcessInstanceVariablesBatchScenario;
import org.operaton.bpm.qa.upgrade.migration.CreateSetVariablesMigrationBatchScenario;
import org.operaton.bpm.qa.upgrade.externaltask.ExternalTaskFailureLogScenario;
import org.operaton.bpm.qa.upgrade.job.JobFailureLogScenario;
import org.operaton.bpm.qa.upgrade.pvm.AsyncJoinScenario;
import org.operaton.bpm.qa.upgrade.job.SetJobRetriesWithDueDateScenario;
import org.operaton.bpm.qa.upgrade.batch.SetRemovalTimeToProcessInstanceScenario;
import org.operaton.bpm.qa.upgrade.httl.EnforceHistoryTimeToLiveScenario;
import org.operaton.bpm.qa.upgrade.jobexecutor.ExclusiveOverProcessHierarchiesScenario;

public class TestFixture {

  private static final String ENGINE_VERSION = "7.24.0";
  private static final Logger LOG = Logger.getLogger("TestFixture-" + ENGINE_VERSION);

  public TestFixture(ProcessEngine processEngine) {
  }

  public static void main(String... args) throws Exception {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("operaton.cfg.xml");

    String changelogDir = System.getProperty("liquibase.changelog.dir");
    if (changelogDir != null) {
      runLiquibase(
        processEngineConfiguration.getJdbcUrl(),
        processEngineConfiguration.getJdbcUsername(),
        processEngineConfiguration.getJdbcPassword(),
        changelogDir);
    }

    ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

    ScenarioRunner runner = new ScenarioRunner(processEngine, ENGINE_VERSION);

    for (Class<?> scenarioClass : getScenarios()) {
      System.out.println("Running scenario " + scenarioClass.getSimpleName());
      runner.setupScenarios(scenarioClass);
    }

    processEngine.close();
  }

  private static void runLiquibase(String url, String username, String password, String changelogDir)
      throws Exception {
    try (Connection conn = DriverManager.getConnection(url, username, password)) {
      Database db = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(conn));
      try {
        new Liquibase("operaton-changelog.xml",
          new FileSystemResourceAccessor(new File(changelogDir)), db)
          .update(new Contexts(), new LabelExpression());
      } finally {
        db.close();
      }
    }
  }

  private static List<Class<?>> getScenarios() {
    List<Class<?>> scenarios = new ArrayList<>();

    // fixture-72 scenarios (version 7.2.0)
    scenarios.add(InterruptingEventSubprocessScenario.class);
    scenarios.add(NonInterruptingEventSubprocessScenario.class);
    scenarios.add(NestedNonInterruptingEventSubprocessScenario.class);
    scenarios.add(ParallelNestedNonInterruptingEventSubprocessScenario.class);
    scenarios.add(NestedParallelNonInterruptingEventSubprocessScenario.class);
    scenarios.add(NestedNonInterruptingEventSubprocessNestedSubprocessScenario.class);
    scenarios.add(NestedInterruptingErrorEventSubprocessScenario.class);
    scenarios.add(TwoLevelNestedNonInterruptingEventSubprocessScenario.class);
    scenarios.add(NestedInterruptingEventSubprocessParallelScenario.class);
    scenarios.add(SequentialMultiInstanceSubprocessScenario.class);
    scenarios.add(NestedSequentialMultiInstanceSubprocessScenario.class);
    scenarios.add(MultiInstanceReceiveTaskScenario.class);
    scenarios.add(ParallelMultiInstanceSubprocessScenario.class);
    scenarios.add(AsyncParallelMultiInstanceScenario.class);
    scenarios.add(AsyncSequentialMultiInstanceScenario.class);
    scenarios.add(NonInterruptingBoundaryEventScenario.class);
    scenarios.add(NestedNonInterruptingBoundaryEventOnInnerSubprocessScenario.class);
    scenarios.add(NestedNonInterruptingBoundaryEventOnOuterSubprocessScenario.class);
    scenarios.add(SingleActivityCompensationScenario.class);
    scenarios.add(SubprocessCompensationScenario.class);
    scenarios.add(TransactionCancelCompensationScenario.class);
    scenarios.add(InterruptingEventSubprocessCompensationScenario.class);
    scenarios.add(SubprocessParallelThrowCompensationScenario.class);
    scenarios.add(SubprocessParallelCreateCompensationScenario.class);
    scenarios.add(OneTaskScenario.class);
    scenarios.add(OneScopeTaskScenario.class);
    scenarios.add(ParallelTasksScenario.class);
    scenarios.add(ParallelScopeTasksScenario.class);
    scenarios.add(EventBasedGatewayScenario.class);
    scenarios.add(HistoricInstancePermissionsWithoutProcDefKeyScenario.class);

    // fixture-73 scenarios (version 7.3.0)
    scenarios.add(SentryScenario.class);
    scenarios.add(NestedCompensationScenario.class);
    scenarios.add(SingleActivityConcurrentCompensationScenario.class);
    scenarios.add(ParallelMultiInstanceCompensationScenario.class);
    scenarios.add(SequentialMultiInstanceCompensationScenario.class);
    scenarios.add(NestedMultiInstanceCompensationScenario.class);
    scenarios.add(InterruptingEventSubProcessCompensationScenario.class);
    scenarios.add(NonInterruptingEventSubProcessCompensationScenario.class);
    scenarios.add(InterruptingEventSubProcessNestedCompensationScenario.class);
    scenarios.add(JobMigrationScenario.class);
    scenarios.add(org.operaton.bpm.qa.upgrade.scenarios730.boundary.NonInterruptingBoundaryEventScenario.class);

    // fixture-75 scenarios (version 7.5.0)
    scenarios.add(DeployProcessWithoutIsExecutableAttributeScenario.class);

    // fixture-76 scenarios (version 7.6.0)
    scenarios.add(DeployUserWithoutSaltForPasswordHashingScenario.class);

    // fixture-77 scenarios (version 7.7.0)
    scenarios.add(CreateProcessInstanceWithVariableScenario.class);

    // fixture-78 scenarios (version 7.8.0)
    scenarios.add(org.operaton.bpm.qa.upgrade.json78.CreateProcessInstanceWithJsonVariablesScenario.class);

    // fixture-79 scenarios (version 7.9.0)
    scenarios.add(CreateProcessInstanceWithJsonVariablesScenario.class);
    scenarios.add(CreateRootProcessInstanceWithoutRootIdScenario.class);

    // fixture-710 scenarios (version 7.10.0)
    scenarios.add(DeleteHistoricDecisionsBatchScenario.class);
    scenarios.add(DeleteHistoricProcessInstancesBatchScenario.class);
    scenarios.add(DeleteProcessInstancesBatchScenario.class);
    scenarios.add(SetExternalTaskRetriesBatchScenario.class);
    scenarios.add(SetJobRetriesBatchScenario.class);
    scenarios.add(UpdateProcessInstanceSuspendStateBatchScenario.class);
    scenarios.add(RestartProcessInstanceBatchScenario.class);
    scenarios.add(TimerChangeProcessDefinitionScenario.class);
    scenarios.add(TimerChangeJobDefinitionScenario.class);
    scenarios.add(ModificationBatchScenario.class);
    scenarios.add(ProcessInstanceModificationScenario.class);
    scenarios.add(MigrationBatchScenario.class);
    scenarios.add(TaskFilterScenario.class);
    scenarios.add(TaskFilterVariablesScenario.class);
    scenarios.add(TaskFilterPropertiesScenario.class);
    scenarios.add(DeploymentDeployTimeScenario.class);
    scenarios.add(JobTimestampsScenario.class);
    scenarios.add(IncidentTimestampScenario.class);
    scenarios.add(TaskCreateTimeScenario.class);
    scenarios.add(ExternalTaskLockExpTimeScenario.class);
    scenarios.add(EventSubscriptionCreateTimeScenario.class);
    scenarios.add(MeterLogTimestampScenario.class);
    scenarios.add(UserLockExpTimeScenario.class);
    scenarios.add(CreateStandaloneTaskScenario.class);
    scenarios.add(SetAssigneeProcessInstanceTaskScenario.class);
    scenarios.add(CreateStandaloneTaskDeleteScenario.class);
    scenarios.add(SuspendProcessDefinitionDeleteScenario.class);
    scenarios.add(AuthorizationCheckProcessDefinitionScenario.class);
    scenarios.add(NoAuthorizationCheckScenario.class);

    // fixture-712 scenarios (version 7.12.0)
    scenarios.add(FailingIntermediateBoundaryTimerJobScenario.class);
    scenarios.add(DeploymentAwareBatchesScenario.class);
    scenarios.add(StartProcessIntanceWithInitialVariablesScenario.class);
    scenarios.add(SetVariablesScenario.class);

    // fixture-714 scenarios (version 7.14.0)
    scenarios.add(EmptyStringVariableScenario.class);

    // fixture-715 scenarios (version 7.15.0)
    scenarios.add(CreateSetVariablesMigrationBatchScenario.class);
    scenarios.add(CreateSetProcessInstanceVariablesBatchScenario.class);

    // fixture-716 scenarios (version 7.16.0)
    scenarios.add(ExternalTaskFailureLogScenario.class);
    scenarios.add(JobFailureLogScenario.class);
    scenarios.add(AsyncJoinScenario.class);

    // fixture-718 scenarios (version 7.18.0)
    scenarios.add(SetJobRetriesWithDueDateScenario.class);

    // fixture-719 scenarios (version 7.19.0)
    scenarios.add(EnforceHistoryTimeToLiveScenario.class);
    scenarios.add(SetRemovalTimeToProcessInstanceScenario.class);

    // fixture-720 scenarios (version 7.20.0)
    scenarios.add(ExclusiveOverProcessHierarchiesScenario.class);

    return scenarios;
  }
}
