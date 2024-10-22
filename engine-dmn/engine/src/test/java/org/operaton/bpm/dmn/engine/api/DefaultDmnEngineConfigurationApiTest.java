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
import org.junit.Before;
import org.junit.Test;

public class DefaultDmnEngineConfigurationApiTest {

  protected DefaultDmnEngineConfiguration configuration;

    /**
   * Initializes the configuration by creating a new DefaultDmnEngineConfiguration instance.
   */
  @Before
  public void initConfiguration() {
    configuration = new DefaultDmnEngineConfiguration();
  }

    /**
   * Test method to verify setting and getting EngineMetricCollector in the configuration object.
   * It first sets the EngineMetricCollector to null, then verifies that it is null.
   * Next, it creates a new DefaultEngineMetricCollector object, sets it in the configuration object,
   * and verifies that the configuration object now contains the new EngineMetricCollector.
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
   * Test method to verify setting the engine metric collector in the configuration.
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
   * Test method to verify the behavior of setting custom pre-decision table evaluation listeners in the configuration.
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
   * Test method to verify setting fluent custom pre-decision table evaluation listeners in configuration.
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
   * Test method to verify the behavior of setting fluent custom pre-decision evaluation listeners in the configuration.
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
   * Test method to verify the functionality of setting custom pre-decision evaluation listeners in the configuration.
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
   * Test method to verify the functionality of setting custom post decision evaluation listeners in the configuration.
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
   * Test method to verify setting fluent custom post decision table evaluation listeners.
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
   * Test method to verify setting custom post decision evaluation listeners in a fluent manner.
   * Verifies setting null, empty list, and list of listeners.
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
   * Test method to verify the functionality of setting and getting the script engine resolver in the configuration.
   */
  @Test
  public void shouldSetScriptEngineResolver() {
      configuration.setScriptEngineResolver(null);
      assertThat(configuration.getScriptEngineResolver())
        .isNull();
  
      DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();
  
      configuration.setScriptEngineResolver(scriptEngineResolver);
      assertThat(configuration.getScriptEngineResolver())
        .isEqualTo(scriptEngineResolver);
  }

    /**
   * Test method to verify that the script engine resolver can be set and retrieved correctly.
   */
  @Test
  public void shouldSetFluentScriptEngineResolver() {
      configuration.scriptEngineResolver(null);
      assertThat(configuration.getScriptEngineResolver())
        .isNull();
  
      DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();
  
      configuration.scriptEngineResolver(scriptEngineResolver);
      assertThat(configuration.getScriptEngineResolver())
        .isEqualTo(scriptEngineResolver);
  }

    /**
   * Test method to verify setting and getting EL provider in the configuration.
   */
  @Test
  public void shouldSetElProvider() {
    configuration.setElProvider(null);
    assertThat(configuration.getElProvider())
      .isNull();

    ElProvider elProvider = new JuelElProvider();

    configuration.setElProvider(elProvider);
    assertThat(configuration.getElProvider())
      .isEqualTo(elProvider);
  }

    /**
   * Test method to verify that the FluentElProvider is correctly set in the configuration.
   */
  @Test
  public void shouldSetFluentElProvider() {
      configuration.elProvider(null);
      assertThat(configuration.getElProvider())
        .isNull();
  
      ElProvider elProvider = new JuelElProvider();
  
      configuration.elProvider(elProvider);
      assertThat(configuration.getElProvider())
        .isEqualTo(elProvider);
  }

    /**
   * Test method to verify the behavior of setting and getting a FeelEngineFactory in the configuration.
   */
  @Test
  public void shouldSetFeelEngineFactory() {
    configuration.setFeelEngineFactory(null);
    assertThat(configuration.getFeelEngineFactory())
      .isNull();

    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();

    configuration.setFeelEngineFactory(feelEngineFactory);
    assertThat(configuration.getFeelEngineFactory())
      .isEqualTo(feelEngineFactory);
  }

    /**
   * Test method to verify setting and getting the Fluent Feel Engine Factory in the configuration.
   */
  @Test
  public void shouldSetFluentFeelEngineFactory() {
    configuration.feelEngineFactory(null);
    assertThat(configuration.getFeelEngineFactory())
      .isNull();

    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();

    configuration.feelEngineFactory(feelEngineFactory);
    assertThat(configuration.getFeelEngineFactory())
      .isEqualTo(feelEngineFactory);
  }

