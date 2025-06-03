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
package org.operaton.bpm.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ClassLoadingException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.util.CollectionUtil;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.servicetask.util.GenderBean;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
@ExtendWith(ProcessEngineExtension.class)
class JavaServiceTaskTest {

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;

  @Deployment
  @Test
  void testJavaServiceDelegation() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("javaServiceDelegation", CollectionUtil.singletonMap("input", "Activiti BPM Engine"));
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState")
      .singleResult();
    assertThat(runtimeService.getVariable(execution.getId(), "input")).isEqualTo("ACTIVITI BPM ENGINE");
  }

  @Deployment
  @Test
  void testFieldInjection() {
    // Process contains 2 service-tasks using field-injection. One should use the exposed setter,
    // the other is using the private field.
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("fieldInjection");
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState")
      .singleResult();

    assertThat(runtimeService.getVariable(execution.getId(), "var")).isEqualTo("HELLO WORLD");
    assertThat(runtimeService.getVariable(execution.getId(), "setterVar")).isEqualTo("HELLO SETTER");
  }

  @Deployment
  @Test
  void testExpressionFieldInjection() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", "kermit");
    vars.put("gender", "male");
    vars.put("genderBean", new GenderBean());

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("expressionFieldInjection", vars);
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState")
      .singleResult();

    assertThat(runtimeService.getVariable(execution.getId(), "var2")).isEqualTo("timrek .rM olleH");
    assertThat(runtimeService.getVariable(execution.getId(), "var1")).isEqualTo("elam :si redneg ruoY");
  }

  @Deployment
  @Test
  void testUnexistingClassDelegation() {
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("unexistingClassDelegation"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Exception while instantiating class 'org.operaton.bpm.engine.test.BogusClass'")
      .hasCauseInstanceOf(ClassLoadingException.class);
  }

  @Test
  void testIllegalUseOfResultVariableName() {
    var deploymentBuilder = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/bpmn/servicetask/JavaServiceTaskTest.testIllegalUseOfResultVariableName.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("resultVariable");
    }
  }

  @Deployment
  @Test
  void testExceptionHandling() {

    // If variable value is != 'throw-exception', process goes
    // through service task and ends immediately
    Map<String, Object> vars = new HashMap<>();
    vars.put("var", "no-exception");
    runtimeService.startProcessInstanceByKey("exceptionHandling", vars);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // If variable value == 'throw-exception', process executes
    // service task, which generates and catches exception,
    // and takes sequence flow to user task
    vars.put("var", "throw-exception");
    runtimeService.startProcessInstanceByKey("exceptionHandling", vars);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Fix Exception");
  }

  @Deployment
  @Test
  void testGetBusinessKeyFromDelegateExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("businessKeyProcess", "1234567890");
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("businessKeyProcess").count()).isEqualTo(1);

    // Check if business-key was available from the process
    String key = (String) runtimeService.getVariable(processInstance.getId(), "businessKeySetOnExecution");
    assertThat(key).isNotNull().isEqualTo("1234567890");

    // check if BaseDelegateExecution#getBusinessKey() behaves like DelegateExecution#getProcessBusinessKey()
    String key2 = (String) runtimeService.getVariable(processInstance.getId(), "businessKeyAsProcessBusinessKey");
    assertThat(key).isEqualTo(key2);
  }

}
