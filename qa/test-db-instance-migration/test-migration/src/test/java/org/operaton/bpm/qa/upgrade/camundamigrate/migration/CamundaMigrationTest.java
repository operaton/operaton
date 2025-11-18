package org.operaton.bpm.qa.upgrade.camundamigrate.migration;

import org.operaton.camunda.migration.CamundaToOperatonMigrationTask;

@Origin("7.24.0")
public class CamundaMigrationTest {

  @Rule
  public final UpgradeTestRule engineRule = new UpgradeTestRule();

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

    this.jobExecutor = engineConfig.getJobExecutor();
    this.commandExecutor = engineConfig.getCommandExecutorTxRequired();
    this.engineConfig = engineConfig;

    storeOriginalStateBeforeTest(engine);
  }

  @After
  public void tearDown() {
    restoreOriginalStateAfterTest();
  }

  @Test
  @ScenarioUnderTest("createCamundaFixturesDataScenario")
  public void test() {
    new CamundaToOperatonMigrationTask().migrate();
  }

  protected void storeOriginalStateBeforeTest(ProcessEngine engine) {
    this.isJobExecutorAcquireExclusiveOverProcessHierarchies = engineConfig.isJobExecutorAcquireExclusiveOverProcessHierarchies();
  }

  protected void restoreOriginalStateAfterTest() {
    this.engineConfig.setJobExecutorAcquireExclusiveOverProcessHierarchies(isJobExecutorAcquireExclusiveOverProcessHierarchies);
  }
}
