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
package org.operaton.bpm.integrationtest.functional.classloading.war;

import static org.assertj.core.api.Assertions.fail;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.functional.classloading.beans.ExampleSignallableActivityBehavior;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

/**
 *
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class SignallableActivityBehaviorResolutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeplyoment() {
    return initWebArchiveDeployment()
            .addClass(ExampleSignallableActivityBehavior.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/SignallableActivityBehaviorResolutionTest.testResolveClass.bpmn20.xml")
            .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/SignallableActivityBehaviorResolutionTest.testResolveClassFromJobExecutor.bpmn20.xml");
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
    try {
      Class.forName("org.operaton.bpm.integrationtest.functional.classloading.beans.ExampleSignallableActivityBehavior");
      fail("CNFE expected");
    }catch (ClassNotFoundException e) {
      // expected
    }

    // but the process can since it performs context switch to the process archive for execution
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testResolveClass");

    runtimeService.signal(processInstance.getId());

  }


  @Test
  @OperateOnDeployment("clientDeployment")
  void testResolveClassFromJobExecutor() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testResolveClassFromJobExecutor");

    Assertions.assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

    runtimeService.signal(processInstance.getId());

    Assertions.assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

  }

}
