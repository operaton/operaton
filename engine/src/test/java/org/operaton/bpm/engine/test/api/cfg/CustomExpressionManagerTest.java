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
package org.operaton.bpm.engine.test.api.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.el.CommandContextFunctions;
import org.operaton.bpm.engine.impl.el.DateTimeFunctions;

/**
 * @author Thorben Lindhauer
 */
public class CustomExpressionManagerTest {

  protected ProcessEngine engine;

  @Test
  public void testBuiltinFunctionMapperRegistration() {
    // given a process engine configuration with a custom function mapper
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
        .setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName());

    CustomExpressionManager customExpressionManager = new CustomExpressionManager();
    assertThat(customExpressionManager.getFunctions()).isEmpty();
    config.setExpressionManager(customExpressionManager);

    // when the engine is initialized
    engine = config.buildProcessEngine();

    // then 4 default functions should be registered
    assertThat(config.getExpressionManager()).isSameAs(customExpressionManager);
    assertThat(customExpressionManager.getFunctions()).hasSize(4);

    Map<String, Method> functions = customExpressionManager.getFunctions();

    assertThat(functions)
            .containsKey(CommandContextFunctions.CURRENT_USER)
            .containsKey(CommandContextFunctions.CURRENT_USER_GROUPS)
            .containsKey(DateTimeFunctions.NOW)
            .containsKey(DateTimeFunctions.DATE_TIME);
  }

  @AfterEach
  public void tearDown() {
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }
}
