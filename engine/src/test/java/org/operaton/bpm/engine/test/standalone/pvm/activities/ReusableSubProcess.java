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
package org.operaton.bpm.engine.test.standalone.pvm.activities;

import java.util.List;

import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SubProcessActivityBehavior;


/**
 * @author Tom Baeyens
 */
public class ReusableSubProcess implements SubProcessActivityBehavior {

  PvmProcessDefinition processDefinition;

  public ReusableSubProcess(PvmProcessDefinition processDefinition) {
    this.processDefinition = processDefinition;
  }

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    PvmProcessInstance subProcessInstance = execution.createSubProcessInstance(processDefinition);

    subProcessInstance.start();
  }

  @Override
  public void passOutputVariables(ActivityExecution targetExecution, VariableScope calledElementInstance) {
    // no-op
  }

  @Override
  public void completed(ActivityExecution execution) throws Exception {
    List<PvmTransition> outgoingTransitions = execution.getActivity().getOutgoingTransitions();
    execution.leaveActivityViaTransitions(outgoingTransitions, null);
  }
}
