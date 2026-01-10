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

import org.junit.runner.Description;

import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.runtime.ExecutionQuery;
import org.operaton.bpm.engine.runtime.IncidentQuery;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.MessageCorrelationBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.ProcessEngineRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public class UpgradeTestRule extends ProcessEngineRule {

  protected String scenarioTestedByClass;
  protected String scenarioName;
  protected String tag;

  public UpgradeTestRule() {
    super("operaton.cfg.xml");
  }

  public UpgradeTestRule(String configurationResource) {
    super(configurationResource);
  }

  @Override
  public void starting(Description description) {
    Class<?> testClass = description.getTestClass();
    if (scenarioTestedByClass == null) {
      ScenarioUnderTest testScenarioClassAnnotation = testClass.getAnnotation(ScenarioUnderTest.class);
      if (testScenarioClassAnnotation != null) {
        scenarioTestedByClass = testScenarioClassAnnotation.value();
      }
    }

    ScenarioUnderTest testScenarioAnnotation = description.getAnnotation(ScenarioUnderTest.class);
    if (testScenarioAnnotation != null) {
      if (scenarioTestedByClass != null) {
        scenarioName = scenarioTestedByClass + "." + testScenarioAnnotation.value();
      } else {
        scenarioName = testScenarioAnnotation.value();
      }
    }

    // method annotation overrides class annotation
    Origin originAnnotation = description.getAnnotation(Origin.class);
    if (originAnnotation == null) {
      originAnnotation = testClass.getAnnotation(Origin.class);
    }

    if (originAnnotation != null) {
      tag = originAnnotation.value();
    }

    if (scenarioName == null) {
      throw new RuntimeException("Could not determine scenario under test for test " + description.getDisplayName());
    }

    super.starting(description);
  }

  public TaskQuery taskQuery() {
    return taskService.createTaskQuery().processInstanceBusinessKey(getBusinessKey());
  }

  public ExecutionQuery executionQuery() {
    return runtimeService.createExecutionQuery().processInstanceBusinessKey(getBusinessKey());
  }

  public JobQuery jobQuery() {
    ProcessInstance instance = processInstance();
    return managementService.createJobQuery().processInstanceId(instance.getId());
  }

  public JobDefinitionQuery jobDefinitionQuery() {
    ProcessInstance instance = processInstance();
    return managementService.createJobDefinitionQuery()
            .processDefinitionId(instance.getProcessDefinitionId());
  }

  public IncidentQuery incidentQuery() {
    ProcessInstance processInstance = processInstance();
    return runtimeService.createIncidentQuery()
        .processInstanceId(processInstance.getId());
  }

  public ProcessInstanceQuery processInstanceQuery() {
    return runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(getBusinessKey());
  }

  public ProcessInstance processInstance() {
    ProcessInstance instance = processInstanceQuery().singleResult();

    if (instance == null) {
      throw new RuntimeException("There is no process instance for scenario " + getBusinessKey());
    }

    return instance;
  }

  public HistoricProcessInstance historicProcessInstance() {
    HistoricProcessInstance historicProcessInstance = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(getBusinessKey())
            .singleResult();

    if (historicProcessInstance == null) {
      throw new RuntimeException("There is no historic process instance for scenario " + getBusinessKey());
    }

    return historicProcessInstance;
  }

  public HistoricIncidentQuery historicIncidentQuery() {
    ProcessInstance processInstance = processInstance();
    return historyService.createHistoricIncidentQuery()
        .processInstanceId(processInstance.getId());
  }

  public MessageCorrelationBuilder messageCorrelation(String messageName) {
    return runtimeService.createMessageCorrelation(messageName).processInstanceBusinessKey(getBusinessKey());
  }

  public void assertScenarioEnded() {
    assertThat(processInstanceQuery().singleResult() == null)
      .withFailMessage("Process instance for scenario " + getBusinessKey() + " should have ended")
      .isTrue();
  }

  // case //////////////////////////////////////////////////
  public CaseInstanceQuery caseInstanceQuery() {
    return caseService
            .createCaseInstanceQuery()
            .caseInstanceBusinessKey(getBusinessKey());
  }

  public CaseExecutionQuery caseExecutionQuery() {
    return caseService
            .createCaseExecutionQuery()
            .caseInstanceBusinessKey(getBusinessKey());
  }

  public CaseInstance caseInstance() {
    CaseInstance instance = caseInstanceQuery().singleResult();

    if (instance == null) {
      throw new RuntimeException("There is no case instance for scenario " + getBusinessKey());
    }

    return instance;
  }

  public String getScenarioName() {
    return scenarioName;
  }

  public String getBusinessKey() {
    if (tag != null) {
      return tag + '.' + scenarioName;
    }
    return scenarioName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

}
