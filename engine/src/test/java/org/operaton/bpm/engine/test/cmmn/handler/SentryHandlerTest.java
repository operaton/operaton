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

import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.cmmn.handler.CasePlanModelHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.SentryHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.TaskItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.*;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformException;
import org.operaton.bpm.model.cmmn.PlanItemTransition;
import org.operaton.bpm.model.cmmn.VariableTransition;
import org.operaton.bpm.model.cmmn.instance.*;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonVariableOnPart;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonVariableTransitionEvent;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Roman Smirnov
 *
 */
public class SentryHandlerTest extends CmmnElementHandlerTest {

  protected Sentry sentry;
  protected PlanItemOnPart onPart;
  protected Task task;
  protected PlanItem planItem;
  protected TaskItemHandler taskItemHandler = new TaskItemHandler();
  protected SentryHandler sentryHandler = new SentryHandler();

  @Before
  public void setUp() {
    task = createElement(casePlanModel, "aTask", Task.class);

    planItem = createElement(casePlanModel, "PI_aTask", PlanItem.class);
    planItem.setDefinition(task);

    sentry = createElement(casePlanModel, "aSentry", Sentry.class);

    onPart = createElement(sentry, "anOnPart", PlanItemOnPart.class);
    onPart.setSource(planItem);
    createElement(onPart, null, PlanItemTransitionStandardEvent.class);
    onPart.setStandardEvent(PlanItemTransition.complete);

  }

  @Test
  public void testSentry() {
    // given

    // when
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);

    assertThat(sentryDeclaration.getId()).isEqualTo(sentry.getId());

