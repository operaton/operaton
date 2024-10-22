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
package org.operaton.bpm.dmn.engine.test;

import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.commons.utils.IoUtil;
import org.junit.Before;
import org.junit.Rule;

public abstract class DmnEngineTest {

  @Rule
  public DmnEngineTestRule dmnEngineRule = new DmnEngineTestRule(getDmnEngineConfiguration());

  public DmnEngine dmnEngine;
  public DmnDecision decision;
  public VariableMap variables;

    /**
   * Returns the DMN engine configuration.
   *
   * @return the DMN engine configuration
   */
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    return null;
  }

    /**
   * Initializes the DMN engine by getting it from the DMN engine rule.
   */
  @Before
  public void initDmnEngine() {
    dmnEngine = dmnEngineRule.getDmnEngine();
  }

    /**
   * Initializes the decision by retrieving it from the DMN engine rule.
   */
  @Before
  public void initDecision() {
    decision = dmnEngineRule.getDecision();
  }

    /**
   * Initializes variables by creating a new instance of Variables.
   */
  @Before
  public void initVariables() {
    variables = Variables.createVariables();
  }

    /**
   * Returns the VariableMap containing all variables.
   *
   * @return the VariableMap containing all variables
   */
  public VariableMap getVariables() {
    return variables;
  }

  // parsing //////////////////////////////////////////////////////////////////

    /**
   * Parses DMN decisions from a file.
   * 
   * @param filename the name of the file to parse decisions from
   * @return a list of parsed DMN decisions
   */
  public List<DmnDecision> parseDecisionsFromFile(String filename) {
    InputStream inputStream = IoUtil.fileAsStream(filename);
    return dmnEngine.parseDecisions(inputStream);
  }

    /**
   * Parses a DMN decision from a file.
   *
   * @param decisionKey the key of the decision to parse
   * @param filename the name of the file containing the DMN decision
   * @return the parsed DMN decision
   */
  public DmnDecision parseDecisionFromFile(String decisionKey, String filename) {
    InputStream inputStream = IoUtil.fileAsStream(filename);
    return dmnEngine.parseDecision(decisionKey, inputStream);
  }

  // evaluations //////////////////////////////////////////////////////////////

    /**
   * Evaluates the decision table using the provided decision and variables.
   *
   * @return the result of the evaluation
   */
  public DmnDecisionTableResult evaluateDecisionTable() {
    return dmnEngine.evaluateDecisionTable(decision, variables);
  }

    /**
   * Evaluates a DMN decision table using the provided DMN engine.
   *
   * @param engine the DMN engine used to evaluate the decision table
   * @return the result of the evaluation
   */
  public DmnDecisionTableResult evaluateDecisionTable(DmnEngine engine) {
    return engine.evaluateDecisionTable(decision, variables);
  }

    /**
   * Evaluates a DMN decision using the decision engine and provided variables.
   * 
   * @return the result of the decision evaluation
   */
  public DmnDecisionResult evaluateDecision() {
    return dmnEngine.evaluateDecision(decision, variables);
  }

  // assertions ///////////////////////////////////////////////////////////////

    /**
   * Asserts the result of a DMN decision table evaluation.
   * 
   * @return an instance of DmnDecisionTableResultAssert for making assertions on the decision table result
   */
  public DmnDecisionTableResultAssert assertThatDecisionTableResult() {
    DmnDecisionTableResult results = evaluateDecisionTable(dmnEngine);
    return assertThat(results);
  }

    /**
   * Asserts the decision table result using the given DMN engine.
   * 
   * @param engine the DMN engine used to evaluate the decision table
   * @return an assertion object for the decision table result
   */
  public DmnDecisionTableResultAssert assertThatDecisionTableResult(DmnEngine engine) {
    DmnDecisionTableResult results = evaluateDecisionTable(engine);
    return assertThat(results);
  }

}
