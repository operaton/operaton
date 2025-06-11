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
package org.operaton.bpm.dmn.engine.api;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationListener;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.metrics.DefaultEngineMetricCollector;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DmnEngineConfigurationApiTest {

  protected DmnEngineConfiguration configuration;

  @BeforeEach
  void initConfiguration() {
    configuration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
  }

  @Test
  void shouldCreateDefaultEngineConfiguration() {
    DmnEngineConfiguration cfg = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    assertThat(cfg).isInstanceOf(DefaultDmnEngineConfiguration.class).isNotNull();
  }

  @Test
  void shouldSetEngineMetricCollector() {
    configuration.setEngineMetricCollector(null);
    assertThat(configuration.getEngineMetricCollector()).isNull();

    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    configuration.setEngineMetricCollector(metricCollector);

    assertThat(configuration.getEngineMetricCollector()).isEqualTo(metricCollector);
  }

  @Test
  void shouldSetFluentEngineMetricCollector() {
    configuration.engineMetricCollector(null);
    assertThat(configuration.getEngineMetricCollector()).isNull();

    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    configuration.engineMetricCollector(metricCollector);

    assertThat(configuration.getEngineMetricCollector()).isEqualTo(metricCollector);
  }

  @Test
  void shouldSetCustomPreDecisionTableEvaluationListeners() {
    configuration.setCustomPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).isNull();

    configuration.setCustomPreDecisionTableEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPreDecisionTableEvaluationListeners() {
    configuration.customPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).isNull();

    configuration.customPreDecisionTableEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPreDecisionEvaluationListeners() {
    configuration.setCustomPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).isNull();

    configuration.setCustomPreDecisionEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPreDecisionEvaluationListeners() {
    configuration.customPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).isNull();

    configuration.customPreDecisionEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPostDecisionTableEvaluationListeners() {
    configuration.setCustomPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).isNull();

    configuration.setCustomPostDecisionTableEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPostDecisionTableEvaluationListeners() {
    configuration.customPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).isNull();

    configuration.customPostDecisionTableEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPostDecisionEvaluationListeners() {
    configuration.setCustomPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).isNull();

    configuration.setCustomPostDecisionEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPostDecisionEvaluationListeners() {
    configuration.customPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).isNull();

    configuration.customPostDecisionEvaluationListeners(Collections.emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners()).containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldBeFluentConfigurable() {
    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    ArrayList<DmnDecisionTableEvaluationListener> preListeners = new ArrayList<>();
    preListeners.add(new DefaultEngineMetricCollector());
    ArrayList<DmnDecisionTableEvaluationListener> postListeners = new ArrayList<>();
    preListeners.add(new DefaultEngineMetricCollector());

    ArrayList<DmnDecisionEvaluationListener> preDecisionListeners = new ArrayList<>();
    preDecisionListeners.add(new TestDecisionEvaluationListener());
    ArrayList<DmnDecisionEvaluationListener> postDecisionListeners = new ArrayList<>();
    postDecisionListeners.add(new TestDecisionEvaluationListener());

    DmnEngine engine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration()
      .engineMetricCollector(metricCollector)
      .customPreDecisionTableEvaluationListeners(preListeners)
      .customPostDecisionTableEvaluationListeners(postListeners)
      .customPreDecisionEvaluationListeners(preDecisionListeners)
      .customPostDecisionEvaluationListeners(postDecisionListeners)
      .buildEngine();

    DmnEngineConfiguration cfg = engine.getConfiguration();
    assertThat(cfg.getEngineMetricCollector()).isEqualTo(metricCollector);
    assertThat(cfg.getCustomPreDecisionTableEvaluationListeners()).containsExactlyElementsOf(preListeners);
    assertThat(cfg.getCustomPostDecisionTableEvaluationListeners()).containsExactlyElementsOf(postListeners);
    assertThat(cfg.getCustomPreDecisionEvaluationListeners()).containsExactlyElementsOf(preDecisionListeners);
    assertThat(cfg.getCustomPostDecisionEvaluationListeners()).containsExactlyElementsOf(
      postDecisionListeners);
  }

  @Test
  void shouldBuildEngine() {
    DmnEngine engine = configuration.buildEngine();
    assertThat(engine).isNotNull();
  }

  // helper
  public static class TestDecisionEvaluationListener implements DmnDecisionEvaluationListener {

    public DmnDecisionEvaluationEvent evaluationEvent;

    @Override
    public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
      this.evaluationEvent = evaluationEvent;
    }

    @SuppressWarnings("unused")
    public DmnDecisionEvaluationEvent getEvaluationEvent() {
      return evaluationEvent;
    }
  }
}
