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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.client.spring.boot.starter.ClientProperties;
import org.operaton.bpm.client.spring.exception.SpringExternalTaskClientException;
import org.operaton.bpm.client.spring.impl.client.ClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
  classes = {
    PropertiesAwareClientFactory.class,
    ClientProperties.class
  })
class PropertiesAwareClientFactoryTest {
  @Autowired
  PropertiesAwareClientFactory clientFactory;

  @Autowired
  ClientProperties clientProperties;

  @BeforeEach
  void setUp() {
    clientProperties.setUseCreateTime(null);
    clientProperties.setOrderByCreateTime(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"useCreateTime", "orderByCreateTime"})
  // useCreateTime and orderByCreateTime are mutually exclusive, so we test them separately
  void shouldApplyProperties(String creationTimeProperty) {
    // given
    clientProperties.setAsyncResponseTimeout(1L);
    clientProperties.setBaseUrl("base-url");
    clientProperties.setDateFormat("date-format");
    clientProperties.setDefaultSerializationFormat("default-serialization-format");
    clientProperties.setDisableAutoFetching(Boolean.TRUE);
    clientProperties.setDisableBackoffStrategy(Boolean.TRUE);
    clientProperties.setLockDuration(2L);
    clientProperties.setMaxTasks(3);
    clientProperties.setUsePriority(Boolean.TRUE);
    clientProperties.setWorkerId("worker-id");
    if ("useCreateTime".equals(creationTimeProperty)) {
      clientProperties.setUseCreateTime(Boolean.TRUE);
    } else if ("orderByCreateTime".equals(creationTimeProperty)) {
      clientProperties.setOrderByCreateTime("order-by-create-time");
    }

    // when
    clientFactory.applyPropertiesFrom(clientProperties);

    // then
    ClientConfiguration clientConfiguration = clientFactory.getClientConfiguration();
    assertThat(clientConfiguration.getAsyncResponseTimeout()).isEqualTo(clientProperties.getAsyncResponseTimeout());
    assertThat(clientConfiguration.getBaseUrl()).isEqualTo(clientProperties.getBaseUrl());
    assertThat(clientConfiguration.getDateFormat()).isEqualTo(clientProperties.getDateFormat());
    assertThat(clientConfiguration.getDefaultSerializationFormat()).isEqualTo(clientProperties.getDefaultSerializationFormat());
    assertThat(clientConfiguration.getDisableAutoFetching()).isEqualTo(clientProperties.getDisableAutoFetching());
    assertThat(clientConfiguration.getDisableBackoffStrategy()).isEqualTo(clientProperties.getDisableBackoffStrategy());
    assertThat(clientConfiguration.getLockDuration()).isEqualTo(clientProperties.getLockDuration());
    assertThat(clientConfiguration.getMaxTasks()).isEqualTo(clientProperties.getMaxTasks());
    assertThat(clientConfiguration.getUsePriority()).isNull();
    assertThat(clientConfiguration.getWorkerId()).isEqualTo(clientProperties.getWorkerId());
    if ("useCreateTime".equals(creationTimeProperty)) {
      assertThat(clientConfiguration.getUseCreateTime()).isEqualTo(clientProperties.getUseCreateTime());
    } else if ("orderByCreateTime".equals(creationTimeProperty)) {
      assertThat(clientConfiguration.getOrderByCreateTime()).isEqualTo(clientProperties.getOrderByCreateTime());
    }
  }

  @Test
  void applyPropertiesFrom_shouldThrow_whenUseCreateTimeAndOrderByCreateTimeAreBothSet() {
    // given
    clientProperties.setUseCreateTime(Boolean.TRUE);
    clientProperties.setOrderByCreateTime("order-by-create-time");

    // when / then
    assertThatThrownBy(() -> clientFactory.applyPropertiesFrom(clientProperties))
      .isInstanceOf(SpringExternalTaskClientException.class)
      .hasMessageContainingAll("useCreateTime", "orderByCreateTime");
  }
}
