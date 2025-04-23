/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.cmmn.decisiontask;

import org.junit.jupiter.api.AfterEach;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.IntegerValue;
import org.operaton.bpm.engine.variable.value.StringValue;

import org.junit.After;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class DmnDecisionTaskResultListenerTest extends CmmnTest {

  protected static final String TEST_CASE = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTaskResultListenerTest.cmmn";
  protected static final String TEST_DECISION = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultTest.dmn11.xml";
  protected static final String TEST_DECISION_COLLECT_SUM = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultCollectSumHitPolicyTest.dmn11.xml";
  protected static final String TEST_DECISION_COLLECT_COUNT = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultCollectCountHitPolicyTest.dmn11.xml";

  protected DmnDecisionResult results;

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testNoOutput() {
    startTestCase("no output");

    assertThat(results).as("The decision result 'ruleResult' should be empty").isEmpty();
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testEmptyOutput() {
    startTestCase("empty output");

    assertThat(results).as("The decision result 'ruleResult' should not be empty").isNotEmpty();

    DmnDecisionResultEntries decisionOutput = results.get(0);
    assertThat(decisionOutput.<Object>getFirstEntry()).isNull();
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testEmptyMap() {
    startTestCase("empty map");

    assertThat(results).hasSize(2);

    for (DmnDecisionResultEntries output : results) {
      assertThat(output).as("The decision output should be empty").isEmpty();
    }
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testSingleEntry() {
    startTestCase("single entry");

    DmnDecisionResultEntries firstOutput = results.get(0);
    assertThat(firstOutput.<String>getFirstEntry()).isEqualTo("foo");
    assertThat(firstOutput.<StringValue>getFirstEntryTyped()).isEqualTo(Variables.stringValue("foo"));
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testMultipleEntries() {
    startTestCase("multiple entries");

    DmnDecisionResultEntries firstOutput = results.get(0);
    assertThat(firstOutput)
            .containsEntry("result1", "foo")
            .containsEntry("result2", "bar");

    assertThat(firstOutput.<StringValue>getEntryTyped("result1")).isEqualTo(Variables.stringValue("foo"));
    assertThat(firstOutput.<StringValue>getEntryTyped("result2")).isEqualTo(Variables.stringValue("bar"));
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testSingleEntryList() {
    startTestCase("single entry list");

    assertThat(results).hasSize(2);

    for (DmnDecisionResultEntries output : results) {
      assertThat(output.<String >getFirstEntry()).isEqualTo("foo");
      assertThat(output.<StringValue>getFirstEntryTyped()).isEqualTo(Variables.stringValue("foo"));
    }
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION})
  @Test
  void testMultipleEntriesList() {
    startTestCase("multiple entries list");

    assertThat(results).hasSize(2);

    for (DmnDecisionResultEntries output : results) {
      assertThat(output)
              .hasSize(2)
              .containsEntry("result1", "foo")
              .containsEntry("result2", "bar");

      assertThat(output.<StringValue>getEntryTyped("result1")).isEqualTo(Variables.stringValue("foo"));
      assertThat(output.<StringValue>getEntryTyped("result2")).isEqualTo(Variables.stringValue("bar"));
    }
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION_COLLECT_COUNT})
  @Test
  void testCollectCountHitPolicyNoOutput() {
    startTestCase("no output");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isZero();
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(0));
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicyNoOutput() {
    startTestCase("no output");

    assertThat(results).as("The decision result 'ruleResult' should be empty").isEmpty();
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicySingleEntry() {
    startTestCase("single entry");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isEqualTo(12);
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(12));
  }

  @Deployment(resources = {TEST_CASE, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicySingleEntryList() {
    startTestCase("single entry list");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isEqualTo(33);
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(33));
  }

  protected CaseInstance startTestCase(String input) {
    CaseInstance caseInstance = createCaseInstanceByKey("case", Variables.createVariables().putValue("input", input));
    results = DecisionResultTestListener.getDecisionResult();
    assertThat(results).isNotNull();
    return caseInstance;
  }

  @AfterEach
  void tearDown() {
    // reset the invoked execution listener
    DecisionResultTestListener.reset();
  }

}
