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
package org.operaton.bpm.client.spring;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.ExternalTaskClientBuilder;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.topic.TopicSubscriptionBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public abstract class MockedTest {

  protected static ExternalTaskClient client;
  protected static ExternalTaskClientBuilder clientBuilder;
  protected static TopicSubscriptionBuilder subscriptionBuilder;

  protected static MockedStatic<ExternalTaskClient> mockedStatic;
  

  @BeforeAll
  public static void mockClient() {
    mockedStatic = mockStatic(ExternalTaskClient.class);
    clientBuilder = mock(ExternalTaskClientBuilder.class, RETURNS_SELF);
    mockedStatic.when(ExternalTaskClient::create).thenReturn(clientBuilder);
    client = mock(ExternalTaskClient.class);
    mockedStatic.when(() -> clientBuilder.build()).thenReturn(client);
    subscriptionBuilder = mock(TopicSubscriptionBuilder.class, RETURNS_SELF);
    mockedStatic.when(() -> client.subscribe(anyString())).thenReturn(subscriptionBuilder);
    TopicSubscription topicSubscription = mock(TopicSubscription.class);
    when(subscriptionBuilder.open()).thenReturn(topicSubscription);
  }

  @AfterAll
  public static void close() {
    mockedStatic.close();
  }

}
