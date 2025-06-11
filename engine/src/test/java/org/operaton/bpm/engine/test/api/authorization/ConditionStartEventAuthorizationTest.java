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
package org.operaton.bpm.engine.test.api.authorization;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.ConditionEvaluationBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionStartEventAuthorizationTest extends AuthorizationTest {

  private static final String SINGLE_CONDITIONAL_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testSingleConditionalStartEvent1.bpmn20.xml";
  private static final String TRUE_CONDITIONAL_XML = "org/operaton/bpm/engine/test/bpmn/event/conditional/ConditionalStartEventTest.testStartInstanceWithTrueConditionalStartEvent.bpmn20.xml";
  protected static final String PROCESS_KEY = "conditionalEventProcess";
  protected static final String PROCESS_KEY_TWO = "trueConditionProcess";

  @Deployment(resources = {SINGLE_CONDITIONAL_XML, TRUE_CONDITIONAL_XML})
  @Test
  void testWithAllPermissions() {
    // given two deployed processes with conditional start event

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY_TWO, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 42)
        .evaluateStartConditions();

    // then
    assertThat(instances).hasSize(1);
  }

  @Deployment(resources = {SINGLE_CONDITIONAL_XML})
  @Test
  void testWithoutProcessDefinitionPermissions() {
    // given deployed process with conditional start event

    // user does not have process definition permissions
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    ConditionEvaluationBuilder evaluationBuilder = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 42);
    assertThatThrownBy(evaluationBuilder::evaluateStartConditions)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("No subscriptions were found during evaluation of the conditional start events.");
  }

  @Deployment(resources = {SINGLE_CONDITIONAL_XML})
  @Test
  void testWithoutCreateInstancePermissions() {
    // given deployed process with conditional start event

    // user does not have process definition CREATE_INSTANCE permissions
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    ConditionEvaluationBuilder evaluationBuilder = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 42);
    assertThatThrownBy(evaluationBuilder::evaluateStartConditions)
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("The user with id 'test' does not have 'CREATE_INSTANCE' permission on resource 'conditionalEventProcess' of type 'ProcessDefinition'.");
  }

  @SuppressWarnings("GrazieInspection")
  @Deployment(resources = {SINGLE_CONDITIONAL_XML})
  @Test
  void testWithoutProcessInstancePermission() {
    // given deployed process with conditional start event

    // the user doesn't have CREATE permission for process instances
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ, CREATE_INSTANCE);

    // when
    var evaluationBuilder = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 42);
    assertThatThrownBy(evaluationBuilder::evaluateStartConditions)
        .isInstanceOf(AuthorizationException.class)
        .hasMessageContaining("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'.");
  }

  @Deployment(resources = {SINGLE_CONDITIONAL_XML})
  @Test
  void testWithRevokeAuthorizations() {
    // given deployed process with conditional start event

    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, CREATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    // when
    var evaluationBuilder = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 42);
    assertThatThrownBy(evaluationBuilder::evaluateStartConditions)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("No subscriptions were found during evaluation of the conditional start events.");
  }
}
