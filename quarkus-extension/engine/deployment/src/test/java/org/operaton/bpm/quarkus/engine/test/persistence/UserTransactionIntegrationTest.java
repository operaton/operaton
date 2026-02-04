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
package org.operaton.bpm.quarkus.engine.test.persistence;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.quarkus.engine.test.helper.ProcessEngineAwareExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTransactionIntegrationTest {

  @RegisterExtension
  static QuarkusUnitTest unitTest = new ProcessEngineAwareExtension()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

  @Inject
  protected UserTransaction userTransactionManager;

  @Inject
  protected RuntimeService runtimeService;

  @Test
  @Deployment
  void shouldSucceed() throws Exception {

    try {
      userTransactionManager.begin();

      String id = runtimeService.startProcessInstanceByKey("testTxSuccess").getId();

      // assert that the transaction is in good shape:
      assertThat(userTransactionManager.getStatus()).isEqualTo(Status.STATUS_ACTIVE);

      // the process instance is visible form our tx:
      ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
          .processInstanceId(id)
          .singleResult();

      assertThat(processInstance).isNotNull();

      userTransactionManager.commit();

      userTransactionManager.begin();

      // the process instance is visible in a new tx:
      processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();

      assertThat(processInstance).isNotNull();

      userTransactionManager.commit();
    } catch (Exception e) {
      userTransactionManager.rollback();
      throw e;
    }
  }

  @Test
  @Deployment
  void shouldMarkAsRollbackOnly() throws Exception {

    /* if we start a transaction here and then start
     * a process instance which synchronously invokes a java delegate,
     * if that delegate fails, the transaction is marked rollback only
     */

    try {
      userTransactionManager.begin();

      // when/then
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcessFailure"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("I'm a complete failure!");

      // assert that now our transaction is marked rollback-only:
      assertThat(userTransactionManager.getStatus()).isEqualTo(Status.STATUS_MARKED_ROLLBACK);

    } finally {
      // make sure we always roll back
      userTransactionManager.rollback();
    }
  }

  @Test
  @Deployment
  void shouldNotStoreProcessInstance() throws Exception {

    /* if we start a transaction here and then successfully start
     * a process instance, if our transaction is rolled back,
     * the process instance is not persisted.
     */

    try {
      userTransactionManager.begin();

      String id = runtimeService.startProcessInstanceByKey("testApplicationFailure").getId();

      // assert that the transaction is in good shape:
      assertThat(userTransactionManager.getStatus()).isEqualTo(Status.STATUS_ACTIVE);

      // now rollback the transaction (simulating an application failure after the process engine is done).
      userTransactionManager.rollback();

      userTransactionManager.begin();

      // the process instance does not exist:
      ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
          .processInstanceId(id)
          .singleResult();

      assertThat(processInstance).isNull();

      userTransactionManager.commit();
    } catch (Exception e) {
      userTransactionManager.rollback();
      throw e;
    }
  }

  @Named
  @Dependent
  public static class FailingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
      throw new RuntimeException("I'm a complete failure!");
    }

  }

}
