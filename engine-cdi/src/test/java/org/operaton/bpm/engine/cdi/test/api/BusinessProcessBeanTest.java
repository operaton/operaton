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
package org.operaton.bpm.engine.cdi.test.api;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.ProcessEngineCdiException;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 */
class BusinessProcessBeanTest extends CdiProcessEngineTestCase {

  /* General test asserting that the business process bean is functional */
  @Test
  @Deployment
  void test() {

    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // start the process
    businessProcess.startProcessByKey("businessProcessBeanTest").getId();

    // ensure that the process is started:
    assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNotNull();

    // ensure that there is a single task waiting
    Task task = processEngine.getTaskService().createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    String value = "value";
    businessProcess.setVariable("key", Variables.stringValue(value));
    assertThat((String)businessProcess.getVariable("key")).isEqualTo(value);

    // Typed variable API
    TypedValue typedValue = businessProcess.getVariableTyped("key");
    assertThat(typedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(typedValue.getValue()).isEqualTo(value);

    // Local variables
    String localValue = "localValue";
    businessProcess.setVariableLocal("localKey", Variables.stringValue(localValue));
    assertThat((String)businessProcess.getVariableLocal("localKey")).isEqualTo(localValue);

    // Local typed variable API
    TypedValue typedLocalValue = businessProcess.getVariableLocalTyped("localKey");
    assertThat(typedLocalValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(typedLocalValue.getValue()).isEqualTo(localValue);

    // complete the task
    assertThat(businessProcess.startTask(task.getId()).getId()).isEqualTo(task.getId());
    businessProcess.completeTask();

    // assert the task is completed
    assertThat(processEngine.getTaskService().createTaskQuery().singleResult()).isNull();

    // assert that the process is ended:
    assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNull();

  }

  @Test
  @Deployment
  void testProcessWithoutWatestate() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // start the process
    businessProcess.startProcessByKey("businessProcessBeanTest").getId();

    // assert that the process is ended:
    assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testResolveProcessInstanceBean() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    assertThat(getBeanInstance(ProcessInstance.class)).isNull();
    assertThat(getBeanInstance("processInstanceId")).isNull();
    assertThat(getBeanInstance(Execution.class)).isNull();
    assertThat(getBeanInstance("executionId")).isNull();

    String pid = businessProcess.startProcessByKey("businessProcessBeanTest").getId();

    // assert that now we can resolve the ProcessInstance-bean
    assertThat(getBeanInstance(ProcessInstance.class).getId()).isEqualTo(pid);
    assertThat(getBeanInstance("processInstanceId")).isEqualTo(pid);
    assertThat(getBeanInstance(Execution.class).getId()).isEqualTo(pid);
    assertThat(getBeanInstance("executionId")).isEqualTo(pid);

    taskService.complete(taskService.createTaskQuery().singleResult().getId());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testResolveTaskBean() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    assertThat(getBeanInstance(Task.class)).isNull();
    assertThat(getBeanInstance("taskId")).isNull();


    businessProcess.startProcessByKey("businessProcessBeanTest");
    String taskId = taskService.createTaskQuery().singleResult().getId();

    businessProcess.startTask(taskId);

    // assert that now we can resolve the Task-bean
    assertThat(getBeanInstance(Task.class).getId()).isEqualTo(taskId);
    assertThat(getBeanInstance("taskId")).isEqualTo(taskId);

    taskService.complete(taskService.createTaskQuery().singleResult().getId());
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void testGetVariableCache() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("aVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    // getting the variable cache does not empty it:
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testGetCachedVariableMap() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("aVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    // getting the variable cache does not empty it:
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void testGetAndClearVariableCache() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getAndClearVariableCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("aVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getAndClearVariableCache()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    // now the variable cache is empty
    assertThat(businessProcess.getAndClearVariableCache()).isEqualTo(Collections.emptyMap());

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getVariableCache()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testGetAndClearCachedVariableMap() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getAndClearCachedVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("aVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getAndClearCachedVariableMap()).isEqualTo(Collections.singletonMap("aVariableName", "aVariableValue"));

    // now the variable cache is empty
    assertThat(businessProcess.getAndClearCachedVariableMap()).isEqualTo(Collections.emptyMap());

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getAndClearCachedVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariable("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getAndClearCachedVariableMap()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void testGetVariableLocalCache() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.emptyMap());

    // set a variable - this should fail before the process is started
    // when/then
    assertThatThrownBy(() -> businessProcess.setVariableLocal("aVariableName", "aVariableValue"))
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("Cannot set a local cached variable: neither a Task nor an Execution is associated.");

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariableLocal("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));

    // getting the variable cache does not empty it:
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testGetCachedLocalVariableMap() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable - this should fail before the process is started
    // when/then
    assertThatThrownBy(() -> businessProcess.setVariableLocal("aVariableName", "aVariableValue"))
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("Cannot set a local cached variable: neither a Task nor an Execution is associated.");

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariableLocal("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getCachedLocalVariableMap()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));

    // getting the variable cache does not empty it:
    assertThat(businessProcess.getCachedLocalVariableMap()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testGetVariableLocal()
  {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);
    ProcessInstance processInstance = businessProcess.startProcessByKey("businessProcessBeanTest");

    TaskService taskService = getBeanInstance(TaskService.class);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(task).isNotNull();

    businessProcess.startTask(task.getId());

    businessProcess.setVariableLocal("aVariableName", "aVariableValue");

    // Flushing and re-getting should retain the value (CAM-1806):
    businessProcess.flushVariableCache();
    assertThat(businessProcess.getCachedLocalVariableMap()).isEmpty();
    assertThat((String)businessProcess.getVariableLocal("aVariableName")).isEqualTo("aVariableValue");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  @SuppressWarnings("deprecation")
  void testGetAndClearVariableLocalCache() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getAndClearVariableLocalCache()).isEqualTo(Collections.emptyMap());

    // set a variable - this should fail before the process is started
    // when/then
    assertThatThrownBy(() -> businessProcess.setVariableLocal("aVariableName", "aVariableValue"))
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("Cannot set a local cached variable: neither a Task nor an Execution is associated.");

    // the variable cache is still empty
    assertThat(businessProcess.getAndClearVariableLocalCache()).isEqualTo(Collections.emptyMap());

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariableLocal("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getVariableLocalCache()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testGetAndClearCachedLocalVariableMap() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // initially, the variable cache is empty
    assertThat(businessProcess.getAndClearCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable - this should fail before the process is started
    // when/then
    assertThatThrownBy(() -> businessProcess.setVariableLocal("aVariableName", "aVariableValue"))
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("Cannot set a local cached variable: neither a Task nor an Execution is associated.");

    // the variable cache is still empty
    assertThat(businessProcess.getAndClearCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // now the variable cache is empty again:
    assertThat(businessProcess.getAndClearCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

    // set a variable
    businessProcess.setVariableLocal("anotherVariableName", "aVariableValue");

    // now the variable is set
    assertThat(businessProcess.getAndClearCachedLocalVariableMap()).isEqualTo(Collections.singletonMap("anotherVariableName", "aVariableValue"));
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testFlushVariableCache() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // cannot flush variable cache in absence of an association:
    // when/then
    assertThatThrownBy(businessProcess::flushVariableCache)
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("Cannot flush variable cache: neither a Task nor an Execution is associated.");

    businessProcess.startProcessByKey("businessProcessBeanTest");

    // set a variable
    businessProcess.setVariable("aVariableName", "aVariable");

    // the variable is not yet present in the execution:
    assertThat(runtimeService.getVariable(businessProcess.getExecutionId(), "aVariableName")).isNull();

    // set a local variable
    businessProcess.setVariableLocal("aVariableLocalName", "aVariableLocal");

    // the local variable is not yet present in the execution:
    assertThat(runtimeService.getVariable(businessProcess.getExecutionId(), "aVariableLocalName")).isNull();

    // flush the cache
    businessProcess.flushVariableCache();

    // the variable is flushed to the execution
    assertThat(runtimeService.getVariable(businessProcess.getExecutionId(), "aVariableName")).isNotNull();

    // the local variable is flushed to the execution
    assertThat(runtimeService.getVariable(businessProcess.getExecutionId(), "aVariableLocalName")).isNotNull();

    // the cache is empty
    assertThat(businessProcess.getCachedVariableMap()).isEqualTo(Collections.emptyMap());

    // the cache is empty
    assertThat(businessProcess.getCachedLocalVariableMap()).isEqualTo(Collections.emptyMap());

  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testSaveTask() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // cannot save task in absence of an association:
    // when/then
    assertThatThrownBy(businessProcess::saveTask)
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("No task associated. Call businessProcess.startTask() first.");

    // start the process
    String processInstanceId = businessProcess.startProcessByKey("businessProcessBeanTest", Collections.singletonMap("key", (Object) "value")).getId();
    assertThat(runtimeService.getVariable(processInstanceId, "key")).isEqualTo("value");

    businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());

    // assignee is not set to jonny
    assertThat(taskService.createTaskQuery().taskAssignee("jonny").singleResult()).isNull();
    Task task = businessProcess.getTask();
    task.setAssignee("jonny");

    assertThat(taskService.createTaskQuery().taskAssignee("jonny").singleResult()).isNull();

    // if we save the task
    businessProcess.saveTask();

    // THEN

    // assignee is now set to jonny
    assertThat(taskService.createTaskQuery().taskAssignee("jonny").singleResult()).isNotNull();
    // business process is still associated with task:
    assertThat(businessProcess.isTaskAssociated()).isTrue();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  void testStopTask() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    // cannot stop task in absence of an association:
    // when/then
    assertThatThrownBy(businessProcess::stopTask)
      .isInstanceOf(ProcessEngineCdiException.class)
      .hasMessage("No task associated. Call businessProcess.startTask() first.");

    // start the process
    String processInstanceId = businessProcess.startProcessByKey("businessProcessBeanTest", Collections.singletonMap("key", (Object) "value")).getId();
    assertThat(runtimeService.getVariable(processInstanceId, "key")).isEqualTo("value");

    businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());

    // assignee is not set to jonny
    assertThat(taskService.createTaskQuery().taskAssignee("jonny").singleResult()).isNull();
    Task task = businessProcess.getTask();
    task.setAssignee("jonny");

    // if we stop the task
    businessProcess.stopTask();

    // THEN

    // assignee is not set to jonny
    assertThat(taskService.createTaskQuery().taskAssignee("jonny").singleResult()).isNull();
    // business process is not associated with task:
    assertThat(businessProcess.isTaskAssociated()).isFalse();
    assertThat(businessProcess.isAssociated()).isFalse();
  }

}
