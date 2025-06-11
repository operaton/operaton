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
package org.operaton.bpm.engine.test.errorcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.authorization.TaskPermissions;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.errorcode.BuiltinExceptionCode;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import ch.qos.logback.classic.Level;

class ExceptionBuiltinCodesTest {

  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
      .watch("org.operaton.bpm.engine.cmd")
      .level(Level.WARN);

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  IdentityService identityService;
  AuthorizationService authorizationService;

  @AfterEach
  void clear() {
    engineRule.getIdentityService().deleteUser("kermit");
    engineRule.getAuthorizationService().createAuthorizationQuery().list()
        .forEach(authorization -> authorizationService.deleteAuthorization(authorization.getId()));
    engineRule.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey("sub-process").list()
        .forEach(pi -> runtimeService.deleteProcessInstance(pi.getProcessInstanceId(), ""));
  }

  @Test
  void shouldHaveColumnSizeTooSmallErrorCode() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done();

    engineTestRule.deploy(modelInstance);

    String businessKey = generateString(1_000);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process", businessKey))
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.COLUMN_SIZE_TOO_SMALL.getCode());
  }

  @Test
  void shouldHaveDefaultErrorCodeUniqueKeyConstraintPersistenceExceptionNotCovered() {
    // given
    Authorization authorizationOne = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorizationOne.setGroupId("aUserId");
    authorizationOne.setPermissions(new Permission[] { TaskPermissions.READ });
    authorizationOne.setResourceId("foo");
    authorizationOne.setResource(Resources.TASK);

    authorizationService.saveAuthorization(authorizationOne);

    Authorization authorizationTwo = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorizationTwo.setGroupId("aUserId");
    authorizationTwo.setPermissions(new Permission[]{TaskPermissions.READ});
    authorizationTwo.setResourceId("foo");
    authorizationTwo.setResource(Resources.TASK);

    // when/then
    assertThatThrownBy(() -> authorizationService.saveAuthorization(authorizationTwo))
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.FALLBACK.getCode());
    assertThat(loggingRule.getLog()).isEmpty();
  }

  @Test
  void shouldHaveOleErrorCode() {
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
        .isInstanceOf(OptimisticLockingException.class)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.OPTIMISTIC_LOCKING.getCode());
  }

  @Test
  void shouldHaveForeignKeyConstraintViolationCode() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("calling")
        .startEvent()
        .callActivity()
          .calledElement("called")
          .operatonInBusinessKey("sub-process")
        .endEvent()
        .done();

    engineTestRule.deploy(modelInstance);

    modelInstance = Bpmn.createExecutableProcess("called")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    engineTestRule.deploy(modelInstance);

    String processInstanceId = runtimeService.startProcessInstanceByKey("calling").getId();

    List<Execution> executions = runtimeService.createExecutionQuery().list();
    executions.forEach((execution -> {
      ((ExecutionEntity) execution).setCachedEntityState(0);

      engineRule.getProcessEngineConfiguration()
          .getCommandExecutorTxRequired()
          .execute((Command<Void>) commandContext -> {
            commandContext.getDbEntityManager().merge(((ExecutionEntity) execution));
            return null;
          });
    }));

    assertThatThrownBy(() -> runtimeService.deleteProcessInstance(processInstanceId, ""))
        .isInstanceOf(ProcessEngineException.class)
        .extracting("code")
        .isEqualTo(BuiltinExceptionCode.FOREIGN_KEY_CONSTRAINT_VIOLATION.getCode());
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  protected String generateString(int size) {
    return new String(new char[size]).replace('\0', 'a');
  }

}
