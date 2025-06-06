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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.test.api.runtime.util.TestVariableScope;
import org.operaton.bpm.engine.variable.VariableMap;

/**
 * @author Daniel Meyer
 *
 */
class VariableScopeTest {

  private static final String VAR_NAME = "foo";

  private static final String VAR_VALUE_STRING = "bar";

  private VariableScope variableScope;

  @BeforeEach
  void setUp() {
    this.variableScope = new TestVariableScope();
    variableScope.setVariable(VAR_NAME, VAR_VALUE_STRING);
  }

  @Test
  void testGetVariables() {
    Map<String, Object> variables = variableScope.getVariables();
    assertThat(variables)
            .isNotNull()
            .containsEntry(VAR_NAME, VAR_VALUE_STRING);
  }

  @Test
  void testGetVariablesTyped() {
    VariableMap variables = variableScope.getVariablesTyped();
    assertThat(variables)
            .isNotNull()
            .containsEntry(VAR_NAME, VAR_VALUE_STRING);
    assertThat(variableScope.getVariablesTyped(true)).isEqualTo(variables);
  }

  @Test
  void testGetVariablesLocal() {
    Map<String, Object> variables = variableScope.getVariablesLocal();
    assertThat(variables)
            .isNotNull()
            .containsEntry(VAR_NAME, VAR_VALUE_STRING);
  }

  @Test
  void testGetVariablesLocalTyped() {
    Map<String, Object> variables = variableScope.getVariablesLocalTyped();
    assertThat(variables)
            .isNotNull()
            .containsEntry(VAR_NAME, VAR_VALUE_STRING);
    assertThat(variableScope.getVariablesLocalTyped(true)).isEqualTo(variables);
  }

}
