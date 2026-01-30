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
package org.operaton.bpm.engine.test.dmn.feel;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

class FeelIntegrationTest {
  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  DecisionService decisionService;

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "Literal Expression, org/operaton/bpm/engine/test/dmn/feel/literal-expression.dmn",
      "Input Expression, org/operaton/bpm/engine/test/dmn/feel/input-expression.dmn",
      "Output Rule, org/operaton/bpm/engine/test/dmn/feel/output-rule.dmn"
  })
  void shouldEvaluate(String name, String dmnResource) {
    // given
    testRule.deploy(dmnResource);

    // when
    String result = decisionService.evaluateDecisionByKey("c").evaluate()
        .getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/feel/input-rule.dmn"})
  void shouldEvaluateInputRule() {
    // given

    // when
    String result = decisionService.evaluateDecisionTableByKey("c",
        Variables.putValue("cellInput", 6)).getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

}
