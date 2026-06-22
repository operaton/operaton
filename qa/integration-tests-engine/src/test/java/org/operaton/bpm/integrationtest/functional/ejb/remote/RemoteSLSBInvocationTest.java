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
package org.operaton.bpm.integrationtest.functional.ejb.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.functional.ejb.remote.bean.BusinessInterface;
import org.operaton.bpm.integrationtest.functional.ejb.remote.bean.RemoteSLSBClientDelegateBean;
import org.operaton.bpm.integrationtest.functional.ejb.remote.bean.RemoteSLSBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * This test verifies that a CDI Java Bean Delegate is able to inject and invoke the
 * local business interface of a SLSB from a different application
 *
 * <p>
 * Note:
 * - works on Jboss
 * - works on Glassfish
 * </p>
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class RemoteSLSBInvocationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="pa", order=2)
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(RemoteSLSBClientDelegateBean.class)
      .addClass(BusinessInterface.class) // the business interface
      .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/remote/RemoteSLSBInvocationTest.testInvokeBean.bpmn20.xml");
  }

  @Deployment(order=1)
  public static WebArchive delegateDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "service.war")
      .addAsLibraries(DeploymentHelper.getEjbClient())
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addClass(JobExecutorWaitUtils.class)
      .addClass(RemoteSLSBean.class) // the EJB
      .addClass(BusinessInterface.class); // the business interface

    TestContainer.addContainerSpecificResourcesForNonPa(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("pa")
  void testInvokeBean(){

    // this testcase first resolves the Bean synchronously and then from the JobExecutor

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testInvokeBean");

    assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo(TRUE);

    runtimeService.setVariable(pi.getId(), "result", false);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo(TRUE);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());
  }

  @Test
  void testMultipleInvocations() {

    // this is greater than any Datasource / EJB / Thread Pool size -> make sure all resources are released properly.
    int instances = 100;
    String[] ids = new String[instances];

    for(int i=0; i<instances; i++) {
      ids[i] = runtimeService.startProcessInstanceByKey("testInvokeBean").getId();
      assertThat(runtimeService.getVariable(ids[i], "result")).as("Incovation=" + i).isEqualTo(TRUE);
      runtimeService.setVariable(ids[i], "result", false);
      taskService.complete(taskService.createTaskQuery().processInstanceId(ids[i]).singleResult().getId());
    }

    waitForJobExecutorToProcessAllJobs(60*1000);

    for(int i=0; i<instances; i++) {
      assertThat(runtimeService.getVariable(ids[i], "result")).as("Incovation=" + i).isEqualTo(TRUE);
      taskService.complete(taskService.createTaskQuery().processInstanceId(ids[i]).singleResult().getId());
    }

  }


}
