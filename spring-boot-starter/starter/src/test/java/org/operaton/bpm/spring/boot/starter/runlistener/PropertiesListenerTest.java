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
package org.operaton.bpm.spring.boot.starter.runlistener;

import org.operaton.bpm.spring.boot.starter.util.OperatonBpmVersion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class PropertiesListenerTest {


  @Mock
  private ConfigurableEnvironment environment;

  @Mock
  private ApplicationEnvironmentPreparedEvent event;

  @Mock
  private MutablePropertySources mutablePropertySources;

  @Captor
  private ArgumentCaptor<PropertiesPropertySource> propertiesPropertySource;

  @BeforeEach
  void setUp() {
    when(event.getEnvironment()).thenReturn(environment);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
  }

  @Test
  void addPropertiesPropertySource() {
    final OperatonBpmVersion version = new OperatonBpmVersion();

    new PropertiesListener(version).onApplicationEvent(event);

    verify(mutablePropertySources).addFirst(propertiesPropertySource.capture());

    assertThat(propertiesPropertySource.getValue()).isEqualTo(version.getPropertiesPropertySource());
  }
}
