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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDeploymentConfigurationTest {

  private DefaultDeploymentConfiguration defaultDeploymentConfiguration;
  private final OperatonBpmProperties operatonBpmProperties = new OperatonBpmProperties();
  private final SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();

  @BeforeEach
  void before() {
    defaultDeploymentConfiguration = new DefaultDeploymentConfiguration(operatonBpmProperties);
  }

  @Test
  void noDeploymentTest() {
    operatonBpmProperties.setAutoDeploymentEnabled(false);
    defaultDeploymentConfiguration.preInit(configuration);

    assertThat(configuration.getDeploymentResources()).isEmpty();
  }

  @Test
  void deploymentTest() {
    operatonBpmProperties.setAutoDeploymentEnabled(true);
    defaultDeploymentConfiguration.preInit(configuration);

    final Resource[] resources = configuration.getDeploymentResources();
    assertThat(resources).hasSize(11);

    assertThat(filenames(resources)).containsOnly("async-service-task.bpmn", "test.cmmn10.xml", "test.bpmn",
        "test.cmmn", "test.bpmn20.xml", "check-order.dmn", "eventing.bpmn", "spin-java8-model.bpmn",
        "eventingWithTaskAssignee.bpmn", "eventingWithBoundary.bpmn", "eventingWithIntermediateCatch.bpmn");
  }

  private Set<String> filenames(Resource[] resources) {
    Set<String> filenames = new HashSet<>();
    for (Resource resource : resources) {
      filenames.add(resource.getFilename());
    }
    return filenames;
  }
}
