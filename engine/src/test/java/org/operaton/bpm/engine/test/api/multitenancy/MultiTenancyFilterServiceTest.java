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
package org.operaton.bpm.engine.test.api.multitenancy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenancyFilterServiceTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";
  protected static final String[] TENANT_IDS = new String[] {TENANT_ONE, TENANT_TWO};

  protected String filterId;
  protected final List<String> taskIds = new ArrayList<>();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected IdentityService identityService;
  protected TaskService taskService;
  protected FilterService filterService;

  @BeforeEach
  void setUp() {
    createTaskWithoutTenantId();
    createTaskForTenant(TENANT_ONE);
    createTaskForTenant(TENANT_TWO);
  }

  @Test
  void testCreateFilterWithTenantIdCriteria() {
    TaskQuery query = taskService.createTaskQuery().tenantIdIn(TENANT_IDS);
    filterId = createFilter(query);

    Filter savedFilter = filterService.getFilter(filterId);
    TaskQueryImpl savedQuery = savedFilter.getQuery();

    assertThat(savedQuery.getTenantIds()).isEqualTo(TENANT_IDS);
  }

  @Test
  void testCreateFilterWithNoTenantIdCriteria() {
    TaskQuery query = taskService.createTaskQuery().withoutTenantId();
    filterId = createFilter(query);

    Filter savedFilter = filterService.getFilter(filterId);
    TaskQueryImpl savedQuery = savedFilter.getQuery();

    assertThat(savedQuery.isWithoutTenantId()).isTrue();
    assertThat(savedQuery.getTenantIds()).isNull();
  }

  @Test
  void testFilterTasksNoTenantIdSet() {
    TaskQuery query = taskService.createTaskQuery();
    filterId = createFilter(query);

    assertThat(filterService.count(filterId)).isEqualTo(3L);
  }

  @Test
  void testFilterTasksByTenantIds() {
    TaskQuery query = taskService.createTaskQuery().tenantIdIn(TENANT_IDS);
    filterId = createFilter(query);

    assertThat(filterService.count(filterId)).isEqualTo(2L);

    TaskQuery extendingQuery = taskService.createTaskQuery().taskName("testTask");
    assertThat(filterService.count(filterId, extendingQuery)).isEqualTo(2L);
  }

  @Test
  void testFilterTasksWithoutTenantId() {
    TaskQuery query = taskService.createTaskQuery().withoutTenantId();
    filterId = createFilter(query);

    assertThat(filterService.count(filterId)).isOne();

    TaskQuery extendingQuery = taskService.createTaskQuery().taskName("testTask");
    assertThat(filterService.count(filterId, extendingQuery)).isOne();
  }

  @Test
  void testFilterTasksByExtendingQueryWithTenantId() {
    TaskQuery query = taskService.createTaskQuery().taskName("testTask");
    filterId = createFilter(query);

    TaskQuery extendingQuery = taskService.createTaskQuery().tenantIdIn(TENANT_ONE);
    assertThat(filterService.count(filterId, extendingQuery)).isOne();
  }

  @Test
  void testFilterTasksByExtendingQueryWithoutTenantId() {
    TaskQuery query = taskService.createTaskQuery().taskName("testTask");
    filterId = createFilter(query);

    TaskQuery extendingQuery = taskService.createTaskQuery().withoutTenantId();
    assertThat(filterService.count(filterId, extendingQuery)).isOne();
  }

  @Test
  void testFilterTasksWithNoAuthenticatedTenants() {
    TaskQuery query = taskService.createTaskQuery();
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, null);

    assertThat(filterService.count(filterId)).isOne();
  }

  @Test
  void testFilterTasksWithAuthenticatedTenant() {
    TaskQuery query = taskService.createTaskQuery();
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    assertThat(filterService.count(filterId)).isEqualTo(2L);
  }

  @Test
  void testFilterTasksWithAuthenticatedTenants() {
    TaskQuery query = taskService.createTaskQuery();
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    assertThat(filterService.count(filterId)).isEqualTo(3L);
  }

  @Test
  void testFilterTasksByTenantIdNoAuthenticatedTenants() {
    TaskQuery query = taskService.createTaskQuery().tenantIdIn(TENANT_ONE);
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, null);

    assertThat(filterService.count(filterId)).isZero();
  }

  @Test
  void testFilterTasksByTenantIdWithAuthenticatedTenant() {
    TaskQuery query = taskService.createTaskQuery().tenantIdIn(TENANT_ONE);
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    assertThat(filterService.count(filterId)).isOne();
  }

  @Test
  void testFilterTasksByExtendingQueryWithTenantIdNoAuthenticatedTenants() {
    TaskQuery query = taskService.createTaskQuery().taskName("testTask");
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, null);

    TaskQuery extendingQuery = taskService.createTaskQuery().tenantIdIn(TENANT_ONE);
    assertThat(filterService.count(filterId, extendingQuery)).isZero();
  }

  @Test
  void testFilterTasksByExtendingQueryWithTenantIdAuthenticatedTenant() {
    TaskQuery query = taskService.createTaskQuery().taskName("testTask");
    filterId = createFilter(query);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    TaskQuery extendingQuery = taskService.createTaskQuery().tenantIdIn(TENANT_ONE);
    assertThat(filterService.count(filterId, extendingQuery)).isOne();
  }

  @Test
  void testFilterTasksWithDisabledTenantCheck() {
    TaskQuery query = taskService.createTaskQuery();
    filterId = createFilter(query);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    assertThat(filterService.count(filterId)).isEqualTo(3L);
  }

  protected void createTaskWithoutTenantId() {
    createTaskForTenant(null);
  }

  protected void createTaskForTenant(String tenantId) {
    Task newTask = taskService.newTask();
    newTask.setName("testTask");

    if(tenantId != null) {
      newTask.setTenantId(tenantId);
    }

    taskService.saveTask(newTask);

    taskIds.add(newTask.getId());
  }

  protected String createFilter(TaskQuery query) {
    Filter newFilter = filterService.newTaskFilter("myFilter");
    newFilter.setQuery(query);

    return filterService.saveFilter(newFilter).getId();
  }

  @AfterEach
  void tearDown() {
    filterService.deleteFilter(filterId);
    identityService.clearAuthentication();
    for(String taskId : taskIds) {
      taskService.deleteTask(taskId, true);
    }
  }
}