    /**
   * Test method to verify setting and getting the default input expression expression language in the configuration.
   */
   @Test
    public void shouldSetDefaultInputExpressionExpressionLanguage() {
      configuration.setDefaultInputExpressionExpressionLanguage(null);
      assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
        .isNull();
  
      configuration.setDefaultInputExpressionExpressionLanguage("operaton");
      assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
        .isEqualTo("operaton");
    }

    /**
   * Test method to verify that the default input expression expression language can be set and retrieved fluently.
   */
  @Test
  public void shouldSetFluentDefaultInputExpressionExpressionLanguage() {
    configuration.defaultInputExpressionExpressionLanguage(null);
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isNull();

    configuration.defaultInputExpressionExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputExpressionExpressionLanguage())
      .isEqualTo("operaton");
  }

    /**
   * Test method to verify setting and getting default input entry expression language in configuration.
   */
  @Test
  public void shouldSetDefaultInputEntryExpressionLanguage() {
    configuration.setDefaultInputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isNull();

    configuration.setDefaultInputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultInputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

    /**
   * Test method to verify that the default input entry expression language can be set fluently.
   */
  @Test
  public void shouldSetFluentDefaultInputEntryExpressionLanguage() {
      configuration.defaultInputEntryExpressionLanguage(null);
      assertThat(configuration.getDefaultInputEntryExpressionLanguage())
        .isNull();
  
      configuration.defaultInputEntryExpressionLanguage("operaton");
      assertThat(configuration.getDefaultInputEntryExpressionLanguage())
        .isEqualTo("operaton");
  }

    /**
   * Tests the setting and getting of the default output entry expression language in the configuration.
   */
  @Test
  public void shouldSetDefaultOutputEntryExpressionLanguage() {
      configuration.setDefaultOutputEntryExpressionLanguage(null);
      assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
          .isNull();
  
      configuration.setDefaultOutputEntryExpressionLanguage("operation");
      assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
          .isEqualTo("operation");
  }

    /**
   * Test method to verify if the default output entry expression language can be set using the fluent API.
   */
  @Test
  public void shouldSetFluentDefaultOutputEntryExpressionLanguage() {
    configuration.defaultOutputEntryExpressionLanguage(null);
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isNull();

    configuration.defaultOutputEntryExpressionLanguage("operaton");
    assertThat(configuration.getDefaultOutputEntryExpressionLanguage())
      .isEqualTo("operaton");
  }

    /**
   * Test method to verify that the default literal expression language can be set and retrieved correctly.
   */
  @Test
  public void shouldSetDefaultLiteralExpressionLanguage() {
    configuration.setDefaultLiteralExpressionLanguage(null);
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isNull();

    configuration.setDefaultLiteralExpressionLanguage("operaton");
    assertThat(configuration.getDefaultLiteralExpressionLanguage())
      .isEqualTo("operaton");
  }

    /**
   * Test method to verify setting and getting default literal expression language using fluent API.
   */
  @Test
  public void shouldSetFluentDefaultLiteralExpressionLanguage() {
      configuration.defaultLiteralExpressionLanguage(null);
      assertThat(configuration.getDefaultLiteralExpressionLanguage())
        .isNull();
  
      configuration.defaultLiteralExpressionLanguage("operaton");
      assertThat(configuration.getDefaultLiteralExpressionLanguage())
        .isEqualTo("operaton");
  }

    /**
   * Test method to verify that the transformer can be set and retrieved correctly in the configuration.
   */
  @Test
  public void shouldSetTransformer() {
    configuration.setTransformer(null);
    assertThat(configuration.getTransformer())
      .isNull();

    DmnTransformer transformer = new DefaultDmnTransformer();

    configuration.setTransformer(transformer);
    assertThat(configuration.getTransformer())
      .isEqualTo(transformer);
  }

    /**
   * Test method to verify that the transformer can be set and retrieved correctly using the configuration object.
   */
  @Test
  public void shouldSetFluentTransformer() {
      configuration.transformer(null);
      assertThat(configuration.getTransformer())
        .isNull();
  
      DmnTransformer transformer = new DefaultDmnTransformer();
  
      configuration.transformer(transformer);
      assertThat(configuration.getTransformer())
        .isEqualTo(transformer);
  }

    /**
   * This method tests if the DmnEngine configuration can be fluently customized and built
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
    DefaultScriptEngineResolver scriptEngineResolver = new DefaultScriptEngineResolver();
    ElProvider elProvider = new JuelElProvider();
    FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();
    DmnTransformer transformer = new DefaultDmnTransformer();

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

    /**
   * This method tests if the decision table evaluation listeners are initialized correctly. 
   */
  @Test
  public void shouldInitDecisionTableEvaluationListeners() {
    ArrayList<DmnDecisionTableEvaluationListener> preListeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    preListeners.add(new DefaultEngineMetricCollector());
    ArrayList<DmnDecisionTableEvaluationListener> postListeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    postListeners.add(new DefaultEngineMetricCollector());

    configuration
      .customPreDecisionTableEvaluationListeners(preListeners)
      .customPostDecisionTableEvaluationListeners(postListeners)
      .buildEngine();

    ArrayList<DmnDecisionTableEvaluationListener> expectedListeners = new ArrayList<DmnDecisionTableEvaluationListener>();
    expectedListeners.addAll(preListeners);
    expectedListeners.addAll(postListeners);

    assertThat(configuration.getDecisionTableEvaluationListeners())
      .containsExactlyElementsOf(expectedListeners);
  }

    /**
   * Initializes decision evaluation listeners by adding pre and post listeners to the configuration.
   * The method builds the engine and sets the expected listeners based on the pre and post listeners added.
   * Finally, it asserts that the decision evaluation listeners match the expected listeners.
   */
  @Test
  public void shouldInitDecisionEvaluationListeners() {
      ArrayList<DmnDecisionEvaluationListener> preListeners = new ArrayList<DmnDecisionEvaluationListener>();
      preListeners.add(new TestDecisionEvaluationListener());
      ArrayList<DmnDecisionEvaluationListener> postListeners = new ArrayList<DmnDecisionEvaluationListener>();
      postListeners.add(new TestDecisionEvaluationListener();
  
      configuration
        .customPreDecisionEvaluationListeners(preListeners)
        .customPostDecisionEvaluationListeners(postListeners)
        .buildEngine();
  
      ArrayList<DmnDecisionEvaluationListener> expectedListeners = new ArrayList<DmnDecisionEvaluationListener>();
      expectedListeners.addAll(preListeners);
      expectedListeners.add((DefaultEngineMetricCollector) configuration.getEngineMetricCollector());
      expectedListeners.addAll(postListeners);
  
      assertThat(configuration.getDecisionEvaluationListeners())
        .containsExactlyElementsOf(expectedListeners);
  }

    /**
   * Test method to initialize the FeelEngine by setting the FeelEngineFactory in the configuration,
   * building the engine, and asserting that the FeelEngine is an instance of FeelEngineImpl and is not null.
   */
  @Test
  public void shouldInitFeelEngine() {
      FeelEngineFactory feelEngineFactory = new FeelEngineFactoryImpl();
      configuration.setFeelEngineFactory(feelEngineFactory);
  
      configuration.buildEngine();
  
      assertThat(configuration.getFeelEngine())
        .isInstanceOf(FeelEngineImpl.class)
        .isNotNull();
  }

    /**
   * Test method to verify that the default DMN engine is built correctly.
   */
  @Test
  public void shouldBuildDefaultDmnEngine() {
    DmnEngine engine = configuration.buildEngine();
    assertThat(engine)
      .isInstanceOf(DefaultDmnEngine.class)
      .isNotNull();
  }

  // helper
  public static class TestDecisionEvaluationListener implements DmnDecisionEvaluationListener {

    public DmnDecisionEvaluationEvent evaluationEvent;

        /**
     * Sets the evaluation event for the DMN decision.
     * 
     * @param evaluationEvent the event to set
     */
    public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
      this.evaluationEvent = evaluationEvent;
    }

        /**
     * Returns the evaluation event associated with this decision.
     *
     * @return the evaluation event
     */
    public DmnDecisionEvaluationEvent getEvaluationEvent() {
      return evaluationEvent;
    }
  }
}
