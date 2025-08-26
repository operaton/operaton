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

import org.operaton.bpm.engine.impl.cmmn.handler.HumanTaskItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.test.cmmn.handler.specification.AbstractExecutionListenerSpec;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.PlanItem;

import static org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.*;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
public class HumanTaskPlanItemExecutionListenerHandlerTest extends CmmnElementHandlerTest {

  @Parameters(name = "testListener: {0}")
  public static Iterable<Object[]> data() {
    return ExecutionListenerCases.TASK_OR_STAGE_CASES;
  }

  protected HumanTask humanTask;
  protected PlanItem planItem;
  protected HumanTaskItemHandler handler = new HumanTaskItemHandler();

  protected AbstractExecutionListenerSpec testSpecification;

  public HumanTaskPlanItemExecutionListenerHandlerTest(AbstractExecutionListenerSpec testSpecification) {
    this.testSpecification = testSpecification;
  }

  @BeforeEach
  void setUp() {
    humanTask = createElement(casePlanModel, "aHumanTask", HumanTask.class);

    planItem = createElement(casePlanModel, "PI_aHumanTask", PlanItem.class);
    planItem.setDefinition(humanTask);

  }

  @TestTemplate
  void testCaseExecutionListener() {
    // given:
    testSpecification.addListenerToElement(modelInstance, humanTask);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    testSpecification.verify(activity);
  }

}
