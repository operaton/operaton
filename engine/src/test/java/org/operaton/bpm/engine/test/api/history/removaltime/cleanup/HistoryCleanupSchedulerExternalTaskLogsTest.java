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
package org.operaton.bpm.engine.test.api.history.removaltime.cleanup;

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupJobHandlerConfiguration.START_DELAY;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Tassilo Weidner
 */
class HistoryCleanupSchedulerExternalTaskLogsTest extends AbstractHistoryCleanupSchedulerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .configurator(configuration ->
      configure(configuration, HistoryEventTypes.EXTERNAL_TASK_SUCCESS)
    ).build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  ExternalTaskService externalTaskService;

  static final String PROCESS_KEY = "process";
  static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess(PROCESS_KEY)
    .operatonHistoryTimeToLive(5)
    .startEvent()
      .userTask("userTask").name("userTask")
    .endEvent().done();

  @BeforeEach
  void init() {
    initEngineConfiguration(engineRule, engineConfiguration);
  }

  @Test
  void shouldScheduleToNow() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .serviceTask().operatonExternalTask("anExternalTaskTopic")
        .multiInstance()
          .cardinality("5")
        .multiInstanceDone()
      .endEvent().done());

    ClockUtil.setCurrentTime(END_DATE);

    runtimeService.startProcessInstanceByKey("process");

    for (int i = 0; i < 5; i++) {
      LockedExternalTask externalTask = externalTaskService.fetchAndLock(1, "aWorkerId")
        .topic("anExternalTaskTopic", 2000)
        .execute()
        .get(0);

      externalTaskService.complete(externalTask.getId(), "aWorkerId");
    }

    engineConfiguration.setHistoryCleanupBatchSize(5);
    engineConfiguration.initHistoryCleanup();

    Date removalTime = addDays(END_DATE, 5);
    ClockUtil.setCurrentTime(removalTime);

    // when
    runHistoryCleanup();

    Job job = historyService.findHistoryCleanupJobs().get(0);

    // then
    assertThat(job.getDuedate()).isEqualTo(removalTime);
  }

  @Test
  void shouldScheduleToLater() {
    // given
    testRule.deploy(Bpmn.createExecutableProcess("process")
      .operatonHistoryTimeToLive(5)
      .startEvent()
        .serviceTask().operatonExternalTask("anExternalTaskTopic")
        .multiInstance()
          .cardinality("5")
        .multiInstanceDone()
      .endEvent().done());

    ClockUtil.setCurrentTime(END_DATE);

    runtimeService.startProcessInstanceByKey("process");

    for (int i = 0; i < 5; i++) {
      LockedExternalTask externalTask = externalTaskService.fetchAndLock(1, "aWorkerId")
        .topic("anExternalTaskTopic", 2000)
        .execute()
        .get(0);

      externalTaskService.complete(externalTask.getId(), "aWorkerId");
    }

    engineConfiguration.setHistoryCleanupBatchSize(6);
    engineConfiguration.initHistoryCleanup();

    Date removalTime = addDays(END_DATE, 5);
    ClockUtil.setCurrentTime(removalTime);

    // when
    runHistoryCleanup();

    Job job = historyService.findHistoryCleanupJobs().get(0);

    // then
    assertThat(job.getDuedate()).isEqualTo(addSeconds(removalTime, START_DELAY));
  }

}
