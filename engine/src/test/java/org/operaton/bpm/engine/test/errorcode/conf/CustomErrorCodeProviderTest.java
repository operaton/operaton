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
package org.operaton.bpm.engine.test.errorcode.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.SQLException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.errorcode.BuiltinExceptionCode;
import org.operaton.bpm.engine.impl.errorcode.ExceptionCodeProvider;
import org.operaton.bpm.engine.test.errorcode.FailingJavaDelegateWithCustomException;
import org.operaton.bpm.engine.test.errorcode.FailingJavaDelegateWithErrorCode;
import org.operaton.bpm.engine.test.errorcode.FailingJavaDelegateWithOleAndErrorCode;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import ch.qos.logback.classic.Level;

class CustomErrorCodeProviderTest {

  protected static final int PROVIDED_CUSTOM_CODE = 33_333;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(c -> {
      c.setCustomExceptionCodeProvider(new ExceptionCodeProvider() {
        
        @Override
        public Integer provideCode(SQLException sqlException) {
          return PROVIDED_CUSTOM_CODE;
        }
        
        @Override
        public Integer provideCode(ProcessEngineException processEngineException) {
          return PROVIDED_CUSTOM_CODE;
        }
        
      });
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
      .watch("org.operaton.bpm.engine.cmd")
      .level(Level.WARN);

  RuntimeService runtimeService;
  IdentityService identityService;

  @AfterEach
  void clear() {
    engineRule.getIdentityService().deleteUser("kermit");
  }

  @Test
  void shouldOverrideProvidedExceptionCode1() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    Throwable exception = catchThrowable(() -> runtimeService.startProcessInstanceByKey("foo",
        Variables.putValue("code", 22_222)));

    // then
    assertThat(((ProcessEngineException) exception).getCode()).isEqualTo(22_222);
  }

  @Test
  void shouldOverrideProvidedExceptionCode2() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    Throwable exception = catchThrowable(() -> runtimeService.startProcessInstanceByKey("foo",
        Variables.putValue("code", 20_000)));

    // then
    assertThat(((ProcessEngineException) exception).getCode()).isEqualTo(20_000);
  }

  @Test
  void shouldOverrideProvidedExceptionCode3() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    Throwable exception = catchThrowable(() -> runtimeService.startProcessInstanceByKey("foo",
        Variables.putValue("code", 39_999)));

    // then
    assertThat(((ProcessEngineException) exception).getCode()).isEqualTo(39_999);
  }

  /**
   * This situation cannot happen right now since OLE are not thrown within delegation code
   * but when the process transaction is flushed. However, with this test case we ensure
   * that the built-in code provider has precedence over a code that was assigned via delegation
   * code. This ensures consistent behavior when we add non-SQL exception related built-in codes in the future.
   */
  @Test
  void shouldOverrideCodeFromDelegationCodeWithBuiltinCode() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithOleAndErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 40_000));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.OPTIMISTIC_LOCKING.getCode());
    assertThat(loggingRule.getLog().get(0).getMessage())
        .contains("Falling back to built-in code");
  }

  @Test
  void shouldOverrideCodeZeroWithCustomCode() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 0));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(PROVIDED_CUSTOM_CODE);
  }

  @Test
  void shouldResetReservedCodeFromDelegationCode1() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 1));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.FALLBACK.getCode());
    assertThat(loggingRule.getLog().get(0).getMessage())
        .contains("Falling back to default error code 0.");
  }

  @Test
  void shouldResetReservedCodeFromDelegationCode2() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 19_999));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.FALLBACK.getCode());
    assertThat(loggingRule.getLog().get(0).getMessage())
        .contains("Falling back to default error code 0.");
  }

  @Test
  void shouldResetReservedCodeFromDelegationCode3() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithErrorCode.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 40_000));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.FALLBACK.getCode());
    assertThat(loggingRule.getLog().get(0).getMessage())
        .contains("Falling back to default error code 0.");
  }

  @Test
  void shouldProvideCustomCodeFromDelegationCodeWithCustomException() {
    // given
    BpmnModelInstance myProcess = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .serviceTask()
          .operatonClass(FailingJavaDelegateWithCustomException.class)
        .endEvent()
        .done();

    testRule.deploy(myProcess);

    // when
    ThrowingCallable callable =
        () -> runtimeService.startProcessInstanceByKey("foo",
            Variables.putValue("code", 22_222));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(22_222);
  }

  @Test
  void shouldHaveSubordinationToBuiltinCode() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done();

    testRule.deploy(modelInstance);

    String businessKey = generateString(1_000);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process", businessKey))
        .isInstanceOf(ProcessEngineException.class)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.COLUMN_SIZE_TOO_SMALL.getCode());
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  protected String generateString(int size) {
    return new String(new char[size]).replace('\0', 'a');
  }

}
