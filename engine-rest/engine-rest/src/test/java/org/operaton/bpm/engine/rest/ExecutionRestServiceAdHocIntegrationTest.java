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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

  @Test
  void shouldDiscoverAndActivateMigratedEnabledTokenThroughRest() {
    RepositoryService repositoryService = realProcessEngine.getRepositoryService();
    RuntimeService runtimeService = realProcessEngine.getRuntimeService();
    TaskService taskService = realProcessEngine.getTaskService();
    String xml = """
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="test">
          <process id="enabledRest" isExecutable="true" operaton:historyTimeToLive="180">
            <startEvent id="start"/><sequenceFlow id="enter" sourceRef="start" targetRef="adhoc"/>
            <adHocSubProcess id="adhoc" ordering="Sequential">
              <userTask id="a"/><userTask id="b"/>
              <sequenceFlow id="ab" sourceRef="a" targetRef="b"/>
            </adHocSubProcess>
            <sequenceFlow id="leave" sourceRef="adhoc" targetRef="after"/><userTask id="after"/>
          </process>
        </definitions>
        """;
    String sourceDeployment = repositoryService.createDeployment().addString("enabled.bpmn", xml).deploy().getId();
    String source = repositoryService.createProcessDefinitionQuery().deploymentId(sourceDeployment).singleResult().getId();
    ProcessInstance instance = runtimeService.startProcessInstanceById(source);
    String scope = runtimeService.createExecutionQuery().processInstanceId(instance.getId())
        .activityId("adhoc").singleResult().getId();
    runtimeService.triggerAdHocActivities(scope, Collections.singletonList("a"), null);
    taskService.complete(taskService.createTaskQuery().taskDefinitionKey("a").singleResult().getId());
    String enabledExecution = runtimeService.getStartableAdHocActivities(scope).stream()
        .filter(activity -> "b".equals(activity.getActivityId())).findFirst().orElseThrow()
        .getEnabledExecutionIds().get(0);
    String targetDeployment = repositoryService.createDeployment()
        .addString("enabled.bpmn", xml.replace("id=\"b\"", "id=\"renamed\"").replace("targetRef=\"b\"", "targetRef=\"renamed\""))
        .deploy().getId();
    String target = repositoryService.createProcessDefinitionQuery().deploymentId(targetDeployment).singleResult().getId();
    runtimeService.newMigration(runtimeService.createMigrationPlan(source, target).mapEqualActivities()
        .mapActivities("b", "renamed").build()).processInstanceIds(instance.getId()).execute();

    given().pathParam("id", scope)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .body("find { it.activityId == 'a' }.starterActivity", equalTo(true))
      .body("find { it.activityId == 'a' }.enabledExecutionIds", hasSize(0))
      .body("find { it.activityId == 'renamed' }.starterActivity", equalTo(false))
      .body("find { it.activityId == 'renamed' }.enabledExecutionIds", containsInAnyOrder(enabledExecution))
      .when().get(STARTABLE_AD_HOC_ACTIVITIES_URL);
    assertThat(taskService.createTaskQuery().count()).isZero();
    given().pathParam("id", scope).contentType(ContentType.JSON)
      .body(Map.of("activities", Collections.singletonList(Map.of("activityId", "renamed"))))
      .then().expect().statusCode(Status.NO_CONTENT.getStatusCode())
      .when().post(TRIGGER_AD_HOC_ACTIVITIES_URL);
    Task task = taskService.createTaskQuery().taskDefinitionKey("renamed").singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isEqualTo(1);
  }

}
