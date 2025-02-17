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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Philipp Ossler
 */
@RunWith(Parameterized.class)
public class PrimitiveTypeValueSerializationTest {

  protected static final String BPMN_FILE = "org/operaton/bpm/engine/test/api/variables/oneTaskProcess.bpmn20.xml";
  protected static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  protected static final String VARIABLE_NAME = "variable";

  @Parameters(name = "{index}: variable = {0}")
  public static Collection<Object[]> data() {
    return Arrays
        .asList(new Object[][] {
          { Variables.stringValue("a"), Variables.stringValue(null) },
          { Variables.booleanValue(true), Variables.booleanValue(null) },
          { Variables.integerValue(4), Variables.integerValue(null) },
          { Variables.shortValue((short) 2), Variables.shortValue(null) },
          { Variables.longValue(6L), Variables.longValue(null) },
          { Variables.doubleValue(4.2), Variables.doubleValue(null) },
          { Variables.dateValue(new Date()), Variables.dateValue(null) }
        });
  }

  @Parameter(0)
  public TypedValue typedValue;

  @Parameter(1)
  public TypedValue nullValue;

  private RuntimeService runtimeService;
  private RepositoryService repositoryService;
  private String deploymentId;

  @Rule
  public ProcessEngineRule rule = new ProvidedProcessEngineRule();

  @Before
  public void setup() {
    runtimeService = rule.getRuntimeService();
    repositoryService = rule.getRepositoryService();

    deploymentId = repositoryService.createDeployment().addClasspathResource(BPMN_FILE).deploy().getId();
  }

  @After
  public void teardown() {
    repositoryService.deleteDeployment(deploymentId, true);
  }

  @Test
  public void shouldGetUntypedVariable() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    runtimeService.setVariable(instance.getId(), VARIABLE_NAME, typedValue);

    Object variableValue = runtimeService.getVariable(instance.getId(), VARIABLE_NAME);
    assertThat(variableValue).isEqualTo(typedValue.getValue());
  }

  @Test
  public void shouldGetTypedVariable() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    runtimeService.setVariable(instance.getId(), VARIABLE_NAME, typedValue);

    TypedValue typedVariableValue = runtimeService.getVariableTyped(instance.getId(), VARIABLE_NAME);
    assertThat(typedVariableValue.getType()).isEqualTo(typedValue.getType());
    assertThat(typedVariableValue.getValue()).isEqualTo(typedValue.getValue());
  }

  @Test
  public void shouldGetTypedNullVariable() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    runtimeService.setVariable(instance.getId(), VARIABLE_NAME, nullValue);

    assertThat(runtimeService.getVariable(instance.getId(), VARIABLE_NAME)).isNull();

    TypedValue typedVariableValue = runtimeService.getVariableTyped(instance.getId(), VARIABLE_NAME);
    assertThat(typedVariableValue.getType()).isEqualTo(nullValue.getType());
    assertThat(typedVariableValue.getValue()).isNull();
  }

}
