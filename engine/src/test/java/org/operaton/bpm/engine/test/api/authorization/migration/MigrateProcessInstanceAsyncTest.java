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
package org.operaton.bpm.engine.test.api.authorization.migration;
import java.util.List;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
public class MigrateProcessInstanceAsyncTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected Batch batch;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ))
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE)),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE))
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE)),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE))
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE)),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "sourceDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE),
          grant(Resources.PROCESS_DEFINITION, "targetDefinitionKey", "userId", Permissions.MIGRATE_INSTANCE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "*", "userId", Permissions.MIGRATE_INSTANCE))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES),
          grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ),
          grant(Resources.PROCESS_DEFINITION, "*", "userId", Permissions.MIGRATE_INSTANCE))
        .succeeds(),
      scenario()
         .withAuthorizations(
           grant(Resources.PROCESS_INSTANCE, "processInstance", "userId", Permissions.READ))
         .failsDueToRequired(
           grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
           grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_MIGRATE_PROCESS_INSTANCES))
         );
  }

  @BeforeEach
  void setUp() {
    batch = null;
    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    if (batch != null) {
      engineRule.getManagementService().deleteBatch(batch.getId(), true);
    }
    authRule.deleteUsersAndGroups();
  }

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml")
  void testMigrate() {

    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("sourceDefinitionKey", sourceDefinition.getKey())
      .bindResource("targetDefinitionKey", targetDefinition.getKey())
      .bindResource("processInstance", processInstance.getId())
      .start();

    batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceIds(List.of(processInstance.getId()))
      .executeAsync();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(batch.getCreateUserId()).isEqualTo("userId");

      assertThat(engineRule.getManagementService().createBatchQuery().count()).isOne();
    }

  }

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml")
  void testMigrateWithQuery() {

    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("sourceDefinitionKey", sourceDefinition.getKey())
      .bindResource("targetDefinitionKey", targetDefinition.getKey())
      .bindResource("processInstance", processInstance.getId())
      .start();

    batch = engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceQuery(query)
      .executeAsync();

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(engineRule.getManagementService().createBatchQuery().count()).isOne();
    }

  }
}
