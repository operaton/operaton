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
package org.operaton.bpm.integrationtest.functional.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.context.beans.CalledProcessDelegate;
import org.operaton.bpm.integrationtest.functional.context.beans.DelegateAfter;
import org.operaton.bpm.integrationtest.functional.context.beans.DelegateBefore;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;


/**
 * <p>This test ensures that if a call activity calls a process
 * from a different process archive than the calling process,
 * we perform the appropriate context switch</p>
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class CallActivityContextSwitchTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="mainDeployment")
  public static WebArchive createProcessArchiveDeplyoment() {
    return initWebArchiveDeployment("mainDeployment.war")
      .addClass(DelegateBefore.class)
      .addClass(DelegateAfter.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.mainProcessSync.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.mainProcessSyncNoWait.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.mainProcessASync.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.mainProcessASyncBefore.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.mainProcessASyncAfter.bpmn20.xml");
  }

  @Deployment(name="calledDeployment")
  public static WebArchive createSecondProcessArchiveDeployment() {
    return initWebArchiveDeployment("calledDeployment.war")
      .addClass(CalledProcessDelegate.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.calledProcessSync.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.calledProcessSyncNoWait.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/functional/context/CallActivityContextSwitchTest.calledProcessASync.bpmn20.xml");
  }

  @Inject
  private BeanManager beanManager;

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testNoWaitState() {

    // this test makes sure the delegate invoked by the called process can be resolved (context switch necessary).

    // we cannot load the class
    try {
      new CalledProcessDelegate();
      fail("exception expected");
    }catch (NoClassDefFoundError e) {
      // expected
    }

    // our bean manager does not know this bean
    Set<Bean< ? >> beans = beanManager.getBeans("calledProcessDelegate");
    Assertions.assertEquals(0, beans.size());

    // but when we execute the process, we perform the context switch to the corresponding deployment
    // and there the class can be resolved and the bean is known.
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessSyncNoWait");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessSyncNoWait", processVariables);

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainSyncCalledSync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessSync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessSync", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessSync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncCalledSync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessSync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASync", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    waitForJobExecutorToProcessAllJobs();

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessSync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncBeforeCalledSync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessSync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASyncBefore", processVariables);

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessSync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncAfterCalledSync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessSync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASyncAfter", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessSync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  // the same in main process but called process async

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainSyncCalledASync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessASync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessSync", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessASync")
      .singleResult();

    assertThat(calledPi).isNotNull();
    assertThat(runtimeService.getVariable(calledPi.getId(), "calledDelegate")).isNull();

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncCalledASync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessASync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASync", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    waitForJobExecutorToProcessAllJobs();

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessASync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncBeforeCalledASync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessASync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASyncBefore", processVariables);

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessASync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }

  @Test
  @OperateOnDeployment("mainDeployment")
  public void testMainASyncAfterCalledASync() {

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put("calledElement", "calledProcessASync");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mainProcessASyncAfter", processVariables);

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateBefore.class.getName()));

    waitForJobExecutorToProcessAllJobs();

    ProcessInstance calledPi = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("calledProcessASync")
      .singleResult();
    Assertions.assertEquals(true, runtimeService.getVariable(calledPi.getId(), "calledDelegate"));

    taskService.complete(taskService.createTaskQuery().processInstanceId(calledPi.getId()).singleResult().getId());

    waitForJobExecutorToProcessAllJobs();

    Assertions.assertEquals(true, runtimeService.getVariable(pi.getId(), DelegateAfter.class.getName()));

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(pi.getId()).singleResult()).isNull();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionId(calledPi.getId()).singleResult()).isNull();
  }



}
