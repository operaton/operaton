/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;

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
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 5 extension counterpart for {@link UpgradeTestRule}.
 *
 * <p>
 * It determines the scenario and origin based on the {@link ScenarioUnderTest}
 * and {@link Origin} annotations and exposes convenience accessors that were
 * previously available on the JUnit 4 rule.
 * </p>
 */
public class UpgradeTestExtension extends ProcessEngineExtension {

  protected String scenarioTestedByClass;
  protected String scenarioName;
  protected String tag;

  @Override
  public void beforeEach(ExtensionContext context) {
    determineScenario(context);
    super.beforeEach(context);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    try {
      if (closeEngine && processEngine != null) {
        processEngine.close();
      }
    } finally {
      processEngine = null;
      processEngineConfiguration = null;
      repositoryService = null;
      runtimeService = null;
      taskService = null;
      historyService = null;
      identityService = null;
      managementService = null;
      formService = null;
      filterService = null;
      authorizationService = null;
      caseService = null;
      externalTaskService = null;
      decisionService = null;
    }
  }

  protected void determineScenario(ExtensionContext context) {
    Method testMethod = context.getRequiredTestMethod();
    Class<?> testClass = context.getRequiredTestClass();

    ScenarioUnderTest classAnnotation = testClass.getAnnotation(ScenarioUnderTest.class);
    scenarioTestedByClass = classAnnotation != null ? classAnnotation.value() : null;

    ScenarioUnderTest methodAnnotation = testMethod.getAnnotation(ScenarioUnderTest.class);
    if (methodAnnotation != null) {
      scenarioName = scenarioTestedByClass == null
          ? methodAnnotation.value()
          : scenarioTestedByClass + "." + methodAnnotation.value();
    } else if (scenarioTestedByClass != null) {
      scenarioName = scenarioTestedByClass;
    } else {
      scenarioName = null;
    }

    if (scenarioName == null) {
      throw new IllegalStateException("Could not determine scenario under test for " + context.getDisplayName());
    }

    Origin originAnnotation = Optional.ofNullable(testMethod.getAnnotation(Origin.class))
        .orElse(testClass.getAnnotation(Origin.class));
    if (originAnnotation != null) {
      tag = originAnnotation.value();
    }
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
