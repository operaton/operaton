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
package org.operaton.bpm.engine.test.api.runtime.migration.util;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public class ConditionalEventFactory implements BpmnEventFactory {

  protected static final String VAR_CONDITION = "${any=='any'}";

  @Override
  public MigratingBpmnEventTrigger addBoundaryEvent(ProcessEngine engine, BpmnModelInstance modelInstance, String activityId, String boundaryEventId) {
    ModifiableBpmnModelInstance.wrap(modelInstance)
      .activityBuilder(activityId)
      .boundaryEvent(boundaryEventId)
        .condition(VAR_CONDITION)
      .done();

    ConditionalEventTrigger trigger = new ConditionalEventTrigger();
    trigger.engine = engine;
    trigger.variableName = "any";
    trigger.variableValue = "any";
    trigger.activityId = boundaryEventId;

    return trigger;
  }

  @Override
  public MigratingBpmnEventTrigger addEventSubProcess(ProcessEngine engine, BpmnModelInstance modelInstance, String parentId, String subProcessId, String startEventId) {
    ModifiableBpmnModelInstance.wrap(modelInstance)
      .addSubProcessTo(parentId)
      .id(subProcessId)
      .triggerByEvent()
      .embeddedSubProcess()
        .startEvent(startEventId)
        .condition(VAR_CONDITION)
      .subProcessDone()
      .done();

    ConditionalEventTrigger trigger = new ConditionalEventTrigger();
    trigger.engine = engine;
    trigger.variableName = "any";
    trigger.variableValue = "any";
    trigger.activityId = startEventId;

    return trigger;
  }

  protected static class ConditionalEventTrigger implements MigratingBpmnEventTrigger {

    protected ProcessEngine engine;
    protected String variableName;
    protected Object variableValue;
    protected String activityId;

    @Override
    public void trigger(String processInstanceId) {
      engine.getRuntimeService().setVariable(processInstanceId, variableName, variableValue);
    }

    @Override
    public void assertEventTriggerMigrated(MigrationTestRule migrationContext, String targetActivityId) {
      migrationContext.assertEventSubscriptionMigrated(activityId, targetActivityId, null);
    }

    @Override
    public void assertEventTriggerMigrated(MigrationTestExtension migrationContext, String targetActivityId) {
      migrationContext.assertEventSubscriptionMigrated(activityId, targetActivityId, null);
    }

    @Override
    public MigratingBpmnEventTrigger inContextOf(String newActivityId) {
      ConditionalEventTrigger newTrigger = new ConditionalEventTrigger();
      newTrigger.activityId = newActivityId;
      newTrigger.engine = engine;
      newTrigger.variableName = variableName;
      newTrigger.variableValue = variableValue;
      return newTrigger;
    }

  }

}
