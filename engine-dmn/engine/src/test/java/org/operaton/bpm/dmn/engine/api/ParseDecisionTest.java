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
package org.operaton.bpm.dmn.engine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.InputStream;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelException;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.commons.utils.IoUtil;
import org.junit.Test;

public class ParseDecisionTest extends DmnEngineTest {

  public static final String NO_DECISION_DMN = "org/operaton/bpm/dmn/engine/api/NoDecision.dmn";
  public static final String NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/NoInput.dmn";
  public static final String INVOCATION_DECISION_DMN = "org/operaton/bpm/dmn/engine/api/InvocationDecision.dmn";
  public static final String MISSING_DECISION_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingDecisionId.dmn";
  public static final String MISSING_INPUT_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingInputId.dmn";
  public static final String MISSING_OUTPUT_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingOutputId.dmn";
  public static final String MISSING_RULE_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingRuleId.dmn";
  public static final String MISSING_COMPOUND_OUTPUT_NAME_DMN = "org/operaton/bpm/dmn/engine/api/CompoundOutputs.noName.dmn";
  public static final String DUPLICATE_COMPOUND_OUTPUT_NAME_DMN = "org/operaton/bpm/dmn/engine/api/CompoundOutputs.duplicateName.dmn";

  public static final String MISSING_VARIABLE_DMN = "org/operaton/bpm/dmn/engine/api/MissingVariable.dmn";

  public static final String MISSING_REQUIRED_DECISION_REFERENCE_DMN = "org/operaton/bpm/dmn/engine/api/MissingRequiredDecisionReference.dmn";
  public static final String WRONG_REQUIRED_DECISION_REFERENCE_DMN = "org/operaton/bpm/dmn/engine/api/WrongRequiredDecisionReference.dmn";
  public static final String MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN = "org/operaton/bpm/dmn/engine/api/MissingRequiredDecisionAttribute.dmn";
  public static final String NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN = "org/operaton/bpm/dmn/engine/api/NoInformationRequirementAttribute.dmn";
  public static final String MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN = "org/operaton/bpm/dmn/engine/api/MissingIds.missingDrdId.dmn";

  public static final String DMN12_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn12/NoInput.dmn";
  public static final String DMN13_NO_INPUT_DMN = "org/operaton/bpm/dmn/engine/api/dmn13/NoInput.dmn";

    /**
   * Parses a decision from an input stream and asserts the result.
   */
  @Test
  public void shouldParseDecisionFromInputStream() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

