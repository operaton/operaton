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
package org.operaton.bpm.integrationtest.functional.error;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;

import jakarta.inject.Named;

@Named
public class ThrowErrorDelegate extends AbstractBpmnActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    handle(execution, "executed");
  }

  @Override
  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
    handle(execution, "signaled");
  }

  protected void handle(ActivityExecution execution, String action) throws Exception {
    execution.setVariable(action, true);
    String type = (String) execution.getVariable("type");
    if ("error".equalsIgnoreCase(type)) {
      throw new BpmnError("MyError");
    }
    else if ("exception".equalsIgnoreCase(type)) {
      throw new MyBusinessException("MyException");
    }
    else if ("leave".equalsIgnoreCase(type)) {
      execution.setVariable("type", null);
      leave(execution);
    }
  }

}
