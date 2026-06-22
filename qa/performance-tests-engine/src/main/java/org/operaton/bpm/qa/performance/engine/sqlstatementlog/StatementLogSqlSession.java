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
package org.operaton.bpm.qa.performance.engine.sqlstatementlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import org.operaton.bpm.qa.performance.engine.util.DelegatingSqlSession;
import org.operaton.bpm.qa.performance.engine.util.JsonUtil;

/**
 * <p>This SqlSession wraps an actual SqlSession and logs executed sql statements. (Calls to the
 * delete*, update*, select*, insert* methods.)</p>
 *
 * @author Daniel Meyer
 *
 */
public class StatementLogSqlSession extends DelegatingSqlSession {

  protected static final ThreadLocal<List<SqlStatementLog>> threadStatementLog = new ThreadLocal<>();

  public StatementLogSqlSession(SqlSession wrappedSession) {
    super(wrappedSession);
  }

  // statement interceptors ///////////////////////////////////

  @Override
  public int delete(String statement) {
    long start = System.currentTimeMillis();

    int result = super.delete(statement);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.DELETE, null, statement, duration);
    return result;
  }

  @Override
  public int delete(String statement, Object parameter) {
    long start = System.currentTimeMillis();

    int result = super.delete(statement, parameter);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.DELETE, parameter, statement, duration);
    return result;
  }

  @Override
  public int insert(String statement) {
    long start = System.currentTimeMillis();

    int result = super.insert(statement);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.INSERT, null, statement, duration);
    return result;
  }

  @Override
  public int insert(String statement, Object parameter) {
    long start = System.currentTimeMillis();

    int result = super.insert(statement, parameter);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.INSERT, parameter, statement, duration);
    return result;
  }

  @Override
  public int update(String statement) {
    long start = System.currentTimeMillis();

    int result = super.update(statement);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.UPDATE, null, statement, duration);
    return result;
  }

  @Override
  public int update(String statement, Object parameter) {
    long start = System.currentTimeMillis();

    int result = super.update(statement, parameter);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.UPDATE, parameter, statement, duration);
    return result;
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    long start = System.currentTimeMillis();

    super.select(statement, parameter, handler);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT, parameter, statement, duration);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    long start = System.currentTimeMillis();

    super.select(statement, parameter, rowBounds, handler);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT, parameter, statement, duration);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    long start = System.currentTimeMillis();

    super.select(statement, handler);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT, null, statement, duration);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    long start = System.currentTimeMillis();

    List<E> result = super.selectList(statement);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_LIST, null, statement, duration);

    return result;
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    long start = System.currentTimeMillis();

    List<E> result = super.selectList(statement, parameter);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_LIST, parameter, statement, duration);

    return result;
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    long start = System.currentTimeMillis();

    List<E> result = super.selectList(statement, parameter, rowBounds);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_LIST, parameter, statement, duration);

    return result;
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    long start = System.currentTimeMillis();

    Map<K, V> result = super.selectMap(statement, parameter, mapKey);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_MAP, parameter, statement, duration);

    return result;
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    long start = System.currentTimeMillis();

    Map<K, V> result = super.selectMap(statement, parameter, mapKey, rowBounds);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_MAP, parameter, statement, duration);

    return result;
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    long start = System.currentTimeMillis();

    Map<K, V> result = super.selectMap(statement, mapKey);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_MAP, mapKey, statement, duration);

    return result;
  }

  @Override
  public <T> T selectOne(String statement) {
    long start = System.currentTimeMillis();

    T result = super.selectOne(statement);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_MAP, null, statement, duration);

    return result;
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    long start = System.currentTimeMillis();

    T result = super.selectOne(statement, parameter);

    long duration = System.currentTimeMillis() - start;
    logStatement(SqlStatementType.SELECT_MAP, parameter, statement, duration);

    return result;
  }

  // logging ////////////////////////////////////////////////

  protected void logStatement(SqlStatementType type, Object parameters, String statement, long duration) {
    List<SqlStatementLog> log = threadStatementLog.get();
    if(log != null) {
      log.add(new SqlStatementLog(type, parameters, statement, duration));
    }
  }

  /**
   * stops logging statement executed by the current thread and returns the list of logged statements.
   * @return the {@link List} of logged sql statements
   */
  public static List<SqlStatementLog> stopLogging() {
    List<SqlStatementLog> log = threadStatementLog.get();
    threadStatementLog.remove();
    return log;
  }

  /**
   * starts logging any statements executed by the calling thread.
   */
  public static void startLogging() {
    threadStatementLog.set(new ArrayList<>());
  }

  // log classes //////////////////////////////////////

  public static class SqlStatementLog {

    protected final SqlStatementType statementType;

    /** the statement (sql string) */
    protected final String statement;

    /** the duration the statement took to execute in Milliseconds */
    protected final long durationMs;

    protected String statementParameters;

    public SqlStatementLog(SqlStatementType type, Object parameters, String statement, long duration) {
      statementType = type;
      this.statement = statement;
      this.durationMs = duration;
      try {
        statementParameters = JsonUtil.getMapper().writeValueAsString(parameters).replace("\"", "'");
      } catch (Exception e) {
        // ignore
      }
    }

    public String getStatement() {
      return statement;
    }

    public SqlStatementType getStatementType() {
      return statementType;
    }

    public long getDurationMs() {
      return durationMs;
    }

    public String getStatementParameters() {
      return statementParameters;
    }

  }

  public enum SqlStatementType {
    SELECT, SELECT_ONE, SELECT_LIST, SELECT_MAP,
    INSERT,
    UPDATE,
    DELETE
  }

}
