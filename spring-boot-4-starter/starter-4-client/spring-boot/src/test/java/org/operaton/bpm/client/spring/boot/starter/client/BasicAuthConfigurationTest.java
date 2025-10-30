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
package org.operaton.bpm.client.spring.boot.starter.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.operaton.bpm.client.ExternalTaskClientBuilder;
import org.operaton.bpm.client.interceptor.ClientRequestInterceptor;
import org.operaton.bpm.client.spring.boot.starter.MockHelper;
import org.operaton.bpm.client.spring.boot.starter.ParsePropertiesHelper;
import org.operaton.bpm.client.spring.boot.starter.client.configuration.SimpleSubscriptionConfiguration;
import org.operaton.bpm.client.spring.boot.starter.impl.ClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
  "operaton.bpm.client.basic-auth.username=my-username",
  "operaton.bpm.client.basic-auth.password=my-password",
})
@ContextConfiguration(classes = {
  ParsePropertiesHelper.TestConfig.class,
  ClientAutoConfiguration.class,
  SimpleSubscriptionConfiguration.class
})
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
class BasicAuthConfigurationTest extends ParsePropertiesHelper {

  protected static ExternalTaskClientBuilder clientBuilder;

  @BeforeAll
  static void initMocks() {
    MockHelper.initMocks();
    clientBuilder = MockHelper.getClientBuilder();
  }

  @AfterAll
  static void reset() {
    MockHelper.reset();
  }

  @Test
  void shouldVerifyBasicAuthCredentials() {
    ArgumentCaptor<ClientRequestInterceptor> argumentCaptor =
        ArgumentCaptor.forClass(ClientRequestInterceptor.class);
    verify(clientBuilder).addInterceptor(argumentCaptor.capture());

    assertThat(argumentCaptor.getValue())
        .extracting("username", "password")
        .containsExactlyInAnyOrder("my-username", "my-password");
  }

}
