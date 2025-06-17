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
package org.operaton.bpm.engine.rest.mapper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.impl.RuntimeServiceImpl;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.util.container.TestContainerExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

public class Java8DateTimeTest extends AbstractRestServiceTest {

  @RegisterExtension
  public static TestContainerExtension rule = new TestContainerExtension();

  protected static final String PROCESS_INSTANCE_URL = TEST_RESOURCE_ROOT_PATH + "/process-instance";
  protected static final String SINGLE_PROCESS_INSTANCE_URL = PROCESS_INSTANCE_URL + "/{id}";
  protected static final String PROCESS_INSTANCE_VARIABLES_URL = SINGLE_PROCESS_INSTANCE_URL + "/variables";
  protected static final String SINGLE_PROCESS_INSTANCE_VARIABLE_URL = PROCESS_INSTANCE_VARIABLES_URL + "/{varId}";

  protected RuntimeServiceImpl runtimeServiceMock;

  @BeforeEach
  void setUpRuntimeData() {
    runtimeServiceMock = mock(RuntimeServiceImpl.class);

    when(runtimeServiceMock.getVariableTyped(EXAMPLE_PROCESS_INSTANCE_ID, EXAMPLE_VARIABLE_KEY, true))
      .thenReturn(Variables.objectValue(new Java8DateTimePojo()).create());

    when(processEngine.getRuntimeService()).thenReturn(runtimeServiceMock);
  }

  @Test
  void testGetVariable() {
    given()
        .pathParam("id", EXAMPLE_PROCESS_INSTANCE_ID)
        .pathParam("varId", EXAMPLE_VARIABLE_KEY)
      .then().expect()
        .statusCode(Status.OK.getStatusCode())
        .body("value.localDate", is(LocalDate.now().toString()))
        .body("type", is("Object"))
      .when()
        .get(SINGLE_PROCESS_INSTANCE_VARIABLE_URL);
  }

}
