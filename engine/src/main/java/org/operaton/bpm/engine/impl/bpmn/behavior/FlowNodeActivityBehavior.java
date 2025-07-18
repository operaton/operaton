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

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.operation.PvmAtomicOperation;


/**
 * Superclass for all 'connectable' BPMN 2.0 process elements: tasks, gateways and events.
 * This means that any subclass can be the source or target of a sequenceflow.
 *
 * Corresponds with the notion of the 'flownode' in BPMN 2.0.
 *
 * @author Joram Barrez
 */
public abstract class FlowNodeActivityBehavior implements SignallableActivityBehavior {

  protected static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  protected BpmnActivityBehavior bpmnActivityBehavior = new BpmnActivityBehavior();

  /**
   * Default behaviour: just leave the activity with no extra functionality.
   */
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    leave(execution);
  }

  /**
   * Default way of leaving a BPMN 2.0 activity: evaluate the conditions on the
   * outgoing sequence flow and take those that evaluate to true.
   */
  public void leave(ActivityExecution execution) {
    ((ExecutionEntity) execution).dispatchDelayedEventsAndPerformOperation(PvmAtomicOperation.ACTIVITY_LEAVE);
  }

  public void doLeave(ActivityExecution execution) {
    bpmnActivityBehavior.performDefaultOutgoingBehavior(execution);
  }

  protected void leaveIgnoreConditions(ActivityExecution activityContext) {
    bpmnActivityBehavior.performIgnoreConditionsOutgoingBehavior(activityContext);
  }

  @Override
  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
    // concrete activity behaviors that do accept signals should override this method;

    throw LOG.unsupportedSignalException(execution.getActivity().getId());
  }
}
