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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.filter.FilterQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ProcessEngineExtension.class)
class FilterQueryTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected FilterService filterService;
  protected TaskService taskService;
  protected ManagementService managementService;

  protected List<String> filterIds = new ArrayList<>();

  @BeforeEach
  void setUp() {
    saveFilter("b", "b");
    saveFilter("d", "d");
    saveFilter("a", "a");
    saveFilter("c_", "c");
  }

  protected void saveFilter(String name, String owner) {
    Filter filter = filterService.newTaskFilter()
      .setName(name)
      .setOwner(owner);
    filterService.saveFilter(filter);
    filterIds.add(filter.getId());
  }

  @AfterEach
  void tearDown() {
    // delete all filters
    for (Filter filter : filterService.createFilterQuery().list()) {
      filterService.deleteFilter(filter.getId());
    }
  }

  @Test
  void testQueryNoCriteria() {
    FilterQuery query = filterService.createFilterQuery();
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);
    try {
      query.singleResult();
      fail("Exception expected");
    }
    catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  void testQueryByFilterId() {
    FilterQuery query = filterService.createFilterQuery().filterId(filterIds.get(0));
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidFilterId() {
    FilterQuery query = filterService.createFilterQuery().filterId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    FilterQuery filterQuery = filterService.createFilterQuery();

    assertThatThrownBy(() -> filterQuery.filterId(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByResourceType() {
    FilterQuery query = filterService.createFilterQuery().filterResourceType(EntityTypes.TASK);
    try {
      query.singleResult();
      fail("Exception expected");
    }
    catch (ProcessEngineException e) {
      // expected
    }
    assertThat(query.list()).hasSize(4);
    assertThat(query.count()).isEqualTo(4);
  }

  @Test
  void testQueryByInvalidResourceType() {
    FilterQuery query = filterService.createFilterQuery().filterResourceType("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    FilterQuery filterQuery = filterService.createFilterQuery();

    assertThatThrownBy(() -> filterQuery.filterResourceType(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByName() {
    FilterQuery query = filterService.createFilterQuery().filterName("a");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByNameLike() {
    FilterQuery query = filterService.createFilterQuery().filterNameLike("%\\_");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidName() {
    FilterQuery query = filterService.createFilterQuery().filterName("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    FilterQuery filterQuery = filterService.createFilterQuery();

    assertThatThrownBy(() -> filterQuery.filterName(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByOwner() {
    FilterQuery query = filterService.createFilterQuery().filterOwner("a");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  void testQueryByInvalidOwner() {
    FilterQuery query = filterService.createFilterQuery().filterOwner("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    FilterQuery filterQuery = filterService.createFilterQuery();

    assertThatThrownBy(() -> filterQuery.filterOwner(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryPaging() {
    FilterQuery query = filterService.createFilterQuery();

    assertThat(query.listPage(0, Integer.MAX_VALUE)).hasSize(4);

    // Verifying the un-paged results
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);

    // Verifying paged results
    assertThat(query.listPage(0, 2)).hasSize(2);
    assertThat(query.listPage(2, 2)).hasSize(2);
    assertThat(query.listPage(3, 1)).hasSize(1);

    // Verifying odd usages
    assertThat(query.listPage(-1, -1)).isEmpty();
    assertThat(query.listPage(4, 2)).isEmpty(); // 4 is the last index with a result
    assertThat(query.listPage(0, 15)).hasSize(4); // there are only 4 tasks
  }

  @Test
  void testQuerySorting() {
    List<String> sortedIds = new ArrayList<>(filterIds);
    Collections.sort(sortedIds);
    assertThat(filterService.createFilterQuery().orderByFilterId().asc().list()).hasSize(4);

    assertThat(filterService.createFilterQuery().orderByFilterId().asc().list())
        .extracting("id")
        .containsExactly(sortedIds.get(0), sortedIds.get(1), sortedIds.get(2), sortedIds.get(3));

    assertThat(filterService.createFilterQuery().orderByFilterResourceType().asc().list())
        .hasSize(4)
        .extracting("resourceType")
        .containsExactly(EntityTypes.TASK, EntityTypes.TASK, EntityTypes.TASK, EntityTypes.TASK);

    assertThat(filterService.createFilterQuery().orderByFilterName().asc().list())
        .hasSize(4)
        .extracting("name")
        .containsExactly("a", "b", "c_", "d");

    assertThat(filterService.createFilterQuery().orderByFilterOwner().asc().list())
        .hasSize(4)
        .extracting("owner")
        .containsExactly("a", "b", "c", "d");

    assertThat(filterService.createFilterQuery().orderByFilterId().desc().list())
        .hasSize(4)
        .extracting("id")
        .containsExactly(sortedIds.get(3), sortedIds.get(2), sortedIds.get(1), sortedIds.get(0));

    assertThat(filterService.createFilterQuery().orderByFilterResourceType().desc().list())
        .hasSize(4)
        .extracting("resourceType")
        .containsExactly(EntityTypes.TASK, EntityTypes.TASK, EntityTypes.TASK, EntityTypes.TASK);

    assertThat(filterService.createFilterQuery().orderByFilterName().desc().list())
        .hasSize(4)
        .extracting("name")
        .containsExactly("d", "c_", "b", "a");

    assertThat(filterService.createFilterQuery().orderByFilterOwner().desc().list())
        .hasSize(4)
        .extracting("owner")
        .containsExactly("d", "c", "b", "a");

    assertThat(filterService.createFilterQuery().orderByFilterId().filterName("a").asc().list()).hasSize(1);
    assertThat(filterService.createFilterQuery().orderByFilterId().filterName("a").desc().list()).hasSize(1);
  }

  @Test
  void testNativeQuery() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    assertThat(managementService.getTableName(Filter.class)).isEqualTo(tablePrefix + "ACT_RU_FILTER");
    assertThat(managementService.getTableName(FilterEntity.class)).isEqualTo(tablePrefix + "ACT_RU_FILTER");
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Filter.class)).list()).hasSize(4);
    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Filter.class)).count()).isEqualTo(4);

    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + tablePrefix + "ACT_RU_FILTER F1, " + tablePrefix + "ACT_RU_FILTER F2").count()).isEqualTo(16);

    // select with distinct
    assertThat(taskService.createNativeTaskQuery().sql("SELECT F1.* FROM " + tablePrefix + "ACT_RU_FILTER F1").list()).hasSize(4);

    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Filter.class) + " F WHERE F.NAME_ = 'a'").count()).isEqualTo(1);
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Filter.class) + " F WHERE F.NAME_ = 'a'").list()).hasSize(1);

    // use parameters
    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Filter.class) + " F WHERE F.NAME_ = #{filterName}").parameter("filterName", "a").count()).isEqualTo(1);
  }

  @Test
  void testNativeQueryPaging() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    assertThat(managementService.getTableName(Filter.class)).isEqualTo(tablePrefix + "ACT_RU_FILTER");
    assertThat(managementService.getTableName(FilterEntity.class)).isEqualTo(tablePrefix + "ACT_RU_FILTER");
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Filter.class)).listPage(0, 3)).hasSize(3);
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Filter.class)).listPage(2, 2)).hasSize(2);
  }

}
