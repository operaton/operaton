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

import org.operaton.bpm.dmn.engine.*;
import org.operaton.bpm.dmn.engine.test.asserts.DmnDecisionTableResultAssert;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.commons.utils.IoUtil;
import static org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class DmnEngineTest {

  @RegisterExtension
  public DmnEngineTestExtension dmnEngineRule = new DmnEngineTestExtension(getDmnEngineConfiguration());

  public DmnEngine dmnEngine;
  public DmnDecision decision;
  public VariableMap variables;

  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    return null;
  }

  @BeforeEach
  public void initDmnEngine() {
    dmnEngine = dmnEngineRule.getDmnEngine();
  }

  @BeforeEach
  public void initDecision() {
    decision = dmnEngineRule.getDecision();
  }

  @BeforeEach
  public void initVariables() {
    variables = Variables.createVariables();
  }

  public VariableMap getVariables() {
    return variables;
  }

  // parsing //////////////////////////////////////////////////////////////////

  public List<DmnDecision> parseDecisionsFromFile(String filename) {
    InputStream inputStream = IoUtil.fileAsStream(filename);
    return dmnEngine.parseDecisions(inputStream);
  }

  public DmnDecision parseDecisionFromFile(String decisionKey, String filename) {
    InputStream inputStream = IoUtil.fileAsStream(filename);
    return dmnEngine.parseDecision(decisionKey, inputStream);
  }

  // evaluations //////////////////////////////////////////////////////////////

  public DmnDecisionTableResult evaluateDecisionTable() {
    return dmnEngine.evaluateDecisionTable(decision, variables);
  }

  public DmnDecisionTableResult evaluateDecisionTable(DmnEngine engine) {
    return engine.evaluateDecisionTable(decision, variables);
  }

  public DmnDecisionResult evaluateDecision() {
    return dmnEngine.evaluateDecision(decision, variables);
  }

  // assertions ///////////////////////////////////////////////////////////////

  public DmnDecisionTableResultAssert assertThatDecisionTableResult() {
    DmnDecisionTableResult results = evaluateDecisionTable(dmnEngine);
    return assertThat(results);
  }

  public DmnDecisionTableResultAssert assertThatDecisionTableResult(DmnEngine engine) {
    DmnDecisionTableResult results = evaluateDecisionTable(engine);
    return assertThat(results);
  }

}
