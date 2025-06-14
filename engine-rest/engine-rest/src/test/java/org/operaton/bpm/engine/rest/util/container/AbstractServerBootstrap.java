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
package org.operaton.bpm.engine.rest.util.container;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;

public abstract class AbstractServerBootstrap {

  protected static final String PORT_PROPERTY = "rest.http.port";
  protected static final String ROOT_RESOURCE_PATH = "/rest-test";
  protected static final String PROPERTIES_FILE = "/testconfig.properties";

  protected static final int STARTUP_RETRIES = 3;

  public abstract void stop();

  public void start() {
    startServer(STARTUP_RETRIES);
  }

  protected abstract void startServer(int startUpRetries);

  protected Properties readProperties() {
    InputStream propStream = null;
    Properties properties = new Properties();

    try {
      propStream = AbstractRestServiceTest.class.getResourceAsStream(PROPERTIES_FILE);
      properties.load(propStream);
    } catch (IOException e) {
      throw new ServerBootstrapException(e);
    } finally {
      try {
        propStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return properties;
  }
}
