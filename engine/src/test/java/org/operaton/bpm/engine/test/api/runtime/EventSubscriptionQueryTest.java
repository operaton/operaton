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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 */
class EventSubscriptionQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @Test
  void testQueryByEventSubscriptionId() {
    createExampleEventSubscriptions();

    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery()
        .eventName("messageName2")
        .list();
    assertThat(list).hasSize(1);

    EventSubscription eventSubscription = list.get(0);

    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery()
        .eventSubscriptionId(eventSubscription.getId());

    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().eventSubscriptionId(null);

    assertThatThrownBy(eventSubscriptionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("event subscription id is null");

    cleanDb();
  }

  @Test
  void testQueryByEventName() {

    createExampleEventSubscriptions();

    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery()
      .eventName("messageName")
      .list();
    assertThat(list).hasSize(2);

    list = runtimeService.createEventSubscriptionQuery()
      .eventName("messageName2")
      .list();
    assertThat(list).hasSize(1);
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().eventName(null);

    assertThatThrownBy(eventSubscriptionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("event name is null");

    cleanDb();

  }

  @Test
  void testQueryByEventType() {

    createExampleEventSubscriptions();

    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery()
      .eventType("signal")
      .list();
    assertThat(list).hasSize(1);

    list = runtimeService.createEventSubscriptionQuery()
      .eventType("message")
      .list();
    assertThat(list).hasSize(2);
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().eventType(null);

    assertThatThrownBy(eventSubscriptionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("event type is null");

    cleanDb();

  }

  @Test
  void testQueryByActivityId() {

    createExampleEventSubscriptions();

    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery()
      .activityId("someOtherActivity")
      .list();
    assertThat(list).hasSize(1);

    list = runtimeService.createEventSubscriptionQuery()
      .activityId("someActivity")
      .eventType("message")
      .list();
    assertThat(list).hasSize(2);
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().activityId(null);

    assertThatThrownBy(eventSubscriptionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("activity id is null");

    cleanDb();

  }

  @Deployment
  @Test
  void testQueryByExecutionId() {

    // starting two instances:
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");
    runtimeService.startProcessInstanceByKey("catchSignal");

    // test query by process instance id
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();
    assertThat(subscription).isNotNull();

    Execution executionWaitingForSignal = runtimeService.createExecutionQuery()
      .activityId("signalEvent")
      .processInstanceId(processInstance.getId())
      .singleResult();

    // test query by execution id
    EventSubscription signalSubscription = runtimeService.createEventSubscriptionQuery()
      .executionId(executionWaitingForSignal.getId())
      .singleResult();
    assertThat(signalSubscription).isNotNull();

    assertThat(subscription).isEqualTo(signalSubscription);
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().executionId(null);

    assertThatThrownBy(eventSubscriptionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("execution id is null");

    cleanDb();

  }

  @Test
  void testQuerySorting() {
    createExampleEventSubscriptions();
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().orderByCreated().asc().list();
    assertThat(eventSubscriptions).hasSize(3);

    assertThat(eventSubscriptions.get(0).getCreated()).isBefore(eventSubscriptions.get(1).getCreated());
    assertThat(eventSubscriptions.get(1).getCreated()).isBefore(eventSubscriptions.get(2).getCreated());

    cleanDb();
  }

  @Deployment
  @Test
  void testMultipleEventSubscriptions() {
    String message = "cancelation-requested";

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    assertThat(testRule.areJobsAvailable()).isTrue();

    long eventSubscriptionCount = runtimeService.createEventSubscriptionQuery().count();
    assertThat(eventSubscriptionCount).isEqualTo(2);

    EventSubscription messageEvent = runtimeService.createEventSubscriptionQuery().eventType("message").singleResult();
    assertThat(messageEvent.getEventName()).isEqualTo(message);

    EventSubscription compensationEvent = runtimeService.createEventSubscriptionQuery().eventType("compensate").singleResult();
    assertThat(compensationEvent.getEventName()).isNull();

    runtimeService.createMessageCorrelation(message).processInstanceId(processInstance.getId()).correlate();

    testRule.assertProcessEnded(processInstance.getId());
  }


  protected void createExampleEventSubscriptions() {
    processEngineConfiguration.getCommandExecutorTxRequired()
    .execute(commandContext -> {
      Calendar calendar = new GregorianCalendar();


      EventSubscriptionEntity messageEventSubscriptionEntity1 = new EventSubscriptionEntity(EventType.MESSAGE);
      messageEventSubscriptionEntity1.setEventName("messageName");
      messageEventSubscriptionEntity1.setActivityId("someActivity");
      calendar.set(2001, 1, 1);
      messageEventSubscriptionEntity1.setCreated(calendar.getTime());
      messageEventSubscriptionEntity1.insert();

      EventSubscriptionEntity messageEventSubscriptionEntity2 = new EventSubscriptionEntity(EventType.MESSAGE);
      messageEventSubscriptionEntity2.setEventName("messageName");
      messageEventSubscriptionEntity2.setActivityId("someActivity");
      calendar.set(2000, 1, 1);
      messageEventSubscriptionEntity2.setCreated(calendar.getTime());
      messageEventSubscriptionEntity2.insert();

      EventSubscriptionEntity signalEventSubscriptionEntity3 = new EventSubscriptionEntity(EventType.SIGNAL);
      signalEventSubscriptionEntity3.setEventName("messageName2");
      signalEventSubscriptionEntity3.setActivityId("someOtherActivity");
      calendar.set(2002, 1, 1);
      signalEventSubscriptionEntity3.setCreated(calendar.getTime());
      signalEventSubscriptionEntity3.insert();

      return null;
    });
  }

  protected void cleanDb() {
    processEngineConfiguration.getCommandExecutorTxRequired()
    .execute(commandContext -> {
      final List<EventSubscription> subscriptions = new EventSubscriptionQueryImpl().list();
      for (EventSubscription eventSubscriptionEntity : subscriptions) {
        ((EventSubscriptionEntity) eventSubscriptionEntity).delete();
      }
      return null;
    });

  }


}
