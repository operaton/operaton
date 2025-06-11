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
package org.operaton.bpm.engine.test.standalone.deploy;

import java.util.List;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.impl.variable.VariableDeclaration;

public class DelegatingBpmnParseListener implements BpmnParseListener {

  public static BpmnParseListener delegate;

  @Override
  public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
    delegate.parseProcess(processElement, processDefinition);
  }

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope,
      ActivityImpl startEventActivity) {
    delegate.parseStartEvent(startEventElement, scope, startEventActivity);
  }

  @Override
  public void parseExclusiveGateway(Element exclusiveGwElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseExclusiveGateway(exclusiveGwElement, scope, activity);
  }

  @Override
  public void parseInclusiveGateway(Element inclusiveGwElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseInclusiveGateway(inclusiveGwElement, scope, activity);
  }

  @Override
  public void parseParallelGateway(Element parallelGwElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseParallelGateway(parallelGwElement, scope, activity);
  }

  @Override
  public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseScriptTask(scriptTaskElement, scope, activity);
  }

  @Override
  public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseServiceTask(serviceTaskElement, scope, activity);
  }

  @Override
  public void parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseBusinessRuleTask(businessRuleTaskElement, scope, activity);
  }

  @Override
  public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseTask(taskElement, scope, activity);
  }

  @Override
  public void parseManualTask(Element manualTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseManualTask(manualTaskElement, scope, activity);
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseUserTask(userTaskElement, scope, activity);
  }

  @Override
  public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseEndEvent(endEventElement, scope, activity);
  }

  @Override
  public void parseBoundaryTimerEventDefinition(Element timerEventDefinition, boolean interrupting,
      ActivityImpl timerActivity) {
    delegate.parseBoundaryTimerEventDefinition(timerEventDefinition, interrupting, timerActivity);
  }

  @Override
  public void parseBoundaryErrorEventDefinition(Element errorEventDefinition, boolean interrupting,
      ActivityImpl activity, ActivityImpl nestedErrorEventActivity) {
    delegate.parseBoundaryErrorEventDefinition(errorEventDefinition, interrupting, activity, nestedErrorEventActivity);
  }

  @Override
  public void parseSubProcess(Element subProcessElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseSubProcess(subProcessElement, scope, activity);
  }

  @Override
  public void parseCallActivity(Element callActivityElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseCallActivity(callActivityElement, scope, activity);
  }

  @Override
  public void parseProperty(Element propertyElement, VariableDeclaration variableDeclaration,
      ActivityImpl activity) {
    delegate.parseProperty(propertyElement, variableDeclaration, activity);
  }

  @Override
  public void parseSequenceFlow(Element sequenceFlowElement, ScopeImpl scopeElement,
      TransitionImpl transition) {
    delegate.parseSequenceFlow(sequenceFlowElement, scopeElement, transition);
  }

  @Override
  public void parseSendTask(Element sendTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseSendTask(sendTaskElement, scope, activity);
  }

  @Override
  public void parseMultiInstanceLoopCharacteristics(Element activityElement,
      Element multiInstanceLoopCharacteristicsElement, ActivityImpl activity) {
    delegate.parseMultiInstanceLoopCharacteristics(activityElement, multiInstanceLoopCharacteristicsElement, activity);
  }

  @Override
  public void parseIntermediateTimerEventDefinition(Element timerEventDefinition,
      ActivityImpl timerActivity) {
    delegate.parseIntermediateTimerEventDefinition(timerEventDefinition, timerActivity);
  }

  @Override
  public void parseRootElement(Element rootElement,
      List<ProcessDefinitionEntity> processDefinitions) {
    delegate.parseRootElement(rootElement, processDefinitions);
  }

  @Override
  public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseReceiveTask(receiveTaskElement, scope, activity);
  }

  @Override
  public void parseIntermediateSignalCatchEventDefinition(Element signalEventDefinition,
      ActivityImpl signalActivity) {
    delegate.parseIntermediateSignalCatchEventDefinition(signalEventDefinition, signalActivity);
  }

  @Override
  public void parseIntermediateMessageCatchEventDefinition(Element messageEventDefinition,
      ActivityImpl nestedActivity) {
    delegate.parseIntermediateMessageCatchEventDefinition(messageEventDefinition, nestedActivity);
  }

  @Override
  public void parseBoundarySignalEventDefinition(Element signalEventDefinition,
      boolean interrupting, ActivityImpl signalActivity) {
    delegate.parseBoundarySignalEventDefinition(signalEventDefinition, interrupting, signalActivity);
  }

  @Override
  public void parseEventBasedGateway(Element eventBasedGwElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseEventBasedGateway(eventBasedGwElement, scope, activity);
  }

  @Override
  public void parseTransaction(Element transactionElement, ScopeImpl scope, ActivityImpl activity) {
    delegate.parseTransaction(transactionElement, scope, activity);
  }

  @Override
  public void parseCompensateEventDefinition(Element compensateEventDefinition,
      ActivityImpl compensationActivity) {
    delegate.parseCompensateEventDefinition(compensateEventDefinition, compensationActivity);
  }

  @Override
  public void parseIntermediateThrowEvent(Element intermediateEventElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseIntermediateThrowEvent(intermediateEventElement, scope, activity);
  }

  @Override
  public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope,
      ActivityImpl activity) {
    delegate.parseIntermediateCatchEvent(intermediateEventElement, scope, activity);
  }

  @Override
  public void parseBoundaryEvent(Element boundaryEventElement, ScopeImpl scopeElement,
      ActivityImpl nestedActivity) {
    delegate.parseBoundaryEvent(boundaryEventElement, scopeElement, nestedActivity);
  }

  @Override
  public void parseBoundaryMessageEventDefinition(Element element, boolean interrupting,
      ActivityImpl messageActivity) {
    delegate.parseBoundaryMessageEventDefinition(element, interrupting, messageActivity);
  }

  @Override
  public void parseBoundaryEscalationEventDefinition(Element escalationEventDefinition,
      boolean interrupting, ActivityImpl boundaryEventActivity) {
    delegate.parseBoundaryEscalationEventDefinition(escalationEventDefinition, interrupting, boundaryEventActivity);
  }

  @Override
  public void parseBoundaryConditionalEventDefinition(Element element, boolean interrupting,
      ActivityImpl conditionalActivity) {
    delegate.parseBoundaryConditionalEventDefinition(element, interrupting, conditionalActivity);
  }

  @Override
  public void parseIntermediateConditionalEventDefinition(Element conditionalEventDefinition,
      ActivityImpl conditionalActivity) {
    delegate.parseIntermediateConditionalEventDefinition(conditionalEventDefinition, conditionalActivity);
  }

  @Override
  public void parseConditionalStartEventForEventSubprocess(Element element,
      ActivityImpl conditionalActivity, boolean interrupting) {
    delegate.parseConditionalStartEventForEventSubprocess(element, conditionalActivity, interrupting);
  }

  @Override
  public void parseIoMapping(Element extensionElements, ActivityImpl activity, IoMapping inputOutput) {
    delegate.parseIoMapping(extensionElements, activity, inputOutput);
  }
}
