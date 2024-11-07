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
package org.operaton.bpm.run.test.config.deploy;

import org.operaton.bpm.run.OperatonBpmRunProcessEngineConfiguration;
import org.operaton.bpm.run.property.OperatonBpmRunDeploymentProperties;
import org.operaton.bpm.run.test.AbstractRestTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = { OperatonBpmRunDeploymentProperties.PREFIX + ".deploy-changed-only=true" })
public class DeployChangedOnlyEnabledTest extends AbstractRestTest {

  @Autowired
  private OperatonBpmRunProcessEngineConfiguration engineConfig;

  @Test
  public void shouldEnableDeployChangedOnlyOnOperatonRunProperty() {
    assertThat(engineConfig.isDeployChangedOnly()).isEqualTo(true);
  }
}