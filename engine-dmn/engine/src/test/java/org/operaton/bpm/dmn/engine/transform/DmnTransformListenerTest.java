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
package org.operaton.bpm.dmn.engine.transform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformListener;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.instance.*;
import org.operaton.commons.utils.IoUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

class DmnTransformListenerTest extends DmnEngineTest {

  private static final String DRG_EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/transform/DrgExample.dmn";
  private static final String DECISION_TRANSFORM_DMN = "org/operaton/bpm/dmn/engine/transform/DmnDecisionTransform.dmn";

  protected TestDmnTransformListener listener;

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    return new TestDmnTransformListenerConfiguration();
  }

  @BeforeEach
  void initListener() {
    TestDmnTransformListenerConfiguration configuration = (TestDmnTransformListenerConfiguration) dmnEngine.getConfiguration();
    listener = configuration.testDmnTransformListener;
  }

  @Test
  void shouldCallListener() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    assertThat(listener.getDmnDecisionRequirementsGraph()).isNotNull();
    assertThat(listener.getDmnDecision()).isNotNull();
    assertThat(listener.getDmnInput()).isNotNull();
    assertThat(listener.getDmnOutput()).isNotNull();
    assertThat(listener.getDmnRule()).isNotNull();
  }

  @Test
  void shouldVerifyDmnDecisionRequirementsGraph() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph = listener.getDmnDecisionRequirementsGraph();
    Definitions definitions = listener.getDefinitions();
    assertThat(dmnDecisionRequirementsGraph.getKey())
      .isEqualTo(definitions.getId())
      .isEqualTo("dish");
    assertThat(dmnDecisionRequirementsGraph.getName())
      .isEqualTo(definitions.getName())
      .isEqualTo("Dish");
    assertThat(dmnDecisionRequirementsGraph.getDecisions()).hasSize(3);
    assertThat(dmnDecisionRequirementsGraph.getDecision("dish-decision")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("season")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("guestCount")).isNotNull();
  }

  @Test
  void shouldVerifyTransformedDmnDecision() {
    InputStream inputStream =  IoUtil.fileAsStream(DECISION_TRANSFORM_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);
    dmnEngine.parseDecisionRequirementsGraph(modelInstance);

    DmnDecision dmnDecision = listener.getDmnDecision();
    Decision decision = listener.getDecision();

    assertThat(dmnDecision.getKey())
      .isEqualTo(decision.getId())
      .isEqualTo("decision1");

    assertThat(dmnDecision.getName())
      .isEqualTo(decision.getName())
      .isEqualTo("operaton");
  }

  @Test
  void shouldVerifyTransformedDmnDecisions() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    List<DmnDecision> transformedDecisions = listener.getTransformedDecisions();
    assertThat(transformedDecisions).hasSize(3);

    assertThat(getDmnDecision(transformedDecisions, "dish-decision")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "season")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "guestCount")).isNotNull();

  }

  @Test
  void shouldVerifyTransformedInput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableInputImpl dmnInput = listener.getDmnInput();
    Input input = listener.getInput();

    assertThat(dmnInput.getId())
      .isEqualTo(input.getId())
      .isEqualTo("input1");

  }

  @Test
  void shouldVerifyTransformedOutput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableOutputImpl dmnOutput = listener.getDmnOutput();
    Output output = listener.getOutput();

    assertThat(dmnOutput.getId())
      .isEqualTo(output.getId())
      .isEqualTo("output1");

  }

  @Test
  void shouldVerifyTransformedRule() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableRuleImpl dmnRule = listener.getDmnRule();
    Rule rule = listener.getRule();

    assertThat(dmnRule.getId())
      .isEqualTo(rule.getId())
      .isEqualTo("rule");

  }

  protected DmnDecision getDmnDecision(List<DmnDecision> decisionList, String key) {
    for(DmnDecision dmnDecision: decisionList) {
      if(dmnDecision.getKey().equals(key)) {
        return dmnDecision;
      }
    }
    return null;
  }

  public static class TestDmnTransformListenerConfiguration extends DefaultDmnEngineConfiguration {

    public TestDmnTransformListener testDmnTransformListener = new TestDmnTransformListener();

    public TestDmnTransformListenerConfiguration() {
      transformer.getTransformListeners().add(testDmnTransformListener);
    }
  }

  public static class TestDmnTransformListener implements DmnTransformListener {

    protected Decision decision;
    protected DmnDecision dmnDecision;
    protected List<DmnDecision> transformedDecisions = new ArrayList<>();

    protected Input input;
    protected DmnDecisionTableInputImpl dmnInput;

    protected Output output;
    protected DmnDecisionTableOutputImpl dmnOutput;

    protected Rule rule;
    protected DmnDecisionTableRuleImpl dmnRule;

    protected Definitions definitions;
    protected DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph;

    public Decision getDecision() {
      return decision;
    }

    public DmnDecision getDmnDecision() {
      return dmnDecision;
    }

    public List<DmnDecision> getTransformedDecisions() {
      return transformedDecisions;
    }

    public Input getInput() {
      return input;
    }

    public DmnDecisionTableInputImpl getDmnInput() {
      return dmnInput;
    }

    public Output getOutput() {
      return output;
    }

    public DmnDecisionTableOutputImpl getDmnOutput() {
      return dmnOutput;
    }

    public Rule getRule() {
      return rule;
    }

    public DmnDecisionTableRuleImpl getDmnRule() {
      return dmnRule;
    }

    public Definitions getDefinitions() {
      return definitions;
    }

    public DmnDecisionRequirementsGraph getDmnDecisionRequirementsGraph() {
      return dmnDecisionRequirementsGraph;
    }

    @Override
    public void transformDecision(Decision decision, DmnDecision dmnDecision) {
      this.decision = decision;
      this.dmnDecision = dmnDecision;
      transformedDecisions.add(dmnDecision);
    }

    @Override
    public void transformDecisionTableInput(Input input, DmnDecisionTableInputImpl dmnInput) {
      this.input = input;
      this.dmnInput = dmnInput;
    }

    @Override
    public void transformDecisionTableOutput(Output output, DmnDecisionTableOutputImpl dmnOutput) {
      this.output = output;
      this.dmnOutput = dmnOutput;
    }

    @Override
    public void transformDecisionTableRule(Rule rule, DmnDecisionTableRuleImpl dmnRule) {
      this.rule = rule;
      this.dmnRule = dmnRule;
    }

    @Override
    public void transformDecisionRequirementsGraph(Definitions definitions, DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph) {
      this.definitions = definitions;
      this.dmnDecisionRequirementsGraph = dmnDecisionRequirementsGraph;
    }
  }
}
