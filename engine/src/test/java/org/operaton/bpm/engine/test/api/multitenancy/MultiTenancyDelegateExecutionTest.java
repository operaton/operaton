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
package org.operaton.bpm.engine.test.api.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.delegate.AssertingJavaDelegate;
import org.operaton.bpm.engine.test.api.delegate.AssertingJavaDelegate.DelegateExecutionAsserter;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

/**
 * Tests if a {@link DelegateExecution} has the correct tenant-id. The
 * assertions are checked inside the service tasks.
 */
public class MultiTenancyDelegateExecutionTest {

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;

  @Test
  public void testSingleExecution() {
    testRule.deployForTenant("tenant1", Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .serviceTask()
        .operatonClass(AssertingJavaDelegate.class.getName())
      .endEvent()
    .done());

    AssertingJavaDelegate.addAsserts(hasTenantId("tenant1"));

    startProcessInstance(PROCESS_DEFINITION_KEY);
  }

  @Test
  public void testConcurrentExecution() {

    testRule.deployForTenant("tenant1", Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
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

    AssertingJavaDelegate.addAsserts(hasTenantId("tenant1"));

    startProcessInstance(PROCESS_DEFINITION_KEY);
  }

  @Test
  public void testEmbeddedSubprocess() {
    testRule.deployForTenant("tenant1", Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
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

    AssertingJavaDelegate.addAsserts(hasTenantId("tenant1"));

    startProcessInstance(PROCESS_DEFINITION_KEY);
  }

  protected void startProcessInstance(String processDefinitionKey) {
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .latestVersion()
        .singleResult();

    runtimeService.startProcessInstanceById(processDefinition.getId());
  }

  @AfterEach
  public void tearDown() {
    AssertingJavaDelegate.clear();

  }

  protected static DelegateExecutionAsserter hasTenantId(final String expectedTenantId) {
    return execution ->
        assertThat(execution.getTenantId()).isEqualTo(expectedTenantId);
  }

}
