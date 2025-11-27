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

import javax.sql.DataSource;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDatasourceConfiguration;
import org.operaton.bpm.spring.boot.starter.property.DatabaseProperty;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

public class DefaultDatasourceConfiguration extends AbstractOperatonConfiguration
  implements OperatonDatasourceConfiguration {

  protected PlatformTransactionManager transactionManager;
  protected PlatformTransactionManager operatonTransactionManager;
  protected DataSource dataSource;
  protected DataSource operatonDataSource;

  public DefaultDatasourceConfiguration(OperatonBpmProperties operatonBpmProperties,
                                        PlatformTransactionManager transactionManager,
                                        PlatformTransactionManager operatonTransactionManager,
                                        DataSource dataSource,
                                        DataSource operatonDataSource) {
    super(operatonBpmProperties);
    this.transactionManager = transactionManager;
    this.operatonTransactionManager = operatonTransactionManager;
    this.dataSource = dataSource;
    this.operatonDataSource = operatonDataSource;
  }

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    final DatabaseProperty database = operatonBpmProperties.getDatabase();

    if (operatonTransactionManager == null) {
      configuration.setTransactionManager(transactionManager);
    } else {
      configuration.setTransactionManager(operatonTransactionManager);
    }

    if (operatonDataSource == null) {
      configuration.setDataSource(dataSource);
    } else {
      configuration.setDataSource(operatonDataSource);
    }

    configuration.setDatabaseType(database.getType());
    configuration.setDatabaseSchemaUpdate(database.getSchemaUpdate());

    if (StringUtils.hasText(database.getTablePrefix())) {
      configuration.setDatabaseTablePrefix(database.getTablePrefix());
    }

    if(StringUtils.hasText(database.getSchemaName())) {
      configuration.setDatabaseSchema(database.getSchemaName());
    }

    configuration.setJdbcBatchProcessing(database.isJdbcBatchProcessing());
  }

  public PlatformTransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public PlatformTransactionManager getOperatonTransactionManager() {
    return operatonTransactionManager;
  }

  public void setOperatonTransactionManager(PlatformTransactionManager operatonTransactionManager) {
    this.operatonTransactionManager = operatonTransactionManager;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getOperatonDataSource() {
    return operatonDataSource;
  }

  public void setOperatonDataSource(DataSource operatonDataSource) {
    this.operatonDataSource = operatonDataSource;
  }

}
