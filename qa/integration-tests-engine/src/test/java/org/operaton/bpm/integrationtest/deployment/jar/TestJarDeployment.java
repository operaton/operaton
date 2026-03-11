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
package org.operaton.bpm.integrationtest.deployment.jar;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.impl.ejb.DefaultEjbProcessApplication;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test only runs on WildFly, as for all other servers, Arquillian wraps the jar in a war file
 * to pack the test runtime. However, we want to deploy a plain jar. This is supported by JBoss-exclusive
 * protocol 'jmx-as7'.
 *
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestJarDeployment extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  @OverProtocol("jmx-as7")
  public static JavaArchive processArchive() {
    var archive = ShrinkWrap.create(JavaArchive.class)
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addClass(DefaultEjbProcessApplication.class)
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
      .addAsResource("org/operaton/bpm/integrationtest/testDeployProcessArchive.bpmn20.xml")
      .addAsManifestResource("org/operaton/bpm/integrationtest/deployment/spring/jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
    for (var lib: DeploymentHelper.getTestingLibs()) {
      archive.merge(lib);
    }
    return archive;
  }

  @Test
  void testDeployProcessArchive() {
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    long count = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("testDeployProcessArchive")
      .count();

    assertThat(count).isOne();
  }

}
