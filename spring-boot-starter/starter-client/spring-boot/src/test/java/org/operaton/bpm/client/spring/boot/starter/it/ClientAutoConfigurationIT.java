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
package org.operaton.bpm.client.spring.boot.starter.it;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.spring.SpringTopicSubscription;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class ClientAutoConfigurationIT {

  @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
  @SuppressWarnings("unused")
  ExternalTaskClient externalTaskClient;

  @Autowired
  List<SpringTopicSubscription> topicSubscriptions;

  @Test
  void startup() {
    assertThat(topicSubscriptions).hasSize(2);
    assertThat(topicSubscriptions)
        .extracting("topicName", "autoOpen", "businessKey", "lockDuration", "processDefinitionKey")
        .containsExactlyInAnyOrder(
            tuple("topic-one", false, null, 33L, null),
            tuple("topic-two", false, "business-key", null, "proc-def-key"));
  }

}
