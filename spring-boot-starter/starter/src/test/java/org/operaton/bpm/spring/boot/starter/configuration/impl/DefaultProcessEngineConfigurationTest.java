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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import java.util.Optional;

import org.junit.Before;
import org.springframework.test.util.ReflectionTestUtils;

public class DefaultProcessEngineConfigurationTest {

  private final DefaultProcessEngineConfiguration instance = new DefaultProcessEngineConfiguration();
  private final SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
  private final OperatonBpmProperties properties = new OperatonBpmProperties();

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(instance, "operatonBpmProperties", properties);
    initIdGenerator(null);
  }

  @Test
  public void setName_if_not_empty() {
    properties.setProcessEngineName("foo");
    instance.preInit(configuration);
    assertThat(configuration.getProcessEngineName()).isEqualTo("foo");
  }

  @Test
  public void setName_ignore_empty() {
    properties.setProcessEngineName(null);
    instance.preInit(configuration);
    assertThat(configuration.getProcessEngineName()).isEqualTo(ProcessEngines.NAME_DEFAULT);

    properties.setProcessEngineName(" ");
    instance.preInit(configuration);
    assertThat(configuration.getProcessEngineName()).isEqualTo(ProcessEngines.NAME_DEFAULT);
  }

  @Test
  public void setName_ignore_hyphen() {
    properties.setProcessEngineName("foo-bar");
    instance.preInit(configuration);
    assertThat(configuration.getProcessEngineName()).isEqualTo(ProcessEngines.NAME_DEFAULT);
  }

  @Test
  public void setDefaultSerializationFormat() {
    final String defaultSerializationFormat = "testformat";
    properties.setDefaultSerializationFormat(defaultSerializationFormat);
    instance.preInit(configuration);
    assertThat(configuration.getDefaultSerializationFormat()).isSameAs(defaultSerializationFormat);
  }

  @Test
  public void setDefaultSerializationFormat_ignore_null() {
    final String defaultSerializationFormat = configuration.getDefaultSerializationFormat();
    properties.setDefaultSerializationFormat(null);
    instance.preInit(configuration);
    assertThat(configuration.getDefaultSerializationFormat()).isEqualTo(defaultSerializationFormat);
  }

  @Test
  public void setDefaultSerializationFormat_ignore_empty() {
    final String defaultSerializationFormat = configuration.getDefaultSerializationFormat();
    properties.setDefaultSerializationFormat(" ");
    instance.preInit(configuration);
    assertThat(configuration.getDefaultSerializationFormat()).isEqualTo(defaultSerializationFormat);
  }

  @Test
  public void setJobExecutorAcquireByPriority() {
    properties.setJobExecutorAcquireByPriority(null);
    instance.preInit(configuration);
    assertThat(configuration.isJobExecutorAcquireByPriority()).isFalse();

    properties.setJobExecutorAcquireByPriority(true);
    instance.preInit(configuration);
    assertThat(configuration.isJobExecutorAcquireByPriority()).isTrue();
  }

  @Test
  public void setDefaultNumberOfRetries() {
    properties.setDefaultNumberOfRetries(null);
    instance.preInit(configuration);
    assertThat(configuration.getDefaultNumberOfRetries()).isEqualTo(3);

    properties.setDefaultNumberOfRetries(1);
    instance.preInit(configuration);
    assertThat(configuration.getDefaultNumberOfRetries()).isEqualTo(1);
  }

  private void initIdGenerator(IdGenerator idGenerator) {
    ReflectionTestUtils.setField(instance, "idGenerator", Optional.ofNullable(idGenerator));
  }
}
