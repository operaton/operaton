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
package org.operaton.bpm.engine.impl.migration.instance.parser;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityEndScope;
import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingTransitionInstance;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.migration.MigrationInstruction;
import org.operaton.bpm.engine.runtime.TransitionInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public class TransitionInstanceHandler implements MigratingInstanceParseHandler<TransitionInstance> {

  @Override
  public void handle(MigratingInstanceParseContext parseContext, TransitionInstance transitionInstance) {

    if (!isMigratableTransitionInstance(transitionInstance)) {
      return;
    }

    MigrationInstruction applyingInstruction = parseContext.getInstructionFor(transitionInstance.getActivityId());

    ScopeImpl sourceScope = parseContext.getSourceProcessDefinition().findActivity(transitionInstance.getActivityId());
    ScopeImpl targetScope = null;

    if (applyingInstruction != null) {
      String activityId = applyingInstruction.getTargetActivityId();
      targetScope = parseContext.getTargetProcessDefinition().findActivity(activityId);
    }

    ExecutionEntity asyncExecution = Context
        .getCommandContext()
        .getExecutionManager()
        .findExecutionById(transitionInstance.getExecutionId());

    MigratingTransitionInstance migratingTransitionInstance = parseContext.getMigratingProcessInstance()
      .addTransitionInstance(
        applyingInstruction,
        transitionInstance,
        sourceScope,
        targetScope,
        asyncExecution);

    MigratingActivityInstance parentInstance = parseContext.getMigratingActivityInstanceById(transitionInstance.getParentActivityInstanceId());
    migratingTransitionInstance.setParent(parentInstance);

    List<JobEntity> jobs = asyncExecution.getJobs();
    parseContext.handleDependentTransitionInstanceJobs(migratingTransitionInstance, jobs);

    parseContext.handleDependentVariables(migratingTransitionInstance, collectTransitionInstanceVariables(migratingTransitionInstance));

    if (migratingTransitionInstance.isPendingActivityEnd()) {
      MigratingActivityEndScope retainedScope = new MigratingActivityEndScope(migratingTransitionInstance);
      // Reporting an uncaught BPMN error can leave its task attached until async retirement resumes.
      if (sourceScope.getActivityBehavior() instanceof UserTaskActivityBehavior userTask) {
        userTask.onParseMigratingInstance(parseContext, retainedScope);
      } else if (sourceScope.getActivityBehavior() instanceof ExternalTaskActivityBehavior externalTask) {
        externalTask.onParseMigratingInstance(parseContext, retainedScope);
      }
      boolean ownsScope = migratingTransitionInstance.isPendingScopedActivityEnd();
      parseContext.handleDependentActivityInstanceJobs(retainedScope, ownsScope ? asyncExecution.getJobs() : List.of());
      parseContext.handleDependentEventSubscriptions(retainedScope,
          ownsScope ? asyncExecution.getEventSubscriptions() : List.of());
      migratingTransitionInstance.addMigratingDependentInstance(retainedScope);
    }

  }

  /**
   * Workaround for CAM-5609: In general, only async continuations should be represented as TransitionInstances, but
   * due to this bug, completed multi-instances are represented like that as well. We tolerate the second case.
   * Enabled ad-hoc activity tokens are also transition instances, but deliberately do not own a job.
   */
  protected boolean isMigratableTransitionInstance(TransitionInstance transitionInstance) {
    String executionId = transitionInstance.getExecutionId();
    ExecutionEntity execution = Context.getCommandContext().getExecutionManager().findExecutionById(executionId);
    if (MigratingTransitionInstance.isAdHocEnabledActivity(execution.getActivity(), execution)) {
      return true;
    }
    return isAsyncTransitionInstance(transitionInstance);
  }

  protected boolean isAsyncTransitionInstance(TransitionInstance transitionInstance) {
    ExecutionEntity execution = Context.getCommandContext().getExecutionManager()
        .findExecutionById(transitionInstance.getExecutionId());
    for (JobEntity job : execution.getJobs()) {
      if (AsyncContinuationJobHandler.TYPE.equals(job.getJobHandlerType())) {
        return true;
      }
    }

    return false;
  }

  protected List<VariableInstanceEntity> collectTransitionInstanceVariables(MigratingTransitionInstance instance) {
    List<VariableInstanceEntity> variables = new ArrayList<>();
    ExecutionEntity representativeExecution = instance.resolveRepresentativeExecution();

    if (instance.isPendingScopedActivityEnd()) {
      // This continuation precedes scope destruction. Its locals have not been moved
      // to a concurrent token as they would be for a sequence-flow continuation.
      variables.addAll(representativeExecution.getVariablesInternal());
      ExecutionEntity parent = representativeExecution.getParent();
      if (parent != null && parent.isConcurrent()) {
        variables.addAll(parent.getVariablesInternal());
      } else if (parent != null) {
        variables.addAll(ActivityInstanceHandler.getConcurrentLocalVariables(parent));
      }
    }
    else if (representativeExecution.isConcurrent()) {
      variables.addAll(representativeExecution.getVariablesInternal());
    }
    else {
      variables.addAll(ActivityInstanceHandler.getConcurrentLocalVariables(representativeExecution));
    }

    return variables;
  }

}
