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
package org.operaton.bpm.integrationtest.functional.classloading.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.functional.classloading.beans.ExampleDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * <p>Deploys an EAR application which contains a WAR process archive, and a client application deployed as a war</p>
 *
 * <p>This test ensures that when the process is started from the client,
 * it is able to make the context switch to the process archive and resolve classes from the
 * process archive.</p>
 *
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class TestJavaDelegateResolution_ClientAsLibInWebModule extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static EnterpriseArchive createProcessArchiveDeplyoment() {
    WebArchive processArchiveWar = initWebArchiveDeployment()
      .addClass(ExampleDelegate.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/JavaDelegateResolutionTest.testResolveClass.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/JavaDelegateResolutionTest.testResolveClassFromJobExecutor.bpmn20.xml");

    return ShrinkWrap.create(EnterpriseArchive.class, "test-app.ear")
      .addAsModule(processArchiveWar);
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(JobExecutorWaitUtils.class)
        .addAsLibraries(DeploymentHelper.getTestingLibs());

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveClass() {
    // assert that we cannot load the delegate here:
    assertThatThrownBy(() -> Class.forName("org.operaton.bpm.integrationtest.functional.classloading.ExampleDelegate")).isInstanceOf(ClassNotFoundException.class);

    // but the process can since it performs context switch to the process archive vor execution
    runtimeService.startProcessInstanceByKey("testResolveClass");
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveClassFromJobExecutor() {

    runtimeService.startProcessInstanceByKey("testResolveClassFromJobExecutor");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

  }

}
