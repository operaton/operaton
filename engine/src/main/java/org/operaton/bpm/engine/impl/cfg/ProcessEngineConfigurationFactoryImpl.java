/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cfg;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.spi.ProcessEngineConfigurationFactory;

import java.io.InputStream;

/**
 * @since 1.1
 */
public class ProcessEngineConfigurationFactoryImpl implements ProcessEngineConfigurationFactory {
  private static final String BEAN_PROCESS_ENGINE_CONFIGURATION = "processEngineConfiguration";

  public ProcessEngineConfiguration createProcessEngineConfigurationFromResourceDefault() {
    ProcessEngineConfiguration processEngineConfiguration = null;
    try {
      processEngineConfiguration = createProcessEngineConfigurationFromResource("operaton.cfg.xml",
              BEAN_PROCESS_ENGINE_CONFIGURATION);
    } catch (RuntimeException ex) {
      processEngineConfiguration = createProcessEngineConfigurationFromResource("activiti.cfg.xml",
              BEAN_PROCESS_ENGINE_CONFIGURATION);
    }
    return processEngineConfiguration;
  }
  
  public ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource) {
    return createProcessEngineConfigurationFromResource(resource, BEAN_PROCESS_ENGINE_CONFIGURATION);
  }

  public  ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
    return BeansConfigurationHelper.parseProcessEngineConfigurationFromResource(resource, beanName);
  }

  public  ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream) {
    return createProcessEngineConfigurationFromInputStream(inputStream, BEAN_PROCESS_ENGINE_CONFIGURATION);
  }

  public  ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return BeansConfigurationHelper.parseProcessEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public  ProcessEngineConfiguration createStandaloneProcessEngineConfiguration() {
    return new StandaloneProcessEngineConfiguration();
  }

  public  ProcessEngineConfiguration createStandaloneInMemProcessEngineConfiguration() {
    return new StandaloneInMemProcessEngineConfiguration();
  }
}
