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
package org.operaton.bpm.integrationtest.functional.ejb.local;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.ejb.local.bean.CallbackBean;
import org.operaton.bpm.integrationtest.functional.ejb.local.bean.InvokeStartProcessDelegateSLSB;
import org.operaton.bpm.integrationtest.functional.ejb.local.bean.StartProcessInterface;
import org.operaton.bpm.integrationtest.functional.ejb.local.bean.StartProcessSLSB;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * This Test deploys two processes:
 * - LocalSLSBInvocationTest.testStartProcess.bpmn20.xml  (1)
 * - LocalSLSBInvocationTest.callbackProcess.bpmn20.xml (2)
 *
 * <p>
 * Two applications are deployed:
 * <ul>
 * <li>test.war - Process Application providing Processes (1+2)</li>
 * <li>service.war - application providing a Local SLSB starting Process (2)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Expected Control flow:
 *
 * <pre>
 *    test.war                                 service.war
 *    ========                                 ===========
 *
 * start (unit test)
 *   Process (1)
 *      |
 *      v
 *   InvokeStartProcessDelegateSLSB  ---->    StartProcessSLSB
 *                                               start Process (2)
 *                                                  |
 *                                                  V
 *       CallbackBean         <-----------  Process Engine
 *  </pre>
 * </p>
 *
 *
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class LocalSLSBInvocationWithCallbackTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="pa", order=2)
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(InvokeStartProcessDelegateSLSB.class)
      .addClass(CallbackBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/local/LocalSLSBInvocationTest.testStartProcess.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/ejb/local/LocalSLSBInvocationTest.callbackProcess.bpmn20.xml")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/functional/ejb/local/jboss-deployment-structure.xml","jboss-deployment-structure.xml");
  }

  @Deployment(order=1)
  public static WebArchive delegateDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "service.war")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addClass(StartProcessSLSB.class)
      .addClass(StartProcessInterface.class)
      .addAsManifestResource(new StringAsset("Dependencies: org.operaton.bpm.operaton-engine"), "MANIFEST.MF"); // get access to engine classes

    TestContainer.addContainerSpecificResourcesForNonPa(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("pa")
  void testInvokeBean(){

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testInvokeBean");

    assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo(TRUE);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());
  }

}
