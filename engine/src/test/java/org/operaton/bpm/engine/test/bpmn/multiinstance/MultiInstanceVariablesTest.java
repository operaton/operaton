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
package org.operaton.bpm.engine.test.bpmn.multiinstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior.NUMBER_OF_INSTANCES;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.CallActivityBuilder;
import org.operaton.bpm.model.bpmn.instance.CallActivity;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonOut;

/**
 * @author Askar Akhmerov
 */
public class MultiInstanceVariablesTest {

  public static final String ALL = "all";
  public static final String SUB_PROCESS_ID = "testProcess";
  public static final String PROCESS_ID = "process";
  public static final String CALL_ACTIVITY = "callActivity";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  @Test
  void testMultiInstanceWithAllInOutMapping() {
    BpmnModelInstance modelInstance = getBpmnModelInstance();

    CallActivityBuilder callActivityBuilder = ((CallActivity) modelInstance.getModelElementById(CALL_ACTIVITY)).builder();

    addAllIn(modelInstance, callActivityBuilder);

    addAllOut(modelInstance, callActivityBuilder);

    BpmnModelInstance testProcess = getBpmnSubProcessModelInstance();

    deployAndStartProcess(modelInstance, testProcess);
    assertThat(engineRule.getRuntimeService().createExecutionQuery().processDefinitionKey(SUB_PROCESS_ID).list()).hasSize(2);

    List<Task> tasks = engineRule.getTaskService().createTaskQuery().active().list();
    for (Task task : tasks) {
      engineRule.getTaskService().setVariable(task.getId(),NUMBER_OF_INSTANCES,"3");
      engineRule.getTaskService().complete(task.getId());
    }

    assertThat(engineRule.getRuntimeService().createExecutionQuery().processDefinitionKey(SUB_PROCESS_ID).list()).isEmpty();
    assertThat(engineRule.getRuntimeService().createExecutionQuery().activityId(CALL_ACTIVITY).list()).isEmpty();
  }

  protected void addAllOut(BpmnModelInstance modelInstance, CallActivityBuilder callActivityBuilder) {
    OperatonOut operatonOut = modelInstance.newInstance(OperatonOut.class);
    operatonOut.setOperatonVariables(ALL);
    callActivityBuilder.addExtensionElement(operatonOut);
  }

  protected void addAllIn(BpmnModelInstance modelInstance, CallActivityBuilder callActivityBuilder) {
    OperatonIn operatonIn = modelInstance.newInstance(OperatonIn.class);
    operatonIn.setOperatonVariables(ALL);
    callActivityBuilder.addExtensionElement(operatonIn);
  }

  protected void deployAndStartProcess(BpmnModelInstance modelInstance, BpmnModelInstance testProcess) {
    engineRule.manageDeployment(engineRule.getRepositoryService().createDeployment()
        .addModelInstance("process.bpmn", modelInstance).deploy());
    engineRule.manageDeployment(engineRule.getRepositoryService().createDeployment()
        .addModelInstance("testProcess.bpmn", testProcess).deploy());
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_ID);
  }

  protected BpmnModelInstance getBpmnModelInstance() {
    return Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
          .callActivity(CALL_ACTIVITY)
          .calledElement(SUB_PROCESS_ID)
          .multiInstance()
            .cardinality("2")
            .multiInstanceDone()
        .endEvent()
        .done();
  }

  protected BpmnModelInstance getBpmnSubProcessModelInstance() {
    return Bpmn.createExecutableProcess(SUB_PROCESS_ID)
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .done();
  }

}
