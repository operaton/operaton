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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import java.util.Optional;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DefaultDatasourceConfigurationTest {

  @Mock
  PlatformTransactionManager platformTransactionManager;

  @Mock
  Optional<PlatformTransactionManager> operatonTransactionManager;

  @InjectMocks
  DefaultDatasourceConfiguration defaultDatasourceConfiguration;

  SpringProcessEngineConfiguration configuration;

  @BeforeEach
  void before() {
    configuration = new SpringProcessEngineConfiguration();
    defaultDatasourceConfiguration.operatonBpmProperties = new OperatonBpmProperties();
  }

  @Test
  void transactionManagerTest() {
    defaultDatasourceConfiguration.dataSource = mock(DataSource.class);
    defaultDatasourceConfiguration.preInit(configuration);
    assertThat(configuration.getTransactionManager()).isSameAs(platformTransactionManager);
  }

  @Test
  void operatonTransactionManagerTest() {
    defaultDatasourceConfiguration.dataSource = mock(DataSource.class);
    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    defaultDatasourceConfiguration.operatonTransactionManager = transactionManager;
    defaultDatasourceConfiguration.preInit(configuration);
    assertThat(configuration.getTransactionManager()).isSameAs(transactionManager);
  }

  @Test
  void defaultDataSourceTest() {
    DataSource datasourceMock = mock(DataSource.class);
    defaultDatasourceConfiguration.dataSource = datasourceMock;
    defaultDatasourceConfiguration.preInit(configuration);
    assertThat(getDataSourceFromConfiguration()).isSameAs(datasourceMock);
  }

  @Test
  void operatonDataSourceTest() {
    DataSource operatonDatasourceMock = mock(DataSource.class);
    defaultDatasourceConfiguration.operatonDataSource = operatonDatasourceMock;
    defaultDatasourceConfiguration.dataSource = mock(DataSource.class);
    defaultDatasourceConfiguration.preInit(configuration);
    assertThat(getDataSourceFromConfiguration()).isSameAs(operatonDatasourceMock);
  }

  private DataSource getDataSourceFromConfiguration() {
    return ((TransactionAwareDataSourceProxy) configuration.getDataSource()).getTargetDataSource();
  }
}
