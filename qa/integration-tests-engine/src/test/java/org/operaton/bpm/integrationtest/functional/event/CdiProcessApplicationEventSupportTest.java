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
package org.operaton.bpm.integrationtest.functional.event;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.functional.event.beans.CdiEventSupportProcessApplication;
import org.operaton.bpm.integrationtest.functional.event.beans.EventObserverCdiBean;
import org.operaton.bpm.integrationtest.functional.event.beans.ExecutionListenerProcessApplication;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class CdiProcessApplicationEventSupportTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(CdiEventSupportProcessApplication.class)
        .addClass(EventObserverCdiBean.class)
        .addAsResource("org/operaton/bpm/integrationtest/functional/event/ProcessApplicationEventSupportTest.testExecutionListener.bpmn20.xml");

    TestContainer.addContainerSpecificResourcesForNonPa(archive);

    return archive;
  }

  @Test
  public void testEventListener() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Integer listenerInvocationCount = (Integer) runtimeService.getVariable(processInstance.getId(), ExecutionListenerProcessApplication.LISTENER_INVOCATION_COUNT);
    assertThat(listenerInvocationCount).isNotNull();
    assertThat(listenerInvocationCount.intValue()).isEqualTo(6);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setAssignee(task.getId(), "demo");
    listenerInvocationCount = (Integer) runtimeService.getVariable(processInstance.getId(), ExecutionListenerProcessApplication.LISTENER_INVOCATION_COUNT);

    // assignment & update events fired
    assertThat(listenerInvocationCount.intValue()).isEqualTo(8);
  }
}
