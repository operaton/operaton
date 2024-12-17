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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * @author Sebastian Menski
 */
public class FilterPropertiesTest {

  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected FilterService filterService;
  protected Filter filter;
  protected String nestedJsonObject = "{\"id\":\"nested\"}";
  protected String nestedJsonArray = "[\"a\",\"b\"]";

  @Before
  public void setUp() {
    filterService = engineRule.getFilterService();
    filter = filterService.newTaskFilter("name").setOwner("owner").setProperties(new HashMap<>());
  }

  @After
  public void tearDown() {
    for (Filter filter : filterService.createFilterQuery().list()) {
      filterService.deleteFilter(filter.getId());
    }
  }


  @Test
  public void testPropertiesFromNull() {
    filter.setProperties(null);
    assertNull(filter.getProperties());
  }

  @Test
  public void testPropertiesInternalFromNull() {
    // given
    Filter noPropsFilter = filterService.
            newTaskFilter("no props filter")
            .setOwner("demo")
            .setProperties(null);
    filterService.saveFilter(noPropsFilter);

    // when
    FilterEntity noPropsFilterEntity = (FilterEntity) filterService
            .createTaskFilterQuery()
            .filterOwner("demo")
            .singleResult();

    // then
    assertThat(noPropsFilterEntity.getPropertiesInternal()).isEqualTo("{}");
  }

  @Test
  public void testPropertiesFromMap() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("color", "#123456");
    properties.put("priority", 42);
    properties.put("userDefined", true);
    properties.put("object", nestedJsonObject);
    properties.put("array", nestedJsonArray);
    filter.setProperties(properties);

    assertTestProperties();
  }

  @Test
  public void testNullProperty() {
    // given
    Map<String, Object> properties = new HashMap<>();
    properties.put("null", null);
    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    // then
    Map<String, Object> persistentProperties = filter.getProperties();
    assertEquals(1, persistentProperties.size());
    assertTrue(persistentProperties.containsKey("null"));
    assertNull(persistentProperties.get("null"));

  }

  @Test
  public void testMapContainingListProperty() {
    // given
    Map properties = Collections.singletonMap("foo", Collections.singletonList("bar"));

    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    Map deserialisedProperties = filter.getProperties();
    List list = (List) deserialisedProperties.get("foo");
    Object string = list.get(0);

    // then
    assertThat(deserialisedProperties).hasSize(1);
    assertThat(string).isInstanceOf(String.class).hasToString("bar");
  }

  @Test
  public void testMapContainingMapProperty() {
    // given
    Map properties = Collections.singletonMap("foo", Collections.singletonMap("bar", "foo"));

    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    Map deserialisedProperties = filter.getProperties();

    Map map = (Map) deserialisedProperties.get("foo");
    Object string = map.get("bar");

    // then
    assertThat(deserialisedProperties).hasSize(1);
    assertThat(string).hasToString("foo");
  }

  @Test
  public void testMapContainingMapContainingListProperty() {
    // given
    Map properties = Collections.singletonMap("foo", Collections.singletonMap("bar", Collections.singletonList("foo")));

    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    Map deserialisedProperties = filter.getProperties();

    Map map = (Map) deserialisedProperties.get("foo");
    List list = (List) map.get("bar");
    Object string = list.get(0);

    // then
    assertThat(deserialisedProperties).hasSize(1);
    assertThat(string).hasToString("foo");
  }

  @Test
  public void testMapContainingListContainingMapProperty_DeserializePrimitives() {
    // given
    Map<String, Object> primitives = new HashMap<>();
    primitives.put("string", "aStringValue");
    primitives.put("int", 47);
    primitives.put("intOutOfRange", Integer.MAX_VALUE + 1L);
    primitives.put("long", Long.MAX_VALUE);
    primitives.put("double", 3.14159265359D);
    primitives.put("boolean", true);
    primitives.put("null", null);

    Map properties = Collections.singletonMap("foo", Collections.singletonList(primitives));

    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    Map deserialisedProperties = filter.getProperties();

    List list = (List) deserialisedProperties.get("foo");
    Map map = (Map) list.get(0);

    // then
    assertThat(deserialisedProperties).hasSize(1);
    assertThat(map).containsEntry("string", "aStringValue")
            .containsEntry("int", 47)
            .containsEntry("intOutOfRange", Integer.MAX_VALUE + 1L)
            .containsEntry("long", Long.MAX_VALUE)
            .containsEntry("double", 3.14159265359D)
            .containsEntry("boolean", true);
    assertThat(map.get("null")).isNull();
  }

  @Test
  public void testMapContainingMapContainingListProperty_DeserializePrimitives() {
    // given
    List<Object> primitives = new ArrayList<>();
    primitives.add("aStringValue");
    primitives.add(47);
    primitives.add(Integer.MAX_VALUE + 1L);
    primitives.add(Long.MAX_VALUE);
    primitives.add(3.14159265359D);
    primitives.add(true);
    primitives.add(null);

    Map properties = Collections.singletonMap("foo", Collections.singletonMap("bar", primitives));

    filter.setProperties(properties);
    filterService.saveFilter(filter);

    // when
    filter = filterService.getFilter(filter.getId());

    Map deserialisedProperties = filter.getProperties();

    List list = (List) ((Map) deserialisedProperties.get("foo")).get("bar");

    // then
    assertThat(deserialisedProperties).hasSize(1);

    assertThat(list.get(0)).isEqualTo("aStringValue");
    assertThat(list.get(1)).isEqualTo(47);
    assertThat(list.get(2)).isEqualTo(Integer.MAX_VALUE + 1L);
    assertThat(list.get(3)).isEqualTo(Long.MAX_VALUE);
    assertThat(list.get(4)).isEqualTo(3.14159265359D);
    assertThat(list.get(5)).isEqualTo(true);
    assertThat(list.get(6)).isNull();
  }

  protected void assertTestProperties() {
    filterService.saveFilter(filter);
    filter = filterService.getFilter(filter.getId());

    Map<String, Object> properties = filter.getProperties();
    assertEquals(5, properties.size());
    assertEquals("#123456", properties.get("color"));
    assertEquals(42, properties.get("priority"));
    assertEquals(true, properties.get("userDefined"));
    assertEquals(nestedJsonObject, properties.get("object"));
    assertEquals(nestedJsonArray, properties.get("array"));
  }
}
