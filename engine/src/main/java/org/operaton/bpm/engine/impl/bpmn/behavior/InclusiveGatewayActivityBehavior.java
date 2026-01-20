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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;
import org.operaton.bpm.engine.impl.Condition;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;

/**
 * Implementation of the Inclusive Gateway/OR gateway/inclusive data-based
 * gateway as defined in the BPMN specification.
 *
 * @author Tijs Rademakers
 * @author Tom Van Buskirk
 * @author Joram Barrez
 */
public class InclusiveGatewayActivityBehavior extends GatewayActivityBehavior {

  protected static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    execution.inactivate();
    lockConcurrentRoot(execution);

    PvmActivity activity = execution.getActivity();
    if (!activatesGateway(execution, activity)) {
      LOG.noActivityActivation(activity.getId());
      return;
    }

    LOG.activityActivation(activity.getId());

    List<ActivityExecution> joinedExecutions = execution.findInactiveConcurrentExecutions(activity);
    String defaultSequenceFlow = (String) execution.getActivity().getProperty("default");

    // find matching non-default sequence flows
    List<PvmTransition> transitionsToTake = execution.getActivity().getOutgoingTransitions()
        .stream()
        .filter(transition -> isNotDefaultFlow(transition, defaultSequenceFlow))
        .filter(transition -> hasNoConditionOrEvaluates(transition, execution))
        .collect(Collectors.toList());

    // if none found, add default flow
    if (transitionsToTake.isEmpty()) {
      if (defaultSequenceFlow == null) {
        // No sequence flow could be found, not even a default one
        throw LOG.stuckExecutionException(execution.getActivity().getId());
      }

      PvmTransition defaultTransition = execution.getActivity().findOutgoingTransition(defaultSequenceFlow);
      if (defaultTransition == null) {
        throw LOG.missingDefaultFlowException(execution.getActivity().getId(), defaultSequenceFlow);
      }

      transitionsToTake.add(defaultTransition);
    }

    // take the flows found
    execution.leaveActivityViaTransitions(transitionsToTake, joinedExecutions);
  }

  private boolean isNotDefaultFlow(PvmTransition transition, String defaultSequenceFlow) {
    return defaultSequenceFlow == null || !transition.getId().equals(defaultSequenceFlow);
  }

  private boolean hasNoConditionOrEvaluates(PvmTransition transition, ActivityExecution execution) {
    Condition condition = (Condition) transition.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
    return condition == null || condition.evaluate(execution);
  }

  protected Collection<ActivityExecution> getLeafExecutions(ActivityExecution parent) {
    List<ActivityExecution> executionlist = new ArrayList<>();
    List<? extends ActivityExecution> subExecutions = parent.getNonEventScopeExecutions();
    if (subExecutions.isEmpty()) {
      executionlist.add(parent);
    } else {
      for (ActivityExecution concurrentExecution : subExecutions) {
        executionlist.addAll(getLeafExecutions(concurrentExecution));
      }
    }

    return executionlist;
  }

  protected boolean activatesGateway(ActivityExecution execution, PvmActivity gatewayActivity) {
    int numExecutionsGuaranteedToActivate = gatewayActivity.getIncomingTransitions().size();
    ActivityExecution scopeExecution = execution.isScope() ? execution : execution.getParent();

    List<ActivityExecution> executionsAtGateway = execution.findInactiveConcurrentExecutions(gatewayActivity);

    if (executionsAtGateway.size() >= numExecutionsGuaranteedToActivate) {
      return true;
    }
    else {
      Collection<ActivityExecution> executionsNotAtGateway = getLeafExecutions(scopeExecution);
      executionsNotAtGateway.removeAll(executionsAtGateway);

      for (ActivityExecution executionNotAtGateway : executionsNotAtGateway) {
        if (canReachActivity(executionNotAtGateway, gatewayActivity)) {
          return false;
        }
      }

      // if no more token may arrive, then activate
      return true;
    }

  }

  protected boolean canReachActivity(ActivityExecution execution, PvmActivity activity) {
    PvmTransition pvmTransition = execution.getTransition();
    if (pvmTransition != null) {
      return isReachable(pvmTransition.getDestination(), activity, new HashSet<>());
    } else {
      return isReachable(execution.getActivity(), activity, new HashSet<>());
    }
  }

  protected boolean isReachable(PvmActivity srcActivity, PvmActivity targetActivity, Set<PvmActivity> visitedActivities) {
    if (srcActivity.equals(targetActivity)) {
      return true;
    }

    if (visitedActivities.contains(srcActivity)) {
      return false;
    }

    // To avoid infinite looping, we must capture every node we visit and
    // check before going further in the graph if we have already visited the node.
    visitedActivities.add(srcActivity);

    List<PvmTransition> outgoingTransitions = srcActivity.getOutgoingTransitions();

    if (outgoingTransitions.isEmpty()) {

      if (srcActivity.getActivityBehavior() instanceof EventBasedGatewayActivityBehavior) {

        ActivityImpl eventBasedGateway = (ActivityImpl) srcActivity;
        Set<ActivityImpl> eventActivities = eventBasedGateway.getEventActivities();

        for (ActivityImpl eventActivity : eventActivities) {
          boolean isReachable = isReachable(eventActivity, targetActivity, visitedActivities);

          if (isReachable) {
            return true;
          }
        }

      }
      else {

        ScopeImpl flowScope = srcActivity.getFlowScope();
        if (flowScope instanceof PvmActivity pvmActivity) {
          return isReachable(pvmActivity, targetActivity, visitedActivities);
        }

      }

      return false;
    }
    else {
      for (PvmTransition pvmTransition : outgoingTransitions) {
        PvmActivity destinationActivity = pvmTransition.getDestination();
        if (destinationActivity != null && !visitedActivities.contains(destinationActivity)) {

          boolean reachable = isReachable(destinationActivity, targetActivity, visitedActivities);

          // If false, we should investigate other paths, and not yet return the result
          if (reachable) {
            return true;
          }

        }
      }
    }

    return false;
  }

}
