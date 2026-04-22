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
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.functional.cdi.beans.DependentScopedBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tassilo Weidner
 */
@ExtendWith(ArquillianExtension.class)
public class CdiBeanDependentScopedAsyncTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(DependentScopedBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/cdi/CdiBeanDependentScoped.testResolveBeanFromJobExecutor.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addClass(DependentScopedBean.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi());

    TestContainer.addContainerSpecificResourcesForNonPaEmbedCdiLib(deployment);

    return deployment;
  }

  @BeforeEach
  @OperateOnDeployment("clientDeployment")
  void setup() {
    DependentScopedBean.reset();

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isZero();

    runtimeService.startProcessInstanceByKey("testResolveBeanFromJobExecutor");

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isOne();

    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("testResolveBeanFromJobExecutor").count()).isZero();
  }

  @Test
  void testResolveBeanFromJobExecutor() {
    assertThat(DependentScopedBean.lifecycle).isEqualTo(List.of("post-construct-invoked", "bean-invoked", "pre-destroy-invoked"));
  }

}
