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
package org.operaton.bpm.engine.impl.bpmn.helper;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.behavior.BpmnBehaviorLogger;
import org.operaton.bpm.engine.impl.bpmn.parser.EscalationEventDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.tree.ActivityExecutionHierarchyWalker;
import org.operaton.bpm.engine.impl.tree.ActivityExecutionMappingCollector;
import org.operaton.bpm.engine.impl.tree.OutputVariablesPropagator;

/**
 * Helper class handling the propagation of escalation.
 */
public class EscalationHandler {

  private static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  private EscalationHandler() {
  }

  public static void propagateEscalation(ActivityExecution execution, String escalationCode) {
    EscalationEventDefinition escalationEventDefinition = executeEscalation(execution, escalationCode);

    if (escalationEventDefinition == null ) {
      throw LOG.missingBoundaryCatchEventEscalation(execution.getActivity().getId(), escalationCode);
    }
  }

  /**
   * Walks through the activity execution hierarchy, fetches and executes matching escalation catch event
   * @return the escalation event definition if found matching escalation catch event
   */
  public static EscalationEventDefinition executeEscalation(ActivityExecution execution,
      String escalationCode) {
    final PvmActivity currentActivity = execution.getActivity();

    final EscalationEventDefinitionFinder escalationEventDefinitionFinder = new EscalationEventDefinitionFinder(escalationCode, currentActivity);
    ActivityExecutionMappingCollector activityExecutionMappingCollector = new ActivityExecutionMappingCollector(execution);

    ActivityExecutionHierarchyWalker walker = new ActivityExecutionHierarchyWalker(execution);
    walker.addScopePreVisitor(escalationEventDefinitionFinder);
    walker.addExecutionPreVisitor(activityExecutionMappingCollector);
    walker.addExecutionPreVisitor(new OutputVariablesPropagator());

    walker.walkUntil(element -> escalationEventDefinitionFinder.getEscalationEventDefinition() != null || element == null);

    EscalationEventDefinition escalationEventDefinition = escalationEventDefinitionFinder.getEscalationEventDefinition();
    if (escalationEventDefinition != null) {
      executeEscalationHandler(escalationEventDefinition, activityExecutionMappingCollector, escalationCode);
    }
    return escalationEventDefinition;
  }

  protected static void executeEscalationHandler(EscalationEventDefinition escalationEventDefinition, ActivityExecutionMappingCollector activityExecutionMappingCollector, String escalationCode) {

    PvmActivity escalationHandler = escalationEventDefinition.getEscalationHandler();
    PvmScope escalationScope = getScopeForEscalation(escalationEventDefinition);
    ActivityExecution escalationExecution = activityExecutionMappingCollector.getExecutionForScope(escalationScope);

    if (escalationEventDefinition.getEscalationCodeVariable() != null) {
      escalationExecution.setVariable(escalationEventDefinition.getEscalationCodeVariable(), escalationCode);
    }

    escalationExecution.executeActivity(escalationHandler);
  }

  protected static PvmScope getScopeForEscalation(EscalationEventDefinition escalationEventDefinition) {
    PvmActivity escalationHandler = escalationEventDefinition.getEscalationHandler();
    if (escalationEventDefinition.isCancelActivity()) {
      return escalationHandler.getEventScope();
    } else {
      return escalationHandler.getFlowScope();
    }
  }
}
