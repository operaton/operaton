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
package org.operaton.bpm.engine.test.standalone.entity;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 * @author Nikola Koevski <nikola.koevski@camunda.com>
 */
class ExecutionEntityTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  @Test
  void testRestoreProcessInstance() {
    //given parent execution
    List<ExecutionEntity> entities = new ArrayList<>();
    ExecutionEntity parent = new ExecutionEntity();
    parent.setId("parent");
    entities.add(parent);
    //when restore process instance is called
    parent.restoreProcessInstance(entities, null, null, null, null, null, null);
    //then no problem should occur

    //when child is added and restore is called again
    ExecutionEntity entity = new ExecutionEntity();
    entity.setId("child");
    entity.setParentId(parent.getId());
    entities.add(entity);

    parent.restoreProcessInstance(entities, null, null, null, null, null, null);
    //then again no problem should occur

    //when parent is deleted from the list
    entities.remove(parent);

    // when
    assertThatThrownBy(() -> parent.restoreProcessInstance(entities, null, null, null, null, null, null))
      // then exception is thrown because child reference to parent which does not exist anymore
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot resolve parent with id 'parent' of execution 'child', perhaps it was deleted in the meantime");
  }

  @Test
  void testRemoveExecutionSequence() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("singleTaskProcess")
      .startEvent()
      .userTask("taskWithLocalVariables")
        .operatonExecutionListenerClass("start", TestLocalVariableExecutionListener.class)
        .operatonTaskListenerClass("delete", TestLocalVariableTaskListener.class)
        .boundaryEvent()
          .signal("interruptSignal")
        .endEvent()
      .moveToActivity("taskWithLocalVariables")
      .endEvent()
      .done();

    testRule.deploy(modelInstance);
    ProcessInstance pi = processEngineRule.getRuntimeService()
      .startProcessInstanceByKey("singleTaskProcess");
    Execution execution = processEngineRule.getRuntimeService()
      .createExecutionQuery()
      .variableValueEquals("localVar", "localVarVal")
      .singleResult();

    // when
    assertThat(execution).isNotNull();
    assertThat(execution.getProcessInstanceId()).isEqualTo(pi.getId());
    assertThat(pi.getProcessDefinitionKey()).isEqualTo("singleTaskProcess");
    processEngineRule.getRuntimeService().signal(execution.getId());

    // then (see #TestLocalVariableTaskListener::notify)
  }

  public static class TestLocalVariableExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      // given (see #testRemoveExecutionSequence)
      execution.setVariableLocal("localVar", "localVarVal");
    }
  }

  public static class TestLocalVariableTaskListener implements TaskListener{

    @Override
    public void notify(DelegateTask delegateTask) {
      // then (see #testRemoveExecutionSequence)
      assertThatCode(() -> {
        StringValue variable = delegateTask.getExecution().getVariableLocalTyped("localVar");
        assertThat(variable.getValue()).isEqualTo("localVarVal");
      }).doesNotThrowAnyException();
    }
  }
}
