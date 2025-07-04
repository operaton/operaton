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
package org.operaton.bpm.qa.rolling.update.timestamp;
import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikola Koevski
 */
@ScenarioUnderTest("JobTimestampsUpdateScenario")
@Origin("7.11.0")
@Parameterized
class JobTimestampsUpdateTest extends AbstractTimestampUpdateTest {

  protected static final long LOCK_DURATION = 300000L;
  protected static final Date LOCK_EXP_TIME = new Date(TIME + LOCK_DURATION);

  @ScenarioUnderTest("initJobTimestamps.1")
  @TestTemplate
  void dueDateConversion() {

    Job job = rule.jobQuery().singleResult();

    // assume
    assertNotNull(job);

    // then
    assertThat(job.getDuedate()).isEqualTo(TIMESTAMP);
  }

  @ScenarioUnderTest("initJobTimestamps.1")
  @TestTemplate
  void lockExpirationTimeConversion() {

    JobEntity job = (JobEntity) rule.jobQuery().singleResult();

    // assume
    assertNotNull(job);

    // then
    assertThat(job.getLockExpirationTime()).isEqualTo(LOCK_EXP_TIME);
  }
}
