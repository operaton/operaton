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
package org.operaton.bpm.spring.boot.starter.util;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.engine.spring.SpringProcessEnginePlugin;

import static org.operaton.bpm.spring.boot.starter.util.OperatonSpringBootUtil.processEngineImpl;
import static org.operaton.bpm.spring.boot.starter.util.OperatonSpringBootUtil.springProcessEngineConfiguration;

/**
 * Convenience class that specializes {@link AbstractProcessEnginePlugin} to
 * use {@link SpringProcessEngineConfiguration} (to save casting).
 */
public class SpringBootProcessEnginePlugin extends SpringProcessEnginePlugin {


  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    springProcessEngineConfiguration(processEngineConfiguration)
      .ifPresent(this::preInit);
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    springProcessEngineConfiguration(processEngineConfiguration)
      .ifPresent(this::postInit);
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
    processEngineImpl(processEngine).ifPresent(this::postProcessEngineBuild);
  }

  public void preInit(SpringProcessEngineConfiguration processEngineConfiguration) {
    // nothing to do
  }

  public void postInit(SpringProcessEngineConfiguration processEngineConfiguration) {
    // nothing to do
  }

  public void postProcessEngineBuild(ProcessEngineImpl processEngine) {
    // noop
  }
}
