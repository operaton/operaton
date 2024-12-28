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
package org.operaton.bpm.dmn.engine.feel;

import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.feel.helper.CustomFunctionProvider;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFunctionConfigTest extends DmnEngineTest {

  @Override
  protected DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
    List<FeelCustomFunctionProvider> customFunctionProviders = new ArrayList<>();
    customFunctionProviders.add(new CustomFunctionProvider("myFunctionOne", "foo"));
    customFunctionProviders.add(new CustomFunctionProvider("myFunctionTwo", "bar"));
    configuration.setFeelCustomFunctionProviders(customFunctionProviders);
    configuration.init();
    return configuration;
  }

  @Test
  @DecisionResource(resource = "custom_function.dmn")
  void shouldRegisterCustomFunctions() {
    // given

    // when
    String result = evaluateDecision().getSingleEntry();

    // then
    assertThat(result).isEqualTo("foobar");
  }

}
