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
package org.operaton.bpm.engine.test.bpmn.exclusive;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 *
 * @author Daniel Meyer
 */
public class ExclusiveTaskTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testNonExclusiveService() {
    // start process
    runtimeService.startProcessInstanceByKey("exclusive");
    // now there should be 1 non-exclusive job in the database:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(((JobEntity) job).isExclusive()).isFalse();

    testRule.waitForJobExecutorToProcessAllJobs(6000L);

    // all the jobs are done
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testExclusiveService() {
    // start process
    runtimeService.startProcessInstanceByKey("exclusive");
    // now there should be 1 exclusive job in the database:
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    assertThat(((JobEntity) job).isExclusive()).isTrue();

    testRule.waitForJobExecutorToProcessAllJobs(6000L);

    // all the jobs are done
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testExclusiveServiceConcurrent() {
    // start process
    runtimeService.startProcessInstanceByKey("exclusive");
    // now there should be 3 exclusive jobs in the database:
    assertThat(managementService.createJobQuery().count()).isEqualTo(3);

    testRule.waitForJobExecutorToProcessAllJobs(6000L);

    // all the jobs are done
    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testExclusiveSequence2() {

    runtimeService.startProcessInstanceByKey("testProcess");

    testRule.waitForJobExecutorToProcessAllJobs(6000L);

    assertThat(managementService.createJobQuery().count()).isZero();
  }

  @Deployment
  @Test
  public void testExclusiveSequence3() {
    runtimeService.startProcessInstanceByKey("testProcess");

    testRule.waitForJobExecutorToProcessAllJobs(6000L);

    assertThat(managementService.createJobQuery().count()).isZero();
  }

}
