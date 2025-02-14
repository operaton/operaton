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
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class MigrationUserOperationLogTest {

  public static final String USER_ID = "userId";

  protected ProcessEngineRule rule = new ProvidedProcessEngineRule();
  protected MigrationTestRule testHelper = new MigrationTestRule(rule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(rule).around(testHelper);

  @Test
  public void testLogCreation() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(
        modify(ProcessModels.ONE_TASK_PROCESS).changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapEqualActivities()
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    // when
    rule.getIdentityService().setAuthenticatedUserId(USER_ID);
    rule.getRuntimeService()
      .newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(processInstance.getId()))
      .execute();
    rule.getIdentityService().clearAuthentication();

    // then
    List<UserOperationLogEntry> opLogEntries = rule.getHistoryService().createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(3);

    Map<String, UserOperationLogEntry> entries = asMap(opLogEntries);

    UserOperationLogEntry procDefEntry = entries.get("processDefinitionId");
    assertThat(procDefEntry).isNotNull();
    assertThat(procDefEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(procDefEntry.getOperationType()).isEqualTo("Migrate");
    assertThat(procDefEntry.getProcessDefinitionId()).isEqualTo(sourceProcessDefinition.getId());
    assertThat(procDefEntry.getProcessDefinitionKey()).isEqualTo(sourceProcessDefinition.getKey());
    assertThat(procDefEntry.getProcessInstanceId()).isNull();
    assertThat(procDefEntry.getOrgValue()).isEqualTo(sourceProcessDefinition.getId());
    assertThat(procDefEntry.getNewValue()).isEqualTo(targetProcessDefinition.getId());
    assertThat(procDefEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry asyncEntry = entries.get("async");
    assertThat(asyncEntry).isNotNull();
    assertThat(asyncEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(asyncEntry.getOperationType()).isEqualTo("Migrate");
    assertThat(asyncEntry.getProcessDefinitionId()).isEqualTo(sourceProcessDefinition.getId());
    assertThat(asyncEntry.getProcessDefinitionKey()).isEqualTo(sourceProcessDefinition.getKey());
    assertThat(asyncEntry.getProcessInstanceId()).isNull();
    assertThat(asyncEntry.getOrgValue()).isNull();
    assertThat(asyncEntry.getNewValue()).isEqualTo("false");
    assertThat(asyncEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    UserOperationLogEntry numInstanceEntry = entries.get("nrOfInstances");
    assertThat(numInstanceEntry).isNotNull();
    assertThat(numInstanceEntry.getEntityType()).isEqualTo("ProcessInstance");
    assertThat(numInstanceEntry.getOperationType()).isEqualTo("Migrate");
    assertThat(numInstanceEntry.getProcessDefinitionId()).isEqualTo(sourceProcessDefinition.getId());
    assertThat(numInstanceEntry.getProcessDefinitionKey()).isEqualTo(sourceProcessDefinition.getKey());
    assertThat(numInstanceEntry.getProcessInstanceId()).isNull();
    assertThat(numInstanceEntry.getOrgValue()).isNull();
    assertThat(numInstanceEntry.getNewValue()).isEqualTo("1");
    assertThat(numInstanceEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(asyncEntry.getOperationId()).isEqualTo(procDefEntry.getOperationId());
    assertThat(numInstanceEntry.getOperationId()).isEqualTo(asyncEntry.getOperationId());
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
