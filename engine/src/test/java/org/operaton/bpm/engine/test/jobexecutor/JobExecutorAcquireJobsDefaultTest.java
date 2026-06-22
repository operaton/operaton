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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.util.ClockTestUtil;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class JobExecutorAcquireJobsDefaultTest extends AbstractJobExecutorAcquireJobsTest {

  @Parameter(0)
  public boolean ensureJobDueDateSet;

  @Parameter(1)
  public Date currentTime;

  @Parameters(name = "Job DueDate is set: {0}")
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
      { false, null },
      { true, ClockTestUtil.setClockToDateWithoutMilliseconds() }
    });
  }

  @BeforeEach
  void setUp() {
    rule.getProcessEngineConfiguration().setEnsureJobDueDateNotNull(ensureJobDueDateSet);
  }

  @TestTemplate
  void testProcessEngineConfiguration() {
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isFalse();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isFalse();
    assertThat(configuration.isEnsureJobDueDateNotNull()).isEqualTo(ensureJobDueDateSet);
  }

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  void testJobDueDateValue() {
    // when
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");
    List<AcquirableJobEntity> jobList = findAcquirableJobs();

    // then
    assertThat(jobList).hasSize(1);
    assertThat(jobList.get(0).getDuedate()).isEqualTo(currentTime);
  }
}
