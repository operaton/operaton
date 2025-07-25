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
package org.operaton.bpm.integrationtest.functional.jpa;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;


/**
 * <p>Checks that activiti / application transaction sharing works as expected</p>
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class JPAIntegrationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(SomeEntity.class)
      .addClass(PersistenceDelegateBean.class)
      .addClass(AsyncPersistenceDelegateBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/jpa/TransactionIntegrationTest.testDelegateParticipateInApplicationTx.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/jpa/TransactionIntegrationTest.testAsyncDelegateNewTx.bpmn20.xml")
      .addAsWebInfResource("persistence.xml", "classes/META-INF/persistence.xml");
  }

  @Inject
  private UserTransaction utx;

  @Inject
  private RuntimeService runtimeService;

  @PersistenceContext
  private EntityManager entityManager;

  @Inject
  private PersistenceDelegateBean persistenceDelegateBean;

  @Inject
  private AsyncPersistenceDelegateBean asyncPersistenceDelegateBean;

  @Test
  void testDelegateParticipateInApplicationTx() throws Exception {

    /* if we start a transaction here, persist an entity and then
     * start a process instance which synchronously invokes a java delegate,
     * that delegate is invoked in the same transaction and thus has access to
     * the same entity manager.
     */

    try {
      utx.begin();

      SomeEntity e = new SomeEntity();
      entityManager.persist(e);

      persistenceDelegateBean.setEntity(e);

      runtimeService.startProcessInstanceByKey("testDelegateParticipateInApplicationTx");

      utx.commit();
    }catch (Exception e) {
      utx.rollback();
      throw e;
    }
  }


  @Test
  void testAsyncDelegateNewTx() throws Exception {

    /* if we start a transaction here, persist an entity and then
     * start a process instance which asynchronously invokes a java delegate,
     * that delegate is invoked in a new transaction and thus does not have access to
     * the same entity manager.
     */

    try {
      utx.begin();

      SomeEntity e = new SomeEntity();
      entityManager.persist(e);

      asyncPersistenceDelegateBean.setEntity(e);

      runtimeService.startProcessInstanceByKey("testAsyncDelegateNewTx");

      utx.commit();

    }catch (Exception e) {
      utx.rollback();
      throw e;
    }

    waitForJobExecutorToProcessAllJobs();

  }
}
