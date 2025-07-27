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
package org.operaton.bpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;


public class ErrorPageIT extends AbstractWebIntegrationTest {

  @BeforeEach
  public void createClient() throws Exception {
    createClient(getWebappCtxPath());
  }

  @Test
  public void shouldCheckNonFoundResponse() {
    // given
    target = client.target(appBasePath + "nonexisting");

    // when
    response = target.request().get();

    // then
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_HTML));
    String responseEntity = response.getEntity().toString();
    assertTrue(responseEntity.contains("Operaton"));
    assertTrue(responseEntity.contains("Not Found"));
  }

}
