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
package org.operaton.bpm.qa.rolling.update.eventSubProcess;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("ProcessWithEventSubProcessScenario")
public class CompleteProcessWithEventSubProcessTest extends AbstractRollingUpdateTestCase {

  @Test
  @ScenarioUnderTest("init.1")
  public void testCompleteProcessWithEventSubProcess() {
    //given process within event sub process
    ProcessInstance oldInstance = rule.processInstance();
    assertThat(oldInstance).isNotNull();
    Job job = rule.jobQuery().singleResult();
    assertThat(job).isNotNull();

    //when job is executed
    rule.getManagementService().executeJob(job.getId());

    //then delegate fails and event sub process is called
    Task task = rule.getTaskService()
                    .createTaskQuery()
                    .processInstanceId(oldInstance.getId())
                    .taskName("TaskInEventSubProcess").singleResult();
    assertThat(task).isNotNull();
    rule.getTaskService().complete(task.getId());
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.error.1")
  public void testCompleteProcessWithInEventSubProcess() {
    //given process within event sub process
    ProcessInstance oldInstance = rule.processInstance();
    Task task = rule.getTaskService()
                    .createTaskQuery()
                    .processInstanceId(oldInstance.getId())
                    .taskName("TaskInEventSubProcess").singleResult();
    assertThat(task).isNotNull();

    //when task is completed
    rule.getTaskService().complete(task.getId());

    //process instance is ended
    rule.assertScenarioEnded();
  }
}
