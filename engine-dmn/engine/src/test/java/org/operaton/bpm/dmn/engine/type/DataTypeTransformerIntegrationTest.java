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
package org.operaton.bpm.dmn.engine.type;

import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformerRegistry;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Tests that {@link DmnDataTypeTransformerTest} is invoked while evaluation of the
 * decision.
 *
 * @author Philipp Ossler
 */
class DataTypeTransformerIntegrationTest extends DmnEngineTest {

  private static final String DMN_OUTPUT_FILE = "org/operaton/bpm/dmn/engine/type/DataTypeTransformerTest-Output.dmn";
  private static final String DMN_INPUT_FILE = "org/operaton/bpm/dmn/engine/type/DataTypeTransformerTest-Input.dmn";
  private static final String DMN_NO_TYPE_FILE = "org/operaton/bpm/dmn/engine/type/DataTypeTransformerTest-NoTypes.dmn";

  private static final TypedValue TRANSFORMED_VALUE = Variables.integerValue(42);

  protected static DmnDataTypeTransformer dataTypeTransformerMock;

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();

    dataTypeTransformerMock = mock(DmnDataTypeTransformer.class);
    when(dataTypeTransformerMock.transform(any())).thenReturn(TRANSFORMED_VALUE);

    DmnDataTypeTransformerRegistry dataTypeTransformerRegistry = mock(DmnDataTypeTransformerRegistry.class);
    when(dataTypeTransformerRegistry.getTransformer(anyString())).thenReturn(dataTypeTransformerMock);

    configuration.getTransformer().setDataTypeTransformerRegistry(dataTypeTransformerRegistry);
    configuration.enableFeelLegacyBehavior(true);

    return configuration;
  }

  @Test
  @DecisionResource(resource = DMN_OUTPUT_FILE)
  void invokeTransformerForOutputDefinition() {
    variables.put("output", 21);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(TRANSFORMED_VALUE.getValue());

    verify(dataTypeTransformerMock, atLeastOnce()).transform(21);
  }

  @Test
  @DecisionResource(resource = DMN_OUTPUT_FILE)
  void dontInvokeTransformerForOutputDefinitionWithNull() {
    variables.put("output", null);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(null);

    verify(dataTypeTransformerMock, never()).transform(any());
  }

  @Test
  @DecisionResource(resource = DMN_INPUT_FILE)
  void invokeTransformerForInputTypeDefinition() {
    variables.put("input", 21);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("is transformed");

    verify(dataTypeTransformerMock, atLeastOnce()).transform(21);
  }

  @Test
  @DecisionResource(resource = DMN_INPUT_FILE)
  void dontInvokeTransformerForInputTypeDefinitionWithNull() {
    variables.put("input", null);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry("is not transformed");

    verify(dataTypeTransformerMock, never()).transform(any());
  }

  @Test
  @DecisionResource(resource = DMN_NO_TYPE_FILE)
  void dontInvokeTransformerForNoTypeDefinition() {
    // no type definition for input clause
    variables.put("input", 21);
    // no output definition for output clause
    variables.put("output", 42);

    assertThatDecisionTableResult()
      .hasSingleResult()
      .hasSingleEntry(42);

    verify(dataTypeTransformerMock, never()).transform(any());
  }

}
