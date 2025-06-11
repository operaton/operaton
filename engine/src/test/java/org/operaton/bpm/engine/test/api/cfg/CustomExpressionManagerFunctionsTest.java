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
package org.operaton.bpm.engine.test.api.cfg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExpressionManagerFunctionsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;

  @BeforeEach
  void initializeServices() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    runtimeService = engineRule.getRuntimeService();
  }

  @Test
  void shouldResolveCustomFunction() {
    // given
    processEngineConfiguration.getExpressionManager().addFunction("foobar", ReflectUtil.getMethod(TestFunctions.class, "foobar"));
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
       .startEvent()
       .serviceTask().operatonExpression("${execution.setVariable(\"baz\", foobar())}")
       .userTask()
       .endEvent()
       .done());
    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    // then
    assertThat(runtimeService.getVariable(processInstanceId, "baz")).isEqualTo("foobar");
  }

  @Test
  void shouldResolveCustomPrefixedFunction() {
    // given
    processEngineConfiguration.getExpressionManager().addFunction("foo:bar", ReflectUtil.getMethod(TestFunctions.class, "foobar"));
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .serviceTask().operatonExpression("${execution.setVariable(\"baz\", foo:bar())}")
        .userTask()
        .endEvent()
        .done());
     // when
     String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
     // then
     assertThat(runtimeService.getVariable(processInstanceId, "baz")).isEqualTo("foobar");
  }

  public static class TestFunctions {
    public static String foobar() {
      return "foobar";
    }
  }
}
