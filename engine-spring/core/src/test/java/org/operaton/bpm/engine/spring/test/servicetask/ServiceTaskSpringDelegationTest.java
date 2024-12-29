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
package org.operaton.bpm.engine.spring.test.servicetask;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/servicetask/servicetaskSpringTest-context.xml")
class ServiceTaskSpringDelegationTest extends SpringProcessEngineTestCase {

  @Deployment
  @Test
  void delegateExpression() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("delegateExpressionToSpringBean");
    assertThat(runtimeService.getVariable(procInst.getId(), "myVar")).isEqualTo("Operaton BPMN 2.0 process engine");
    assertThat(runtimeService.getVariable(procInst.getId(), "fieldInjection")).isEqualTo("fieldInjectionWorking");
  }

  @Deployment
  @Test
  void delegateClass() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("delegateClassToSpringBean");
    assertThat(runtimeService.getVariable(procInst.getId(), "myVar")).isEqualTo("Operaton BPMN 2.0 process engine");
    assertThat(runtimeService.getVariable(procInst.getId(), "fieldInjection")).isEqualTo("fieldInjectionWorking");
  }

  @Deployment
  @Test
  void delegateClassNotABean() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("delegateClassToSpringBean");
    assertThat(runtimeService.getVariable(procInst.getId(), "message")).isEqualTo("DelegateClassNotABean was called");
    assertThat((Boolean) runtimeService.getVariable(procInst.getId(), "injectedFieldIsNull")).isTrue();
  }

  @Deployment
  @Test
  void methodExpressionOnSpringBean() {
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("methodExpressionOnSpringBean");
    assertThat(runtimeService.getVariable(procInst.getId(), "myVar")).isEqualTo("Operaton BPMN 2.0 PROCESS ENGINE");
  }

  @Deployment
  @Test
  void executionAndTaskListenerDelegationExpression() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionAndTaskListenerDelegation");
    assertThat(runtimeService.getVariable(processInstance.getId(), "executionListenerVar")).isEqualTo("working");
    assertThat(runtimeService.getVariable(processInstance.getId(), "taskListenerVar")).isEqualTo("working");

    assertThat(runtimeService.getVariable(processInstance.getId(), "executionListenerField")).isEqualTo("executionListenerInjection");
    assertThat(runtimeService.getVariable(processInstance.getId(), "taskListenerField")).isEqualTo("taskListenerInjection");
  }
  
}
