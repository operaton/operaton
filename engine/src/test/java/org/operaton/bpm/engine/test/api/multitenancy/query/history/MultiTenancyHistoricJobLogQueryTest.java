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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.historicJobLogByTenantId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;

import java.util.Arrays;
import java.util.List;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class MultiTenancyHistoricJobLogQueryTest {

  protected static final BpmnModelInstance BPMN = Bpmn.createExecutableProcess("failingProcess")
      .startEvent()
      .serviceTask()
        .operatonExpression("${failing}")
        .operatonAsyncBefore()
        .operatonFailedJobRetryTimeCycle("R1/PT1M")
      .endEvent()
      .done();

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  @Before
  public void setUp() {
    historyService = engineRule.getHistoryService();
    runtimeService = engineRule.getRuntimeService();
    identityService = engineRule.getIdentityService();

    // given
    testRule.deployForTenant(TENANT_NULL, BPMN);
    testRule.deployForTenant(TENANT_ONE, BPMN);
    testRule.deployForTenant(TENANT_TWO, BPMN);

    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_NULL);
    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_ONE);
    startProcessInstanceAndExecuteFailingJobForTenant(TENANT_TWO);
  }

  @Test
  public void shouldQueryWithoutTenantId() {
    // when
    HistoricJobLogQuery query = historyService
        .createHistoricJobLogQuery();

    // then
    assertThat(query.count()).isEqualTo(6L);
  }

  @Test
  public void shouldQueryFilterWithoutTenantId() {
    // when
    HistoricJobLogQuery query = historyService
        .createHistoricJobLogQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  public void shouldQueryByTenantId() {
    // when
    HistoricJobLogQuery queryTenantOne = historyService
        .createHistoricJobLogQuery()
        .tenantIdIn(TENANT_ONE);

    HistoricJobLogQuery queryTenantTwo = historyService
        .createHistoricJobLogQuery()
        .tenantIdIn(TENANT_TWO);

    // then
    assertThat(queryTenantOne.count()).isEqualTo(2L);
    assertThat(queryTenantTwo.count()).isEqualTo(2L);
  }

  @Test
  public void shouldQueryByTenantIds() {
    // when
    HistoricJobLogQuery query = historyService
        .createHistoricJobLogQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    // then
    assertThat(query.count()).isEqualTo(4L);
  }

  @Test
  public void shouldQueryByNonExistingTenantId() {
    // when
    HistoricJobLogQuery query = historyService
        .createHistoricJobLogQuery()
        .tenantIdIn("nonExisting");

    // then
    assertThat(query.count()).isZero();
  }

  @Test
  public void shouldFailQueryByTenantIdNull() {
    var historicJobLogQuery = historyService.createHistoricJobLogQuery();
    try {
      // when
      historicJobLogQuery.tenantIdIn((String) null);

      fail("expected exception");

      // then
    } catch (NullValueException e) {
    }
  }

  @Test
  public void shouldQuerySortingAsc() {
    // when
    List<HistoricJobLog> historicJobLogs = historyService.createHistoricJobLogQuery()
        .orderByTenantId()
        .asc()
        .list();

    // then
    assertThat(historicJobLogs).hasSize(6);
    verifySorting(historicJobLogs, historicJobLogByTenantId());
  }

  @Test
  public void shouldQuerySortingDesc() {
    // when
    List<HistoricJobLog> historicJobLogs = historyService.createHistoricJobLogQuery()
        .orderByTenantId()
        .desc()
        .list();

    // then
    assertThat(historicJobLogs).hasSize(6);
    verifySorting(historicJobLogs, inverted(historicJobLogByTenantId()));
  }

  @Test
  public void shouldQueryNoAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, null);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.count()).isEqualTo(2L); // null-tenant entries are still included
  }

  @Test
  public void shouldQueryAuthenticatedTenant() {
    // given
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.count()).isEqualTo(4L);  // null-tenant entries are still included
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.withoutTenantId().count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  public void shouldQueryAuthenticatedTenants() {
    // given
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.count()).isEqualTo(6L); // null-tenant entries are still included
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(2L);
    assertThat(query.withoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  public void shouldQueryDisabledTenantCheck() {
    // given
    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    HistoricJobLogQuery query = historyService.createHistoricJobLogQuery();

    // then
    assertThat(query.count()).isEqualTo(6L);
  }

  protected void startProcessInstanceAndExecuteFailingJobForTenant(String tenant) {
    runtimeService.createProcessInstanceByKey("failingProcess").processDefinitionTenantId(tenant).execute();
    testRule.executeAvailableJobs();
  }

}
