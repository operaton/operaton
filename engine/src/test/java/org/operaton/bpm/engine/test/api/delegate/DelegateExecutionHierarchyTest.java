/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.delegate;

import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the execution hierarchy methods exposed in delegate execution
 *
 * @author Daniel Meyer
 *
 */
public class DelegateExecutionHierarchyTest extends PluggableProcessEngineTest {

  @After
  public void tearDown() {
    AssertingJavaDelegate.clear();

  }

  @Test
  public void testSingleNonScopeActivity() {

   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .serviceTask()
        .operatonClass(AssertingJavaDelegate.class.getName())
      .endEvent()
    .done());

    AssertingJavaDelegate.addAsserts(
        execution -> {
          assertEquals(execution, execution.getProcessInstance());
          assertNull(execution.getSuperExecution());
        }
    );

    runtimeService.startProcessInstanceByKey("testProcess");

  }

  @Test
  public void testConcurrentServiceTasks() {

   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .parallelGateway("fork")
        .serviceTask()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .parallelGateway("join")
        .endEvent()
        .moveToNode("fork")
          .serviceTask()
          .operatonClass(AssertingJavaDelegate.class.getName())
          .connectTo("join")
          .done());

    AssertingJavaDelegate.addAsserts(
        execution -> {
          assertNotEquals(execution, execution.getProcessInstance());
          assertNull(execution.getSuperExecution());
        }
    );

    runtimeService.startProcessInstanceByKey("testProcess");

  }

  @Test
  public void testTaskInsideEmbeddedSubprocess() {
   testRule.deploy(Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .subProcess()
          .embeddedSubProcess()
            .startEvent()
            .serviceTask()
              .operatonClass(AssertingJavaDelegate.class.getName())
            .endEvent()
        .subProcessDone()
        .endEvent()
      .done());

    AssertingJavaDelegate.addAsserts(
        execution -> {
          assertNotEquals(execution, execution.getProcessInstance());
          assertNull(execution.getSuperExecution());
        }
    );

    runtimeService.startProcessInstanceByKey("testProcess");
  }

  @Test
  public void testSubProcessInstance() {

   testRule.deploy(
      Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .callActivity()
          .calledElement("testProcess2")
        .endEvent()
      .done(),
      Bpmn.createExecutableProcess("testProcess2")
        .startEvent()
        .serviceTask()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .endEvent()
      .done());

    AssertingJavaDelegate.addAsserts(
        execution -> {
          assertEquals(execution, execution.getProcessInstance());
          assertNotNull(execution.getSuperExecution());
        }
    );

    runtimeService.startProcessInstanceByKey("testProcess");
  }
}
