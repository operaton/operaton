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

import org.operaton.bpm.engine.cdi.BusinessProcessEvent;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(ArquillianExtension.class)
class EventNotificationTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml"})
  void receiveAll() {
    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    // assert that the bean has received 0 events
    assertThat(listenerBean.getEventsReceived().size()).isEqualTo(0);
    runtimeService.startProcessInstanceByKey("process1");

    // complete user task
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    assertThat(listenerBean.getEventsReceived().size()).isEqualTo(16);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml",
      "org/operaton/bpm/engine/cdi/test/impl/event/EventNotificationTest.process2.bpmn20.xml"})
  void selectEventsPerProcessDefinition() {
    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    assertThat(listenerBean.getEventsReceivedByKey().size()).isEqualTo(0);
    //start the 2 processes
    runtimeService.startProcessInstanceByKey("process1");
    runtimeService.startProcessInstanceByKey("process2");

    // assert that now the bean has received 11 events
    assertThat(listenerBean.getEventsReceivedByKey().size()).isEqualTo(11);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml"})
  void selectEventsPerActivity() {
    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    assertThat(listenerBean.getEndActivityService1()).isEqualTo(0);
    assertThat(listenerBean.getStartActivityService1()).isEqualTo(0);
    assertThat(listenerBean.getTakeTransition1()).isEqualTo(0);

    // start the process
    runtimeService.startProcessInstanceByKey("process1");

    // assert
    assertThat(listenerBean.getEndActivityService1()).isEqualTo(1);
    assertThat(listenerBean.getStartActivityService1()).isEqualTo(1);
    assertThat(listenerBean.getTakeTransition1()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml"})
  void selectEventsPerTask() {
    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    assertThat(listenerBean.getCreateTaskUser1()).isEqualTo(0);
    assertThat(listenerBean.getAssignTaskUser1()).isEqualTo(0);
    assertThat(listenerBean.getCompleteTaskUser1()).isEqualTo(0);
    assertThat(listenerBean.getDeleteTaskUser1()).isEqualTo(0);

    // assert that the bean has received 0 events
    assertThat(listenerBean.getEventsReceived().size()).isEqualTo(0);
    runtimeService.startProcessInstanceByKey("process1");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(), "demo");

    taskService.complete(task.getId());

    assertThat(listenerBean.getCreateTaskUser1()).isEqualTo(1);
    assertThat(listenerBean.getAssignTaskUser1()).isEqualTo(1);
    assertThat(listenerBean.getCompleteTaskUser1()).isEqualTo(1);
    assertThat(listenerBean.getDeleteTaskUser1()).isEqualTo(0);

    listenerBean.reset();
    assertThat(listenerBean.getDeleteTaskUser1()).isEqualTo(0);

    // assert that the bean has received 0 events
    assertThat(listenerBean.getEventsReceived().size()).isEqualTo(0);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1");

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    assertThat(listenerBean.getDeleteTaskUser1()).isEqualTo(1);
  }

  @Test
  @Deployment
  void multiInstanceEvents(){
    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    assertThat(listenerBean.getEventsReceived().size(), is(0));
    runtimeService.startProcessInstanceByKey("process1");
    waitForJobExecutorToProcessAllJobs(TimeUnit.SECONDS.toMillis(5L), 500L);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName(), is("User Task"));

    // 2: start event (start + end)
    // 1: transition to first mi activity
    // 2: first mi body (start + end)
    // 4: two instances of the inner activity (start + end)
    // 1: transition to second mi activity
    // 2: second mi body (start + end)
    // 4: two instances of the inner activity (start + end)
    // 1: transition to the user task
    // 2: user task (start + task create event)
    // = 19
    assertThat(listenerBean.getEventsReceived().size(), is(19));
  }

  @Test
  @Deployment
  void multiInstanceEventsAfterExternalTrigger() {

    runtimeService.startProcessInstanceByKey("process");

    TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
    listenerBean.reset();

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks.size()).isEqualTo(3);

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    // 6: three user task instances (complete + end)
    // 1: one mi body instance (end)
    // 1: one sequence flow instance (take)
    // 2: one end event instance (start + end)
    // = 5
    Set<BusinessProcessEvent> eventsReceived = listenerBean.getEventsReceived();
    assertThat(eventsReceived.size(), is(10));
  }

}
