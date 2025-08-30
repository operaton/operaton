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
package org.operaton.bpm.qa.upgrade.scenarios.sentry;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

/**
 * @author Roman Smirnov
 *
 */
public final class SentryScenario {

  private SentryScenario() {
  }

  @Deployment
  public static String deployOneTaskProcess() {
    return "org/operaton/bpm/qa/upgrade/sentry/sentry.cmmn";
  }

  @DescribesScenario("triggerTaskEntryCriterion")
  public static ScenarioSetup triggerEntryCriterion() {
    return (engine, scenarioName) -> {
      CaseService caseService = engine.getCaseService();
      caseService.createCaseInstanceByKey("case", scenarioName);
    };
  }

  @DescribesScenario("triggerStageEntryCriterion")
  public static ScenarioSetup completeStage() {
    return (engine, scenarioName) -> {
      CaseService caseService = engine.getCaseService();
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("case", scenarioName);
      String caseInstanceId = caseInstance.getId();

      CaseExecutionQuery query = caseService.createCaseExecutionQuery().caseInstanceId(caseInstanceId);

      String firstHumanTaskId = query.activityId("PI_HumanTask_1").singleResult().getId();
      caseService.manuallyStartCaseExecution(firstHumanTaskId);
      caseService.completeCaseExecution(firstHumanTaskId);

      String secondHumanTaskId = query.activityId("PI_HumanTask_2").singleResult().getId();
      caseService.manuallyStartCaseExecution(secondHumanTaskId);
    };
  }

  @DescribesScenario("newSentryInstance")
  public static ScenarioSetup newSentryInstance() {
    return (engine, scenarioName) -> {
      CaseService caseService = engine.getCaseService();
      CaseInstance caseInstance = caseService.createCaseInstanceByKey("case", scenarioName);
      String caseInstanceId = caseInstance.getId();

      CaseExecutionQuery query = caseService.createCaseExecutionQuery().caseInstanceId(caseInstanceId);

      String firstHumanTaskId = query.activityId("PI_HumanTask_1").singleResult().getId();
      caseService.manuallyStartCaseExecution(firstHumanTaskId);
      caseService.completeCaseExecution(firstHumanTaskId);

      String secondHumanTaskId = query.activityId("PI_HumanTask_2").singleResult().getId();
      caseService.manuallyStartCaseExecution(secondHumanTaskId);
      caseService.completeCaseExecution(secondHumanTaskId);
    };
  }

  @DescribesScenario("completeInstance")
  public static ScenarioSetup completeInstance() {
    return (engine, scenarioName) -> {
      CaseService caseService = engine.getCaseService();
      caseService.createCaseInstanceByKey("case", scenarioName);
    };
  }
}
