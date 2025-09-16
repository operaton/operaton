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
package org.operaton.bpm.integrationtest.functional.condition;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.integrationtest.functional.condition.bean.ConditionalBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class ConditionalStartEventTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeployment() {
    return initWebArchiveDeployment()
      .addClass(ConditionalBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/condition/ConditionalStartEventTest.bpmn20.xml");
  }

  @Test
  void testStartInstanceWithBeanCondition() {
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

    assertThat(eventSubscriptions.size()).isEqualTo(1);
    assertThat(eventSubscriptions.get(0).getEventType()).isEqualTo(EventType.CONDITONAL.name());

    List<ProcessInstance> instances = runtimeService
        .createConditionEvaluation()
        .setVariable("foo", 1)
        .evaluateStartConditions();

    assertThat(instances.size()).isEqualTo(1);

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("conditionalEventProcess").singleResult()).isNotNull();

    VariableInstance vars = runtimeService.createVariableInstanceQuery().singleResult();
    assertThat(instances.get(0).getId()).isEqualTo(vars.getProcessInstanceId());
    assertThat(vars.getValue()).isEqualTo(1);
  }
}
