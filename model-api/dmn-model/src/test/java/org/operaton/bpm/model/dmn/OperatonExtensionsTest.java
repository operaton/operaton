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

import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.Input;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class OperatonExtensionsTest {

  private final DmnModelInstance originalModelInstance;
  private DmnModelInstance modelInstance;

   @Parameters(name="Namespace: {0}")
   public static Collection<Object[]> parameters(){
     return Arrays.asList(new Object[][]{
         {Dmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsTest.dmn"))},
         // for compatibility reasons we gotta check the old namespace, too
         {Dmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatibilityTest.dmn"))}
     });
   }

  public OperatonExtensionsTest(DmnModelInstance originalModelInstance) {
    this.originalModelInstance = originalModelInstance;
  }

  @Before
  public void parseModel() {
    modelInstance = originalModelInstance.clone();

  }

  @Test
  public void testOperatonClauseOutput() {
    Input input = modelInstance.getModelElementById("input");
    assertThat(input.getOperatonInputVariable()).isEqualTo("myVariable");
    input.setOperatonInputVariable("foo");
    assertThat(input.getOperatonInputVariable()).isEqualTo("foo");
  }

  @Test
  public void testOperatonHistoryTimeToLive() {
    Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getOperatonHistoryTimeToLive()).isEqualTo(5);
    decision.setOperatonHistoryTimeToLive(6);
    assertThat(decision.getOperatonHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  public void testOperatonVersionTag() {
    Decision decision = modelInstance.getModelElementById("decision");
    assertThat(decision.getVersionTag()).isEqualTo("1.0.0");
    decision.setVersionTag("1.1.0");
    assertThat(decision.getVersionTag()).isEqualTo("1.1.0");
  }

  @After
  public void validateModel() {
    Dmn.validateModel(modelInstance);
  }

}
