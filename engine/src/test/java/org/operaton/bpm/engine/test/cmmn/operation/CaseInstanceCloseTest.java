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

import org.operaton.bpm.engine.exception.cmmn.CaseIllegalStateTransitionException;
import org.operaton.bpm.engine.impl.cmmn.behavior.StageActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnCaseInstance;
import org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CaseDefinitionBuilder;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class CaseInstanceCloseTest {

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
  void testCloseCompletedCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("A")
        .behavior(new TaskWaitState())
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // disable task A -> completes case instance
    taskA.disable();

    assertThat(caseInstance.isCompleted()).isTrue();

    // when

    // close case
    caseInstance.close();

    // then
    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // completed --close(Case1)--> closed
    expectedStateTransitions.add("completed --close(Case1)--> closed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    assertThat(caseInstance.isClosed()).isTrue();
  }

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
  void testCloseTerminatedCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("A")
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    caseInstance.terminate();
    assertThat(caseInstance.isTerminated()).isTrue();

    // when

    // close case
    caseInstance.close();

    // then
    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // terminated --close(Case1)--> closed
    expectedStateTransitions.add("terminated --close(Case1)--> closed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    assertThat(caseInstance.isClosed()).isTrue();
  }

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
  void testCloseSuspendedCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("A")
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    caseInstance.suspend();
    assertThat(caseInstance.isSuspended()).isTrue();

    // when

    // close case
    caseInstance.close();

    // then
    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // suspended --close(Case1)--> closed
    expectedStateTransitions.add("suspended --close(Case1)--> closed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    assertThat(caseInstance.isClosed()).isTrue();

    assertThat(caseInstance.findCaseExecution("A")).isNull();
  }


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
  void testCloseActiveCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("A")
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    assertThat(caseInstance.isActive()).isTrue();

    assertThatThrownBy(caseInstance::close)
      .isInstanceOf(CaseIllegalStateTransitionException.class)
      .hasMessageContaining("The case instance must be in state '[completed|terminated|suspended]' to close it, but the state is 'active'.");

    // then
    assertThat(stateTransitionCollector.stateTransitions).isEmpty();

    assertThat(caseInstance.isActive()).isTrue();

    assertThat(caseInstance.findCaseExecution("A")).isNotNull();
  }

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
  void testCloseTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("A")
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    assertThatThrownBy(taskA::close).isInstanceOf(CaseIllegalStateTransitionException.class);

    // then
    assertThat(stateTransitionCollector.stateTransitions).isEmpty();

    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.findCaseExecution("A")).isNotNull();
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
  void testCloseStage() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("close", stateTransitionCollector)
      .createActivity("X")
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    assertThatThrownBy(stageX::close).isInstanceOf(CaseIllegalStateTransitionException.class);

    // then
    assertThat(stateTransitionCollector.stateTransitions).isEmpty();

    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.findCaseExecution("X")).isNotNull();
  }
}
