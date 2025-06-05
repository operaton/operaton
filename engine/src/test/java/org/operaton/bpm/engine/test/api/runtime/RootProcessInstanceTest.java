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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Tassilo Weidner
 */
class RootProcessInstanceTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  FormService formService;

  static final String CALLED_PROCESS_KEY = "calledProcess";
  static final BpmnModelInstance CALLED_PROCESS = Bpmn.createExecutableProcess(CALLED_PROCESS_KEY)
    .startEvent()
      .userTask("userTask")
    .endEvent().done();

  static final String CALLED_AND_CALLING_PROCESS_KEY = "calledAndCallingProcess";
  static final BpmnModelInstance CALLED_AND_CALLING_PROCESS =
    Bpmn.createExecutableProcess(CALLED_AND_CALLING_PROCESS_KEY)
    .startEvent()
      .callActivity()
        .calledElement(CALLED_PROCESS_KEY)
    .endEvent().done();

  static final String CALLING_PROCESS_KEY = "callingProcess";
  static final BpmnModelInstance CALLING_PROCESS = Bpmn.createExecutableProcess(CALLING_PROCESS_KEY)
    .startEvent()
      .callActivity()
        .calledElement(CALLED_AND_CALLING_PROCESS_KEY)
    .endEvent().done();

  @Test
  void shouldPointToItself() {
    // given
    testRule.deploy(CALLED_PROCESS);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(CALLED_PROCESS_KEY);

    // assume
    assertThat(processInstance.getRootProcessInstanceId()).isNotNull();

    // then
    assertThat(processInstance.getRootProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
  }

  @Test
  void shouldPointToItselfBySubmittingStartForm() {
    // given
    DeploymentWithDefinitions deployment = testRule.deploy(CALLED_PROCESS);

    String processDefinitionId = deployment.getDeployedProcessDefinitions().get(0).getId();
    Map<String, Object> properties = new HashMap<>();

    // when
    ProcessInstance processInstance = formService.submitStartForm(processDefinitionId, properties);

    // assume
    assertThat(processInstance.getRootProcessInstanceId()).isNotNull();

    // then
    assertThat(processInstance.getRootProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
  }

  @Test
  void shouldPointToItselfByStartingAtActivity() {
    // given
    testRule.deploy(CALLED_PROCESS);

    // when
    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey(CALLED_PROCESS_KEY)
      .startAfterActivity("userTask")
      .execute();

    // assume
    assertThat(processInstance.getRootProcessInstanceId()).isNotNull();

    // then
    assertThat(processInstance.getRootProcessInstanceId()).isEqualTo(processInstance.getProcessInstanceId());
  }

  @Test
  void shouldPointToRoot() {
    // given
    testRule.deploy(CALLED_PROCESS);
    testRule.deploy(CALLED_AND_CALLING_PROCESS);
    testRule.deploy(CALLING_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey(CALLING_PROCESS_KEY);

    ProcessInstance calledProcessInstance = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(CALLED_PROCESS_KEY)
      .singleResult();

    ProcessInstance calledAndCallingProcessInstance = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(CALLED_AND_CALLING_PROCESS_KEY)
      .singleResult();

    ProcessInstance callingProcessInstance = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(CALLING_PROCESS_KEY)
      .singleResult();

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3L);
    assertThat(callingProcessInstance.getRootProcessInstanceId()).isNotNull();

    // then
    assertThat(callingProcessInstance.getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
    assertThat(calledProcessInstance.getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
    assertThat(calledAndCallingProcessInstance.getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
  }

  @Test
  void shouldPointToRootWithInitialCallAfterParallelGateway() {
    // given
    testRule.deploy(CALLED_PROCESS);

    testRule.deploy(CALLED_AND_CALLING_PROCESS);

    testRule.deploy(Bpmn.createExecutableProcess("callingProcessWithGateway")
      .startEvent()
        .parallelGateway("split")
          .callActivity()
            .calledElement(CALLED_AND_CALLING_PROCESS_KEY)
          .moveToNode("split")
            .callActivity()
              .calledElement(CALLED_AND_CALLING_PROCESS_KEY)
      .endEvent().done());

    // when
    runtimeService.startProcessInstanceByKey("callingProcessWithGateway");

    List<ProcessInstance> calledProcessInstances = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(CALLED_PROCESS_KEY)
      .list();

    List<ProcessInstance> calledAndCallingProcessInstances = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(CALLED_AND_CALLING_PROCESS_KEY)
      .list();

    ProcessInstance callingProcessInstance = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("callingProcessWithGateway")
      .singleResult();

    // assume
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(5L);
    assertThat(callingProcessInstance.getProcessInstanceId()).isNotNull();

    assertThat(calledProcessInstances).hasSize(2);
    assertThat(calledAndCallingProcessInstances).hasSize(2);

    // then
    assertThat(callingProcessInstance.getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());

    assertThat(calledProcessInstances.get(0).getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
    assertThat(calledProcessInstances.get(1).getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());

    assertThat(calledAndCallingProcessInstances.get(0).getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
    assertThat(calledAndCallingProcessInstances.get(1).getRootProcessInstanceId()).isEqualTo(callingProcessInstance.getProcessInstanceId());
  }

}
