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
import java.util.Locale;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    String expectedStackTrace = "java.lang.Throwable" + System.lineSeparator()
        + "\tat ExceptionUtil.getExceptionStackTrace(ExceptionUtil.java:55)" + System.lineSeparator();
    assertThat(stackTrace).isEqualTo(expectedStackTrace);
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

  // Messages in German: checks must not need English text
  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', textBlock = """
      Oracle insert   | ORA-02291: Integritäts-Constraint (TEST.FK) verletzt - übergeordneter Schlüssel nicht gefunden | 23000 | 2291
      Oracle delete   | ORA-02292: Integritäts-Constraint (TEST.FK) verletzt - untergeordneter Datensatz gefunden     | 23000 | 2292
      SqlServer       | Die DELETE-Anweisung steht in Konflikt mit der REFERENCE-Einschränkung                         | 23000 | 547
      MySql insert    | Kann Kind-Zeile nicht hinzufügen oder aktualisieren                                            | 23000 | 1452
      MySql delete    | Kann Eltern-Zeile nicht löschen oder aktualisieren                                             | 23000 | 1451
      PostgreSQL      | Aktualisieren oder Löschen in Tabelle verletzt Fremdschlüssel-Constraint                       | 23503 | 0
      H2 parent       | Referentielle Integrität verletzt                                                              | 23506 | 23506
      H2 child        | Referentielle Integrität verletzt                                                              | 23503 | 23503
      DB2             | Fremdschlüsselwert ungültig                                                                    | 23503 | -530
      """)
  void shouldDetectForeignKeyViolationByCode(String database, String message, String sqlState, int errorCode) {
    SQLException exception = new SQLException(message, sqlState, errorCode);

    assertThat(ExceptionUtil.checkForeignKeyConstraintViolation(exception)).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', textBlock = """
      Oracle check     | ORA-02290: CHECK-Constraint (TEST.CK) verletzt                  | 23000 | 2290
      Oracle unique    | ORA-00001: Unique Constraint (TEST.PK) verletzt                 | 23000 | 1
      SqlServer unique | Verletzung der PRIMARY KEY-Einschränkung                        | 23000 | 2627
      PostgreSQL       | doppelter Schlüsselwert verletzt Unique-Constraint              | 23505 | 0
      """)
  void shouldNotDetectOtherViolationAsForeignKey(String database, String message, String sqlState, int errorCode) {
    SQLException exception = new SQLException(message, sqlState, errorCode);

    assertThat(ExceptionUtil.checkForeignKeyConstraintViolation(exception)).isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', textBlock = """
      Oracle 12899 | ORA-12899: Wert zu groß für Spalte "TEST"."ACT_HI_PROCINST"."BUSINESS_KEY_" | 72000 | 12899
      Oracle 1461  | ORA-01461: Ein LONG-Wert kann nur zur Einfügung in eine LONG-Spalte gebunden werden | 72000 | 1461
      SqlServer    | Zeichenfolgen- oder Binärdaten würden abgeschnitten                          | 22001 | 2628
      PostgreSQL   | Wert zu lang für Typ character varying(255)                                  | 22001 | 0
      MySql        | Daten zu lang für Spalte                                                     | 22001 | 1406
      """)
  void shouldDetectValueTooLongByCode(String database, String message, String sqlState, int errorCode) {
    SQLException exception = new SQLException(message, sqlState, errorCode);

    assertThat(ExceptionUtil.checkValueTooLongException(exception)).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', textBlock = """
      SqlServer  | Verletzung der UNIQUE KEY-Einschränkung | 23000 | 2627
      Oracle     | ORA-00001: Unique Constraint verletzt   | 23000 | 1
      PostgreSQL | doppelter Schlüsselwert                 | 23505 | 0
      """)
  void shouldDetectConstraintViolationByCode(String database, String message, String sqlState, int errorCode) {
    SQLException exception = new SQLException(message, sqlState, errorCode);

    assertThat(ExceptionUtil.checkConstraintViolationException(
        new ProcessEngineException(new PersistenceException(exception)))).isTrue();
  }

  @Test
  void shouldDetectByCodeWithoutMessage() {
    assertThat(ExceptionUtil.checkForeignKeyConstraintViolation(new SQLException(null, "23000", 2292))).isTrue();
    assertThat(ExceptionUtil.checkValueTooLongException(new SQLException(null, "72000", 12899))).isTrue();
  }

  @Test
  void shouldMatchMessageWithTurkishLocale() {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      SQLException exception = new SQLException("Referential INTEGRITY CONSTRAINT violation");

      assertThat(ExceptionUtil.checkForeignKeyConstraintViolation(exception)).isTrue();
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

}
