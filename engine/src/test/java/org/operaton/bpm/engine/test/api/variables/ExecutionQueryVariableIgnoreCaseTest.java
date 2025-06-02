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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.ExecutionQueryImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.Execution;

class ExecutionQueryVariableIgnoreCaseTest extends AbstractVariableIgnoreCaseTest<ExecutionQueryImpl, Execution> {

  RepositoryService repositoryService;
  RuntimeService runtimeService;

  @BeforeEach
  void init() {
    repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml").deploy();
    instance = runtimeService.startProcessInstanceByKey("oneTaskProcess", VARIABLES);
  }

  @AfterEach
  void tearDown() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Override
  protected ExecutionQueryImpl createQuery() {
    return (ExecutionQueryImpl) runtimeService.createExecutionQuery();
  }

  @Override
  protected void assertThatTwoInstancesAreEqual(Execution one, Execution two) {
    assertThat(one.getId()).isEqualTo(two.getId());
  }

  @Test
  void testProcessVariableNameEqualsIgnoreCase() {
    // given
    // when
    List<Execution> eq = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> eqNameLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> eqValueLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<Execution> eqNameValueLC = queryNameIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThat(eqValueLC).isEmpty();
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  void testProcessVariableNameNotEqualsIgnoreCase() {
    // given
    // when
    List<Execution> neq = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> neqNameLC = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> neqValueNE = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<Execution> neqNameLCValueNE = queryNameIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThatListContainsOnlyExpectedElement(neqNameLCValueNE, instance);
  }

  @Test
  void testProcessVariableValueEqualsIgnoreCase() {
    // given
    // when
    List<Execution> eq = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> eqNameLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> eqValueLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<Execution> eqNameValueLC = queryValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThat(eqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqNameValueLC).isEmpty();
  }

  @Test
  void testProcessVariableValueNotEqualsIgnoreCase() {
    // given
    // when
    List<Execution> neq = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> neqNameLC = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> neqValueNE = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<Execution> neqNameLCValueNE = queryValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThat(neqNameLCValueNE).isEmpty();
  }

  @Test
  void testProcessVariableNameAndValueEqualsIgnoreCase() {
    // given
    // when
    List<Execution> eq = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> eqNameLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> eqValueLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<Execution> eqValueNE = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<Execution> eqNameValueLC = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();
    List<Execution> eqNameLCValueNE = queryNameValueIgnoreCase().processVariableValueEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThatListContainsOnlyExpectedElement(eq, instance);
    assertThatListContainsOnlyExpectedElement(eqNameLC, instance);
    assertThatListContainsOnlyExpectedElement(eqValueLC, instance);
    assertThat(eqValueNE).isEmpty();
    assertThatListContainsOnlyExpectedElement(eqNameValueLC, instance);
    assertThat(eqNameLCValueNE).isEmpty();
  }

  @Test
  void testProcessVariableNameAndValueNotEqualsIgnoreCase() {
    // given
    // when
    List<Execution> neq = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE).list();
    List<Execution> neqNameLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE).list();
    List<Execution> neqValueLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_LC).list();
    List<Execution> neqValueNE = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME, VARIABLE_VALUE_NE).list();
    List<Execution> neqNameValueLC = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_LC).list();
    List<Execution> neqNameLCValueNE = queryNameValueIgnoreCase().processVariableValueNotEquals(VARIABLE_NAME_LC, VARIABLE_VALUE_NE).list();

    // then
    assertThat(neq).isEmpty();
    assertThat(neqNameLC).isEmpty();
    assertThat(neqValueLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqValueNE, instance);
    assertThat(neqNameValueLC).isEmpty();
    assertThatListContainsOnlyExpectedElement(neqNameLCValueNE, instance);
  }
}
