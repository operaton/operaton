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
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JuelFeelBehaviorTest extends FeelBehavior {

  @Override
  public DmnEngineConfiguration getDmnEngineConfiguration() {
    DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
    configuration.setFeelEngineFactory(new FeelEngineFactoryImpl());
    configuration.init();
    return configuration;
  }

  /**
   * For expression languages, so-called context functions can be used [1].
   * <p>
   * This test ensures that context functions cannot be called in the
   * juel as well as the scala-based implementation.
   * </p>
   * @see
   * <a href="https://docs.operaton.org/manual/user-guide/process-engine/expression-language/#internal-context-functions">Internal Context Functions</a>
   */
  @Test
  @DecisionResource(resource = "context_function.dmn")
  void shouldFailOnInternalContextFunctions() {
    assertThrows(FeelException.class, () -> {
      // given
      getVariables().putValue("myDate", new Date());

      // when
      evaluateDecision().getSingleEntry();
    });
  }

}
