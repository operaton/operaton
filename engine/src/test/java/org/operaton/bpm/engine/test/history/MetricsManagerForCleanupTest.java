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
package org.operaton.bpm.engine.test.history;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class MetricsManagerForCleanupTest {

  private static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("process")
      .startEvent("start")
      .userTask("userTask1")
      .sequenceFlowId("seq")
      .userTask("userTask2")
      .endEvent("end")
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setProcessEngineName("metricsEngine");
      configuration.setTaskMetricsEnabled(true);
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ManagementService managementService;
  RuntimeService runtimeService;
  TaskService taskService;

  @AfterEach
  void clearDatabase() {
    testRule.deleteHistoryCleanupJobs();
    managementService.deleteTaskMetrics(null);
  }

  @Parameter(0)
  public int taskMetricHistoryTTL;

  @Parameter(1)
  public int metric1DaysInThePast;

  @Parameter(2)
  public int metric2DaysInThePast;

  @Parameter(3)
  public int batchSize;

  @Parameter(4)
  public int resultCount;

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
        // all historic batches are old enough to be cleaned up
        { 5, -6, -7, 50, 2 },
        // one batch should be cleaned up
        { 5, -3, -7, 50, 1 },
        // not enough time has passed
        { 5, -3, -4, 50, 0 },
        // batchSize will reduce the result
        { 5, -6, -7, 1, 1 } });
  }

  @TestTemplate
  void testFindHistoricBatchIdsForCleanup() {
    // given
    prepareTaskMetrics();

    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute((Command<Object>) commandContext -> {
      // when
      List<String> taskMetricIdsForCleanup = commandContext.getMeterLogManager()
          .findTaskMetricsForCleanup(batchSize, taskMetricHistoryTTL, 0, 59);

      // then
      assertThat(taskMetricIdsForCleanup).hasSize(resultCount);

      return null;
    });
  }

  private void prepareTaskMetrics() {
    testRule.deploy(PROCESS);
    runtimeService.startProcessInstanceByKey("process");

    String taskId = taskService.createTaskQuery().singleResult().getId();

    ClockUtil.offset(TimeUnit.DAYS.toMillis(metric1DaysInThePast));
    taskService.setAssignee(taskId, "kermit");

    ClockUtil.offset(TimeUnit.DAYS.toMillis(metric2DaysInThePast));
    taskService.setAssignee(taskId, "gonzo");

    ClockUtil.reset();
  }
}
