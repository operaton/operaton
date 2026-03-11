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
package org.operaton.bpm.spring.boot.starter.rest;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jersey.autoconfigure.JerseyApplicationPath;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration;
import org.springframework.context.annotation.Bean;

import org.operaton.bpm.engine.rest.impl.FetchAndLockContextListener;
import org.operaton.bpm.spring.boot.starter.OperatonBpmAutoConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

@AutoConfigureBefore({JerseyAutoConfiguration.class})
@AutoConfigureAfter({OperatonBpmAutoConfiguration.class})
public class OperatonBpmRestJerseyAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OperatonJerseyResourceConfig.class)
  public OperatonJerseyResourceConfig createRestConfig() {
    return new OperatonJerseyResourceConfig();
  }

  @Bean
  public FetchAndLockContextListener getFetchAndLockContextListener() {
    return new FetchAndLockContextListener();
  }

  @Bean
  public OperatonBpmRestInitializer operatonBpmRestInitializer(JerseyApplicationPath applicationPath, OperatonBpmProperties props) {
    return new OperatonBpmRestInitializer(applicationPath, props);
  }
}
