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
package org.operaton.bpm.engine.test.cmmn.handler;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.StageActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.handler.CasePlanModelHandler;
import org.operaton.bpm.engine.impl.cmmn.handler.SentryHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.model.cmmn.instance.Body;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.cmmn.instance.Sentry;

import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_DESCRIPTION;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_TYPE;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_AUTO_COMPLETE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class CasePlanModelHandlerTest extends CmmnElementHandlerTest {

  protected CasePlanModelHandler handler = new CasePlanModelHandler();

  @Test
  void testCasePlanModelActivityName() {
    // given:
    // the case plan model has a name "A CasePlanModel"
    String name = "A CasePlanModel";
    casePlanModel.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    assertThat(activity.getName()).isEqualTo(name);
  }

  @Test
  void testCasePlanModelActivityType() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    String activityType = (String) activity.getProperty(PROPERTY_ACTIVITY_TYPE);
    assertThat(activityType).isEqualTo("casePlanModel");
  }

  @Test
  @SuppressWarnings("deprecation")
  void testCasePlanModelDescription() {
    // given
    String description = "This is a casePlanModal";
    casePlanModel.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  void testActivityBehavior() {
    // given: a case plan model

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    assertThat(behavior).isInstanceOf(StageActivityBehavior.class);
  }

  @Test
  void testWithoutParent() {
    // given: a casePlanModel

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    assertThat(activity.getParent()).isNull();
  }

  @Test
  void testWithParent() {
    // given:
    // a new activity as parent
    CmmnCaseDefinition parent = new CmmnCaseDefinition("aParentActivity");
    context.setParent(parent);

    // when
    CmmnActivity activity = handler.handleElement(casePlanModel, context);

    // then
    assertThat(activity.getParent()).isEqualTo(parent);
    assertThat(parent.getActivities()).contains(activity);
  }

  @Test
  void testExitCriteria() {
    // given

    // create sentry containing ifPart
    Sentry sentry = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart = createElement(sentry, "abc", IfPart.class);
    ConditionExpression conditionExpression = createElement(ifPart, "def", ConditionExpression.class);
    Body body = createElement(conditionExpression, null, Body.class);
    body.setTextContent("${test}");

    // set exitCriteria
    ExitCriterion criterion = createElement(casePlanModel, ExitCriterion.class);
    criterion.setSentry(sentry);

    // transform casePlanModel
    CmmnActivity newActivity = handler.handleElement(casePlanModel, context);

    // transform Sentry
    context.setParent(newActivity);
    SentryHandler sentryHandler = new SentryHandler();
    CmmnSentryDeclaration sentryDeclaration = sentryHandler.handleElement(sentry, context);

    // when
    handler.initializeExitCriterias(casePlanModel, newActivity, context);

    // then
    assertThat(newActivity.getEntryCriteria()).isEmpty();

    assertThat(newActivity.getExitCriteria()).isNotEmpty();
    assertThat(newActivity.getExitCriteria()).hasSize(1);

    assertThat(newActivity.getExitCriteria().get(0)).isEqualTo(sentryDeclaration);

  }

  @Test
  void testMultipleExitCriteria() {
    // given

    // create first sentry containing ifPart
    Sentry sentry1 = createElement(casePlanModel, "Sentry_1", Sentry.class);
    IfPart ifPart1 = createElement(sentry1, "abc", IfPart.class);
    ConditionExpression conditionExpression1 = createElement(ifPart1, "def", ConditionExpression.class);
    conditionExpression1.setText("${test}");

    // set first exitCriteria
    ExitCriterion criterion1 = createElement(casePlanModel, ExitCriterion.class);
    criterion1.setSentry(sentry1);

    // create first sentry containing ifPart
    Sentry sentry2 = createElement(casePlanModel, "Sentry_2", Sentry.class);
    IfPart ifPart2 = createElement(sentry2, "ghi", IfPart.class);
    ConditionExpression conditionExpression2 = createElement(ifPart2, "jkl", ConditionExpression.class);
    conditionExpression2.setText("${test}");

    // set second exitCriteria
    ExitCriterion criterion2 = createElement(casePlanModel, ExitCriterion.class);
    criterion2.setSentry(sentry2);

    // transform casePlanModel
    CmmnActivity newActivity = handler.handleElement(casePlanModel, context);

    context.setParent(newActivity);
    SentryHandler sentryHandler = new SentryHandler();

    // transform first Sentry
    CmmnSentryDeclaration firstSentryDeclaration = sentryHandler.handleElement(sentry1, context);

    // transform second Sentry
    CmmnSentryDeclaration secondSentryDeclaration = sentryHandler.handleElement(sentry2, context);

    // when
    handler.initializeExitCriterias(casePlanModel, newActivity, context);

    // then
    assertThat(newActivity.getEntryCriteria()).isEmpty();

    assertThat(newActivity.getExitCriteria()).isNotEmpty();
    assertThat(newActivity.getExitCriteria()).hasSize(2);

    assertThat(newActivity.getExitCriteria()).contains(firstSentryDeclaration);
    assertThat(newActivity.getExitCriteria()).contains(secondSentryDeclaration);

  }

  @Test
  void testAutoComplete() {
    // given
    casePlanModel.setAutoComplete(true);

    // when
    CmmnActivity newActivity = handler.handleElement(casePlanModel, context);

    // then
    Object autoComplete = newActivity.getProperty(PROPERTY_AUTO_COMPLETE);
    assertThat(autoComplete).isNotNull();
    assertThat((Boolean) autoComplete).isTrue();
  }

}
