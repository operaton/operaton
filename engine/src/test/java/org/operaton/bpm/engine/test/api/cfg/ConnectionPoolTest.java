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
package org.operaton.bpm.engine.test.api.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.Configuration;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.Test;


/**
 * @author Joram Barrez
 */
public class ConnectionPoolTest {

  @Test
  public void testMyBatisConnectionPoolProperlyConfigured() {
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("org/operaton/bpm/engine/test/api/cfg/connection-pool.operaton.cfg.xml");

    ProcessEngine engine = config.buildProcessEngine();

    // Expected values
    int maxActive = 25;
    int maxIdle = 10;
    int maxCheckoutTime = 30000;
    int maxWaitTime = 25000;
    Integer jdbcStatementTimeout = 300;

    assertThat(config.getJdbcMaxActiveConnections()).isEqualTo(maxActive);
    assertThat(config.getJdbcMaxIdleConnections()).isEqualTo(maxIdle);
    assertThat(config.getJdbcMaxCheckoutTime()).isEqualTo(maxCheckoutTime);
    assertThat(config.getJdbcMaxWaitTime()).isEqualTo(maxWaitTime);
    assertThat(config.getJdbcStatementTimeout()).isEqualTo(jdbcStatementTimeout);

    // Verify that these properties are correctly set in the MyBatis datasource
    Configuration sessionFactoryConfiguration = config.getDbSqlSessionFactory().getSqlSessionFactory().getConfiguration();
    DataSource datasource = sessionFactoryConfiguration.getEnvironment().getDataSource();
    assertTrue(datasource instanceof PooledDataSource);

    PooledDataSource pooledDataSource = (PooledDataSource) datasource;
    assertThat(pooledDataSource.getPoolMaximumActiveConnections()).isEqualTo(maxActive);
    assertThat(pooledDataSource.getPoolMaximumIdleConnections()).isEqualTo(maxIdle);
    assertThat(pooledDataSource.getPoolMaximumCheckoutTime()).isEqualTo(maxCheckoutTime);
    assertThat(pooledDataSource.getPoolTimeToWait()).isEqualTo(maxWaitTime);

    assertThat(sessionFactoryConfiguration.getDefaultStatementTimeout()).isEqualTo(jdbcStatementTimeout);

    engine.close();
  }

}