    /**
   * Parses a decision from a given DMN model instance and asserts its correctness.
   */
  @Test
  public void shouldParseDecisionFromModelInstance() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    decision = dmnEngine.parseDecision("decision", modelInstance);
    assertDecision(decision, "decision");
  }

    /**
   * This method tests if an exception is thrown when trying to parse a decision
   * from a file with an unknown decision key.
   */
  @Test
  public void shouldFailIfDecisionKeyIsUnknown() {
    try {
      parseDecisionFromFile("unknownDecision", NO_INPUT_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      Assertions.assertThat(e)
        .hasMessageStartingWith("DMN-01001")
        .hasMessageContaining("Unable to find decision")
        .hasMessageContaining("unknownDecision");
    }
  }

    /**
   * Test method to verify that an exception is thrown if a decision id is missing when parsing decisions from a file.
   */
  @Test
  public void shouldFailIfDecisionIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_DECISION_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02010")
        .hasMessageContaining("Decision With Missing Id");
    }
  }

    /**
   * Test method to verify that an exception is thrown when input id is missing in the decision file.
   */
  @Test
  public void shouldFailIfInputIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_INPUT_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02011")
        .hasMessageContaining("Decision With Missing Input Id");
    }
  }

    /**
   * Test method to verify that an exception is thrown if the output id is missing when parsing decisions from a file.
   */
  @Test
  public void shouldFailIfOutputIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_OUTPUT_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02012")
        .hasMessageContaining("Decision With Missing Output Id");
    }
  }

    /**
   * Test method to verify that an exception is thrown when a rule id is missing in the DMN file.
   */
  @Test
  public void shouldFailIfRuleIdIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_RULE_ID_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02013")
        .hasMessageContaining("Decision With Missing Rule Id");
    }
  }

    /**
   * This method tests that an exception is thrown if a compound outputs name is missing when parsing decisions from a file.
   */
  @Test
  public void shouldFailIfCompoundOutputsNameIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_COMPOUND_OUTPUT_NAME_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02008")
        .hasMessageContaining("does not have an output name");
    }
  }

    /**
   * This method tests if an exception is thrown when a DMN file contains compound outputs with duplicate names.
   */
  @Test
  public void shouldFailIfCompoundOutputsHaveDuplicateName() {
    try {
      parseDecisionsFromFile(DUPLICATE_COMPOUND_OUTPUT_NAME_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02009")
        .hasMessageContaining("has a compound output but name of output")
        .hasMessageContaining("is duplicate");
    }
  }

    /**
   * This method tests if an exception is thrown when a variable is missing in a DMN file.
   */
  @Test
  public void shouldFailIfVariableIsMissing() {
    try {
      parseDecisionsFromFile(MISSING_VARIABLE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("DMN-02018")
        .hasMessageContaining("The decision 'missing-variable' must have an 'variable' element");
    }
  }

    /**
   * Test method to verify that a DmnTransformException is thrown if a required decision reference is missing.
   */
  @Test
  public void shouldFailIfRequiredDecisionReferenceMissing() {
    try {
      parseDecisionsFromFile(MISSING_REQUIRED_DECISION_REFERENCE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(ModelException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("Unable to find a model element instance for id null");
    }
  }

    /**
   * This method tests whether an exception is thrown when parsing a DMN file with a wrong required decision reference.
   */
  @Test
  public void shouldFailIfWrongRequiredDecisionReference() {
    try {
      parseDecisionsFromFile(WRONG_REQUIRED_DECISION_REFERENCE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(ModelException.class)
        .hasMessageStartingWith("DMN-02004")
        .hasMessageContaining("Unable to find a model element instance for id");
    }
  }

    /**
   * Test method to verify that the system does not fail if a required decision attribute is missing.
   */
  @Test
  public void shouldNotFailIfMissingRequiredDecisionAttribute() {
    List<DmnDecision> decisions = parseDecisionsFromFile(MISSING_REQUIRED_DECISION_ATTRIBUTE_DMN);
    assertThat(decisions.size()).isEqualTo(1);
    assertThat(decisions.get(0).getRequiredDecisions().size()).isEqualTo(0);
  }

    /**
   * This method tests if an exception is thrown when attempting to parse decisions from a file
   * that does not contain the required information requirement attribute.
   */
  @Test
  public void shouldFailIfNoInformationRequirementAttribute() {
    try {
      parseDecisionsFromFile(NO_INFORMATION_REQUIREMENT_ATTRIBUTE_DMN);
      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnModelException.class)
        .hasMessageStartingWith("DMN-02003")
        .hasMessageContaining("Unable to transform decisions from input stream");
    }
  }

    /**
   * This method tests the parsing of a Decision Requirements Graph (DRG) from an input stream.
   * It reads an input stream from a file, parses it using the DMN engine, and asserts the result.
   */
  @Test
  public void shouldParseDrgFromInputStream() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

    /**
   * Parses a Decision Requirements Graph (DRG) from a DMN model instance and asserts the result.
   */
  @Test
  public void shouldParseDrgFromModelInstance() {
    InputStream inputStream = IoUtil.fileAsStream(NO_INPUT_DMN);
    DmnModelInstance modelInstance = Dmn.readModelFromStream(inputStream);

    DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(modelInstance);

    assertDecisionRequirementsGraph(drg, "definitions");
  }

    /**
   * Test method to verify that an exception is thrown if the Decision Requirement Diagram ID is missing.
   */
  @Test
  public void shouldFailIfDecisionDrgIdIsMissing() {
    try {
      InputStream inputStream = IoUtil.fileAsStream(MISSING_DECISION_REQUIREMENT_DIAGRAM_ID_DMN);
      dmnEngine.parseDecisionRequirementsGraph(inputStream);

      failBecauseExceptionWasNotThrown(DmnTransformException.class);
    }
    catch (DmnTransformException e) {
      assertThat(e)
        .hasCauseExactlyInstanceOf(DmnTransformException.class)
        .hasMessageStartingWith("DMN-02016")
        .hasMessageContaining("DMN-02017")
        .hasMessageContaining("DRD with Missing Id");
    }
  }

    /**
   * Reads a DMN 1.2 decision from an input stream, parses it using the DMN engine, and asserts its correctness.
   */
  @Test
  public void shouldParseDecisionFromInputStream_Dmn12() {
    InputStream inputStream = IoUtil.fileAsStream(DMN12_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

    /**
   * Test method to parse a decision from an input stream using DMN 1.3 version.
   */
  @Test
  public void shouldParseDecisionFromInputStream_Dmn13() {
    InputStream inputStream = IoUtil.fileAsStream(DMN13_NO_INPUT_DMN);
    decision = dmnEngine.parseDecision("decision", inputStream);
    assertDecision(decision, "decision");
  }

    /**
   * Asserts that the provided DmnDecision object is not null and its key matches the given key.
   *
   * @param decision the DmnDecision object to be asserted
   * @param key the key to compare the DmnDecision key to
   */
  protected void assertDecision(DmnDecision decision, String key) {
    assertThat(decision).isNotNull();
    assertThat(decision.getKey()).isEqualTo(key);
  }

    /**
   * Asserts that the given DmnDecisionRequirementsGraph is not null and has a key that matches the specified key.
   * 
   * @param drg the DmnDecisionRequirementsGraph to be checked
   * @param key the key to compare against the DmnDecisionRequirementsGraph key
   */
  protected void assertDecisionRequirementsGraph(DmnDecisionRequirementsGraph drg, String key) {
    assertThat(drg).isNotNull();
    assertThat(drg.getKey()).isEqualTo(key);
  }

}
