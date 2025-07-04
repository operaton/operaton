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
package org.operaton.bpm.engine.impl.db.sql;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.AbstractPersistenceSession;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbBulkOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbEntityOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation.State;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperationType;
import org.operaton.bpm.engine.impl.util.DatabaseUtil;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;

/**
*
* @author Tom Baeyens
* @author Joram Barrez
* @author Daniel Meyer
* @author Sebastian Menski
* @author Roman Smirnov
*
*/
public abstract class DbSqlSession extends AbstractPersistenceSession {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;
  public static final String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };
  public static final String[] PG_JDBC_METADATA_TABLE_TYPES = { "TABLE", "PARTITIONED TABLE" };

  private static final String COMPONENT_CASE_ENGINE = "case.engine";
  private static final String COMPONENT_DECISION_ENGINE = "decision.engine";
  private static final String COMPONENT_ENGINE = "engine";
  private static final String COMPONENT_HISTORY = "history";
  private static final String COMPONENT_IDENTITY = "identity";

  private static final String DB_OPERATION_CREATE = "create";
  private static final String DB_OPERATION_DROP = "drop";

  protected SqlSession sqlSession;
  protected DbSqlSessionFactory dbSqlSessionFactory;

  protected String connectionMetadataDefaultCatalog = null;
  protected String connectionMetadataDefaultSchema = null;

  protected DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    SqlSessionFactory sqlSessionFactory = dbSqlSessionFactory.getSqlSessionFactory();
    this.sqlSession = ExceptionUtil.doWithExceptionWrapper(sqlSessionFactory::openSession);
  }

  protected DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    SqlSessionFactory sqlSessionFactory = dbSqlSessionFactory.getSqlSessionFactory();
    this.sqlSession = ExceptionUtil.doWithExceptionWrapper(() -> sqlSessionFactory.openSession(connection));
    this.connectionMetadataDefaultCatalog = catalog;
    this.connectionMetadataDefaultSchema = schema;
  }

  // select ////////////////////////////////////////////

  @Override
  public List<?> selectList(String statement, Object parameter) {
    statement = dbSqlSessionFactory.mapStatement(statement);
    List<Object> resultList = executeSelectList(statement, parameter);
    for (Object object : resultList) {
      fireEntityLoaded(object);
    }
    return resultList;
  }

  public List<Object> executeSelectList(String statement, Object parameter) {
    return ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.selectList(statement, parameter));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends DbEntity> T selectById(Class<T> type, String id) {
    String selectStatement = dbSqlSessionFactory.getSelectStatement(type);
    String mappedSelectStatement = dbSqlSessionFactory.mapStatement(selectStatement);
    ensureNotNull("no select statement for " + type + " in the ibatis mapping files", "selectStatement", selectStatement);

    Object result = ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.selectOne(mappedSelectStatement, id));
    fireEntityLoaded(result);
    return (T) result;
  }

  @Override
  public Object selectOne(String statement, Object parameter) {
    String mappedStatement = dbSqlSessionFactory.mapStatement(statement);
    Object result = ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.selectOne(mappedStatement, parameter));
    fireEntityLoaded(result);
    return result;
  }

  // lock ////////////////////////////////////////////

  @Override
  public void lock(String statement, Object parameter) {
    // do not perform locking if H2 database is used. H2 uses table level locks
    // by default which may cause deadlocks if the deploy command needs to get a new
    // Id using the DbIdGenerator while performing a deployment.
    if (!DatabaseUtil.checkDatabaseType(DbSqlSessionFactory.H2)) {
      String mappedStatement = dbSqlSessionFactory.mapStatement(statement);
      executeSelectForUpdate(mappedStatement, parameter);
    } else {
      LOG.debugDisabledPessimisticLocks();
    }
  }

  protected abstract void executeSelectForUpdate(String statement, Object parameter);

  protected void entityUpdatePerformed(DbEntityOperation operation,
                                       int rowsAffected,
                                       PersistenceException failure) {
    if (failure != null) {
      configureFailedDbEntityOperation(operation, failure);
    } else {
      DbEntity dbEntity = operation.getEntity();

      if (dbEntity instanceof HasDbRevision versionedObject) {
        if (rowsAffected != 1) {
          // failed with optimistic locking
          operation.setState(State.FAILED_CONCURRENT_MODIFICATION);
        } else {
          // increment revision of our copy
          versionedObject.setRevision(versionedObject.getRevisionNext());
          operation.setState(State.APPLIED);
        }
      } else {
        operation.setState(State.APPLIED);
      }
    }
  }

  protected void bulkUpdatePerformed(DbBulkOperation operation,
                                     int rowsAffected,
                                     PersistenceException failure) {

    bulkOperationPerformed(operation, rowsAffected, failure);
  }

  protected void bulkDeletePerformed(DbBulkOperation operation,
                                     int rowsAffected,
                                     PersistenceException failure) {

    bulkOperationPerformed(operation, rowsAffected, failure);
  }

  protected void bulkOperationPerformed(DbBulkOperation operation,
                                        int rowsAffected,
                                        PersistenceException failure) {

    if (failure != null) {
      operation.setFailure(failure);

      State failedState = State.FAILED_ERROR;
      operation.setState(failedState);
    } else {
      operation.setRowsAffected(rowsAffected);
      operation.setState(State.APPLIED);
    }
  }

  protected void entityDeletePerformed(DbEntityOperation operation,
                                       int rowsAffected,
                                       PersistenceException failure) {

    if (failure != null) {
      configureFailedDbEntityOperation(operation, failure);
    } else {
      operation.setRowsAffected(rowsAffected);

      DbEntity dbEntity = operation.getEntity();

      // It only makes sense to check for optimistic locking exceptions for objects that actually have a revision
      if (dbEntity instanceof HasDbRevision && rowsAffected == 0) {
        operation.setState(State.FAILED_CONCURRENT_MODIFICATION);
      } else {
        operation.setState(State.APPLIED);
      }
    }
  }

  protected void configureFailedDbEntityOperation(DbEntityOperation operation, PersistenceException failure) {
    operation.setRowsAffected(0);
    operation.setFailure(failure);

    DbOperationType operationType = operation.getOperationType();
    DbOperation dependencyOperation = operation.getDependentOperation();

    State failedState;
    if (isConcurrentModificationException(operation, failure)) {
      failedState = DatabaseUtil.checkDatabaseRollsBackTransactionOnError()
          ? State.FAILED_CONCURRENT_MODIFICATION_EXCEPTION
          : State.FAILED_CONCURRENT_MODIFICATION;
    } else if (DbOperationType.DELETE.equals(operationType)
              && dependencyOperation != null
              && dependencyOperation.getState() != null
              && dependencyOperation.getState() != State.APPLIED) {

      // the owning operation was not successful, so the prerequisite for this operation was not given
      LOG.ignoreFailureDuePreconditionNotMet(operation, "Parent database operation failed", dependencyOperation);
      failedState = State.NOT_APPLIED;
    } else {

      failedState = State.FAILED_ERROR;
    }
    operation.setState(failedState);
  }

  protected boolean isConcurrentModificationException(DbOperation failedOperation,
                                                      PersistenceException cause) {

    boolean isConstraintViolation = ExceptionUtil.checkForeignKeyConstraintViolation(cause);
    boolean isVariableIntegrityViolation = ExceptionUtil.checkVariableIntegrityViolation(cause);

    if (isVariableIntegrityViolation) {
      return true;
    } else if (
      isConstraintViolation
      && failedOperation instanceof DbEntityOperation dbEntityOperation
      && dbEntityOperation.getEntity() instanceof HasDbReferences hasDbReferences
      && (failedOperation.getOperationType().equals(DbOperationType.INSERT)
      || failedOperation.getOperationType().equals(DbOperationType.UPDATE))
      ) {
      if (DatabaseUtil.checkDatabaseRollsBackTransactionOnError()) {
        return Context.getCommandContext().getProcessEngineConfiguration().isEnableOptimisticLockingOnForeignKeyViolation();
      }
      for (Map.Entry<String, Class> reference : hasDbReferences.getReferencedEntitiesIdAndClass().entrySet()) {
        DbEntity referencedEntity = selectById(reference.getValue(), reference.getKey());
        if (referencedEntity == null) {
          return true;
        }
      }
    }

    return false;
  }

  // insert //////////////////////////////////////////

  @Override
  protected void insertEntity(DbEntityOperation operation) {

    final DbEntity dbEntity = operation.getEntity();

    // get statement
    String insertStatement = dbSqlSessionFactory.getInsertStatement(dbEntity);
    insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);
    ensureNotNull("no insert statement for " + dbEntity.getClass() + " in the ibatis mapping files", "insertStatement", insertStatement);

    // execute the insert
    executeInsertEntity(insertStatement, dbEntity);
  }

  protected void executeInsertEntity(String insertStatement, Object parameter) {
    LOG.executeDatabaseOperation("INSERT", parameter);
    try {
      sqlSession.insert(insertStatement, parameter);
    } catch (Exception e) {
      // exception is wrapped later
      throw e;
    }
  }

  @SuppressWarnings("unused")
  protected void entityInsertPerformed(DbEntityOperation operation,
                                       int rowsAffected,
                                       PersistenceException failure) {
    DbEntity entity = operation.getEntity();

    if (failure != null) {
      configureFailedDbEntityOperation(operation, failure);
    } else {
      // set revision of our copy to 1
      if (entity instanceof HasDbRevision versionedObject) {
        versionedObject.setRevision(1);
      }

      operation.setState(State.APPLIED);
    }
  }

  // delete ///////////////////////////////////////////

  protected int executeDelete(String deleteStatement, Object parameter) {
    // map the statement
    String mappedDeleteStatement = dbSqlSessionFactory.mapStatement(deleteStatement);
    try {
      return sqlSession.delete(mappedDeleteStatement, parameter);
    } catch (Exception e) {
      // Exception is wrapped later
      throw e;
    }
  }

  // update ////////////////////////////////////////

  public int executeUpdate(String updateStatement, Object parameter) {
    String mappedUpdateStatement = dbSqlSessionFactory.mapStatement(updateStatement);
    try {
      return sqlSession.update(mappedUpdateStatement, parameter);
    } catch (Exception e) {
      // Exception is wrapped later
      throw e;
    }
  }

  public int update(String updateStatement, Object parameter) {
    return ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.update(updateStatement, parameter));
  }

  @Override
  public int executeNonEmptyUpdateStmt(String updateStmt, Object parameter) {
    String mappedUpdateStmt = dbSqlSessionFactory.mapStatement(updateStmt);

    //if mapped statement is empty, which can happens for some databases, we have no need to execute it
    boolean isMappedStmtEmpty = ExceptionUtil.doWithExceptionWrapper(() -> {
      Configuration configuration = sqlSession.getConfiguration();
      MappedStatement mappedStatement = configuration.getMappedStatement(mappedUpdateStmt);
      BoundSql boundSql = mappedStatement.getBoundSql(parameter);
      String sql = boundSql.getSql();
      return sql.isEmpty();
    });

    if (isMappedStmtEmpty) {
      return 0;
    }

    return update(mappedUpdateStmt, parameter);
  }

  // flush ////////////////////////////////////////////////////////////////////

  @Override
  public void flush() {
  }

  @Override
  public void flushOperations() {
    ExceptionUtil.doWithExceptionWrapper(this::flushBatchOperations);
  }

  public List<BatchResult> flushBatchOperations() {
    try {
      return sqlSession.flushStatements();

    } catch (PersistenceException ex) {
      // exception is wrapped later
      throw ex;

    }
  }

  @Override
  public void close() {
    ExceptionUtil.doWithExceptionWrapper(() -> {
      sqlSession.close();
      return null;
    });
  }

  @Override
  public void commit() {
    ExceptionUtil.doWithExceptionWrapper(() -> {
      sqlSession.commit();
      return null;
    });
  }

  @Override
  public void rollback() {
    ExceptionUtil.doWithExceptionWrapper(() -> {
      sqlSession.rollback();
      return null;
    });
  }

  // schema operations ////////////////////////////////////////////////////////

  @Override
  public void dbSchemaCheckVersion() {
    try {
      String dbVersion = getDbVersion();
      if (!ProcessEngine.VERSION.equals(dbVersion)) {
        throw LOG.wrongDbVersionException(ProcessEngine.VERSION, dbVersion);
      }

      List<String> missingComponents = new ArrayList<>();
      if (!isEngineTablePresent()) {
        missingComponents.add(COMPONENT_ENGINE);
      }
      if (dbSqlSessionFactory.isDbHistoryUsed() && !isHistoryTablePresent()) {
        missingComponents.add(COMPONENT_HISTORY);
      }
      if (dbSqlSessionFactory.isDbIdentityUsed() && !isIdentityTablePresent()) {
        missingComponents.add(COMPONENT_IDENTITY);
      }
      if (dbSqlSessionFactory.isCmmnEnabled() && !isCmmnTablePresent()) {
        missingComponents.add(COMPONENT_CASE_ENGINE);
      }
      if (dbSqlSessionFactory.isDmnEnabled() && !isDmnTablePresent()) {
        missingComponents.add(COMPONENT_DECISION_ENGINE);
      }

      if (!missingComponents.isEmpty()) {
        throw LOG.missingTableException(missingComponents);
      }

    } catch (Exception e) {
      if (isMissingTablesException(e)) {
        throw LOG.missingActivitiTablesException();
      } else {
        if (e instanceof RuntimeException runtimeException) {
          throw runtimeException;
        } else {
          throw LOG.unableToFetchDbSchemaVersion(e);
        }
      }
    }
  }

  @Override
  protected String getDbVersion() {
    String selectSchemaVersionStatement = dbSqlSessionFactory.mapStatement("selectDbSchemaVersion");
    return ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.selectOne(selectSchemaVersionStatement));
  }

  @Override
  protected void dbSchemaCreateIdentity() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, COMPONENT_IDENTITY);
  }

  @Override
  protected void dbSchemaCreateHistory() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, COMPONENT_HISTORY);
  }

  @Override
  protected void dbSchemaCreateEngine() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, COMPONENT_ENGINE);
  }

  @Override
  protected void dbSchemaCreateCmmn() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, COMPONENT_CASE_ENGINE);
  }

  @Override
  protected void dbSchemaCreateCmmnHistory() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, "case.history");
  }

  @Override
  protected void dbSchemaCreateDmn() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, COMPONENT_DECISION_ENGINE);
  }


  @Override
  protected void dbSchemaCreateDmnHistory() {
    executeMandatorySchemaResource(DB_OPERATION_CREATE, "decision.history");
  }

  @Override
  protected void dbSchemaDropIdentity() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, COMPONENT_IDENTITY);
  }

  @Override
  protected void dbSchemaDropHistory() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, COMPONENT_HISTORY);
  }

  @Override
  protected void dbSchemaDropEngine() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, COMPONENT_ENGINE);
  }

  @Override
  protected void dbSchemaDropCmmn() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, COMPONENT_CASE_ENGINE);
  }

  @Override
  protected void dbSchemaDropCmmnHistory() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, "case.history");
  }

  @Override
  protected void dbSchemaDropDmn() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, COMPONENT_DECISION_ENGINE);
  }

  @Override
  protected void dbSchemaDropDmnHistory() {
    executeMandatorySchemaResource(DB_OPERATION_DROP, "decision.history");
  }

  public void executeMandatorySchemaResource(String operation, String component) {
    executeSchemaResource(operation, component, getResourceForDbOperation(operation, operation, component), false);
  }

  @Override
  public boolean isEngineTablePresent(){
    return isTablePresent("ACT_RU_EXECUTION");
  }
  @Override
  public boolean isHistoryTablePresent(){
    return isTablePresent("ACT_HI_PROCINST");
  }
  @Override
  public boolean isIdentityTablePresent(){
    return isTablePresent("ACT_ID_USER");
  }

  @Override
  public boolean isCmmnTablePresent() {
    return isTablePresent("ACT_RE_CASE_DEF");
  }

  @Override
  public boolean isCmmnHistoryTablePresent() {
    return isTablePresent("ACT_HI_CASEINST");
  }

  @Override
  public boolean isDmnTablePresent() {
    return isTablePresent("ACT_RE_DECISION_DEF");
  }

  @Override
  public boolean isDmnHistoryTablePresent() {
    return isTablePresent("ACT_HI_DECINST");
  }

  public boolean isTablePresent(String tableName) {
    tableName = prependDatabaseTablePrefix(tableName);
    Connection connection = null;
    try {
      connection = ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.getConnection());
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      ResultSet tables = null;

      String schema = this.connectionMetadataDefaultSchema;
      if (dbSqlSessionFactory.getDatabaseSchema()!=null) {
        schema = dbSqlSessionFactory.getDatabaseSchema();
      }

      if (DatabaseUtil.checkDatabaseType(DbSqlSessionFactory.POSTGRES)) {
        tableName = tableName.toLowerCase();
      }

      try {
        tables = databaseMetaData.getTables(this.connectionMetadataDefaultCatalog, schema, tableName, getTableTypes());
        return tables.next();
      } finally {
        if (tables != null) {
          tables.close();
        }
      }

    } catch (Exception e) {
      throw LOG.checkDatabaseTableException(e);
    }
  }

  @Override
  public List<String> getTableNamesPresent() {
    List<String> tableNames = new ArrayList<>();

    try {
      ResultSet tablesRs = null;

      try {
        if (DbSqlSessionFactory.ORACLE.equals(getDbSqlSessionFactory().getDatabaseType())) {
          tableNames = getTablesPresentInOracleDatabase();
        } else {
          Connection connection = getSqlSession().getConnection();

          String databaseTablePrefix = getDbSqlSessionFactory().getDatabaseTablePrefix();
          String schema = getDbSqlSessionFactory().getDatabaseSchema();
          String tableNameFilter = prependDatabaseTablePrefix("ACT_%");

          // for postgres, we have to use lower case
          if (DatabaseUtil.checkDatabaseType(DbSqlSessionFactory.POSTGRES)) {
            schema = schema == null ? schema : schema.toLowerCase();
            tableNameFilter = tableNameFilter.toLowerCase();
          }

          DatabaseMetaData databaseMetaData = connection.getMetaData();
          tablesRs = databaseMetaData.getTables(null, schema, tableNameFilter, getTableTypes());
          while (tablesRs.next()) {
            String tableName = tablesRs.getString("TABLE_NAME");
            if (!databaseTablePrefix.isEmpty()) {
              tableName = databaseTablePrefix + tableName;
            }
            tableName = tableName.toUpperCase();
            tableNames.add(tableName);
          }
          LOG.fetchDatabaseTables("jdbc metadata", tableNames);
        }
      } catch (SQLException se) {
        throw se;
      } finally {
        if (tablesRs != null) {
          tablesRs.close();
        }
      }
    } catch (Exception e) {
      throw LOG.getDatabaseTableNameException(e);
    }

    return tableNames;
  }

  protected List<String> getTablesPresentInOracleDatabase() throws SQLException {
    List<String> tableNames = new ArrayList<>();
    String selectTableNamesFromOracle = "SELECT table_name FROM all_tables WHERE table_name LIKE ? ESCAPE '-'";
    String databaseTablePrefix = getDbSqlSessionFactory().getDatabaseTablePrefix();

    try(Connection connection = Context.getProcessEngineConfiguration().getDataSource().getConnection();
        PreparedStatement prepStat = connection.prepareStatement(selectTableNamesFromOracle)) {

      prepStat.setString(1, databaseTablePrefix + "ACT-_%");

      try(ResultSet tablesRs = prepStat.executeQuery()) {
        while (tablesRs.next()) {
          String tableName = tablesRs.getString("TABLE_NAME");
          tableName = tableName.toUpperCase();
          tableNames.add(tableName);
        }
        LOG.fetchDatabaseTables("oracle all_tables", tableNames);
      }
    }

    return tableNames;
  }


  public String prependDatabaseTablePrefix(String tableName) {
    String prefixWithoutSchema = dbSqlSessionFactory.getDatabaseTablePrefix();
    String schema = dbSqlSessionFactory.getDatabaseSchema();
    if (prefixWithoutSchema == null) {
      return tableName;
    }
    if (schema == null) {
      return prefixWithoutSchema + tableName;
    }

    if (prefixWithoutSchema.startsWith(schema + ".")) {
      prefixWithoutSchema = prefixWithoutSchema.substring(schema.length() + 1);
    }

    return prefixWithoutSchema + tableName;
  }

  public String getResourceForDbOperation(String directory, String operation, String component) {
    String databaseType = dbSqlSessionFactory.getDatabaseType();
    return "org/operaton/bpm/engine/db/" + directory + "/activiti." + databaseType + "." + operation + "."+component+".sql";
  }

  public void executeSchemaResource(String operation, String component, String resourceName, boolean isOptional) {
    try (InputStream inputStream = ReflectUtil.getResourceAsStream(resourceName)) {
      if (inputStream == null) {
        if (isOptional) {
          LOG.missingSchemaResource(resourceName, operation);
        } else {
          throw LOG.missingSchemaResourceException(resourceName, operation);
        }
      } else {
        executeSchemaResource(operation, component, resourceName, inputStream);
      }
    } catch (IOException e) {
      //ignore
    }
  }

  public void executeSchemaResource(String schemaFileResourceName) {
    try (FileInputStream inputStream = new FileInputStream(schemaFileResourceName)) {
      executeSchemaResource("schema operation", "process engine", schemaFileResourceName, inputStream);
    } catch (FileNotFoundException e) {
      throw LOG.missingSchemaResourceFileException(schemaFileResourceName, e);
    } catch (IOException e) {
      //ignore
    }
  }

  private void executeSchemaResource(String operation, String component, String resourceName, InputStream inputStream) {
    String sqlStatement = null;
    String exceptionSqlStatement = null;
    try {
      Connection connection = ExceptionUtil.doWithExceptionWrapper(() -> sqlSession.getConnection());
      Exception exception = null;
      byte[] bytes = IoUtil.readInputStream(inputStream, resourceName);
      String ddlStatements = new String(bytes);
      BufferedReader reader = new BufferedReader(new StringReader(ddlStatements));
      String line = readNextTrimmedLine(reader);

      List<String> logLines = new ArrayList<>();

      while (line != null) {
        if (line.startsWith("# ")) {
          logLines.add(line.substring(2));
        } else if (line.startsWith("-- ")) {
          logLines.add(line.substring(3));
        } else if (!line.isEmpty()) {

          if (line.endsWith(";")) {
            sqlStatement = addSqlStatementPiece(sqlStatement, line.substring(0, line.length()-1));
            try {
              Statement jdbcStatement = connection.createStatement();
              // no logging needed as the connection will log it
              logLines.add(sqlStatement);
              jdbcStatement.execute(sqlStatement);
              jdbcStatement.close();
            } catch (Exception e) {
              if (exception == null) {
                exception = e;
                exceptionSqlStatement = sqlStatement;
              }
              LOG.failedDatabaseOperation(operation, sqlStatement, e);
            } finally {
              sqlStatement = null;
            }
          } else {
            sqlStatement = addSqlStatementPiece(sqlStatement, line);
          }
        }

        line = readNextTrimmedLine(reader);
      }
      LOG.performingDatabaseOperation(operation, component, resourceName);
      LOG.executingDDL(logLines);

      if (exception != null) {
        throw exception;
      }

      LOG.successfulDatabaseOperation(operation, component);
    } catch (Exception e) {
      throw LOG.performDatabaseOperationException(operation, exceptionSqlStatement, e);
    }
  }

  protected String addSqlStatementPiece(String sqlStatement, String line) {
    if (sqlStatement==null) {
      return line;
    }
    return sqlStatement + " \n" + line;
  }

  protected String readNextTrimmedLine(BufferedReader reader) throws IOException {
    String line = reader.readLine();
    if (line!=null) {
      line = line.trim();
    }
    return line;
  }

  protected boolean isMissingTablesException(Exception e) {
    Throwable cause = e.getCause();
    if (cause != null) {
      String exceptionMessage = cause.getMessage();
      if(cause.getMessage() != null) {
        // Matches message returned from H2
        if ((exceptionMessage.contains("Table")) && (exceptionMessage.contains("not found"))) {
          return true;
        }

        // Message returned from MySQL and Oracle
        if ((exceptionMessage.contains("Table") || exceptionMessage.contains("table")) && (exceptionMessage.contains("doesn't exist"))) {
          return true;
        }

        // Message returned from Postgres
        return (exceptionMessage.contains("relation") || exceptionMessage.contains("table")) && (exceptionMessage.contains("does not exist"));
      }
    }
    return false;
  }

  protected String[] getTableTypes() {
    // the PostgreSQL JDBC API changed in 42.2.11 and partitioned tables
    // are not detected unless the corresponding table type flag is added.
    if (DatabaseUtil.checkDatabaseType(DbSqlSessionFactory.POSTGRES)) {
      return PG_JDBC_METADATA_TABLE_TYPES;
    }

    return JDBC_METADATA_TABLE_TYPES;
  }

  // getters and setters //////////////////////////////////////////////////////

  public SqlSession getSqlSession() {
    return sqlSession;
  }
  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

}
