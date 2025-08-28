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

import java.util.List;

import org.junit.Test;

import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test ensures that the old engine can complete an
 * existing process with parallel gateway and user task on the new schema.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("ProcessWithParallelGatewayScenario")
public class CompleteProcessWithParallelGatewayTest extends AbstractRollingUpdateTestCase {

  @Test
  @ScenarioUnderTest("init.none.1")
  public void testCompleteProcessWithParallelGateway() {
    //given an already started process instance with two user tasks
    ProcessInstance oldInstance = rule.processInstance();
    assertThat(oldInstance).isNotNull();

    List<Task> tasks = rule.taskQuery().list();
    assertThat(tasks.size()).isEqualTo(2);

    //when completing the user tasks
    for (Task task : tasks) {
      rule.getTaskService().complete(task.getId());
    }

    //then there exists no more tasks
    //and the process instance is also completed
    assertThat(rule.taskQuery().count()).isEqualTo(0);
    rule.assertScenarioEnded();
  }


  @Test
  @ScenarioUnderTest("init.complete.one.1")
  public void testCompleteProcessWithParallelGatewayAndSingleUserTask() {
    //given an already started process instance
    ProcessInstance oldInstance = rule.processInstance();
    assertThat(oldInstance).isNotNull();

    //with one completed user task
    HistoricTaskInstanceQuery historicTaskQuery = rule.getHistoryService()
            .createHistoricTaskInstanceQuery()
            .processInstanceId(oldInstance.getId())
            .finished();
    assertThat(historicTaskQuery.count()).isEqualTo(1);

    //and one waiting
    Task task = rule.taskQuery().singleResult();
    assertThat(task).isNotNull();

    //when completing the user task
    rule.getTaskService().complete(task.getId());

    //then there exists no more tasks
    assertThat(rule.taskQuery().count()).isEqualTo(0);
    //and two historic tasks
    assertThat(historicTaskQuery.count()).isEqualTo(2);
    //and the process instance is also completed
    rule.assertScenarioEnded();
  }

  @Test
  @ScenarioUnderTest("init.complete.two.1")
  public void testQueryHistoricProcessWithParallelGateway() {
    //given an already finished process instance with parallel gateway and two user tasks
    HistoricProcessInstance historicProcessInstance = rule.historicProcessInstance();

    //when query history
    HistoricTaskInstanceQuery historicTaskQuery = rule.getHistoryService()
            .createHistoricTaskInstanceQuery()
            .processInstanceId(historicProcessInstance.getId());

    //then two historic user tasks are returned
    assertThat(historicTaskQuery.count()).isEqualTo(2);
  }

}
