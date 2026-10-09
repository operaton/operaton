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
package org.operaton.bpm.engine.impl.migration.instance;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler.AsyncContinuationConfiguration;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.operation.PvmAtomicOperation;
import org.operaton.bpm.engine.management.JobDefinition;

/**
 * @author Thorben Lindhauer
 *
 */
public class MigratingAsyncJobInstance extends MigratingJobInstance {

  protected final boolean deferredActivityEnd;
  protected final boolean retiringActivityEnd;

  public MigratingAsyncJobInstance(JobEntity jobEntity, JobDefinitionEntity jobDefinitionEntity, ScopeImpl targetScope) {
    super(jobEntity, jobDefinitionEntity, targetScope);
    ActivityImpl sourceActivity = jobEntity.getExecution().getActivity();
    AsyncContinuationConfiguration configuration = (AsyncContinuationConfiguration) jobEntity.getJobHandlerConfiguration();
    boolean genericActivityEnd = PvmAtomicOperation.ACTIVITY_END.getCanonicalName().equals(configuration.getAtomicOperation());
    retiringActivityEnd = PvmAtomicOperation.ACTIVITY_END_RETIRE.getCanonicalName().equals(configuration.getAtomicOperation())
        || genericActivityEnd && sourceActivity != null && !sourceActivity.getOutgoingTransitions().isEmpty();
    deferredActivityEnd = PvmAtomicOperation.ACTIVITY_END_DEFERRED.getCanonicalName().equals(configuration.getAtomicOperation())
        || genericActivityEnd && sourceActivity != null && !retiringActivityEnd
            && (sourceActivity.isScope() && jobEntity.getExecution().isScope()
                || targetScope != null && targetScope.isScope()
                || isAdHocActivity(sourceActivity)
                || targetScope instanceof ActivityImpl targetActivity && isAdHocActivity(targetActivity));
  }

  public boolean isDeferredActivityEnd() {
    return deferredActivityEnd;
  }

  /** Generic END on an activity with outgoing flows retires the token instead of completing normally. */
  public boolean isRetiringActivityEnd() {
    return retiringActivityEnd;
  }

  public static boolean isActivityEnd(AsyncContinuationConfiguration configuration) {
    String operation = configuration.getAtomicOperation();
    return PvmAtomicOperation.ACTIVITY_END.getCanonicalName().equals(operation)
        || PvmAtomicOperation.ACTIVITY_END_DEFERRED.getCanonicalName().equals(operation)
        || PvmAtomicOperation.ACTIVITY_END_RETIRE.getCanonicalName().equals(operation);
  }

  @Override
  protected void migrateJobHandlerConfiguration() {
    AsyncContinuationConfiguration configuration = (AsyncContinuationConfiguration) jobEntity.getJobHandlerConfiguration();

    if (isAsyncAfter()) {
      updateAsyncAfterTargetConfiguration(configuration);
    }
    else {
      updateAsyncBeforeTargetConfiguration();
    }
  }


  public boolean isAsyncAfter() {
    JobDefinition jobDefinition = jobEntity.getJobDefinition();
    return MessageJobDeclaration.ASYNC_AFTER.equals(jobDefinition.getJobConfiguration());
  }

  public boolean isAsyncBefore() {
    return !isAsyncAfter();
  }

  protected void updateAsyncBeforeTargetConfiguration() {

    AsyncContinuationConfiguration targetConfiguration = new AsyncContinuationConfiguration();
    AsyncContinuationConfiguration currentConfiguration = (AsyncContinuationConfiguration) jobEntity.getJobHandlerConfiguration();

    if (PvmAtomicOperation.PROCESS_START.getCanonicalName().equals(currentConfiguration.getAtomicOperation())) {
      // process start always stays process start
      targetConfiguration.setAtomicOperation(PvmAtomicOperation.PROCESS_START.getCanonicalName());
    }
    else {
      if (((ActivityImpl) targetScope).getIncomingTransitions().isEmpty()) {
        targetConfiguration.setAtomicOperation(PvmAtomicOperation.ACTIVITY_START_CREATE_SCOPE.getCanonicalName());
      }
      else {
        targetConfiguration.setAtomicOperation(PvmAtomicOperation.TRANSITION_CREATE_SCOPE.getCanonicalName());
      }
    }


    jobEntity.setJobHandlerConfiguration(targetConfiguration);
  }

  protected static boolean isAdHocActivity(ActivityImpl activity) {
    return activity.getFlowScope().getActivityBehavior() instanceof AdHocSubProcessActivityBehavior behavior
        && behavior.isCompletableActivity(activity);
  }

  protected void updateAsyncAfterTargetConfiguration(AsyncContinuationConfiguration currentConfiguration) {
    ActivityImpl targetActivity = (ActivityImpl) targetScope;
    List<PvmTransition> outgoingTransitions = targetActivity.getOutgoingTransitions();

    AsyncContinuationConfiguration targetConfiguration = new AsyncContinuationConfiguration();

    if (deferredActivityEnd) {
      targetConfiguration.setAtomicOperation(PvmAtomicOperation.ACTIVITY_END_DEFERRED.getCanonicalName());
    }
    else if (retiringActivityEnd) {
      targetConfiguration.setAtomicOperation(PvmAtomicOperation.ACTIVITY_END_RETIRE.getCanonicalName());
    }
    else if (outgoingTransitions.isEmpty()) {
      String operation = currentConfiguration.getAtomicOperation();
      if (PvmAtomicOperation.TRANSITION_NOTIFY_LISTENER_TAKE.getCanonicalName().equals(operation)
          || PvmAtomicOperation.ACTIVITY_END_DISPOSED.getCanonicalName().equals(operation)) {
        // Scope output/destruction already happened. Keep the selected flow for a later migration.
        targetConfiguration.setAtomicOperation(PvmAtomicOperation.ACTIVITY_END_DISPOSED.getCanonicalName());
        targetConfiguration.setTransitionId(currentConfiguration.getTransitionId());
      } else {
        targetConfiguration.setAtomicOperation(PvmAtomicOperation.ACTIVITY_END.getCanonicalName());
      }
    }
    else {
      targetConfiguration.setAtomicOperation(PvmAtomicOperation.TRANSITION_NOTIFY_LISTENER_TAKE.getCanonicalName());

      if (outgoingTransitions.size() == 1) {
        targetConfiguration.setTransitionId(outgoingTransitions.get(0).getId());
      }
      else {
        TransitionImpl matchingTargetTransition = null;
        String currentTransitionId = currentConfiguration.getTransitionId();
        if (currentTransitionId != null) {
          matchingTargetTransition = targetActivity.findOutgoingTransition(currentTransitionId);
        }

        if (matchingTargetTransition != null) {
          targetConfiguration.setTransitionId(matchingTargetTransition.getId());
        }
        else {
          // should not happen since it is avoided by validation
          throw new ProcessEngineException("Cannot determine matching outgoing sequence flow");
        }
      }
    }

    jobEntity.setJobHandlerConfiguration(targetConfiguration);
  }

}
