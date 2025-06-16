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

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.impl.AuthorizationServiceImpl;
import org.operaton.bpm.engine.impl.IdentityServiceImpl;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationRestServiceQueryTest extends AbstractRestServiceTest {

  protected static final String SERVICE_PATH = TEST_RESOURCE_ROOT_PATH + AuthorizationRestService.PATH;
  protected static final String SERVICE_COUNT_PATH = TEST_RESOURCE_ROOT_PATH + AuthorizationRestService.PATH+"/count";

  protected AuthorizationService authorizationServiceMock;
  protected IdentityService identityServiceMock;
  protected ProcessEngineConfigurationImpl processEngineConfigurationMock;

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  @BeforeEach
  public void setUpRuntimeData() {
    authorizationServiceMock = mock(AuthorizationServiceImpl.class);
    identityServiceMock = mock(IdentityServiceImpl.class);
    processEngineConfigurationMock = mock(ProcessEngineConfigurationImpl.class);

    when(processEngine.getAuthorizationService()).thenReturn(authorizationServiceMock);
    when(processEngine.getIdentityService()).thenReturn(identityServiceMock);
    when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfigurationMock);
    when(processEngineConfigurationMock.getPermissionProvider()).thenReturn(new DefaultPermissionProvider());
  }

  private AuthorizationQuery setUpMockQuery(List<Authorization> list) {
    AuthorizationQuery query = mock(AuthorizationQuery.class);
    when(query.list()).thenReturn(list);
    when(query.count()).thenReturn((long) list.size());

    when(processEngine.getAuthorizationService().createAuthorizationQuery()).thenReturn(query);

    return query;
  }

  @Test
  public void testEmptyQuery() {

    setUpMockQuery(MockProvider.createMockAuthorizations());

    String queryKey = "";
    given().queryParam("name", queryKey)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(SERVICE_PATH);

  }

  @Test
  public void testSortByParameterOnly() {
    given().queryParam("sortBy", "resourceType")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(SERVICE_PATH);
  }

  @Test
  public void testSortOrderParameterOnly() {
    given().queryParam("sortOrder", "asc")
      .then().expect().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Only a single sorting parameter specified. sortBy and sortOrder required"))
      .when().get(SERVICE_PATH);
  }

  @Test
  public void testNoParametersQuery() {

    AuthorizationQuery mockQuery = setUpMockQuery(MockProvider.createMockAuthorizations());

    expect().statusCode(Status.OK.getStatusCode()).when().get(SERVICE_PATH);

    verify(mockQuery).list();
    verifyNoMoreInteractions(mockQuery);
  }

  @Test
  public void testSimpleAuthorizationQuery() {

    List<Authorization> mockAuthorizations = MockProvider.createMockGlobalAuthorizations();
    AuthorizationQuery mockQuery = setUpMockQuery(mockAuthorizations);

    Response response = given().queryParam("type", Authorization.AUTH_TYPE_GLOBAL)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(SERVICE_PATH);

    InOrder inOrder = inOrder(mockQuery);
    inOrder.verify(mockQuery).authorizationType(Authorization.AUTH_TYPE_GLOBAL);
    inOrder.verify(mockQuery).list();

    String content = response.asString();
    List<Map<String, Object>> instances = from(content).getList("");
    Assertions.assertEquals(1, instances.size(), "There should be one authorization returned.");
    assertThat(instances.get(0)).as("The returned authorization should not be null.").isNotNull();

    Authorization mockAuthorization = mockAuthorizations.get(0);

    Assertions.assertEquals(mockAuthorization.getId(), from(content).getString("[0].id"));
    Assertions.assertEquals(mockAuthorization.getAuthorizationType(), from(content).getInt("[0].type"));
    Assertions.assertEquals(Permissions.READ.getName(), from(content).getString("[0].permissions[0]"));
    Assertions.assertEquals(Permissions.UPDATE.getName(), from(content).getString("[0].permissions[1]"));
    Assertions.assertEquals(mockAuthorization.getUserId(), from(content).getString("[0].userId"));
    Assertions.assertEquals(mockAuthorization.getGroupId(), from(content).getString("[0].groupId"));
    Assertions.assertEquals(mockAuthorization.getResourceType(), from(content).getInt("[0].resourceType"));
    Assertions.assertEquals(mockAuthorization.getResourceId(), from(content).getString("[0].resourceId"));
    Assertions.assertEquals(mockAuthorization.getRemovalTime(),
        DateTimeUtil.parseDate(from(content).getString("[0].removalTime")));
    Assertions.assertEquals(mockAuthorization.getRootProcessInstanceId(),
        from(content).getString("[0].rootProcessInstanceId"));

  }

  @Test
  public void testCompleteGetParameters() {

    List<Authorization> mockAuthorizations = MockProvider.createMockGlobalAuthorizations();
    AuthorizationQuery mockQuery = setUpMockQuery(mockAuthorizations);

    Map<String, String> queryParameters = getCompleteStringQueryParameters();

    RequestSpecification requestSpecification = given().contentType(POST_JSON_CONTENT_TYPE);
    for (Entry<String, String> paramEntry : queryParameters.entrySet()) {
      requestSpecification.param(paramEntry.getKey(), paramEntry.getValue());
    }

    requestSpecification.expect().statusCode(Status.OK.getStatusCode())
      .when().get(SERVICE_PATH);

    verify(mockQuery).authorizationId(MockProvider.EXAMPLE_AUTHORIZATION_ID);
    verify(mockQuery).authorizationType(MockProvider.EXAMPLE_AUTHORIZATION_TYPE);
    verify(mockQuery).userIdIn(new String[]{MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_ID2});
    verify(mockQuery).groupIdIn(new String[]{MockProvider.EXAMPLE_GROUP_ID, MockProvider.EXAMPLE_GROUP_ID2});
    verify(mockQuery).resourceType(MockProvider.EXAMPLE_RESOURCE_TYPE_ID);
    verify(mockQuery).resourceId(MockProvider.EXAMPLE_RESOURCE_ID);

    verify(mockQuery).list();

  }


  private Map<String, String> getCompleteStringQueryParameters() {
    Map<String, String> parameters = new HashMap<>();

    parameters.put("id", MockProvider.EXAMPLE_AUTHORIZATION_ID);
    parameters.put("type", MockProvider.EXAMPLE_AUTHORIZATION_TYPE_STRING);
    parameters.put("userIdIn", MockProvider.EXAMPLE_USER_ID + ","+MockProvider.EXAMPLE_USER_ID2);
    parameters.put("groupIdIn", MockProvider.EXAMPLE_GROUP_ID+","+MockProvider.EXAMPLE_GROUP_ID2);
    parameters.put("resourceType", MockProvider.EXAMPLE_RESOURCE_TYPE_ID_STRING);
    parameters.put("resourceId", MockProvider.EXAMPLE_RESOURCE_ID);

    return parameters;
  }

  @Test
  public void testQueryCount() {

    AuthorizationQuery mockQuery = setUpMockQuery(MockProvider.createMockAuthorizations());

    expect().statusCode(Status.OK.getStatusCode())
      .body("count", equalTo(3))
      .when().get(SERVICE_COUNT_PATH);

    verify(mockQuery).count();
  }

  @Test
  public void testSuccessfulPagination() {

    AuthorizationQuery mockQuery = setUpMockQuery(MockProvider.createMockAuthorizations());

    int firstResult = 0;
    int maxResults = 10;
    given().queryParam("firstResult", firstResult).queryParam("maxResults", maxResults)
      .then().expect().statusCode(Status.OK.getStatusCode())
      .when().get(SERVICE_PATH);

    verify(mockQuery).listPage(firstResult, maxResults);
  }


}
