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
package org.operaton.bpm.engine.test.bpmn.event.end;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Kristin Polenz
 * @author Nico Rehwaldt
 */
class MessageEndEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  
  RuntimeService runtimeService;

  @Deployment
  @Test
  void testMessageEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(processInstance).isNotNull();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testMessageEndEventServiceTaskBehavior() {
    Map<String, Object> variables = new HashMap<>();

    // class
    variables.put("wasExecuted", true);
    variables.put("expressionWasExecuted", false);
    variables.put("delegateExpressionWasExecuted", false);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);
    assertThat(processInstance).isNotNull();

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(DummyServiceTask.wasExecuted).isTrue();

    // expression
    variables = new HashMap<>();
    variables.put("wasExecuted", false);
    variables.put("expressionWasExecuted", true);
    variables.put("delegateExpressionWasExecuted", false);
    variables.put("endEventBean", new EndEventBean());
    processInstance = runtimeService.startProcessInstanceByKey("process", variables);
    assertThat(processInstance).isNotNull();

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(DummyServiceTask.expressionWasExecuted).isTrue();

    // delegate expression
    variables = new HashMap<>();
    variables.put("wasExecuted", false);
    variables.put("expressionWasExecuted", false);
    variables.put("delegateExpressionWasExecuted", true);
    variables.put("endEventBean", new EndEventBean());
    processInstance = runtimeService.startProcessInstanceByKey("process", variables);
    assertThat(processInstance).isNotNull();

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(DummyServiceTask.delegateExpressionWasExecuted).isTrue();
  }

}
