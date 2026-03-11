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
package org.operaton.bpm.integrationtest.functional.message;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class IntermediateMessageCatchEventTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeployment() {
    return initWebArchiveDeployment()
      .addAsResource("org/operaton/bpm/integrationtest/functional/message/IntermediateMessageCatchEventTest.bpmn20.xml");
  }

  @Test
  void test() {
    runtimeService.startProcessInstanceByKey("testProcess");

    long eventSubscriptionCount = runtimeService.createEventSubscriptionQuery().count();
    assertThat(eventSubscriptionCount).isOne();

    Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("Test Message").singleResult();

    assertThat(execution).isNotNull();

    runtimeService.createMessageCorrelation("Test Message").correlate();

    eventSubscriptionCount = runtimeService.createEventSubscriptionQuery().count();
    assertThat(eventSubscriptionCount).isZero();

    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

}
