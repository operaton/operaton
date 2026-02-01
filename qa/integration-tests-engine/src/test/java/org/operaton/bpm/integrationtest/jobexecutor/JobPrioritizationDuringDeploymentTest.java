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
package org.operaton.bpm.integrationtest.jobexecutor;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.integrationtest.jobexecutor.beans.PriorityBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Requires fix for CAM-3163
 *
 * @author Thorben Lindhauer
 */
@ExtendWith(ArquillianExtension.class)
@Disabled
public class JobPrioritizationDuringDeploymentTest extends AbstractFoxPlatformIntegrationTest {

  @ArquillianResource
  protected Deployer deployer;

  @Override
  public void setupBeforeTest() {
    // don't lookup the default engine since this method is not executed in the deployment
  }

  // deploy this manually
  @Deployment(name="timerStart", managed = false)
  public static WebArchive createTimerStartDeployment() {
    return initWebArchiveDeployment()
      .addClass(PriorityBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationDuringDeploymentTest.timerStart.bpmn20.xml");

  }

  @Test
  @Order(1)
  void testPriorityOnTimerStartEvent() {
    assertThatCode(() -> deployer.deploy("timerStart"))
      .withFailMessage("deployment should be successful, i.e. bean for timer start event should get resolved")
      .doesNotThrowAnyException();
  }

  @Test
  @OperateOnDeployment("timerStart")
  @Order(2)
  void testAssertPriority() {

    // then the timer start event job has the priority resolved from the bean
    Job job = managementService.createJobQuery().activityId("timerStart").singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getPriority()).isEqualTo(PriorityBean.PRIORITY);
  }
}
