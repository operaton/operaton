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
package org.operaton.bpm.integrationtest.deployment.war;


import static org.assertj.core.api.Assertions.fail;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.integrationtest.deployment.war.apps.CustomServletPA;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;


/**
 * <p>This test makes sure that if we deploy a process application which contains a persistence.xml
 * file which references a non-existing datasource, the MSC does not run into a deadlock.</p>
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestWarDeploymentWithNonExistingDS_JBOSS {

  private static final String DEPLOYMENT_WITH_EJB_PA = "deployment-with-EJB-PA";
  private static final String DEPLOYMENT_WITH_SERVLET_PA = "deployment-with-SERVLET-PA";

  @ArquillianResource
  private Deployer deployer;

  @Deployment(managed=false, name=DEPLOYMENT_WITH_EJB_PA)
  public static WebArchive createDeployment1() {
    WebArchive archive = ShrinkWrap.create(WebArchive.class, "test1.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
        .addAsResource("persistence-nonexisting-ds.xml", "META-INF/persistence.xml");

    TestContainer.addContainerSpecificResources(archive);

    return archive;
  }

  @Deployment(managed=false, name=DEPLOYMENT_WITH_SERVLET_PA)
  public static WebArchive createDeployment2() {
    return ShrinkWrap.create(WebArchive.class, "test2.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
        .addAsResource("persistence-nonexisting-ds.xml", "META-INF/persistence.xml")

        .addClass(CustomServletPA.class);
  }

  @Test
  @RunAsClient
  void testDeploymentFails(){

    try {
      deployer.deploy(DEPLOYMENT_WITH_EJB_PA);
      fail("Deployment exception expected");
    } catch(Exception e) {
      // expected
    }

    try {
      deployer.deploy(DEPLOYMENT_WITH_SERVLET_PA);
      fail("Deployment exception expected");
    } catch(Exception e) {
      // expected
    }

  }

}
