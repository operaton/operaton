/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.spring.boot.starter.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.client.spring.boot.starter.ClientProperties;
import org.operaton.bpm.client.spring.impl.client.ClientConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PropertiesAwareClientFactoryTest {

  @Test
  void shouldApplyProperties() {
    // given
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.setAsyncResponseTimeout(1L);
    clientProperties.setBaseUrl("base-url");
    clientProperties.setDateFormat("date-format");
    clientProperties.setDefaultSerializationFormat("default-serialization-format");
    clientProperties.setDisableAutoFetching(Boolean.TRUE);
    clientProperties.setDisableBackoffStrategy(Boolean.TRUE);
    clientProperties.setLockDuration(2L);
    clientProperties.setMaxTasks(3);
    clientProperties.setOrderByCreateTime("order-by-create-time");
    clientProperties.setUseCreateTime(Boolean.TRUE);
    clientProperties.setUsePriority(Boolean.TRUE);
    clientProperties.setWorkerId("worker-id");

    PropertiesAwareClientFactory factory = new PropertiesAwareClientFactory(clientProperties);

    // when
    factory.applyPropertiesFrom(clientProperties);

    // then
    ClientConfiguration clientConfiguration = factory.getClientConfiguration();
    assertThat(clientConfiguration.getAsyncResponseTimeout()).isEqualTo(clientProperties.getAsyncResponseTimeout());
    assertThat(clientConfiguration.getBaseUrl()).isEqualTo(clientProperties.getBaseUrl());
    assertThat(clientConfiguration.getDateFormat()).isEqualTo(clientProperties.getDateFormat());
    assertThat(clientConfiguration.getDefaultSerializationFormat()).isEqualTo(clientProperties.getDefaultSerializationFormat());
    assertThat(clientConfiguration.getDisableAutoFetching()).isEqualTo(clientProperties.getDisableAutoFetching());
    assertThat(clientConfiguration.getDisableBackoffStrategy()).isEqualTo(clientProperties.getDisableBackoffStrategy());
    assertThat(clientConfiguration.getLockDuration()).isEqualTo(clientProperties.getLockDuration());
    assertThat(clientConfiguration.getMaxTasks()).isEqualTo(clientProperties.getMaxTasks());
    assertThat(clientConfiguration.getOrderByCreateTime()).isEqualTo(clientProperties.getOrderByCreateTime());
    assertThat(clientConfiguration.getUseCreateTime()).isEqualTo(clientProperties.getUseCreateTime());
    assertThat(clientConfiguration.getUsePriority()).isNull();
    assertThat(clientConfiguration.getWorkerId()).isEqualTo(clientProperties.getWorkerId());
  }
}
