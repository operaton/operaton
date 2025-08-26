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
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionImpl;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnCaseInstance;
import org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CaseDefinitionBuilder;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
class CaseExecutionCompletionTest {

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
  void testCompleteActiveTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // when

    // completing task A
    taskA.complete();

    // then
    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --complete(A)--> completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // task A is completed ...
    assertThat(taskA.isCompleted()).isTrue();
    // ... and the case instance is also completed
    assertThat(caseInstance.isCompleted()).isTrue();

    // task A is not part of the case instance anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // the case instance has no children
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();
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
  void testManualCompleteActiveTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // when

    // completing task A
    taskA.manualComplete();

    // then
    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --complete(A)--> completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // task A is completed ...
    assertThat(taskA.isCompleted()).isTrue();
    // ... and the case instance is also completed
    assertThat(caseInstance.isCompleted()).isTrue();

    // task A is not part of the case instance anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // the case instance has no children
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();
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
  void testCompleteEnabledTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    try {
      // when
      // completing task A
      taskA.complete();
      fail("It should not be possible to complete an enabled task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still enabled
      assertThat(taskA.isEnabled()).isTrue();
    }

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
  void testManualCompleteEnabledTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    try {
      // when
      // completing task A
      taskA.manualComplete();
      fail("It should not be possible to complete an enabled task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still enabled
      assertThat(taskA.isEnabled()).isTrue();
    }

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
  void testCompleteAlreadyCompletedTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    taskA.complete();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    try {
      // when
      // complete A
      taskA.complete();
      fail("It should not be possible to complete an already completed task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still completed
      assertThat(taskA.isCompleted()).isTrue();
    }

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
  void testManualCompleteAlreadyCompletedTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    taskA.complete();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    try {
      // when
      // complete A
      taskA.manualComplete();
      fail("It should not be possible to complete an already completed task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still completed
      assertThat(taskA.isCompleted()).isTrue();
    }

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
  void testCompleteTerminatedTask() {
    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    taskA.terminate();

    // task A is completed
    assertThat(taskA.isTerminated()).isTrue();

    try {
      // when
      // complete A
      taskA.complete();
      fail("It should not be possible to complete an already completed task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still completed
      assertThat(taskA.isTerminated()).isTrue();
    }
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
  void testManualCompleteTerminatedTask() {
    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");
    taskA.terminate();

    // task A is completed
    assertThat(taskA.isTerminated()).isTrue();

    try {
      // when
      // complete A
      taskA.manualComplete();
      fail("It should not be possible to complete an already completed task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // task A is still completed
      assertThat(taskA.isTerminated()).isTrue();
    }
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
  void testDisableTaskShouldCompleteCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("disable", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    // when
    // complete A
    taskA.disable();

    // then

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // enabled --disable(A)-->      disabled
    // active  --complete(Case1)--> completed
    expectedStateTransitions.add("enabled --disable(A)--> disabled");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // task A is disabled
    assertThat(taskA.isDisabled()).isTrue();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

    assertThat(caseInstance.findCaseExecution("A")).isNull();
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

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
  void testTerminateTaskShouldCompleteCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("terminate", stateTransitionCollector)
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is active
    assertThat(taskA.isActive()).isTrue();

    // when
    // terminate A
    taskA.terminate();

    // then

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --terminate(A)-->    terminated
    // active  --complete(Case1)--> completed
    expectedStateTransitions.add("active --terminate(A)--> terminated");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // task A is disabled
    assertThat(taskA.isTerminated()).isTrue();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

    assertThat(caseInstance.findCaseExecution("A")).isNull();
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

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
  void testCompleteActiveCaseInstanceWithEnabledTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    try {
      // when
      // complete caseInstance
      caseInstance.complete();
    } catch (Exception e) {
      // then
      // case instance is still active
      assertThat(caseInstance.isActive()).isTrue();

      assertThat(caseInstance.findCaseExecution("A")).isNotNull();
    }
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
  void testManualCompleteActiveCaseInstanceWithEnabledTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    // when

    // complete caseInstance (manualCompletion == true)
    caseInstance.manualComplete();

    // then

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // the case instance
    assertThat(caseInstance.isCompleted()).isTrue();

    // task A is not a child of the case instance anymore
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
  void testCompleteActiveCaseInstanceWithActiveTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is active
    assertThat(taskA.isActive()).isTrue();

    try {
      // when
      caseInstance.complete();
      fail("It should not be possible to complete a case instance containing an active task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // the case instance is still active
      assertThat(caseInstance.isActive()).isTrue();
      assertThat(caseInstance.isCompleted()).isFalse();
    }
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
  void testManualCompleteActiveCaseInstanceWithActiveTask() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");
    assertThat(taskA).isNotNull();

    try {
      // when
      caseInstance.manualComplete();
      fail("It should not be possible to complete a case instance containing an active task.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // the case instance is still active
      assertThat(caseInstance.isActive()).isTrue();
      assertThat(caseInstance.isCompleted()).isFalse();
    }
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
  void testCompleteAlreadyCompletedCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    // case instance is already completed
    caseInstance.manualComplete();

    try {
      // when
      caseInstance.complete();
      fail("It should not be possible to complete an already completed case instance.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      // the case instance is still completed
      assertThat(caseInstance.isCompleted()).isTrue();
    }

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
  void testManualCompleteAlreadyCompletedCaseInstance() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("A")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new TaskWaitState())
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // task A as a child of the case instance
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // task A is enabled
    assertThat(taskA.isEnabled()).isTrue();

    // case instance is already completed
    caseInstance.manualComplete();

    try {
      // when
      caseInstance.manualComplete();
      fail("It should not be possible to complete an already completed case instance.");
    } catch (CaseIllegalStateTransitionException e) {
      // then

      assertThat(caseInstance.isCompleted()).describedAs("the case instance is still completed").isTrue();
    }

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
  void testCompleteOnlyTaskA() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // complete task A
    taskA.complete();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(A)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    // task B is still active
    assertThat(taskB.isActive()).isTrue();

    // stage X is still active
    assertThat(stageX.isActive()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();

    // task B is still part of the case instance
    assertThat(caseInstance.findCaseExecution("B")).isNotNull();

    // stage X is still part of the case instance
    assertThat(caseInstance.findCaseExecution("X")).isNotNull();

    // case instance has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).hasSize(1);

    // stage X has two children
    assertThat(((CaseExecutionImpl) stageX).getCaseExecutions()).hasSize(1);

    // case instance is still active
    assertThat(caseInstance.isActive()).isTrue();

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
  void testManualCompleteOnlyTaskA() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // complete task A
    taskA.manualComplete();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(A)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    // task B is still active
    assertThat(taskB.isActive()).isTrue();

    // stage X is still active
    assertThat(stageX.isActive()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();

    // task B is still part of the case instance
    assertThat(caseInstance.findCaseExecution("B")).isNotNull();

    // stage X is still part of the case instance
    assertThat(caseInstance.findCaseExecution("X")).isNotNull();

    // case instance has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).hasSize(1);

    // stage X has two children
    assertThat(((CaseExecutionImpl) stageX).getCaseExecutions()).hasSize(1);

    // case instance is still active
    assertThat(caseInstance.isActive()).isTrue();

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
  void testDisableOnlyTaskA() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // disable task A
    taskA.disable();

    // then ////////////////////////////////////////////////////////////////

    assertThat(stateTransitionCollector.stateTransitions).isEmpty();

    // task A is disabled
    assertThat(taskA.isDisabled()).isTrue();

    // task B is still active
    assertThat(taskB.isActive()).isTrue();

    // stage X is still active
    assertThat(stageX.isActive()).isTrue();

    // task B is still part of the case instance
    assertThat(caseInstance.findCaseExecution("A")).isNotNull();

    // task B is still part of the case instance
    assertThat(caseInstance.findCaseExecution("B")).isNotNull();

    // stage X is still part of the case instance
    assertThat(caseInstance.findCaseExecution("X")).isNotNull();

    // case instance has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).hasSize(1);

    // stage X has only one child
    assertThat(((CaseExecutionImpl) stageX).getCaseExecutions()).hasSize(2);

    // case instance is still active
    assertThat(caseInstance.isActive()).isTrue();

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
  void testTerminateOnlyTaskA() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // complete task A
    taskA.terminate();

    // then ////////////////////////////////////////////////////////////////

    assertThat(stateTransitionCollector.stateTransitions).isEmpty();

    // task A is terminated
    assertThat(taskA.isTerminated()).isTrue();

    // task B is still active
    assertThat(taskB.isActive()).isTrue();

    // stage X is still active
    assertThat(stageX.isActive()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();

    // task B is still part of the case instance
    assertThat(caseInstance.findCaseExecution("B")).isNotNull();

    // stage X is still part of the case instance
    assertThat(caseInstance.findCaseExecution("X")).isNotNull();

    // case instance has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).hasSize(1);

    // stage X has only one child
    assertThat(((CaseExecutionImpl) stageX).getCaseExecutions()).hasSize(1);

    // case instance is still active
    assertThat(caseInstance.isActive()).isTrue();

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
  void testCompleteTaskAAndTaskB() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // complete task A
    taskA.complete();
    // complete task B
    taskB.complete();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(A)-->     completed
    // active --complete(B)-->     completed
    // active --complete(X)-->     completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");
    expectedStateTransitions.add("active --complete(B)--> completed");
    expectedStateTransitions.add("active --complete(X)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    // task B is completed
    assertThat(taskB.isCompleted()).isTrue();

    // stage X is completed
    assertThat(stageX.isCompleted()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // stage X does not contain task B anymore
    assertThat(caseInstance.findCaseExecution("B")).isNull();
    // stage X does not contain task X anymore
    assertThat(caseInstance.findCaseExecution("X")).isNull();

    // stage X has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

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
  void testManualCompleteTaskAAndTaskB() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // complete task A
    taskA.manualComplete();
    // complete task B
    taskB.manualComplete();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(A)-->     completed
    // active --complete(B)-->     completed
    // active --complete(X)-->     completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(A)--> completed");
    expectedStateTransitions.add("active --complete(B)--> completed");
    expectedStateTransitions.add("active --complete(X)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is completed
    assertThat(taskA.isCompleted()).isTrue();

    // task B is completed
    assertThat(taskB.isCompleted()).isTrue();

    // stage X is completed
    assertThat(stageX.isCompleted()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // stage X does not contain task B anymore
    assertThat(caseInstance.findCaseExecution("B")).isNull();
    // stage X does not contain task X anymore
    assertThat(caseInstance.findCaseExecution("X")).isNull();

    // stage X has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

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
  void testDisableTaskAAndTaskB() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // disable task A
    taskA.disable();
    // disable task B
    taskB.disable();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(X)-->     completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(X)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is disabled
    assertThat(taskA.isDisabled()).isTrue();

    // task B is disabled
    assertThat(taskB.isDisabled()).isTrue();

    // stage X is completed
    assertThat(stageX.isCompleted()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // stage X does not contain task B anymore
    assertThat(caseInstance.findCaseExecution("B")).isNull();
    // stage X does not contain task X anymore
    assertThat(caseInstance.findCaseExecution("X")).isNull();

    // stage X has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

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
  void testTerminateTaskAAndTaskB() {

    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given ///////////////////////////////////////////////////////////////

    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .behavior(new StageActivityBehavior())
        .createActivity("A")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
        .createActivity("B")
          .listener("complete", stateTransitionCollector)
          .behavior(new TaskWaitState())
        .endActivity()
      .endActivity()
      .buildCaseDefinition();

    // an active case instance
    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // a case execution associated with Stage X
    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // a case execution associated with Task A
    CmmnActivityExecution taskA = caseInstance.findCaseExecution("A");

    // a case execution associated with Task B
    CmmnActivityExecution taskB = caseInstance.findCaseExecution("B");

    // when ////////////////////////////////////////////////////////////////

    // terminate task A
    taskA.terminate();
    // terminate task B
    taskB.terminate();

    // then ////////////////////////////////////////////////////////////////

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transitions:
    // active --complete(X)-->     completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(X)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);

    // clear lists
    expectedStateTransitions.clear();
    stateTransitionCollector.stateTransitions.clear();

    // task A is terminated
    assertThat(taskA.isTerminated()).isTrue();

    // task B is terminated
    assertThat(taskB.isTerminated()).isTrue();

    // stage X is completed
    assertThat(stageX.isCompleted()).isTrue();

    // stage X does not contain task A anymore
    assertThat(caseInstance.findCaseExecution("A")).isNull();
    // stage X does not contain task B anymore
    assertThat(caseInstance.findCaseExecution("B")).isNull();
    // stage X does not contain task X anymore
    assertThat(caseInstance.findCaseExecution("X")).isNull();

    // stage X has only one child
    assertThat(((CaseExecutionImpl) caseInstance).getCaseExecutions()).isEmpty();

    // case instance is completed
    assertThat(caseInstance.isCompleted()).isTrue();

  }

  /**
   *
   *   +-----------------+
   *   | Case1            \
   *   +-------------------+---+
   *   |                       |
   *   |                       |
   *   |                       |
   *   |                       |
   *   |                       |
   *   +-----------------------+
   *
   */
  @Test
  void testAutoCompletionCaseInstanceWithoutChildren() {
    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .buildCaseDefinition();

    // when

    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();

    // then

    assertThat(caseInstance.isCompleted()).isTrue();

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);
  }

  /**
   *
   *   +-----------------+
   *   | Case1            \
   *   +-------------------+-----------------+
   *   |                                     |
   *   |     +------------------------+      |
   *   |    / X                        \     |
   *   |   +                            +    |
   *   |   |                            |    |
   *   |   +                            +    |
   *   |    \                          /     |
   *   |     +------------------------+      |
   *   |                                     |
   *   +-------------------------------------+
   *
   */
  @Test
  void testAutoCompletionStageWithoutChildren() {
    CaseExecutionStateTransitionCollector stateTransitionCollector = new CaseExecutionStateTransitionCollector();

    // given
    // a case definition
    CmmnCaseDefinition caseDefinition = new CaseDefinitionBuilder("Case1")
      .listener("complete", stateTransitionCollector)
      .createActivity("X")
        .listener("complete", stateTransitionCollector)
        .property(ItemHandler.PROPERTY_MANUAL_ACTIVATION_RULE, TestHelper.defaultManualActivation())
        .behavior(new StageActivityBehavior())
      .endActivity()
      .buildCaseDefinition();

    CmmnCaseInstance caseInstance = caseDefinition.createCaseInstance();
    caseInstance.create();


    CmmnActivityExecution stageX = caseInstance.findCaseExecution("X");

    // when
    stageX.manualStart();

    // then

    assertThat(caseInstance.isCompleted()).isTrue();
    assertThat(stageX.isCompleted()).isTrue();

    List<String> expectedStateTransitions = new ArrayList<>();

    // expected state transition:
    // active --complete(X)-->     completed
    // active --complete(Case1)--> completed
    expectedStateTransitions.add("active --complete(X)--> completed");
    expectedStateTransitions.add("active --complete(Case1)--> completed");

    assertThat(stateTransitionCollector.stateTransitions).isEqualTo(expectedStateTransitions);
  }

}
