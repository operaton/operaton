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
package org.operaton.bpm.engine.test.bpmn.callactivity;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class CallActivityDelegateMappingTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);
  RuntimeService runtimeService;
  TaskService taskService;

  @Before
  public void setUp() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMapping.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMapping() {
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
  public void testCallSubProcessWithDelegatedVariableMappingeExpression() {
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
  public void testCallSubProcessWithDelegatedVariableMappingNotFound() {
    try {
      runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
      fail("Execption expected!");
    } catch (ProcessEngineException e) {
      //Exception while instantiating class 'org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping'
      assertEquals("ENGINE-09008 Exception while instantiating class 'org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping': ENGINE-09017 Cannot load class 'org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping': org.operaton.bpm.engine.test.bpmn.callactivity.NotFoundMapping",
              e.getMessage());
    }
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionNotFound.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMappingeExpressionNotFound() {
    try {
      runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
      fail("Exception expected!");
    } catch (ProcessEngineException pex) {
      assertEquals(
              "Unknown property used in expression: ${notFound}. Cause: Cannot resolve identifier 'notFound'",
              pex.getMessage());
    }
  }

  private void delegateVariableMappingThrowException() {
    //given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertEquals("Task before subprocess", taskBeforeSubProcess.getName());

    //when completing the task continues the process which leads to calling the subprocess
    //which throws an exception
    try {
      taskService.complete(taskBeforeSubProcess.getId());
      fail("Exeption expected!");
    } catch (ProcessEngineException pex) { //then
      Assert.assertTrue(pex.getMessage().equalsIgnoreCase("org.operaton.bpm.engine.ProcessEngineException: New process engine exception.")
              || pex.getMessage().contains("1234"));
    }

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
  public void testCallSubProcessWithDelegatedVariableMappingThrowException() {
    delegateVariableMappingThrowException();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowException() {
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
  public void testCallSubProcessWithDelegatedVariableMappingThrowBpmnError() {
    delegateVariableMappingThrowException();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowBpmnError() {
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

    //when completing the task continues the process which leads to calling the output mapping
    //which throws an exception
    try {
      taskService.complete(taskInSubProcess.getId());
      fail("Exeption expected!");
    } catch (ProcessEngineException pex) { //then
      Assert.assertTrue(pex.getMessage().equalsIgnoreCase("org.operaton.bpm.engine.ProcessEngineException: New process engine exception.")
              || pex.getMessage().contains("1234"));
    }

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
  public void testCallSubProcessWithDelegatedVariableMappingThrowExceptionOutput() {
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowExceptionOutput() {
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
  public void testCallSubProcessWithDelegatedVariableMappingThrowBpmnErrorOutput() {
    delegateVariableMappingThrowExceptionOutput();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityDelegateMappingTest.testCallSimpleSubProcessDelegateVarMappingExpressionThrowException.bpmn20.xml",
    "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  public void testCallSubProcessWithDelegatedVariableMappingeExpressionThrowBpmnErrorOutput() {
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
  public void testCallFailingSubProcessWithDelegatedVariableMapping() {
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
  public void testCallSubProcessWithDelegatedVariableMappingAndAsyncServiceTask() {
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
