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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class UpdateSuspendStateUserOperationLogTest {
  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(rule);
  // do an update here
  protected BatchSuspensionHelper helper = new BatchSuspensionHelper(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testRule);

  protected RuntimeService runtimeService;
  protected HistoryService historyService;

  @Before
  public void initServices() {
    runtimeService = rule.getRuntimeService();
    historyService = rule.getHistoryService();
  }

  @After
  public void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testLogCreation() {


    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    rule.getIdentityService().setAuthenticatedUserId("userId");

    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspendAsync();
    rule.getIdentityService().clearAuthentication();
    helper.completeSeedJobs(suspendprocess);
    helper.executeJobs(suspendprocess);

    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries.size()).isEqualTo(2);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);



    UserOperationLogEntry asyncEntry = entries.get("async");
    assertNotNull(asyncEntry);
    assertThat(asyncEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(asyncEntry.getOperationType()).isEqualTo("SuspendJob");
    assertNull(asyncEntry.getProcessInstanceId());
    assertNull(asyncEntry.getOrgValue());
    assertThat(asyncEntry.getNewValue()).isEqualTo("true");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstancesEntry = entries.get("nrOfInstances");
    assertNotNull(numInstancesEntry);
    assertThat(numInstancesEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(numInstancesEntry.getOperationType()).isEqualTo("SuspendJob");
    assertNull(numInstancesEntry.getProcessInstanceId());
    assertNull(numInstancesEntry.getProcessDefinitionKey());
    assertNull(numInstancesEntry.getProcessDefinitionId());
    assertNull(numInstancesEntry.getOrgValue());
    assertThat(numInstancesEntry.getNewValue()).isEqualTo("2");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(numInstancesEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  public void testNoCreationOnSyncBatchJobExecution() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");


    // when
    Batch suspendprocess = runtimeService.updateProcessInstanceSuspensionState().byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspendAsync();
    helper.completeSeedJobs(suspendprocess);

    // when
    rule.getIdentityService().setAuthenticatedUserId("userId");
    helper.executeJobs(suspendprocess);
    rule.getIdentityService().clearAuthentication();

    // then
    assertThat(rule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.PROCESS_INSTANCE).count()).isEqualTo(0);
  }

  protected Map<String, UserOperationLogEntry> asMap(List<UserOperationLogEntry> logEntries) {
    Map<String, UserOperationLogEntry> map = new HashMap<>();

    for (UserOperationLogEntry entry : logEntries) {

      UserOperationLogEntry previousValue = map.put(entry.getProperty(), entry);
      if (previousValue != null) {
        fail("expected only entry for every property");
      }
    }

    return map;
  }

}
