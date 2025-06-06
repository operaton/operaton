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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.operaton.bpm.dmn.feel.impl.juel.FeelSyntaxException;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FeelEnableLegacyBehaviorConfigTest {

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setDmnFeelEnableLegacyBehavior(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected DecisionService decisionService;

  @Before
  public void setup() {
    decisionService = engineRule.getProcessEngine().getDecisionService();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/feel/legacy/literal-expression.dmn"})
  public void shouldEvaluateLiteralExpression() {
    // given

    // when
    String result = decisionService.evaluateDecisionByKey("c").evaluate()
        .getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/feel/legacy/input-expression.dmn"})
  public void shouldEvaluateInputExpression() {
    // given

    // when
    String result = decisionService.evaluateDecisionByKey("c").evaluate()
        .getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/feel/legacy/input-rule.dmn"})
  public void shouldEvaluateInputRule() {
    // given

    // when/then
    assertThatThrownBy(() -> decisionService.evaluateDecisionTableByKey("c",
        Variables.putValue("cellInput", 6)).getSingleEntry())
      .hasCauseInstanceOf(FeelSyntaxException.class)
      .extracting("cause.message")
      .isEqualTo("FEEL-01010 Syntax error in expression 'for x in 1..3 return x * 2'");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/dmn/feel/legacy/output-rule.dmn"})
  public void shouldEvaluateOutputRule() {
    // given

    // when
    String result = decisionService.evaluateDecisionByKey("c").evaluate()
        .getSingleEntry();

    // then
    assertThat(result).isEqualTo("foo");
  }

}
