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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author drobisch
 */
public class TestProperties {

  public static final String TESTCONFIG_PROPERTIES_FILE = "/testconfig.properties";
  public static final String HTTP_PORT = "http.port";
  public static final String HTTP_CTX_PATH_WEBAPP = "http.ctx-path.webapp";
  public static final String HTTP_CTX_PATH_REST = "http.ctx-path.rest";

  private final Properties properties;
  private final int defaultPort;

  public TestProperties() {
    this(8080);
  }

  public TestProperties(int defaultPort) {

    this.defaultPort = defaultPort;

    properties = getTestProperties();
  }

  public String getApplicationPath(String contextPath) {
    return "http://%s:%d%s".formatted(getHttpHost(), getHttpPort(), contextPath);
  }

  public int getHttpPort() {

    try {
      return Integer.parseInt(properties.getProperty(HTTP_PORT));
    } catch (RuntimeException e) {
      return defaultPort;
    }
  }

  public String getStringProperty(String propName, String defaultValue) {
    String propertyValue = properties.getProperty(propName, defaultValue);
    if (propertyValue.startsWith("${") && propertyValue.endsWith("}")) {
      Objects.requireNonNull(defaultValue, () -> "Property %s is not set.".formatted(propName));
      return defaultValue;
    }
    return propertyValue;
  }

  public String getHttpHost() {
    return getStringProperty("http.host", "localhost");
  }

  public String getWebappCtxPath() {
    return getStringProperty("webapp.ctx.path", "operaton/");
  }

  public String getRestCtxPath() {
    return getStringProperty("rest.ctx.path", "engine-rest/");
  }

  public static Properties getTestProperties() {
    Properties properties = new Properties();

    try (InputStream propertiesStream = TestProperties.class.getResourceAsStream(TESTCONFIG_PROPERTIES_FILE)) {
      properties.load(propertiesStream);
    } catch (IOException e) {
      throw new RuntimeException("Could not load test properties from " + TESTCONFIG_PROPERTIES_FILE, e);
    }
    Stream.of(HTTP_PORT, HTTP_CTX_PATH_WEBAPP, HTTP_CTX_PATH_REST).forEach(prop -> {
      if (System.getProperty(prop) != null) {
        properties.setProperty(prop, System.getProperty(prop));
      }
    });

    return properties;
  }
}
