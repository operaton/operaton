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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.history.UpdateValueDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

class ImplicitVariableUpdateDisabledTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(config -> config.setImplicitVariableUpdateDetectionEnabled(false))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;

  @Deployment(resources = "org/operaton/bpm/engine/test/api/variables/ImplicitVariableUpdateTest.sequence.bpmn20.xml")
  @Test
  @SuppressWarnings("unchecked")
  void testUpdate() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
            .putValue("listVar", new ArrayList<String>())
            .putValue("delegate", new UpdateValueDelegate()));

    List<String> list = (List<String>) runtimeService.getVariable(instance.getId(), "listVar");

    assertThat(list).isEmpty(); // implicit update of 'listVar' in the java delegate was not detected.
  }

  /**
   * In addition to the previous test cases, this method ensures that
   * the variable is also not implicitly updated when only the serialized
   * value changes (without explicit instructions to change the object)
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/api/variables/ImplicitVariableUpdateTest.sequence.bpmn20.xml")
  @Test
  void testSerialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
            .putValue("pojo", new Pojo(1))
            .putValue("delegate", new NoopDelegate()));

    // at this point, the value of Pojo.foo = 1 in database.

    Pojo.shouldUpdateFoo = true; // implicitly update the value of 'foo' during deserialization.

    // will read foo = 1 from database but increment it to 2 during deserialization.
    Pojo pojo1 = (Pojo) runtimeService.getVariable(instance.getId(), "pojo");

    // foo = 2
    assertThat(pojo1.getFoo()).isEqualTo(2);

    // at this point, the database still has value of foo as 1 because implicit update detection is disabled.
    // i.e : ProcessEngineConfiguration.implicitVariableUpdateDetectionEnabled = false

    Pojo.shouldUpdateFoo = false; // turn off the implicit update of 'foo'

    // read foo again from database
    Pojo pojo2 = (Pojo) runtimeService.getVariable(instance.getId(), "pojo");

    // foo = 1 was fetched from database
    assertThat(pojo2.getFoo()).isEqualTo(1);
  }
}
