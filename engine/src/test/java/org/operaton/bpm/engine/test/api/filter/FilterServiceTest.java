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
package org.operaton.bpm.engine.test.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ProcessEngineExtension.class)
public class FilterServiceTest {

  protected FilterService filterService;
  protected TaskService taskService;
  protected Filter filter;

  @BeforeEach
  void setUp() {
    filter = filterService.newTaskFilter()
      .setName("name")
      .setOwner("owner")
      .setQuery(taskService.createTaskQuery())
      .setProperties(new HashMap<>());
    assertThat(filter.getId()).isNull();
    filterService.saveFilter(filter);
    assertThat(filter.getId()).isNotNull();
  }

  @AfterEach
  void tearDown() {
    // delete all existing filters
    filterService.createTaskFilterQuery().list().forEach(f -> filterService.deleteFilter(f.getId()));
  }

  @Test
  void testCreateFilter() {
    assertThat(filter).isNotNull();

    Filter filter2 = filterService.getFilter(filter.getId());
    assertThat(filter2).isNotNull();

    compareFilter(filter, filter2);
  }

  @Test
  void testCreateInvalidFilter() {
    assertThatThrownBy(() -> filter.setName(null))
      .isInstanceOf(ProcessEngineException.class);// when

    assertThatThrownBy(() -> filter.setName(""))
      .isInstanceOf(ProcessEngineException.class);// when

    assertThatThrownBy(() -> filter.setQuery((Query<?, ?>) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testUpdateFilter() {
    filter.setName("newName");
    filter.setOwner("newOwner");
    filter.setQuery(taskService.createTaskQuery());
    filter.setProperties(new HashMap<>());

    filterService.saveFilter(filter);

    Filter filter2 = filterService.getFilter(filter.getId());

    compareFilter(filter, filter2);
  }

  @Test
  void testExtendFilter() {
    TaskQuery extendingQuery = taskService.createTaskQuery()
      .taskName("newName")
      .taskOwner("newOwner");
    Filter newFilter = filter.extend(extendingQuery);
    assertThat(newFilter.getId()).isNull();

    TaskQueryImpl filterQuery = newFilter.getQuery();
    assertThat(filterQuery.getName()).isEqualTo("newName");
    assertThat(filterQuery.getOwner()).isEqualTo("newOwner");
  }

  @Test
  void testQueryFilter() {

    Filter filter2 = filterService.createTaskFilterQuery()
      .filterId(filter.getId())
      .filterName("name")
      .filterOwner("owner")
      .singleResult();

    compareFilter(filter, filter2);

    filter2 = filterService.createTaskFilterQuery()
      .filterNameLike("%m%")
      .singleResult();

    compareFilter(filter, filter2);
  }

  @Test
  void testQueryUnknownFilter() {
    Filter unknownFilter = filterService.createTaskFilterQuery()
      .filterId("unknown")
      .singleResult();

    assertThat(unknownFilter).isNull();

    unknownFilter = filterService.createTaskFilterQuery()
      .filterId(filter.getId())
      .filterName("invalid")
      .singleResult();

    assertThat(unknownFilter).isNull();
  }

  @Test
  void testDeleteFilter() {
    filterService.deleteFilter(filter.getId());

    filter = filterService.getFilter(filter.getId());
    assertThat(filter).isNull();
  }

  @Test
  void testDeleteUnknownFilter() {
    filterService.deleteFilter(filter.getId());
    long count = filterService.createFilterQuery().count();
    assertThat(count).isZero();

    String filterId = filter.getId();
    assertThatThrownBy(() -> filterService.deleteFilter(filterId))
      .isInstanceOf(ProcessEngineException.class);
  }

  public static void compareFilter(Filter filter1, Filter filter2) {
    assertThat(filter1).isNotNull();
    assertThat(filter2).isNotNull();
    assertThat(filter2.getId()).isEqualTo(filter1.getId());
    assertThat(filter2.getResourceType()).isEqualTo(filter1.getResourceType());
    assertThat(filter2.getName()).isEqualTo(filter1.getName());
    assertThat(filter2.getOwner()).isEqualTo(filter1.getOwner());
    assertThat(((FilterEntity) filter2).getQueryInternal()).isEqualTo(((FilterEntity) filter1).getQueryInternal());
    assertThat(filter2.getProperties()).isEqualTo(filter1.getProperties());
  }

}
