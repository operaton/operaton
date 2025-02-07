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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSession;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.ReflectUtil;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test.None;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;

/**
 * @author Ronny Bräunlich
 */
public class DatabaseTableSchemaTest {

  private static final String SCHEMA_NAME = "SCHEMA1";
  private static final String PREFIX_NAME = "PREFIX1_";

  private PooledDataSource pooledDataSource;

  @Before
  public void setUp() {
    pooledDataSource = new PooledDataSource(ReflectUtil.getClassLoader(), "org.h2.Driver",
        "jdbc:h2:mem:DatabaseTableSchemaTest;DB_CLOSE_DELAY=1000", "sa", "");
  }

  @After
  public void tearDown() throws SQLException {

    Connection connection = pooledDataSource.getConnection();
    connection.createStatement().execute("SHUTDOWN");
    connection.close();
  }

  @Test(expected=None.class)
  public void testPerformDatabaseSchemaOperationCreateTwice() throws Exception {

    Connection connection = pooledDataSource.getConnection();
    connection.createStatement().execute("drop schema if exists " + SCHEMA_NAME + " cascade");
    connection.createStatement().execute("create schema " + SCHEMA_NAME);
    connection.close();

    ProcessEngineConfigurationImpl config1 = createCustomProcessEngineConfiguration().setProcessEngineName("DatabaseTablePrefixTest-engine1")
    // disable auto create/drop schema
        .setDataSource(pooledDataSource).setDatabaseSchemaUpdate("NO_CHECK");
    config1.setDatabaseTablePrefix(SCHEMA_NAME + ".");
    config1.setDatabaseSchema(SCHEMA_NAME);
    config1.setDbMetricsReporterActivate(false);
    ProcessEngine engine1 = config1.buildProcessEngine();

    // create the tables for the first time
    connection = pooledDataSource.getConnection();
    connection.createStatement().execute("set schema " + SCHEMA_NAME);
    engine1.getManagementService().databaseSchemaUpgrade(connection, "", SCHEMA_NAME);
    connection.close();
    // create the tables for the second time; here we shouldn't crash since the
    // session should tell us that the tables are already present and
    // databaseSchemaUpdate is set to noop
    connection = pooledDataSource.getConnection();
    connection.createStatement().execute("set schema " + SCHEMA_NAME);
    engine1.getManagementService().databaseSchemaUpgrade(connection, "", SCHEMA_NAME);
    engine1.close();
  }

  @Test
  public void testTablePresentWithSchemaAndPrefix() throws SQLException {

    Connection connection = pooledDataSource.getConnection();
    connection.createStatement().execute("drop schema if exists " + SCHEMA_NAME + " cascade");
    connection.createStatement().execute("create schema " + SCHEMA_NAME);
    connection.createStatement().execute("create table " + SCHEMA_NAME + "." + PREFIX_NAME + "SOME_TABLE(id varchar(64));");
    connection.close();

    ProcessEngineConfigurationImpl config1 = createCustomProcessEngineConfiguration().setProcessEngineName("DatabaseTablePrefixTest-engine1")
    // disable auto create/drop schema
        .setDataSource(pooledDataSource).setDatabaseSchemaUpdate("NO_CHECK");
    config1.setDatabaseTablePrefix(SCHEMA_NAME + "." + PREFIX_NAME);
    config1.setDatabaseSchema(SCHEMA_NAME);
    config1.setDbMetricsReporterActivate(false);
    ProcessEngine engine = config1.buildProcessEngine();
    CommandExecutor commandExecutor = config1.getCommandExecutorTxRequired();

    commandExecutor.execute(commandContext -> {
      DbSqlSession sqlSession = commandContext.getSession(DbSqlSession.class);
      assertTrue(sqlSession.isTablePresent("SOME_TABLE"));
      return null;
    });

    engine.close();

  }

  @Test
  public void testCreateConfigurationWithMismatchtingSchemaAndPrefix() {
    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
    configuration.setDatabaseSchema("foo");
    configuration.setDatabaseTablePrefix("bar");

    assertThatThrownBy(configuration::buildProcessEngine)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("When setting a schema the prefix has to be schema + '.'");
  }

  @Test
  public void testCreateConfigurationWithMissingDotInSchemaAndPrefix() {
    StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
    configuration.setDatabaseSchema("foo");
    configuration.setDatabaseTablePrefix("foo");

    assertThatThrownBy(configuration::buildProcessEngine).isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("When setting a schema the prefix has to be schema + '.'");
  }

  // ----------------------- TEST HELPERS -----------------------

  // allows to return a process engine configuration which doesn't create a
  // schema when it's build.
  private static class CustomStandaloneInMemProcessEngineConfiguration extends StandaloneInMemProcessEngineConfiguration {

    @Override
    public ProcessEngine buildProcessEngine() {
      init();
      return new NoSchemaProcessEngineImpl(this);
    }

    class NoSchemaProcessEngineImpl extends ProcessEngineImpl {
      public NoSchemaProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
      }

      @Override
      protected void executeSchemaOperations() {
        // nop - do not execute create schema operations
      }
    }

  }

  private static ProcessEngineConfigurationImpl createCustomProcessEngineConfiguration() {
    return new CustomStandaloneInMemProcessEngineConfiguration().setHistory(ProcessEngineConfiguration.HISTORY_FULL);
  }

}
