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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
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

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public class UpgradeTestRule implements BeforeAllCallback, BeforeEachCallback {

  private String scenarioTestedByClass = null;
  private String scenarioName;
  private String tag;

  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private RuntimeService runtimeService;
  private ManagementService managementService;
  private TaskService taskService;
  private HistoryService historyService;
  private CaseService caseService;
  private RepositoryService repositoryService;

  @Override
  public void beforeAll(ExtensionContext context) {
    // Note: engine classes are loaded from the "old" engine jar.
    // The classes are not the current ones.
    var processEngine = ProcessEngines.getProcessEngine("default");
    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    runtimeService = processEngineConfiguration.getRuntimeService();
    managementService = processEngineConfiguration.getManagementService();
    taskService = processEngineConfiguration.getTaskService();
    historyService = processEngineConfiguration.getHistoryService();
    caseService = processEngineConfiguration.getCaseService();
    repositoryService = processEngineConfiguration.getRepositoryService();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();

    if (scenarioTestedByClass == null) {
      ScenarioUnderTest testScenarioClassAnnotation = testClass.getAnnotation(ScenarioUnderTest.class);
      if (testScenarioClassAnnotation != null) {
        scenarioTestedByClass = testScenarioClassAnnotation.value();
      }
    }
    ScenarioUnderTest testScenarioAnnotation = context.getTestMethod()
      .map(testMethod -> testMethod.getAnnotation(ScenarioUnderTest.class))
      .orElse(null);
    if (testScenarioAnnotation != null) {
      if (scenarioTestedByClass != null) {
        scenarioName = scenarioTestedByClass + "." + testScenarioAnnotation.value();
      } else {
        scenarioName = testScenarioAnnotation.value();
      }
    }

    // method annotation overrides class annotation
    var originAnnotation = context.getTestMethod()
      .map(testMethod -> testMethod.getAnnotation(Origin.class))
      .orElse(testClass.getAnnotation(Origin.class));

    if (originAnnotation != null) {
      tag = originAnnotation.value();
    }

    requireNonNull(scenarioName, "Could not determine scenario under test for test " + context.getDisplayName());

  }

  public String getScenarioName() {
    return scenarioName;
  }

  public String getBusinessKey() {
    return tag != null ? tag + '.' + scenarioName : scenarioName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public RuntimeService getRuntimeService() {
    return runtimeService;
  }

  public ManagementService getManagementService() {
    return managementService;
  }

  public HistoryService getHistoryService() {
    return historyService;
  }

  public CaseService getCaseService() {
    return caseService;
  }

  public TaskService getTaskService() {
    return taskService;
  }

  public RepositoryService getRepositoryService() {
    return repositoryService;
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }

  public IdentityService getIdentityService() {
    return processEngineConfiguration.getIdentityService();
  }

  public ExternalTaskService getExternalTaskService() {
    return processEngineConfiguration.getExternalTaskService();
  }

  public FormService getFormService() {
    return processEngineConfiguration.getFormService();
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
    return managementService.createJobDefinitionQuery().processDefinitionId(instance.getProcessDefinitionId());
  }

  public IncidentQuery incidentQuery() {
    ProcessInstance processInstance = processInstance();
    return runtimeService.createIncidentQuery().processInstanceId(processInstance.getId());
  }

  public ProcessInstanceQuery processInstanceQuery() {
    return runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(getBusinessKey());
  }

  public ProcessInstance processInstance() {
    return requireNonNull(processInstanceQuery().singleResult(), "No process instance for scenario " + getBusinessKey());
  }

  public HistoricProcessInstance historicProcessInstance() {
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
      .processInstanceBusinessKey(getBusinessKey())
      .singleResult();
    return requireNonNull(historicProcessInstance,
      "There is no  historic process instance for scenario " + getBusinessKey());
  }

  public HistoricIncidentQuery historicIncidentQuery() {
    ProcessInstance processInstance = processInstance();
    return historyService.createHistoricIncidentQuery().processInstanceId(processInstance.getId());
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
    return caseService.createCaseInstanceQuery().caseInstanceBusinessKey(getBusinessKey());
  }

  public CaseExecutionQuery caseExecutionQuery() {
    return caseService.createCaseExecutionQuery().caseInstanceBusinessKey(getBusinessKey());
  }

  public CaseInstance caseInstance() {
    return requireNonNull(caseInstanceQuery().singleResult(), "No case instance for scenario " + getBusinessKey());
  }
}
