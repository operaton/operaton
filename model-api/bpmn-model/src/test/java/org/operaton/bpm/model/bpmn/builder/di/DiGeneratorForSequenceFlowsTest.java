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
package org.operaton.bpm.model.bpmn.builder.di;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.END_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SEQUENCE_FLOW_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnEdge;

class DiGeneratorForSequenceFlowsTest {

  private BpmnModelInstance instance;

  @AfterEach
  void validateModel() throws IOException {
    if (instance != null) {
      Bpmn.validateModel(instance);
    }
  }

  @Test
  void shouldGenerateEdgeForSequenceFlow() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
                  .startEvent(START_EVENT_ID)
                  .sequenceFlowId(SEQUENCE_FLOW_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    assertEquals(1, allEdges.size());

    assertBpmnEdgeExists(SEQUENCE_FLOW_ID);
  }

  @Test
  void shouldGenerateEdgesForSequenceFlowsUsingGateway() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId("s1")
        .parallelGateway("gateway")
        .sequenceFlowId("s2")
        .endEvent("e1")
        .moveToLastGateway()
        .sequenceFlowId("s3")
        .endEvent("e2")
        .done();

    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    assertEquals(3, allEdges.size());

    assertBpmnEdgeExists("s1");
    assertBpmnEdgeExists("s2");
    assertBpmnEdgeExists("s3");
  }

  @Test
  void shouldGenerateEdgesWhenUsingMoveToActivity() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId("s1")
        .exclusiveGateway()
        .sequenceFlowId("s2")
        .userTask(USER_TASK_ID)
        .sequenceFlowId("s3")
        .endEvent("e1")
        .moveToActivity(USER_TASK_ID)
        .sequenceFlowId("s4")
        .endEvent("e2")
        .done();

    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    assertEquals(4, allEdges.size());

    assertBpmnEdgeExists("s1");
    assertBpmnEdgeExists("s2");
    assertBpmnEdgeExists("s3");
    assertBpmnEdgeExists("s4");
  }

  @Test
  void shouldGenerateEdgesWhenUsingMoveToNode() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId("s1")
        .exclusiveGateway()
        .sequenceFlowId("s2")
        .userTask(USER_TASK_ID)
        .sequenceFlowId("s3")
        .endEvent("e1")
        .moveToNode(USER_TASK_ID)
        .sequenceFlowId("s4")
        .endEvent("e2")
        .done();

    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    assertEquals(4, allEdges.size());

    assertBpmnEdgeExists("s1");
    assertBpmnEdgeExists("s2");
    assertBpmnEdgeExists("s3");
    assertBpmnEdgeExists("s4");
  }

  @Test
  void shouldGenerateEdgesWhenUsingConnectTo() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId("s1")
        .exclusiveGateway("gateway")
        .sequenceFlowId("s2")
        .userTask(USER_TASK_ID)
        .sequenceFlowId("s3")
        .endEvent(END_EVENT_ID)
        .moveToNode(USER_TASK_ID)
        .sequenceFlowId("s4")
        .connectTo("gateway")
        .done();

    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    assertEquals(4, allEdges.size());

    assertBpmnEdgeExists("s1");
    assertBpmnEdgeExists("s2");
    assertBpmnEdgeExists("s3");
    assertBpmnEdgeExists("s4");
  }

  protected BpmnEdge findBpmnEdge(String sequenceFlowId) {
    Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
    Iterator<BpmnEdge> iterator = allEdges.iterator();

    while (iterator.hasNext()) {
      BpmnEdge edge = iterator.next();
      if(edge.getBpmnElement().getId().equals(sequenceFlowId)) {
        return edge;
      }
    }
    return null;
  }

  protected void assertBpmnEdgeExists(String id) {
    BpmnEdge edge = findBpmnEdge(id);
    assertNotNull(edge);
  }
}
