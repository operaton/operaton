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
package org.operaton.bpm.integrationtest.functional.spin;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FailingJsonDataFormatConfigurator;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.JsonSerializable;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.spin.spi.DataFormatConfigurator;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@RunWith(Arquillian.class)
public class PaDataFormatConfiguratorFailingTest {

  @ArquillianResource
  private Deployer deployer;

  @Deployment(managed = false, name = "deployment")
  public static WebArchive createDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "PaDataFormatConfiguratorFailingTest.war")
        .addAsResource("META-INF/processes.xml")
        .addAsManifestResource("org/operaton/bpm/integrationtest/deployment/spring/jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(ReferenceStoringProcessApplication.class)
        .addAsResource("org/operaton/bpm/integrationtest/oneTaskProcess.bpmn")
        .addClass(JsonSerializable.class)
        .addClass(FailingJsonDataFormatConfigurator.class)
        .addAsServiceProvider(DataFormatConfigurator.class, FailingJsonDataFormatConfigurator.class);

    TestContainer.addSpinJacksonJsonDataFormat(webArchive);

    return webArchive;

  }

  @Deployment(name = "checkDeployment")
  public static WebArchive createCheckDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class)
        .addAsLibraries(DeploymentHelper.getAssertJ());
    TestContainer.addContainerSpecificResourcesForNonPa(webArchive);
    return webArchive;
  }

  @Before
  public void setUp() {
    try {
      deployer.deploy("deployment");
    } catch (Exception e) {
      // The failing configurator provokes a RuntimeException in a servlet context listener.
      // Apparently such an exception needs not cancel the deployment of a Java EE application.
      // That means deployment fails for some servers and for others not.
      // => we don't care if there is an exception here or not
    }
  }

  @Test
  @OperateOnDeployment("checkDeployment")
  public void testNoProcessApplicationIsDeployed() {
    Set<String> registeredPAs = BpmPlatform.getProcessApplicationService().getProcessApplicationNames();
    assertThat(registeredPAs).isEmpty();
  }
}
