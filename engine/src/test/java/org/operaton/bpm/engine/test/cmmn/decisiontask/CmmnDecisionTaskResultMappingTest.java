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
package org.operaton.bpm.engine.test.cmmn.decisiontask;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.StringValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class CmmnDecisionTaskResultMappingTest extends CmmnTest {

  protected static final String TEST_DECISION = "org/operaton/bpm/engine/test/dmn/result/DmnDecisionResultTest.dmn11.xml";
  protected static final String SINGLE_ENTRY_MAPPING_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testSingleEntryMapping.cmmn";
  protected static final String SINGLE_RESULT_MAPPING_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testSingleResultMapping.cmmn";
  protected static final String COLLECT_ENTRIES_MAPPING_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testCollectEntriesMapping.cmmn";
  protected static final String RESULT_LIST_MAPPING_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testResultListMapping.cmmn";
  protected static final String DEFAULT_MAPPING_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testDefaultResultMapping.cmmn";
  protected static final String OVERRIDE_DECISION_RESULT_CMMN = "org/operaton/bpm/engine/test/cmmn/decisiontask/DmnDecisionTableResultMappingTest.testFailedToOverrideDecisionResultVariable.cmmn";

  @Deployment(resources = {SINGLE_ENTRY_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testSingleEntryMapping() {
    CaseInstance caseInstance = createTestCase("single entry");

    assertThat(caseService.getVariable(caseInstance.getId(), "result")).isEqualTo("foo");
    assertThat(caseService.<StringValue>getVariableTyped(caseInstance.getId(), "result")).isEqualTo(Variables.stringValue("foo"));
  }

  @SuppressWarnings("unchecked")
  @Deployment(resources = {SINGLE_RESULT_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testSingleResultMapping() {
    CaseInstance caseInstance = createTestCase("multiple entries");

    Map<String, Object> output = (Map<String, Object>) caseService.getVariable(caseInstance.getId(), "result");

    assertThat(output)
            .hasSize(2)
            .containsEntry("result1", "foo")
            .containsEntry("result2", "bar");
  }

  @SuppressWarnings("unchecked")
  @Deployment(resources = {COLLECT_ENTRIES_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testCollectEntriesMapping() {
    CaseInstance caseInstance = createTestCase("single entry list");

    List<String> output = (List<String>) caseService.getVariable(caseInstance.getId(), "result");

    assertThat(output).hasSize(2);
    assertThat(output.get(0)).isEqualTo("foo");
    assertThat(output.get(1)).isEqualTo("foo");
  }

  @SuppressWarnings("unchecked")
  @Deployment(resources = {RESULT_LIST_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testResultListMapping() {
    CaseInstance caseInstance = createTestCase("multiple entries list");

    List<Map<String, Object>> resultList = (List<Map<String, Object>>) caseService.getVariable(caseInstance.getId(), "result");
    assertThat(resultList).hasSize(2);

    for (Map<String, Object> valueMap : resultList) {
      assertThat(valueMap)
              .hasSize(2)
              .containsEntry("result1", "foo")
              .containsEntry("result2", "bar");
    }
  }

  @SuppressWarnings("unchecked")
  @Deployment(resources = {DEFAULT_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testDefaultResultMapping() {
    CaseInstance caseInstance = createTestCase("multiple entries list");

    // default mapping is 'resultList'
    List<Map<String, Object>> resultList = (List<Map<String, Object>>) caseService.getVariable(caseInstance.getId(), "result");
    assertThat(resultList).hasSize(2);

    for (Map<String, Object> valueMap : resultList) {
      assertThat(valueMap)
              .hasSize(2)
              .containsEntry("result1", "foo")
              .containsEntry("result2", "bar");
    }
  }

  @Deployment(resources = {SINGLE_ENTRY_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testSingleEntryMappingFailureMultipleOutputs() {
    // when/then
    assertThatThrownBy(() -> createTestCase("single entry list"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-22001");
  }

  @Deployment(resources = {SINGLE_ENTRY_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testSingleEntryMappingFailureMultipleValues() {
    // when/then
    assertThatThrownBy(() -> createTestCase("multiple entries"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-22001");
  }

  @Deployment(resources = {SINGLE_RESULT_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testSingleResultMappingFailure() {
    // when/then
    assertThatThrownBy(() -> createTestCase("single entry list"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-22001");
  }

  @Deployment(resources = {COLLECT_ENTRIES_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testCollectEntriesMappingFailure() {
    // when/then
    assertThatThrownBy(() -> createTestCase("multiple entries"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-22002");
  }

  @Deployment(resources = {DEFAULT_MAPPING_CMMN, TEST_DECISION})
  @Test
  void testTransientDecisionResult() {
    // when a decision is evaluated and the result is stored in a transient variable "decisionResult"
    CaseInstance caseInstance = createTestCase("single entry");

    // then the variable should not be available outside the decision task
    assertThat(caseService.getVariable(caseInstance.getId(), "decisionResult")).isNull();
  }

  @Deployment(resources = {OVERRIDE_DECISION_RESULT_CMMN, TEST_DECISION})
  @Test
  void testFailedToOverrideDecisionResultVariable() {
    // when/then the transient variable "decisionResult" should not be overridden by the task result variable
    assertThatThrownBy(() -> createTestCase("single entry"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("transient variable with name 'decisionResult' to non-transient");
  }

  protected CaseInstance createTestCase(String input) {
    return createCaseInstanceByKey("case", Variables.createVariables().putValue("input", input));
  }

}
