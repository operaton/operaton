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
package org.operaton.bpm.engine.test.api.authorization;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ExecutionTree;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.BATCH;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleProcessInstanceModificationAsyncAuthorizationTest extends AuthorizationTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";

  @AfterEach
  @Override
  public void tearDown() {
    disableAuthorization();
    List<Batch> batches = managementService.createBatchQuery().list();
    for (Batch batch : batches) {
      managementService.deleteBatch(batch.getId(), true);
    }

    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      managementService.deleteJob(job.getId());
    }
    enableAuthorization();
    super.tearDown();
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testModificationWithAllPermissions() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "parallelGateway", userId, CREATE_INSTANCE, READ_INSTANCE, UPDATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);
    createGrantAuthorization(BATCH, ANY, userId, CREATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");
    String processInstanceId = processInstance.getId();
    String processDefinitionId = processInstance.getProcessDefinitionId();
    disableAuthorization();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
    enableAuthorization();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    // when
    Batch modificationBatch = runtimeService
        .createProcessInstanceModification(processInstance.getId())
        .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
        .executeAsync();
    assertThat(modificationBatch).isNotNull();
    Job job = managementService.createJobQuery().jobDefinitionId(modificationBatch.getSeedJobDefinitionId()).singleResult();
    // seed job
    managementService.executeJob(job.getId());

    // then
    for (Job pending : managementService.createJobQuery().jobDefinitionId(modificationBatch.getBatchJobDefinitionId()).list()) {
      managementService.executeJob(pending.getId());
      assertThat(pending.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());
    }

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    Assertions.assertThat(updatedTree).isNotNull();
    assertThat(updatedTree.getProcessInstanceId()).isEqualTo(processInstanceId);

    assertThat(updatedTree).hasStructure(describeActivityInstanceTree(processInstance.getProcessDefinitionId()).activity("task2").done());

    ExecutionTree executionTree = ExecutionTree.forExecution(processInstanceId, processEngine);

    assertThat(executionTree).matches(describeExecutionTree("task2").scope().done());

    // complete the process
    disableAuthorization();
    completeTasksInOrder("task2");
    testRule.assertProcessEnded(processInstanceId);
    enableAuthorization();
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testModificationWithoutBatchPermissions() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "parallelGateway", userId, CREATE_INSTANCE, READ_INSTANCE, UPDATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"));

    // when
    assertThatThrownBy(processInstanceModificationBuilder::executeAsync)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The user with id 'test' does not have")
      .hasMessageContaining("'CREATE' permission on resource 'Batch'")
      .hasMessageContaining("'CREATE_BATCH_MODIFY_PROCESS_INSTANCES' permission on resource 'Batch'");
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  @Test
  void testModificationRevoke() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, "parallelGateway", userId, CREATE_INSTANCE, READ_INSTANCE, UPDATE_INSTANCE);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, CREATE);
    createGrantAuthorization(BATCH, ANY, userId, CREATE);
    createRevokeAuthorization(BATCH, ANY, userId, CREATE_BATCH_MODIFY_PROCESS_INSTANCES);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());
    var processInstanceModificationBuilder = runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"));

    // when
    assertThatThrownBy(processInstanceModificationBuilder::executeAsync)
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The user with id 'test' does not have")
      .hasMessageContaining("'CREATE' permission on resource 'Batch'")
      .hasMessageContaining("'CREATE_BATCH_MODIFY_PROCESS_INSTANCES' permission on resource 'Batch'");
  }

  protected String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }


  protected ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    if (activityId.equals(activityInstance.getActivityId())) {
      return activityInstance;
    }

    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
      if (instance != null) {
        return instance;
      }
    }

    return null;
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertThat(!tasks.isEmpty()).as("task for activity %s does not exist".formatted(taskName)).isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }

}
