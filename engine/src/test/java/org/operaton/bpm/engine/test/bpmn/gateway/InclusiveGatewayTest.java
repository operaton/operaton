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
package org.operaton.bpm.engine.test.bpmn.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Joram Barrez
 * @author Tom Van Buskirk
 * @author Tijs Rademakers
 */
class InclusiveGatewayTest {

  private static final String TASK1_NAME = "Task 1";
  private static final String TASK2_NAME = "Task 2";
  private static final String TASK3_NAME = "Task 3";

  private static final String BEAN_TASK1_NAME = "Basic service";
  private static final String BEAN_TASK2_NAME = "Standard service";
  private static final String BEAN_TASK3_NAME = "Gold Member service";

  protected static final String ASYNC_CONCURRENT_PARALLEL_GATEWAY =
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.AsyncConcurrentExecutions.ParallelGateway.bpmn";
  protected static final String ASYNC_CONCURRENT_PARALLEL_INCLUSIVE_GATEWAY =
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.AsyncConcurrentExecutions.ParallelInclusiveGateway.bpmn";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  HistoryService historyService;

  @Deployment
  @Test
  void testDivergingInclusiveGateway() {
    for (int i = 1; i <= 3; i++) {
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("input", i));
      List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
      List<String> expectedNames = new ArrayList<>();
      if (i == 1) {
        expectedNames.add(TASK1_NAME);
      }
      if (i <= 2) {
        expectedNames.add(TASK2_NAME);
      }
      expectedNames.add(TASK3_NAME);
      for (Task task : tasks) {
        System.out.println("task " + task.getName());
      }
      assertThat(tasks).hasSize(4 - i);
      for (Task task : tasks) {
        expectedNames.remove(task.getName());
      }
      assertThat(expectedNames).isEmpty();
      runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }
  }

  @Deployment
  @Test
  void testMergingInclusiveGateway() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
    assertThat(taskService.createTaskQuery().count()).isOne();

    runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
  }

  @Deployment
  @Test
  void testMergingInclusiveGatewayAsync() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      managementService.executeJob(job.getId());
    }
    assertThat(taskService.createTaskQuery().count()).isOne();

    runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
  }

  @Deployment
  @Test
  void testPartialMergingInclusiveGateway() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("partialInclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
    Task partialTask = taskService.createTaskQuery().singleResult();
    assertThat(partialTask.getTaskDefinitionKey()).isEqualTo("partialTask");

    taskService.complete(partialTask.getId());

    Task fullTask = taskService.createTaskQuery().singleResult();
    assertThat(fullTask.getTaskDefinitionKey()).isEqualTo("theTask");

    runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
  }

  @Deployment
  @Test
  void testNoSequenceFlowSelected() {
    // given
    var variables = CollectionUtil.singletonMap("input", 4);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGwNoSeqFlowSelected", variables))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("ENGINE-02004 No outgoing sequence flow for the element with id 'inclusiveGw' could be selected for continuing the process.");
  }

  /**
   * Test for ACT-1216: When merging a concurrent execution the parent is not activated correctly
   */
  @Deployment
  @Test
  void testParentActivationOnNonJoiningEnd() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parentActivationOnNonJoiningEnd");

    List<Execution> executionsBefore = runtimeService.createExecutionQuery().list();
    assertThat(executionsBefore).hasSize(3);

    // start first round of tasks
    List<Task> firstTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

    assertThat(firstTasks).hasSize(2);

    for (Task t: firstTasks) {
      taskService.complete(t.getId());
    }

    // start first round of tasks
    List<Task> secondTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

    assertThat(secondTasks).hasSize(2);

    // complete one task
    Task task = secondTasks.get(0);
    taskService.complete(task.getId());

    // should have merged last child execution into parent
    List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
    assertThat(executionsAfter).hasSize(1);

    Execution execution = executionsAfter.get(0);

    // and should have one active activity
    List<String> activeActivityIds = runtimeService.getActiveActivityIds(execution.getId());
    assertThat(activeActivityIds).hasSize(1);

    // Completing last task should finish the process instance

    Task lastTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(lastTask.getId());

    assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();
  }

  /**
   * Test for bug ACT-10: whitespaces/newlines in expressions lead to exceptions
   */
  @Deployment
  @Test
  void testWhitespaceInExpression() {
    // Starting a process instance will lead to an exception if whitespace are
    // incorrectly handled
    var variables = CollectionUtil.singletonMap("input", 1);
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("inclusiveWhiteSpaceInExpression", variables))
      .doesNotThrowAnyException();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testDivergingInclusiveGateway.bpmn20.xml"})
  @Test
  void testUnknownVariableInExpression() {
    // given
    var variables = CollectionUtil.singletonMap("iinput", 1);
    // Instead of 'input' we're starting a process instance with the name
    // 'iinput' (i.e. a typo)

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", variables))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Unknown property used in expression");
  }

  @Deployment
  @Test
  void testDecideBasedOnBeanProperty() {
    runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanProperty", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(150)));
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    Map<String, String> expectedNames = new HashMap<>();
    expectedNames.put(BEAN_TASK2_NAME, BEAN_TASK2_NAME);
    expectedNames.put(BEAN_TASK3_NAME, BEAN_TASK3_NAME);
    for (Task task : tasks) {
      expectedNames.remove(task.getName());
    }
    assertThat(expectedNames).isEmpty();
  }

  @Deployment
  @Test
  void testDecideBasedOnListOrArrayOfBeans() {
    List<InclusiveGatewayTestOrder> orders = new ArrayList<>();
    orders.add(new InclusiveGatewayTestOrder(50));
    orders.add(new InclusiveGatewayTestOrder(300));
    orders.add(new InclusiveGatewayTestOrder(175));

    ProcessInstance pi;
    var variables = CollectionUtil.singletonMap("orders", orders);
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", variables)).isInstanceOf(ProcessEngineException.class);

    orders.set(1, new InclusiveGatewayTestOrder(175));
    pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", variables);
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(BEAN_TASK3_NAME);

    orders.set(1, new InclusiveGatewayTestOrder(125));
    pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", variables);
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);
    List<String> expectedNames = new ArrayList<>();
    expectedNames.add(BEAN_TASK2_NAME);
    expectedNames.add(BEAN_TASK3_NAME);
    for (Task t : tasks) {
      expectedNames.remove(t.getName());
    }
    assertThat(expectedNames).isEmpty();

    // Arrays are usable in exactly the same way
    InclusiveGatewayTestOrder[] orderArray = orders.toArray(new InclusiveGatewayTestOrder[0]);
    orderArray[1].setPrice(10);
    pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orderArray));
    tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(3);
    expectedNames.clear();
    expectedNames.add(BEAN_TASK1_NAME);
    expectedNames.add(BEAN_TASK2_NAME);
    expectedNames.add(BEAN_TASK3_NAME);
    for (Task t : tasks) {
      expectedNames.remove(t.getName());
    }
    assertThat(expectedNames).isEmpty();
  }

  @Deployment
  @Test
  void testDecideBasedOnBeanMethod() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod",
            CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(200)));
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo(BEAN_TASK3_NAME);

    pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod",
            CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(125)));
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(2);
    List<String> expectedNames = new ArrayList<>();
    expectedNames.add(BEAN_TASK2_NAME);
    expectedNames.add(BEAN_TASK3_NAME);
    for (Task t : tasks) {
      expectedNames.remove(t.getName());
    }
    assertThat(expectedNames).isEmpty();
    var variables = CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(300));

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", variables)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment
  @Test
  void testInvalidMethodExpression() {
    // given
    var variables = CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(50));

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveInvalidMethodExpression", variables))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Unknown method used in expression");
  }

  @Deployment
  @Test
  void testDefaultSequenceFlow() {
    // Input == 1 -> default is not selected, other 2 tasks are selected
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 1));
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(2);
    Map<String, String> expectedNames = new HashMap<>();
    expectedNames.put("Input is one", "Input is one");
    expectedNames.put("Input is three or one", "Input is three or one");
    for (Task t : tasks) {
      expectedNames.remove(t.getName());
    }
    assertThat(expectedNames).isEmpty();
    runtimeService.deleteProcessInstance(pi.getId(), null);

    // Input == 3 -> default is not selected, "one or three" is selected
    pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 3));
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Input is three or one");

    // Default input
    pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 5));
    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Default input");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testDefaultSequenceFlow.bpmn20.xml")
  @Test
  void testDefaultSequenceFlowExecutionIsActive() {
    // given a triggered inclusive gateway default flow
    runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 5));

    // then the process instance execution is not deactivated
    ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().singleResult();
    assertThat(execution.getActivityId()).isEqualTo("theTask2");
    assertThat(execution.isActive()).isTrue();
  }

  /**
   * 1. or split
   * 2. or join
   * 3. that same or join splits again (in this case has a single default sequence flow)
   */
  @Deployment
  @Test
  void testSplitMergeSplit() {
    // given a process instance with two concurrent tasks
    ProcessInstance processInstance =
        runtimeService.startProcessInstanceByKey("inclusiveGwSplitAndMerge", CollectionUtil.singletonMap("input", 1));

    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(2);

    // when the executions are joined at an inclusive gateway and the gateway itself has an outgoing default flow
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // then the task after the inclusive gateway is reached by the process instance execution (i.e. concurrent root)
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    assertThat(task.getExecutionId()).isEqualTo(processInstance.getId());
  }


  @Deployment
  @Test
  void testNoIdOnSequenceFlow() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 3));
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Input is more than one");

    // Both should be enabled on 1
    pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 1));
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(2);
    Map<String, String> expectedNames = new HashMap<>();
    expectedNames.put("Input is one", "Input is one");
    expectedNames.put("Input is more than one", "Input is more than one");
    for (Task t : tasks) {
      expectedNames.remove(t.getName());
    }
    assertThat(expectedNames).isEmpty();
  }

  /** This test the isReachable() check that is done to check if
   * upstream tokens can reach the inclusive gateway.
   * <p>
   * In case of loops, special care needs to be taken in the algorithm,
   * or else stackoverflows will happen very quickly.
   * </p>
   */
  @Deployment
  @Test
  void testLoop() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveTestLoop",
            CollectionUtil.singletonMap("counter", 1));

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task C");

    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().count()).isZero();


    for (Execution execution : runtimeService.createExecutionQuery().list()) {
      System.out.println(((ExecutionEntity) execution).getActivityId());
    }

    assertThat(runtimeService.createExecutionQuery().count()).as("Found executions: " + runtimeService.createExecutionQuery().list()).isZero();
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testJoinAfterSubprocesses() {
    // Test case to test act-1204
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("a", 1);
    variableMap.put("b", 1);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
    assertThat(processInstance.getId()).isNotNull();

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    taskService.complete(tasks.get(0).getId());
    assertThat(taskService.createTaskQuery().count()).isOne();

    taskService.complete(tasks.get(1).getId());

    Task task = taskService.createTaskQuery().taskAssignee("c").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(processInstance).isNull();

    variableMap = new HashMap<>();
    variableMap.put("a", 1);
    variableMap.put("b", 2);
    processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
    assertThat(processInstance.getId()).isNotNull();

    tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(taskService.createTaskQuery().count()).isOne();

    task = tasks.get(0);
    assertThat(task.getAssignee()).isEqualTo("a");
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().taskAssignee("c").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

    processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(processInstance).isNull();

    variableMap = new HashMap<>();
    variableMap.put("a", 2);
    variableMap.put("b", 2);
    try {
      runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
      fail("");
    } catch(ProcessEngineException e) {
      assertThat(e.getMessage()).contains("No outgoing sequence flow");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCall.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCallSubProcess.bpmn20.xml"})
  @Test
  void testJoinAfterCall() {
    // Test case to test act-1026
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGatewayAfterCall");
    assertThat(processInstance.getId()).isNotNull();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

    // now complete task A and check number of remaining tasks.
    // inclusive gateway should wait for the "Task B" and "Task C"
    Task taskA = taskService.createTaskQuery().taskName("Task A").singleResult();
    assertThat(taskA).isNotNull();
    taskService.complete(taskA.getId());
    assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

    // now complete task B and check number of remaining tasks
    // inclusive gateway should wait for "Task C"
    Task taskB = taskService.createTaskQuery().taskName("Task B").singleResult();
    assertThat(taskB).isNotNull();
    taskService.complete(taskB.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();

    // now complete task C. Gateway activates and "Task C" remains
    Task taskC = taskService.createTaskQuery().taskName("Task C").singleResult();
    assertThat(taskC).isNotNull();
    taskService.complete(taskC.getId());
    assertThat(taskService.createTaskQuery().count()).isOne();

    // check that remaining task is in fact task D
    Task taskD = taskService.createTaskQuery().taskName("Task D").singleResult();
    assertThat(taskD).isNotNull();
    assertThat(taskD.getName()).isEqualTo("Task D");
    taskService.complete(taskD.getId());

    processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(processInstance).isNull();
  }

  /**
   * The test process has an OR gateway where, the 'input' variable is used to
   * select the expected outgoing sequence flow.
   */
  @Deployment
  @Test
  void testDecisionFunctionality() {

    Map<String, Object> variables = new HashMap<>();

    // Test with input == 1
    variables.put("input", 1);
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(3);
    Map<String, String> expectedMessages = new HashMap<>();
    expectedMessages.put(TASK1_NAME, TASK1_NAME);
    expectedMessages.put(TASK2_NAME, TASK2_NAME);
    expectedMessages.put(TASK3_NAME, TASK3_NAME);
    for (Task task : tasks) {
      expectedMessages.remove(task.getName());
    }
    assertThat(expectedMessages).isEmpty();

    // Test with input == 2
    variables.put("input", 2);
    pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
    tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(2);
    expectedMessages = new HashMap<>();
    expectedMessages.put(TASK2_NAME, TASK2_NAME);
    expectedMessages.put(TASK3_NAME, TASK3_NAME);
    for (Task task : tasks) {
      expectedMessages.remove(task.getName());
    }
    assertThat(expectedMessages).isEmpty();

    // Test with input == 3
    variables.put("input", 3);
    pi = runtimeService.startProcessInstanceByKey("inclusiveGateway", variables);
    tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    assertThat(tasks).hasSize(1);
    expectedMessages = new HashMap<>();
    expectedMessages.put(TASK3_NAME, TASK3_NAME);
    for (Task task : tasks) {
      expectedMessages.remove(task.getName());
    }
    assertThat(expectedMessages).isEmpty();

    // Test with input == 4
    variables.put("input", 4);
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGateway", variables)).isInstanceOf(ProcessEngineException.class);

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterSequentialMultiInstanceSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterParallelMultiInstanceSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterNestedScopes.bpmn20.xml"
  })
  void testJoin(String bpmnResource) {
    // given
    testRule.deploy(bpmnResource);
    runtimeService.startProcessInstanceByKey("process");

    TaskQuery query = taskService.createTaskQuery();

    // when
    Task task = query
        .taskDefinitionKey("task")
        .singleResult();
    taskService.complete(task.getId());

    // then
    assertThat(query.taskDefinitionKey("taskAfterJoin").singleResult()).isNull();
  }

  @Test
  void testTriggerGatewayWithEnoughArrivedTokens() {
   testRule.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("beforeTask")
      .inclusiveGateway("gw")
      .userTask("afterTask")
      .endEvent()
      .done());

    // given
    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("process")
      .startBeforeActivity("beforeTask")
      .startBeforeActivity("beforeTask")
      .execute();

    Task task = taskService.createTaskQuery().list().get(0);

    // when
    taskService.complete(task.getId());

    // then
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    ActivityInstanceAssert.assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .activity("afterTask")
      .done());
  }

  @Deployment
  @Test
  void testLoopingInclusiveGateways() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstance.getId());

    // then
    ActivityInstanceAssert.assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("task1")
        .activity("task2")
        .activity("inclusiveGw3")
      .done());
  }

  @Test
  void testRemoveConcurrentExecutionLocalVariablesOnJoin() {
   testRule.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .inclusiveGateway("fork")
      .userTask("task1")
      .inclusiveGateway("join")
      .userTask("afterTask")
      .endEvent()
      .moveToNode("fork")
      .userTask("task2")
      .connectTo("join")
      .done());

    // given
    runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      runtimeService.setVariableLocal(task.getExecutionId(), "var", "value");
    }

    // when
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterEventBasedGateway.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterEventBasedGatewayInSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterEventBasedGatewayContainedInSubProcess.bpmn20.xml"
  })
  void testJoinAfterEventBasedGateway(String resource) {
    // given
    testRule.deploy(resource);
    TaskQuery taskQuery = taskService.createTaskQuery();

    runtimeService.startProcessInstanceByKey("process");
    Task task = taskQuery.singleResult();
    taskService.complete(task.getId());

    // assume
    assertThat(taskQuery.singleResult()).isNull();

    // when
    runtimeService.correlateMessage("foo");

    // then
    task = taskQuery.singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterJoin");
  }

  @Deployment(resources = ASYNC_CONCURRENT_PARALLEL_GATEWAY)
  @Test
  void shouldCompleteWithConcurrentExecution_ParallelGateway() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    // when
    testRule.executeAvailableJobs(1);

    // then
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().singleResult().getState())
        .isEqualTo("COMPLETED");
  }

  @Deployment(resources = ASYNC_CONCURRENT_PARALLEL_INCLUSIVE_GATEWAY)
  @Test
  void shouldCompleteWithConcurrentExecution_InclusiveGateway() {
    // given
    runtimeService.startProcessInstanceByKey("process");

    // when
    testRule.executeAvailableJobs(1);

    // then
    assertThat(managementService.createJobQuery().count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().singleResult().getState())
        .isEqualTo("COMPLETED");
  }

}
