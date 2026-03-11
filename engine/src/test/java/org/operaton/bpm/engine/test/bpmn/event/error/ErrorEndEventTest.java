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
package org.operaton.bpm.engine.test.bpmn.event.error;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorEndEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/error/testPropagateOutputVariablesWhileThrowError.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventTest.errorParent.bpmn20.xml"})
  void testPropagateOutputVariablesWhileThrowError() {
    // given
    Map<String,Object> variables = new HashMap<>();
    variables.put("input", 42);
    String processInstanceId = runtimeService.startProcessInstanceByKey("ErrorParentProcess", variables).getId();

    // when
    String id = taskService.createTaskQuery().taskName("ut2").singleResult().getId();
    taskService.complete(id);

    // then
    assertThat(taskService.createTaskQuery().taskName("task after catched error").count()).isOne();
    // and set the output variable of the called process to the process
    assertThat(runtimeService.getVariable(processInstanceId, "cancelReason")).isNotNull();
    assertThat(runtimeService.getVariable(processInstanceId, "output")).isEqualTo(42);
  }

  @Test
  @Deployment
  void testErrorMessage() {
    // given a process definition including an error with operaton:errorMessage property
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("testErrorMessage");

    // when
    Map<String, Object> variables = runtimeService.getVariables(instance.getId());

    // then the error message defined in XML is accessible
    assertThat((String) variables.get("errorCode")).isEqualTo("123");
    assertThat((String) variables.get("errorMessage")).isEqualTo("This is the error message indicating what went wrong.");
  }


  @Test
  @Deployment
  void testErrorMessageExpression() {
    // given a process definition including an error with operaton:errorMessage property with an expression value
    String errorMessage = "This is the error message indicating what went wrong.";
    Map<String, Object> initialVariables = new HashMap<>();
    initialVariables.put("errorMessageExpression", errorMessage);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("testErrorMessageExpression", initialVariables);

    // when
    Map<String, Object> variables = runtimeService.getVariables(instance.getId());

    // then the error message expression is resolved
    assertThat((String) variables.get("errorCode")).isEqualTo("123");
    assertThat((String) variables.get("errorMessage")).isEqualTo(errorMessage);
  }

  @Test
  @Deployment
  void testError() {
    // given a process definition including an error
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("testError");

    // when
    Map<String, Object> variables = runtimeService.getVariables(instance.getId());

    // then the error message defined in XML is accessible
    assertThat((String) variables.get("errorCode")).isEqualTo("123");
    assertThat(variables.get("errorMessage")).isNull();
  }
}
