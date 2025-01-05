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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Sebastian Menski
 */
public class FilterServiceTest extends PluggableProcessEngineTest {

  protected Filter filter;

  @Before
  public void setUp() {
    filter = filterService.newTaskFilter()
      .setName("name")
      .setOwner("owner")
      .setQuery(taskService.createTaskQuery())
      .setProperties(new HashMap<>());
    assertNull(filter.getId());
    filterService.saveFilter(filter);
    assertNotNull(filter.getId());
  }

  @After
  public void tearDown() {
    // delete all existing filters
    filterService.createTaskFilterQuery().list().forEach(f -> filterService.deleteFilter(f.getId()));
  }

  @Test
  public void testCreateFilter() {
    assertNotNull(filter);

    Filter filter2 = filterService.getFilter(filter.getId());
    assertNotNull(filter2);

    compareFilter(filter, filter2);
  }

  @Test
  public void testCreateInvalidFilter() {
    assertThatThrownBy(() -> filter.setName(null))
      .isInstanceOf(ProcessEngineException.class);// when

    assertThatThrownBy(() -> filter.setName(""))
      .isInstanceOf(ProcessEngineException.class);// when

    assertThatThrownBy(() -> filter.setQuery((Query<?, ?>) null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  public void testUpdateFilter() {
    filter.setName("newName");
    filter.setOwner("newOwner");
    filter.setQuery(taskService.createTaskQuery());
    filter.setProperties(new HashMap<>());

    filterService.saveFilter(filter);

    Filter filter2 = filterService.getFilter(filter.getId());

    compareFilter(filter, filter2);
  }

  @Test
  public void testExtendFilter() {
    TaskQuery extendingQuery = taskService.createTaskQuery()
      .taskName("newName")
      .taskOwner("newOwner");
    Filter newFilter = filter.extend(extendingQuery);
    assertNull(newFilter.getId());

    TaskQueryImpl filterQuery = newFilter.getQuery();
    assertEquals("newName", filterQuery.getName());
    assertEquals("newOwner", filterQuery.getOwner());
  }

  @Test
  public void testQueryFilter() {

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
  public void testQueryUnknownFilter() {
    Filter unknownFilter = filterService.createTaskFilterQuery()
      .filterId("unknown")
      .singleResult();

    assertNull(unknownFilter);

    unknownFilter = filterService.createTaskFilterQuery()
      .filterId(filter.getId())
      .filterName("invalid")
      .singleResult();

    assertNull(unknownFilter);
  }

  @Test
  public void testDeleteFilter() {
    filterService.deleteFilter(filter.getId());

    filter = filterService.getFilter(filter.getId());
    assertNull(filter);
  }

  @Test
  public void testDeleteUnknownFilter() {
    filterService.deleteFilter(filter.getId());
    long count = filterService.createFilterQuery().count();
    assertEquals(0, count);

    String filterId = filter.getId();
    assertThatThrownBy(() -> filterService.deleteFilter(filterId))
      .isInstanceOf(ProcessEngineException.class);
  }

  public static void compareFilter(Filter filter1, Filter filter2) {
    assertNotNull(filter1);
    assertNotNull(filter2);
    assertEquals(filter1.getId(), filter2.getId());
    assertEquals(filter1.getResourceType(), filter2.getResourceType());
    assertEquals(filter1.getName(), filter2.getName());
    assertEquals(filter1.getOwner(), filter2.getOwner());
    assertEquals(((FilterEntity) filter1).getQueryInternal(), ((FilterEntity) filter2).getQueryInternal());
    assertEquals(filter1.getProperties(), filter2.getProperties());
  }

}
