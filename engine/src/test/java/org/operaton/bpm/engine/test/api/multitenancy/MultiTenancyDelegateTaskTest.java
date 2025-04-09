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
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.delegate.AssertingTaskListener;
import org.operaton.bpm.engine.test.api.delegate.AssertingTaskListener.DelegateTaskAsserter;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Tests if a {@link DelegateTask} has the correct tenant-id. The
 * assertions are checked inside the task listener.
 */
class MultiTenancyDelegateTaskTest {

  protected static final String BPMN = "org/operaton/bpm/engine/test/api/multitenancy/taskListener.bpmn";

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Test
  void testSingleExecutionWithUserTask() {
    testRule.deployForTenant("tenant1", BPMN);

    AssertingTaskListener.addAsserts(hasTenantId("tenant1"));

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    runtimeService.startProcessInstanceById(processDefinition.getId());
  }

  protected static DelegateTaskAsserter hasTenantId(final String expectedTenantId) {
    return task ->
        assertThat(task.getTenantId()).isEqualTo(expectedTenantId);
  }

  @AfterEach
  void tearDown() {
    AssertingTaskListener.clear();

  }

}
