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
package org.operaton.bpm.qa.performance.engine.util;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 * <p>Implementation of {@link SqlSession} delegating to a wrapped session</p>
 *
 * @author Daniel Meyer
 */
public class DelegatingSqlSession implements SqlSession {

  protected final SqlSession wrappedSession;

  public DelegatingSqlSession(SqlSession wrappedSession) {
    this.wrappedSession = wrappedSession;
  }

  @Override
  public <T> T selectOne(String statement) {
    return wrappedSession.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return wrappedSession.selectOne(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    return wrappedSession.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return wrappedSession.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return wrappedSession.selectList(statement, parameter, rowBounds);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return wrappedSession.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return wrappedSession.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return wrappedSession.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public <T> Cursor<T> selectCursor(String s) {
    return wrappedSession.selectCursor(s);
  }

  @Override
  public <T> Cursor<T> selectCursor(String s, Object o) {
    return wrappedSession.selectCursor(s, o);
  }

  @Override
  public <T> Cursor<T> selectCursor(String s, Object o, RowBounds rowBounds) {
    return wrappedSession.selectCursor(s, o, rowBounds);
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    wrappedSession.select(statement, parameter, handler);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    wrappedSession.select(statement, handler);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    wrappedSession.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    return wrappedSession.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    return wrappedSession.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    return wrappedSession.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    return wrappedSession.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    return wrappedSession.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    return wrappedSession.delete(statement, parameter);
  }

  @Override
  public void commit() {
    wrappedSession.commit();
  }

  @Override
  public void commit(boolean force) {
    wrappedSession.commit(force);
  }

  @Override
  public void rollback() {
    wrappedSession.rollback();
  }

  @Override
  public void rollback(boolean force) {
    wrappedSession.rollback(force);
  }

  @Override
  public List<BatchResult> flushStatements() {
    return wrappedSession.flushStatements();
  }

  @Override
  public void close() {
    wrappedSession.close();
  }

  @Override
  public void clearCache() {
    wrappedSession.clearCache();
  }

  @Override
  public Configuration getConfiguration() {
    return wrappedSession.getConfiguration();
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return wrappedSession.getMapper(type);
  }

  @Override
  public Connection getConnection() {
    return wrappedSession.getConnection();
  }


}
