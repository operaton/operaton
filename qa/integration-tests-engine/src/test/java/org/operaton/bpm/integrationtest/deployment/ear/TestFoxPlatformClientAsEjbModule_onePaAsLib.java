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
package org.operaton.bpm.integrationtest.deployment.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * This test verifies that a process archive packaging the Operaton client
 * can be packaged inside an EAR application.
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestFoxPlatformClientAsEjbModule_onePaAsLib extends AbstractFoxPlatformIntegrationTest {

  /**
   * Deployment layout
   * <pre>
   * test-application.ear
   *    |-- lib /
   *        |-- processes.jar
   *          |-- META-INF/processes.xml
   *          |-- org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml
   *
   * <p>
   *    |-- fox-platform-client.jar  <<===============================||
   *                                                                  ||  Class-Path reference
   *    |-- test.war (contains the test-class but also processes)     ||
   *        |-- META-INF/MANIFEST.MF =================================||
   *        |-- WEB-INF/beans.xml
   *        |-- + test classes
   * </pre>
   * </p>
   */
  @Deployment
  public static EnterpriseArchive onePaAsLib() {

    JavaArchive processArchiveJar = ShrinkWrap.create(JavaArchive.class, "processes.jar")
      .addAsResource("org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml")
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml");

    JavaArchive foxPlatformClientJar = DeploymentHelper.getEjbClient();

    WebArchive testJar = ShrinkWrap.create(WebArchive.class, "test.war")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .setManifest(new ByteArrayAsset(("Class-Path: " + foxPlatformClientJar.getName()+"\n").getBytes()))
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addClass(TestFoxPlatformClientAsEjbModule_onePaAsLib.class);

    return ShrinkWrap.create(EnterpriseArchive.class, "onePaAsLib.ear")
      .addAsLibrary(processArchiveJar)
      .addAsModule(foxPlatformClientJar)
      .addAsModule(testJar)
      .addAsLibrary(DeploymentHelper.getEngineCdi())
      .addAsLibraries(DeploymentHelper.getTestingLibs());
  }

  @Test
  void testOnePaAsLib() {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    long count = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("testDeployProcessArchive")
      .count();

    assertThat(count).isOne();
  }

}
