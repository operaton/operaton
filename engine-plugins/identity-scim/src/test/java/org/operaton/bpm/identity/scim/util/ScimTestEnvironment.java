/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.scim.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SCIM test environment using WireMock to simulate a SCIM 2.0 server.
 */
public class ScimTestEnvironment {

  protected WireMockServer wireMockServer;
  protected int port;
  protected String serverUrl;

  private Map<String, JsonNode> users = new HashMap<>();
  private Map<String, JsonNode> groups = new HashMap<>();

  public ScimTestEnvironment() {
    this.port = 8443; // Use fixed port by default
  }

  public ScimTestEnvironment(int port) {
    this.port = port;
  }

  public void init() throws Exception {
    WireMockConfiguration config = WireMockConfiguration.wireMockConfig();
    if (port > 0) {
      config.port(port);
    } else {
      config.dynamicPort();
    }
    
    wireMockServer = new WireMockServer(config);
    wireMockServer.start();
    
    this.port = wireMockServer.port();
    this.serverUrl = "http://localhost:" + this.port;
    
    setupScimEndpoints();
  }

  protected void setupScimEndpoints() throws Exception {
    // Setup test data
    setupUsers();
    setupGroups();
  }

  protected void setupUsers() throws JsonMappingException, JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    MappingBuilder currMapping = null;

    String emptyResponseDescr = "{\n" +
            "  \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:ListResponse\"],\n" +
            "  \"totalResults\": 0,\n" +
            "  \"Resources\": []}";

    // User 1: Oscar
    String userOscarDescr = "{\n" +
            "  \"id\": \"user-oscar\",\n" +
            "  \"userName\": \"oscar\",\n" +
            "  \"name\": {\n" +
            "    \"givenName\": \"Oscar\",\n" +
            "    \"familyName\": \"The Crouch\"\n" +
            "  },\n" +
            "  \"emails\": [{\n" +
            "    \"value\": \"oscar@operaton.org\",\n" +
            "    \"type\": \"work\"\n" +
            "  }],\n" +
            "  \"displayName\": \"Oscar The Crouch\"\n" +
            "}";
    
    // User 2: Monster
    String userMonsterDescr = "{\n" +
            "  \"id\": \"user-monster\",\n" +
            "  \"userName\": \"monster\",\n" +
            "  \"name\": {\n" +
            "    \"givenName\": \"Cookie\",\n" +
            "    \"familyName\": \"Monster\"\n" +
            "  },\n" +
            "  \"emails\": [{\n" +
            "    \"value\": \"monster@operaton.org\",\n" +
            "    \"type\": \"work\"\n" +
            "  }],\n" +
            "  \"displayName\": \"Cookie Monster\"\n" +
            "}";

    // User 3: Daniel       
    String userDanielDescr = "{\n" +
            "  \"id\": \"user-daniel\",\n" +
            "  \"userName\": \"daniel\",\n" +
            "  \"name\": {\n" +
            "    \"givenName\": \"Daniel\",\n" +
            "    \"familyName\": \"Meyer\"\n" +
            "  },\n" +
            "  \"emails\": [{\n" +
            "    \"value\": \"daniel@operaton.org\",\n" +
            "    \"type\": \"work\"\n" +
            "  }],\n" +
            "  \"displayName\": \"Daniel Meyer\"\n" +
            "}";
    
    users.clear();
    users.put("user-oscar", objectMapper.readTree(userOscarDescr)); 
    users.put("user-monster", objectMapper.readTree(userMonsterDescr)); 
    users.put("user-daniel", objectMapper.readTree(userDanielDescr));

    // User 1: Oscar

    // Stub for GET /Users: oscar by userName (id-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("userName eq \"oscar\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));

