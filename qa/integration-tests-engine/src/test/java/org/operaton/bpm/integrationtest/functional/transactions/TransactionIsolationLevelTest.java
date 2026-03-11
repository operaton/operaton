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
package org.operaton.bpm.integrationtest.functional.transactions;

import java.sql.Connection;
import java.sql.SQLException;
import jakarta.inject.Inject;

import org.apache.ibatis.session.SqlSession;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.operaton.bpm.integrationtest.util.TestContainer.addContainerSpecificResourcesForNonPaWithoutWeld;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(ArquillianExtension.class)
public class TransactionIsolationLevelTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    WebArchive archive = initWebArchiveDeployment();
    addContainerSpecificResourcesForNonPaWithoutWeld(archive);
    return archive;
  }

  @Inject
  private ProcessEngine processEngine;

  @Test
  void testTransactionIsolationLevelOnConnection() {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    SqlSession sqlSession = processEngineConfiguration.getDbSqlSessionFactory()
        .getSqlSessionFactory()
        .openSession();
    try {
      int transactionIsolation = sqlSession.getConnection().getTransactionIsolation();
      assertThat(transactionIsolation).as("TransactionIsolationLevel for connection is %d instead of %d".formatted(transactionIsolation, Connection.TRANSACTION_READ_COMMITTED)).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
