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

import java.sql.SQLException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ExceptionUtilTest {

  @Test
  void getExceptionStackTraceReturnsStackTrace() {
    // given
    Throwable throwable = new Throwable();
    StackTraceElement stackTraceElement = new StackTraceElement("ExceptionUtil", "getExceptionStackTrace",
        "ExceptionUtil.java", 55);
    throwable.setStackTrace(new StackTraceElement[]{stackTraceElement});

    // when
    String stackTrace = ExceptionUtil.getExceptionStacktrace(throwable);

    // then
    assertThat(stackTrace)
      .startsWith("java.lang.Throwable")
      .contains("at ExceptionUtil.getExceptionStackTrace(ExceptionUtil.java:55)");
  }

  @Test
  void checkValueTooLongException() {
    // when
    boolean resultForUnknown = ExceptionUtil.checkValueTooLongException(mock(SQLException.class));
    // then
    assertThat(resultForUnknown).isFalse();

    // given
    SQLException tooLong = mock(SQLException.class);
    doReturn("too long").when(tooLong).getMessage();

    // when
    boolean resultForTooLong = ExceptionUtil.checkValueTooLongException(tooLong);
    // then
    assertThat(resultForTooLong).isTrue();
  }

  @Test
  void checkConstraintViolationException() {
    // given
    PersistenceException pe = new PersistenceException(mock(SQLException.class));
    // when / then
    assertThat(ExceptionUtil.checkConstraintViolationException(new ProcessEngineException(pe))).isFalse();

    // given
    SQLException constraintViolation = mock(SQLException.class);
    doReturn("ora-00001").when(constraintViolation).getMessage();

    // when
    boolean result = ExceptionUtil.checkConstraintViolationException(new ProcessEngineException(new PersistenceException(constraintViolation)));
    // then
    assertThat(result).isTrue();
  }

  @Test
  void checkForeignKeyConstraintViolation() {
    // when
    boolean unknown = ExceptionUtil.checkForeignKeyConstraintViolation(mock(SQLException.class));
    // then
    assertThat(unknown).isFalse();

    // given
    SQLException constraintViolation = mock(SQLException.class);
    doReturn("integrity constraint").when(constraintViolation).getMessage();

    // when
    boolean result = ExceptionUtil.checkForeignKeyConstraintViolation(constraintViolation);
    // then
    assertThat(result).isTrue();
  }

  @Test
  void checkVariableIntegrityViolation() {
    // when
    boolean none = ExceptionUtil.checkVariableIntegrityViolation(new PersistenceException(mock(SQLException.class)));
    // then
    assertThat(none).isFalse();

    // given
    SQLException integrityViolation = mock(SQLException.class);
    doReturn("act_uniq_variable").when(integrityViolation).getMessage();
    doReturn("23505").when(integrityViolation).getSQLState();

    // when
    boolean result = ExceptionUtil.checkVariableIntegrityViolation(new PersistenceException(integrityViolation));
    // then
    assertThat(result).isTrue();
  }

  @Test
  void checkDeadlockException() {
    // when
    boolean none = ExceptionUtil.checkDeadlockException(mock(SQLException.class));
    // then
    assertThat(none).isFalse();

    // given
    SQLException deadlock = mock(SQLException.class);
    doReturn("40P01").when(deadlock).getSQLState();  // PostgreSQL

    // when
    boolean result = ExceptionUtil.checkDeadlockException(deadlock);
    // then
    assertThat(result).isTrue();
  }

}
