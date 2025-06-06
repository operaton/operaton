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
import org.operaton.bpm.engine.impl.cmmn.handler.ProcessTaskItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.test.cmmn.handler.specification.AbstractExecutionListenerSpec;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
public class ProcessTaskPlanItemExecutionListenerHandlerTest extends CmmnElementHandlerTest {

  @Parameters(name = "testListener: {0}")
  public static Iterable<Object[]> data() {
    return ExecutionListenerCases.TASK_OR_STAGE_CASES;
  }

  protected ProcessTask processTask;
  protected PlanItem planItem;
  protected ProcessTaskItemHandler handler = new ProcessTaskItemHandler();

  protected AbstractExecutionListenerSpec testSpecification;

  public ProcessTaskPlanItemExecutionListenerHandlerTest(AbstractExecutionListenerSpec testSpecification) {
    this.testSpecification = testSpecification;
  }

  @BeforeEach
  void setUp() {
    processTask = createElement(casePlanModel, "aProcessTask", ProcessTask.class);

    planItem = createElement(casePlanModel, "PI_aProcessTask", PlanItem.class);
    planItem.setDefinition(processTask);

  }

  @TestTemplate
  void testCaseExecutionListener() {
    // given:
    testSpecification.addListenerToElement(modelInstance, processTask);

    // when
    CmmnActivity activity = handler.handleElement(planItem, context);

    // then
    testSpecification.verify(activity);
  }

}
