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
package org.operaton.bpm.model.cmmn10;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.impl.CmmnModelConstants;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseRole;
import org.operaton.bpm.model.cmmn.instance.CaseRoles;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.Documentation;
import org.operaton.bpm.model.cmmn.instance.EntryCriterion;
import org.operaton.bpm.model.cmmn.instance.Event;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.cmmn.instance.InputCaseParameter;
import org.operaton.bpm.model.cmmn.instance.InputsCaseParameter;
import org.operaton.bpm.model.cmmn.instance.OutputCaseParameter;
import org.operaton.bpm.model.cmmn.instance.OutputsCaseParameter;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.cmmn.instance.TimerEvent;
import org.operaton.bpm.model.cmmn.instance.UserEvent;
import org.operaton.bpm.model.xml.ModelValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@SuppressWarnings("java:S1874") // Tests for deprecated methods
class Cmmn10Test {

  @Test
  void shouldGetCasePlanModelExitCriterion() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    CasePlanModel casePlanModel = modelInstance.getModelElementsByType(CasePlanModel.class).iterator().next();

    Collection<Sentry> exitCriterias = casePlanModel.getExitCriterias();
    assertThat(exitCriterias).hasSize(1);

    Collection<Sentry> exitCriteria = casePlanModel.getExitCriteria();
    assertThat(exitCriteria).hasSize(1);

    Collection<ExitCriterion> exitCriterions = casePlanModel.getExitCriterions();
    assertThat(exitCriterions).isEmpty();
  }

  @Test
  void shouldGetPlanItemExitCriterion() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    PlanItem planItem = modelInstance.getModelElementsByType(PlanItem.class).iterator().next();

    Collection<Sentry> exitCriterias = planItem.getExitCriterias();
    assertThat(exitCriterias).hasSize(1);

    Collection<Sentry> exitCriteria = planItem.getExitCriteria();
    assertThat(exitCriteria).hasSize(1);

    Collection<ExitCriterion> exitCriterions = planItem.getExitCriterions();
    assertThat(exitCriterions).isEmpty();
  }

  @Test
  void shouldGetPlanItemEntryCriterion() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    PlanItem planItem = modelInstance.getModelElementsByType(PlanItem.class).iterator().next();

    Collection<Sentry> entryCriterias = planItem.getEntryCriterias();
    assertThat(entryCriterias).hasSize(1);

    Collection<Sentry> entryCriteria = planItem.getEntryCriteria();
    assertThat(entryCriteria).hasSize(1);

    Collection<EntryCriterion> entryCriterions = planItem.getEntryCriterions();
    assertThat(entryCriterions).isEmpty();
  }

  @Test
  void shouldGetTaskInputsOutputs() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    HumanTask humanTask = modelInstance.getModelElementsByType(HumanTask.class).iterator().next();

    Collection<InputsCaseParameter> inputs = humanTask.getInputs();
    assertThat(inputs).hasSize(1);

    Collection<InputCaseParameter> inputParameters = humanTask.getInputParameters();
    assertThat(inputParameters).isEmpty();

    Collection<OutputsCaseParameter> outputs = humanTask.getOutputs();
    assertThat(outputs).hasSize(1);

    Collection<OutputCaseParameter> outputParameters = humanTask.getOutputParameters();
    assertThat(outputParameters).isEmpty();
  }

  @Test
  void shouldGetEvents() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();

    Event event = modelInstance.getModelElementsByType(Event.class).iterator().next();
    assertThat(event).isNotNull();

    UserEvent userEvent = modelInstance.getModelElementsByType(UserEvent.class).iterator().next();
    assertThat(userEvent).isNotNull();

    TimerEvent timerEvent = modelInstance.getModelElementsByType(TimerEvent.class).iterator().next();
    assertThat(timerEvent).isNotNull();
  }

  @Test
  void shouldGetDescription() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    CasePlanModel casePlanModel = modelInstance.getModelElementsByType(CasePlanModel.class).iterator().next();

    String description = casePlanModel.getDescription();
    assertThat(description).isEqualTo("This is a description...");

    Collection<Documentation> documentations = casePlanModel.getDocumentations();
    assertThat(documentations).isEmpty();
  }

  @Test
  void shouldGetMultipleIfPartConditions() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    Sentry sentry = modelInstance.getModelElementsByType(Sentry.class).iterator().next();

    IfPart ifPart = sentry.getIfPart();
    assertThat(ifPart).isNotNull();

    Collection<ConditionExpression> conditions = ifPart.getConditions();
    assertThat(conditions).hasSize(2);

    ConditionExpression condition = ifPart.getCondition();
    assertThat(condition).isNotNull();
  }

  @Test
  void shouldGetCaseRoles() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    Case caseElement = modelInstance.getModelElementsByType(Case.class).iterator().next();

    Collection<CaseRole> roles = caseElement.getCaseRoles();
    assertThat(roles).hasSize(2);

    CaseRoles caseRole = caseElement.getRoles();
    assertThat(caseRole).isNull();
  }

  @Test
  void shouldGetExpressionTextContent() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    ConditionExpression expression = modelInstance.getModelElementsByType(ConditionExpression.class).iterator().next();

    assertThat(expression.getBody()).isEqualTo("${value >= 100}");
    assertThat(expression.getText()).isEqualTo("${value >= 100}");
  }

  @Test
  void shouldNotAbleToAddNewElement() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    CasePlanModel casePlanModel = modelInstance.getModelElementsByType(CasePlanModel.class).iterator().next();

    HumanTask humanTask = modelInstance.newInstance(HumanTask.class);
    casePlanModel.getPlanItemDefinitions().add(humanTask);

    assertThatThrownBy(() -> Cmmn.writeModelToStream(System.out, modelInstance), "save cmmn 1.0 model should fail")
        .isInstanceOf(ModelValidationException.class);
  }

  @Test
  void shouldReturnCmmn11Namespace() {
    CmmnModelInstance modelInstance = getCmmnModelInstance();
    CasePlanModel casePlanModel = modelInstance.getModelElementsByType(CasePlanModel.class).iterator().next();

    assertThat(casePlanModel.getElementType().getTypeNamespace()).isEqualTo(CmmnModelConstants.CMMN11_NS);
  }

  @Test
  void shouldNotAbleToAddCmmn10Element() {
    CmmnModelInstance modelInstance = Cmmn.readModelFromStream(Cmmn10Test.class.getResourceAsStream("Cmmn11Test.cmmn"));
    CasePlanModel casePlanModel = modelInstance.getModelElementsByType(CasePlanModel.class).iterator().next();

    Event event = modelInstance.newInstance(Event.class);
    casePlanModel.getPlanItemDefinitions().add(event);

    assertThatThrownBy(() -> Cmmn.writeModelToStream(System.out, modelInstance), "save cmmn 1.1 model should fail")
        .isInstanceOf(ModelValidationException.class);
  }

  protected CmmnModelInstance getCmmnModelInstance() {
    return Cmmn.readModelFromStream(Cmmn10Test.class.getResourceAsStream("Cmmn10Test.cmmn"));
  }

}
