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
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestConstants;

import static org.assertj.core.api.Assertions.fail;

@ExtendWith(ArquillianExtension.class)
public class PlatformServicesJndiBindingTest extends AbstractFoxPlatformIntegrationTest {

  protected TestConstants testConstants = new TestConstants();

  @Deployment
  public static WebArchive app1() {
    return initWebArchiveDeployment();
  }

  @Test
  void testProcessApplicationServiceBinding() {
    try {
      InitialContext.doLookup(testConstants.getProcessApplicationService());
    } catch (NamingException e) {
      fail("Failed to lookup ProcessApplicationService '%s'. Reason: %s".formatted(TestConstants.PROCESS_APPLICATION_SERVICE_JNDI_NAME, e));
    }
  }

  @Test
  void testProcessEngineServiceBinding() {
    try {
      InitialContext.doLookup(testConstants.getEngineService());
    } catch (NamingException e) {
      fail("Failed to lookup ProcessEngineService '%s'. Reason: %s".formatted(TestConstants.PROCESS_ENGINE_SERVICE_JNDI_NAME, e));
    }
  }

}
