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
package org.operaton.bpm.spring.boot.starter.actuator;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.spring.boot.starter.actuator.JobExecutorHealthIndicator.Details;

import static org.operaton.bpm.engine.test.util.ProcessEngineUtils.newRandomProcessEngineName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutorHealthIndicatorTest {

  private static final String LOCK_OWNER = "lockowner";
  private static final int LOCK_TIME_IN_MILLIS = 5;
  private static final int MAX_JOBS_PER_ACQUISITION = 6;
  private static final String JOB_EXECUTOR_NAME = "job executor name";
  private static final int WAIT_TIME_IN_MILLIS = 7;
  private static final List<ProcessEngineImpl> PROCESS_ENGINES = new ArrayList<>();
  private static final String PROCESS_ENGINE_NAME = newRandomProcessEngineName();

  static {
    ProcessEngineImpl processEngineImpl = mock(ProcessEngineImpl.class);
    when(processEngineImpl.getName()).thenReturn(PROCESS_ENGINE_NAME);
    PROCESS_ENGINES.add(processEngineImpl);
  }

  @Mock
  private JobExecutor jobExecutor;

  @BeforeEach
  void init() {
    lenient().when(jobExecutor.getLockOwner()).thenReturn(LOCK_OWNER);
    lenient().when(jobExecutor.getLockTimeInMillis()).thenReturn(LOCK_TIME_IN_MILLIS);
    lenient().when(jobExecutor.getMaxJobsPerAcquisition()).thenReturn(MAX_JOBS_PER_ACQUISITION);
    lenient().when(jobExecutor.getName()).thenReturn(JOB_EXECUTOR_NAME);
    lenient().when(jobExecutor.getWaitTimeInMillis()).thenReturn(WAIT_TIME_IN_MILLIS);
    lenient().when(jobExecutor.getProcessEngines()).thenReturn(PROCESS_ENGINES);
  }

  @Test
  void nullTest() {
    assertThatNullPointerException().isThrownBy(() -> new JobExecutorHealthIndicator(null));
  }

  @Test
  void upTest() {
    when(jobExecutor.isActive()).thenReturn(true);
    JobExecutorHealthIndicator indicator = new JobExecutorHealthIndicator(jobExecutor);
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertDetails(health);
  }

  @Test
  void downTest() {
    when(jobExecutor.isActive()).thenReturn(false);
    JobExecutorHealthIndicator indicator = new JobExecutorHealthIndicator(jobExecutor);
    Health health = indicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertDetails(health);
  }

  private void assertDetails(Health health) {
    Details details = (Details) health.getDetails().get("jobExecutor");
    assertThat(details.getLockOwner()).isEqualTo(LOCK_OWNER);
    assertThat(details.getLockTimeInMillis()).isEqualTo(LOCK_TIME_IN_MILLIS);
    assertThat(details.getMaxJobsPerAcquisition()).isEqualTo(MAX_JOBS_PER_ACQUISITION);
    assertThat(details.getName()).isEqualTo(JOB_EXECUTOR_NAME);
    assertThat(details.getWaitTimeInMillis()).isEqualTo(WAIT_TIME_IN_MILLIS);
    assertThat(details.getProcessEngineNames()).hasSameSizeAs(PROCESS_ENGINES);
    assertThat(details.getProcessEngineNames().iterator().next()).isEqualTo(PROCESS_ENGINE_NAME);
  }
}
