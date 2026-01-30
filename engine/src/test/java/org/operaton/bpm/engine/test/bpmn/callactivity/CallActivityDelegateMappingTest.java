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
package org.operaton.bpm.engine.test.bpmn.callactivity;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
class CallActivityDelegateMappingTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMapping.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMapping() {
    //given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();

    //when
    Task taskInSubProcess = taskQuery.singleResult();
    assertEquals("Task in subprocess", taskInSubProcess.getName());

    //then check value from input variable
    Object inputVar = runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "TestInputVar");
    assertEquals("inValue", inputVar);

    //when completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertEquals("Task after subprocess", taskAfterSubProcess.getName());

    //then check value from output variable
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    Object outputVar = runtimeService.getVariable(processInstance.getId(), "TestOutputVar");
    assertEquals("outValue", outputVar);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpression() {
    //given

    Map<Object, Object> vars = engineRule.getProcessEngineConfiguration().getBeans();
    vars.put("expr", new DelegatedVarMapping());
    engineRule.getProcessEngineConfiguration().setBeans(vars);
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();

    //when
    Task taskInSubProcess = taskQuery.singleResult();
    assertEquals("Task in subprocess", taskInSubProcess.getName());

    //then check if variable mapping was executed - check if input variable exist
    Object inputVar = runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "TestInputVar");
    assertEquals("inValue", inputVar);

    //when completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertEquals("Task after subprocess", taskAfterSubProcess.getName());

    //then check if variable output mapping was executed - check if output variable exist
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    Object outputVar = runtimeService.getVariable(processInstance.getId(), "TestOutputVar");
    assertEquals("outValue", outputVar);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingNotFound.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingNotFound() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("callSimpleSubProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-09008 Exception while instantiating class 'org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping': ENGINE-09017 Cannot load class 'org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping': org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping");
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionNotFound.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpressionNotFound() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("callSimpleSubProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Unknown property used in expression: ${notFound}. Cause: Cannot resolve identifier 'notFound'");
  }

  private void delegateVariableMappingThrowException() {
    //given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
    var taskId = taskBeforeSubProcess.getId();

    //when/then completing the task continues the process which leads to calling the subprocess
    //which throws an exception
    assertThatThrownBy(() -> taskService.complete(taskId))
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(pex -> assertThat("org.operaton.bpm.engine.ProcessEngineException: New process engine exception.".equalsIgnoreCase(pex.getMessage())
          || pex.getMessage().contains("1234")).isTrue());

    //then process rollback to user task which is before sub process
    //not catched by boundary event
    taskBeforeSubProcess = taskQuery.singleResult();
    assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingThrowException.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingThrowException() {
    delegateVariableMappingThrowException();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowException() {
    //given
    Map<Object, Object> vars = engineRule.getProcessEngineConfiguration().getBeans();
    vars.put("expr", new DelegateVarMappingThrowException());
    engineRule.getProcessEngineConfiguration().setBeans(vars);
    delegateVariableMappingThrowException();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingThrowBpmnError.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingThrowBpmnError() {
    delegateVariableMappingThrowException();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowBpmnError() {
    //given
    Map<Object, Object> vars = engineRule.getProcessEngineConfiguration().getBeans();
    vars.put("expr", new DelegateVarMappingThrowBpmnError());
    engineRule.getProcessEngineConfiguration().setBeans(vars);
    delegateVariableMappingThrowException();
  }

  private void delegateVariableMappingThrowExceptionOutput() {
    //given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();
    var taskInSubProcessId = taskInSubProcess.getId();

    //when/then completing the task continues the process which leads to calling the output mapping
    //which throws an exception
    assertThatThrownBy(() -> taskService.complete(taskInSubProcessId))
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(pex -> assertThat("org.operaton.bpm.engine.ProcessEngineException: New process engine exception.".equalsIgnoreCase(pex.getMessage())
          || pex.getMessage().contains("1234")).isTrue());

    //then process rollback to user task which is in sub process
    //not catched by boundary event
    taskInSubProcess = taskQuery.singleResult();
    assertEquals("Task in subprocess", taskInSubProcess.getName());
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingThrowExceptionOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingThrowExceptionOutput() {
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowExceptionOutput() {
    //given
    Map<Object, Object> vars = engineRule.getProcessEngineConfiguration().getBeans();
    vars.put("expr", new DelegateVarMappingThrowExceptionOutput());
    engineRule.getProcessEngineConfiguration().setBeans(vars);
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingThrowBpmnErrorOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingThrowBpmnErrorOutput() {
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowBpmnErrorOutput() {
    //given
    Map<Object, Object> vars = engineRule.getProcessEngineConfiguration().getBeans();
    vars.put("expr", new DelegateVarMappingThrowBpmnErrorOutput());
    engineRule.getProcessEngineConfiguration().setBeans(vars);
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallFailingSubProcessWithDelegatedVariableMapping.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/failingSubProcess.bpmn20.xml"
  })
  void testCallFailingSubProcessWithDelegatedVariableMapping() {
    //given starting process instance with call activity
    //when call activity execution fails
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    //then output mapping should be executed
    Object outputVar = runtimeService.getVariable(procInst.getId(), "TestOutputVar");
    assertEquals("outValue", outputVar);
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSubProcessWithDelegatedVariableMappingAndAsyncServiceTask.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcessWithAsyncService.bpmn20.xml"
  })
  void testCallSubProcessWithDelegatedVariableMappingAndAsyncServiceTask() {
    //given starting process instance with call activity which has asyn service task
    ProcessInstance superProcInst = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    ProcessInstance subProcInst = runtimeService
            .createProcessInstanceQuery()
            .processDefinitionKey("simpleSubProcessWithAsyncService").singleResult();

    //then delegation variable mapping class should also been resolved
    //input mapping should be executed
    Object inVar = runtimeService.getVariable(subProcInst.getId(), "TestInputVar");
    assertEquals("inValue", inVar);

    //and after finish call activity the ouput mapping is executed
    testHelper.executeAvailableJobs();

    Object outputVar = runtimeService.getVariable(superProcInst.getId(), "TestOutputVar");
    assertEquals("outValue", outputVar);
  }

}
