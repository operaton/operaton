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
package org.operaton.bpm.engine.test.standalone.el;

import jakarta.el.PropertyNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Frederik Heremans
 */
class ExpressionBeanAccessTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/standalone/el/operaton.cfg.xml")
    .build();

  RuntimeService runtimeService;

  @Deployment
  @Test
  void testConfigurationBeanAccess() {
    // given
    // Exposed bean returns 'I'm exposed' when to-string is called in first service-task
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("expressionBeanAccess");
    assertThat(runtimeService.getVariable(pi.getId(), "exposedBeanResult")).isEqualTo("I'm exposed");
    var processInstanceId = pi.getId();

    // when/then
    // After signaling, an expression tries to use a bean that is present in the configuration but
    // is not added to the beans-list
    assertThatThrownBy(() -> runtimeService.signal(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasCauseInstanceOf(PropertyNotFoundException.class);
  }
}
