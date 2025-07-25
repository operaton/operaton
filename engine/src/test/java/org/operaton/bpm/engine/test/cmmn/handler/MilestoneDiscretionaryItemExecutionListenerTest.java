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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.impl.cmmn.handler.MilestoneItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.test.cmmn.handler.specification.AbstractExecutionListenerSpec;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.model.cmmn.instance.DiscretionaryItem;
import org.operaton.bpm.model.cmmn.instance.Milestone;
import org.operaton.bpm.model.cmmn.instance.PlanningTable;

import static org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.*;

/**
 * @author Roman Smirnov
 *
 */
@Parameterized
public class MilestoneDiscretionaryItemExecutionListenerTest extends CmmnElementHandlerTest {

  @Parameters(name = "testListener: {0}")
  public static Iterable<Object[]> data() {
    return ExecutionListenerCases.EVENTLISTENER_OR_MILESTONE_CASES;
  }

  protected Milestone milestone;
  protected PlanningTable planningTable;
  protected DiscretionaryItem discretionaryItem;
  protected MilestoneItemHandler handler = new MilestoneItemHandler();

  protected AbstractExecutionListenerSpec testSpecification;

  public MilestoneDiscretionaryItemExecutionListenerTest(AbstractExecutionListenerSpec testSpecification) {
    this.testSpecification = testSpecification;
  }

  @BeforeEach
  void setUp() {
    milestone = createElement(casePlanModel, "aMilestone", Milestone.class);

    planningTable = createElement(casePlanModel, "aPlanningTable", PlanningTable.class);

    discretionaryItem = createElement(planningTable, "DI_aMilestone", DiscretionaryItem.class);
    discretionaryItem.setDefinition(milestone);

  }

  @TestTemplate
  void testCaseExecutionListener() {
    // given:
    testSpecification.addListenerToElement(modelInstance, milestone);

    // when
    CmmnActivity activity = handler.handleElement(discretionaryItem, context);

    // then
    testSpecification.verify(activity);
  }

}
