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
package org.operaton.bpm.application.impl.event;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.impl.util.xml.Element;

/**
 *
 * @author Daniel Meyer
 *
 */
public class ProcessApplicationEventParseListener extends AbstractBpmnParseListener {

  public static final ExecutionListener EXECUTION_LISTENER = new ProcessApplicationEventListenerDelegate();
  public static final TaskListener TASK_LISTENER = new ProcessApplicationEventListenerDelegate();

  protected void addEndEventListener(ScopeImpl activity) {
    activity.addListener(ExecutionListener.EVENTNAME_END, EXECUTION_LISTENER);
  }

  protected void addStartEventListener(ScopeImpl activity) {
    activity.addListener(ExecutionListener.EVENTNAME_START, EXECUTION_LISTENER);
  }

  protected void addTakeEventListener(TransitionImpl transition) {
    transition.addListener(ExecutionListener.EVENTNAME_TAKE, EXECUTION_LISTENER);
  }

  protected void addTaskAssignmentListeners(TaskDefinition taskDefinition) {
    taskDefinition.addTaskListener(TaskListener.EVENTNAME_ASSIGNMENT, TASK_LISTENER);
  }

  protected void addTaskCreateListeners(TaskDefinition taskDefinition) {
    taskDefinition.addTaskListener(TaskListener.EVENTNAME_CREATE, TASK_LISTENER);
  }

  protected void addTaskCompleteListeners(TaskDefinition taskDefinition) {
    taskDefinition.addTaskListener(TaskListener.EVENTNAME_COMPLETE, TASK_LISTENER);
  }

  protected void addTaskUpdateListeners(TaskDefinition taskDefinition) {
    taskDefinition.addTaskListener(TaskListener.EVENTNAME_UPDATE, TASK_LISTENER);
  }

  protected void addTaskDeleteListeners(TaskDefinition taskDefinition) {
    taskDefinition.addTaskListener(TaskListener.EVENTNAME_DELETE, TASK_LISTENER);
  }

  // BpmnParseListener implementation /////////////////////////////////////////////////////////

  @Override
  public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
    addStartEventListener(processDefinition);
    addEndEventListener(processDefinition);
  }

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEventActivity) {
    addStartEventListener(startEventActivity);
    addEndEventListener(startEventActivity);
  }

  @Override
  public void parseExclusiveGateway(Element exclusiveGwElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseInclusiveGateway(Element inclusiveGwElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseParallelGateway(Element parallelGwElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseManualTask(Element manualTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
    UserTaskActivityBehavior activityBehavior = (UserTaskActivityBehavior) activity.getActivityBehavior();
    TaskDefinition taskDefinition = activityBehavior.getTaskDefinition();
    addTaskCreateListeners(taskDefinition);
    addTaskAssignmentListeners(taskDefinition);
    addTaskCompleteListeners(taskDefinition);
    addTaskUpdateListeners(taskDefinition);
    addTaskDeleteListeners(taskDefinition);
  }

  @Override
  public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseBoundaryTimerEventDefinition(Element timerEventDefinition, boolean interrupting, ActivityImpl timerActivity) {
    // start and end event listener are set by parseBoundaryEvent()
  }

  @Override
  public void parseBoundaryErrorEventDefinition(Element errorEventDefinition, boolean interrupting, ActivityImpl activity, ActivityImpl nestedErrorEventActivity) {
    // start and end event listener are set by parseBoundaryEvent()
  }

  @Override
  public void parseSubProcess(Element subProcessElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseCallActivity(Element callActivityElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseSequenceFlow(Element sequenceFlowElement, ScopeImpl scopeElement, TransitionImpl transition) {
    addTakeEventListener(transition);
  }

  @Override
  public void parseSendTask(Element sendTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseMultiInstanceLoopCharacteristics(Element activityElement, Element multiInstanceLoopCharacteristicsElement, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseIntermediateTimerEventDefinition(Element timerEventDefinition, ActivityImpl timerActivity) {
    // start and end event listener are set by parseIntermediateCatchEvent()
  }

  @Override
  public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseIntermediateSignalCatchEventDefinition(Element signalEventDefinition, ActivityImpl signalActivity) {
    // start and end event listener are set by parseIntermediateCatchEvent()
  }

  @Override
  public void parseBoundarySignalEventDefinition(Element signalEventDefinition, boolean interrupting, ActivityImpl signalActivity) {
    // start and end event listener are set by parseBoundaryEvent()
  }

  @Override
  public void parseEventBasedGateway(Element eventBasedGwElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseTransaction(Element transactionElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseIntermediateThrowEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

  @Override
  public void parseBoundaryEvent(Element boundaryEventElement, ScopeImpl scopeElement, ActivityImpl activity) {
    addStartEventListener(activity);
    addEndEventListener(activity);
  }

}
