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
package org.operaton.bpm.integrationtest.jobexecutor;

import java.util.function.Supplier;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.integrationtest.jobexecutor.beans.FailingSLSB;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(ArquillianExtension.class)
public class FailedJobCommandTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return initWebArchiveDeployment()
      .addClass(FailingSLSB.class)
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/FailedJobCommandTest.bpmn20.xml");

  }

  @Test
  void testJobRetriesDecremented() {
    runtimeService.startProcessInstanceByKey("theProcess");
    Supplier<JobQuery> createQuery = () -> managementService.createJobQuery().processDefinitionKey("theProcess");

    assertThat(createQuery.get().withRetriesLeft().count()).isOne();

    waitForJobExecutorToProcessAllJobs();

    // now the retries = 0

    assertThat(createQuery.get().withRetriesLeft().count()).isZero();
    assertThat(createQuery.get().noRetriesLeft().count()).isOne();

  }

  @Test
  void testJobRetriesDecremented_multiple() {

    for(int i = 0; i < 50; i++) {
      runtimeService.startProcessInstanceByKey("theProcess");
    }
    Supplier<JobQuery> createQuery = () -> managementService.createJobQuery().processDefinitionKey("theProcess");

    assertThat(createQuery.get().withRetriesLeft().count()).isEqualTo(50);

    waitForJobExecutorToProcessAllJobs(6 * 60 * 1000);

    // now the retries = 0

    assertThat(createQuery.get().withRetriesLeft().count()).isZero();
    assertThat(createQuery.get().noRetriesLeft().count()).isEqualTo(51);

  }

}
