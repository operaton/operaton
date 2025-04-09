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
package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Event;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.bpmn.instance.StartEvent;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;


/**
 * @author Sebastian Menski
 */
@ExtendWith(ProcessEngineExtension.class)
public class BpmnModelInstanceCmdTest {

  private static final String PROCESS_KEY = "one";

  RepositoryService repositoryService;
  
  @Deployment(resources = "org/operaton/bpm/engine/test/repository/one.bpmn20.xml")
  @Test
  public void testRepositoryService() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_KEY).singleResult().getId();

    BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);
    assertThat(modelInstance).isNotNull();

    Collection<ModelElementInstance> events = modelInstance.getModelElementsByType(modelInstance.getModel().getType(Event.class));
    assertThat(events).hasSize(2);

    Collection<ModelElementInstance> sequenceFlows = modelInstance.getModelElementsByType(modelInstance.getModel().getType(SequenceFlow.class));
    assertThat(sequenceFlows).hasSize(1);

    StartEvent startEvent = modelInstance.getModelElementById("start");
    assertThat(startEvent).isNotNull();
  }

}
