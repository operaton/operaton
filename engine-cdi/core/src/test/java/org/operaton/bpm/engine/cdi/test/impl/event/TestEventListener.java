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
package org.operaton.bpm.engine.cdi.test.impl.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.cdi.BusinessProcessEvent;
import org.operaton.bpm.engine.cdi.annotation.event.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class TestEventListener {

  public void reset() {
    startActivityService1 = 0;
    endActivityService1 = 0;
    takeTransition1 = 0;
    createTaskUser1 = 0;
    assignTaskUser1 = 0;
    completeTaskUser1 = 0;
    deleteTaskUser1 = 0;

    eventsReceivedByKey.clear();
    eventsReceived.clear();
  }

  private final Set<BusinessProcessEvent> eventsReceivedByKey = new HashSet<>();

  // receives all events related to "process1"
  public void onProcessEventByKey(@Observes @BusinessProcessDefinition("process1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getProcessDefinition().getKey()).isEqualTo("process1");
    eventsReceivedByKey.add(businessProcessEvent);
  }

  public Set<BusinessProcessEvent> getEventsReceivedByKey() {
    return eventsReceivedByKey;
  }


  // ---------------------------------------------------------

  private final Set<BusinessProcessEvent> eventsReceived = new HashSet<>();

  // receives all events
  public void onProcessEvent(@Observes BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    eventsReceived.add(businessProcessEvent);
  }

  public Set<BusinessProcessEvent> getEventsReceived() {
    return eventsReceived;
  }

  // ---------------------------------------------------------

  private int startActivityService1 = 0;
  private int endActivityService1 = 0;
  private int takeTransition1 = 0;

  public void onStartActivityService1(@Observes @StartActivity("service1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent.getActivityId()).isEqualTo("service1");
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNull();
    assertThat(businessProcessEvent.getTaskId()).isNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isNull();
    startActivityService1 += 1;
  }

  public void onEndActivityService1(@Observes @EndActivity("service1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent.getActivityId()).isEqualTo("service1");
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNull();
    assertThat(businessProcessEvent.getTaskId()).isNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isNull();
    endActivityService1 += 1;
  }

  public void takeTransition1(@Observes @TakeTransition("t1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent.getTransitionName()).isEqualTo("t1");
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNull();
    assertThat(businessProcessEvent.getTaskId()).isNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isNull();
    takeTransition1 += 1;
  }

  public int getEndActivityService1() {
    return endActivityService1;
  }

  public int getStartActivityService1() {
    return startActivityService1;
  }

  public int getTakeTransition1() {
    return takeTransition1;
  }


  // ---------------------------------------------------------

  private int createTaskUser1 = 0;
  private int assignTaskUser1 = 0;
  private int completeTaskUser1 = 0;
  private int deleteTaskUser1 = 0;

  public void onCreateTask(@Observes @CreateTask("user1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNotNull();
    assertThat(businessProcessEvent.getTaskId()).isNotNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isEqualTo("user1");
    createTaskUser1++;
  }

  public void onAssignTask(@Observes @AssignTask("user1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNotNull();
    assertThat(businessProcessEvent.getTaskId()).isNotNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isEqualTo("user1");
    assignTaskUser1++;
  }

  public void onCompleteTask(@Observes @CompleteTask("user1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNotNull();
    assertThat(businessProcessEvent.getTaskId()).isNotNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isEqualTo("user1");
    completeTaskUser1++;
  }

  public void onDeleteTask(@Observes @DeleteTask("user1") BusinessProcessEvent businessProcessEvent) {
    assertThat(businessProcessEvent).isNotNull();
    assertThat(businessProcessEvent.getTask()).isNotNull();
    assertThat(businessProcessEvent.getTaskId()).isNotNull();
    assertThat(businessProcessEvent.getTaskDefinitionKey()).isEqualTo("user1");
    deleteTaskUser1++;
  }

  public int getCreateTaskUser1() {
    return createTaskUser1;
  }

  public int getAssignTaskUser1() {
    return assignTaskUser1;
  }

  public int getCompleteTaskUser1() {
    return completeTaskUser1;
  }

  public int getDeleteTaskUser1() {
    return deleteTaskUser1;
  }

}
