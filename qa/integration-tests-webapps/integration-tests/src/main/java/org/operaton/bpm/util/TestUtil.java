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

import org.operaton.bpm.TestProperties;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import kong.unirest.json.JSONObject;

/**
 *
 * @author nico.rehwaldt
 */
public class TestUtil {

  private final HttpClient client;
  private final TestProperties testProperties;

  public TestUtil(TestProperties testProperties) {
    this.testProperties = testProperties;
    this.client = HttpClient.newBuilder().build();
  }

  public void createInitialUser(String id, String password, String firstName, String lastName) throws IOException, InterruptedException {
    UserDto user = new UserDto();
    UserCredentialsDto credentials = new UserCredentialsDto();
    credentials.setPassword(password);
    user.setCredentials(credentials);
    UserProfileDto profile = new UserProfileDto();
    profile.setId(id);
    profile.setFirstName(firstName);
    profile.setLastName(lastName);
    user.setProfile(profile);

    String jsonPayload = new JSONObject(user).toString();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(testProperties.getApplicationPath("/operaton/api/admin/setup/default/user/create")))
      .header("Content-Type", MediaType.APPLICATION_JSON)
      .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
      .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != Response.Status.NO_CONTENT.getStatusCode()) {
      throw new WebApplicationException(response.statusCode());
    }
  }

  public void deleteUser(String id) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(testProperties.getApplicationPath("/engine-rest/user/admin")))
      .header("Content-Type", MediaType.APPLICATION_JSON)
      .DELETE()
      .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != Response.Status.NO_CONTENT.getStatusCode()) {
      throw new WebApplicationException(response.statusCode());
    }
  }
}