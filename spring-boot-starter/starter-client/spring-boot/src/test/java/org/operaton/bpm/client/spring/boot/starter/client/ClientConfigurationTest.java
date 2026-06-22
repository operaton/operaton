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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.operaton.bpm.client.spring.boot.starter.ParsePropertiesHelper;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
  "operaton.bpm.client.base-url=base-url",
  "operaton.bpm.client.worker-id=worker-id",
  "operaton.bpm.client.max-tasks=111",
  "operaton.bpm.client.order-by-create-time=asc",
  "operaton.bpm.client.use-priority=false",
  "operaton.bpm.client.use-create-time=true",
  "operaton.bpm.client.default-serialization-format=serialization-format",
  "operaton.bpm.client.date-format=date-format",
  "operaton.bpm.client.async-response-timeout=555",
  "operaton.bpm.client.lock-duration=777",
  "operaton.bpm.client.disable-auto-fetching=true",
  "operaton.bpm.client.disable-backoff-strategy=true",
  "operaton.bpm.client.basic-auth.username=username",
  "operaton.bpm.client.basic-auth.password=password",
})
@ExtendWith(SpringExtension.class)
class ClientConfigurationTest extends ParsePropertiesHelper {

  @Test
  void shouldCheckProperties() {
    assertThat(properties.getBaseUrl()).isEqualTo("base-url");
    assertThat(properties.getWorkerId()).isEqualTo("worker-id");
    assertThat(properties.getMaxTasks()).isEqualTo(111);
    assertThat(properties.getOrderByCreateTime()).isEqualTo("asc");
    assertThat(properties.getUseCreateTime()).isTrue();
    assertThat(properties.getUsePriority()).isFalse();
    assertThat(properties.getDefaultSerializationFormat()).isEqualTo("serialization-format");
    assertThat(properties.getDateFormat()).isEqualTo("date-format");
    assertThat(properties.getAsyncResponseTimeout()).isEqualTo(555);
    assertThat(properties.getLockDuration()).isEqualTo(777);
    assertThat(properties.getDisableAutoFetching()).isTrue();
    assertThat(properties.getDisableBackoffStrategy()).isTrue();
    assertThat(basicAuth.getUsername()).isEqualTo("username");
    assertThat(basicAuth.getPassword()).isEqualTo("password");
    assertThat(subscriptions).isEmpty();
  }

}
