/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.util;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.operaton.bpm.TestProperties;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.client.Entity;

public class TestUtil {

  private final Client client;
  private final TestProperties testProperties;

  public TestUtil(TestProperties testProperties) {
    this.testProperties = testProperties;

    // Create Jersey client and register Jackson for POJO mapping
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(JacksonJaxbJsonProvider.class);  // Register Jackson for POJO mapping

    client = ClientBuilder.newClient(clientConfig);
  }

  public void destroy() {
    client.close();
  }

  public void createInitialUser(String id, String password, String firstName, String lastName) {
    UserDto user = new UserDto();
    UserCredentialsDto credentials = new UserCredentialsDto();
    credentials.setPassword(password);
    user.setCredentials(credentials);

    UserProfileDto profile = new UserProfileDto();
    profile.setId(id);
    profile.setFirstName(firstName);
    profile.setLastName(lastName);
    user.setProfile(profile);

    // Build the resource URL
    WebTarget webTarget = client.target(testProperties.getApplicationPath("/operaton/api/admin/setup/default/user/create"));

    // Send the POST request
    Response clientResponse = webTarget
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(user, MediaType.APPLICATION_JSON));

    try {
      // Check for successful creation
      if (clientResponse.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
        throw new WebApplicationException(clientResponse.getStatus());
      }
    } finally {
      clientResponse.close();  // Ensure the response is closed
    }
  }

  public void deleteUser(String id) {
    // Delete the user
    var webTarget = client.target(testProperties.getApplicationPath("/engine-rest/user/admin"));

    // Send DELETE request
    var response = webTarget
            .request(MediaType.APPLICATION_JSON)
            .delete();

    // Check for success
    if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
      throw new WebApplicationException(response.getStatus());
    }
    response.close();
  }
}
