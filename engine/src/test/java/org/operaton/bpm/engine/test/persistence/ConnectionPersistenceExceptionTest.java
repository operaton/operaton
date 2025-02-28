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
package org.operaton.bpm.engine.test.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.PERSISTENCE_CONNECTION_ERROR_CLASS;

import java.sql.SQLException;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.impl.test.RequiredDatabase;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

// This test is excluded on Oracle since the SQL State changed with the new version of the JDBC driver.
@RequiredDatabase(excludes = { DbSqlSessionFactory.H2, DbSqlSessionFactory.ORACLE })
public class ConnectionPersistenceExceptionTest {

  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected IdentityService identityService;

  protected ProcessEngineConfigurationImpl engineConfig;

  protected String resetUrl;

  @Before
  public void assignServices() {
    identityService = engineRule.getIdentityService();

    engineConfig = engineRule.getProcessEngineConfiguration();

    resetUrl = ((PooledDataSource) engineConfig.getDataSource()).getUrl();
  }

  @After
  public void resetEngine() {
    ((PooledDataSource) engineConfig.getDataSource()).setUrl(resetUrl);
    engineRule.getIdentityService().deleteUser("foo");
  }

  @Test
  public void shouldFailWithConnectionError() {
    // given
    User user = identityService.newUser("foo");
    identityService.saveUser(user);

    // when
    SQLException sqlException = provokePersistenceConnectionError();

    // then
    assertThat(sqlException.getSQLState()).startsWith(PERSISTENCE_CONNECTION_ERROR_CLASS);
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  protected SQLException provokePersistenceConnectionError() {

    String jdbcUrl = resetUrl.replace(":tc", "");
    ((PooledDataSource) engineConfig.getDataSource()).setUrl(jdbcUrl);

    Throwable result = catchThrowable(() -> identityService.deleteUser("foo"));

    assertThat(result).isInstanceOf(ProcessEngineException.class);
    assertThat(result.getCause())
        .isInstanceOf(PersistenceException.class) // 1st cause
        .hasCauseInstanceOf(SQLException.class); // 2nd cause

    return (SQLException) result.getCause().getCause();
  }

}
