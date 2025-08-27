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
package org.operaton.bpm.qa.rolling.update.task;

import java.util.Date;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * This test ensures that the old engine can complete an
 * existing process with user task and timer on the new schema.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("ProcessWithUserTaskAndTimerScenario")
public class CompleteProcessWithUserTaskAndTimerTest extends AbstractRollingUpdateTestCase {

  @Test
  @ScenarioUnderTest("init.1")
  public void testCompleteProcessWithUserTaskAndTimer() {
    //given a process instance with user task and timer boundary event
    Job job = rule.jobQuery().singleResult();
    assertThat(job).isNotNull();
    //job is not available since timer is set to 2 mintues in the future
    assertThat(!job.isSuspended()
            && job.getRetries() > 0
            && (job.getDuedate() == null
                || ClockUtil.getCurrentTime().after(job.getDuedate())))
            .isFalse();

    //when time is incremented by five minutes
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 60 * 1000 * 5));

    //then job is available and timer should executed and process instance ends
    job = rule.jobQuery().singleResult();
    assertThat(!job.isSuspended()
            && job.getRetries() > 0
            && (job.getDuedate() == null
                || ClockUtil.getCurrentTime().after(job.getDuedate())))
            .isTrue();
    rule.getManagementService().executeJob(job.getId());
    rule.assertScenarioEnded();
  }

}
