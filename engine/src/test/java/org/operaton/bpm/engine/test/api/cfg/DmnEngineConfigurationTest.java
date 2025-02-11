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
package org.operaton.bpm.engine.test.api.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnEngine;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.dmn.el.ProcessEngineJuelElProvider;
import org.junit.After;
import org.junit.Test;

/**
 * @author Philipp Ossler
 */
public class DmnEngineConfigurationTest {

  protected static final String CONFIGURATION_XML = "org/operaton/bpm/engine/test/api/cfg/custom-dmn-operaton.cfg.xml";

  protected ProcessEngine engine;

  @After
  public void tearDown() {
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Test
  public void setDefaultInputExpressionLanguage() {
    // given a DMN engine configuration with default expression language
    DefaultDmnEngineConfiguration dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    dmnEngineConfiguration.setDefaultInputExpressionExpressionLanguage("groovy");

    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    processEngineConfiguration.setDmnEngineConfiguration(dmnEngineConfiguration);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the default expression language should be set on the DMN engine
    assertThat(getConfigurationOfDmnEngine().getDefaultInputExpressionExpressionLanguage()).isEqualTo("groovy");
  }

  @Test
  public void setCustomPostTableExecutionListener() {
    // given a DMN engine configuration with custom listener
    DefaultDmnEngineConfiguration dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    DmnDecisionTableEvaluationListener customEvaluationListener = mock(DmnDecisionTableEvaluationListener.class);
    List<DmnDecisionTableEvaluationListener> customListeners = new ArrayList<>();
    customListeners.add(customEvaluationListener);
    dmnEngineConfiguration.setCustomPostDecisionTableEvaluationListeners(customListeners);

    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    processEngineConfiguration.setDmnEngineConfiguration(dmnEngineConfiguration);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the custom listener should be set on the DMN engine
    assertThat(getConfigurationOfDmnEngine().getCustomPostDecisionTableEvaluationListeners()).contains(customEvaluationListener);
  }

  @Test
  public void setFeelEngineFactory() {
    // given a DMN engine configuration with feel engine factory
    DefaultDmnEngineConfiguration dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    FeelEngineFactory feelEngineFactory = mock(FeelEngineFactory.class);
    dmnEngineConfiguration.setFeelEngineFactory(feelEngineFactory);

    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    processEngineConfiguration.setDmnEngineConfiguration(dmnEngineConfiguration);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the feel engine factory should be set on the DMN engine
    assertThat(getConfigurationOfDmnEngine().getFeelEngineFactory()).isSameAs(feelEngineFactory);
  }

  @Test
  public void setScriptEngineResolver() {
    // given a DMN engine configuration with script engine resolver
    DefaultDmnEngineConfiguration dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    DmnScriptEngineResolver scriptEngineResolver = mock(DmnScriptEngineResolver.class);
    dmnEngineConfiguration.setScriptEngineResolver(scriptEngineResolver);

    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    processEngineConfiguration.setDmnEngineConfiguration(dmnEngineConfiguration);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the script engine resolver should be set on the DMN engine
    assertThat(getConfigurationOfDmnEngine().getScriptEngineResolver()).isSameAs(scriptEngineResolver);
  }

  @Test
  public void setElProvider() {
    // given a DMN engine configuration with el provider
    DefaultDmnEngineConfiguration dmnEngineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    ElProvider elProvider = mock(ElProvider.class);
    dmnEngineConfiguration.setElProvider(elProvider);

    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    processEngineConfiguration.setDmnEngineConfiguration(dmnEngineConfiguration);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the el provider should be set on the DMN engine
    assertThat(getConfigurationOfDmnEngine().getElProvider()).isSameAs(elProvider);
  }

  @Test
  public void setProcessEngineElProviderByDefault() {
    // given a default DMN engine configuration without el provider
    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the DMN engine should use the process engine el provider
    assertThat(getConfigurationOfDmnEngine().getElProvider().getClass()).isEqualTo(ProcessEngineJuelElProvider.class);
  }

  @Test
  public void setProvidedElProvider() {
    // given provided a el provider in process engine configuration
    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();
    ElProvider elProvider = mock(ElProvider.class);
    processEngineConfiguration.setDmnElProvider(elProvider);

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the DMN engine should use the provided el provider
    assertThat(getConfigurationOfDmnEngine().getElProvider()).isEqualTo(elProvider);
  }

  @Test
  public void setProcessEngineScriptEnginesByDefault() {
    // given a default DMN engine configuration without script engine resolver
    ProcessEngineConfigurationImpl processEngineConfiguration = createProcessEngineConfiguration();

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the DMN engine should use the script engines from the process engine
    assertThat(getConfigurationOfDmnEngine().getScriptEngineResolver()).isEqualTo(processEngineConfiguration.getScriptingEngines());
  }

  @Test
  public void setDmnEngineConfigurationOverXmlConfiguration() {
    // given an embedded DMN engine configuration in XML process engine configuration
    // with default expression language
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource(CONFIGURATION_XML);

    // checks that the configuration is set as on XML
    DefaultDmnEngineConfiguration dmnEngineConfiguration = processEngineConfiguration.getDmnEngineConfiguration();
    assertThat(dmnEngineConfiguration).isNotNull();
    assertThat(dmnEngineConfiguration.getDefaultInputExpressionExpressionLanguage()).isEqualTo("groovy");

    // when the engine is initialized
    engine = processEngineConfiguration.buildProcessEngine();

    // then the default expression language should be set in the DMN engine
    assertThat(getConfigurationOfDmnEngine().getDefaultInputExpressionExpressionLanguage()).isEqualTo("groovy");
  }

  protected ProcessEngineConfigurationImpl createProcessEngineConfiguration() {
    return (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
        .setJdbcUrl("jdbc:h2:mem:operaton" + getClass().getSimpleName());
  }

  protected DefaultDmnEngineConfiguration getConfigurationOfDmnEngine() {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();

    DmnEngine dmnEngine = processEngineConfiguration.getDmnEngine();
    return (DefaultDmnEngineConfiguration) dmnEngine.getConfiguration();
  }

}
