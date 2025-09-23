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
package org.operaton.bpm.integrationtest.functional.transactions;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.transactions.beans.FailingDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * <p>Checks that activiti / application transaction sharing works as expected</p>
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class TransactionIntegrationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(FailingDelegate.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/TransactionIntegrationTest.testProcessFailure.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/TransactionIntegrationTest.testApplicationFailure.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/TransactionIntegrationTest.testTxSuccess.bpmn20.xml")
      .addAsWebInfResource("persistence.xml", "classes/META-INF/persistence.xml");
  }

  @Inject
  private UserTransaction utx;

  @Inject
  private RuntimeService runtimeService;

  @Test
  void testProcessFailure() throws Exception {

    /* if we start a transaction here and then start
     * a process instance which synchronously invokes a java delegate,
     * if that delegate fails, the transaction is marked rollback only
     */

    try {
      utx.begin();

      try {
        runtimeService.startProcessInstanceByKey("testProcessFailure");
        fail("Exception expected");
      }catch (Exception ex) {
        if(!(ex instanceof RuntimeException)) {
          fail("Wrong exception of type " + ex + " RuntimeException expected!");
        }
        if(!ex.getMessage().contains("I'm a complete failure!")) {
          fail("Different message expected");
        }
      }

      // assert that now our transaction is marked rollback-only:
      assertThat(utx.getStatus()).isEqualTo(Status.STATUS_MARKED_ROLLBACK);

    } finally {
      // make sure we always rollback
      utx.rollback();
    }
  }

  @Test
  void testApplicationFailure() throws Exception {

    /* if we start a transaction here and then successfully start
     * a process instance, if our transaction is rolled back,
     * the process instnace is not persisted.
     */

    try {
      utx.begin();

      String id = runtimeService.startProcessInstanceByKey("testApplicationFailure").getId();

      // assert that the transaction is in good shape:
      assertThat(utx.getStatus()).isEqualTo(Status.STATUS_ACTIVE);

      // now rollback the transaction (simmulating an application failure after the process engine is done).
      utx.rollback();

      utx.begin();

      // the process instance does not exist:
      ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(id)
        .singleResult();

      assertThat(processInstance).isNull();

      utx.commit();
    }catch (Exception e) {
      utx.rollback();
      throw e;
    }
  }


  @Test
  void testTxSuccess() throws Exception {

    try {
      utx.begin();

      String id = runtimeService.startProcessInstanceByKey("testTxSuccess").getId();

      // assert that the transaction is in good shape:
      assertThat(utx.getStatus()).isEqualTo(Status.STATUS_ACTIVE);

      // the process instance is visible form our tx:
      ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(id)
        .singleResult();

      assertThat(processInstance).isNotNull();

      utx.commit();

      utx.begin();

      // the process instance is visible in a new tx:
      processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(id)
        .singleResult();

      assertThat(processInstance).isNotNull();

      utx.commit();
    }catch (Exception e) {
      utx.rollback();
      throw e;
    }
  }
}
