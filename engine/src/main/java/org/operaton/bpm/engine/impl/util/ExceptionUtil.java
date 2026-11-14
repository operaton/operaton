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
package org.operaton.bpm.engine.impl.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchExecutorException;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ProcessEnginePersistenceException;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.repository.ResourceType;

import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.DB2;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.H2;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.MARIADB_MYSQL;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.MSSQL;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.ORACLE;
import static org.operaton.bpm.engine.impl.util.ExceptionUtil.DEADLOCK_CODES.POSTGRES;

/**
 * @author Roman Smirnov
 * @author Askar Akhmerov
 */
public final class ExceptionUtil {

  public static final String PERSISTENCE_EXCEPTION_MESSAGE = "An exception occurred in the " +
      "persistence layer. Please check the server logs for a detailed message and the entire " +
      "exception stack trace.";

  public static final String PERSISTENCE_CONNECTION_ERROR_CLASS = "08";

  private ExceptionUtil() {
  }

  // Locale.ROOT, e.g. Turkish breaks "I"
  private static String toLowerCase(@Nullable String message) {
    return message == null ? "" : message.toLowerCase(Locale.ROOT);
  }

  public static String getExceptionStacktrace(Throwable exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  public static @Nullable String getExceptionStacktrace(@Nullable ByteArrayEntity byteArray) {
    if (byteArray == null) {
      return null;
    }

    return StringUtil.fromBytes(byteArray.getBytes());
  }

  public static @Nullable ByteArrayEntity createJobExceptionByteArray(byte[] byteArray, ResourceType type) {
    return createExceptionByteArray("job.exceptionByteArray", byteArray, type);
  }

  /**
   * create ByteArrayEntity with specified name and payload and make sure it's
   * persisted
   *
   * <p>
   * used in Jobs and ExternalTasks
   * </p>
   *
   * @param name - type\source of the exception
   * @param byteArray - payload of the exception
   * @param type - resource type of the exception
   * @return persisted entity
   */
  public static @Nullable ByteArrayEntity createExceptionByteArray(String name, byte @Nullable[] byteArray, ResourceType type) {
    if (byteArray == null) {
      return null;
    }

    ByteArrayEntity result = new ByteArrayEntity(name, byteArray, type);
    Context.getCommandContext()
      .getByteArrayManager()
      .insertByteArray(result);
    return result;
  }

  private static Throwable getPersistenceCauseException(PersistenceException persistenceException) {
    Throwable cause = persistenceException.getCause();
    if (cause instanceof BatchExecutorException) {
      return cause.getCause();
    } else {
      return persistenceException.getCause();
    }
  }

  public static @Nullable SQLException unwrapException(PersistenceException persistenceException) {
    Throwable cause = getPersistenceCauseException(persistenceException);
    if (cause instanceof SQLException sqlException) {
      SQLException nextException = sqlException.getNextException();
      return Objects.requireNonNullElse(nextException, sqlException);
    } else {
      return null;
    }
  }

  public static @Nullable SQLException unwrapException(ProcessEngineException genericPersistenceException) {
    Throwable cause = genericPersistenceException.getCause();

    if (cause instanceof ProcessEngineException processEngineException) {
      Throwable processEngineExceptionCause = processEngineException.getCause();
      if (processEngineExceptionCause instanceof PersistenceException persistenceException) {
        return unwrapException(persistenceException);
      } else {
        return null;
      }
    } else if (cause instanceof PersistenceException persistenceException) {
      return unwrapException(persistenceException);
    } else {
      return null;
    }
  }

  public static boolean checkValueTooLongException(SQLException sqlException) {
    String sqlState = sqlException.getSQLState();
    int errorCode = sqlException.getErrorCode();

    // SQL standard: string data, right truncation
    if ("22001".equals(sqlState)
        // Oracle
        || errorCode == 12899 || errorCode == 1461 || errorCode == 1401) {
      return true;
    }

    // Fallback for drivers without the codes above
    String message = toLowerCase(sqlException.getMessage());
    return message.contains("too long") ||
        message.contains("too large") ||
        message.contains("ora-01461") ||
        message.contains("ora-01401") ||
        message.contains("data would be truncated") ||
        message.contains("sqlcode=-302, sqlstate=22001");
  }

  public static boolean checkValueTooLongException(ProcessEngineException genericPersistenceException) {
    SQLException sqlException = unwrapException(genericPersistenceException);

    if (sqlException == null) {
      return false;
    }

    return ExceptionUtil.checkValueTooLongException(sqlException);
  }

  public static boolean checkConstraintViolationException(ProcessEngineException genericPersistenceException) {
    SQLException sqlException = unwrapException(genericPersistenceException);

    if (sqlException == null) {
      return false;
    }

    // SQL standard: integrity constraint violation
    String sqlState = sqlException.getSQLState();
    if (sqlState != null && sqlState.startsWith("23")) {
      return true;
    }

    // Fallback for drivers without the codes above
    String message = toLowerCase(sqlException.getMessage());
    return message.contains("constraint") ||
        message.contains("violat") ||
        message.contains("duplicate") ||
        message.contains("ora-00001") ||
        message.contains("sqlcode=-803, sqlstate=23505");
  }

  public static boolean checkForeignKeyConstraintViolation(PersistenceException persistenceException) {
    SQLException sqlException = unwrapException(persistenceException);

    if (sqlException == null) {
      return false;
    }

    return checkForeignKeyConstraintViolation(sqlException);
  }

  public static boolean checkForeignKeyConstraintViolation(SQLException sqlException) {
    String sqlState = sqlException.getSQLState();
    int errorCode = sqlException.getErrorCode();

    // SQL standard: PostgreSQL, DB2, H2
    if ("23503".equals(sqlState)
        // H2
        || "23506".equals(sqlState)
        // DB2
        || "23504".equals(sqlState) && errorCode == -532
        // SqlServer
        || "23000".equals(sqlState) && errorCode == 547
        // MySql & MariaDB
        || "23000".equals(sqlState) && (errorCode == 1451 || errorCode == 1452)
        // Oracle
        || "23000".equals(sqlState) && (errorCode == 2291 || errorCode == 2292)) {
      return true;
    }

    // Fallback for drivers without the codes above
    String message = toLowerCase(sqlException.getMessage());
    return message.contains("foreign key constraint") ||
        message.contains("integrity constraint");
  }

  public static boolean checkVariableIntegrityViolation(PersistenceException persistenceException) {
    SQLException sqlException = unwrapException(persistenceException);

    if (sqlException == null || sqlException.getMessage() == null) {
      return false;
    }

    String message = toLowerCase(sqlException.getMessage());
    String sqlState = sqlException.getSQLState();
    int errorCode = sqlException.getErrorCode();

    // MySQL & MariaDB
    return (message.contains("act_uniq_variable") && "23000".equals(sqlState) && errorCode == 1062)
        // PostgreSQL
        || (message.contains("act_uniq_variable") && "23505".equals(sqlState) && errorCode == 0)
        // SqlServer
        || (message.contains("act_uniq_variable") && "23000".equals(sqlState) && errorCode == 2601)
        // Oracle
        || (message.contains("act_uniq_variable") && "23000".equals(sqlState) && errorCode == 1)
        // H2
        || (message.contains("act_uniq_variable") && "23505".equals(sqlState) && errorCode == 23505);
  }

  public enum DEADLOCK_CODES {
    MARIADB_MYSQL(1213, "40001"),
    MSSQL(1205, "40001"),
    DB2(-911, "40001"),
    ORACLE(60, "61000"),
    POSTGRES(0, "40P01"),
    H2(40001, "40001");
    private final int errorCode;

    private final String sqlState;

    DEADLOCK_CODES(int errorCode, String sqlState) {
      this.errorCode = errorCode;
      this.sqlState = sqlState;
    }

    public int getErrorCode() {
      return errorCode;
    }

    public String getSqlState() {
      return sqlState;
    }

    private boolean equals(int errorCode, String sqlState) {
      return this.getErrorCode() == errorCode && this.getSqlState().equals(sqlState);
    }

  }

  public static boolean checkDeadlockException(SQLException sqlException) {
    String sqlState = sqlException.getSQLState();
    if (sqlState != null) {
      sqlState = sqlState.toUpperCase();
    } else {
      return false;
    }

    int errorCode = sqlException.getErrorCode();

    return MARIADB_MYSQL.equals(errorCode, sqlState) ||
        MSSQL.equals(errorCode, sqlState) ||
        DB2.equals(errorCode, sqlState) ||
        ORACLE.equals(errorCode, sqlState) ||
        POSTGRES.equals(errorCode, sqlState) ||
        H2.equals(errorCode, sqlState);
  }

  public static @Nullable BatchExecutorException findBatchExecutorException(PersistenceException exception) {
    Throwable cause = exception;
    do {
      if (cause instanceof BatchExecutorException batchExecutorException) {
        return batchExecutorException;
      }
      cause = cause.getCause();
    } while (cause != null);

    return null;
  }

  /**
   * Pass logic, which directly calls MyBatis API. In case a MyBatis exception is thrown, it is
   * wrapped into a {@link ProcessEngineException} and never propagated directly to an Engine API
   * call. In some cases, the top-level exception and its message are shown as a response body in
   * the REST API. Wrapping all MyBatis API calls in our codebase makes sure that the top-level
   * exception is always a {@link ProcessEngineException} with a generic message. Like this, SQL
   * details are never disclosed to potential attackers.
   *
   * @param supplier which calls MyBatis API
   * @param <T> is the type of the return value
   * @return the value returned by the supplier
   * @throws ProcessEngineException which wraps the actual exception
   */
  public static <T> T doWithExceptionWrapper(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      throw wrapPersistenceException(ex);
    }
  }

  public static ProcessEnginePersistenceException wrapPersistenceException(Exception ex) {
    return new ProcessEnginePersistenceException(PERSISTENCE_EXCEPTION_MESSAGE, ex);
  }
}
