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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;

import static org.operaton.bpm.engine.rest.helper.MockProvider.EXAMPLE_USER_OPERATION_ANNOTATION;
import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class IncidentRestServiceInteractionTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String INCIDENT_URL = TEST_RESOURCE_ROOT_PATH + "/incident";
  protected static final String SINGLE_INCIDENT_URL = INCIDENT_URL + "/{id}";
  protected static final String INCIDENT_ANNOTATION_URL = SINGLE_INCIDENT_URL + "/annotation";

  private RuntimeServiceImpl mockRuntimeService;
  private IncidentQuery mockedQuery;

  @BeforeEach
  void setUpRuntimeData() {
    mockedQuery = setupMockIncidentQuery();
  }

  private IncidentQuery setupMockIncidentQuery() {
    IncidentQuery sampleQuery = mock(IncidentQuery.class);

    when(sampleQuery.incidentId(anyString())).thenReturn(sampleQuery);
    when(sampleQuery.singleResult()).thenReturn(mock(Incident.class));

    mockRuntimeService = mock(RuntimeServiceImpl.class);
    when(processEngine.getRuntimeService()).thenReturn(mockRuntimeService);
    when(mockRuntimeService.createIncidentQuery()).thenReturn(sampleQuery);

    return sampleQuery;
  }

  @Test
  void testGetIncident() {

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.OK.getStatusCode())
    .when()
      .get(SINGLE_INCIDENT_URL);

    verify(mockRuntimeService).createIncidentQuery();
    verify(mockedQuery).incidentId(EXAMPLE_INCIDENT_ID);
    verify(mockedQuery).singleResult();
  }

  @Test
  void testGetUnexistingIncident() {
    when(mockedQuery.singleResult()).thenReturn(null);

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
    .when()
      .get(SINGLE_INCIDENT_URL);

    verify(mockRuntimeService).createIncidentQuery();
    verify(mockedQuery).incidentId(EXAMPLE_INCIDENT_ID);
    verify(mockedQuery).singleResult();
  }

  @Test
  void testResolveIncident() {

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(SINGLE_INCIDENT_URL);

    verify(mockRuntimeService).resolveIncident(EXAMPLE_INCIDENT_ID);
  }

  @Test
  void testResolveUnexistingIncident() {
    doThrow(new NotFoundException()).when(mockRuntimeService).resolveIncident(anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.NOT_FOUND.getStatusCode())
    .when()
      .delete(SINGLE_INCIDENT_URL);

    verify(mockRuntimeService).resolveIncident(EXAMPLE_INCIDENT_ID);
  }

  @Test
  void shouldSetAnnotation() {
    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
      .contentType(MediaType.APPLICATION_JSON)
      .body("{ \"annotation\": \"" + EXAMPLE_USER_OPERATION_ANNOTATION + "\" }")
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .put(INCIDENT_ANNOTATION_URL);

    verify(mockRuntimeService)
      .setAnnotationForIncidentById(EXAMPLE_INCIDENT_ID, EXAMPLE_USER_OPERATION_ANNOTATION);
  }

  @Test
  void shouldThrowNotValidExceptionWhenSetAnnotation() {
    doThrow(new NotValidException("expected"))
      .when(mockRuntimeService)
      .setAnnotationForIncidentById(anyString(), anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
      .contentType(MediaType.APPLICATION_JSON)
      .body("{ \"annotation\": \"" + EXAMPLE_USER_OPERATION_ANNOTATION + "\" }")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .put(INCIDENT_ANNOTATION_URL);
  }

  @Test
  void shouldThrowAuthorizationExceptionWhenSetAnnotation() {
    doThrow(new AuthorizationException("expected"))
      .when(mockRuntimeService)
      .setAnnotationForIncidentById(anyString(), anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
      .contentType(MediaType.APPLICATION_JSON)
      .body("{ \"annotation\": \"" + EXAMPLE_USER_OPERATION_ANNOTATION + "\" }")
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
    .when()
      .put(INCIDENT_ANNOTATION_URL);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenSetAnnotation() {
    doThrow(new BadUserRequestException("expected"))
      .when(mockRuntimeService)
      .setAnnotationForIncidentById(anyString(), anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
      .contentType(MediaType.APPLICATION_JSON)
      .body("{ \"annotation\": \"" + EXAMPLE_USER_OPERATION_ANNOTATION + "\" }")
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .put(INCIDENT_ANNOTATION_URL);
  }

  @Test
  void shouldClearAnnotation() {
    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.NO_CONTENT.getStatusCode())
    .when()
      .delete(INCIDENT_ANNOTATION_URL);

    verify(mockRuntimeService).clearAnnotationForIncidentById(EXAMPLE_INCIDENT_ID);
  }

  @Test
  void shouldThrowNotValidExceptionWhenClearAnnotation() {
    doThrow(new NotValidException("expected"))
      .when(mockRuntimeService)
      .clearAnnotationForIncidentById(anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .delete(INCIDENT_ANNOTATION_URL);
  }

  @Test
  void shouldThrowAuthorizationExceptionWhenClearAnnotation() {
    doThrow(new AuthorizationException("expected"))
      .when(mockRuntimeService)
      .clearAnnotationForIncidentById(anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .then().expect()
      .statusCode(Status.FORBIDDEN.getStatusCode())
    .when()
      .delete(INCIDENT_ANNOTATION_URL);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenClearAnnotation() {
    doThrow(new BadUserRequestException("expected"))
      .when(mockRuntimeService)
      .clearAnnotationForIncidentById(anyString());

    given()
      .pathParam("id", EXAMPLE_INCIDENT_ID)
    .expect()
      .statusCode(Status.BAD_REQUEST.getStatusCode())
    .when()
      .delete(INCIDENT_ANNOTATION_URL);
  }
}
