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

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.delegate.AssertingJavaDelegate;
import org.operaton.bpm.engine.test.api.delegate.AssertingJavaDelegate.DelegateExecutionAsserter;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

class MultiTenancyJobExecutorTest {

  protected static final String TENANT_ID = "tenant1";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Test
  void setAuthenticatedTenantForTimerStartEvent() {
    testRule.deployForTenant(TENANT_ID, Bpmn.createExecutableProcess("process")
        .startEvent()
          .timerWithDuration("PT1M")
        .serviceTask()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .userTask()
        .endEvent()
      .done());

    AssertingJavaDelegate.addAsserts(hasAuthenticatedTenantId(TENANT_ID));

    ClockUtil.setCurrentTime(tomorrow());
    testRule.waitForJobExecutorToProcessAllJobs();

    assertThat(engineRule.getTaskService().createTaskQuery().count()).isEqualTo(1L);
  }

  @Test
  void setAuthenticatedTenantForIntermediateTimerEvent() {
    testRule.deployForTenant(TENANT_ID, Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateCatchEvent()
          .timerWithDuration("PT1M")
        .serviceTask()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .endEvent()
      .done());

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    AssertingJavaDelegate.addAsserts(hasAuthenticatedTenantId(TENANT_ID));

    ClockUtil.setCurrentTime(tomorrow());
    testRule.waitForJobExecutorToProcessAllJobs();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void setAuthenticatedTenantForAsyncJob() {
    testRule.deployForTenant(TENANT_ID, Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonAsyncBefore()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .endEvent()
      .done());

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    AssertingJavaDelegate.addAsserts(hasAuthenticatedTenantId(TENANT_ID));

    testRule.waitForJobExecutorToProcessAllJobs();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void dontSetAuthenticatedTenantForJobWithoutTenant() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonAsyncBefore()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .endEvent()
      .done());

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    AssertingJavaDelegate.addAsserts(hasNoAuthenticatedTenantId());

    testRule.waitForJobExecutorToProcessAllJobs();
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Test
  void dontSetAuthenticatedTenantWhileManualJobExecution() {
    testRule.deployForTenant(TENANT_ID, Bpmn.createExecutableProcess("process")
        .startEvent()
        .serviceTask()
          .operatonAsyncBefore()
          .operatonClass(AssertingJavaDelegate.class.getName())
        .endEvent()
      .done());

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("process");

    AssertingJavaDelegate.addAsserts(hasNoAuthenticatedTenantId());

    testRule.executeAvailableJobs();
    testRule.assertProcessEnded(processInstance.getId());
  }

  protected static DelegateExecutionAsserter hasAuthenticatedTenantId(final String expectedTenantId) {
    return execution -> {
      IdentityService identityService = execution.getProcessEngineServices().getIdentityService();

      Authentication currentAuthentication = identityService.getCurrentAuthentication();
      assertThat(currentAuthentication).isNotNull();
      assertThat(currentAuthentication.getTenantIds()).contains(expectedTenantId);
    };
  }

  protected static DelegateExecutionAsserter hasNoAuthenticatedTenantId() {
    return execution -> {
      IdentityService identityService = execution.getProcessEngineServices().getIdentityService();

      Authentication currentAuthentication = identityService.getCurrentAuthentication();
      assertThat(currentAuthentication).isNull();
    };
  }

  protected Date tomorrow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, 1);

    return calendar.getTime();
  }

  @AfterEach
  void tearDown() {
    AssertingJavaDelegate.clear();
  }

}
