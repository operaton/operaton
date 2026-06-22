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

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.FlushResult;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbBulkOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbEntityOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * For mybatis {@link ExecutorType#SIMPLE}
 */
public class SimpleDbSqlSession extends DbSqlSession {

  public SimpleDbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
    super(dbSqlSessionFactory);
  }

  public SimpleDbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
    super(dbSqlSessionFactory, connection, catalog, schema);
  }

  // lock ////////////////////////////////////////////

  @Override
  protected void executeSelectForUpdate(String statement, Object parameter) {
    update(statement, parameter);
  }

  @Override
  public FlushResult executeDbOperations(List<DbOperation> operations) {

    for (int i = 0; i < operations.size(); i++) {

      DbOperation operation = operations.get(i);

      executeDbOperation(operation);

      if (operation.isFailed()) {
        List<DbOperation> remainingOperations = operations.subList(i + 1, operations.size());
        return FlushResult.withFailuresAndRemaining(Collections.singletonList(operation), remainingOperations);
      }
    }

    return FlushResult.allApplied();
  }

  // insert //////////////////////////////////////////

  @Override
  protected void insertEntity(DbEntityOperation operation) {

    final DbEntity dbEntity = operation.getEntity();

    // get statement
    String insertStatement = dbSqlSessionFactory.getInsertStatement(dbEntity);
    insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);
    ensureNotNull("no insert statement for %s in the ibatis mapping files".formatted(dbEntity.getClass()), "insertStatement", insertStatement);

    // execute the insert
    try {
      executeInsertEntity(insertStatement, dbEntity);
      entityInsertPerformed(operation, 1, null);
    } catch (PersistenceException e) {
      entityInsertPerformed(operation, 0, e);
    }
  }

  // delete ///////////////////////////////////////////

  @Override
  protected void deleteEntity(DbEntityOperation operation) {

    final DbEntity dbEntity = operation.getEntity();

    // get statement
    String deleteStatement = dbSqlSessionFactory.getDeleteStatement(dbEntity.getClass());
    ensureNotNull("no delete statement for %s in the ibatis mapping files".formatted(dbEntity.getClass()), "deleteStatement", deleteStatement);

    LOG.executeDatabaseOperation("DELETE", dbEntity);

    try {
      int nrOfRowsDeleted = executeDelete(deleteStatement, dbEntity);
      entityDeletePerformed(operation, nrOfRowsDeleted, null);
    } catch (PersistenceException e) {
      entityDeletePerformed(operation, 0, e);
    }
  }

  @Override
  protected void deleteBulk(DbBulkOperation operation) {
    String statement = operation.getStatement();
    Object parameter = operation.getParameter();

    LOG.executeDatabaseBulkOperation("DELETE", statement, parameter);

    try {
      int rowsAffected = executeDelete(statement, parameter);
      bulkDeletePerformed(operation, rowsAffected, null);
    } catch (PersistenceException e) {
      bulkDeletePerformed(operation, 0, e);
    }
  }

  // update ////////////////////////////////////////

  @Override
  protected void updateEntity(DbEntityOperation operation) {

    final DbEntity dbEntity = operation.getEntity();

    String updateStatement = dbSqlSessionFactory.getUpdateStatement(dbEntity);
    ensureNotNull("no update statement for %s in the ibatis mapping files".formatted(dbEntity.getClass()), "updateStatement", updateStatement);

    LOG.executeDatabaseOperation("UPDATE", dbEntity);

    try {
      int rowsAffected = executeUpdate(updateStatement, dbEntity);
      entityUpdatePerformed(operation, rowsAffected, null);
    } catch (PersistenceException e) {
      entityUpdatePerformed(operation, 0, e);
    }
  }

  @Override
  protected void updateBulk(DbBulkOperation operation) {
    String statement = operation.getStatement();
    Object parameter = operation.getParameter();

    LOG.executeDatabaseBulkOperation("UPDATE", statement, parameter);

    try {
      int rowsAffected = executeUpdate(statement, parameter);
      bulkUpdatePerformed(operation, rowsAffected, null);
    } catch (PersistenceException e) {
      bulkUpdatePerformed(operation, 0, e);
    }
  }
}
