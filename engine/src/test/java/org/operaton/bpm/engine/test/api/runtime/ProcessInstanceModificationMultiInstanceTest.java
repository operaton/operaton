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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ExecutionTree;

import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Thorben Lindhauer
 *
 */
public class ProcessInstanceModificationMultiInstanceTest {

  public static final String PARALLEL_MULTI_INSTANCE_TASK_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.parallelTasks.bpmn20.xml";
  public static final String PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.parallelSubprocess.bpmn20.xml";

  public static final String SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.sequentialTasks.bpmn20.xml";
  public static final String SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.sequentialSubprocess.bpmn20.xml";

  public static final String PARALLEL_MULTI_INSTANCE_TASK_COMPLETION_CONDITION_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.parallelTasksCompletionCondition.bpmn20.xml";
  public static final String PARALLEL_MULTI_INSTANCE_SUBPROCESS_COMPLETION_CONDITION_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.parallelSubprocessCompletionCondition.bpmn20.xml";

  public static final String NESTED_PARALLEL_MULTI_INSTANCE_TASK_PROCESS =
      "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationMultiInstanceTest.nestedParallelTasks.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeMultiInstanceBodyParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks#multiInstanceBody")
      .execute();

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miTasks")
          .activity("miTasks")
          .activity("miTasks")
          .activity("miTasks")
        .endScope()
        .beginMiBody("miTasks")
          .activity("miTasks")
          .activity("miTasks")
          .activity("miTasks")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("miTasks").concurrent().noScope().up()
            .child("miTasks").concurrent().noScope().up()
            .child("miTasks").concurrent().noScope().up().up().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("miTasks").concurrent().noScope().up()
            .child("miTasks").concurrent().noScope().up()
            .child("miTasks").concurrent().noScope().up()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "miTasks", "miTasks", "miTasks", "miTasks", "miTasks", "afterTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeMultiInstanceBodyParallelSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miSubProcess#multiInstanceBody")
      .execute();

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
        .endScope()
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope().up().up()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope().up().up()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope().up().up().up().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope().up().up()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope().up().up()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("subProcessTask", "subProcessTask", "subProcessTask",
        "subProcessTask", "subProcessTask", "afterTask", "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_COMPLETION_CONDITION_PROCESS)
  @Test
  void testStartInnerActivityParallelTasksWithCompletionCondition() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasksCompletionCondition");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks")
      .startBeforeActivity("miTasks")
      .execute();

    // then the process is able to complete successfully and respects the completion condition
    completeTasksInOrder("miTasks", "miTasks", "miTasks", "miTasks");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_COMPLETION_CONDITION_PROCESS)
  @Test
  void testStartInnerActivityParallelSubprocessWithCompletionCondition() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocessCompletionCondition");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miSubProcess")
      .startBeforeActivity("miSubProcess")
      .execute();

    // then the process is able to complete successfully and respects the completion condition
    completeTasksInOrder("subProcessTask", "subProcessTask", "subProcessTask",
        "subProcessTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeMultiInstanceBodySequentialTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks#multiInstanceBody")
      .execute();

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miTasks")
          .activity("miTasks")
        .endScope()
        .beginMiBody("miTasks")
          .activity("miTasks")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).concurrent().noScope()
          .child("miTasks").scope().up().up()
        .child(null).concurrent().noScope()
          .child("miTasks").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "miTasks", "miTasks", "miTasks", "miTasks", "miTasks", "afterTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeMultiInstanceBodySequentialSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miSubProcess#multiInstanceBody")
      .execute();

    // then
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
        .endScope()
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("subProcessTask").scope().up().up().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("subProcessTask").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("subProcessTask", "subProcessTask", "subProcessTask",
        "subProcessTask", "subProcessTask", "subProcessTask", "afterTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivityParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks", tree.getActivityInstances("miTasks#multiInstanceBody")[0].getId())
      .execute();

    // then the mi variables should be correct
    List<Execution> leafExecutions = runtimeService.createExecutionQuery().activityId("miTasks").list();
    assertThat(leafExecutions).hasSize(4);
    assertVariableSet(leafExecutions, "loopCounter", List.of(0, 1, 2, 3));
    for (Execution leafExecution : leafExecutions) {
      assertVariable(leafExecution, "nrOfInstances", 4);
      assertVariable(leafExecution, "nrOfCompletedInstances", 0);
      assertVariable(leafExecution, "nrOfActiveInstances", 4);
    }

    // and the trees should be correct
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miTasks")
          .activity("miTasks")
          .activity("miTasks")
          .activity("miTasks")
          .activity("miTasks")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child("miTasks").concurrent().noScope().up()
          .child("miTasks").concurrent().noScope().up()
          .child("miTasks").concurrent().noScope().up()
          .child("miTasks").concurrent().noScope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "miTasks", "miTasks", "miTasks", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityParallelSubprocess() {
    // given the mi body is already instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miSubProcess", tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId())
      .execute();

    // then the mi variables should be correct
    List<Execution> leafExecutions = runtimeService.createExecutionQuery().activityId("subProcessTask").list();
    assertThat(leafExecutions).hasSize(4);
    assertVariableSet(leafExecutions, "loopCounter", List.of(0, 1, 2, 3));
    for (Execution leafExecution : leafExecutions) {
      assertVariable(leafExecution, "nrOfInstances", 4);
      assertVariable(leafExecution, "nrOfCompletedInstances", 0);
      assertVariable(leafExecution, "nrOfActiveInstances", 4);
    }

    // and the trees are correct
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope().up().up()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope().up().up()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope().up().up()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("subProcessTask", "subProcessTask", "subProcessTask",
        "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodyParallelTasks() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks")
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("miTasks").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 1);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the tree should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miTasks")
          .activity("miTasks")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("miTasks").concurrent().noScope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder(
        "miTasks", "afterTask", "beforeTask", "miTasks",
        "miTasks", "miTasks", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodyParallelTasksActivityStatistics() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks")
      .execute();

    // then the activity instance statistics are correct
    List<ActivityStatistics> statistics = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId()).list();
    assertThat(statistics).hasSize(2);

    ActivityStatistics miTasksStatistics = getStatisticsForActivity(statistics, "miTasks");
    assertThat(miTasksStatistics).isNotNull();
    assertThat(miTasksStatistics.getInstances()).isEqualTo(1);

    ActivityStatistics beforeTaskStatistics = getStatisticsForActivity(statistics, "beforeTask");
    assertThat(beforeTaskStatistics).isNotNull();
    assertThat(beforeTaskStatistics.getInstances()).isEqualTo(1);
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodyParallelSubprocess() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("subProcessTask").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 1);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the tree should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder(
        "subProcessTask", "afterTask", "beforeTask", "subProcessTask",
        "subProcessTask", "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodyParallelSubprocessActivityStatistics() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .execute();

    // then the activity instance statistics are correct
    List<ActivityStatistics> statistics = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId()).list();
    assertThat(statistics).hasSize(2);

    ActivityStatistics miTasksStatistics = getStatisticsForActivity(statistics, "subProcessTask");
    assertThat(miTasksStatistics).isNotNull();
    assertThat(miTasksStatistics.getInstances()).isEqualTo(1);

    ActivityStatistics beforeTaskStatistics = getStatisticsForActivity(statistics, "beforeTask");
    assertThat(beforeTaskStatistics).isNotNull();
    assertThat(beforeTaskStatistics.getInstances()).isEqualTo(1);
  }


  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySetNrOfInstancesParallelSubprocess() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .setVariable("nrOfInstances", 3)
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("subProcessTask").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 3);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the trees should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child(null).concurrent().noScope()
              .child("subProcessTask").scope()
      .done()
      );

    // and completing the single active instance completes the mi body (even though nrOfInstances is 3;
    // joining is performed on the number of concurrent executions)
    completeTasksInOrder("subProcessTask");
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .activity("afterTask")
      .done());

    // and the remainder of the process completes successfully
    completeTasksInOrder("beforeTask", "subProcessTask", "afterTask",
        "subProcessTask", "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivitySequentialTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");
    completeTasksInOrder("beforeTask");
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks", tree.getActivityInstances("miTasks#multiInstanceBody")[0].getId());

    // when/then
    // creating a second inner instance is not possible
    assertThatThrownBy(processInstanceModificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Concurrent instantiation not possible for activities in scope miTasks#multiInstanceBody");
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivitySequentialSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
    completeTasksInOrder("beforeTask");
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService
        .createProcessInstanceModification(processInstance.getId())
        .startBeforeActivity("miSubProcess", tree.getActivityInstances("miSubProcess#multiInstanceBody")[0].getId());

    // when/then
    assertThatThrownBy(processInstanceModificationBuilder::execute)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Concurrent instantiation not possible for activities in scope miSubProcess#multiInstanceBody");
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySequentialTasks() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks")
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("miTasks").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 1);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the trees should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miTasks")
          .activity("miTasks")
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child("miTasks").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "afterTask",
        "beforeTask", "miTasks", "miTasks", "miTasks", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySequentialTasksActivityStatistics() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("miTasks")
      .execute();

    // then the activity instance statistics are correct
    List<ActivityStatistics> statistics = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId()).list();
    assertThat(statistics).hasSize(2);

    ActivityStatistics miTasksStatistics = getStatisticsForActivity(statistics, "miTasks");
    assertThat(miTasksStatistics).isNotNull();
    assertThat(miTasksStatistics.getInstances()).isEqualTo(1);

    ActivityStatistics beforeTaskStatistics = getStatisticsForActivity(statistics, "beforeTask");
    assertThat(beforeTaskStatistics).isNotNull();
    assertThat(beforeTaskStatistics.getInstances()).isEqualTo(1);
  }

  protected ActivityStatistics getStatisticsForActivity(List<ActivityStatistics> statistics, String activityId) {
    for (ActivityStatistics statisticsInstance : statistics) {
      if (statisticsInstance.getId().equals(activityId)) {
        return statisticsInstance;
      }
    }
    return null;
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySequentialSubprocess() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("subProcessTask").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 1);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the trees should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("subProcessTask").scope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("subProcessTask", "afterTask",
        "beforeTask", "subProcessTask", "subProcessTask", "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySequentialSubprocessActivityStatistics() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .execute();

    // then the activity instance statistics are correct
    List<ActivityStatistics> statistics = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId()).list();
    assertThat(statistics).hasSize(2);

    ActivityStatistics miTasksStatistics = getStatisticsForActivity(statistics, "subProcessTask");
    assertThat(miTasksStatistics).isNotNull();
    assertThat(miTasksStatistics.getInstances()).isEqualTo(1);

    ActivityStatistics beforeTaskStatistics = getStatisticsForActivity(statistics, "beforeTask");
    assertThat(beforeTaskStatistics).isNotNull();
    assertThat(beforeTaskStatistics.getInstances()).isEqualTo(1);
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testStartBeforeInnerActivityWithMiBodySetNrOfInstancesSequentialSubprocess() {
    // given the mi body is not yet instantiated
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("subProcessTask")
      .setVariable("nrOfInstances", 3)
      .execute();

    // then the mi variables should be correct
    Execution leafExecution = runtimeService.createExecutionQuery().activityId("subProcessTask").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 0);
    assertVariable(leafExecution, "nrOfInstances", 3);
    assertVariable(leafExecution, "nrOfCompletedInstances", 0);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    // and the trees should be correct
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child("beforeTask").concurrent().noScope().up()
        .child(null).concurrent().noScope()
          .child(null).scope()
            .child("subProcessTask").scope()
      .done()
      );

    // and two following sequential instances should be created
    completeTasksInOrder("subProcessTask");

    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .activity("beforeTask")
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
      .done());

    leafExecution = runtimeService.createExecutionQuery().activityId("subProcessTask").singleResult();
    assertThat(leafExecution).isNotNull();
    assertVariable(leafExecution, "loopCounter", 1);
    assertVariable(leafExecution, "nrOfInstances", 3);
    assertVariable(leafExecution, "nrOfCompletedInstances", 1);
    assertVariable(leafExecution, "nrOfActiveInstances", 1);

    completeTasksInOrder("subProcessTask");

    // and the remainder of the process completes successfully
    completeTasksInOrder("subProcessTask", "beforeTask", "subProcessTask",
        "subProcessTask", "subProcessTask", "afterTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelMultiInstanceBodyParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miTasks#multiInstanceBody")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testCancelMultiInstanceBodyParallelSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miSubProcess#multiInstanceBody")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelMultiInstanceBodySequentialTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miTasks#multiInstanceBody")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testCancelMultiInstanceBodySequentialSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miSubProcess#multiInstanceBody")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelInnerActivityParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(tree.getActivityInstances("miTasks")[0].getId())
      .execute();

    // then
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miTasks")
          .activity("miTasks")
          .activity("miTasks")
        .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child("miTasks").concurrent().noScope().up()
          .child("miTasks").concurrent().noScope().up()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "miTasks", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelAllInnerActivityParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miTasks")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = NESTED_PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelAllInnerActivityNestedParallelTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedMiParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miTasks")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  /**
   * Ensures that the modification cmd does not prune the last concurrent execution
   * because parallel MI requires this
   */
  @Deployment(resources = PARALLEL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelInnerActivityParallelTasksAllButOne() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(tree.getActivityInstances("miTasks")[0].getId())
      .cancelActivityInstance(tree.getActivityInstances("miTasks")[1].getId())
      .execute();

    // then
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miTasks")
          .activity("miTasks")
        .endScope()
      .done());

    // the execution tree should still be in the expected shape
    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child("miTasks").concurrent().noScope()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("miTasks", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = PARALLEL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testCancelInnerActivityParallelSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(tree.getActivityInstances("miSubProcess")[0].getId())
      .execute();

    // then
    tree = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(tree).hasStructure(
      describeActivityInstanceTree(processInstance.getProcessDefinitionId())
        .beginMiBody("miSubProcess")
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
          .beginScope("miSubProcess")
            .activity("subProcessTask")
          .endScope()
      .done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstance.getId(), processEngine);
    assertThat(executionTree).matches(
      describeExecutionTree(null).scope()
        .child(null).scope()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope().up().up()
          .child(null).concurrent().noScope()
            .child("subProcessTask").scope().up().up()
      .done()
      );

    // and the process is able to complete successfully
    completeTasksInOrder("subProcessTask", "subProcessTask", "afterTask");
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelInnerActivitySequentialTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(tree.getActivityInstances("miTasks")[0].getId())
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_TASK_PROCESS)
  @Test
  void testCancelAllInnerActivitySequentialTasks() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialUserTasks");
    completeTasksInOrder("beforeTask");

    // when
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelAllForActivity("miTasks")
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = SEQUENTIAL_MULTI_INSTANCE_SUBPROCESS_PROCESS)
  @Test
  void testCancelInnerActivitySequentialSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess");
    completeTasksInOrder("beforeTask");

    // when
    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(tree.getActivityInstances("miSubProcess")[0].getId())
      .execute();

    // then
    testRule.assertProcessEnded(processInstance.getId());
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertThat(!tasks.isEmpty()).as("task for activity %s does not exist".formatted(taskName)).isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }


  protected void assertVariable(Execution execution, String variableName, Object expectedValue) {
    Object variableValue = runtimeService.getVariable(execution.getId(), variableName);
    assertThat(variableValue).as("Value for variable '%s' and %s does not match.".formatted(variableName, execution)).isEqualTo(expectedValue);
  }

  protected void assertVariableSet(List<Execution> executions, String variableName, List<?> expectedValues) {
    List<Object> actualValues = new ArrayList<>();
    for (Execution execution : executions) {
      actualValues.add(runtimeService.getVariable(execution.getId(), variableName));
    }

    for (Object expectedValue : expectedValues) {
      boolean valueFound = actualValues.remove(expectedValue);
      assertThat(valueFound).as("Expected variable value '%s' not contained in the list of actual values. Unmatched actual values: %s".formatted(expectedValue, actualValues)).isTrue();
    }
    assertThat(actualValues).as("There are more actual than expected values.").isEmpty();
  }

}
