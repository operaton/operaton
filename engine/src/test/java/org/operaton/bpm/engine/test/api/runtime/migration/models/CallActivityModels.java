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
package org.operaton.bpm.engine.test.api.runtime.migration.models;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public final class CallActivityModels {

  private CallActivityModels() {
  }

  public static BpmnModelInstance oneBpmnCallActivityProcess(String calledProcessKey) {
    return ProcessModels.newModel()
        .startEvent()
        .callActivity("callActivity")
          .calledElement(calledProcessKey)
        .userTask("userTask")
        .endEvent()
        .done();
  }

  public static BpmnModelInstance subProcessBpmnCallActivityProcess(String calledProcessKey) {
    return ProcessModels.newModel()
        .startEvent()
        .subProcess("subProcess")
        .embeddedSubProcess()
          .startEvent()
          .callActivity("callActivity")
            .calledElement(calledProcessKey)
          .userTask("userTask")
          .endEvent()
        .subProcessDone()
        .endEvent()
        .done();
  }

  public static BpmnModelInstance oneCmmnCallActivityProcess(String caseCaseKey) {
    return ProcessModels.newModel()
        .startEvent()
        .callActivity("callActivity")
          .operatonCaseRef(caseCaseKey)
        .userTask("userTask")
        .endEvent()
        .done();
  }

  public static BpmnModelInstance oneBpmnCallActivityProcessAsExpression(int processNumber){
    return ProcessModels.newModel(processNumber)
        .startEvent()
        .callActivity()
          .calledElement("${NextProcess}")
          .operatonIn("NextProcess", "NextProcess")
        .endEvent()
        .done();
  }

  public static BpmnModelInstance oneBpmnCallActivityProcessAsExpressionAsync(int processNumber){
    return ProcessModels.newModel(processNumber)
        .startEvent()
          .operatonAsyncBefore(true)
        .callActivity()
          .calledElement("${NextProcess}")
          .operatonIn("NextProcess", "NextProcess")
        .endEvent()
        .done();
  }

  public static BpmnModelInstance oneBpmnCallActivityProcessPassingVariables(int processNumber, int calledProcessNumber){
    return ProcessModels.newModel(processNumber)
        .startEvent()
        .callActivity()
          .calledElement("Process"+calledProcessNumber)
          .operatonInputParameter("NextProcess", "Process"+(processNumber+1))
          .operatonIn("NextProcess", "NextProcess")
        .endEvent()
        .done();
  }
}
