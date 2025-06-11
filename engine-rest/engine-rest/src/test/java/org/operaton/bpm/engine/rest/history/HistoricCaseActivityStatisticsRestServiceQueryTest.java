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
package org.operaton.bpm.engine.rest.history;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.history.HistoricCaseActivityStatistics;
import org.operaton.bpm.engine.history.HistoricCaseActivityStatisticsQuery;
import org.operaton.bpm.engine.impl.HistoricCaseActivityStatisticsQueryImpl;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import io.restassured.response.Response;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricCaseActivityStatisticsRestServiceQueryTest extends AbstractRestServiceTest {

  @ClassRule
  public static TestContainerRule rule = new TestContainerRule();

  protected static final String HISTORY_URL = TEST_RESOURCE_ROOT_PATH + "/history";
  protected static final String HISTORIC_CASE_ACTIVITY_STATISTICS_URL = HISTORY_URL + "/case-definition/{id}/statistics";

  protected static HistoricCaseActivityStatisticsQuery historicCaseActivityStatisticsQuery;

  @Before
  public void setUpRuntimeData() {
    List<HistoricCaseActivityStatistics> mocks = MockProvider.createMockHistoricCaseActivityStatistics();

    historicCaseActivityStatisticsQuery = mock(HistoricCaseActivityStatisticsQueryImpl.class);
    when(processEngine.getHistoryService().createHistoricCaseActivityStatisticsQuery(MockProvider.EXAMPLE_CASE_DEFINITION_ID)).thenReturn(historicCaseActivityStatisticsQuery);
    when(historicCaseActivityStatisticsQuery.unlimitedList()).thenReturn(mocks);
  }

  @Test
  public void testHistoricCaseActivityStatisticsRetrieval() {
    given().pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .body("$.size()", is(2))
      .body("id", hasItems(MockProvider.EXAMPLE_ACTIVITY_ID, MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID))
    .when().get(HISTORIC_CASE_ACTIVITY_STATISTICS_URL);
  }

  @Test
  public void testSimpleTaskQuery() {
    Response response = given()
          .pathParam("id", MockProvider.EXAMPLE_CASE_DEFINITION_ID)
         .then().expect()
           .statusCode(Status.OK.getStatusCode())
      .when().get(HISTORIC_CASE_ACTIVITY_STATISTICS_URL);

    String content = response.asString();
    List<String> result = from(content).getList("");
    Assert.assertEquals(2, result.size());

    assertThat(result.get(0)).isNotNull();
    assertThat(result.get(1)).isNotNull();

    String id = from(content).getString("[0].id");
    long available = from(content).getLong("[0].available");
    long active = from(content).getLong("[0].active");
    long completed = from(content).getLong("[0].completed");
    long disabled = from(content).getLong("[0].disabled");
    long enabled = from(content).getLong("[0].enabled");
    long terminated = from(content).getLong("[0].terminated");

    Assert.assertEquals(MockProvider.EXAMPLE_ACTIVITY_ID, id);
    Assert.assertEquals(MockProvider.EXAMPLE_AVAILABLE_LONG, available);
    Assert.assertEquals(MockProvider.EXAMPLE_ACTIVE_LONG, active);
    Assert.assertEquals(MockProvider.EXAMPLE_COMPLETED_LONG, completed);
    Assert.assertEquals(MockProvider.EXAMPLE_DISABLED_LONG, disabled);
    Assert.assertEquals(MockProvider.EXAMPLE_ENABLED_LONG, enabled);
    Assert.assertEquals(MockProvider.EXAMPLE_TERMINATED_LONG, terminated);

    id = from(content).getString("[1].id");
    available = from(content).getLong("[1].available");
    active = from(content).getLong("[1].active");
    completed = from(content).getLong("[1].completed");
    disabled = from(content).getLong("[1].disabled");
    enabled = from(content).getLong("[1].enabled");
    terminated = from(content).getLong("[1].terminated");

    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_ACTIVITY_ID, id);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_AVAILABLE_LONG, available);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_ACTIVE_LONG, active);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_COMPLETED_LONG, completed);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_DISABLED_LONG, disabled);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_ENABLED_LONG, enabled);
    Assert.assertEquals(MockProvider.ANOTHER_EXAMPLE_TERMINATED_LONG, terminated);

  }

}
