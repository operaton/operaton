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
package org.operaton.bpm.dmn.engine.transform;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.Definitions;
import org.operaton.bpm.model.dmn.instance.Input;
import org.operaton.bpm.model.dmn.instance.Output;
import org.operaton.bpm.model.dmn.instance.Rule;
import org.operaton.commons.utils.IoUtil;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */

public class DmnTransformListenerTest extends DmnEngineTest {

  public static final String DRG_EXAMPLE_DMN = "org/operaton/bpm/dmn/engine/transform/DrgExample.dmn";
  public static final String DECISION_TRANSFORM_DMN = "org/operaton/bpm/dmn/engine/transform/DmnDecisionTransform.dmn";

  protected TestDmnTransformListener listener;

    /**
   * Returns the DmnEngineConfiguration by creating a new instance of TestDmnTransformListenerConfiguration.
   * 
   * @return the DmnEngineConfiguration
   */
  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return new TestDmnTransformListenerConfiguration();
  }

    /**
   * Initializes the listener by retrieving the configuration from the DMN engine and assigning the test DMN transform listener to the listener variable.
   */
  @Before
  public void initListener() {
    TestDmnTransformListenerConfiguration configuration = (TestDmnTransformListenerConfiguration) dmnEngine.getConfiguration();
    listener = configuration.testDmnTransformListener;
  }

    /**
   * Test method to verify that the listener is called and all its attributes are not null after parsing a decision requirements graph.
   */
  @Test
  public void shouldCallListener() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    assertThat(listener.getDmnDecisionRequirementsGraph()).isNotNull();
    assertThat(listener.getDmnDecision()).isNotNull();
    assertThat(listener.getDmnInput()).isNotNull();
    assertThat(listener.getDmnOutput()).isNotNull();
    assertThat(listener.getDmnRule()).isNotNull();
  }

    /**
   * This method verifies the correctness of a DMN Decision Requirements Graph by parsing a DMN file, retrieving the decision requirements graph from a listener, and making several assertions about its key, name, number of decisions, and specific decisions.
   */
  @Test
  public void shouldVerifyDmnDecisionRequirementsGraph() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph = listener.getDmnDecisionRequirementsGraph();
    Definitions definitions = listener.getDefinitions();
    assertThat(dmnDecisionRequirementsGraph.getKey())
      .isEqualTo(definitions.getId())
      .isEqualTo("dish");
    assertThat(dmnDecisionRequirementsGraph.getName())
      .isEqualTo(definitions.getName())
      .isEqualTo("Dish");
    assertThat(dmnDecisionRequirementsGraph.getDecisions().size()).isEqualTo(3);
    assertThat(dmnDecisionRequirementsGraph.getDecision("dish-decision")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("season")).isNotNull();
    assertThat(dmnDecisionRequirementsGraph.getDecision("guestCount")).isNotNull();
  }

    /**
   * Verifies that the transformed DMN decision matches the expected values.
   */
  @Test
  public void shouldVerifyTransformedDmnDecision() {
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

    /**
   * Verifies the transformed DMN decisions after parsing a decision requirements graph.
   */
  @Test
  public void shouldVerifyTransformedDmnDecisions() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DRG_EXAMPLE_DMN));
    List<DmnDecision> transformedDecisions = listener.getTransformedDecisions();
    assertThat(transformedDecisions.size()).isEqualTo(3);

    assertThat(getDmnDecision(transformedDecisions, "dish-decision")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "season")).isNotNull();
    assertThat(getDmnDecision(transformedDecisions, "guestCount")).isNotNull();

  }

    /**
   * Verifies that the transformed input matches the expected input defined in a DMN decision table.
   */
  @Test
  public void shouldVerifyTransformedInput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableInputImpl dmnInput = listener.getDmnInput();
    Input input = listener.getInput();

    assertThat(dmnInput.getId())
      .isEqualTo(input.getId())
      .isEqualTo("input1");

  }

    /**
   * This method verifies the transformed output of a decision table by comparing the output id with the expected value "output1".
   */
  @Test
  public void shouldVerifyTransformedOutput() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableOutputImpl dmnOutput = listener.getDmnOutput();
    Output output = listener.getOutput();

    assertThat(dmnOutput.getId())
      .isEqualTo(output.getId())
      .isEqualTo("output1");

  }

    /**
   * This method verifies that a transformed rule matches the expected rule.
   */
  @Test
  public void shouldVerifyTransformedRule() {
    dmnEngine.parseDecisionRequirementsGraph(IoUtil.fileAsStream(DECISION_TRANSFORM_DMN));
    DmnDecisionTableRuleImpl dmnRule = listener.getDmnRule();
    Rule rule = listener.getRule();

    assertThat(dmnRule.getId())
      .isEqualTo(rule.getId())
      .isEqualTo("rule");

  }

    /**
   * Returns the DmnDecision object with the specified key from the given list of DmnDecisions.
   * 
   * @param decisionList the list of DmnDecisions to search through
   * @param key the key to search for
   * @return the DmnDecision object with the specified key, or null if not found
   */
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
    protected List<DmnDecision> transformedDecisions = new ArrayList<DmnDecision>();

    protected Input input;
    protected DmnDecisionTableInputImpl dmnInput;

    protected Output output;
    protected DmnDecisionTableOutputImpl dmnOutput;

    protected Rule rule;
    protected DmnDecisionTableRuleImpl dmnRule;

    protected Definitions definitions;
    protected DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph;

        /**
     * Returns the decision.
     *
     * @return the decision
     */
    public Decision getDecision() {
      return decision;
    }

        /**
     * Returns the DmnDecision object.
     *
     * @return the DmnDecision object
     */
    public DmnDecision getDmnDecision() {
      return dmnDecision;
    }

        /**
     * Returns the list of transformed DMN decisions.
     *
     * @return the list of transformed DMN decisions
     */
    public List<DmnDecision> getTransformedDecisions() {
          return transformedDecisions;
        }

        /**
     * Returns the input object.
     *
     * @return the input object
     */
    public Input getInput() {
      return input;
    }

        /**
     * Returns the DmnDecisionTableInputImpl object.
     *
     * @return the DmnDecisionTableInputImpl object
     */
    public DmnDecisionTableInputImpl getDmnInput() {
          return dmnInput;
        }

        /**
     * Returns the output object.
     *
     * @return the output object
     */
    public Output getOutput() {
      return output;
    }

        /**
     * Returns the DMN decision table output.
     *
     * @return the DMN decision table output
     */
    public DmnDecisionTableOutputImpl getDmnOutput() {
          return dmnOutput;
        }

        /**
     * Returns the Rule associated with this object.
     *
     * @return the Rule object
     */
    public Rule getRule() {
      return rule;
    }

        /**
     * Returns the DmnDecisionTableRule implementation.
     *
     * @return the DmnDecisionTableRule implementation
     */
    public DmnDecisionTableRuleImpl getDmnRule() {
          return dmnRule;
        }

        /**
     * Returns the definitions object.
     * 
     * @return the definitions object
     */
    public Definitions getDefinitions() {
      return definitions;
    }

        /**
     * Retrieves the DMN decision requirements graph associated with this object.
     * 
     * @return the DMN decision requirements graph
     */
    public DmnDecisionRequirementsGraph getDmnDecisionRequirementsGraph() {
      return dmnDecisionRequirementsGraph;
    }

        /**
     * Sets the given Decision and DmnDecision objects and adds the DmnDecision to the transformedDecisions list.
     * 
     * @param decision the Decision object to set
     * @param dmnDecision the DmnDecision object to set
     */
    public void transformDecision(Decision decision, DmnDecision dmnDecision) {
          this.decision = decision;
          this.dmnDecision = dmnDecision;
          transformedDecisions.add(dmnDecision);
        }

        /**
     * Sets the input and decision table input for further processing.
     * 
     * @param input the input object to be set
     * @param dmnInput the decision table input object to be set
     */
    public void transformDecisionTableInput(Input input, DmnDecisionTableInputImpl dmnInput) {
          this.input = input;
          this.dmnInput = dmnInput;
        }

        /**
     * Sets the output and decision table output for the transformation.
     * 
     * @param output the output to be set
     * @param dmnOutput the decision table output to be set
     */
    public void transformDecisionTableOutput(Output output, DmnDecisionTableOutputImpl dmnOutput) {
          this.output = output;
          this.dmnOutput = dmnOutput;
        }

        /**
     * Sets the rule and DMN rule for a decision table rule.
     * 
     * @param rule the rule to set
     * @param dmnRule the DMN rule to set
     */
    public void transformDecisionTableRule(Rule rule, DmnDecisionTableRuleImpl dmnRule) {
          this.rule = rule;
          this.dmnRule = dmnRule;
        }

        /**
     * Sets the Definitions and DMN Decision Requirements Graph for transformation.
     * 
     * @param definitions the Definitions to set
     * @param dmnDecisionRequirementsGraph the DMN Decision Requirements Graph to set
     */
    public void transformDecisionRequirementsGraph(Definitions definitions, DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph) {
      this.definitions = definitions;
      this.dmnDecisionRequirementsGraph = dmnDecisionRequirementsGraph;
    }
  }
}
