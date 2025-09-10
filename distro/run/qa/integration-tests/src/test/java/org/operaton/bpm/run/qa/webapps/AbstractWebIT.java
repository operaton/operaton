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
package org.operaton.bpm.run.qa.webapps;

import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;

import org.operaton.bpm.TestProperties;
import org.operaton.bpm.util.TestUtil;

/**
 * NOTE: copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/test/java/org/operaton/bpm/AbstractWebIntegrationTest.java">platform</a>,
 * might be removed with https://jira.camunda.com/browse/CAM-11379
 */
public abstract class AbstractWebIT {

  private static final Logger LOGGER = Logger.getLogger(AbstractWebIT.class.getName());

  protected static final String TASKLIST_PATH = "app/tasklist/default/";
  protected static final String HOST_NAME = "localhost";
  protected String appBasePath;

  protected String appUrl;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  public String httpPort;

  @BeforeAll
  public static void setUpClass() {
    Unirest.config().reset().enableCookieManagement(false).setObjectMapper(new ObjectMapper() {
      final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

      public String writeValue(Object value) {
        try {
          return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }

      public <T> T readValue(String value, Class<T> valueType) {
        try {
          return mapper.readValue(value, valueType);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @BeforeEach
  public void before() throws Exception {
    testProperties = new TestProperties(48080);
  }

  public void createClient(String ctxPath) throws Exception {
    // Initialize test properties
    testProperties = new TestProperties();

    // Get the application base path
    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application " + appBasePath);
  }

  public void preventRaceConditions() throws InterruptedException {
    // just wait some seconds before starting because of Wildfly / Cargo race conditions
    Thread.sleep(6 * 1000);
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", null);
  }
}
