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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestMultipleClasspathRoots extends AbstractFoxPlatformIntegrationTest {

  public static final String PROCESSES_XML =
      "<process-application xmlns=\"http://www.operaton.org/schema/1.0/ProcessApplication\">" +

        "<process-archive name=\"PA_NAME\">" +
          "<properties>" +
            "<property name=\"isDeleteUponUndeploy\">true</property>" +
            "<property name=\"resourceRootPath\">classpath:directory</property>" +
          "</properties>" +
        "</process-archive>" +

      "</process-application>";

  /**
   * <pre>
   *   |-- test.war
   *       |-- WEB-INF
   *           |-- classes
   *               |-- META-INF/processes.xml                   resourceRootPath: classpath:directory
   *               |-- directory/processes/process.bpmn         (1)
   *           |-- lib/
   *               |-- pa0.jar
   *                   |-- directory/processes/process.bpmn     (2)
   * </pre>
   *
   * <p>
   * Processes (1) + (2) will have the same resource name (= "processes/process.bpmn"),
   * so that only one process should be deployed.
   * </p>
   *
   */
  @Deployment
  public static WebArchive processArchive() {

    Asset paProcessesXml = TestHelper.getStringAsAssetWithReplacements(
            PROCESSES_XML,
            new String[][]{new String[]{"PA_NAME","PA0"}});
    assertThat(paProcessesXml).isNotNull();

    Asset[] processAssets = TestHelper.generateProcessAssets(2);

    JavaArchive pa0 = ShrinkWrap.create(JavaArchive.class, "pa0.jar")
        .addAsResource(processAssets[0], "directory/processes/process.bpmn");

    return initWebArchiveDeployment()
        .addAsLibraries(pa0)
        .addAsResource(processAssets[1], "directory/processes/process.bpmn")
        .addClass(TestResourceName.class);
  }

  @Test
  void testMultipleClasspathRoots() {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();

    RepositoryService repositoryService = processEngine.getRepositoryService();

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    long count = query.count();
    assertThat(count).isOne();
  }

}
