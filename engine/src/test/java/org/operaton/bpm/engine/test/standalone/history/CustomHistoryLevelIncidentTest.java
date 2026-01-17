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
package org.operaton.bpm.engine.test.standalone.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.FailingDelegate;
import org.operaton.bpm.engine.test.api.runtime.migration.batch.BatchMigrationHelper;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_END_TIME_BASED;
import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class CustomHistoryLevelIncidentTest {

  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
      new Object[]{ List.of(HistoryEventTypes.INCIDENT_CREATE) },
      new Object[]{ List.of(HistoryEventTypes.INCIDENT_CREATE, HistoryEventTypes.INCIDENT_RESOLVE) },
      new Object[]{ List.of(HistoryEventTypes.INCIDENT_DELETE, HistoryEventTypes.INCIDENT_CREATE, HistoryEventTypes.INCIDENT_MIGRATE, HistoryEventTypes.INCIDENT_RESOLVE) }
    });
  }

  @Parameter(0)
  public static List<HistoryEventTypes> eventTypes;

  static CustomHistoryLevelIncident customHistoryLevelIncident = new CustomHistoryLevelIncident(eventTypes);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(cfg -> {
      cfg.setJdbcUrl("jdbc:h2:mem:" + CustomHistoryLevelIncident.class.getSimpleName());
      List<HistoryLevel> levels = new ArrayList<>();
      levels.add(customHistoryLevelIncident);
      cfg.setCustomHistoryLevels(levels);
      cfg.setHistory("aCustomHistoryLevelIncident");
      cfg.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_CREATE_DROP);
      cfg.setProcessEngineName("someRandomProcessEngineName");
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  MigrationTestExtension migrationRule = new MigrationTestExtension(engineRule);
  BatchMigrationHelper migrationHelper = new BatchMigrationHelper(engineRule, migrationRule);

  HistoryService historyService;
  RuntimeService runtimeService;
  ManagementService managementService;
  RepositoryService repositoryService;
  TaskService taskService;
  ProcessEngineConfigurationImpl configuration;

  DeploymentWithDefinitions deployment;

  public static final String PROCESS_DEFINITION_KEY = "oneFailingServiceTaskProcess";
  public static final BpmnModelInstance FAILING_SERVICE_TASK_MODEL  = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
    .startEvent("start")
    .serviceTask("task")
      .operatonAsyncBefore()
      .operatonClass(FailingDelegate.class.getName())
    .endEvent("end")
    .done();

  @BeforeEach
  void setUp() {
    customHistoryLevelIncident.setEventTypes(eventTypes);
    configuration.setHistoryCleanupStrategy(HISTORY_CLEANUP_STRATEGY_END_TIME_BASED);
  }

  @AfterEach
  void tearDown() {
    customHistoryLevelIncident.setEventTypes(null);
    if (deployment != null) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
    migrationHelper.removeAllRunningAndHistoricBatches();

    configuration.getCommandExecutorTxRequired().execute(commandContext -> {

      List<Job> jobs = managementService.createJobQuery().list();
      for (Job job : jobs) {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
      }

      List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();
      for (HistoricIncident historicIncident : historicIncidents) {
        commandContext.getDbEntityManager().delete((HistoricIncidentEntity) historicIncident);
      }

      commandContext.getMeterLogManager().deleteAll();

      return null;
    });
  }

  @TestTemplate
  void testDeleteHistoricIncidentByProcDefId() {
    // given
    deployment = repositoryService.createDeployment().addModelInstance("process.bpmn", FAILING_SERVICE_TASK_MODEL).deployWithResult();
    String processDefinitionId = deployment.getDeployedProcessDefinitions().get(0).getId();

    runtimeService.startProcessInstanceById(processDefinitionId);
    executeAvailableJobs();


    if (eventTypes != null) {
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey(PROCESS_DEFINITION_KEY)
      .cascade()
      .delete();

    // then
    List<HistoricIncident> incidents = historyService.createHistoricIncidentQuery().list();
    assertThat(incidents).isEmpty();
  }

  @TestTemplate
  void testDeleteHistoricIncidentByBatchId() {
    // given
    initBatchOperationHistoryTimeToLive();
    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), -11));

    BatchEntity batch = (BatchEntity) createFailingMigrationBatch();

    migrationHelper.completeSeedJobs(batch);

    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      if ("instance-migration".equals(((JobEntity) job).getJobHandlerType())) {
        managementService.setJobRetries(job.getId(), 1);
      }
    }
    migrationHelper.executeJobs(batch);

    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), -10));
    managementService.deleteBatch(batch.getId(), false);
    ClockUtil.setCurrentTime(new Date());

    // assume
    if (eventTypes != null) {
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
    }

    // when
    historyService.cleanUpHistoryAsync(true);
    for (Job job : historyService.findHistoryCleanupJobs()) {
      managementService.executeJob(job.getId());
    }

    // then
    List<HistoricIncident> incidents = historyService.createHistoricIncidentQuery().list();
    assertThat(incidents).isEmpty();
  }

  @TestTemplate
  void testDeleteHistoricIncidentByJobDefinitionId() {
    // given
    BatchEntity batch = (BatchEntity) createFailingMigrationBatch();

    migrationHelper.completeSeedJobs(batch);

    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      if ("instance-migration".equals(((JobEntity) job).getJobHandlerType())) {
        managementService.setJobRetries(job.getId(), 1);
      }
    }
    migrationHelper.executeJobs(batch);

    // assume
    if (eventTypes != null) {
      HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
      assertThat(historicIncident).isNotNull();
    }

    // when
    managementService.deleteBatch(batch.getId(), true);

    // then
    List<HistoricIncident> incidents = historyService.createHistoricIncidentQuery().list();
    assertThat(incidents).isEmpty();
  }

  protected void executeAvailableJobs() {
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();

    if (jobs.isEmpty()) {
      return;
    }

    for (Job job : jobs) {
      executeJobIgnoringException(managementService, job.getId());
    }

    executeAvailableJobs();
  }

  protected void initBatchOperationHistoryTimeToLive() {
    configuration.setBatchOperationHistoryTimeToLive("P0D");
    configuration.initHistoryCleanup();
  }

  protected Batch createFailingMigrationBatch() {
    BpmnModelInstance instance = createModelInstance();

    ProcessDefinition sourceProcessDefinition = migrationRule.deployAndGetDefinition(instance);
    ProcessDefinition targetProcessDefinition = migrationRule.deployAndGetDefinition(instance);

    MigrationPlan migrationPlan = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstance processInstance = runtimeService.startProcessInstanceById(sourceProcessDefinition.getId());

    return runtimeService.newMigration(migrationPlan).processInstanceIds(List.of(processInstance.getId(), "unknownId")).executeAsync();
  }

  protected BpmnModelInstance createModelInstance() {
    return Bpmn.createExecutableProcess("process")
        .startEvent("start")
        .userTask("userTask1")
        .endEvent("end")
        .done();
  }
}
