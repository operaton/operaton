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
package org.operaton.bpm.engine.test.api.authorization.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrateProcessInstanceSyncQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected List<Authorization> authorizations;

  @BeforeEach
  void setUp() {
    authorizations = new ArrayList<>();
    authRule.createUserAndGroup("userId", "groupId");
  }

  @AfterEach
  void tearDown() {
    for (Authorization authorization : authorizations) {
      engineRule.getAuthorizationService().deleteAuthorization(authorization.getId());
    }

    authRule.deleteUsersAndGroups();
  }

  @Test
  void testMigrateWithQuery() {
    // given
    ProcessDefinition sourceDefinition = testHelper.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    ProcessDefinition targetDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "new" + ProcessModels.PROCESS_KEY));

    ProcessInstance instance1 = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    ProcessInstance instance2 = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());

    grantAuthorization("user", Resources.PROCESS_INSTANCE, instance2.getId(), Permissions.READ);
    grantAuthorization("user", Resources.PROCESS_DEFINITION, "*", Permissions.MIGRATE_INSTANCE);

    MigrationPlan migrationPlan = engineRule.getRuntimeService()
        .createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
        .mapEqualActivities()
        .build();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();

    // when
    authRule.enableAuthorization("user");
    engineRule.getRuntimeService().newMigration(migrationPlan)
      .processInstanceQuery(query)
      .execute();

    authRule.disableAuthorization();


    // then
    ProcessInstance instance1AfterMigration = engineRule
      .getRuntimeService()
      .createProcessInstanceQuery()
      .processInstanceId(instance1.getId())
      .singleResult();

    assertThat(instance1AfterMigration.getProcessDefinitionId()).isEqualTo(sourceDefinition.getId());
  }

  protected void grantAuthorization(String userId, Resource resource, String resourceId, Permission permission) {
    Authorization authorization = engineRule.getAuthorizationService().createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
    authorization.setResource(resource);
    authorization.setResourceId(resourceId);
    authorization.addPermission(permission);
    authorization.setUserId(userId);
    engineRule.getAuthorizationService().saveAuthorization(authorization);
    authorizations.add(authorization);
  }
}
