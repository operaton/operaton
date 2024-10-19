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
package org.operaton.bpm.dmn.engine.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationListener;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.metrics.DefaultEngineMetricCollector;
import org.junit.Before;
import org.junit.Test;

public class DmnEngineConfigurationApiTest {

  protected DmnEngineConfiguration configuration;

    /**
   * Initializes the configuration for the DMN engine.
   */
  @Before
  public void initConfiguration() {
    configuration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
  }

    /**
   * Test method to verify that a default engine configuration is created successfully.
   */
  @Test
  public void shouldCreateDefaultEngineConfiguration() {
    DmnEngineConfiguration configuration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    assertThat(configuration)
      .isInstanceOf(DefaultDmnEngineConfiguration.class)
      .isNotNull();
  }

    /**
   * Test method to verify the setting and retrieval of the engine metric collector in the configuration.
   */
  @Test
    public void shouldSetEngineMetricCollector() {
      configuration.setEngineMetricCollector(null);
      assertThat(configuration.getEngineMetricCollector())
        .isNull();
  
      DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
      configuration.setEngineMetricCollector(metricCollector);
  
      assertThat(configuration.getEngineMetricCollector())
        .isEqualTo(metricCollector);
      }

    /**
   * Test method to verify the setting of the FluentEngineMetricCollector in the configuration.
   */
  @Test
  public void shouldSetFluentEngineMetricCollector() {
    configuration.engineMetricCollector(null);
    assertThat(configuration.getEngineMetricCollector())
      .isNull();

    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    configuration.engineMetricCollector(metricCollector);

    assertThat(configuration.getEngineMetricCollector())
      .isEqualTo(metricCollector);
  }

    /**
   * Test method to verify the behavior of setting custom pre decision table evaluation listeners in the configuration.
   * 
   * It sets the listeners to null, empty list, and a list with two DefaultEngineMetricCollector instances, 
   * and then asserts the behavior of the configuration getters.
   */
  @Test
  public void shouldSetCustomPreDecisionTableEvaluationListeners() {
    configuration.setCustomPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isNull();

    configuration.setCustomPreDecisionTableEvaluationListeners(Collections.<DmnDecisionTableEvaluationListener>emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method to verify the behavior of setting fluent custom pre-decision table evaluation listeners.
   */
  @Test
  public void shouldSetFluentCustomPreDecisionTableEvaluationListeners() {
    configuration.customPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isNull();

    configuration.customPreDecisionTableEvaluationListeners(Collections.<DmnDecisionTableEvaluationListener>emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method for setting custom pre-decision evaluation listeners.
   * Verifies that the configuration can set custom listeners to null, an empty list, 
   * or a list of specific decision evaluation listeners.
   */
  @Test
  public void shouldSetCustomPreDecisionEvaluationListeners() {
    configuration.setCustomPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isNull();

    configuration.setCustomPreDecisionEvaluationListeners(Collections.<DmnDecisionEvaluationListener>emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<DmnDecisionEvaluationListener>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method to verify the setting of custom pre-decision evaluation listeners in the configuration.
   */
  @Test
  public void shouldSetFluentCustomPreDecisionEvaluationListeners() {
    configuration.customPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isNull();

    configuration.customPreDecisionEvaluationListeners(Collections.<DmnDecisionEvaluationListener>emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<DmnDecisionEvaluationListener>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method to verify the functionality of setting custom post decision table evaluation listeners in the configuration.
   */
  @Test
  public void shouldSetCustomPostDecisionTableEvaluationListeners() {
    configuration.setCustomPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isNull();

    configuration.setCustomPostDecisionTableEvaluationListeners(Collections.<DmnDecisionTableEvaluationListener>emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method to verify setting custom post decision table evaluation listeners in configuration.
   */
  @Test
  public void shouldSetFluentCustomPostDecisionTableEvaluationListeners() {
    configuration.customPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isNull();

    configuration.customPostDecisionTableEvaluationListeners(Collections.<DmnDecisionTableEvaluationListener>emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method for setting custom post decision evaluation listeners in the configuration.
   * Verifies that the listeners are set correctly and can be retrieved as expected.
   */
  @Test
  public void shouldSetCustomPostDecisionEvaluationListeners() {
    configuration.setCustomPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isNull();

    configuration.setCustomPostDecisionEvaluationListeners(Collections.<DmnDecisionEvaluationListener>emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<DmnDecisionEvaluationListener>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * Test method to verify setting fluent custom post decision evaluation listeners.
   * It sets listeners to null, empty list, and a list with test listeners, then asserts the configuration.
   */
  @Test
  public void shouldSetFluentCustomPostDecisionEvaluationListeners() {
    configuration.customPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isNull();

    configuration.customPostDecisionEvaluationListeners(Collections.<DmnDecisionEvaluationListener>emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<DmnDecisionEvaluationListener>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

    /**
   * This method tests the configurability of the DmnEngine by setting various custom listeners and metric collectors
   * and then verifying that the configurations are correctly set.
   */
  @Test
  public void shouldBeFluentConfigurable() {
    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    ArrayList<DmnDecisionTableEvaluationListener> preListeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    preListeners.add(new DefaultEngineMetricCollector());
    ArrayList<DmnDecisionTableEvaluationListener> postListeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    preListeners.add(new DefaultEngineMetricCollector());

    ArrayList<DmnDecisionEvaluationListener> preDecisionListeners = new ArrayList<DmnDecisionEvaluationListener>();
    preDecisionListeners.add(new TestDecisionEvaluationListener());
    ArrayList<DmnDecisionEvaluationListener> postDecisionListeners = new ArrayList<DmnDecisionEvaluationListener>();
    postDecisionListeners.add(new TestDecisionEvaluationListener());

    DmnEngine engine = DmnEngineConfiguration
      .createDefaultDmnEngineConfiguration()
      .engineMetricCollector(metricCollector)
      .customPreDecisionTableEvaluationListeners(preListeners)
      .customPostDecisionTableEvaluationListeners(postListeners)
      .customPreDecisionEvaluationListeners(preDecisionListeners)
      .customPostDecisionEvaluationListeners(postDecisionListeners)
      .buildEngine();

    DmnEngineConfiguration configuration = engine.getConfiguration();
    assertThat(configuration.getEngineMetricCollector())
      .isEqualTo(metricCollector);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(preListeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(postListeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .containsExactlyElementsOf(preDecisionListeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .containsExactlyElementsOf(postDecisionListeners);
  }

    /**
   * Test method to verify the successful creation of a DmnEngine using the configuration.
   */
  @Test
  public void shouldBuildEngine() {
    DmnEngine engine = configuration.buildEngine();
    assertThat(engine).isNotNull();
  }

  // helper
  public static class TestDecisionEvaluationListener implements DmnDecisionEvaluationListener {

    public DmnDecisionEvaluationEvent evaluationEvent;

        /**
     * This method sets the provided DmnDecisionEvaluationEvent as the evaluation event.
     * 
     * @param evaluationEvent the DmnDecisionEvaluationEvent to be set
     */
    public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
          this.evaluationEvent = evaluationEvent;
        }

        /**
     * Returns the evaluation event associated with the decision.
     *
     * @return the evaluation event
     */
    public DmnDecisionEvaluationEvent getEvaluationEvent() {
      return evaluationEvent;
    }
  }
}
