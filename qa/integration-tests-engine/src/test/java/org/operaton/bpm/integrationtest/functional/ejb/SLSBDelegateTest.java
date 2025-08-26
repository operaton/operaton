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
package org.operaton.bpm.integrationtest.functional.ejb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.ejb.beans.SLSBClientDelegate;
import org.operaton.bpm.integrationtest.functional.ejb.beans.SLSBDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;


/**
 * Testcase verifying various ways to use a SLSB as a JavaDelegate
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class SLSBDelegateTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(SLSBDelegate.class)
      .addClass(SLSBClientDelegate.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/SLSBDelegateTest.testBeanResolution.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/SLSBDelegateTest.testBeanResolutionFromClient.bpmn20.xml");
  }


  @Test
  void testBeanResolution() {

    // this testcase first resolves the SLSB synchronously and then from the JobExecutor

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testBeanResolution");

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), SLSBDelegate.class.getName()));

    runtimeService.setVariable(pi.getId(), SLSBDelegate.class.getName(), false);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), SLSBDelegate.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

  }

  @Test
  void testBeanResolutionfromClient() {

    // this testcase invokes a CDI bean that injects the EJB

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testBeanResolutionfromClient");

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), SLSBDelegate.class.getName()));

    runtimeService.setVariable(pi.getId(), SLSBDelegate.class.getName(), false);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), SLSBDelegate.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());
  }

  @Test
  void testMultipleInvocations() {

    // this is greater than any Datasource / EJB / Thread Pool size -> make sure all resources are released properly.
    int instances = 100;
    String[] ids = new String[instances];

    for(int i=0; i<instances; i++) {
      ids[i] = runtimeService.startProcessInstanceByKey("testBeanResolutionfromClient").getId();
      Assertions.assertEquals(true, runtimeService.getVariable(ids[i], SLSBDelegate.class.getName()), "Incovation=" + i);
      runtimeService.setVariable(ids[i], SLSBDelegate.class.getName(), false);
      taskService.complete(taskService.createTaskQuery().processInstanceId(ids[i]).singleResult().getId());
    }

    waitForJobExecutorToProcessAllJobs(60*1000);

    for(int i=0; i<instances; i++) {
      Assertions.assertEquals(true, runtimeService.getVariable(ids[i], SLSBDelegate.class.getName()), "Incovation=" + i);
      taskService.complete(taskService.createTaskQuery().processInstanceId(ids[i]).singleResult().getId());
    }

  }


}
