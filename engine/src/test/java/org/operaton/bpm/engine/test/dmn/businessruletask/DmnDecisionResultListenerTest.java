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
package org.operaton.bpm.engine.test.dmn.businessruletask;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.IntegerValue;
import org.operaton.bpm.engine.variable.value.StringValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the decision result that is retrieved by an execution listener.
 *
 * @author Philipp Ossler
 */
@ExtendWith(ProcessEngineExtension.class)
class DmnDecisionResultListenerTest {

  protected static final String TEST_PROCESS = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultTest.bpmn20.xml";
  protected static final String TEST_DECISION = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultTest.dmn11.xml";
  protected static final String TEST_DECISION_COLLECT_SUM = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultCollectSumHitPolicyTest.dmn11.xml";
  protected static final String TEST_DECISION_COLLECT_COUNT = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultCollectCountHitPolicyTest.dmn11.xml";

  DmnDecisionResult results;
  
  RuntimeService runtimeService;

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testNoOutput() {
    startTestProcess("no output");

    assertThat(results).as("The decision result 'ruleResult' should be empty").isEmpty();
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testEmptyOutput() {
    startTestProcess("empty output");

    assertThat(results).as("The decision result 'ruleResult' should not be empty").isNotEmpty();

    DmnDecisionResultEntries decisionOutput = results.get(0);
    assertThat(decisionOutput.<Object>getFirstEntry()).isNull();
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testEmptyMap() {
    startTestProcess("empty map");

    assertThat(results).hasSize(2);

    for (DmnDecisionResultEntries output : results) {
      assertThat(output).as("The decision output should be empty").isEmpty();
    }
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testSingleEntry() {
    startTestProcess("single entry");

    DmnDecisionResultEntries firstOutput = results.get(0);
    assertThat(firstOutput.<String>getFirstEntry()).isEqualTo("foo");
    assertThat(firstOutput.<StringValue>getFirstEntryTyped()).isEqualTo(Variables.stringValue("foo"));
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testMultipleEntries() {
    startTestProcess("multiple entries");

    DmnDecisionResultEntries firstOutput = results.get(0);
    assertThat(firstOutput)
            .containsEntry("result1", "foo")
            .containsEntry("result2", "bar");

    assertThat(firstOutput.<StringValue>getEntryTyped("result1")).isEqualTo(Variables.stringValue("foo"));
    assertThat(firstOutput.<StringValue>getEntryTyped("result2")).isEqualTo(Variables.stringValue("bar"));
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testSingleEntryList() {
    startTestProcess("single entry list");

    assertThat(results).hasSize(2);

    for (DmnDecisionResultEntries output : results) {
      assertThat(output.<String>getFirstEntry()).isEqualTo("foo");
      assertThat(output.<StringValue>getFirstEntryTyped()).isEqualTo(Variables.stringValue("foo"));
    }
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION})
  @Test
  void testMultipleEntriesList() {
    startTestProcess("multiple entries list");

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

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION_COLLECT_COUNT})
  @Test
  void testCollectCountHitPolicyNoOutput() {
    startTestProcess("no output");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isZero();
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(0));
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicyNoOutput() {
    startTestProcess("no output");

    assertThat(results).as("The decision result 'ruleResult' should be empty").isEmpty();
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicySingleEntry() {
    startTestProcess("single entry");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isEqualTo(12);
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(12));
  }

  @Deployment(resources = {TEST_PROCESS, TEST_DECISION_COLLECT_SUM})
  @Test
  void testCollectSumHitPolicySingleEntryList() {
    startTestProcess("single entry list");

    assertThat(results).hasSize(1);
    DmnDecisionResultEntries firstOutput = results.get(0);

    assertThat((int) firstOutput.getFirstEntry()).isEqualTo(33);
    assertThat(firstOutput.<IntegerValue>getFirstEntryTyped()).isEqualTo(Variables.integerValue(33));
  }

  protected ProcessInstance startTestProcess(String input) {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", Collections.singletonMap("input", input));

    // get the result from an execution listener that is invoked at the end of the business rule activity
    results = DecisionResultTestListener.getDecisionResult();
    assertThat(results).isNotNull();

    return processInstance;
  }

  @AfterEach
  void tearDown() {
    // reset the invoked execution listener
    DecisionResultTestListener.reset();
  }

}
