package org.operaton.bpm.qa.upgrade.scenarioscamunda.migration;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.JobExecutor;
import org.operaton.bpm.engine.CommandExecutor;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;

import org.operaton.camunda.migration.CamundaToOperatonMigrationTask;

import static org.assertj.core.api.Assertions.assertThat;

@Origin("7.24.0")
@ScenarioUnderTest("createCamundaFixturesDataScenario")
public class CamundaMigrationTest {

  @Rule
  public final UpgradeTestRule engineRule = new UpgradeTestRule();

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;
  HistoryService historyService;

  JobExecutor jobExecutor;
  CommandExecutor commandExecutor;

  ProcessEngineConfigurationImpl engineConfig;
  boolean isJobExecutorAcquireExclusiveOverProcessHierarchies;

  @Before
  public void init() {
    var engine = engineRule.getProcessEngine();
    var engineConfig = (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();

    this.runtimeService = engine.getRuntimeService();
    this.managementService = engine.getManagementService();
    this.historyService = engine.getHistoryService();
    this.repositoryService = engine.getRepositoryService();

    this.jobExecutor = engineConfig.getJobExecutor();
    this.commandExecutor = engineConfig.getCommandExecutorTxRequired();
    this.engineConfig = engineConfig;
  }

  @Test
  public void testProcessWithCamundaImportsMigratesSuccessfully() {
    engineConfig.setCamundaCompatibilityMode(ProcessEngineConfiguration.DB_CAMUNDA_COMPATIBILITY_MIGRATE_DATA);

    new SchemaOperationsProcessEngineBuild().execute(commandExecutor);

    var processDefinitions = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("processWithCamundaScriptTask")
        .latestVersion()
        .list();

    assertThat(processDefinitions).isNotNull();

    boolean isThereMigratedProcessDefinition = false;
    for(var definition : processDefinitions) {
      var bpmnModelInstance = repositorySerice.getBpmnModelInstance(definition.getId());

      var scriptTask = bpmnModelInstance.getDocument().getElementById("scriptTask");
      if(scriptTask != null && scriptTask.getTextContent() != null
          && !scriptTask.getTextContent().contains("org.camunda")
          && scriptTask.getTextContent().contains("org.operaton")) {
        isThereMigratedProcessDefinition = true;
      }
    }

    assertThat(isThereMigratedProcessDefinition).isTrue();
  }
}
