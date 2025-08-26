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

import org.junit.Assert;
import org.junit.Test;

import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;


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
    Assert.assertNotNull(job);
    //job is not available since timer is set to 2 mintues in the future
    Assert.assertFalse(!job.isSuspended()
            && job.getRetries() > 0
            && (job.getDuedate() == null
                || ClockUtil.getCurrentTime().after(job.getDuedate())));

    //when time is incremented by five minutes
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + 60 * 1000 * 5));

    //then job is available and timer should executed and process instance ends
    job = rule.jobQuery().singleResult();
    Assert.assertTrue(!job.isSuspended()
            && job.getRetries() > 0
            && (job.getDuedate() == null
                || ClockUtil.getCurrentTime().after(job.getDuedate())));
    rule.getManagementService().executeJob(job.getId());
    rule.assertScenarioEnded();
  }

}