    assertNull(sentryDeclaration.getIfPart());
    assertTrue(sentryDeclaration.getOnParts().isEmpty());
  }

  @Test
  public void testSentryWithIfPart() {
    // given
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    String expression = "${test}";
    body.setTextContent(expression);

    // when
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);

    CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
    assertNotNull(ifPartDeclaration);

    Expression condition = ifPartDeclaration.getCondition();
    assertNotNull(condition);
    assertThat(condition.getExpressionText()).isEqualTo(expression);

    assertTrue(sentryDeclaration.getOnParts().isEmpty());

  }

  @Test
  public void testSentryWithIfPartWithMultipleCondition() {
    // given
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);

    ConditionExpression firstConditionExpression = createElement(ifPart, "con_1", ConditionExpression.class);
    Body firstBody = createElement(firstConditionExpression, null, Body.class);
    String firstExpression = "${firstExpression}";
    firstBody.setTextContent(firstExpression);

    ConditionExpression secondConditionExpression = createElement(ifPart, "con_2", ConditionExpression.class);
    Body secondBody = createElement(secondConditionExpression, null, Body.class);
    String secondExpression = "${secondExpression}";
    secondBody.setTextContent(secondExpression);

    // when
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);

    CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
    assertNotNull(ifPartDeclaration);

    Expression condition = ifPartDeclaration.getCondition();
    assertNotNull(condition);
    assertThat(condition.getExpressionText()).isEqualTo(firstExpression);

    // the second condition will be ignored!

    assertTrue(sentryDeclaration.getOnParts().isEmpty());

  }

  @Test
  public void testSentryWithOnPart() {
    // given
    CmmnActivity casePlanModelActivity = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(casePlanModelActivity);

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);
    CmmnActivity source = taskItemHandler.handleElement(planItem, context);

    // when
    sentryHandler.initializeOnParts(sentry, context);

    // then
    assertNotNull(sentryDeclaration);

    List<CmmnOnPartDeclaration> onParts = sentryDeclaration.getOnParts();
    assertNotNull(onParts);
    assertFalse(onParts.isEmpty());
    assertThat(onParts).hasSize(1);

    List<CmmnOnPartDeclaration> onPartsAssociatedWithSource = sentryDeclaration.getOnParts(source.getId());
    assertNotNull(onPartsAssociatedWithSource);
    assertFalse(onPartsAssociatedWithSource.isEmpty());
    assertThat(onParts).hasSize(1);

    CmmnOnPartDeclaration onPartDeclaration = onPartsAssociatedWithSource.get(0);
    assertNotNull(onPartDeclaration);
    // source
    assertThat(onPartDeclaration.getSource()).isEqualTo(source);
    assertThat(onPartDeclaration.getSource().getId()).isEqualTo(onPart.getSource().getId());
    // standardEvent
    assertThat(onPartDeclaration.getStandardEvent()).isEqualTo(onPart.getStandardEvent().name());
    // sentry
    assertNull(onPartDeclaration.getSentry());

    assertNull(sentryDeclaration.getIfPart());

  }

  @Test
  public void testSentryWithOnPartReferencesSentry() {
    // given
    Sentry exitSentry = createElement(casePlanModel, "anotherSentry", Sentry.class);
    IfPart ifPart = createElement(exitSentry, "IfPart_1", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "con_1", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    body.setTextContent("${test}");

    onPart.setSentry(exitSentry);

    CmmnActivity casePlanModelActivity = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(casePlanModelActivity);

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);
    CmmnSentryDeclaration exitSentryDeclaration = sentryHandler.handleElement(exitSentry, context);
    CmmnActivity source = taskItemHandler.handleElement(planItem, context);

    // when
    sentryHandler.initializeOnParts(sentry, context);

    // then
    assertNotNull(sentryDeclaration);

    List<CmmnOnPartDeclaration> onParts = sentryDeclaration.getOnParts();
    assertNotNull(onParts);
    assertFalse(onParts.isEmpty());
    assertThat(onParts).hasSize(1);

    List<CmmnOnPartDeclaration> onPartsAssociatedWithSource = sentryDeclaration.getOnParts(source.getId());
    assertNotNull(onPartsAssociatedWithSource);
    assertFalse(onPartsAssociatedWithSource.isEmpty());
    assertThat(onParts).hasSize(1);

    CmmnOnPartDeclaration onPartDeclaration = onPartsAssociatedWithSource.get(0);
    assertNotNull(onPartDeclaration);
    // source
    assertThat(onPartDeclaration.getSource()).isEqualTo(source);
    assertThat(onPartDeclaration.getSource().getId()).isEqualTo(onPart.getSource().getId());
    // standardEvent
    assertThat(onPartDeclaration.getStandardEvent()).isEqualTo(onPart.getStandardEvent().name());
    // sentry
    assertNotNull(onPartDeclaration.getSentry());
    assertThat(onPartDeclaration.getSentry()).isEqualTo(exitSentryDeclaration);

    assertNull(sentryDeclaration.getIfPart());

  }

  // variableOnParts
  @Test
  public void sentryTransformWithVariableOnPart() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    variableOnPart.setVariableEvent(VariableTransition.create);
    variableOnPart.setVariableName("aVariable");

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertNotNull(variableOnParts);
    assertFalse(variableOnParts.isEmpty());
    assertThat(variableOnParts).hasSize(1);

    CmmnVariableOnPartDeclaration transformedVariableOnPart = variableOnParts.get(0);
    assertThat(transformedVariableOnPart.getVariableName()).isEqualTo("aVariable");
    assertThat(transformedVariableOnPart.getVariableEvent()).isEqualTo(VariableTransition.create.name());

  }

  @Test
  public void sentryTransformWithMultipleVariableOnPart() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    variableOnPart.setVariableEvent(VariableTransition.create);
    variableOnPart.setVariableName("aVariable");

    OperatonVariableOnPart additionalVariableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(additionalVariableOnPart, null, OperatonVariableTransitionEvent.class);
    additionalVariableOnPart.setVariableEvent(VariableTransition.update);
    additionalVariableOnPart.setVariableName("bVariable");

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertNotNull(variableOnParts);
    assertFalse(variableOnParts.isEmpty());
    assertThat(variableOnParts).hasSize(2);

  }

  @Test
  public void sentryTransformWithSameVariableOnPartTwice() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    variableOnPart.setVariableEvent(VariableTransition.create);
    variableOnPart.setVariableName("aVariable");

    OperatonVariableOnPart additionalVariableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(additionalVariableOnPart, null, OperatonVariableTransitionEvent.class);
    additionalVariableOnPart.setVariableEvent(VariableTransition.create);
    additionalVariableOnPart.setVariableName("aVariable");

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertNotNull(variableOnParts);
    assertFalse(variableOnParts.isEmpty());
    assertThat(variableOnParts).hasSize(1);

  }

  @Test
  public void sentryTransformShouldFailWithMissingVariableEvent() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    variableOnPart.setVariableName("aVariable");

    // when/then
    assertThatThrownBy(() -> sentryHandler.handleElement(sentry, context))
      .isInstanceOf(CmmnTransformException.class)
      .hasMessageContaining("The variableOnPart of the sentry with id 'aSentry' must have one valid variable event.");
  }

  @Test
  public void sentryTransformShouldFailWithInvalidVariableEvent() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    OperatonVariableTransitionEvent transitionEvent = createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    transitionEvent.setTextContent("invalid");
    variableOnPart.setVariableName("aVariable");

    // when/then
    assertThatThrownBy(() -> sentryHandler.handleElement(sentry, context))
      .isInstanceOf(CmmnTransformException.class)
      .hasMessageContaining("The variableOnPart of the sentry with id 'aSentry' must have one valid variable event.");
  }

  @Test
  public void sentryTransformWithMultipleVariableEvent() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    OperatonVariableTransitionEvent transitionEvent = createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    transitionEvent.setTextContent("create");
    OperatonVariableTransitionEvent additionalTransitionEvent = createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    additionalTransitionEvent.setTextContent("delete");
    variableOnPart.setVariableName("aVariable");

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertNotNull(sentryDeclaration);
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertNotNull(variableOnParts);
    assertFalse(variableOnParts.isEmpty());
    assertThat(variableOnParts).hasSize(1);

    CmmnVariableOnPartDeclaration transformedVariableOnPart = variableOnParts.get(0);
    assertThat(transformedVariableOnPart.getVariableName()).isEqualTo("aVariable");
    // when there are multiple variable events then, only first variable event is considered.
    assertThat(transformedVariableOnPart.getVariableEvent()).isEqualTo(VariableTransition.create.name());
  }

  @Test
  public void sentryTransformShouldFailWithMissingVariableName() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    variableOnPart.setVariableEvent(VariableTransition.create);

    // when/then
    assertThatThrownBy(() -> sentryHandler.handleElement(sentry, context))
      .isInstanceOf(CmmnTransformException.class)
      .hasMessageContaining("The variableOnPart of the sentry with id 'aSentry' must have variable name.");
  }
}
