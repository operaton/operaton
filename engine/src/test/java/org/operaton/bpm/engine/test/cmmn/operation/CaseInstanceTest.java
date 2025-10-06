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
package org.operaton.bpm.engine.test.cmmn.operation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cmmn.behavior.StageActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionImpl;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnCaseInstance;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CaseDefinitionBuilder;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class CaseInstanceTest {

  /**
   *
   *   +-----------------+
   *   | Case1            \
   *   +-------------------+---+
   *   |                       |
   *   |     +-------+         |
   *   |     |   A   |         |
   *   |     +-------+         |
   *   |                       |
   *   +-----------------------+
   *
   */
  @Test
  void testCaseInstanceWithOneTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("create", stateTransitionCollector)
      .createActivity("A")
        .listener("create", stateTransitionCollector)
        .listener("enable", stateTransitionCollector)
        .listener("manualStart", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // create a new case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // expected state transitions after creation of a case instance:
    // ()        --create(Case1)--> active
    // ()        --create(A)-->     available
    // available --enable(A)-->     enabled
    List<String> expectedStateTransitions = new ArrayList<>();
    expectedStateTransitions.add("() --create(Case1)--> active");
    expectedStateTransitions.add("() --create(A)--> available");
    expectedStateTransitions.add("available --enable(A)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // case instance is active
    assertThat(caseInstance.isActive()).isTrue();

    CaseExecutionImpl instance = (CaseExecutionImpl) caseInstance;

    // case instance has one child plan item
    List<CaseExecutionImpl> childPlanItems = instance.getCaseExecutions();
    assertThat(childPlanItems).hasSize(1);

    CaseExecutionImpl planItemA = childPlanItems.get(0);

    // the child plan item is enabled
    assertThat(planItemA.isEnabled()).isTrue();

    // the parent of the child plan item is the case instance
    assertThat(planItemA.getParent()).isEqualTo(caseInstance);

    // manual start of A
    planItemA.manualStart();

    // expected state transition after manual start of A:
    // enabled --enable(A)--> active
    expectedStateTransitions.add("enabled --manualStart(A)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    assertThat(planItemA.isActive()).isTrue();
  }

  /**
  *
  *   +-----------------+
  *   | Case1            \
  *   +-------------------+-----------------+
  *   |                                     |
  *   |     +------------------------+      |
  *   |    / X                        \     |
  *   |   +    +-------+  +-------+    +    |
  *   |   |    |   A   |  |   B   |    |    |
  *   |   +    +-------+  +-------+    +    |
  *   |    \                          /     |
  *   |     +------------------------+      |
  *   |                                     |
  *   +-------------------------------------+
  *
  */
  @Test
  void testCaseInstanceWithOneState() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("create", stateTransitionCollector)
      .createActivity("X")
        .listener("create", stateTransitionCollector)
        .listener("enable", stateTransitionCollector)
        .listener("manualStart", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // expected state transitions after the creation of a case instance:
    // ()        --create(Case1)--> active
    // ()        --create(X)-->     available
    // available --enable(X)-->     enabled
    List<String> expectedStateTransitions = initAndAssertExpectedTransitions(stateTransitionCollector);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    CaseExecutionImpl planItemX = assertCaseXState(caseInstance);
    List<CaseExecutionImpl> childPlanItems;


    // manual start of x
    planItemX.manualStart();

    // X should be active
    assertThat(planItemX.isActive()).isTrue();

    // expected state transitions after a manual start of X:
    // enabled   --manualStart(X)--> active
    // ()        --create(A)-->      available
    // available --enable(A)-->      enabled
    // ()        --create(B)-->      available
    // available --enable(B)-->      enabled
    expectedStateTransitions.add("enabled --manualStart(X)--> active");
    expectedStateTransitions.add("() --create(A)--> available");
    expectedStateTransitions.add("available --enable(A)--> enabled");
    expectedStateTransitions.add("() --create(B)--> available");
    expectedStateTransitions.add("available --enable(B)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // X should have two chil plan items
    childPlanItems = planItemX.getCaseExecutions();
    assertThat(childPlanItems).hasSize(2);

    for (CmmnExecution childPlanItem : childPlanItems) {
      // both children should be enabled
      assertThat(childPlanItem.isEnabled()).isTrue();

      // manual start of a child
      childPlanItem.manualStart();

      // the child should be active
      assertThat(childPlanItem.isActive()).isTrue();

      // X should be the parent of both children
      assertThat(childPlanItem.getParent()).isEqualTo(planItemX);
    }

    // expected state transitions after the manual starts of A and B:
    // enabled   --manualStart(A)--> active
    // enabled   --manualStart(B)--> active
    expectedStateTransitions.add("enabled --manualStart(A)--> active");
    expectedStateTransitions.add("enabled --manualStart(B)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

  }

  protected CaseExecutionImpl assertCaseXState(CmmnCaseInstance caseInstance) {

    // case instance is active
    assertThat(caseInstance.isActive()).isTrue();

    CaseExecutionImpl instance = (CaseExecutionImpl) caseInstance;

    // case instance has one child plan item
    List<CaseExecutionImpl> childPlanItems = instance.getCaseExecutions();
    assertThat(childPlanItems).hasSize(1);

    CaseExecutionImpl planItemX = childPlanItems.get(0);

    // the case instance should be the parent of X
    assertThat(planItemX.getParent()).isEqualTo(caseInstance);

    // X should be enabled
    assertThat(planItemX.isEnabled()).isTrue();

    // before activation (ie. manual start) X should not have any children
    assertThat(planItemX.getCaseExecutions()).isEmpty();
    return planItemX;
  }

  @Test
  void testCaseInstanceWithOneStateWithoutManualStartOfChildren() {
    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
        .listener("create", stateTransitionCollector)
          .createActivity("X")
            .listener("create", stateTransitionCollector)
            .listener("enable", stateTransitionCollector)
            .listener("manualStart", stateTransitionCollector)
            .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
            .behavior(new StageActivityBehavior())
          .createActivity("A")
            .listener("create", stateTransitionCollector)
            .listener("start", stateTransitionCollector)
            .behavior(new TaskWaitState())
          .endActivity()
          .createActivity("B")
            .listener("create", stateTransitionCollector)
            .listener("start", stateTransitionCollector)
            .behavior(new TaskWaitState())
          .endActivity()
        .endActivity()
        .buildCaseDefinition();

    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();
    List<String> expectedStateTransitions = initAndAssertExpectedTransitions(stateTransitionCollector);
    emptyCollector(stateTransitionCollector, expectedStateTransitions);


    // clear lists
    CaseExecutionImpl planItemX = assertCaseXState(caseInstance);

    // manual start of x
    planItemX.manualStart();

    // X should be active
    assertThat(planItemX.isActive()).isTrue();

    // expected state transitions after a manual start of X:
    expectedStateTransitions.add("enabled --manualStart(X)--> active");
    expectedStateTransitions.add("() --create(A)--> available");
    expectedStateTransitions.add("available --start(A)--> active");
    expectedStateTransitions.add("() --create(B)--> available");
    expectedStateTransitions.add("available --start(B)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // X should have two chil plan items
    List<CaseExecutionImpl> childPlanItems;
    childPlanItems = planItemX.getCaseExecutions();
    assertThat(childPlanItems).hasSize(2);

    for (CmmnExecution childPlanItem : childPlanItems) {
      // both children should be active
      assertThat(childPlanItem.isActive()).isTrue();

      // X should be the parent of both children
      assertThat(childPlanItem.getParent()).isEqualTo(planItemX);
    }
  }

  protected void emptyCollector(CaseExecutionStateTransitionCollector stateTransitionCollector, List<String> expectedStateTransitions) {
    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();
  }

  protected List<String> initAndAssertExpectedTransitions(CaseExecutionStateTransitionCollector stateTransitionCollector) {
    // expected state transitions after the creation of a case instance:
    // ()        --create(Case1)--> active
    // ()        --create(X)-->     available
    // available --enable(X)-->     enabled
    List<String> expectedStateTransitions = new ArrayList<>();
    expectedStateTransitions.add("() --create(Case1)--> active");
    expectedStateTransitions.add("() --create(X)--> available");
    expectedStateTransitions.add("available --enable(X)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);
    return expectedStateTransitions;
  }


  /**
  *
  *   +-----------------+
  *   | Case1            \
  *   +-------------------+-------------------+
  *   |                                       |
  *   |  +-------+                            |
  *   |  |  A1   |                            |
  *   |  +-------+                            |
  *   |                                       |
  *   |    +------------------------+         |
  *   |   / X1                       \        |
  *   |  +    +-------+  +-------+    +       |
  *   |  |    |  A2   |  |  B1   |    |       |
  *   |  +    +-------+  +-------+    +       |
  *   |   \                          /        |
  *   |    +------------------------+         |
  *   |                                       |
  *   |    +-----------------------------+    |
  *   |   / Y                             \   |
  *   |  +    +-------+                    +  |
  *   |  |    |   C   |                    |  |
  *   |  |    +-------+                    |  |
  *   |  |                                 |  |
  *   |  |   +------------------------+    |  |
  *   |  |  / X2                       \   |  |
  *   |  | +    +-------+  +-------+    +  |  |
  *   |  | |    |  A3   |  |  B2   |    |  |  |
  *   |  | +    +-------+  +-------+    +  |  |
  *   |  |  \                          /   |  |
  *   |  +   +------------------------+    +  |
  *   |   \                               /   |
  *   |    +-----------------------------+    |
  *   |                                       |
  *   +---------------------------------------+
  *
  */
  @Test
  void testStartComplexCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("create", stateTransitionCollector)
      .createActivity("A1")
        .listener("create", stateTransitionCollector)
        .listener("enable", stateTransitionCollector)
        .listener("manualStart", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .createActivity("X1")
        .listener("create", stateTransitionCollector)
        .listener("enable", stateTransitionCollector)
        .listener("manualStart", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new StageActivityBehavior())
        .createActivity("A2")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B1")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .createActivity("Y")
        .listener("create", stateTransitionCollector)
        .listener("enable", stateTransitionCollector)
        .listener("manualStart", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new StageActivityBehavior())
        .createActivity("C")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("X2")
          .listener("create", stateTransitionCollector)
          .listener("enable", stateTransitionCollector)
          .listener("manualStart", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new StageActivityBehavior())
          .createActivity("A3")
            .listener("create", stateTransitionCollector)
            .listener("enable", stateTransitionCollector)
            .listener("manualStart", stateTransitionCollector)
            .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
            .behavior(new TaskWaitState())
          .endActivity()
          .createActivity("B2")
            .listener("create", stateTransitionCollector)
            .listener("enable", stateTransitionCollector)
            .listener("manualStart", stateTransitionCollector)
            .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
            .behavior(new TaskWaitState())
          .endActivity()
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // expected state transitions after the creation of a case instance:
    // ()        --create(Case1)--> active
    // ()        --create(A1)-->    available
    // available --enable(A1)-->    enabled
    // ()        --create(X1)-->    available
    // available --enable(X1)-->    enabled
    // ()        --create(Y)-->     available
    // available --enable(Y)-->     enabled
    List<String> expectedStateTransitions = new ArrayList<>();
    expectedStateTransitions.add("() --create(Case1)--> active");
    expectedStateTransitions.add("() --create(A1)--> available");
    expectedStateTransitions.add("available --enable(A1)--> enabled");
    expectedStateTransitions.add("() --create(X1)--> available");
    expectedStateTransitions.add("available --enable(X1)--> enabled");
    expectedStateTransitions.add("() --create(Y)--> available");
    expectedStateTransitions.add("available --enable(Y)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    CaseExecutionImpl instance = (CaseExecutionImpl) caseInstance;

    // the case instance should be active
    assertThat(instance.isActive()).isTrue();

    // the case instance should have three child plan items (A1, X1, Y)
    List<CaseExecutionImpl> childPlanItems = instance.getCaseExecutions();
    assertThat(childPlanItems).hasSize(3);

    // handle plan item A1 //////////////////////////////////////////////////

    CaseExecutionImpl planItemA1 = (CaseExecutionImpl) instance.findCaseExecution("A1");

    // case instance should be the parent of A1
    assertThat(planItemA1.getParent()).isEqualTo(caseInstance);

    // A1 should be enabled
    assertThat(planItemA1.isEnabled()).isTrue();

    // manual start of A1
    planItemA1.manualStart();

    // A1 should be active
    assertThat(planItemA1.isActive()).isTrue();

    // expected state transitions:
    // enabled --manualStart(A1)--> active
    expectedStateTransitions.add("enabled --manualStart(A1)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item X1 ///////////////////////////////////////////////////

    CaseExecutionImpl planItemX1 = (CaseExecutionImpl) instance.findCaseExecution("X1");

    // case instance should be the parent of X1
    assertThat(planItemX1.getParent()).isEqualTo(caseInstance);

    // X1 should be enabled
    assertThat(planItemX1.isEnabled()).isTrue();

    // manual start of X1
    planItemX1.manualStart();

    // X1 should be active
    assertThat(planItemX1.isActive()).isTrue();

    // X1 should have two children
    childPlanItems = planItemX1.getCaseExecutions();
    assertThat(childPlanItems).hasSize(2);

    // expected state transitions after manual start of X1:
    // enabled   --manualStart(X1)--> active
    // ()        --create(A2)-->      available
    // available --enable(A2)-->      enabled
    // ()        --create(B1)-->      available
    // available --enable(B1)-->      enabled
    expectedStateTransitions.add("enabled --manualStart(X1)--> active");
    expectedStateTransitions.add("() --create(A2)--> available");
    expectedStateTransitions.add("available --enable(A2)--> enabled");
    expectedStateTransitions.add("() --create(B1)--> available");
    expectedStateTransitions.add("available --enable(B1)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item A2 ////////////////////////////////////////////////

    CaseExecutionImpl planItemA2 = (CaseExecutionImpl) instance.findCaseExecution("A2");

    // X1 should be the parent of A2
    assertThat(planItemA2.getParent()).isEqualTo(planItemX1);

    // A2 should be enabled
    assertThat(planItemA2.isEnabled()).isTrue();

    // manual start of A2
    planItemA2.manualStart();

    // A2 should be active
    assertThat(planItemA2.isActive()).isTrue();

    // expected state transition after manual start of A2:
    // enabled --manualStart(A2)--> active
    expectedStateTransitions.add("enabled --manualStart(A2)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item B1 /////////////////////////////////////////////////

    CaseExecutionImpl planItemB1 = (CaseExecutionImpl) instance.findCaseExecution("B1");

    // X1 should be the parent of B1
    assertThat(planItemB1.getParent()).isEqualTo(planItemX1);

    // B1 should be enabled
    assertThat(planItemB1.isEnabled()).isTrue();

    // manual start of B1
    planItemB1.manualStart();

    // B1 should be active
    assertThat(planItemB1.isActive()).isTrue();

    // expected state transition after manual start of B1:
    // enabled --manualStart(B1)--> active
    expectedStateTransitions.add("enabled --manualStart(B1)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item Y ////////////////////////////////////////////////

    CaseExecutionImpl planItemY = (CaseExecutionImpl) instance.findCaseExecution("Y");

    // case instance should be the parent of Y
    assertThat(planItemY.getParent()).isEqualTo(caseInstance);

    // Y should be enabled
    assertThat(planItemY.isEnabled()).isTrue();

    // manual start of Y
    planItemY.manualStart();

    // Y should be active
    assertThat(planItemY.isActive()).isTrue();

    // Y should have two children
    childPlanItems = planItemY.getCaseExecutions();
    assertThat(childPlanItems).hasSize(2);

    // expected state transitions after manual start of Y:
    // enabled   --manualStart(Y)--> active
    // ()        --create(C)-->      available
    // available --enable(C)-->      enabled
    // ()        --create(X2)-->      available
    // available --enable(X2)-->      enabled
    expectedStateTransitions.add("enabled --manualStart(Y)--> active");
    expectedStateTransitions.add("() --create(C)--> available");
    expectedStateTransitions.add("available --enable(C)--> enabled");
    expectedStateTransitions.add("() --create(X2)--> available");
    expectedStateTransitions.add("available --enable(X2)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item C //////////////////////////////////////////////////

    CaseExecutionImpl planItemC = (CaseExecutionImpl) instance.findCaseExecution("C");

    // Y should be the parent of C
    assertThat(planItemC.getParent()).isEqualTo(planItemY);

    // C should be enabled
    assertThat(planItemC.isEnabled()).isTrue();

    // manual start of C
    planItemC.manualStart();

    // C should be active
    assertThat(planItemC.isActive()).isTrue();

    // expected state transition after manual start of C:
    // enabled --manualStart(C)--> active
    expectedStateTransitions.add("enabled --manualStart(C)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item X2 ///////////////////////////////////////////

    CaseExecutionImpl planItemX2 = (CaseExecutionImpl) instance.findCaseExecution("X2");

    // Y should be the parent of X2
    assertThat(planItemX2.getParent()).isEqualTo(planItemY);

    // X2 should be enabled
    assertThat(planItemX2.isEnabled()).isTrue();

    // manual start of X2
    planItemX2.manualStart();

    // X2 should be active
    assertThat(planItemX2.isActive()).isTrue();

    // X2 should have two children
    childPlanItems = planItemX2.getCaseExecutions();
    assertThat(childPlanItems).hasSize(2);

    // expected state transitions after manual start of X2:
    // enabled   --manualStart(X2)--> active
    // ()        --create(A3)-->      available
    // available --enable(A3)-->      enabled
    // ()        --create(B2)-->      available
    // available --enable(B2)-->      enabled
    expectedStateTransitions.add("enabled --manualStart(X2)--> active");
    expectedStateTransitions.add("() --create(A3)--> available");
    expectedStateTransitions.add("available --enable(A3)--> enabled");
    expectedStateTransitions.add("() --create(B2)--> available");
    expectedStateTransitions.add("available --enable(B2)--> enabled");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item A3 //////////////////////////////////////////////

    CaseExecutionImpl planItemA3 = (CaseExecutionImpl) instance.findCaseExecution("A3");

    // A3 should be the parent of X2
    assertThat(planItemA3.getParent()).isEqualTo(planItemX2);

    // A3 should be enabled
    assertThat(planItemA3.isEnabled()).isTrue();

    // manual start of A3
    planItemA3.manualStart();

    // A3 should be active
    assertThat(planItemA3.isActive()).isTrue();

    // expected state transition after manual start of A3:
    // enabled --manualStart(A3)--> active
    expectedStateTransitions.add("enabled --manualStart(A3)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

    // handle plan item B2 /////////////////////////////////////////////////

    CaseExecutionImpl planItemB2 = (CaseExecutionImpl) instance.findCaseExecution("B2");

    // B2 should be the parent of X2
    assertThat(planItemB2.getParent()).isEqualTo(planItemX2);

    // B2 should be enabled
    assertThat(planItemB2.isEnabled()).isTrue();

    // manual start of B2
    planItemB2.manualStart();

    // B2 should be active
    assertThat(planItemB2.isActive()).isTrue();

    // expected state transition after manual start of B2:
    // enabled --manualStart(B2)--> active
    expectedStateTransitions.add("enabled --manualStart(B2)--> active");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    emptyCollector(stateTransitionCollector, expectedStateTransitions);

  }

}
