/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.bpmn.delegate;

import org.operaton.bpm.engine.impl.delegate.DelegateInvocation;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;

/**
 * @author Roman Smirnov
 *
 */
public class ActivityBehaviorSignalInvocation extends DelegateInvocation {

  protected SignallableActivityBehavior behaviorInstance;
  protected ActivityExecution execution;
  protected String signalName;
  protected Object signalData;

  public ActivityBehaviorSignalInvocation(SignallableActivityBehavior behaviorInstance, ActivityExecution execution, String signalName, Object signalData) {
    super(execution, null);
    this.behaviorInstance = behaviorInstance;
    this.execution = execution;
    this.signalName = signalName;
    this.signalData = signalData;
  }

  protected void invoke() throws Exception {
    behaviorInstance.signal(execution, signalName, signalData);
  }

}