      wireMockServer.stubFor(currMapping);
    }
        
    // Stub for POST /Users: oscar
    currMapping = post(urlPathEqualTo("/Users"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .withRequestBody(matchingJsonPath("$.schemas[?(@ == 'urn:ietf:params:scim:schemas:core:2.0:User')]"))
        .withRequestBody(matchingJsonPath("$.id", absent()))
        .withRequestBody(matchingJsonPath("$.userName", equalTo("oscar")))
        .withRequestBody(matchingJsonPath("$.name.givenName", equalTo("Oscar")))
        .withRequestBody(matchingJsonPath("$.name.familyName", equalTo("The Crouch")));   
    if (currMapping != null) { 
      // configure and set response: return existing user-oscar, 
      // it should match the user in the post request
      currMapping.willReturn(aResponse()
          .withStatus(201)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(users.get("user-oscar").toString()));
                                  
      wireMockServer.stubFor(currMapping);
    } 
    
    // Stub for PATCH /Users (oscar)
    currMapping = patch(urlPathEqualTo("/Users/user-oscar"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .withRequestBody(matchingJsonPath("$.schemas[?(@ == 'urn:ietf:params:scim:api:messages:2.0:PatchOp')]"))
        .withRequestBody(matchingJsonPath("$.Operations[0].op", equalTo("replace")))
        .withRequestBody(matchingJsonPath("$.Operations[0].path", equalTo("name")))
        .withRequestBody(matchingJsonPath("$.Operations[0].value.givenName", equalTo("Oscar")))
        .withRequestBody(matchingJsonPath("$.Operations[0].value.familyName", equalTo("The (Even Cleaner) Crouch")));
    if (currMapping != null) { 
      // configure and set response: return patched copy of the existing user 
      ObjectNode patchedUser = users.get("user-oscar").deepCopy();
      ((ObjectNode)patchedUser.get("name")).put("givenName", "Oscar");
      ((ObjectNode)patchedUser.get("name")).put("familyName", "The (Even Cleaner) Crouch");
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(patchedUser.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    }
    
    // Stub for DELETE /Users/user-oscar (delete user)
    wireMockServer.stubFor(delete(urlPathEqualTo("/Users/user-oscar"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .willReturn(aResponse()
            .withStatus(204))); // SCIM DELETE = 204 No Content by default

    // Stub for GET /Users: oscar by id (scimId-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", equalTo("id eq \"user-oscar\""))
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));

      wireMockServer.stubFor(currMapping);
    } 

    // Stub for GET /Users: oscar by givenName (firstName-attribute)    
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", equalTo("name.givenName eq \"Oscar\""))
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));

      wireMockServer.stubFor(currMapping);
    } 
    
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", equalTo("emails[type eq \"work\"].value eq \"oscar@operaton.org\""))
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));

      wireMockServer.stubFor(currMapping);
    }     
    
    // User 2: Monster    

    // Stub for GET /Users: monster by userName (id-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("userName eq \"monster\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));

      wireMockServer.stubFor(currMapping);
    }
    
    // Stub for GET /Users: monster by id (scimId-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("id eq \"user-monster\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
        // configure and set response
        ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
        currResponse.withArray("Resources").add(users.get("user-monster"));
        currResponse.put("totalResults", currResponse.withArray("Resources").size());
        currResponse.put("startIndex", 1);
        currMapping.willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/scim+json")
            .withBody(currResponse.toString()));
        
        wireMockServer.stubFor(currMapping);
    }
    
    // Stub for GET /Users: monster by givenName (firstName-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("name.givenName eq \"Cookie\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                
      wireMockServer.stubFor(currMapping);
    }         

    // Stub for GET /Users: monster by familyName (lastName-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("name.familyName eq \"Monster\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                          
      wireMockServer.stubFor(currMapping);
    }
    
    // User 3: Daniel
    
    // Stub for GET /Users: daniel by userName (id-attribute)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("userName eq \"daniel\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));  
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-daniel"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                            
      wireMockServer.stubFor(currMapping);
    }

    // Other stubs
    
    // All users query (no filter or empty filter)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", absent())
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*")); 
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      for (var user : users.entrySet()) {
        currResponse.withArray("Resources").add(user.getValue());
      }
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    }
        
    // Non-existing user
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("userName eq \"non-existing\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                    
      wireMockServer.stubFor(currMapping);
    }

    // Non-existing user
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", equalTo("id eq \"non-existing\""))
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                      
      wireMockServer.stubFor(currMapping);
    }

    // Multiple user IDs (OR query)
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", equalTo("(userName eq \"oscar\" or userName eq \"monster\")"))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                              
      wireMockServer.stubFor(currMapping);
    }

    // Multiple user IDs (OR query)
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", equalTo("(id eq \"user-oscar\" or id eq \"user-monster\")"))
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                
      wireMockServer.stubFor(currMapping);
    }

    // Pagination sub-query 1
    currMapping = get(urlPathEqualTo("/Users"))
            .withQueryParam("filter", absent())
            .withQueryParam("startIndex", equalTo("1"))
            .withQueryParam("count", equalTo("2"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-monster"));
      currResponse.withArray("Resources").add(users.get("user-oscar"));
      currResponse.put("totalResults", users.size());
      currResponse.put("startIndex", 1);
      currResponse.put("itemsPerPage", 2);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                
      wireMockServer.stubFor(currMapping);
    }    
  
    // Pagination sub-query 2
    currMapping = get(urlPathEqualTo("/Users"))
        .withQueryParam("filter", absent())
        .withQueryParam("startIndex", equalTo("3"))
        .withQueryParam("count", equalTo("2"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(users.get("user-daniel"));
      currResponse.put("totalResults", users.size());
      currResponse.put("startIndex", 3);
      currResponse.put("itemsPerPage", 2);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    }  
  }

  protected void setupGroups() throws JsonMappingException, JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    MappingBuilder currMapping = null;

    String emptyResponseDescr = "{\n" +
           "  \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:ListResponse\"],\n" +
           "  \"totalResults\": 0,\n" +
           "  \"Resources\": []}";

    // Group 1: development
    String devGroupDescr = "{\n" +
            "  \"id\": \"group-development\",\n" +
            "  \"externalId\": \"group-development\",\n" +
            "  \"displayName\": \"development\",\n" +
            "  \"members\": [\n" +
            "    {\"value\": \"user-oscar\", \"$ref\": \"/Users/user-oscar\"},\n" +
            "    {\"value\": \"user-daniel\", \"$ref\": \"/Users/user-daniel\"}\n" +
            "  ]\n" +
            "}";
    
    // Group 2: management
    String mgmGroupDescr = "{\n" +
            "  \"id\": \"group-management\",\n" +
            "  \"externalId\": \"group-management\",\n" +
            "  \"displayName\": \"management\",\n" +
            "  \"members\": [\n" +
            "    {\"value\": \"user-daniel\", \"$ref\": \"/Users/user-daniel\"}\n" +
            "  ]\n" +
            "}";

    groups.clear();
    groups.put("group-development", objectMapper.readTree(devGroupDescr)); 
    groups.put("group-management", objectMapper.readTree(mgmGroupDescr)); 
    
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("displayName eq \"development\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));   
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(groups.get("group-development"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                              
      wireMockServer.stubFor(currMapping);
    }
    
    // Stub for POST /Groups (development)
    currMapping = post(urlPathEqualTo("/Groups"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .withRequestBody(matchingJsonPath("$.schemas[?(@ == 'urn:ietf:params:scim:schemas:core:2.0:Group')]"))
        .withRequestBody(matchingJsonPath("$.id", absent()))
        .withRequestBody(matchingJsonPath("$.externalId", equalTo("group-development")))
        .withRequestBody(matchingJsonPath("$.displayName", equalTo("development")));
    if (currMapping != null) { 
      // configure and set response: return existing group-development, 
      // it should match the group in the post request
      currMapping.willReturn(aResponse()
          .withStatus(201)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(groups.get("group-development").toString()));
                                
      wireMockServer.stubFor(currMapping);
    }    
    
    // Stub for PATCH /Groups (development)
    currMapping = patch(urlPathEqualTo("/Groups/group-development"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .withRequestBody(matchingJsonPath("$.schemas[?(@ == 'urn:ietf:params:scim:api:messages:2.0:PatchOp')]"))
        .withRequestBody(matchingJsonPath("$.Operations[0].op", equalTo("replace")))
        .withRequestBody(matchingJsonPath("$.Operations[0].path", equalTo("displayName")))
        .withRequestBody(matchingJsonPath("$.Operations[0].value", equalTo("DEVELOPMENT")));   
    if (currMapping != null) { 
      // configure and set response: return patched copy of the existing group-development
      ObjectNode patchedGroup = groups.get("group-development").deepCopy();
      patchedGroup.put("displayName", "DEVELOPMENT");
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(patchedGroup.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    }

    // Stub for DELETE /Groups/group-development (delete group)
    wireMockServer.stubFor(delete(urlPathEqualTo("/Groups/group-development"))
        .withHeader("Content-Type", containing("application/scim+json"))
        .willReturn(aResponse()
            .withStatus(204))); // SCIM DELETE = 204 No Content by default

    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("externalId eq \"group-development\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(groups.get("group-development"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                
      wireMockServer.stubFor(currMapping);
    }    
    
    // Group 2: management
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("displayName eq \"management\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*")); 
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(groups.get("group-management"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    } 
 
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("externalId eq \"group-management\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(groups.get("group-management"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                  
      wireMockServer.stubFor(currMapping);
    }    

    // Non existing group
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("externalId eq \"non-existing\""))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                    
      wireMockServer.stubFor(currMapping);
    } 
 
    // All groups query
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", absent())
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
        // configure and set response
        ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
        for (var group : groups.entrySet()) {
          currResponse.withArray("Resources").add(group.getValue());
        }
        currResponse.put("totalResults", currResponse.withArray("Resources").size());
        currResponse.put("startIndex", 1);
        currMapping.willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/scim+json")
            .withBody(currResponse.toString()));
                                    
        wireMockServer.stubFor(currMapping);
      }

    // Groups by user filter - oscar
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("members[value eq \"user-oscar\"]"))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));   
    if (currMapping != null) { 
      // configure and set response
      ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
      currResponse.withArray("Resources").add(groups.get("group-development"));
      currResponse.put("totalResults", currResponse.withArray("Resources").size());
      currResponse.put("startIndex", 1);
      currMapping.willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/scim+json")
          .withBody(currResponse.toString()));
                                
      wireMockServer.stubFor(currMapping);
    }

    // Groups by user filter - daniel (member of both)
    currMapping = get(urlPathEqualTo("/Groups"))
        .withQueryParam("filter", equalTo("members[value eq \"user-daniel\"]"))
        .withQueryParam("startIndex", equalTo("1"))
        .withQueryParam("count", matching(".*"));
    if (currMapping != null) { 
        // configure and set response
        ObjectNode currResponse = (ObjectNode) objectMapper.readTree(emptyResponseDescr);
        currResponse.withArray("Resources").add(groups.get("group-development"));
        currResponse.withArray("Resources").add(groups.get("group-management"));
        currResponse.put("totalResults", currResponse.withArray("Resources").size());
        currResponse.put("startIndex", 1);
        currMapping.willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/scim+json")
            .withBody(currResponse.toString()));
                                  
        wireMockServer.stubFor(currMapping);
      }
   

    // Get group by ID
    /*wireMockServer.stubFor(get(urlPathEqualTo("/Groups/development"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/scim+json")
            .withBody("{\n" +
                "  \"id\": \"group-development\",\n" +
                "  \"displayName\": \"development\",\n" +
                "  \"members\": [\n" +
                "    {\"value\": \"user-oscar\", \"$ref\": \"/Users/user-oscar\"},\n" +
                "    {\"value\": \"user-daniel\", \"$ref\": \"/Users/user-daniel\"}\n" +
                "  ]\n" +
                "}")));*/
  }

  public void shutdown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public int getPort() {
    return port;
  }

  public int getTotalNumberOfUsersCreated() {
    return users.size();
  }

  public int getTotalNumberOfGroupsCreated() {
    return groups.size();
  }
}
