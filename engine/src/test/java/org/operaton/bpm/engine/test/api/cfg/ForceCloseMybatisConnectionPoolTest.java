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
package org.operaton.bpm.engine.test.api.cfg;

import org.apache.ibatis.datasource.pooled.PoolState;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class ForceCloseMybatisConnectionPoolTest {


  @Test
  void testForceCloseMybatisConnectionPoolTrue() {

    // given
    // that the process engine is configured with forceCloseMybatisConnectionPool = true
    ProcessEngineConfigurationImpl configurationImpl = new StandaloneInMemProcessEngineConfiguration()
     .setJdbcUrl("jdbc:h2:mem:operaton-forceclose")
     .setProcessEngineName("engine-forceclose")
     .setForceCloseMybatisConnectionPool(true);

    ProcessEngine processEngine = configurationImpl
     .buildProcessEngine();

    PooledDataSource pooledDataSource = (PooledDataSource) configurationImpl.getDataSource();
    PoolState state = pooledDataSource.getPoolState();


    // then
    // if the process engine is closed
    processEngine.close();

    // the idle connections are closed
    assertThat(state.getIdleConnectionCount()).isZero();

  }

  @Test
  void testForceCloseMybatisConnectionPoolFalse() {

    // given
    // that the process engine is configured with forceCloseMybatisConnectionPool = false
    ProcessEngineConfigurationImpl configurationImpl = new StandaloneInMemProcessEngineConfiguration()
     .setJdbcUrl("jdbc:h2:mem:operaton-forceclose")
     .setProcessEngineName("engine-forceclose")
     .setForceCloseMybatisConnectionPool(false);

    ProcessEngine processEngine = configurationImpl
     .buildProcessEngine();

    PooledDataSource pooledDataSource = (PooledDataSource) configurationImpl.getDataSource();
    PoolState state = pooledDataSource.getPoolState();
    int idleConnections = state.getIdleConnectionCount();


    // then
    // if the process engine is closed
    processEngine.close();

    // the idle connections are not closed
    assertThat(idleConnections).isEqualTo(state.getIdleConnectionCount());

    pooledDataSource.forceCloseAll();

    assertThat(state.getIdleConnectionCount()).isZero();
  }

}
