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
package org.operaton.bpm.spring.boot.starter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

@SpringBootTest(
  classes = { TestApplication.class },
  properties = {
    "operaton.bpm.preview-features-enabled=true"
  },
  webEnvironment = NONE
)
@ActiveProfiles("test")
class PreviewFeaturesSpringBootIT {

  @Autowired
  private ProcessEngine processEngine;

  @Test
  void shouldPropagatePreviewFeaturesFlagToEngineConfiguration() {
    ProcessEngineConfigurationImpl configuration =
        (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    assertThat(configuration.isPreviewFeaturesEnabled()).isTrue();
  }
}
