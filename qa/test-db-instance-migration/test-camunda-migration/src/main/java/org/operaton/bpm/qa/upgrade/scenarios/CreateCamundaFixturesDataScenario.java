package org.operaton.bpm.qa.upgrade.scenarios;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;
import org.operaton.bpm.qa.upgrade.Times;

/**
 * @author sevdan
 */
public class CreateCamundaFixturesDataScenario {

  private static final String PROCESS_DEF_KEY = "processWithCamundaScriptTask";

  private CreateCamundaFixturesDataScenario() {
  }

  @Deployment
  public static String deploy() {
    return "org/operaton/bpm/qa/upgrade/migrate/processWithCamundaScriptTask.bpmn20.xml";
  }

  @DescribesScenario("createCamundaFixturesDataScenario")
  public static ScenarioSetup createCamundaFixturesDataScenario() {
    return (engine, scenarioName) ->
        engine
            .getRuntimeService()
            .startProcessInstanceByKey(PROCESS_DEF_KEY, scenarioName);
  }

}
