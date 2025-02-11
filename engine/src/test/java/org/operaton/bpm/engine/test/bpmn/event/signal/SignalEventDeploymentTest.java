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
package org.operaton.bpm.engine.test.bpmn.event.signal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Philipp Ossler
 */
public class SignalEventDeploymentTest extends PluggableProcessEngineTest {

  private static final String SIGNAL_START_EVENT_PROCESS = "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml";
  private static final String SIGNAL_START_EVENT_PROCESS_NEW_VERSION = "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent_v2.bpmn20.xml";

  @Test
  public void testCreateEventSubscriptionOnDeployment() {
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource(SIGNAL_START_EVENT_PROCESS));

    EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertNotNull(eventSubscription);

    assertThat(eventSubscription.getEventType()).isEqualTo(EventType.SIGNAL.name());
    assertThat(eventSubscription.getEventName()).isEqualTo("alert");
    assertThat(eventSubscription.getActivityId()).isEqualTo("start");
  }

  @Test
  public void testUpdateEventSubscriptionOnDeployment(){
    testRule.deploy(repositoryService.createDeployment()
        .addClasspathResource(SIGNAL_START_EVENT_PROCESS));

    EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().eventType("signal").singleResult();
    assertNotNull(eventSubscription);
    assertThat(eventSubscription.getEventName()).isEqualTo("alert");

    // deploy a new version of the process with different signal name
    String newDeploymentId = repositoryService.createDeployment()
        .addClasspathResource(SIGNAL_START_EVENT_PROCESS_NEW_VERSION)
        .deploy().getId();

    ProcessDefinition newProcessDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().singleResult();
    assertThat(newProcessDefinition.getVersion()).isEqualTo(2);

    List<EventSubscription> newEventSubscriptions = runtimeService.createEventSubscriptionQuery().eventType("signal").list();
    // only one event subscription for the new version of the process definition
    assertThat(newEventSubscriptions.size()).isEqualTo(1);

    EventSubscriptionEntity newEventSubscription = (EventSubscriptionEntity) newEventSubscriptions.iterator().next();
    assertThat(newEventSubscription.getConfiguration()).isEqualTo(newProcessDefinition.getId());
    assertThat(newEventSubscription.getEventName()).isEqualTo("abort");

    // clean db
    repositoryService.deleteDeployment(newDeploymentId);
  }

  @Test
  public void testAsyncSignalStartEventDeleteDeploymentWhileAsync() {
    // given a deployment
    org.operaton.bpm.engine.repository.Deployment deployment =
        repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTest.signalStartEvent.bpmn20.xml")
          .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventTests.throwAlertSignalAsync.bpmn20.xml")
          .deploy();

    // and an active job for asynchronously triggering a signal start event
    runtimeService.startProcessInstanceByKey("throwSignalAsync");

    // then deleting the deployment succeeds
    repositoryService.deleteDeployment(deployment.getId(), true);

    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(0);

    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      // and there are no job logs left
      assertThat(historyService.createHistoricJobLogQuery().count()).isEqualTo(0);
    }

  }

}
