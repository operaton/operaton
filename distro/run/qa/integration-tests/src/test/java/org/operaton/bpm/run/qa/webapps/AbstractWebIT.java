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
package org.operaton.bpm.run.qa.webapps;

//import org.glassfish.jersey.client.Client;
import com.sun.jersey.api.client.Client;
//import jakarta.ws.rs.client.ClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.junit.jupiter.api.AfterEach;
import org.operaton.bpm.TestProperties;
import org.operaton.bpm.util.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;

import java.util.logging.Logger;

/**
 * NOTE: copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/test/java/org/operaton/bpm/AbstractWebIntegrationTest.java">platform</a>,
 * might be removed with https://jira.camunda.com/browse/CAM-11379
 */
public abstract class AbstractWebIT {

  private static final Logger LOGGER = Logger.getLogger(AbstractWebIT.class.getName());

  protected String TASKLIST_PATH = "app/tasklist/default/";
  public static final String HOST_NAME = "localhost";
  public String APP_BASE_PATH;

  protected String appUrl;
  protected TestUtil testUtil;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  public JerseyClient client;
  public DefaultHttpClient defaultHttpClient;
  public String httpPort;

  @BeforeEach
  public void before() throws Exception {
    testProperties = new TestProperties(48080);
    testUtil = new TestUtil(testProperties);
  }

  @AfterEach
  public void destroyClient() {
    client.close();
  }

  public void createClient(String ctxPath) throws Exception {
    // Initialize test properties
    testProperties = new TestProperties();

    // Get the application base path
    APP_BASE_PATH = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application " + APP_BASE_PATH);

    // Create ClientConfig and register JacksonFeature for POJO mapping
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(JacksonFeature.class);

    // Create the underlying HttpClient (e.g., Apache HttpClient)
    /*HttpClient apacheHttpClient = HttpClients.custom()
            .setConnectionManager(new PoolingHttpClientConnectionManager())  // Optional: configure connection manager
            .build();*/

    // Use JerseyClientBuilder to create the client and configure it with HttpClient
    client = (JerseyClient) JerseyClientBuilder.newBuilder()
            .withConfig(clientConfig)
            .build();



    // Set connection timeout and socket timeout
    // These can be set directly in the HttpClient or via properties in JerseyClient
    client.property("jersey.config.client.connectTimeout", 3 * 60 * 1000);  // 3 minutes
    client.property("jersey.config.client.readTimeout", 10 * 60 * 1000);
  }

  public void preventRaceConditions() throws InterruptedException {
    // just wait some seconds before starting because of Wildfly / Cargo race conditions
    Thread.sleep(5 * 1000);
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", null);
  }
}
