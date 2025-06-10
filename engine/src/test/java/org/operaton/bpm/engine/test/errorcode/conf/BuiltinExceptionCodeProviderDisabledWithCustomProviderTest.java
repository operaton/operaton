/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.errorcode.conf;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.errorcode.ExceptionCodeProvider;
import org.operaton.bpm.engine.test.errorcode.FailingJavaDelegateWithErrorCode;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class BuiltinExceptionCodeProviderDisabledWithCustomProviderTest {

  protected static final int PROVIDED_CUSTOM_CODE = 888_888;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(c -> {
      c.setDisableBuiltinExceptionCodeProvider(true);
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

  ProcessEngineConfigurationImpl engineConfig;
  RuntimeService runtimeService;
  IdentityService identityService;

  @AfterEach
  void clear() {
    engineRule.getIdentityService().deleteUser("kermit");
  }

  @Test
  void shouldOverrideBuiltinCodeColumnSizeTooSmall() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done();

    testRule.deploy(modelInstance);

    String businessKey = generateString(1_000);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process", businessKey))
        .extracting("code")
        .isEqualTo(PROVIDED_CUSTOM_CODE);
  }

  @Test
  void shouldOverrideBuiltinCodeOptimisticLockingException() {
    // given
    User user = identityService.newUser("kermit");
    identityService.saveUser(user);

    User user1 = identityService.createUserQuery().singleResult();
    User user2 = identityService.createUserQuery().singleResult();

    user1.setFirstName("name one");
    identityService.saveUser(user1);

    user2.setFirstName("name two");

    // when/then
    assertThatThrownBy(() -> identityService.saveUser(user2))
        .extracting("code")
        .isEqualTo(PROVIDED_CUSTOM_CODE);
  }

  @Test
  void shouldOverrideProvidedExceptionCodeFromDelegationCode() {
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
            Variables.putValue("code", 999_999));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(999_999);
  }

  @Test
  void shouldOverrideProvidedExceptionCodeFromDelegationCodeAndAllowOverridingReservedCode() {
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
            Variables.putValue("code", 1000));

    // then
    assertThatThrownBy(callable)
        .extracting("code")
        .isEqualTo(1000);
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  protected String generateString(int size) {
    return new String(new char[size]).replace('\0', 'a');
  }

}
