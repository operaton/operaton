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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class SentryHandlerTest extends CmmnElementHandlerTest {

  protected Sentry sentry;
  protected PlanItemOnPart onPart;
  protected Task task;
  protected PlanItem planItem;
  protected TaskItemHandler taskItemHandler = new TaskItemHandler();
  protected SentryHandler sentryHandler = new SentryHandler();

  @BeforeEach
  void setUp() {
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
  void testSentry() {
    // given

    // when
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertThat(sentryDeclaration).isNotNull();

    assertThat(sentryDeclaration.getId()).isEqualTo(sentry.getId());

    assertThat(sentryDeclaration.getIfPart()).isNull();
    assertThat(sentryDeclaration.getOnParts()).isEmpty();
  }

  @Test
  void testSentryWithIfPart() {
    // given
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    String expression = "${test}";
    body.setTextContent(expression);

    // when
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertThat(sentryDeclaration).isNotNull();

    CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
    assertThat(ifPartDeclaration).isNotNull();

    Expression condition = ifPartDeclaration.getCondition();
    assertThat(condition).isNotNull();
    assertThat(condition.getExpressionText()).isEqualTo(expression);

    assertThat(sentryDeclaration.getOnParts()).isEmpty();

  }

  @Test
  void testSentryWithIfPartWithMultipleCondition() {
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
    assertThat(sentryDeclaration).isNotNull();

    CmmnIfPartDeclaration ifPartDeclaration = sentryDeclaration.getIfPart();
    assertThat(ifPartDeclaration).isNotNull();

    Expression condition = ifPartDeclaration.getCondition();
    assertThat(condition).isNotNull();
    assertThat(condition.getExpressionText()).isEqualTo(firstExpression);

    // the second condition will be ignored!

    assertThat(sentryDeclaration.getOnParts()).isEmpty();

  }

  @Test
  void testSentryWithOnPart() {
    // given
    CmmnActivity casePlanModelActivity = new CasePlanModelHandler().handleElement(casePlanModel, context);
    context.setParent(casePlanModelActivity);

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);
    CmmnActivity source = taskItemHandler.handleElement(planItem, context);

    // when
    sentryHandler.initializeOnParts(sentry, context);

    // then
    assertThat(sentryDeclaration).isNotNull();

    List<CmmnOnPartDeclaration> onParts = sentryDeclaration.getOnParts();
    assertThat(onParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(1);

    List<CmmnOnPartDeclaration> onPartsAssociatedWithSource = sentryDeclaration.getOnParts(source.getId());
    assertThat(onPartsAssociatedWithSource)
            .isNotNull()
            .isNotEmpty();
    assertThat(onParts).hasSize(1);

    CmmnOnPartDeclaration onPartDeclaration = onPartsAssociatedWithSource.get(0);
    assertThat(onPartDeclaration).isNotNull();
    // source
    assertThat(onPartDeclaration.getSource()).isEqualTo(source);
    assertThat(onPartDeclaration.getSource().getId()).isEqualTo(onPart.getSource().getId());
    // standardEvent
    assertThat(onPartDeclaration.getStandardEvent()).isEqualTo(onPart.getStandardEvent().name());
    // sentry
    assertThat(onPartDeclaration.getSentry()).isNull();

    assertThat(sentryDeclaration.getIfPart()).isNull();

  }

  @Test
  void testSentryWithOnPartReferencesSentry() {
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
    assertThat(sentryDeclaration).isNotNull();

    List<CmmnOnPartDeclaration> onParts = sentryDeclaration.getOnParts();
    assertThat(onParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(1);

    List<CmmnOnPartDeclaration> onPartsAssociatedWithSource = sentryDeclaration.getOnParts(source.getId());
    assertThat(onPartsAssociatedWithSource)
            .isNotNull()
            .isNotEmpty();
    assertThat(onParts).hasSize(1);

    CmmnOnPartDeclaration onPartDeclaration = onPartsAssociatedWithSource.get(0);
    assertThat(onPartDeclaration).isNotNull();
    // source
    assertThat(onPartDeclaration.getSource()).isEqualTo(source);
    assertThat(onPartDeclaration.getSource().getId()).isEqualTo(onPart.getSource().getId());
    // standardEvent
    assertThat(onPartDeclaration.getStandardEvent()).isEqualTo(onPart.getStandardEvent().name());
    // sentry
    assertThat(onPartDeclaration.getSentry()).isNotNull();
    assertThat(onPartDeclaration.getSentry()).isEqualTo(exitSentryDeclaration);

    assertThat(sentryDeclaration.getIfPart()).isNull();

  }

  // variableOnParts
  @Test
  void sentryTransformWithVariableOnPart() {
    // given
    ExtensionElements extensionElements = createElement(sentry, "extensionElements", ExtensionElements.class);
    OperatonVariableOnPart variableOnPart = createElement(extensionElements, null, OperatonVariableOnPart.class);
    createElement(variableOnPart, null, OperatonVariableTransitionEvent.class);
    variableOnPart.setVariableEvent(VariableTransition.create);
    variableOnPart.setVariableName("aVariable");

    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // then
    assertThat(sentryDeclaration).isNotNull();
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertThat(variableOnParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(1);

    CmmnVariableOnPartDeclaration transformedVariableOnPart = variableOnParts.get(0);
    assertThat(transformedVariableOnPart.getVariableName()).isEqualTo("aVariable");
    assertThat(transformedVariableOnPart.getVariableEvent()).isEqualTo(VariableTransition.create.name());

  }

  @Test
  void sentryTransformWithMultipleVariableOnPart() {
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
    assertThat(sentryDeclaration).isNotNull();
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertThat(variableOnParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(2);

  }

  @Test
  void sentryTransformWithSameVariableOnPartTwice() {
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
    assertThat(sentryDeclaration).isNotNull();
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertThat(variableOnParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(1);

  }

  @Test
  void sentryTransformShouldFailWithMissingVariableEvent() {
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
  void sentryTransformShouldFailWithInvalidVariableEvent() {
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
  void sentryTransformWithMultipleVariableEvent() {
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
    assertThat(sentryDeclaration).isNotNull();
    List<CmmnVariableOnPartDeclaration> variableOnParts = sentryDeclaration.getVariableOnParts();
    assertThat(variableOnParts)
            .isNotNull()
            .isNotEmpty()
            .hasSize(1);

    CmmnVariableOnPartDeclaration transformedVariableOnPart = variableOnParts.get(0);
    assertThat(transformedVariableOnPart.getVariableName()).isEqualTo("aVariable");
    // when there are multiple variable events then, only first variable event is considered.
    assertThat(transformedVariableOnPart.getVariableEvent()).isEqualTo(VariableTransition.create.name());
  }

  @Test
  void sentryTransformShouldFailWithMissingVariableName() {
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
