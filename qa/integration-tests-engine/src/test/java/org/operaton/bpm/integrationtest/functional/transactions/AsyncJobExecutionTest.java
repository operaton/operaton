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

import java.util.HashMap;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.transactions.beans.FailingTransactionListenerDelegate;
import org.operaton.bpm.integrationtest.functional.transactions.beans.GetVersionInfoDelegate;
import org.operaton.bpm.integrationtest.functional.transactions.beans.UpdateRouterConfiguration;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(ArquillianExtension.class)
public class AsyncJobExecutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addClass(GetVersionInfoDelegate.class)
            .addClass(UpdateRouterConfiguration.class)
            .addClass(FailingTransactionListenerDelegate.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/AsyncJobExecutionTest.testAsyncServiceTasks.bpmn20.xml")
            .addAsResource("org/operaton/bpm/integrationtest/functional/transactions/AsyncJobExecutionTest.failingTransactionListener.bpmn20.xml")
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
  void shouldExecuteAsyncServiceTasks() {
    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("serialnumber", "23");
    runtimeService.startProcessInstanceByKey("configure-router", variables);

    // when
    // all jobs are executed
    assertThatCode(() -> waitForJobExecutorToProcessAllJobs()).doesNotThrowAnyException();

    // then
    // there are no failures
  }

  @Test
  void shouldFailJobWithFailingTransactionListener() {
    // given
    runtimeService.startProcessInstanceByKey("failingTransactionListener");

    // when
    waitForJobExecutorToProcessAllJobs(10000);

    // then
    // the job exists with no retries, and an incident is raised
    Job job = managementService.createJobQuery().processDefinitionKey("failingTransactionListener").singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
    assertThat(job.getExceptionMessage()).isNotNull();
    assertThat(managementService.getJobExceptionStacktrace(job.getId())).isNotNull();
  }

}
