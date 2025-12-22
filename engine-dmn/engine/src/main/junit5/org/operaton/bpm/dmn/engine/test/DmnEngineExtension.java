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
package org.operaton.bpm.dmn.engine.test;

import java.util.Objects;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;

/**
 * JUnit 5 extension for {@link DmnEngine} initialization.
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * public class YourDmnTest {
 *
 * <p>
 *   public class YourDmnTest {
 * </p>
 *
 * <p>
 *     &#64;RegisterExtension
 *     public static DmnEngineExtension dmnEngineExtension = new DmnEngineExtension();
 * </p>
 *
 * <p>
 *     ...
 *   }
 * </pre>
 * </p>
 *
 * <p>
 * The DMN engine will be made available to the test class
 * through the getters of the {@code dmnEngineExtension} (see {@link #getDmnEngine()}).
 * The DMN engine will be initialized with the default DMN engine configuration.
 * To specify a different configuration, pass the configuration to the
 * {@link #DmnEngineExtension(DmnEngineConfiguration)} constructor.
 * </p>
 */
public class DmnEngineExtension implements BeforeEachCallback {

  protected DmnEngine dmnEngine;
  protected DmnEngineConfiguration dmnEngineConfiguration;

  /**
   * Creates a {@link DmnEngine} with the default {@link DmnEngineConfiguration}
   */
  public DmnEngineExtension() {
    this(null);
  }

  /**
   * Creates a {@link DmnEngine} with the given {@link DmnEngineConfiguration}
   */
  public DmnEngineExtension(DmnEngineConfiguration dmnEngineConfiguration) {
    this.dmnEngineConfiguration = Objects.requireNonNullElseGet(dmnEngineConfiguration,
        DmnEngineConfiguration::createDefaultDmnEngineConfiguration);
  }

  /**
   * @return the {@link DmnEngine}
   */
  public DmnEngine getDmnEngine() {
    return dmnEngine;
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    if (dmnEngine == null) {
      dmnEngine = dmnEngineConfiguration.buildEngine();
    }
  }
}
