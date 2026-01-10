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
package org.operaton.bpm.qa.rolling.update.externalTask;

import java.util.List;

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.rolling.update.RollingUpdateTest;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("ProcessWithExternalTaskScenario")
class CompleteProcessWithExternalTaskTest extends AbstractRollingUpdateTestCase {

  public static final long LOCK_TIME = 5 * 60 * 1000;

  @RollingUpdateTest
  @ScenarioUnderTest("init.1")
  void testCompleteProcessWithExternalTask() {
    //given process with external task
    String buisnessKey = rule.getBusinessKey();
    List<LockedExternalTask> externalTasks = rule.getExternalTaskService().fetchAndLock(1, buisnessKey)
      .topic(buisnessKey, LOCK_TIME)
      .execute();
    assertThat(externalTasks).hasSize(1);

    //when external task is completed
    rule.getExternalTaskService().complete(externalTasks.get(0).getId(), buisnessKey);

    //then process instance is ended
    rule.assertScenarioEnded();
  }

  @RollingUpdateTest
  @ScenarioUnderTest("init.fetch.1")
  void testCompleteProcessWithFetchedExternalTask() {
    //given process with locked external task
    String buisnessKey = rule.getBusinessKey();
    ExternalTask task = rule.getExternalTaskService()
                            .createExternalTaskQuery()
                            .locked()
                            .topicName(buisnessKey)
                            .workerId(buisnessKey)
                            .singleResult();
    assertThat(task).isNotNull();

    //when external task is completed
    rule.getExternalTaskService().complete(task.getId(), buisnessKey);

    //then no locked external task with worker id exists
    task = rule.getExternalTaskService()
                            .createExternalTaskQuery()
                            .locked()
                            .topicName(buisnessKey)
                            .workerId(buisnessKey)
                            .singleResult();
    assertThat(task).isNull();
    rule.assertScenarioEnded();
  }
}
