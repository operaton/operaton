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
package org.operaton.bpm.engine.test.cmmn.activation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.jupiter.api.Test;

/**
 * @author Thorben Lindhauer
 *
 */
class ManualActivationRuleTest extends CmmnTest {

  /**
   * CAM-3170
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testManualActivationRuleEvaluatesToTrue() {
    caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("manualActivation", true));

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isTrue();
    assertThat(taskExecution.isActive()).isFalse();
  }

  /**
   * CAM-3170
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testManualActivationRuleEvaluatesToFalse() {
    caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("manualActivation", false));

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isFalse();
    assertThat(taskExecution.isActive()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultManualActivationRuleEvaluatesToTrue() {
    caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("manualActivation", true));

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isTrue();
    assertThat(taskExecution.isActive()).isFalse();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultManualActivationRuleEvaluatesToFalse() {
    caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("manualActivation", false));

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isFalse();
    assertThat(taskExecution.isActive()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testActivationWithoutDefinition.cmmn")
  @Test
  void testActivationWithoutManualActivationDefined() {
    caseService.createCaseInstanceByKey("case");

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isFalse();
    assertThat(taskExecution.isActive()).describedAs("Human Task is active, when ManualActivation is omitted").isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testActivationWithoutManualActivationExpressionDefined.cmmn")
  @Test
  void testActivationWithoutManualActivationExpressionDefined() {
    caseService.createCaseInstanceByKey("case");

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isTrue();
    assertThat(taskExecution.isActive()).describedAs("Human Task is not active, when ManualActivation's condition is empty").isFalse();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/activation/ManualActivationRuleTest.testActivationWithoutManualActivationConditionDefined.cmmn")
  @Test
  void testActivationWithoutManualActivationConditionDefined() {
    caseService.createCaseInstanceByKey("case");

    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isTrue();
    assertThat(taskExecution.isActive()).describedAs("Human Task is not active, when ManualActivation's condition is empty").isFalse();
  }

}
