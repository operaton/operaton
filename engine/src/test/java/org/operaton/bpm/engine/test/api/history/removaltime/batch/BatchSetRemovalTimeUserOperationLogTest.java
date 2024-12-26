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
package org.operaton.bpm.engine.test.api.history.removaltime.batch;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.history.removaltime.batch.helper.BatchSetRemovalTimeRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_FULL;

/**
 * @author Tassilo Weidner
 */
@RequiredHistoryLevel(HISTORY_FULL)
public class BatchSetRemovalTimeUserOperationLogTest {

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule engineTestRule = new ProcessEngineTestRule(engineRule);
  protected BatchSetRemovalTimeRule testRule = new BatchSetRemovalTimeRule(engineRule, engineTestRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(engineTestRule).around(testRule);

  protected RuntimeService runtimeService;
  protected DecisionService decisionService;
  protected HistoryService historyService;
  protected ManagementService managementService;
  protected IdentityService identityService;

  @Before
  public void assignServices() {
    runtimeService = engineRule.getRuntimeService();
    decisionService = engineRule.getDecisionService();
    historyService = engineRule.getHistoryService();
    managementService = engineRule.getManagementService();
    identityService = engineRule.getIdentityService();
  }

  @After
  public void clearAuth() {
    identityService.clearAuthentication();
  }

  @After
  public void clearDatabase() {
    List<Batch> batches = managementService.createBatchQuery()
      .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
      .list();

    if (!batches.isEmpty()) {
      for (Batch batch : batches) {
        managementService.deleteBatch(batch.getId(), true);
      }
    }

    String batchId = managementService.createBatchQuery().singleResult().getId();
    managementService.deleteBatch(batchId, true);
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .calculatedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery().list();

    // then
    assertProperties(userOperationLogEntries, "mode", "removalTime", "hierarchical", "nrOfInstances", "async", "updateInChunks", "chunkSize");
    assertOperationType(userOperationLogEntries, "SetRemovalTime");
    assertCategory(userOperationLogEntries, "Operator");
    assertEntityType(userOperationLogEntries, "ProcessInstance");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_ModeCalculatedRemovalTime() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .calculatedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("CALCULATED_REMOVAL_TIME");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_ModeAbsoluteRemovalTime() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .absoluteRemovalTime(new Date())
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("ABSOLUTE_REMOVAL_TIME");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_RemovalTime() {
    // given
    Date removalTime = new Date();

    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .absoluteRemovalTime(removalTime)
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(fromMillis(userOperationLogEntry.getNewValue())).isCloseTo(removalTime, 1000);
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_RemovalTimeNull() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isNull();
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_NrOfInstances() {
    // given
    testRule.process().serviceTask().deploy().start();
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("nrOfInstances")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("2");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_AsyncTrue() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("async")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_HierarchicalTrue() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .hierarchical()
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("hierarchical")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_HierarchicalFalse() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("hierarchical")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("false");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_UpdateInChunksTrue() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .updateInChunks()
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("updateInChunks")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_UpdateInChunksFalse() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("updateInChunks")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("false");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_ChunkSize() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .updateInChunks()
      .chunkSize(12)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("chunkSize")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("12");
  }

  @Test
  public void shouldWriteUserOperationLogForProcessInstances_ChunkSizeNull() {
    // given
    testRule.process().serviceTask().deploy().start();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricProcessInstances()
      .clearedRemovalTime()
      .byQuery(historicProcessInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("chunkSize")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isNull();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .calculatedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery().list();

    // then
    assertProperties(userOperationLogEntries, "mode", "removalTime", "hierarchical", "nrOfInstances", "async");
    assertOperationType(userOperationLogEntries, "SetRemovalTime");
    assertCategory(userOperationLogEntries, "Operator");
    assertEntityType(userOperationLogEntries, "DecisionInstance");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_ModeCalculatedRemovalTime() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .calculatedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("CALCULATED_REMOVAL_TIME");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_ModeAbsoluteRemovalTime() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(new Date())
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("ABSOLUTE_REMOVAL_TIME");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_RemovalTime() {
    // given
    Date removalTime = new Date();

    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .absoluteRemovalTime(removalTime)
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(fromMillis(userOperationLogEntry.getNewValue())).isCloseTo(removalTime, 1000);
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_RemovalTimeNull() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .clearedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isNull();
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_NrOfInstances() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .clearedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("nrOfInstances")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("3");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_AsyncTrue() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .clearedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("async")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_HierarchicalTrue() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .clearedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .hierarchical()
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("hierarchical")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml"
  })
  public void shouldWriteUserOperationLogForDecisionInstances_HierarchicalFalse() {
    // given
    evaluate();

    identityService.setAuthenticatedUserId("aUserId");

    HistoricDecisionInstanceQuery historicDecisionInstanceQuery = historyService.createHistoricDecisionInstanceQuery();

    // when
    historyService.setRemovalTimeToHistoricDecisionInstances()
      .clearedRemovalTime()
      .byQuery(historicDecisionInstanceQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("hierarchical")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("false");
  }

  @Test
  public void shouldWriteUserOperationLogForBatches() {
    // given
    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .calculatedRemovalTime()
      .byQuery(historicBatchQuery)
      .executeAsync();

    List<UserOperationLogEntry> userOperationLogEntries = historyService.createUserOperationLogQuery().list();

    // then
    assertProperties(userOperationLogEntries, "mode", "removalTime", "nrOfInstances", "async");
    assertOperationType(userOperationLogEntries, "SetRemovalTime");
    assertEntityType(userOperationLogEntries, "Batch");
    assertCategory(userOperationLogEntries, "Operator");
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_ModeCalculatedRemovalTime() {
    // given
    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .calculatedRemovalTime()
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("CALCULATED_REMOVAL_TIME");
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_ModeAbsoluteRemovalTime() {
    // given
    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .absoluteRemovalTime(new Date())
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("mode")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("ABSOLUTE_REMOVAL_TIME");
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_RemovalTime() {
    // given
    Date removalTime = new Date();

    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .absoluteRemovalTime(removalTime)
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(fromMillis(userOperationLogEntry.getNewValue())).isCloseTo(removalTime, 1000);
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_RemovalTimeNull() {
    // given
    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .clearedRemovalTime()
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("removalTime")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isNull();
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_NrOfInstances() {
    // given
    createBatch(2);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .clearedRemovalTime()
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("nrOfInstances")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("2");
  }

  @Test
  public void shouldWriteUserOperationLogForBatches_AsyncTrue() {
    // given
    createBatch(1);

    identityService.setAuthenticatedUserId("aUserId");

    HistoricBatchQuery historicBatchQuery = historyService.createHistoricBatchQuery();

    // when
    historyService.setRemovalTimeToHistoricBatches()
      .clearedRemovalTime()
      .byQuery(historicBatchQuery)
      .executeAsync();

    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
      .property("async")
      .singleResult();

    // then
    assertThat(userOperationLogEntry.getOrgValue()).isNull();
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected void assertProperties(List<UserOperationLogEntry> userOperationLogEntries, String... expectedProperties) {
    assertThat(userOperationLogEntries).hasSize(expectedProperties.length);

    assertThat(userOperationLogEntries)
      .extracting("property", String.class)
      .containsExactlyInAnyOrder(expectedProperties);
  }

  protected void assertEntityType(List<UserOperationLogEntry> userOperationLogEntries, String entityType) {
    for (UserOperationLogEntry userOperationLogEntry : userOperationLogEntries) {
      assertThat(userOperationLogEntry.getEntityType()).isEqualTo(entityType);
    }
  }

  protected void assertOperationType(List<UserOperationLogEntry> userOperationLogEntries, String operationType) {
    for (UserOperationLogEntry userOperationLogEntry : userOperationLogEntries) {
      assertThat(userOperationLogEntry.getOperationType()).isEqualTo(operationType);
    }
  }

  protected void assertCategory(List<UserOperationLogEntry> userOperationLogEntries, String category) {
    for (UserOperationLogEntry userOperationLogEntry : userOperationLogEntries) {
      assertThat(userOperationLogEntry.getCategory()).isEqualTo(category);
    }
  }

  protected Date fromMillis(String milliseconds) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(Long.parseLong(milliseconds));

    return calendar.getTime();
  }

  protected void evaluate() {
    decisionService.evaluateDecisionByKey("dish-decision")
      .variables(
        Variables.createVariables()
          .putValue("temperature", 32)
          .putValue("dayType", "Weekend")
      ).evaluate();
  }

  protected void createBatch(int times) {
    for (int i = 0; i < times; i++) {
      String processInstanceId = testRule.process().serviceTask().deploy().start();
      historyService.deleteHistoricProcessInstancesAsync(Collections.singletonList(processInstanceId), "aDeleteReason");
    }
  }

}
