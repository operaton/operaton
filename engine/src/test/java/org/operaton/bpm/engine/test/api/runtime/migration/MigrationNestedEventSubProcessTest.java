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
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.migration.models.EventSubProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
public class MigrationNestedEventSubProcessTest {

  protected static final String USER_TASK_ID = "userTask";
  protected static final String EVENT_SUB_PROCESS_START_ID = "eventSubProcessStart";
  protected static final String EVENT_SUB_PROCESS_TASK_ID = "eventSubProcessTask";
  public static final String TIMER_DATE = "2016-02-11T12:13:14Z";

  protected abstract static class MigrationEventSubProcessTestConfiguration {
    public abstract BpmnModelInstance getSourceProcess();

    public abstract String getEventName();

    public void assertMigration(MigrationTestExtension testHelper) {
      testHelper.assertEventSubscriptionRemoved(EVENT_SUB_PROCESS_START_ID, getEventName());
      testHelper.assertEventSubscriptionCreated(EVENT_SUB_PROCESS_START_ID, getEventName());
    }

    public abstract void triggerEventSubProcess(MigrationTestExtension testHelper);
  }


  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][]{
      {//message event sub process configuration
        new MigrationEventSubProcessTestConfiguration() {
          @Override
          public BpmnModelInstance getSourceProcess() {
            return EventSubProcessModels.NESTED_EVENT_SUB_PROCESS_PROCESS;
          }

          @Override
          public String getEventName() {
            return EventSubProcessModels.MESSAGE_NAME;
          }

          @Override
          public void triggerEventSubProcess(MigrationTestExtension testHelper) {
            testHelper.correlateMessage(EventSubProcessModels.MESSAGE_NAME);
          }

          @Override
          public String toString() {
            return "MigrateMessageEventSubProcess";
          }
        }},
      //signal event sub process configuration
      {new MigrationEventSubProcessTestConfiguration() {
        @Override
        public BpmnModelInstance getSourceProcess() {
          return modify(ProcessModels.SUBPROCESS_PROCESS)
            .addSubProcessTo(EventSubProcessModels.SUB_PROCESS_ID)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent(EVENT_SUB_PROCESS_START_ID).signal(EventSubProcessModels.SIGNAL_NAME)
            .userTask(EVENT_SUB_PROCESS_TASK_ID)
            .endEvent()
            .subProcessDone()
            .done();
        }

        @Override
        public String getEventName() {
          return EventSubProcessModels.SIGNAL_NAME;
        }

        @Override
        public void triggerEventSubProcess(MigrationTestExtension testHelper) {
          testHelper.sendSignal(EventSubProcessModels.SIGNAL_NAME);
        }

        @Override
        public String toString() {
          return "MigrateSignalEventSubProcess";
        }
      }},
      //timer event sub process configuration
      {new MigrationEventSubProcessTestConfiguration() {
        @Override
        public BpmnModelInstance getSourceProcess() {
          return modify(ProcessModels.SUBPROCESS_PROCESS)
            .addSubProcessTo(EventSubProcessModels.SUB_PROCESS_ID)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent(EVENT_SUB_PROCESS_START_ID).timerWithDate(TIMER_DATE)
            .userTask(EVENT_SUB_PROCESS_TASK_ID)
            .endEvent()
            .subProcessDone()
            .done();
        }

        @Override
        public void assertMigration(MigrationTestExtension testHelper) {
          testHelper.assertEventSubProcessTimerJobRemoved(EVENT_SUB_PROCESS_START_ID);
          testHelper.assertEventSubProcessTimerJobCreated(EVENT_SUB_PROCESS_START_ID);
        }

        @Override
        public String getEventName() {
          return null;
        }

        @Override
        public void triggerEventSubProcess(MigrationTestExtension testHelper) {
          testHelper.triggerTimer();
        }

        @Override
        public String toString() {
          return "MigrateTimerEventSubProcess";
        }
      }},
      //conditional event sub process configuration
      {new MigrationEventSubProcessTestConfiguration() {
        @Override
        public BpmnModelInstance getSourceProcess() {
          return modify(ProcessModels.SUBPROCESS_PROCESS)
            .addSubProcessTo(EventSubProcessModels.SUB_PROCESS_ID)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent(EVENT_SUB_PROCESS_START_ID)
            .condition(EventSubProcessModels.VAR_CONDITION)
            .userTask(EVENT_SUB_PROCESS_TASK_ID)
            .endEvent()
            .subProcessDone()
            .done();
        }

        @Override
        public String getEventName() {
          return null;
        }

        @Override
        public void triggerEventSubProcess(MigrationTestExtension testHelper) {
          testHelper.setAnyVariable(testHelper.snapshotAfterMigration.getProcessInstanceId());
        }

        @Override
        public String toString() {
          return "MigrateConditionalEventSubProcess";
        }
      }}
    });
  }

  @Parameter
  public MigrationEventSubProcessTestConfiguration configuration;

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @TestTemplate
  void testMapUserTaskSiblingOfEventSubProcess() {

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(configuration.getSourceProcess());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(configuration.getSourceProcess());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(USER_TASK_ID).scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope(EventSubProcessModels.SUB_PROCESS_ID)
        .activity(USER_TASK_ID, testHelper.getSingleActivityInstanceBeforeMigration(USER_TASK_ID).getId())
        .done());

    configuration.assertMigration(testHelper);

    // and it is possible to successfully complete the migrated instance
    testHelper.completeTask(USER_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @TestTemplate
  void testMapUserTaskSiblingOfEventSubProcessAndTriggerEvent() {
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(configuration.getSourceProcess());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(configuration.getSourceProcess());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities(USER_TASK_ID, USER_TASK_ID)
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then it is possible to trigger event sub process and successfully complete the migrated instance
    configuration.triggerEventSubProcess(testHelper);
    testHelper.completeTask(EVENT_SUB_PROCESS_TASK_ID);
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }
}
