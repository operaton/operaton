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
package org.operaton.bpm.integrationtest.functional.modification;

import org.operaton.bpm.engine.runtime.ProcessInstanceModificationInstantiationBuilder;
import org.operaton.bpm.integrationtest.functional.modification.beans.ExampleDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import org.jboss.arquillian.container.test.api.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class ModificationContextSwitchTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeplyoment() {
    return initWebArchiveDeployment()
            .addClass(ExampleDelegate.class)
            .addAsResource("org/operaton/bpm/integrationtest/functional/modification/ModificationContextSwitchTest.testModificationClassloading.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;

  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void testModificationClassloading() {
    // given
    // process instance is in state "waitState"
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();
    assertThat(runtimeService.getVariable(pi, "executed")).isNull();

    // if
    // we modify the process instance to start the next task:
    ProcessInstanceModificationInstantiationBuilder modification = runtimeService.createProcessInstanceModification(pi)
      .startBeforeActivity("ServiceTask_1");

    // then
    // the modification does not fail
    modification.execute();
    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
  }

}
