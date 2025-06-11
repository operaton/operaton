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
package org.operaton.bpm.engine.rest.standalone;

import java.util.Base64;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.identity.*;
import org.operaton.bpm.engine.impl.AuthorizationServiceImpl;
import org.operaton.bpm.engine.impl.IdentityServiceImpl;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.ProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;
import org.operaton.bpm.engine.rest.impl.NamedProcessEngineRestServiceImpl;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;

import io.restassured.http.ContentType;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AbstractAuthenticationFilterTest extends AbstractRestServiceTest {

  protected static final String SERVLET_PATH = "/rest";
  protected static final String SERVICE_PATH = TEST_RESOURCE_ROOT_PATH + SERVLET_PATH + NamedProcessEngineRestServiceImpl.PATH + "/{name}"+ ProcessDefinitionRestService.PATH;

  protected AuthorizationService authorizationServiceMock;
  protected IdentityService identityServiceMock;
  protected RepositoryService repositoryServiceMock;

  protected User userMock;
  protected List<String> groupIds;
  protected List<String> tenantIds;

  @Before
  public void setup() {
    authorizationServiceMock = mock(AuthorizationServiceImpl.class);
    identityServiceMock = mock(IdentityServiceImpl.class);
    repositoryServiceMock = mock(RepositoryService.class);

    when(processEngine.getAuthorizationService()).thenReturn(authorizationServiceMock);
    when(processEngine.getIdentityService()).thenReturn(identityServiceMock);
    when(processEngine.getRepositoryService()).thenReturn(repositoryServiceMock);

    // for authentication
    userMock = MockProvider.createMockUser();

    List<Group> groupMocks = MockProvider.createMockGroups();
    groupIds = setupGroupQueryMock(groupMocks);

    List<Tenant> tenantMocks = Collections.singletonList(MockProvider.createMockTenant());
    tenantIds = setupTenantQueryMock(tenantMocks);

    // example method
    ProcessDefinition mockDefinition = MockProvider.createMockDefinition();
    List<ProcessDefinition> mockDefinitions = List.of(mockDefinition);
    ProcessDefinitionQuery mockQuery = mock(ProcessDefinitionQuery.class);
    when(repositoryServiceMock.createProcessDefinitionQuery()).thenReturn(mockQuery);
    when(mockQuery.list()).thenReturn(mockDefinitions);
  }

  protected List<String> setupGroupQueryMock(List<Group> groups) {
    GroupQuery mockGroupQuery = mock(GroupQuery.class);

    when(identityServiceMock.createGroupQuery()).thenReturn(mockGroupQuery);
    when(mockGroupQuery.groupMember(anyString())).thenReturn(mockGroupQuery);
    when(mockGroupQuery.list()).thenReturn(groups);

    return groups.stream().map(Group::getId).toList();
  }

  protected List<String> setupTenantQueryMock(List<Tenant> tenants) {
    TenantQuery mockTenantQuery = mock(TenantQuery.class);

    when(identityServiceMock.createTenantQuery()).thenReturn(mockTenantQuery);
    when(mockTenantQuery.userMember(anyString())).thenReturn(mockTenantQuery);
    when(mockTenantQuery.includingGroupsOfUser(anyBoolean())).thenReturn(mockTenantQuery);
    when(mockTenantQuery.list()).thenReturn(tenants);

    return tenants.stream().map(Tenant::getId).toList();
  }

  @Test
  public void testHttpBasicAuthenticationCheck() {
    when(identityServiceMock.checkPassword(MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_PASSWORD)).thenReturn(true);

    given()
      .auth().basic(MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_PASSWORD)
      .pathParam("name", "default")
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(MediaType.APPLICATION_JSON)
    .when().get(SERVICE_PATH);

    verify(identityServiceMock).setAuthentication(MockProvider.EXAMPLE_USER_ID, groupIds, tenantIds);
  }

  @Test
  public void testFailingAuthenticationCheck() {
    when(identityServiceMock.checkPassword(MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_PASSWORD)).thenReturn(false);

    given()
      .auth().basic(MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_PASSWORD)
      .pathParam("name", "default")
    .then().expect()
      .statusCode(Status.UNAUTHORIZED.getStatusCode())
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"default\"")
    .when().get(SERVICE_PATH);
  }

  @Test
  public void testMissingAuthHeader() {
    given()
      .pathParam("name", "someengine")
    .then().expect()
      .statusCode(Status.UNAUTHORIZED.getStatusCode())
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"someengine\"")
    .when().get(SERVICE_PATH);
  }

  @Test
  public void testUnexpectedAuthHeaderFormat() {
    given()
      .header(HttpHeaders.AUTHORIZATION, "Digest somevalues, and, some, more")
      .pathParam("name", "someengine")
    .then().expect()
      .statusCode(Status.UNAUTHORIZED.getStatusCode())
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"someengine\"")
    .when().get(SERVICE_PATH);
  }

  @Test
  public void testMalformedCredentials() {
    given()
      .header(HttpHeaders.AUTHORIZATION,
          "Basic " + Base64.getEncoder().encodeToString("this is not a valid format".getBytes()))
      .pathParam("name", "default")
    .then().expect()
      .statusCode(Status.UNAUTHORIZED.getStatusCode())
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"default\"")
    .when().get(SERVICE_PATH);
  }

  @Test
  public void testNonExistingEngineAuthentication() {
    given()
      .auth().basic(MockProvider.EXAMPLE_USER_ID, MockProvider.EXAMPLE_USER_PASSWORD)
      .pathParam("name", MockProvider.NON_EXISTING_PROCESS_ENGINE_NAME)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode()).contentType(ContentType.JSON)
      .body("type", equalTo(InvalidRequestException.class.getSimpleName()))
      .body("message", equalTo("Process engine " + MockProvider.NON_EXISTING_PROCESS_ENGINE_NAME + " not available"))
    .when().get(SERVICE_PATH);
  }

  @Test
  public void testMalformedBase64Value() {
    given()
      .header(HttpHeaders.AUTHORIZATION, "Basic someNonBase64Characters!(#")
      .pathParam("name", "default")
    .then().expect()
      .statusCode(Status.UNAUTHORIZED.getStatusCode())
      .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"default\"")
    .when().get(SERVICE_PATH);
  }

}
