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
package org.operaton.bpm.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Event;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ProcessEngineExtension.class)
class ServiceTaskBpmnModelExecutionContextTest {

  private static final String PROCESS_ID = "process";
  private String deploymentId;

  RuntimeService runtimeService;
  RepositoryService repositoryService;

  @Test
  void testJavaDelegateModelExecutionContext() {
    deploy();

    runtimeService.startProcessInstanceByKey(PROCESS_ID);

    BpmnModelInstance modelInstance = ModelExecutionContextServiceTask.modelInstance;
    assertThat(modelInstance).isNotNull();

    Model model = modelInstance.getModel();
    Collection<ModelElementInstance> events = modelInstance.getModelElementsByType(model.getType(Event.class));
    assertThat(events).hasSize(2);
    Collection<ModelElementInstance> tasks = modelInstance.getModelElementsByType(model.getType(Task.class));
    assertThat(tasks).hasSize(1);

    Process process = (Process) modelInstance.getDefinitions().getRootElements().iterator().next();
    assertThat(process.getId()).isEqualTo(PROCESS_ID);
    assertThat(process.isExecutable()).isTrue();

    ServiceTask serviceTask = ModelExecutionContextServiceTask.serviceTask;
    assertThat(serviceTask).isNotNull();
    assertThat(serviceTask.getOperatonClass()).isEqualTo(ModelExecutionContextServiceTask.class.getName());
  }

  private void deploy() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_ID)
      .startEvent()
      .serviceTask()
        .operatonClass(ModelExecutionContextServiceTask.class.getName())
      .endEvent()
      .done();

    deploymentId = repositoryService.createDeployment().addModelInstance("process.bpmn", modelInstance).deploy().getId();
  }

  @AfterEach
  void tearDown() {
    ModelExecutionContextServiceTask.clear();
    repositoryService.deleteDeployment(deploymentId, true);
  }

}
