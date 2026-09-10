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
package org.operaton.bpm.engine.impl.bpmn.parser;

import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.impl.Condition;
import org.operaton.bpm.engine.impl.core.variable.event.VariableEvent;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Represents the conditional event definition corresponding to the
 * ConditionalEvent defined by the BPMN 2.0 spec.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public @NullMarked class ConditionalEventDefinition extends EventSubscriptionDeclaration {

  private @Nullable String conditionAsString;
  private final @Nullable Condition condition;
  private boolean interrupting;
  private @Nullable String variableName;
  private @Nullable Set<String> variableEvents;
  private ActivityImpl conditionalActivity;

  public ConditionalEventDefinition(@Nullable Condition condition, ActivityImpl conditionalActivity) {
    super(null, EventType.CONDITONAL);
    this.activityId = conditionalActivity.getActivityId();
    this.conditionalActivity = conditionalActivity;
    this.condition = condition;
  }

  public ActivityImpl getConditionalActivity() {
    return conditionalActivity;
  }

  public void setConditionalActivity(ActivityImpl conditionalActivity) {
    this.conditionalActivity = conditionalActivity;
  }

  public boolean isInterrupting() {
    return interrupting;
  }

  public void setInterrupting(boolean interrupting) {
    this.interrupting = interrupting;
  }

  public @Nullable String getVariableName() {
    return variableName;
  }

  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  public Set<String> getVariableEvents() {
    return variableEvents != null ? variableEvents : Set.of();
  }

  public void setVariableEvents(Set<String> variableEvents) {
    this.variableEvents = variableEvents;
  }

  public @Nullable String getConditionAsString() {
    return conditionAsString;
  }

  public void setConditionAsString(String conditionAsString) {
    this.conditionAsString = conditionAsString;
  }

  public boolean shouldEvaluateForVariableEvent(VariableEvent event) {
    return
    (variableName == null || event.getVariableInstance().getName().equals(variableName))
                                          &&
    ((variableEvents == null || variableEvents.isEmpty()) || variableEvents.contains(event.getEventName()));
  }

  public boolean evaluate(DelegateExecution execution) {
    if (condition != null) {
      return condition.evaluate(execution, execution);
    }
    throw new IllegalStateException("Conditional event must have a condition!");
  }

  public boolean tryEvaluate(DelegateExecution execution) {
    if (condition != null) {
      return condition.tryEvaluate(execution, execution);
    }
    throw new IllegalStateException("Conditional event must have a condition!");
  }

  public boolean tryEvaluate(@Nullable VariableEvent variableEvent, DelegateExecution execution) {
    return (variableEvent == null || shouldEvaluateForVariableEvent(variableEvent)) && tryEvaluate(execution);
  }
}
