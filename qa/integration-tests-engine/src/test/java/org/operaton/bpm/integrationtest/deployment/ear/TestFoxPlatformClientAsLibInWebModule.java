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

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;


/**
 * This test verifies that a process archive packaging the Operaton client
 * can be packaged inside an EAR application.
 *
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestFoxPlatformClientAsLibInWebModule extends AbstractFoxPlatformIntegrationTest {

  /**
   * Deployment layout
   * <pre>
   * test-application.ear
   *    |-- test.war
   *        |-- lib /
   *            |-- fox-platform-client.jar
   *        |-- WEB-INF/classes
   *            |-- META-INF/processes.xml
   *        |-- org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml
   * </pre>
   */
  @Deployment
  public static EnterpriseArchive deployment() {

    // this creates the process archive as a WAR file
    WebArchive processArchive = initWebArchiveDeployment()
      .addAsResource("org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml")
      .addClass(TestFoxPlatformClientAsLibInWebModule.class);

    // this packages the WAR file inside an EAR file
    return ShrinkWrap.create(EnterpriseArchive.class, "test-application.ear")
      .addAsModule(processArchive);

  }

  @Test
  public void testDeployProcessArchive() {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    long count = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("testDeployProcessArchive")
      .count();

    Assertions.assertEquals(1, count);
  }

}
