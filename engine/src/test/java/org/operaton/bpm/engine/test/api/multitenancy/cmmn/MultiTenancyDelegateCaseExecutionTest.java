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
package org.operaton.bpm.engine.test.api.multitenancy.cmmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.delegate.DelegateCaseExecution;
import org.operaton.bpm.engine.test.api.multitenancy.listener.AssertingCaseExecutionListener;
import org.operaton.bpm.engine.test.api.multitenancy.listener.AssertingCaseExecutionListener.DelegateCaseExecutionAsserter;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * Tests if a {@link DelegateCaseExecution} has the correct tenant-id.
 */
class MultiTenancyDelegateCaseExecutionTest {

  protected static final String HUMAN_TASK_CMMN_FILE = "org/operaton/bpm/engine/test/api/multitenancy/HumanTaskCaseExecutionListener.cmmn";
  protected static final String CASE_TASK_CMMN_FILE = "org/operaton/bpm/engine/test/api/multitenancy/CaseTaskCaseExecutionListener.cmmn";
  protected static final String CMMN_FILE = "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn";

  protected static final String TENANT_ID = "tenant1";

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @AfterEach
  void tearDown() {
    AssertingCaseExecutionListener.clear();
  }

  @Test
  void testSingleExecution() {
    testRule.deployForTenant(TENANT_ID, HUMAN_TASK_CMMN_FILE);

    AssertingCaseExecutionListener.addAsserts(hasTenantId("tenant1"));

    testRule.createCaseInstanceByKey("case");
  }

  @Test
  void testCallCaseTask() {
    testRule.deployForTenant(TENANT_ID, CMMN_FILE);
    testRule.deploy(CASE_TASK_CMMN_FILE);

    AssertingCaseExecutionListener.addAsserts(hasTenantId("tenant1"));

    testRule.createCaseInstanceByKey("oneCaseTaskCase");
  }

  protected static DelegateCaseExecutionAsserter hasTenantId(final String expectedTenantId) {
    return execution -> assertThat(execution.getTenantId()).isEqualTo(expectedTenantId);
  }

}
