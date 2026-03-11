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
package org.operaton.bpm.engine.rest;

import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ExternalTaskTopicNamesRestServiceTest extends AbstractRestServiceTest {
  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String EXTERNAL_TASK_QUERY_URL = TEST_RESOURCE_ROOT_PATH + "/external-task";
  protected static final String GET_EXTERNAL_TASK_TOPIC_NAMES_URL = EXTERNAL_TASK_QUERY_URL + "/topic-names";

  protected static final String WITH_LOCKED_TASKS = "withLockedTasks";
  protected static final String WITH_UNLOCKED_TASKS = "withUnlockedTasks";
  protected static final String WITH_RETRIES_LEFT = "withRetriesLeft";

  @BeforeEach
  void setupMocks(){
    when(processEngine.getExternalTaskService().getTopicNames(false,false,false)).thenReturn(List.of("allTopics"));
    when(processEngine.getExternalTaskService().getTopicNames(true,false,false)).thenReturn(List.of("lockedTasks"));
    when(processEngine.getExternalTaskService().getTopicNames(false,true,false)).thenReturn(List.of("unlockedTasks"));
    when(processEngine.getExternalTaskService().getTopicNames(false,false,true)).thenReturn(List.of("withRetriesLeft"));
  }

  @Test
  void testGetTopicNames(){
    Response response = given()
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(GET_EXTERNAL_TASK_TOPIC_NAMES_URL);

    String content = response.asString();
    List<Map<String, Object>> topicNames = from(content).getList("");

    assertThat(topicNames).first()
      .isInstanceOf(String.class).isEqualTo("allTopics");
  }

  @Test
  void testGetTopicNamesOfLockedTasks(){
    Response response = given()
        .header("accept", MediaType.APPLICATION_JSON)
        .param(WITH_LOCKED_TASKS, true)
        .param(WITH_UNLOCKED_TASKS, false)
        .param(WITH_RETRIES_LEFT, false)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(GET_EXTERNAL_TASK_TOPIC_NAMES_URL);

    String content = response.asString();
    List<Map<String, Object>> topicNames = from(content).getList("");

    assertThat(topicNames).first()
      .isInstanceOf(String.class).isEqualTo("lockedTasks");
  }

  @Test
  void testGetTopicNamesOfUnlockedTasks(){
    Response response = given()
        .header("accept", MediaType.APPLICATION_JSON)
        .param(WITH_LOCKED_TASKS, false)
        .param(WITH_UNLOCKED_TASKS, true)
        .param(WITH_RETRIES_LEFT, false)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(GET_EXTERNAL_TASK_TOPIC_NAMES_URL);

    String content = response.asString();
    List<Map<String, Object>> topicNames = from(content).getList("");

    assertThat(topicNames).first()
      .isInstanceOf(String.class).isEqualTo("unlockedTasks");
  }

  @Test
  void testGetTopicNamesOfTasksWithRetriesLeft(){
    Response response = given()
        .header("accept", MediaType.APPLICATION_JSON)
        .param(WITH_LOCKED_TASKS, false)
        .param(WITH_UNLOCKED_TASKS, false)
        .param(WITH_RETRIES_LEFT, true)
        .then()
        .expect()
        .statusCode(Status.OK.getStatusCode())
        .when()
        .get(GET_EXTERNAL_TASK_TOPIC_NAMES_URL);

    String content = response.asString();
    List<Map<String, Object>> topicNames = from(content).getList("");

    assertThat(topicNames).first()
      .isInstanceOf(String.class).isEqualTo("withRetriesLeft");
  }

}
