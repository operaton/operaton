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
package org.operaton.bpm.qa.performance.engine.util;

import java.sql.Connection;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * <p>Implements the {@link SqlSessionFactory} delegating
 * to a wrapped {@link SqlSessionFactory}</p>
 *
 * @author Daniel Meyer
 *
 */
public class DelegatingSqlSessionFactory implements SqlSessionFactory {

  protected SqlSessionFactory wrappedSessionFactory;

  public DelegatingSqlSessionFactory(SqlSessionFactory wrappSqlSessionFactory) {
    wrappedSessionFactory = wrappSqlSessionFactory;
  }

  @Override
  public SqlSession openSession() {
    return wrappedSessionFactory.openSession();
  }

  @Override
  public SqlSession openSession(boolean autoCommit) {
    return wrappedSessionFactory.openSession(autoCommit);
  }

  @Override
  public SqlSession openSession(Connection connection) {
    return wrappedSessionFactory.openSession(connection);
  }

  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return wrappedSessionFactory.openSession(level);
  }

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return wrappedSessionFactory.openSession(execType);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return wrappedSessionFactory.openSession(execType, autoCommit);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return wrappedSessionFactory.openSession(execType, level);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return wrappedSessionFactory.openSession(execType, connection);
  }

  @Override
  public Configuration getConfiguration() {
    return wrappedSessionFactory.getConfiguration();
  }

}
