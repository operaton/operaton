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
package org.operaton.bpm.qa.rolling.update.variable;

import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test ensures that the old engine can read an empty String variable created by the new engine.
 * Note: this test class needs to be adjusted after 7.15.0, since the behavior is fixed in 7.15.0
 * and therefore will work in rolling updates from there on
 *
 */
@ScenarioUnderTest("EmptyStringVariableScenario")
@Parameterized
class EmptyStringVariableTest extends AbstractRollingUpdateTestCase {

  @TestTemplate
  @ScenarioUnderTest("init.1")
  void shouldFindEmptyStringVariableWithValue() {
    //given
    VariableInstance variableInstance = rule.getRuntimeService().createVariableInstanceQuery()
        .variableName("myStringVar")
        .singleResult();

    // then
    assertThat(variableInstance.getValue()).isEqualTo("");
  }

  @TestTemplate
  @ScenarioUnderTest("init.1")
  void shouldQueryEmptyStringVariableWithValueEquals() {
    //given
    VariableInstanceQuery variableInstanceQuery = rule.getRuntimeService().createVariableInstanceQuery()
        .variableValueEquals("myStringVar", "");

    // then
    assertThat(variableInstanceQuery.count()).isOne();
  }

  @TestTemplate
  @ScenarioUnderTest("init.1")
  void shouldQueryEmptyStringVariableWithValueNotEquals() {
    //given
    VariableInstanceQuery variableInstanceQuery = rule.getRuntimeService().createVariableInstanceQuery()
        .variableValueNotEquals("myStringVar", "");

    // then
    assertThat(variableInstanceQuery.count()).isZero();
  }

}
