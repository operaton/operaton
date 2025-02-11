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
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_REPETITION_RULE;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_REQUIRED_RULE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.helper.CmmnProperties;
import org.operaton.bpm.engine.impl.cmmn.CaseControlRule;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.ProcessTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.handler.CasePlanModelHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.ProcessTaskItemHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.SentryHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement.CallableElementBinding;
import org.operaton.bpm.engine.impl.core.model.CallableElement;
import org.operaton.bpm.engine.impl.core.model.CallableElementParameter;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ConstantValueProvider;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.impl.el.ElValueProvider;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.instance.Body;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.DefaultControl;
import org.operaton.bpm.model.cmmn.instance.EntryCriterion;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.ExtensionElements;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.cmmn.instance.ItemControl;
import org.operaton.bpm.model.cmmn.instance.ManualActivationRule;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.PlanItemControl;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;
import org.operaton.bpm.model.cmmn.instance.RepetitionRule;
import org.operaton.bpm.model.cmmn.instance.RequiredRule;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonOut;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessTaskPlanItemHandlerTest extends CmmnElementHandlerTest {

  protected ProcessTask processTask;
  protected PlanItem planItem;
  protected ProcessTaskItemHandler handler = new ProcessTaskItemHandler();

  @Before
  public void setUp() {
    processTask = createElement(casePlanModel, "aProcessTask", ProcessTask.class);

    planItem = createElement(casePlanModel, "PI_aProcessTask", PlanItem.class);
    planItem.setDefinition(processTask);

  }

  @Test
  public void testProcessTaskActivityName() {
    // given:
    // the processTask has a name "A ProcessTask"
    String name = "A ProcessTask";
    processTask.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getName()).isEqualTo(name);
  }

  @Test
  public void testPlanItemActivityName() {
    // given:
    // the processTask has a name "A CaseTask"
    String processTaskName = "A ProcessTask";
    processTask.setName(processTaskName);

    // the planItem has an own name "My LocalName"
    String planItemName = "My LocalName";
    planItem.setName(planItemName);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertNotEquals(processTaskName, activity.getName());
    assertThat(activity.getName()).isEqualTo(planItemName);
  }

  @Test
  public void testProcessTaskActivityType() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    String activityType = (String) activity.getProperty(PROPERTY_ACTIVITY_TYPE);
    assertThat(activityType).isEqualTo("processTask");
  }

  @Test
  public void testProcessTaskDescription() {
    // given
    String description = "This is a processTask";
    processTask.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  public void testPlanItemDescription() {
    // given
    String description = "This is a planItem";
    planItem.setDescription(description);

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
    assertTrue(behavior instanceof ProcessTaskActivityBehavior);
  }

  @Test
  public void testIsBlockingEqualsTrueProperty() {
    // given: a processTask with isBlocking = true (defaultValue)

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    Boolean isBlocking = (Boolean) activity.getProperty(PROPERTY_IS_BLOCKING);
    assertTrue(isBlocking);
  }

  @Test
  public void testIsBlockingEqualsFalseProperty() {
    // given:
    // a processTask with isBlocking = false
    processTask.setIsBlocking(false);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    Boolean isBlocking = (Boolean) activity.getProperty(PROPERTY_IS_BLOCKING);
    assertFalse(isBlocking);
  }

  @Test
  public void testWithoutParent() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertNull(activity.getParent());
  }

  @Test
  public void testWithParent() {
    // given:
    // a new activity as parent
    CmmnCaseDefinition parent = new CmmnCaseDefinition("aParentActivity");
    context.setParent(parent);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    assertThat(activity.getParent()).isEqualTo(parent);
    assertTrue(parent.getActivities().contains(activity));
  }

  @Test
  public void testCallableElement() {
    // given: a plan item

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    // there exists a callableElement
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();

    assertNotNull(behavior.getCallableElement());
  }

  @Test
  public void testProcessRefConstant() {
    // given:
    String processRef = "aProcessToCall";
    processTask.setProcess(processRef);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider processRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertNotNull(processRefValueProvider);

    assertTrue(processRefValueProvider instanceof ConstantValueProvider);
    ConstantValueProvider valueProvider = (ConstantValueProvider) processRefValueProvider;
    assertThat(valueProvider.getValue(null)).isEqualTo(processRef);
  }

  @Test
  public void testProcessRefExpression() {
    // given:
    String processRef = "${aProcessToCall}";
    processTask.setProcess(processRef);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider processRefValueProvider = callableElement.getDefinitionKeyValueProvider();
    assertNotNull(processRefValueProvider);

    assertTrue(processRefValueProvider instanceof ElValueProvider);
    ElValueProvider valueProvider = (ElValueProvider) processRefValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(processRef);
  }

  @Test
  public void testBinding() {
    // given:
    CallableElementBinding processBinding = CallableElementBinding.LATEST;
    processTask.setOperatonProcessBinding(processBinding.getValue());

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    CallableElementBinding binding = callableElement.getBinding();
    assertNotNull(binding);
    assertThat(binding).isEqualTo(processBinding);
  }

  @Test
  public void testVersionConstant() {
    // given:
    String processVersion = "2";
    processTask.setOperatonProcessVersion(processVersion);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider processVersionValueProvider = callableElement.getVersionValueProvider();
    assertNotNull(processVersionValueProvider);

    assertTrue(processVersionValueProvider instanceof ConstantValueProvider);
    assertThat(processVersionValueProvider.getValue(null)).isEqualTo(processVersion);
  }

  @Test
  public void testVersionExpression() {
    // given:
    String processVersion = "${aVersion}";
    processTask.setOperatonProcessVersion(processVersion);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider processVersionValueProvider = callableElement.getVersionValueProvider();
    assertNotNull(processVersionValueProvider);

    assertTrue(processVersionValueProvider instanceof ElValueProvider);
    ElValueProvider valueProvider = (ElValueProvider) processVersionValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(processVersion);
  }

  @Test
  public void testBusinessKeyConstant() {
    // given:
    String businessKey = "myBusinessKey";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn businessKeyElement = createElement(extensionElements, null, OperatonIn.class);
    businessKeyElement.setOperatonBusinessKey(businessKey);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider businessKeyValueProvider = callableElement.getBusinessKeyValueProvider();
    assertNotNull(businessKeyValueProvider);

    assertTrue(businessKeyValueProvider instanceof ConstantValueProvider);
    assertThat(businessKeyValueProvider.getValue(null)).isEqualTo(businessKey);
  }

  @Test
  public void testBusinessKeyExpression() {
    // given:
    String businessKey = "${myBusinessKey}";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn businessKeyElement = createElement(extensionElements, null, OperatonIn.class);
    businessKeyElement.setOperatonBusinessKey(businessKey);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    ParameterValueProvider businessKeyValueProvider = callableElement.getBusinessKeyValueProvider();
    assertNotNull(businessKeyValueProvider);

    assertTrue(businessKeyValueProvider instanceof ElValueProvider);
    ElValueProvider valueProvider = (ElValueProvider) businessKeyValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(businessKey);
  }

  @Test
  public void testInputs() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn variablesElement = createElement(extensionElements, null, OperatonIn.class);
    variablesElement.setOperatonVariables("all");
    OperatonIn sourceElement = createElement(extensionElements, null, OperatonIn.class);
    sourceElement.setOperatonSource("a");
    OperatonIn sourceExpressionElement = createElement(extensionElements, null, OperatonIn.class);
    sourceExpressionElement.setOperatonSourceExpression("${b}");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then

    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    List<CallableElementParameter> inputs = callableElement.getInputs();
    assertNotNull(inputs);
    assertFalse(inputs.isEmpty());
    assertThat(inputs.size()).isEqualTo(3);
  }

  @Test
  public void testInputVariables() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn variablesElement = createElement(extensionElements, null, OperatonIn.class);
    variablesElement.setOperatonVariables("all");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then

    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getInputs().get(0);

    assertNotNull(parameter);
    assertTrue(parameter.isAllVariables());
  }

  @Test
  public void testInputSource() {
    // given:
    String source = "a";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn sourceElement = createElement(extensionElements, null, OperatonIn.class);
    sourceElement.setOperatonSource(source);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getInputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    ParameterValueProvider sourceValueProvider = parameter.getSourceValueProvider();
    assertNotNull(sourceValueProvider);

    assertTrue(sourceValueProvider instanceof ConstantValueProvider);
    assertThat(sourceValueProvider.getValue(null)).isEqualTo(source);
  }

  @Test
  public void testInputSourceExpression() {
    // given:
    String source = "${a}";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn sourceElement = createElement(extensionElements, null, OperatonIn.class);
    sourceElement.setOperatonSourceExpression(source);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getInputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    ParameterValueProvider sourceExpressionValueProvider = parameter.getSourceValueProvider();
    assertNotNull(sourceExpressionValueProvider);

    assertTrue(sourceExpressionValueProvider instanceof ElValueProvider);
    ElValueProvider valueProvider = (ElValueProvider) sourceExpressionValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(source);
  }

  @Test
  public void testInputTarget() {
    // given:
    String target = "b";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonIn sourceElement = createElement(extensionElements, null, OperatonIn.class);
    sourceElement.setOperatonTarget(target);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getInputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    assertThat(parameter.getTarget()).isEqualTo(target);
  }

  @Test
  public void testOutputs() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonOut variablesElement = createElement(extensionElements, null, OperatonOut.class);
    variablesElement.setOperatonVariables("all");
    OperatonOut sourceElement = createElement(extensionElements, null, OperatonOut.class);
    sourceElement.setOperatonSource("a");
    OperatonOut sourceExpressionElement = createElement(extensionElements, null, OperatonOut.class);
    sourceExpressionElement.setOperatonSourceExpression("${b}");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then

    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();

    List<CallableElementParameter> outputs = callableElement.getOutputs();
    assertNotNull(outputs);
    assertFalse(outputs.isEmpty());
    assertThat(outputs.size()).isEqualTo(3);
  }

  @Test
  public void testOutputVariables() {
    // given:
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonOut variablesElement = createElement(extensionElements, null, OperatonOut.class);
    variablesElement.setOperatonVariables("all");

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then

    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getOutputs().get(0);

    assertNotNull(parameter);
    assertTrue(parameter.isAllVariables());
  }

  @Test
  public void testOutputSource() {
    // given:
    String source = "a";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonOut sourceElement = createElement(extensionElements, null, OperatonOut.class);
    sourceElement.setOperatonSource(source);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getOutputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    ParameterValueProvider sourceValueProvider = parameter.getSourceValueProvider();
    assertNotNull(sourceValueProvider);

    assertTrue(sourceValueProvider instanceof ConstantValueProvider);
    assertThat(sourceValueProvider.getValue(null)).isEqualTo(source);
  }

  @Test
  public void testOutputSourceExpression() {
    // given:
    String source = "${a}";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonOut sourceElement = createElement(extensionElements, null, OperatonOut.class);
    sourceElement.setOperatonSourceExpression(source);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getOutputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    ParameterValueProvider sourceExpressionValueProvider = parameter.getSourceValueProvider();
    assertNotNull(sourceExpressionValueProvider);

    assertTrue(sourceExpressionValueProvider instanceof ElValueProvider);
    ElValueProvider valueProvider = (ElValueProvider) sourceExpressionValueProvider;
    assertThat(valueProvider.getExpression().getExpressionText()).isEqualTo(source);
  }

  @Test
  public void testOutputTarget() {
    // given:
    String target = "b";
    ExtensionElements extensionElements = addExtensionElements(processTask);
    OperatonOut sourceElement = createElement(extensionElements, null, OperatonOut.class);
    sourceElement.setOperatonTarget(target);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    ProcessTaskActivityBehavior behavior = (ProcessTaskActivityBehavior) activity.getActivityBehavior();
    CallableElement callableElement = behavior.getCallableElement();
    CallableElementParameter parameter = callableElement.getOutputs().get(0);

    assertNotNull(parameter);
    assertFalse(parameter.isAllVariables());

    assertThat(parameter.getTarget()).isEqualTo(target);
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
    assertTrue(newActivity.getEntryCriteria().isEmpty());

    assertFalse(newActivity.getExitCriteria().isEmpty());
    assertThat(newActivity.getExitCriteria().size()).isEqualTo(1);

    assertThat(newActivity.getExitCriteria().get(0)).isEqualTo(sentryDeclaration);

  }

  @Test
  public void testMultipleExitCriteria() {
    // given

    // create first sentry containing ifPart
    Sentry sentry1 = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart1 = createElement(sentry1, "abc", IfPart.class);
    ConditionExpression conditionExpression1 = createElement(ifPart1, "def", ConditionExpression.class);
    conditionExpression1.setText("${test}");

    // set first exitCriteria
    ExitCriterion criterion1 = createElement(planItem, ExitCriterion.class);
    criterion1.setSentry(sentry1);

    // create first sentry containing ifPart
    Sentry sentry2 = createElement(casePlanModel, "Sentry_2", Sentry.class);
    IfPart ifPart2 = createElement(sentry2, "ghi", IfPart.class);
    ConditionExpression conditionExpression2 = createElement(ifPart2, "jkl", ConditionExpression.class);
    conditionExpression2.setText("${test}");

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
    assertTrue(newActivity.getEntryCriteria().isEmpty());

    assertFalse(newActivity.getExitCriteria().isEmpty());
    assertThat(newActivity.getExitCriteria().size()).isEqualTo(2);

    assertTrue(newActivity.getExitCriteria().contains(firstSentryDeclaration));
    assertTrue(newActivity.getExitCriteria().contains(secondSentryDeclaration));

  }

  @Test
  public void testEntryCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    conditionExpression.setText("${test}");

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
    assertTrue(newActivity.getExitCriteria().isEmpty());

    assertFalse(newActivity.getEntryCriteria().isEmpty());
    assertThat(newActivity.getEntryCriteria().size()).isEqualTo(1);

    assertThat(newActivity.getEntryCriteria().get(0)).isEqualTo(sentryDeclaration);

  }

  @Test
  public void testMultipleEntryCriteria() {
    // given

    // create first sentry containing ifPart
    Sentry sentry1 = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart1 = createElement(sentry1, "abc", IfPart.class);
    ConditionExpression conditionExpression1 = createElement(ifPart1, "def", ConditionExpression.class);
    conditionExpression1.setText("${test}");

    // set first entryCriteria
    EntryCriterion criterion1 = createElement(planItem, EntryCriterion.class);
    criterion1.setSentry(sentry1);

    // create first sentry containing ifPart
    Sentry sentry2 = createElement(casePlanModel, "Sentry_2", Sentry.class);
    IfPart ifPart2 = createElement(sentry2, "ghi", IfPart.class);
    ConditionExpression conditionExpression2 = createElement(ifPart2, "jkl", ConditionExpression.class);
    conditionExpression2.setText("${test}");

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
    assertTrue(newActivity.getExitCriteria().isEmpty());

    assertFalse(newActivity.getEntryCriteria().isEmpty());
    assertThat(newActivity.getEntryCriteria().size()).isEqualTo(2);

    assertTrue(newActivity.getEntryCriteria().contains(firstSentryDeclaration));
    assertTrue(newActivity.getEntryCriteria().contains(secondSentryDeclaration));

  }

  @Test
  public void testEntryCriteriaAndExitCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    conditionExpression.setText("${test}");

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
    assertFalse(newActivity.getExitCriteria().isEmpty());
    assertThat(newActivity.getExitCriteria().size()).isEqualTo(1);
    assertThat(newActivity.getExitCriteria().get(0)).isEqualTo(sentryDeclaration);

    assertFalse(newActivity.getEntryCriteria().isEmpty());
    assertThat(newActivity.getEntryCriteria().size()).isEqualTo(1);
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
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
  }

  @Test
  public void testManualActivationRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(processTask, "ItemControl_1", DefaultControl.class);
    ManualActivationRule manualActivationRule = createElement(defaultControl, "ManualActivationRule_1", ManualActivationRule.class);
    ConditionExpression expression = createElement(manualActivationRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_MANUAL_ACTIVATION_RULE);
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
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
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
  }

  @Test
  public void testRequiredRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(processTask, "ItemControl_1", DefaultControl.class);
    RequiredRule requiredRule = createElement(defaultControl, "RequiredRule_1", RequiredRule.class);
    ConditionExpression expression = createElement(requiredRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REQUIRED_RULE);
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
  }

  @Test
  public void testRepetitionRule() {
    // given
    ItemControl itemControl = createElement(planItem, "ItemControl_1", ItemControl.class);
    RepetitionRule repetitionRule = createElement(itemControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REPETITION_RULE);
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
  }

  @Test
  public void testRepetitionRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(processTask, "DefaultControl_1", DefaultControl.class);
    RepetitionRule repetitionRule = createElement(defaultControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REPETITION_RULE);
    assertNotNull(rule);
    assertTrue(rule instanceof CaseControlRule);
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
    assertNotNull(events);
    assertThat(events.size()).isEqualTo(2);
    assertTrue(events.contains(CaseExecutionListener.COMPLETE));
    assertTrue(events.contains(CaseExecutionListener.TERMINATE));
  }

  @Test
  public void testRepetitionRuleStandardEventsByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(processTask, "DefaultControl_1", DefaultControl.class);
    RepetitionRule repetitionRule = createElement(defaultControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertNotNull(events);
    assertThat(events.size()).isEqualTo(2);
    assertTrue(events.contains(CaseExecutionListener.COMPLETE));
    assertTrue(events.contains(CaseExecutionListener.TERMINATE));
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
    assertNotNull(events);
    assertThat(events.size()).isEqualTo(1);
    assertTrue(events.contains(CaseExecutionListener.DISABLE));
  }

  @Test
  public void testRepetitionRuleCustomStandardEventsByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(processTask, "DefaultControl_1", DefaultControl.class);
    RepetitionRule repetitionRule = createElement(defaultControl, "RepititionRule_1", RepetitionRule.class);
    ConditionExpression expression = createElement(repetitionRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    repetitionRule.setOperatonRepeatOnStandardEvent(CaseExecutionListener.DISABLE);

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(planItem, context);

    // then
    List<String> events = newActivity.getProperties().get(CmmnProperties.REPEAT_ON_STANDARD_EVENTS);
    assertNotNull(events);
    assertThat(events.size()).isEqualTo(1);
    assertTrue(events.contains(CaseExecutionListener.DISABLE));
  }

}
