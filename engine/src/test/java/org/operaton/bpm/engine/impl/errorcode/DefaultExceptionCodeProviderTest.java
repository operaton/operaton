/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.errorcode;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.commons.utils.ServiceLoaderUtil;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class DefaultExceptionCodeProviderTest {
  /** @see BuiltinExceptionCode#OPTIMISTIC_LOCKING */
  private static final int CODE_OPTIMISTIC_LOCKING = 1;
  /** @see BuiltinExceptionCode#DEADLOCK */
  private static final int CODE_DEADLOCK = 10_000;
  /** @see BuiltinExceptionCode#FOREIGN_KEY_CONSTRAINT_VIOLATION */
  private static final int CODE_FOREIGN_KEY_CONSTRAINT_VIOLATION = 10_001;
  /** @see BuiltinExceptionCode#COLUMN_SIZE_TOO_SMALL */
  private static final int CODE_COLUMN_SIZE_TOO_SMALL = 10_002;

  DefaultExceptionCodeProvider exceptionCodeProvider = new DefaultExceptionCodeProvider();

  @Test
  void canBeLoadedViaServiceLoader () {
    assertThat(ServiceLoaderUtil.loadSingleService(ExceptionCodeProvider.class)).isInstanceOf(DefaultExceptionCodeProvider.class);
  }

  @Test
  void provideCode_returnsOptimisticLockingCode_whenCalledWithOptimisticLockingException () {
    assertThat(exceptionCodeProvider.provideCode(mock(OptimisticLockingException.class))).isEqualTo(CODE_OPTIMISTIC_LOCKING);
  }

  @Test
  void provideCode_returnsNull_whenCalledWithProcessEngineException () {
    assertThat(exceptionCodeProvider.provideCode(mock(ProcessEngineException.class))).isNull();
  }

  @Test
  void provideCode_returnsDeadlockCode_whenDetected () {
    // given
    try (var exceptionUtil = mockStatic(ExceptionUtil.class)) {
      exceptionUtil.when(() -> ExceptionUtil.checkDeadlockException(any())).thenReturn(true);
      var sqlException = mock(SQLException.class);

      // when
      Integer code = exceptionCodeProvider.provideCode(sqlException);

      // then
      assertThat(code).isEqualTo(CODE_DEADLOCK);
    }
  }

  @Test
  void provideCode_returnsForeignKeyViolationCode_whenDetected () {
    // given
    try (var exceptionUtil = mockStatic(ExceptionUtil.class)) {
      exceptionUtil.when(() -> ExceptionUtil.checkForeignKeyConstraintViolation(any(SQLException.class))).thenReturn(true);
      var sqlException = mock(SQLException.class);

      // when
      Integer code = exceptionCodeProvider.provideCode(sqlException);

      // then
      assertThat(code).isEqualTo(CODE_FOREIGN_KEY_CONSTRAINT_VIOLATION);
    }
  }

  @Test
  void provideCode_returnsColumnSizeTooSmallCode_whenValueTooLongDetected () {
    // given
    try (var exceptionUtil = mockStatic(ExceptionUtil.class)) {
      exceptionUtil.when(() -> ExceptionUtil.checkValueTooLongException(any(SQLException.class))).thenReturn(true);
      var sqlException = mock(SQLException.class);

      // when
      Integer code = exceptionCodeProvider.provideCode(sqlException);

      // then
      assertThat(code).isEqualTo(CODE_COLUMN_SIZE_TOO_SMALL);
    }
  }

  @Test
  void provideCode_returnsNull_whenOtherSQLException () {
    // given
    var sqlException = mock(SQLException.class);

    // when
    Integer code = exceptionCodeProvider.provideCode(sqlException);

    // then
    assertThat(code).isNull();
  }
}
