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

import com.fasterxml.jackson.core.JsonProcessingException;
import kong.unirest.ObjectMapper;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import org.operaton.bpm.TestProperties;

import static org.awaitility.Awaitility.await;

/**
 * NOTE: copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/test/java/org/operaton/bpm/AbstractWebIntegrationTest.java">platform</a>,
 * might be removed with https://jira.camunda.com/browse/CAM-11379
 */
public abstract class AbstractWebIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebIT.class);

  protected String appBasePath;

  protected String appUrl;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  @BeforeAll
  public static void setUpClass() {
    Unirest.config().reset().enableCookieManagement(false).setObjectMapper(new ObjectMapper() {
      final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

      @Override
      public String writeValue(Object value) {
        try {
          return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
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
  public void before() {
    testProperties = new TestProperties(48080);
  }

  public void createClient(String ctxPath) {
    // Initialize test properties
    testProperties = new TestProperties();

    // Get the application base path
    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application {}", appBasePath);
  }

  public void preventRaceConditions() {
    // just wait until the application is available before starting because of Wildfly / Cargo race conditions
    if (appBasePath != null) {
      await().atMost(10, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .ignoreExceptions()
        .until(() -> Unirest.head(appBasePath).asEmpty().isSuccess());
    }
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", null);
  }
}
