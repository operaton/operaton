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
package org.operaton.bpm.engine.test.bpmn.common;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Daniel Meyer
 *
 */
public abstract class AbstractProcessEngineServicesAccessTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected TaskService taskService;

  private static final String TASK_DEF_KEY = "someTask";

  private static final String PROCESS_DEF_KEY = "testProcess";

  private static final String CALLED_PROCESS_DEF_ID = "calledProcess";

  protected List<String> deploymentIds = new ArrayList<>();

  @AfterEach
  public void tearDown() {
    for (String deploymentId : deploymentIds) {
      repositoryService.deleteDeployment(deploymentId, true);
    }

  }

  @Test
  public void testServicesAccessible() {
    // this test makes sure that the process engine services can be accessed and are non-null.
    createAndDeployModelForClass(getTestServiceAccessibleClass());

    // this would fail if api access was not assured.
    assertThatCode(() -> runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY)).doesNotThrowAnyException();
  }

  @Test
  public void testQueryAccessible() {
    // this test makes sure we can perform a query
    createAndDeployModelForClass(getQueryClass());

    // this would fail if api access was not assured.
    assertThatCode(() -> runtimeService.createProcessInstanceQuery().count()).doesNotThrowAnyException();
  }

  @Test
  public void testStartProcessInstance() {

    // given
    createAndDeployModelForClass(getStartProcessInstanceClass());

    assertStartProcessInstance();
  }

  @Test
  public void testStartProcessInstanceFails() {

    // given
    createAndDeployModelForClass(getStartProcessInstanceClass());

    assertStartProcessInstanceFails();
  }

  @Test
  public void testProcessEngineStartProcessInstance() {

    // given
    createAndDeployModelForClass(getProcessEngineStartProcessClass());

    assertStartProcessInstance();
  }

  protected void assertStartProcessInstanceFails() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CALLED_PROCESS_DEF_ID)
        .startEvent()
        .scriptTask("scriptTask")
          .scriptFormat("groovy")
          .scriptText("throw new RuntimeException(\"BOOOM!\")")
        .endEvent()
      .done();

    deployModel(modelInstance);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("BOOOM");

    // then
    // starting the process fails and everything is rolled back:
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

  protected abstract Class<?> getTestServiceAccessibleClass();

  protected abstract Class<?> getQueryClass();

  protected abstract Class<?> getStartProcessInstanceClass();

  protected abstract Class<?> getProcessEngineStartProcessClass();

  protected abstract Task createModelAccessTask(BpmnModelInstance modelInstance, Class<?> delegateClass);

  // Helper methods //////////////////////////////////////////////

  private void createAndDeployModelForClass(Class<?> delegateClass) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_DEF_KEY)
      .startEvent()
      .manualTask("templateTask")
      .endEvent()
    .done();

    // replace the template task with the actual task provided by the subtask
    modelInstance.getModelElementById("templateTask")
      .replaceWithElement(createModelAccessTask(modelInstance, delegateClass));

    deployModel(modelInstance);
  }


  private void deployModel(BpmnModelInstance model) {
    Deployment deployment = repositoryService.createDeployment().addModelInstance("testProcess.bpmn", model).deploy();
    deploymentIds.add(deployment.getId());
  }


  protected void assertStartProcessInstance() {
    deployModel(Bpmn.createExecutableProcess(CALLED_PROCESS_DEF_ID)
      .startEvent()
      .userTask(TASK_DEF_KEY)
      .endEvent()
    .done());

    // if
    runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

    // then
    // the started process instance is still active and waiting at the user task
    assertThat(taskService.createTaskQuery().taskDefinitionKey(TASK_DEF_KEY).count()).isOne();
  }

  @Test
  public void testProcessEngineStartProcessInstanceFails() {

    // given
    createAndDeployModelForClass(getProcessEngineStartProcessClass());

    assertStartProcessInstanceFails();
  }

  public static void assertCanAccessServices(ProcessEngineServices services) {
    assertThat(services.getAuthorizationService()).isNotNull();
    assertThat(services.getFormService()).isNotNull();
    assertThat(services.getHistoryService()).isNotNull();
    assertThat(services.getIdentityService()).isNotNull();
    assertThat(services.getManagementService()).isNotNull();
    assertThat(services.getRepositoryService()).isNotNull();
    assertThat(services.getRuntimeService()).isNotNull();
    assertThat(services.getTaskService()).isNotNull();
  }

  public static void assertCanPerformQuery(ProcessEngineServices services) {
    services.getRepositoryService()
      .createProcessDefinitionQuery()
      .count();
  }

  public static void assertCanStartProcessInstance(ProcessEngineServices services) {
    services.getRuntimeService().startProcessInstanceByKey(CALLED_PROCESS_DEF_ID);
  }

  public static void assertCanStartProcessInstance(ProcessEngine processEngine) {
    processEngine.getRuntimeService().startProcessInstanceByKey(CALLED_PROCESS_DEF_ID);
  }
}
