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
package org.operaton.bpm.integrationtest.service;

import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestConstants;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(ArquillianExtension.class)
public class PlatformServicesJndiBindingTest extends AbstractFoxPlatformIntegrationTest {

  protected TestConstants testConstants = new TestConstants();

  @Deployment
  public static WebArchive app1() {
    return initWebArchiveDeployment();
  }

  @Test
  void testProcessApplicationServiceBinding() {
    assertThatCode(() -> InitialContext.doLookup(testConstants.getProcessApplicationService()))
      .withFailMessage("Failed to lookup ProcessApplicationService '%s'".formatted(TestConstants.PROCESS_APPLICATION_SERVICE_JNDI_NAME))
      .doesNotThrowAnyException();
  }

  @Test
  void testProcessEngineServiceBinding() {
    assertThatCode(() -> InitialContext.doLookup(testConstants.getEngineService()))
      .withFailMessage("Failed to lookup ProcessEngineService '%s'".formatted(TestConstants.PROCESS_ENGINE_SERVICE_JNDI_NAME))
      .doesNotThrowAnyException();
  }

}
