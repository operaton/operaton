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
package org.operaton.bpm.integrationtest.functional.spring;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.spring.beans.ErrorDelegate;
import org.operaton.bpm.integrationtest.functional.spring.beans.RetryConfig;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Integration test that makes sure the shared container managed process engine is able to resolve
 * Spring beans form a process application</p>
 *
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class SpringRetryConfigurationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {

    // deploy spring Process Application (does not include ejb-client nor cdi modules)
    return ShrinkWrap.create(WebArchive.class, "test.war")

      // add example bean to serve as JavaDelegate
      .addClass(ErrorDelegate.class)
      .addClass(RetryConfig.class)
      // add process definitions
      .addAsResource("org/operaton/bpm/integrationtest/functional/RetryConfigurationTest.testResolveRetryConfigBean.bpmn20.xml")

      // add custom servlet process application
      .addClass(CustomServletProcessApplication.class)
      // regular deployment descriptor
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")

      // web xml that bootstrapps spring
      .addAsWebInfResource("org/operaton/bpm/integrationtest/functional/spring/web.xml", "web.xml")

      // spring application context & libs
      .addAsWebInfResource("org/operaton/bpm/integrationtest/functional/spring/SpringRetryConfigurationTest-context.xml", "applicationContext.xml")
      .addAsLibraries(DeploymentHelper.getEngineSpring())

      // adding module dependency on process engine module (jboss only)
      .addAsManifestResource("org/operaton/bpm/integrationtest/functional/spring/jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
  }


  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {

    // the test is deployed as a seperate deployment

    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addAsLibraries(DeploymentHelper.getEngineCdi());

    TestContainer.addContainerSpecificResourcesForNonPa(deployment);

    return deployment;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void testResolveRetryConfigBean() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testRetry");

    JobQuery query = managementService
        .createJobQuery()
        .processInstanceId(processInstance.getId());

    Job job = query.singleResult();

    // when job fails
    try {
      managementService.executeJob(job.getId());
    } catch (Exception e) {
      // ignore
    }

    // then
    job = query.singleResult();
    Assert.assertEquals(6, job.getRetries());
  }

}
