/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.rest.spi.impl.MockedProcessEngineProvider;
import org.operaton.bpm.engine.rest.util.VariablesBuilder;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;

public class ExecutionRestServiceAdHocIntegrationTest extends AbstractRestServiceTest {

  private static final String EXECUTION_URL = TEST_RESOURCE_ROOT_PATH + "/execution/{id}";
  private static final String STARTABLE_AD_HOC_ACTIVITIES_URL = EXECUTION_URL + "/ad-hoc-activities";
  private static final String TRIGGER_AD_HOC_ACTIVITIES_URL = EXECUTION_URL + "/ad-hoc-activities/trigger";
  private static final String COMPLETE_AD_HOC_SUB_PROCESS_URL = EXECUTION_URL + "/ad-hoc-activities/complete";
  private static final String PROCESS_KEY = "adHocRestIntegrationProcess";
  private static final String PROCESS_RESOURCE = "processes/ad-hoc-rest-integration.bpmn20.xml";

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  private ProcessEngine realProcessEngine;

  @BeforeEach
  void setUpRealProcessEngine() {
    ProcessEngineConfigurationImpl configuration =
        (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
    configuration.setProcessEngineName("ad-hoc-rest-integration");
    configuration.setJdbcUrl("jdbc:h2:mem:ad-hoc-rest-integration-" + System.nanoTime());
    configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    configuration.setHistory(ProcessEngineConfiguration.HISTORY_NONE);
    configuration.setJobExecutorActivate(false);

    realProcessEngine = configuration.buildProcessEngine();
    processEngine = realProcessEngine;
    MockedProcessEngineProvider.setDefaultProcessEngine(realProcessEngine);
  }

  @AfterEach
  void closeRealProcessEngine() {
    MockedProcessEngineProvider.setDefaultProcessEngine(null);
    if (realProcessEngine != null) {
      realProcessEngine.close();
      realProcessEngine = null;
    }
  }

  @Test
  void shouldExecuteAdHocLifecycleThroughRestAgainstRealEngine() {
    RepositoryService repositoryService = realProcessEngine.getRepositoryService();
    RuntimeService runtimeService = realProcessEngine.getRuntimeService();
    TaskService taskService = realProcessEngine.getTaskService();

    repositoryService.createDeployment()
        .addClasspathResource(PROCESS_RESOURCE)
        .deploy();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);
    assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());

    given().pathParam("id", adHocExecution.getId())
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("activityId", containsInAnyOrder("taskA", "taskB"))
      .body("find { it.activityId == 'taskA' }.activityName", equalTo("Task A"))
      .body("find { it.activityId == 'taskB' }.activityName", equalTo("Task B"))
      .when().get(STARTABLE_AD_HOC_ACTIVITIES_URL);

    Map<String, Object> instruction = new HashMap<>();
    instruction.put("activityId", "taskA");
    Map<String, Object> triggerPayload = new HashMap<>();
    triggerPayload.put("activities", Collections.singletonList(instruction));

    given().pathParam("id", adHocExecution.getId())
      .contentType(ContentType.JSON)
      .body(triggerPayload)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(TRIGGER_AD_HOC_ACTIVITIES_URL);

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertNotNull(taskA);
    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult());

    taskService.complete(taskA.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    Map<String, Object> completionPayload = new HashMap<>();
    completionPayload.put("variables", VariablesBuilder.create().variable("completionReason", "rest").getVariables());

    given().pathParam("id", adHocExecution.getId())
      .contentType(ContentType.JSON)
      .body(completionPayload)
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(COMPLETE_AD_HOC_SUB_PROCESS_URL);

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
    assertEquals("rest", runtimeService.getVariable(processInstance.getId(), "completionReason"));
  }
}
