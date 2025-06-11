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
package org.operaton.bpm.model.bpmn.builder.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.END_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SEND_TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SEQUENCE_FLOW_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SERVICE_TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SUB_PROCESS_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;

import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.operaton.bpm.model.bpmn.instance.dc.Bounds;
import org.operaton.bpm.model.bpmn.instance.di.Waypoint;

class CoordinatesGenerationTest {

  private BpmnModelInstance instance;

  @Test
  void shouldPlaceStartEvent() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .done();

    Bounds startBounds = findBpmnShape(START_EVENT_ID).getBounds();
    assertShapeCoordinates(startBounds, 100, 100);
  }

  @Test
  void shouldPlaceUserTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .userTask(USER_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceSendTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .sendTask(SEND_TASK_ID)
        .done();

    Bounds sendTaskBounds = findBpmnShape(SEND_TASK_ID).getBounds();
    assertShapeCoordinates(sendTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceServiceTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .serviceTask(SERVICE_TASK_ID)
        .done();

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceReceiveTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .receiveTask(TASK_ID)
        .done();

    Bounds receiveTaskBounds = findBpmnShape(TASK_ID).getBounds();
    assertShapeCoordinates(receiveTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceManualTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .manualTask(TASK_ID)
        .done();

    Bounds manualTaskBounds = findBpmnShape(TASK_ID).getBounds();
    assertShapeCoordinates(manualTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceBusinessRuleTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .businessRuleTask(TASK_ID)
        .done();

    Bounds businessRuleTaskBounds = findBpmnShape(TASK_ID).getBounds();
    assertShapeCoordinates(businessRuleTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceScriptTask() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .scriptTask(TASK_ID)
        .done();

    Bounds scriptTaskBounds = findBpmnShape(TASK_ID).getBounds();
    assertShapeCoordinates(scriptTaskBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceCatchingIntermediateEvent() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .intermediateCatchEvent("id")
        .done();

    Bounds catchEventBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(catchEventBounds, 186, 100);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceThrowingIntermediateEvent() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .intermediateThrowEvent("id")
        .done();

    Bounds throwEventBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(throwEventBounds, 186, 100);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceEndEvent() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .done();

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 186, 100);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceCallActivity() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .callActivity("id")
        .done();

    Bounds callActivityBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(callActivityBounds, 186, 78);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceExclusiveGateway() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .exclusiveGateway("id")
        .done();

    Bounds gatewayBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(gatewayBounds, 186, 93);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceInclusiveGateway() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .inclusiveGateway("id")
        .done();

    Bounds gatewayBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(gatewayBounds, 186, 93);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceParallelGateway() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .parallelGateway("id")
        .done();

    Bounds gatewayBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(gatewayBounds, 186, 93);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceEventBasedGateway() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .eventBasedGateway()
          .id("id")
        .done();

    Bounds gatewayBounds = findBpmnShape("id").getBounds();
    assertShapeCoordinates(gatewayBounds, 186, 93);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceBlankSubProcess() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .subProcess(SUB_PROCESS_ID)
        .done();

    Bounds subProcessBounds = findBpmnShape(SUB_PROCESS_ID).getBounds();
    assertShapeCoordinates(subProcessBounds, 186, 18);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 136, 118);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 186, 118);

  }

  @Test
  void shouldPlaceBoundaryEventForTask() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .userTask(USER_TASK_ID)
        .boundaryEvent("boundary")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(USER_TASK_ID)
        .endEvent()
        .done();

    Bounds boundaryEventBounds = findBpmnShape("boundary").getBounds();
    assertShapeCoordinates(boundaryEventBounds, 218, 140);

  }

  @Test
  void shouldPlaceFollowingFlowNodeProperlyForTask() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .userTask(USER_TASK_ID)
        .boundaryEvent("boundary")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(USER_TASK_ID)
        .endEvent()
        .done();

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 266.5, 208);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 236, 176);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 266.5, 226);
  }

  @Test
  void shouldPlaceTwoBoundaryEventsForTask() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .userTask(USER_TASK_ID)
        .boundaryEvent("boundary1")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(USER_TASK_ID)
        .endEvent()
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary2")
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 218, 140);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 254, 140);

  }

  @Test
  void shouldPlaceThreeBoundaryEventsForTask() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .userTask(USER_TASK_ID)
        .boundaryEvent("boundary1")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(USER_TASK_ID)
        .endEvent()
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary2")
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary3")
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 218, 140);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 254, 140);

    Bounds boundaryEvent3Bounds = findBpmnShape("boundary3").getBounds();
    assertShapeCoordinates(boundaryEvent3Bounds, 182, 140);

  }

  @Test
  void shouldPlaceManyBoundaryEventsForTask() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .userTask(USER_TASK_ID)
        .boundaryEvent("boundary1")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(USER_TASK_ID)
        .endEvent()
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary2")
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary3")
        .moveToActivity(USER_TASK_ID)
        .boundaryEvent("boundary4")
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 218, 140);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 254, 140);

    Bounds boundaryEvent3Bounds = findBpmnShape("boundary3").getBounds();
    assertShapeCoordinates(boundaryEvent3Bounds, 182, 140);

    Bounds boundaryEvent4Bounds = findBpmnShape("boundary4").getBounds();
    assertShapeCoordinates(boundaryEvent4Bounds, 218, 140);

  }

  @Test
  void shouldPlaceBoundaryEventForSubProcess() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
        .boundaryEvent("boundary")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(SUB_PROCESS_ID)
        .endEvent()
        .done();

    Bounds boundaryEventBounds = findBpmnShape("boundary").getBounds();
    assertShapeCoordinates(boundaryEventBounds, 343, 200);

  }

  @Test
  void shouldPlaceFollowingFlowNodeForSubProcess() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
        .boundaryEvent("boundary")
        .sequenceFlowId(SEQUENCE_FLOW_ID)
        .endEvent(END_EVENT_ID)
        .moveToActivity(SUB_PROCESS_ID)
        .endEvent()
        .done();

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 391.5, 268);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge(SEQUENCE_FLOW_ID).getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 361, 236);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 391.5, 286);
  }

  @Test
  void shouldPlaceTwoBoundaryEventsForSubProcess() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
        .boundaryEvent("boundary1")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary2")
        .moveToActivity(SUB_PROCESS_ID)
        .endEvent()
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 343, 200);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 379, 200);

  }

  @Test
  void shouldPlaceThreeBoundaryEventsForSubProcess() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
        .boundaryEvent("boundary1")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary2")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary3")
        .moveToActivity(SUB_PROCESS_ID)
        .endEvent()
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 343, 200);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 379, 200);

    Bounds boundaryEvent3Bounds = findBpmnShape("boundary3").getBounds();
    assertShapeCoordinates(boundaryEvent3Bounds, 307, 200);
  }

  @Test
  void shouldPlaceManyBoundaryEventsForSubProcess() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
        .boundaryEvent("boundary1")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary2")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary3")
        .moveToActivity(SUB_PROCESS_ID)
        .boundaryEvent("boundary4")
        .moveToActivity(SUB_PROCESS_ID)
        .endEvent()
        .done();

    Bounds boundaryEvent1Bounds = findBpmnShape("boundary1").getBounds();
    assertShapeCoordinates(boundaryEvent1Bounds, 343, 200);

    Bounds boundaryEvent2Bounds = findBpmnShape("boundary2").getBounds();
    assertShapeCoordinates(boundaryEvent2Bounds, 379, 200);

    Bounds boundaryEvent3Bounds = findBpmnShape("boundary3").getBounds();
    assertShapeCoordinates(boundaryEvent3Bounds, 307, 200);

    Bounds boundaryEvent4Bounds = findBpmnShape("boundary4").getBounds();
    assertShapeCoordinates(boundaryEvent4Bounds, 343, 200);
  }

  @Test
  void shouldPlaceTwoBranchesForParallelGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .parallelGateway("id")
        .sequenceFlowId("s1")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s2")
        .endEvent(END_EVENT_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s2").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 226);
  }

  @Test
  void shouldPlaceThreeBranchesForParallelGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .parallelGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .serviceTask(SERVICE_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 334);
  }

  @Test
  void shouldPlaceManyBranchesForParallelGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .parallelGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .serviceTask(SERVICE_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .sendTask(SEND_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Bounds sendTaskBounds = findBpmnShape(SEND_TASK_ID).getBounds();
    assertShapeCoordinates(sendTaskBounds, 286, 424);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 464);
  }

  @Test
  void shouldPlaceTwoBranchesForExclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .exclusiveGateway("id")
        .sequenceFlowId("s1")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s2")
        .endEvent(END_EVENT_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s2").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 226);
  }

  @Test
  void shouldPlaceThreeBranchesForExclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .exclusiveGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .serviceTask(SERVICE_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 334);
  }

  @Test
  void shouldPlaceManyBranchesForExclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .exclusiveGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .serviceTask(SERVICE_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .sendTask(SEND_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Bounds sendTaskBounds = findBpmnShape(SEND_TASK_ID).getBounds();
    assertShapeCoordinates(sendTaskBounds, 286, 424);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 464);
  }

  @Test
  void shouldPlaceTwoBranchesForEventBasedGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .eventBasedGateway()
          .id("id")
        .sequenceFlowId("s1")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s2")
        .endEvent(END_EVENT_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s2").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 226);
  }

  @Test
  void shouldPlaceThreeBranchesForEventBasedGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .eventBasedGateway()
          .id("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .serviceTask(SERVICE_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 334);
  }

  @Test
  void shouldPlaceManyBranchesForEventBasedGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .eventBasedGateway()
          .id("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .serviceTask(SERVICE_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .sendTask(SEND_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Bounds sendTaskBounds = findBpmnShape(SEND_TASK_ID).getBounds();
    assertShapeCoordinates(sendTaskBounds, 286, 424);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 464);
  }

  @Test
  void shouldPlaceTwoBranchesForInclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .inclusiveGateway("id")
        .sequenceFlowId("s1")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s2")
        .endEvent(END_EVENT_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s2").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 226);
  }

  @Test
  void shouldPlaceThreeBranchesForInclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .inclusiveGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .serviceTask(SERVICE_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 334);
  }

  @Test
  void shouldPlaceManyBranchesForInclusiveGateway() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .inclusiveGateway("id")
        .userTask(USER_TASK_ID)
        .moveToNode("id")
        .endEvent(END_EVENT_ID)
        .moveToNode("id")
        .serviceTask(SERVICE_TASK_ID)
        .moveToNode("id")
        .sequenceFlowId("s1")
        .sendTask(SEND_TASK_ID)
        .done();

    Bounds userTaskBounds = findBpmnShape(USER_TASK_ID).getBounds();
    assertShapeCoordinates(userTaskBounds, 286, 78);

    Bounds endEventBounds = findBpmnShape(END_EVENT_ID).getBounds();
    assertShapeCoordinates(endEventBounds, 286, 208);

    Bounds serviceTaskBounds = findBpmnShape(SERVICE_TASK_ID).getBounds();
    assertShapeCoordinates(serviceTaskBounds, 286, 294);

    Bounds sendTaskBounds = findBpmnShape(SEND_TASK_ID).getBounds();
    assertShapeCoordinates(sendTaskBounds, 286, 424);

    Collection<Waypoint> sequenceFlowWaypoints = findBpmnEdge("s1").getWaypoints();
    Iterator<Waypoint> iterator = sequenceFlowWaypoints.iterator();

    Waypoint waypoint = iterator.next();
    assertWaypointCoordinates(waypoint, 211, 143);

    while(iterator.hasNext()){
      waypoint = iterator.next();
    }

    assertWaypointCoordinates(waypoint, 286, 464);
  }

  public void shouldPlaceStartEventWithinSubProcess() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
          .embeddedSubProcess()
          .startEvent("innerStartEvent")
          .done();

    Bounds startEventBounds = findBpmnShape("innerStartEvent").getBounds();
    assertShapeCoordinates(startEventBounds, 236, 100);
  }

  @Test
  void shouldAdjustSubProcessWidth() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
          .embeddedSubProcess()
          .startEvent("innerStartEvent")
          .parallelGateway("innerParallelGateway")
          .userTask("innerUserTask")
          .endEvent("innerEndEvent")
        .subProcessDone()
        .done();

    Bounds subProcessBounds = findBpmnShape(SUB_PROCESS_ID).getBounds();
    assertThat(subProcessBounds.getWidth()).isEqualTo(472);
  }

  @Test
  void shouldAdjustSubProcessWidthWithEmbeddedSubProcess() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
          .embeddedSubProcess()
          .startEvent("innerStartEvent")
          .subProcess("innerSubProcess")
            .embeddedSubProcess()
            .startEvent()
            .userTask()
            .userTask()
            .endEvent()
          .subProcessDone()
          .endEvent("innerEndEvent")
        .subProcessDone()
        .done();

    Bounds subProcessBounds = findBpmnShape(SUB_PROCESS_ID).getBounds();
    assertThat(subProcessBounds.getWidth()).isEqualTo(794);
  }

  @Test
  void shouldAdjustSubProcessHeight() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
          .embeddedSubProcess()
          .startEvent("innerStartEvent")
          .parallelGateway("innerParallelGateway")
          .endEvent("innerEndEvent")
          .moveToNode("innerParallelGateway")
          .userTask("innerUserTask")
        .subProcessDone()
        .done();

    Bounds subProcessBounds = findBpmnShape(SUB_PROCESS_ID).getBounds();
    assertThat(subProcessBounds.getHeight()).isEqualTo(298);
  }

  @Test
  void shouldAdjustSubProcessHeightWithEmbeddedProcess() {

    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent(START_EVENT_ID)
        .subProcess(SUB_PROCESS_ID)
          .embeddedSubProcess()
          .startEvent("innerStartEvent")
          .subProcess()
            .embeddedSubProcess()
              .startEvent()
              .exclusiveGateway("id")
              .userTask()
              .moveToNode("id")
              .endEvent()
          .subProcessDone()
          .endEvent("innerEndEvent")
        .subProcessDone()
        .endEvent()
        .done();

    Bounds subProcessBounds = findBpmnShape(SUB_PROCESS_ID).getBounds();
    assertThat(subProcessBounds.getY()).isEqualTo(-32);
    assertThat(subProcessBounds.getHeight()).isEqualTo(376);
  }

  @Test
  void shouldPlaceCompensation() {
    ProcessBuilder builder = Bpmn.createExecutableProcess();

    instance = builder
        .startEvent()
        .userTask("task")
        .boundaryEvent("boundary")
          .compensateEventDefinition().compensateEventDefinitionDone()
          .compensationStart()
          .userTask("compensate").name("compensate")
          .compensationDone()
        .userTask("task2")
          .boundaryEvent("boundary2")
            .compensateEventDefinition().compensateEventDefinitionDone()
            .compensationStart()
            .userTask("compensate2").name("compensate2")
            .compensationDone()
        .endEvent("theend")
        .done();

    Bounds compensationBounds = findBpmnShape("compensate").getBounds();
    assertShapeCoordinates(compensationBounds, 266.5, 186);
    Bounds compensation2Bounds = findBpmnShape("compensate2").getBounds();
    assertShapeCoordinates(compensation2Bounds, 416.5, 186);
  }

  protected BpmnShape findBpmnShape(String id) {
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);

    Iterator<BpmnShape> iterator = allShapes.iterator();
    while (iterator.hasNext()) {
      BpmnShape shape = iterator.next();
      if (shape.getBpmnElement().getId().equals(id)) {
        return shape;
      }
    }
    return null;
  }

  protected BpmnEdge findBpmnEdge(String sequenceFlowId){
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

  protected void assertShapeCoordinates(Bounds bounds, double x, double y){
    assertThat(bounds.getX()).isEqualTo(x);
    assertThat(bounds.getY()).isEqualTo(y);
  }

  protected void assertWaypointCoordinates(Waypoint waypoint, double x, double y){
    assertThat(x).isEqualTo(waypoint.getX());
    assertThat(y).isEqualTo(waypoint.getY());
  }
}
