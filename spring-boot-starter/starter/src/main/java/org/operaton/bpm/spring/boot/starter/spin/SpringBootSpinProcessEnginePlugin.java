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
package org.operaton.bpm.spring.boot.starter.spin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.support.SpringFactoriesLoader;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;
import org.operaton.spin.DataFormats;
import org.operaton.spin.plugin.impl.SpinProcessEnginePlugin;
import org.operaton.spin.spi.DataFormatConfigurator;

public class SpringBootSpinProcessEnginePlugin extends SpinProcessEnginePlugin {

  protected Optional<OperatonJacksonFormatConfiguratorJSR310> dataFormatConfiguratorJsr310;

  protected Optional<OperatonJacksonFormatConfiguratorParameterNames> dataFormatConfiguratorParameterNames;

  protected Optional<OperatonJacksonFormatConfiguratorJdk8> dataFormatConfiguratorJdk8;

  public SpringBootSpinProcessEnginePlugin(Optional<OperatonJacksonFormatConfiguratorJSR310> dataFormatConfiguratorJsr310,
                                           Optional<OperatonJacksonFormatConfiguratorParameterNames> dataFormatConfiguratorParameterNames,
                                           Optional<OperatonJacksonFormatConfiguratorJdk8> dataFormatConfiguratorJdk8) {
    this.dataFormatConfiguratorJsr310 = dataFormatConfiguratorJsr310;
    this.dataFormatConfiguratorParameterNames = dataFormatConfiguratorParameterNames;
    this.dataFormatConfiguratorJdk8 = dataFormatConfiguratorJdk8;
  }

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    ClassLoader classloader = ClassLoaderUtil.getClassloader(SpringBootSpinProcessEnginePlugin.class);
    loadSpringBootDataFormats(classloader);
  }

  protected void loadSpringBootDataFormats(ClassLoader classloader) {
    List configurators = new ArrayList<>();

    // add the auto-config Jackson Java 8 module configurators
    dataFormatConfiguratorJsr310.ifPresent(configurators::add);
    dataFormatConfiguratorParameterNames.ifPresent(configurators::add);
    dataFormatConfiguratorJdk8.ifPresent(configurators::add);

    // next, add any configurators defined in the spring.factories file
    configurators.addAll(SpringFactoriesLoader.loadFactories(DataFormatConfigurator.class, classloader));

    DataFormats.loadDataFormats(classloader, configurators);
  }
}
