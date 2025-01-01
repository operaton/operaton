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
package org.operaton.bpm.qa.performance.engine.junit;

import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestConfiguration;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestException;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit 5 extension allowing to load the performance test configuration from a file.
 * This extension loads the configuration once before all tests are executed.
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;ExtendWith(PerfTestConfigurationExtension.class)
 * public class YourTest {
 *   // your test methods
 * }
 * </pre>
 *
 * <p>The configuration file should be named "perf-test-config.properties" and should be located in the classpath.</p>
 *
 * <p>Example configuration file:</p>
 * <pre>
 * key1=value1
 * key2=value2
 * </pre>
 *
 * @author Daniel Meyer
 */
public class PerfTestConfigurationExtension implements BeforeAllCallback, TestWatcher {

  private static final String PROPERTY_FILE_NAME = "perf-test-config.properties";

  static PerfTestConfiguration perfTestConfiguration;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (perfTestConfiguration == null) {
      File file = IoUtil.getFile(PROPERTY_FILE_NAME);
      if (!file.exists()) {
        throw new PerfTestException("Cannot load file '" + PROPERTY_FILE_NAME + "': file does not exist.");
      }
      try (FileInputStream propertyInputStream = new FileInputStream(file)) {
        Properties properties = new Properties();
        properties.load(propertyInputStream);
        perfTestConfiguration = new PerfTestConfiguration(properties);
      } catch (Exception e) {
        throw new PerfTestException("Cannot load properties from file " + PROPERTY_FILE_NAME + ": " + e);
      }
    }
  }

  public PerfTestConfiguration getPerformanceTestConfiguration() {
    return perfTestConfiguration;
  }
}
