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
package org.operaton.bpm.dmn.engine.api;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelException;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.commons.utils.IoUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParseDecisionTest extends DmnEngineTest {

  @SuppressWarnings("unused")
  private static final String NO_DECISION_DMN = "org/operaton/bpm/dmn/engine/api/NoDecision.dmn";
  private static final String NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/NoInput.dmn";
  @SuppressWarnings("unused")
  private static final String INVOCATION_DECISION_DMN = "org/operaton/bpm/dmn/engine/api/InvocationDecision.dmn";
  private static final String MISSING_DECISION_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingDecisionId.dmn";
  private static final String MISSING_INPUT_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingInputId.dmn";
  private static final String MISSING_OUTPUT_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingOutputId.dmn";
  private static final String MISSING_RULE_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingRuleId.dmn";
  private static final String MISSING_COMPOUND_OUTPUT_NAME_DMN = "org/operaton/bpm/dmn/engine/api/CompoundOutputs.noName.dmn";
  private static final String DUPLICATE_COMPOUND_OUTPUT_NAME_DMN = "org/operaton/bpm/dmn/engine/api/CompoundOutputs.duplicateName.dmn";

  private static final String MISSING_VARIABLE_DMN = "org/operaton/bpm/dmn/engine/api/MissingVariable.dmn";

  private static final String MISSING_REQUIRED_DECISION_REFERENCE_DMN = "org/operaton/bpm/dmn/engine/api/MissingRequiredDecisionReference.dmn";
  private static final String WRONG_REQUIRED_DECISION_REFERENCE_DMN = "org/operaton/bpm/dmn/engine/api/WrongRequiredDecisionReference.dmn";
  private static final String MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN = "org/operaton/bpm/dmn/engine/api/MissingRequiredDecisionAttribute.dmn";
  private static final String NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN = "org/operaton/bpm/dmn/engine/api/NoInformationRequirementAttribute.dmn";
  private static final String MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingDrdId.dmn";

  private static final String DMN12_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn12/NoInput.dmn";
  private static final String DMN13_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn13/NoInput.dmn";

  @Test
  void shouldParseDecisionFromInputStream() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  @Test
  void shouldParseDecisionFromModelInstance() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    decision = dmnEngine.parseDecision("decision", modelInstance);
    assertDecision(decision, "decision");
  }

  @Test
  void shouldFailIfDecisionKeyIsUnknown() {
    assertThatThrownBy(() -> parseDecisionFromFile("unknownDecision", NO_INPUT_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasMessageStartingWith("DMN-01001")
      .hasMessageContaining("Unable to find decision")
      .hasMessageContaining("unknownDecision");
  }

  @Test
  void shouldFailIfDecisionIdIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_DECISION_ID_DMN)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02010")
      .hasMessageContaining("Decision With Missing Id");
  }

  @Test
  void shouldFailIfInputIdIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_INPUT_ID_DMN)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02011")
      .hasMessageContaining("Decision With Missing Input Id");
  }

  @Test
  void shouldFailIfOutputIdIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_OUTPUT_ID_DMN)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02012")
      .hasMessageContaining("Decision With Missing Output Id");
  }

  @Test
  void shouldFailIfRuleIdIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_RULE_ID_DMN)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02013")
      .hasMessageContaining("Decision With Missing Rule Id");
  }

  @Test
  void shouldFailIfCompoundOutputsNameIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_COMPOUND_OUTPUT_NAME_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02008")
      .hasMessageContaining("does not have an output name");
  }

  @Test
  void shouldFailIfCompoundOutputsHaveDuplicateName() {
    assertThatThrownBy(() -> parseDecisionsFromFile(DUPLICATE_COMPOUND_OUTPUT_NAME_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02009")
      .hasMessageContaining("has a compound output but name of output")
      .hasMessageContaining("is duplicate");
  }

  @Test
  void shouldFailIfVariableIsMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_VARIABLE_DMN)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("DMN-02018")
      .hasMessageContaining("The decision 'missing-variable' must have an 'variable' element");
  }

  @Test
  void shouldFailIfRequiredDecisionReferenceMissing() {
    assertThatThrownBy(() -> parseDecisionsFromFile(MISSING_REQUIRED_DECISION_REFERENCE_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasCauseExactlyInstanceOf(ModelException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("Unable to find a model element instance for id null");
  }

  @Test
  void shouldFailIfWrongRequiredDecisionReference() {
    assertThatThrownBy(() -> parseDecisionsFromFile(WRONG_REQUIRED_DECISION_REFERENCE_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasCauseExactlyInstanceOf(ModelException.class)
      .hasMessageStartingWith("DMN-02004")
      .hasMessageContaining("Unable to find a model element instance for id");
  }

  @Test
  void shouldNotFailIfMissingRequiredDecisionAttribute() {
    List<DmnDecision> decisions = parseDecisionsFromFile(MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN);
    assertThat(decisions).hasSize(1);
    assertThat(decisions.get(0).getRequiredDecisions()).isEmpty();
  }

  @Test
  void shouldFailIfNoInformationRequirementAttribute() {
    assertThatThrownBy(() -> parseDecisionsFromFile(NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN)).isInstanceOf(
        DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnModelException.class)
      .hasMessageStartingWith("DMN-02003")
      .hasMessageContaining("Unable to transform decisions from input stream");
  }

  @Test
  void shouldParseDrgFromInputStream() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

  @Test
  void shouldParseDrgFromModelInstance() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(modelInstance);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

  @Test
  void shouldFailIfDecisionDrgIdIsMissing() {
    InputStream inputStream = IoUtil.fileAsStream(MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN);
    assertThatThrownBy(() -> dmnEngine.parseDecisionRequirementsGraph(inputStream)).isInstanceOf(DmnTransformException.class)
      .hasCauseExactlyInstanceOf(DmnTransformException.class)
      .hasMessageStartingWith("DMN-02016")
      .hasMessageContaining("DMN-02017")
      .hasMessageContaining("DRD with Missing Id");
  }

  @Test
  void shouldParseDecisionFromInputStream_Dmn12() {
    InputStream inputStream = IoUtil.fileAsStream(DMN12_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  @Test
  void shouldParseDecisionFromInputStream_Dmn13() {
    InputStream inputStream = IoUtil.fileAsStream(DMN13_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

  protected void assertDecision(DmnDecision decision, String key) {
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo(key);
  }

  protected void assertDecisionRequirementsGraph(DmnDecisionRequirementsGraph drg, String key) {
    assertThat(drg).isNotNull();
    assertThat(drg.getKey()).isEqualTo(key);
  }

}
