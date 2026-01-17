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
package org.operaton.bpm.engine.test.api.runtime.migration;
import java.util.List;

import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.util.BpmnEventFactory;
import org.operaton.bpm.engine.test.api.runtime.migration.util.ConditionalEventFactory;
import org.operaton.bpm.engine.test.api.runtime.migration.util.MessageEventFactory;
import org.operaton.bpm.engine.test.api.runtime.migration.util.MigratingBpmnEventTrigger;
import org.operaton.bpm.engine.test.api.runtime.migration.util.SignalEventFactory;
import org.operaton.bpm.engine.test.api.runtime.migration.util.TimerEventFactory;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class MigrateEventSubProcessAndTriggerTest {

  @Parameters
  public static Collection<Object[]> data() {
      return List.of(new Object[][] {
               new Object[]{ new TimerEventFactory() },
               new Object[]{ new MessageEventFactory() },
               new Object[]{ new SignalEventFactory() },
               new Object[]{ new ConditionalEventFactory() }
         });
  }

  @Parameter
  public BpmnEventFactory eventFactory;

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @BeforeEach
  void setUp() {
    ClockUtil.setCurrentTime(new Date()); // lock time so that timer job is effectively not updated
  }

  @TestTemplate
  void testMigrateEventSubprocessSignalTrigger() {
    BpmnModelInstance processModel = ProcessModels.ONE_TASK_PROCESS.copy();
    MigratingBpmnEventTrigger eventTrigger = eventFactory.addEventSubProcess(
        rule.getProcessEngine(),
        processModel,
        ProcessModels.PROCESS_KEY,
        "eventSubProcess",
        "eventSubProcessStart");
    ModifiableBpmnModelInstance.wrap(processModel).startEventBuilder("eventSubProcessStart")
        .userTask("eventSubProcessTask")
        .endEvent()
        .done();

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(processModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(processModel);

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask", "userTask")
        .mapActivities("eventSubProcessStart", "eventSubProcessStart").updateEventTrigger()
        .build();

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    eventTrigger.assertEventTriggerMigrated(testHelper, "eventSubProcessStart");

    // and it is possible to trigger the event subprocess
    eventTrigger.trigger(processInstance.getId());
    assertThat(rule.getTaskService().createTaskQuery().count()).isOne();

    // and complete the process instance
    testHelper.completeTask("eventSubProcessTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }
}
