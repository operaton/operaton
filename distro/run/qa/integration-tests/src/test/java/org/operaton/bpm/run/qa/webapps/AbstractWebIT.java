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

import org.operaton.bpm.TestProperties;
import org.operaton.bpm.util.TestUtil;

import java.net.http.HttpClient;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.chrome.ChromeDriverService;

/**
 * NOTE: copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/test/java/org/operaton/bpm/AbstractWebIntegrationTest.java">platform</a>,
 * might be removed with https://jira.camunda.com/browse/CAM-11379
 */
public abstract class AbstractWebIT {

  private static final Logger LOGGER = Logger.getLogger(AbstractWebIT.class.getName());

  protected String TASKLIST_PATH = "app/tasklist/default/";
  public static final String HOST_NAME = "localhost";
  public String appBasePath;

  protected String appUrl;
  protected TestUtil testUtil;
  protected TestProperties testProperties;

  protected static ChromeDriverService service;

  protected HttpClient client;
  protected String httpPort;

  @BeforeEach
  public void before() throws Exception {
    testProperties = new TestProperties(48080);
    testUtil = new TestUtil(testProperties);
  }

  public void createClient(String ctxPath) throws Exception {
    testProperties = new TestProperties();

    appBasePath = testProperties.getApplicationPath("/" + ctxPath);
    LOGGER.info("Connecting to application "+ appBasePath);

    client = HttpClient.newBuilder().build();
  }

  public void preventRaceConditions() throws InterruptedException {
    // just wait some seconds before starting because of Wildfly / Cargo race conditions
    Thread.sleep(5 * 1000);
  }

  protected String getWebappCtxPath() {
    return testProperties.getStringProperty("http.ctx-path.webapp", "operaton/");
  }
}
