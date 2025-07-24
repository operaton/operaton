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
package org.operaton.bpm.integrationtest.deployment.callbacks;

import static org.operaton.bpm.integrationtest.util.TestContainer.addContainerSpecificResourcesForNonPa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.integrationtest.deployment.callbacks.apps.CustomEjbProcessApplication;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class EjbPALifecycleCallbacksTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    var webArchive = ShrinkWrap.create(WebArchive.class, "test.war")
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addClass(CustomEjbProcessApplication.class)
        .addClass(AbstractFoxPlatformIntegrationTest.class);

    addContainerSpecificResourcesForNonPa(webArchive);
    return webArchive;
  }

  @Test
  @SuppressWarnings("java:S2699")
  public void testPaLifecycleCallbacks() {
    // if we get here, everything is all right :)
  }

}
