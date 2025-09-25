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
package org.operaton.bpm.engine.spring.test.application;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.spring.application.SpringProcessApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Testcases for {@link SpringProcessApplication}</p>
 *
 * @author Daniel Meyer
 *
 */
class SpringProcessApplicationTest {

  @Test
  void processApplicationDeployment() {

    // initially no applications are deployed:
    assertThat(BpmPlatform.getProcessApplicationService().getProcessApplicationNames()).isEmpty();

    // start a spring application context
    AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("org/operaton/bpm/engine/spring/test/application/SpringProcessApplicationDeploymentTest-context.xml");
    applicationContext.start();

    // assert that there is a process application deployed with the name of the process application bean
    assertThat(BpmPlatform.getProcessApplicationService()
      .getProcessApplicationInfo("myProcessApplication")).isNotNull();

    // close the spring application context
    applicationContext.close();

    // after closing the application context, the process application is undeployed.
    assertThat(BpmPlatform.getProcessApplicationService()
      .getProcessApplicationInfo("myProcessApplication")).isNull();

  }

  @Test
  void deployProcessArchive() {

    // start a spring application context
    AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("org/operaton/bpm/engine/spring/test/application/SpringProcessArchiveDeploymentTest-context.xml");
    applicationContext.start();

    // assert the process archive is deployed:
    ProcessEngine processEngine = BpmPlatform.getDefaultProcessEngine();
    assertThat(processEngine.getRepositoryService().createDeploymentQuery().deploymentName("pa").singleResult()).isNotNull();

    applicationContext.close();

    // assert the process is undeployed
    assertThat(processEngine.getRepositoryService().createDeploymentQuery().deploymentName("pa").singleResult()).isNull();

  }

  @Test
  void postDeployRegistrationPa() {
    // this test verifies that a process application is able to register a deployment from the @PostDeploy callback:

    AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("org/operaton/bpm/engine/spring/test/application/PostDeployRegistrationPaTest-context.xml");
    applicationContext.start();

    ProcessEngine processEngine = BpmPlatform.getDefaultProcessEngine();

    // create a manual deployment:
    Deployment deployment = processEngine.getRepositoryService()
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/spring/test/application/process.bpmn20.xml")
      .deploy();

    // lookup the process application spring bean:
    PostDeployRegistrationPa processApplication = applicationContext.getBean("customProcessApplication",
        PostDeployRegistrationPa.class);

    assertThat(processApplication.isPostDeployInvoked()).isFalse();
    processApplication.deploy();
    assertThat(processApplication.isPostDeployInvoked()).isTrue();

    // the process application was not invoked
    assertThat(processApplication.isInvoked()).isFalse();

    // start process instance:
    processEngine.getRuntimeService()
      .startProcessInstanceByKey("startToEnd");

    // now the process application was invoked:
    assertThat(processApplication.isInvoked()).isTrue();

    // undeploy PA
    assertThat(processApplication.isPreUndeployInvoked()).isFalse();
    processApplication.undeploy();
    assertThat(processApplication.isPreUndeployInvoked()).isTrue();

    // manually undeploy the process
    processEngine.getRepositoryService()
      .deleteDeployment(deployment.getId(), true);

    applicationContext.close();

  }

  /*
   * This test case checks if the process application deployment is done when
   * application context is refreshed, but not when child contexts are
   * refreshed.
   *
   * As a side test it checks if events thrown in the PostDeploy-method are
   * caught by the main application context.
   */
  @Test
  void postDeployWithNestedContext() {
    AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        "org/operaton/bpm/engine/spring/test/application/PostDeployWithNestedContext-context.xml");
    applicationContext.start();

    // lookup the process application spring bean:
    PostDeployWithNestedContext processApplication = applicationContext.getBean("customProcessApplication",
        PostDeployWithNestedContext.class);

    assertThat(processApplication.isDeployOnChildRefresh()).isFalse();
    assertThat(processApplication.isLateEventTriggered()).isTrue();

    processApplication.undeploy();
    applicationContext.close();
  }

}
