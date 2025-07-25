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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.transactions.beans.TransactionRollbackDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import jakarta.inject.Inject;

/**
 * This test class ensures that when a UserTransaction is explicitly marked as ROLLBACK_ONLY,
 * and this code is executed within a Job, then the transaction is rolled back, and the job
 * execution is marked as failed, reducing the job retries.
 */
@ExtendWith(ArquillianExtension.class)
public class AsyncJobExecutionWithRollbackTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addClass(TransactionRollbackDelegate.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/AsyncJobExecutionWithRollbackTest.transactionRollbackInServiceTask.bpmn20.xml")
            .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/AsyncJobExecutionWithRollbackTest.transactionRollbackInServiceTaskWithCustomRetryCycle.bpmn20.xml")
            .addAsWebInfResource("persistence.xml", "classes/META-INF/persistence.xml");
  }

  @Inject
  private RuntimeService runtimeService;

  @AfterEach
  void cleanUp() {
    for (ProcessInstance processInstance : runtimeService.createProcessInstanceQuery().list()) {
      runtimeService.deleteProcessInstance(processInstance.getId(), "test ended", true);
    }
  }

  @Test
  void shouldRollbackTransactionInServiceTask() {
    // given
    runtimeService.startProcessInstanceByKey("txRollbackServiceTask");

    // when
    // the job is executed
    waitForJobExecutorToProcessAllJobs(10000);

    // then
    // the job exists with no retries, and an incident is raised
    Job job = managementService.createJobQuery().processDefinitionKey("txRollbackServiceTask").singleResult();

    assertThat(job).isNotNull();
    assertEquals(0, job.getRetries());
    assertThat(job.getExceptionMessage()).isNotNull();
    assertThat(managementService.getJobExceptionStacktrace(job.getId())).isNotNull();
  }

  @Test
  void shouldRollbackTransactionInServiceTaskWithCustomRetryCycle() {
    // given
    runtimeService.startProcessInstanceByKey("txRollbackServiceTaskWithCustomRetryCycle");

    // when
    waitForJobExecutorToProcessAllJobs(10000);

    // then
    // the job exists with no retries, and an incident is raised
    Job job = managementService.createJobQuery().processDefinitionKey("txRollbackServiceTaskWithCustomRetryCycle").singleResult();

    assertThat(job).isNotNull();
    assertEquals(0, job.getRetries());
    assertThat(job.getExceptionMessage()).isNotNull();
    assertThat(managementService.getJobExceptionStacktrace(job.getId())).isNotNull();
  }

}
