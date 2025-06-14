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
package org.operaton.bpm.client.spring;

import org.operaton.bpm.client.spring.configuration.FullConfiguration;
import org.operaton.bpm.client.task.ExternalTaskHandler;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {FullConfiguration.class})
@DirtiesContext // context cannot be reused since the mocks need to be reinitialized completely
class ConfigurationTest extends MockedTest {

  @Autowired
  @Qualifier("handler")
  protected ExternalTaskHandler handler;

  @BeforeEach
  void init() {
    when(clientBuilder.orderByCreateTime()).thenReturn(clientBuilder);
    when(clientBuilder.asc()).thenReturn(clientBuilder);
  }

  @Test
  void shouldVerifyClientConfiguration() {
    verify(clientBuilder).baseUrl("url");
    verify(clientBuilder).maxTasks(1111);
    verify(clientBuilder).workerId("worker-id");
    verify(clientBuilder).usePriority(false);
    verify(clientBuilder).asyncResponseTimeout(5555);
    verify(clientBuilder).disableAutoFetching();
    verify(clientBuilder).disableBackoffStrategy();
    verify(clientBuilder).lockDuration(4444);
    verify(clientBuilder).dateFormat("date-format");
    verify(clientBuilder).defaultSerializationFormat("default-serialization-format");
    verify(clientBuilder).orderByCreateTime();
    verify(clientBuilder).asc();
    verify(clientBuilder).build();

    verifyNoMoreInteractions(clientBuilder);
  }

  @Test
  void shouldVerifySubscriptionConfiguration() {
    verify(client).subscribe("topic-name");
    verify(subscriptionBuilder).handler(handler);
    verify(subscriptionBuilder).variables("variable-one", "variable-two");
    verify(subscriptionBuilder).lockDuration(1111);
    verify(subscriptionBuilder).localVariables(true);
    verify(subscriptionBuilder).businessKey("business-key");
    verify(subscriptionBuilder).processDefinitionId("process-definition-id");
    verify(subscriptionBuilder).processDefinitionIdIn("id-one", "id-two");
    verify(subscriptionBuilder).processDefinitionKey("key");
    verify(subscriptionBuilder).processDefinitionKeyIn("key-one", "key-two");
    verify(subscriptionBuilder).processDefinitionVersionTag("version-tag");

    ArgumentCaptor<Map<String, Object>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(subscriptionBuilder).processVariablesEqualsIn(argumentCaptor.capture());
    Map<String, String> variables = new HashMap<>();
    variables.put("var-name-foo", "var-val-foo");
    variables.put("var-name-bar", "var-val-bar");
    assertThat(argumentCaptor.getValue()).containsAllEntriesOf(variables);

    verify(subscriptionBuilder).withoutTenantId();
    verify(subscriptionBuilder).tenantIdIn("tenant-id-one", "tenant-id-two");
    verify(subscriptionBuilder).includeExtensionProperties(true);
    verify(subscriptionBuilder).open();
    verifyNoMoreInteractions(subscriptionBuilder);
  }

}
