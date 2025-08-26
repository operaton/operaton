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
package org.operaton.bpm.engine.test.cmmn.required;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
class RequiredRuleTest extends CmmnTest {

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/required/RequiredRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testRequiredRuleEvaluatesToTrue() {
    CaseInstance caseInstance =
        caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("required", true));
    var caseInstanceId = caseInstance.getId();

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isTrue();

    try {
      caseService.completeCaseExecution(caseInstanceId);
      fail("completing the containing stage should not be allowed");
    } catch (NotAllowedException e) {
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/required/RequiredRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testRequiredRuleEvaluatesToFalse() {
    CaseInstance caseInstance =
        caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("required", false));

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isFalse();

    // completing manually should be allowed
    caseService.completeCaseExecution(caseInstance.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/required/RequiredRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultRequiredRuleEvaluatesToTrue() {
    CaseInstance caseInstance =
        caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("required", true));
    var caseInstanceId = caseInstance.getId();

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isTrue();

    try {
      caseService.completeCaseExecution(caseInstanceId);
      fail("completing the containing stage should not be allowed");
    } catch (NotAllowedException e) {
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/required/RequiredRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultRequiredRuleEvaluatesToFalse() {
    CaseInstance caseInstance =
        caseService.createCaseInstanceByKey("case", Collections.<String, Object>singletonMap("required", false));

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isFalse();

    // completing manually should be allowed
    caseService.completeCaseExecution(caseInstance.getId());
  }

  @Deployment
  @Test
  void testDefaultRequiredRuleWithoutConditionEvaluatesToTrue() {
    caseService.createCaseInstanceByKey("case");

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isTrue();
  }

  @Deployment
  @Test
  void testDefaultRequiredRuleWithEmptyConditionEvaluatesToTrue() {
    caseService.createCaseInstanceByKey("case");

    CaseExecution taskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isTrue();
  }
}
