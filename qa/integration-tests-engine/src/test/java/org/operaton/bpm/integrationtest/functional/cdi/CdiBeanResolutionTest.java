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
package org.operaton.bpm.integrationtest.functional.cdi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.cdi.CdiStandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.functional.cdi.beans.ExampleBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * <p>Deploys two different applications, a process archive and a client application.</p>
 *
 * <p>This test ensures that when the process is started from the client,
 * it is able to make the context switch to the process archive and resolve cdi beans
 * from the process archive.</p>
 *
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class CdiBeanResolutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addClass(ExampleBean.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanResolutionTest.testResolveBean.bpmn20.xml")
            .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanResolutionTest.testResolveBeanFromJobExecutor.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addClass(JobExecutorWaitUtils.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi())
            .addAsLibraries(DeploymentHelper.getTestingLibs());

    TestContainer.addContainerSpecificResourcesForNonPaEmbedCdiLib(deployment);

    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveBean() {
    // assert that we cannot resolve the bean here:
    assertThat(ProgrammaticBeanLookup.lookup("exampleBean")).isNull();

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBean").count()).isZero();
    // but the process engine can:
    runtimeService.startProcessInstanceByKey("testResolveBean");

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBean").count()).isZero();
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveBeanFromJobExecutor() {

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isZero();
    runtimeService.startProcessInstanceByKey("testResolveBeanFromJobExecutor");
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isEqualTo(1);

    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isZero();

  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveCdiStandaloneProcessEngineConfigBean() {

    assertThat(ProgrammaticBeanLookup.lookup(CdiStandaloneProcessEngineConfiguration.class)).isNotNull();

  }

}
