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
package org.operaton.bpm.engine.impl.history.parser;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmEvent;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.impl.util.xml.Element;

/**
 * <p>This class is responsible for wiring history as execution listeners into process execution.
 *
 * <p>NOTE: the role of this class has changed since 7.0: in order to customize history behavior it is
 * usually not necessary to override this class but rather the {@link HistoryEventProducer} for
 * customizing data acquisition and {@link HistoryEventHandler} for customizing the persistence behavior
 * or if you need a history event stream.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Bernd Ruecker (Camunda)
 * @author Christian Lipphardt (Camunda)
 *
 * @author Daniel Meyer
 */
public class HistoryParseListener implements BpmnParseListener {

  // Cached listeners
  // listeners can be reused for a given process engine instance but cannot be cached in static fields since
  // different process engine instances on the same Classloader may have different HistoryEventProducer
  // configurations wired
  protected ExecutionListener processInstanceStartListener;
  protected ExecutionListener processInstanceEndListener;

  protected ExecutionListener activityInstanceStartListener;
  protected ExecutionListener activityInstanceEndListener;

  protected TaskListener userTaskAssignmentHandler;
  protected TaskListener userTaskIdHandler;

  // The history level set in the process engine configuration
  protected HistoryLevel historyLevel;

  public HistoryParseListener(HistoryEventProducer historyEventProducer) {
    initExecutionListeners(historyEventProducer);
  }

  protected void initExecutionListeners(HistoryEventProducer historyEventProducer) {
    processInstanceStartListener = new ProcessInstanceStartListener(historyEventProducer);
    processInstanceEndListener = new ProcessInstanceEndListener(historyEventProducer);

    activityInstanceStartListener = new ActivityInstanceStartListener(historyEventProducer);
    activityInstanceEndListener = new ActivityInstanceEndListener(historyEventProducer);

    userTaskAssignmentHandler = new ActivityInstanceUpdateListener(historyEventProducer);
    userTaskIdHandler = userTaskAssignmentHandler;
  }

  @Override
  public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
    ensureHistoryLevelInitialized();
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.PROCESS_INSTANCE_END, null)) {
      processDefinition.addBuiltInListener(PvmEvent.EVENTNAME_END, processInstanceEndListener);
    }
  }

  @Override
  public void parseExclusiveGateway(Element exclusiveGwElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseInclusiveGateway(Element inclusiveGwElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseCallActivity(Element callActivityElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseManualTask(Element manualTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    ensureHistoryLevelInitialized();
    addActivityHandlers(activity);

    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.TASK_INSTANCE_CREATE, null)) {
      TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDefinition();
      taskDefinition.addBuiltInTaskListener(TaskListener.EVENTNAME_ASSIGNMENT, userTaskAssignmentHandler);
      taskDefinition.addBuiltInTaskListener(TaskListener.EVENTNAME_CREATE, userTaskIdHandler);
    }
  }

  @Override
  public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseSubProcess(Element subProcessElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseSendTask(Element sendTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseParallelGateway(Element parallelGwElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }


  @Override
  public void parseEventBasedGateway(Element eventBasedGwElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseMultiInstanceLoopCharacteristics(Element activityElement,
          Element multiInstanceLoopCharacteristicsElement, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseIntermediateSignalCatchEventDefinition(Element signalEventDefinition, ActivityImpl signalActivity) {
    // nothing to do
  }

  @Override
  public void parseTransaction(Element transactionElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseIntermediateThrowEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
    addActivityHandlers(activity);
  }

  @Override
  public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
    // do not write history for link events
    if(!"intermediateLinkCatch".equals(activity.getProperty("type"))) {
      addActivityHandlers(activity);
    }
  }

  @Override
  public void parseBoundaryEvent(Element boundaryEventElement, ScopeImpl scopeElement, ActivityImpl activity) {
    addActivityHandlers(activity);
  }


  // helper methods ///////////////////////////////////////////////////////////

  protected void addActivityHandlers(ActivityImpl activity) {
    ensureHistoryLevelInitialized();
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_START, null)) {
      activity.addBuiltInListener(PvmEvent.EVENTNAME_START, activityInstanceStartListener, 0);
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_END, null)) {
      activity.addBuiltInListener(PvmEvent.EVENTNAME_END, activityInstanceEndListener);
    }
  }

  protected void ensureHistoryLevelInitialized() {
    if (historyLevel == null) {
      historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    }
  }

}
