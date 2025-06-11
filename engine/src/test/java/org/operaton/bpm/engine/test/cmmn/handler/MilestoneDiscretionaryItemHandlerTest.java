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

import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_DESCRIPTION;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_ACTIVITY_TYPE;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_DISCRETIONARY;
import static org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler.PROPERTY_REQUIRED_RULE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.operaton.bpm.engine.impl.cmmn.CaseControlRule;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.behavior.MilestoneActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.handler.MilestoneItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.DefaultControl;
import org.operaton.bpm.model.cmmn.instance.DiscretionaryItem;
import org.operaton.bpm.model.cmmn.instance.ItemControl;
import org.operaton.bpm.model.cmmn.instance.Milestone;
import org.operaton.bpm.model.cmmn.instance.PlanItemControl;
import org.operaton.bpm.model.cmmn.instance.PlanningTable;
import org.operaton.bpm.model.cmmn.instance.RequiredRule;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Smirnov
 *
 */
class MilestoneDiscretionaryItemHandlerTest extends CmmnElementHandlerTest {

  protected Milestone milestone;
  protected PlanningTable planningTable;
  protected DiscretionaryItem discretionaryItem;
  protected MilestoneItemHandler handler = new MilestoneItemHandler();

  @BeforeEach
  void setUp() {
    milestone = createElement(casePlanModel, "aMilestone", Milestone.class);

    planningTable = createElement(casePlanModel, "aPlanningTable", PlanningTable.class);

    discretionaryItem = createElement(planningTable, "DI_aMilestone", DiscretionaryItem.class);
    discretionaryItem.setDefinition(milestone);

  }

  @Test
  void testMilestoneActivityName() {
    // given:
    // the Milestone has a name "A Milestone"
    String name = "A Milestone";
    milestone.setName(name);

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    assertThat(activity.getName()).isEqualTo(name);
  }

  @Test
  void testMilestoneActivityType() {
    // given

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    String activityType = (String) activity.getProperty(PROPERTY_ACTIVITY_TYPE);
    assertThat(activityType).isEqualTo("milestone");
  }

  @Test
  @SuppressWarnings("deprecation")
  void testMilestoneDescription() {
    // given
    String description = "This is a milestone";
    milestone.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  @SuppressWarnings("deprecation")
  void testDiscretionaryItemDescription() {
    // given
    String description = "This is a discretionaryItem";
    discretionaryItem.setDescription(description);

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    assertThat(activity.getProperty(PROPERTY_ACTIVITY_DESCRIPTION)).isEqualTo(description);
  }

  @Test
  void testActivityBehavior() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    assertThat(behavior).isInstanceOf(MilestoneActivityBehavior.class);
  }

  @Test
  void testIsDiscretionaryProperty() {
    // given:
    // a discretionary item to handle

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    Boolean discretionary = (Boolean) activity.getProperty(PROPERTY_DISCRETIONARY);
    assertThat(discretionary).isTrue();
  }

  @Test
  void testWithoutParent() {
    // given: a planItem

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

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
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    assertThat(activity.getParent()).isEqualTo(parent);
    assertThat(parent.getActivities()).contains(activity);
  }

  @Test
  void testRequiredRule() {
    // given
    ItemControl itemControl = createElement(discretionaryItem, "ItemControl_1", ItemControl.class);
    RequiredRule requiredRule = createElement(itemControl, "RequiredRule_1", RequiredRule.class);
    ConditionExpression expression = createElement(requiredRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(discretionaryItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REQUIRED_RULE);
    assertThat(rule).isInstanceOf(CaseControlRule.class);
  }

  @Test
  void testRequiredRuleByDefaultPlanItemControl() {
    // given
    PlanItemControl defaultControl = createElement(milestone, "ItemControl_1", DefaultControl.class);
    RequiredRule requiredRule = createElement(defaultControl, "RequiredRule_1", RequiredRule.class);
    ConditionExpression expression = createElement(requiredRule, "Expression_1", ConditionExpression.class);
    expression.setText("${true}");

    Cmmn.validateModel(modelInstance);

    // when
    CmmnActivity newActivity = handler.handleElement(discretionaryItem, context);

    // then
    Object rule = newActivity.getProperty(PROPERTY_REQUIRED_RULE);
    assertThat(rule).isInstanceOf(CaseControlRule.class);
  }

}
