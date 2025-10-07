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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Roman Smirnov
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestWarDeploymentDeployChangedOnlyWithJarAsLib extends AbstractFoxPlatformIntegrationTest {

  private static final String PA1 = "PA1";
  private static final String PA2 = "PA2";

  /**
   * <pre>
   *   |-- pa1.war
   *       |-- WEB-INF
   *           |-- lib/
   *               |-- test-v1.jar
   *                   |-- META-INF/processes.xml
   *                   |-- process.bpmn
   * </pre>
   */
  @Deployment(order=1, name=PA1)
  public static WebArchive archive1() {

    JavaArchive processArchiveJar = ShrinkWrap.create(JavaArchive.class, "test-v1.jar")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveUnchanged.bpmn20.xml", "process.bpmn")
      .addAsResource("META-INF/processes.xml");

    WebArchive archive = ShrinkWrap.create(WebArchive.class, "pa1.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())

        .addAsLibraries(processArchiveJar)

        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(TestWarDeploymentDeployChangedOnlyWithJarAsLib.class);

    TestContainer.addContainerSpecificResources(archive);

    return archive;
  }

  /**
   * <pre>
   *   |-- pa2.war
   *       |-- WEB-INF
   *           |-- lib/
   *               |-- test-v2.jar
   *                   |-- META-INF/processes.xml
   *                   |-- process.bpmn
   * </pre>
   */
  @Deployment(order=2, name=PA2)
  public static WebArchive archive2() {

    JavaArchive processArchiveJar = ShrinkWrap.create(JavaArchive.class, "test-v2.jar")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveUnchanged.bpmn20.xml", "process.bpmn")
      .addAsResource("META-INF/processes.xml");

    WebArchive archive = ShrinkWrap.create(WebArchive.class, "pa2.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsLibraries(DeploymentHelper.getTestingLibs())

        .addAsLibraries(processArchiveJar)

        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(TestWarDeploymentDeployChangedOnlyWithJarAsLib.class);

    TestContainer.addContainerSpecificResources(archive);

    return archive;
  }

  @Test
  @OperateOnDeployment(PA2)
  void deployProcessArchive() {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();

    RepositoryService repositoryService = processEngine.getRepositoryService();

    long count = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("testDeployProcessArchiveUnchanged")
      .count();

    assertThat(count).isOne();
  }

}
