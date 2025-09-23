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
package org.operaton.bpm.engine.impl.test;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class TestHelperTest {

  @Test
  void shouldGetPublicMethod() throws Exception {
    // WHEN we call get method to retrieve a method with public accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeTestClass.class, "testSomethingWithPublicAccessor", new Class[0]);
    assertThat(methodName).hasToString("public void org.operaton.bpm.engine.impl.test" +
            ".TestHelperTest$SomeTestClass.testSomethingWithPublicAccessor()");
  }

  @Test
  void shouldGetPublicMethodFromSuperClass() throws Exception {
    // WHEN we call get method to retrieve a method with public accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeOtherTestClass.class, "testSomethingWithPublicAccessor", new Class[0]);
    assertThat(methodName).hasToString("public void org.operaton.bpm.engine.impl.test.TestHelperTest$SomeTestClass.testSomethingWithPublicAccessor()");
  }

  @Test
  void shouldGetPackagePrivateMethod() throws Exception {
    // WHEN we call get method to retrieve a method with package private accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeTestClass.class, "testSomethingWithPackagePrivateAccessor", new Class[0]);
    assertThat(methodName).hasToString("void org.operaton.bpm.engine.impl.test.TestHelperTest$SomeTestClass.testSomethingWithPackagePrivateAccessor()");
  }

  @Test
  void shouldGetPackagePrivateMethodFromSuperClass() throws Exception {
    // WHEN we call get method to retrieve a method with package private accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeOtherTestClass.class, "testSomethingWithPackagePrivateAccessor", new Class[0]);
    assertThat(methodName).hasToString("void org.operaton.bpm.engine.impl.test.TestHelperTest$SomeTestClass.testSomethingWithPackagePrivateAccessor()");
  }

  @Test
  void shouldGetProtectedMethod() throws Exception {
    // WHEN we call get method to retrieve a method with protected accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeTestClass.class, "testSomethingWithProtected", new Class[0]);
    assertThat(methodName).hasToString("protected void org.operaton.bpm.engine.impl.test.TestHelperTest$SomeTestClass.testSomethingWithProtected()");
  }

  @Test
  void shouldGetProtectedMethodFromSuperClass() throws Exception {
    // WHEN we call get method to retrieve a method with protected accessor, no exception should be thrown
    Object methodName = TestHelper.getMethod(SomeOtherTestClass.class, "testSomethingWithProtected", new Class[0]);
    assertThat(methodName).hasToString("protected void org.operaton.bpm.engine.impl.test.TestHelperTest$SomeTestClass.testSomethingWithProtected()");
  }

  static class SomeTestClass {

    public void testSomethingWithPublicAccessor() {
      // no-op
    }

    void testSomethingWithPackagePrivateAccessor() {
      // no-op
    }

    protected void testSomethingWithProtected() {
      // no-op
    }

  }

  static class SomeOtherTestClass extends SomeTestClass {
  }

  @Test
  void shouldIgnoreExceptionDuringJobExecution() {
    ManagementService managementService = mock(ManagementService.class);
    doThrow(ProcessEngineException.class).when(managementService).executeJob("aJobId");
    assertThatCode(() -> TestHelper.executeJobIgnoringException(managementService, "aJobId"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldExpectExceptionDuringJobExecutionWhenExceptionIsThrown() {
    ManagementService managementService = mock(ManagementService.class);
    doThrow(new ProcessEngineException("some exception message")).when(managementService).executeJob("aJobId");
    assertThatCode(() -> TestHelper.executeJobExpectingException(managementService, "aJobId"))
        .doesNotThrowAnyException();
    assertThatCode(() -> TestHelper.executeJobExpectingException(managementService, "aJobId", "some exception message"))
        .doesNotThrowAnyException();
    assertThatCode(() -> TestHelper.executeJobExpectingException(managementService, "aJobId", "exception message"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldExpectExceptionDuringJobExecutionWhenExceptionIsNotThrown() {
    ManagementService managementService = mock(ManagementService.class);
    doNothing().when(managementService).executeJob("aJobId");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> TestHelper.executeJobExpectingException(managementService, "aJobId"));
  }

  @Test
  void shouldNotExpectExceptionDuringJobExecutionWhenExceptionIsThrown() {
    ManagementService managementService = mock(ManagementService.class);
    doThrow(ProcessEngineException.class).when(managementService).executeJob("aJobId");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> TestHelper.executeJobNotExpectingException(managementService, "aJobId"));
  }

  @Test
  void shouldNotExpectExceptionDuringJobExecutionWhenExceptionIsNotThrown() {
    ManagementService managementService = mock(ManagementService.class);
    doNothing().when(managementService).executeJob("aJobId");
    assertThatCode(() -> TestHelper.executeJobNotExpectingException(managementService, "aJobId"))
        .doesNotThrowAnyException();
  }

}
