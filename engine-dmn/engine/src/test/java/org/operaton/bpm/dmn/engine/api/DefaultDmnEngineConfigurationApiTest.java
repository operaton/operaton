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

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationListener;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngine;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.el.DefaultScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.el.JuelElProvider;
import org.operaton.bpm.dmn.engine.impl.metrics.DefaultEngineMetricCollector;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.dmn.engine.impl.transform.DefaultDmnTransformer;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.operaton.bpm.dmn.feel.impl.juel.FeelEngineImpl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultDmnEngineConfigurationApiTest {

  protected DefaultDmnEngineConfiguration configuration;

  @BeforeEach
  void initConfiguration() {
    configuration = new DefaultDmnEngineConfiguration();
  }

  @Test
  void shouldSetEngineMetricCollector() {
    configuration.setEngineMetricCollector(null);
    assertThat(configuration.getEngineMetricCollector())
      .isNull();

    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    configuration.setEngineMetricCollector(metricCollector);

    assertThat(configuration.getEngineMetricCollector())
      .isEqualTo(metricCollector);
    }

  @Test
  void shouldSetFluentEngineMetricCollector() {
    configuration.engineMetricCollector(null);
    assertThat(configuration.getEngineMetricCollector())
      .isNull();

    DefaultEngineMetricCollector metricCollector = new DefaultEngineMetricCollector();
    configuration.engineMetricCollector(metricCollector);

    assertThat(configuration.getEngineMetricCollector())
      .isEqualTo(metricCollector);
  }

  @Test
  void shouldSetCustomPreDecisionTableEvaluationListeners() {
    configuration.setCustomPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isNull();

    configuration.setCustomPreDecisionTableEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPreDecisionTableEvaluationListeners() {
    configuration.customPreDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isNull();

    configuration.customPreDecisionTableEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPreDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPreDecisionEvaluationListeners() {
    configuration.customPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isNull();

    configuration.customPreDecisionEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPostDecisionTableEvaluationListeners() {
    configuration.setCustomPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isNull();

    configuration.setCustomPostDecisionTableEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.setCustomPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPreDecisionEvaluationListeners() {
    configuration.setCustomPreDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isNull();

    configuration.setCustomPreDecisionEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPreDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPreDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetCustomPostDecisionEvaluationListeners() {
    configuration.setCustomPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isNull();

    configuration.setCustomPostDecisionEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.setCustomPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPostDecisionTableEvaluationListeners() {
    configuration.customPostDecisionTableEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isNull();

    configuration.customPostDecisionTableEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new DefaultEngineMetricCollector());
    listeners.add(new DefaultEngineMetricCollector());

    configuration.customPostDecisionTableEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetFluentCustomPostDecisionEvaluationListeners() {
    configuration.customPostDecisionEvaluationListeners(null);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isNull();

    configuration.customPostDecisionEvaluationListeners(emptyList());
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .isEmpty();

    ArrayList<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    listeners.add(new TestDecisionEvaluationListener());
    listeners.add(new TestDecisionEvaluationListener());

    configuration.customPostDecisionEvaluationListeners(listeners);
    assertThat(configuration.getCustomPostDecisionEvaluationListeners())
      .containsExactlyElementsOf(listeners);
  }

  @Test
  void shouldSetScriptEngineResolver() {
    configuration.setScriptEngineResolver(null);
    assertThat(configuration.getScriptEngineResolver())
      .isNull();

    DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();

    configuration.setScriptEngineResolver(scriptEngineResolver);
    assertThat(configuration.getScriptEngineResolver())
      .isEqualTo(scriptEngineResolver);
  }

  @Test
  void shouldSetFluentScriptEngineResolver() {
    configuration.scriptEngineResolver(null);
    assertThat(configuration.getScriptEngineResolver())
      .isNull();

    DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();

    configuration.scriptEngineResolver(scriptEngineResolver);
    assertThat(configuration.getScriptEngineResolver())
      .isEqualTo(scriptEngineResolver);
  }

  @Test
  void shouldSetElProvider() {
    configuration.setElProvider(null);
    assertThat(configuration.getElProvider())
      .isNull();

    ElProvider elProvider = new JuelElProvider();

    configuration.setElProvider(elProvider);
    assertThat(configuration.getElProvider())
      .isEqualTo(elProvider);
  }

  @Test
  void shouldSetFluentElProvider() {
    configuration.elProvider(null);
    assertThat(configuration.getElProvider())
      .isNull();

    ElProvider elProvider = new JuelElProvider();

    configuration.elProvider(elProvider);
    assertThat(configuration.getElProvider())
      .isEqualTo(elProvider);
  }

  @Test
  void shouldSetFeelEngineFactory() {
    configuration.setFeelEngineFactory(null);
    assertThat(configuration.getFeelEngineFactory())
      .isNull();

    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();

    configuration.setFeelEngineFactory(feelEngineFactory);
    assertThat(configuration.getFeelEngineFactory())
      .isEqualTo(feelEngineFactory);
  }

  @Test
  void shouldSetFluentFeelEngineFactory() {
    configuration.feelEngineFactory(null);
    assertThat(configuration.getFeelEngineFactory())
      .isNull();

    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();

    configuration.feelEngineFactory(feelEngineFactory);
    assertThat(configuration.getFeelEngineFactory())
      .isEqualTo(feelEngineFactory);
  }

  @Test
  void shouldSetDefaultInputExpressionExpressionLanguage() {
    configuration.setDefaultInputExpressionExpressionLanguage(null);
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isNull();

    configuration.setDefaultInputExpressionExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetFluentDefaultInputExpressionExpressionLanguage() {
    configuration.defaultInputExpressionExpressionLanguage(null);
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isNull();

    configuration.defaultInputExpressionExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetDefaultInputEntryExpressionLanguage() {
    configuration.setDefaultInputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isNull();

    configuration.setDefaultInputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetFluentDefaultInputEntryExpressionLanguage() {
    configuration.defaultInputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isNull();

    configuration.defaultInputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetDefaultOutputEntryExpressionLanguage() {
    configuration.setDefaultOutputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isNull();

    configuration.setDefaultOutputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetFluentDefaultOutputEntryExpressionLanguage() {
    configuration.defaultOutputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isNull();

    configuration.defaultOutputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetDefaultLiteralExpressionLanguage() {
    configuration.setDefaultLiteralExpressionLanguage(null);
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isNull();

    configuration.setDefaultLiteralExpressionLanguage("operaton");
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetFluentDefaultLiteralExpressionLanguage() {
    configuration.defaultLiteralExpressionLanguage(null);
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isNull();

    configuration.defaultLiteralExpressionLanguage("operaton");
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isEqualTo("operaton");
  }

  @Test
  void shouldSetTransformer() {
    configuration.setTransformer(null);
    assertThat(configuration.getTransformer())
      .isNull();

    DmnTransformer transformer = new DefaultDmnTransformer(configuration);

    configuration.setTransformer(transformer);
    assertThat(configuration.getTransformer())
      .isEqualTo(transformer);
  }

  @Test
  void shouldSetFluentTransformer() {
    configuration.transformer(null);
    assertThat(configuration.getTransformer())
      .isNull();

    DmnTransformer transformer = new DefaultDmnTransformer(configuration);

    configuration.transformer(transformer);
    assertThat(configuration.getTransformer())
      .isEqualTo(transformer);
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
    DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();
    ElProvider elProvider = new JuelElProvider();
    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();
    DmnTransformer transformer = new DefaultDmnTransformer(configuration);

    DmnEngine engine = configuration
      .engineMetricCollector(metricCollector)
      .customPreDecisionTableEvaluationListeners(preListeners)
      .customPostDecisionTableEvaluationListeners(postListeners)
      .customPreDecisionEvaluationListeners(preDecisionListeners)
      .customPostDecisionEvaluationListeners(postDecisionListeners)
      .scriptEngineResolver(scriptEngineResolver)
      .elProvider(elProvider)
      .feelEngineFactory(feelEngineFactory)
      .defaultInputExpressionExpressionLanguage("operaton")
      .defaultInputEntryExpressionLanguage("operaton")
      .defaultOutputEntryExpressionLanguage("operaton")
      .transformer(transformer)
      .buildEngine();

    configuration = (DefaultDmnEngineConfiguration) engine.getConfiguration();
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
    assertThat(configuration.getScriptEngineResolver())
      .isEqualTo(scriptEngineResolver);
    assertThat(configuration.getElProvider())
      .isEqualTo(elProvider);
    assertThat(configuration.getFeelEngineFactory())
      .isEqualTo(feelEngineFactory);
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isEqualTo("operaton");
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isEqualTo("operaton");
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isEqualTo("operaton");
    assertThat(configuration.getTransformer())
      .isEqualTo(transformer);
  }

  @Test
  void shouldInitDecisionTableEvaluationListeners() {
    ArrayList<DmnDecisionTableEvaluationListener> preListeners = new ArrayList<>();
    preListeners.add(new DefaultEngineMetricCollector());
    ArrayList<DmnDecisionTableEvaluationListener> postListeners = new ArrayList<>();
    postListeners.add(new DefaultEngineMetricCollector());

    configuration
      .customPreDecisionTableEvaluationListeners(preListeners)
      .customPostDecisionTableEvaluationListeners(postListeners)
      .buildEngine();

    ArrayList<DmnDecisionTableEvaluationListener> expectedListeners = new ArrayList<>();
    expectedListeners.addAll(preListeners);
    expectedListeners.addAll(postListeners);

    assertThat(configuration.getDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(expectedListeners);
  }

  @Test
  void shouldInitDecisionEvaluationListeners() {
    ArrayList<DmnDecisionEvaluationListener> preListeners = new ArrayList<>();
    preListeners.add(new TestDecisionEvaluationListener());
    ArrayList<DmnDecisionEvaluationListener> postListeners = new ArrayList<>();
    postListeners.add(new TestDecisionEvaluationListener());

    configuration
      .customPreDecisionEvaluationListeners(preListeners)
      .customPostDecisionEvaluationListeners(postListeners)
      .buildEngine();

    ArrayList<DmnDecisionEvaluationListener> expectedListeners = new ArrayList<>(preListeners);
    expectedListeners.add((DefaultEngineMetricCollector) configuration.getEngineMetricCollector());
    expectedListeners.addAll(postListeners);

    assertThat(configuration.getDecisionEvaluationListeners())
      .containsExactlyElementsOf(expectedListeners);
  }

  @Test
  void shouldInitFeelEngine() {
    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();
    configuration.setFeelEngineFactory(feelEngineFactory);

    configuration.buildEngine();

    assertThat(configuration.getFeelEngine())
      .isInstanceOf(FeelEngineImpl.class)
      .isNotNull();
  }

  @Test
  void shouldBuildDefaultDmnEngine() {
    DmnEngine engine = configuration.buildEngine();
    assertThat(engine)
      .isInstanceOf(DefaultDmnEngine.class)
      .isNotNull();
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
