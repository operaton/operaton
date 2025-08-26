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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class ActivityInstanceCountMetricsTest extends AbstractMetricsTest {

  @Test
  void testBpmnActivityInstances() {
    testRule.deploy(Bpmn.createExecutableProcess("testProcess")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .manualTask()
        .endEvent()
        .done());

    // given
    // that no activity instances have been executed
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isZero();

    // if
    // a process instance is started
    runtimeService.startProcessInstanceByKey("testProcess");

    // then
    // the increased count is immediately visible
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(3L);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 3
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(3L);

    // still 3 with the new metric name
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.FLOW_NODE_INSTANCES)
        .sum()).isEqualTo(3L);
  }

  @Test
  void testStandaloneTask() {

    // given
    // that no activity instances have been executed
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isZero();

    // if
    // I complete a standalone task
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // then
    // the increased count is immediately visible
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(1L);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 1
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(1L);

    taskService.deleteTask(task.getId());

    // clean up
    HistoricTaskInstance hti = historyService.createHistoricTaskInstanceQuery().singleResult();
    if(hti!=null) {
      historyService.deleteHistoricTaskInstance(hti.getId());
    }
  }

  @Deployment
  @Test
  void testCmmnActivityInstances() {
    // given
    // that no activity instances have been executed
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isZero();

    caseService.createCaseInstanceByKey("case");

    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(1L);

    // start PI_HumanTask_1 and PI_Milestone_1
    List<CaseExecution> list = caseService.createCaseExecutionQuery().enabled().list();
    for (CaseExecution caseExecution : list) {
      caseService.withCaseExecution(caseExecution.getId())
        .manualStart();
    }

    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(2L);

    // and force the db metrics reporter to report
    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // still 2
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(2L);

    // trigger the milestone
    CaseExecution taskExecution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    caseService.completeCaseExecution(taskExecution.getId());

    // milestone is counted
    assertThat(managementService.createMetricsQuery()
        .name(Metrics.ACTIVTY_INSTANCE_START)
        .sum()).isEqualTo(3L);

  }

}
