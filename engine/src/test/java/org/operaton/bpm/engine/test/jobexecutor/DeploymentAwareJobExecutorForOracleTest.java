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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class DeploymentAwareJobExecutorForOracleTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> configuration.setJobExecutorDeploymentAware(true))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Test
  void testFindAcquirableJobsWhen0InstancesDeployed() {
    // given
    Assumptions.assumeTrue(engineRule.getProcessEngineConfiguration().getDatabaseType().equals("oracle"));

    // then
    findAcquirableJobs();
  }

  @Test
  void testFindAcquirableJobsWhen1InstanceDeployed() {
    // given
    Assumptions.assumeTrue(engineRule.getProcessEngineConfiguration().getDatabaseType().equals("oracle"));
    // when
    testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    // then
    findAcquirableJobs();
  }

  @Test
  void testFindAcquirableJobsWhen1000InstancesDeployed() {
    // given
    Assumptions.assumeTrue(engineRule.getProcessEngineConfiguration().getDatabaseType().equals("oracle"));
    // when
    for (int i=0; i<1000; i++) {
      testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    }
    // then
    findAcquirableJobs();
  }

  @Test
  void testFindAcquirableJobsWhen1001InstancesDeployed() {
    // given
    Assumptions.assumeTrue(engineRule.getProcessEngineConfiguration().getDatabaseType().equals("oracle"));
    // when
    for (int i=0; i<1001; i++) {
      testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    }
    // then
    findAcquirableJobs();
  }

  @Test
  void testFindAcquirableJobsWhen2000InstancesDeployed() {
    // given
    Assumptions.assumeTrue(engineRule.getProcessEngineConfiguration().getDatabaseType().equals("oracle"));
    // when
    for (int i=0; i<2000; i++) {
      testRule.deploy(ProcessModels.ONE_TASK_PROCESS);
    }
    // then
    findAcquirableJobs();
  }

  protected List<AcquirableJobEntity> findAcquirableJobs() {
    return engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> commandContext
        .getJobManager()
        .findNextJobsToExecute(new Page(0, 100)));
  }
}
