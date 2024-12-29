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
package org.operaton.bpm.model.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.Input;
import org.junit.jupiter.api.AfterEach;

public class OperatonExtensionsTest {

  private DmnModelInstance modelInstance;

   public static Collection<Object[]> parameters(){
     return Arrays.asList(new Object[][]{
         {Dmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsTest.dmn"))},
         // for compatibility reasons we gotta check the old namespace, too
         {Dmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatibilityTest.dmn"))}
     });
   }

  public void initOperatonExtensionsTest(DmnModelInstance originalModelInstance) {
    this.modelInstance = originalModelInstance.clone();
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonClauseOutput(DmnModelInstance originalModelInstance) {
    initOperatonExtensionsTest(originalModelInstance);
    Input input = modelInstance.getModelElementById("input");
    assertThat(input.getOperatonInputVariable()).isEqualTo("myVariable");
    input.setOperatonInputVariable("foo");
    assertThat(input.getOperatonInputVariable()).isEqualTo("foo");
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonHistoryTimeToLive(DmnModelInstance originalModelInstance) {
    initOperatonExtensionsTest(originalModelInstance);
    Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getOperatonHistoryTimeToLive()).isEqualTo(5);
    decision.setOperatonHistoryTimeToLive(6);
    assertThat(decision.getOperatonHistoryTimeToLive()).isEqualTo(6);
  }

  @MethodSource("parameters")
  @ParameterizedTest(name = "Namespace: {0}")
  void testOperatonVersionTag(DmnModelInstance originalModelInstance) {
    initOperatonExtensionsTest(originalModelInstance);
    Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getVersionTag()).isEqualTo("1.0.0");
    decision.setVersionTag("1.1.0");
    assertThat(decision.getVersionTag()).isEqualTo("1.1.0");
  }

  @AfterEach
  void validateModel() {
    Dmn.validateModel(modelInstance);
  }

}
