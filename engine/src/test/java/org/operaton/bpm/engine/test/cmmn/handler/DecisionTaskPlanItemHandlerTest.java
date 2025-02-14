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
package org.operaton.bpm.engine.test.cmmn.handler;

import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_DESCRIPTION;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_TYPE;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_IS_BLOCKING;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_REQUIRED_RULE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.helper.CmmnProperties;
import org.operaton.bpm.engine.impl.cmmn.CaseControlRule;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.DecisionTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.DmnDecisionTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.handler.CasePlanModelHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.DecisionTaskItemHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.SentryHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement.CallableElementBinding;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ConstantValueProvider;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.impl.dmn.result.DecisionResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.ResultListDecisionTableResultMapper;
import org.operaton.bpm.engine.impl.dmn.result.SingleResultDecisionResultMapper;
import org.operaton.bpm.engine.impl.el.ElValueProvider;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.instance.Body;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.DecisionRefExpression;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;
import org.operaton.bpm.model.cmmn.instance.DefaultControl;
import org.operaton.bpm.model.cmmn.instance.Documentation;
import org.operaton.bpm.model.cmmn.instance.EntryCriterion;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.cmmn.instance.ItemControl;
import org.operaton.bpm.model.cmmn.instance.ManualActivationRule;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.PlanItemControl;
import org.operaton.bpm.model.cmmn.instance.RepetitionRule;
import org.operaton.bpm.model.cmmn.instance.RequiredRule;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class DecisionTaskPlanItemHandlerTest extends CmmnElementHandlerTest {

  protected DecisionTask decisionTask;
  protected PlanItem planItem;
  protected DecisionTaskItemHandler handler = new DecisionTaskItemHandler();

  @Before
  public void setUp() {
    decisionTask = createElement(casePlanModel, "aHumanTask", DecisionTask.class);

    planItem = createElement(casePlanModel, "PI_aHumanTask", PlanItem.class);
    planItem.setDefinition(decisionTask);
  }

  @Test
  public void testActivityName() {
    // given:
    String name = "A DecisionTask";
    decisionTask.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getName()).isEqualTo(name);
  }

  @Test
  public void testPlanItemActivityName() {
    // given:
    String humanTaskName = "A DecisionTask";
    decisionTask.setName(humanTaskName);

    String planItemName = "My LocalName";
    planItem.setName(planItemName);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getName()).isNotEqualTo(humanTaskName);
    assertThat(activity.getName()).isEqualTo(planItemName);
  }

  @Test
  public void testActivityType() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    String activityType = (String) activity.getProperty(PROPERTY_ACTIVITY_TYPE);
    assertThat(activityType).isEqualTo("decisionTask");
  }

  @Test
  public void testDescriptionProperty() {
    // given
    String description = "This is a decisionTask";
    decisionTask.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  public void testPlanItemDescriptionProperty() {
    // given
    String description = "This is a planItem";
    planItem.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  public void testDocumentation() {
    // given
    String description = "This is a documenation";
    Documentation documentation = createElement(decisionTask, Documentation.class);
    documentation.setTextContent(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  public void testPlanItemDocumentation() {
    // given
    String description = "This is a planItem";
    Documentation documentationElem = createElement(planItem, Documentation.class);
    documentationElem.setTextContent(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  public void testActivityBehavior() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    assertThat(behavior instanceof DmnDecisionTaskActivityBehavior).isTrue();
  }

  @Test
  public void testIsBlockingEqualsTrueProperty() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    Boolean isBlocking = (Boolean) activity.getProperty(PROPERTY_IS_BLOCKING);
    assertThat(isBlocking).isTrue();
  }

  @Test
  public void testIsBlockingEqualsFalseProperty() {
    // given:
    decisionTask.setIsBlocking(false);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    Boolean isBlocking = (Boolean) activity.getProperty(PROPERTY_IS_BLOCKING);
    assertThat(isBlocking).isFalse();
  }

  @Test
  public void testExitCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    body.setTextContent("${test}");

    // set exitCriteria
    ExitCriterion criterion = createElement(planItem, ExitCriterion.class);
    criterion.setSentry(sentry);

    // transform casePlanModel as parent
    CmmnActivity parent = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(parent);

    // transform Sentry
    CmmnSentryDeclaration sentryDeclaration = new SentryHandler().handleElement(sentry, context);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    assertThat(newActivity.getEntryCriteria()).isEmpty();

    assertThat(newActivity.getExitCriteria()).isNotEmpty();
    assertThat(newActivity.getExitCriteria()).hasSize(1);

    assertThat(newActivity.getExitCriteria().get(0)).isEqualTo(sentryDeclaration);
  }

  @Test
  public void testMultipleExitCriteria() {
    // given

    // create first sentry containing ifPart
    Sentry sentry1 = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart1 = createElement(sentry1, "abc", IfPart.class);
    ConditionExpression conditionExpression1 = createElement(ifPart1, "def", ConditionExpression.class);
    Body body1 = createElement(conditionExpression1, null, Body.class);
    body1.setTextContent("${test}");

    // set first exitCriteria
    ExitCriterion criterion1 = createElement(planItem, ExitCriterion.class);
    criterion1.setSentry(sentry1);

    // create first sentry containing ifPart
    Sentry sentry2 = createElement(casePlanModel, "Sentry_2", Sentry.class);
    IfPart ifPart2 = createElement(sentry2, "ghi", IfPart.class);
    ConditionExpression conditionExpression2 = createElement(ifPart2, "jkl", ConditionExpression.class);
    Body body2 = createElement(conditionExpression2, null, Body.class);
    body2.setTextContent("${test}");

    // set second exitCriteria
    ExitCriterion criterion2 = createElement(planItem, ExitCriterion.class);
    criterion2.setSentry(sentry2);

    // transform casePlanModel as parent
    CmmnActivity parent = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(parent);

    // transform Sentry
    CmmnSentryDeclaration firstSentryDeclaration = new SentryHandler().handleElement(sentry1, context);
    CmmnSentryDeclaration secondSentryDeclaration = new SentryHandler().handleElement(sentry2, context);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    assertThat(newActivity.getEntryCriteria()).isEmpty();

    assertThat(newActivity.getExitCriteria()).isNotEmpty();
    assertThat(newActivity.getExitCriteria()).hasSize(2);

    assertThat(newActivity.getExitCriteria()).contains(firstSentryDeclaration);
    assertThat(newActivity.getExitCriteria()).contains(secondSentryDeclaration);
  }

  @Test
  public void testEntryCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    body.setTextContent("${test}");

    // set entryCriteria
    EntryCriterion criterion = createElement(planItem, EntryCriterion.class);
    criterion.setSentry(sentry);

    // transform casePlanModel as parent
    CmmnActivity parent = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(parent);

    // transform Sentry
    CmmnSentryDeclaration sentryDeclaration = new SentryHandler().handleElement(sentry, context);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    assertThat(newActivity.getExitCriteria()).isEmpty();

    assertThat(newActivity.getEntryCriteria()).isNotEmpty();
    assertThat(newActivity.getEntryCriteria()).hasSize(1);

    assertThat(newActivity.getEntryCriteria().get(0)).isEqualTo(sentryDeclaration);
  }

  @Test
  public void testMultipleEntryCriteria() {
    // given

    // create first sentry containing ifPart
    Sentry sentry1 = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart1 = createElement(sentry1, "abc", IfPart.class);
    ConditionExpression conditionExpression1 = createElement(ifPart1, "def", ConditionExpression.class);
    Body body1 = createElement(conditionExpression1, null, Body.class);
    body1.setTextContent("${test}");

    // set first entryCriteria
    EntryCriterion criterion1 = createElement(planItem, EntryCriterion.class);
    criterion1.setSentry(sentry1);

    // create first sentry containing ifPart
    Sentry sentry2 = createElement(casePlanModel, "Sentry_2", Sentry.class);
    IfPart ifPart2 = createElement(sentry2, "ghi", IfPart.class);
    ConditionExpression conditionExpression2 = createElement(ifPart2, "jkl", ConditionExpression.class);
    Body body2 = createElement(conditionExpression2, null, Body.class);
    body2.setTextContent("${test}");

    // set second entryCriteria
    EntryCriterion criterion2 = createElement(planItem, EntryCriterion.class);
    criterion2.setSentry(sentry2);

    // transform casePlanModel as parent
    CmmnActivity parent = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(parent);

    // transform Sentry
    CmmnSentryDeclaration firstSentryDeclaration = new SentryHandler().handleElement(sentry1, context);
    CmmnSentryDeclaration secondSentryDeclaration = new SentryHandler().handleElement(sentry2, context);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    assertThat(newActivity.getExitCriteria()).isEmpty();

    assertThat(newActivity.getEntryCriteria()).isNotEmpty();
    assertThat(newActivity.getEntryCriteria()).hasSize(2);

    assertThat(newActivity.getEntryCriteria()).contains(firstSentryDeclaration);
    assertThat(newActivity.getEntryCriteria()).contains(secondSentryDeclaration);
  }

  @Test
  public void testEntryCriteriaAndExitCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    body.setTextContent("${test}");

    // set entry-/exitCriteria
    EntryCriterion criterion1 = createElement(planItem, EntryCriterion.class);
    criterion1.setSentry(sentry);
    ExitCriterion criterion2 = createElement(planItem, ExitCriterion.class);
    criterion2.setSentry(sentry);

    // transform casePlanModel as parent
    CmmnActivity parent = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(parent);

    // transform Sentry
    CmmnSentryDeclaration sentryDeclaration = new SentryHandler().handleElement(sentry, context);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    assertThat(newActivity.getExitCriteria()).isNotEmpty();
    assertThat(newActivity.getExitCriteria()).hasSize(1);
    assertThat(newActivity.getExitCriteria().get(0)).isEqualTo(sentryDeclaration);

    assertThat(newActivity.getEntryCriteria()).isNotEmpty();
    assertThat(newActivity.getEntryCriteria()).hasSize(1);
    assertThat(newActivity.getEntryCriteria().get(0)).isEqualTo(sentryDeclaration);
  }

  @Test
  public void testManualActivationRule() {
    // given
    ItemControl itemControl = createElement(planItem, "ItemControl_1", ItemControl.class);
    ManualActivationRule manualActivationRule = createElement(itemControl, "ManualActivationRule_1", ManualActivationRule.class);
    ConditionExpression expression = createElement(manualActivationRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_MANUAL_ACTIVATION_RULE);
    assertThat(rule).isNotNull();
    assertThat(rule instanceof CaseControlRule).isTrue();
  }

  @Test
  public void testManualActivationRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(decisionTask, "ItemControl_1", DefaultControl.class);
    ManualActivationRule manualActivationRule = createElement(defaultControl, "ManualActivationRule_1", ManualActivationRule.class);
    ConditionExpression expression = createElement(manualActivationRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_MANUAL_ACTIVATION_RULE);
    assertThat(rule).isNotNull();
    assertThat(rule instanceof CaseControlRule).isTrue();
  }

  @Test
  public void testRequiredRule() {
    // given
    ItemControl itemControl = createElement(planItem, "ItemControl_1", ItemControl.class);
    RequiredRule requiredRule = createElement(itemControl, "RequiredRule_1", RequiredRule.class);
    ConditionExpression expression = createElement(requiredRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REQUIRED_RULE);
    assertThat(rule).isNotNull();
    assertThat(rule instanceof CaseControlRule).isTrue();
  }

  @Test
  public void testRequiredRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(decisionTask, "ItemControl_1", DefaultControl.class);
    RequiredRule requiredRule = createElement(defaultControl, "RequiredRule_1", RequiredRule.class);
    ConditionExpression expression = createElement(requiredRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REQUIRED_RULE);
    assertThat(rule).isNotNull();
    assertThat(rule instanceof CaseControlRule).isTrue();
  }

  @Test
  public void testRepetitionRuleStandardEvents() {
    // given
    ItemControl itemControl = createElement(planItem, "ItemControl_1", ItemControl.class);
    RepetitionRule repetitionRule = createElement(itemControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertThat(events)
            .isNotNull()
            .hasSize(2)
            .contains(CaseExecutionListener.COMPLETE)
            .contains(CaseExecutionListener.TERMINATE);
  }

  @Test
  public void testRepetitionRuleStandardEventsByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(decisionTask, "DefaultControl_1", DefaultControl.class);
    RepetitionRule repetitionRule = createElement(defaultControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertThat(events)
            .isNotNull()
            .hasSize(2)
            .contains(CaseExecutionListener.COMPLETE)
            .contains(CaseExecutionListener.TERMINATE);
  }

  @Test
  public void testRepetitionRuleCustomStandardEvents() {
    // given
    ItemControl itemControl = createElement(planItem, "ItemControl_1", ItemControl.class);
    RepetitionRule repetitionRule = createElement(itemControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    repetitionRule.setOperatonRepeatOnStandardEvent(CaseExecutionListener.DISABLE);

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertThat(events)
            .isNotNull()
            .hasSize(1)
            .contains(CaseExecutionListener.DISABLE);
  }

  @Test
  public void testRepetitionRuleCustomStandardEventsByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(decisionTask, "DefaultControl_1", DefaultControl.class);
    RepetitionRule repetitionRule = createElement(defaultControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    repetitionRule.setOperatonRepeatOnStandardEvent(CaseExecutionListener.DISABLE);

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertThat(events)
            .isNotNull()
            .hasSize(1)
            .contains(CaseExecutionListener.DISABLE);
  }

  @Test
  public void testWithoutParent() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getParent()).isNull();
  }

  @Test
  public void testWithParent() {
    // given
    CmmnCaseDefinition parent = new CmmnCaseDefinition("aParentActivity");
    context.setParent(parent);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getParent()).isEqualTo(parent);
    assertThat(parent.getActivities()).contains(activity);
  }

  @Test
  public void testCallableElement() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    assertThat(behavior.getCallableElement()).isNotNull();
  }

  @Test
  public void testConstantDecisionRef() {
    // given:
    String decisionRef = "aDecisionToCall";
    decisionTask.setDecision(decisionRef);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider decisionRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertThat(decisionRefValueProvider).isNotNull();

    assertThat(decisionRefValueProvider instanceof ConstantValueProvider).isTrue();
    ConstantValueProvider valueProvider = (ConstantValueProvider) decisionRefValueProvider;
    assertThat(valueProvider.getValue(null)).isEqualTo(decisionRef);
  }

  @Test
  public void testExpressionDecisionRef() {
    // given:
    String decisionRef = "${aDecisionToCall}";
    decisionTask.setDecision(decisionRef);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider caseRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertThat(caseRefValueProvider).isNotNull();

    assertThat(caseRefValueProvider instanceof ElValueProvider).isTrue();
    ElValueProvider valueProvider = (ElValueProvider) caseRefValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(decisionRef);
  }

  @Test
  public void testConstantDecisionRefExpression() {
    // given:
    String decision = "aDecisionToCall";
    DecisionRefExpression decisionRefExpression= createElement(decisionTask, DecisionRefExpression.class);
    decisionRefExpression.setText(decision);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider decisionRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertThat(decisionRefValueProvider).isNotNull();

    assertThat(decisionRefValueProvider instanceof ConstantValueProvider).isTrue();
    ConstantValueProvider valueProvider = (ConstantValueProvider) decisionRefValueProvider;
    assertThat(valueProvider.getValue(null)).isEqualTo(decision);
  }

  @Test
  public void testExpressionDecisionRefExpression() {
    // given:
    String decision = "${aDecisionToCall}";
    DecisionRefExpression decisionRefExpression= createElement(decisionTask, DecisionRefExpression.class);
    decisionRefExpression.setText(decision);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider caseRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertThat(caseRefValueProvider).isNotNull();

    assertThat(caseRefValueProvider instanceof ElValueProvider).isTrue();
    ElValueProvider valueProvider = (ElValueProvider) caseRefValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(decision);
  }

  @Test
  public void testBinding() {
    // given:
    CallableElementBinding caseBinding = CallableElementBinding.LATEST;
    decisionTask.setOperatonDecisionBinding(caseBinding.getValue());

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    CallableElementBinding binding = callableElement.getBinding();
    assertThat(binding).isNotNull();
    assertThat(binding).isEqualTo(caseBinding);
  }

  @Test
  public void testVersionConstant() {
    // given:
    String caseVersion = "2";
    decisionTask.setOperatonDecisionVersion(caseVersion);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider caseVersionValueProvider = callableElement.getVersionValueProvider();
    assertThat(caseVersionValueProvider).isNotNull();

    assertThat(caseVersionValueProvider instanceof ConstantValueProvider).isTrue();
    assertThat(caseVersionValueProvider.getValue(null)).isEqualTo(caseVersion);
  }

  @Test
  public void testVersionExpression() {
    // given:
    String caseVersion = "${aVersion}";
    decisionTask.setOperatonDecisionVersion(caseVersion);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    BaseCallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider caseVersionValueProvider = callableElement.getVersionValueProvider();
    assertThat(caseVersionValueProvider).isNotNull();

    assertThat(caseVersionValueProvider instanceof ElValueProvider).isTrue();
    ElValueProvider valueProvider = (ElValueProvider) caseVersionValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(caseVersion);
  }

  @Test
  public void testResultVariable() {
    // given:
    decisionTask.setOperatonResultVariable("aResultVariable");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DecisionTaskActivityBehavior behavior = (DecisionTaskActivityBehavior) activity.getActivityBehavior();
    assertThat(behavior.getResultVariable()).isEqualTo("aResultVariable");
  }

  @Test
  public void testDefaultMapDecisionResult() {
    // given:

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DmnDecisionTaskActivityBehavior behavior = (DmnDecisionTaskActivityBehavior) activity.getActivityBehavior();
    DecisionResultMapper mapper = behavior.getDecisionTableResultMapper();
    assertThat(mapper instanceof ResultListDecisionTableResultMapper).isTrue();
  }

  @Test
  public void testMapDecisionResult() {
    // given:
    decisionTask.setOperatonMapDecisionResult("singleResult");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    DmnDecisionTaskActivityBehavior behavior = (DmnDecisionTaskActivityBehavior) activity.getActivityBehavior();
    DecisionResultMapper mapper = behavior.getDecisionTableResultMapper();
    assertThat(mapper instanceof SingleResultDecisionResultMapper).isTrue();
  }

}
