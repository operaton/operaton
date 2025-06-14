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
package org.operaton.bpm.engine.test.api.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Tobias Metzke
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class FilterServiceUserOperationLogTest {

  protected FilterService filterService;
  protected HistoryService historyService;
  protected TaskService taskService;
  protected IdentityService identityService;

  @AfterEach
  void tearDown() {
    // delete all existing filters
    for (Filter filter : filterService.createTaskFilterQuery().list()) {
      filterService.deleteFilter(filter.getId());
    }
  }

  @Test
  void testCreateFilter() {
    // given
    Filter filter = filterService.newTaskFilter()
        .setName("name")
        .setOwner("owner")
        .setQuery(taskService.createTaskQuery())
        .setProperties(new HashMap<>());

    // when
    identityService.setAuthenticatedUserId("userId");
    filterService.saveFilter(filter);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.FILTER);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(logEntry.getProperty()).isEqualTo("filterId");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(filter.getId());
  }

  @Test
  void testUpdateFilter() {
    // given
    Filter filter = filterService.newTaskFilter()
        .setName("name")
        .setOwner("owner")
        .setQuery(taskService.createTaskQuery())
        .setProperties(new HashMap<>());
    filterService.saveFilter(filter);

    // when
    identityService.setAuthenticatedUserId("userId");
    filter.setName(filter.getName() + "_new");
    filterService.saveFilter(filter);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.FILTER);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_UPDATE);
    assertThat(logEntry.getProperty()).isEqualTo("filterId");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(filter.getId());
  }

  @Test
  void testDeleteFilter() {
    // given
    Filter filter = filterService.newTaskFilter()
        .setName("name")
        .setOwner("owner")
        .setQuery(taskService.createTaskQuery())
        .setProperties(new HashMap<>());
    filterService.saveFilter(filter);

    // when
    identityService.setAuthenticatedUserId("userId");
    filterService.deleteFilter(filter.getId());
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.FILTER);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("filterId");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(filter.getId());
  }

}
