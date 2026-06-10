/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.security.auth.impl.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

  public static final String PROPERTIES_FILE = "operaton-plugins.properties";
  public static final String JWT_SECRET_PROPERTY = "authentication.jwtSecret";

  private static Configuration instance;

  protected final String secret;

  public static synchronized Configuration getInstance() {
    if (instance == null) {
      instance = new Configuration();
    }
    return instance;
  }

  Configuration() {
    this(loadProperties());
  }

  Configuration(Properties properties) {
    this.secret = properties.getProperty(JWT_SECRET_PROPERTY);
  }

  public String getSecret() {
    return secret;
  }

  protected static Properties loadProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
      return properties;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load authentication configuration from: " + PROPERTIES_FILE, e);
    }
  }

}
