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
package org.operaton.bpm.engine.test.standalone.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.util.CompareUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filip Hrisafov
 */
class CompareUtilTest {

  @Test
  void testDateNotInAnAscendingOrder() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2015, Calendar.MARCH, 15);
    Date first = calendar.getTime();
    calendar.set(2015, Calendar.AUGUST, 15);
    Date second = calendar.getTime();
    Date nullDate = null;
    assertThat(CompareUtil.areNotInAscendingOrder(null, first, null, second)).isFalse();
    assertThat(CompareUtil.areNotInAscendingOrder(null, first, null, first)).isFalse();
    assertThat(CompareUtil.areNotInAscendingOrder(null, second, null, first)).isTrue();
    assertThat(CompareUtil.areNotInAscendingOrder(nullDate, nullDate, nullDate)).isFalse();

    assertThat(CompareUtil.areNotInAscendingOrder(List.of(first, second))).isFalse();
    assertThat(CompareUtil.areNotInAscendingOrder(List.of(first, first))).isFalse();
    assertThat(CompareUtil.areNotInAscendingOrder(List.of(second, first))).isTrue();
  }

  @Test
  void testIsNotContainedIn() {
    String element = "test";
    String [] values = {"test", "test1", "test2"};
    String [] values2 = {"test1", "test2"};
    String [] nullValues = null;
    List<String> nullList = null;

    assertThat(CompareUtil.elementIsNotContainedInArray(element, values)).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInArray(element, values2)).isTrue();
    assertThat(CompareUtil.elementIsNotContainedInArray(null, values)).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInArray(null, nullValues)).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInArray(element, nullValues)).isFalse();

    assertThat(CompareUtil.elementIsNotContainedInList(element, List.of(values))).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInList(element, List.of(values2))).isTrue();
    assertThat(CompareUtil.elementIsNotContainedInList(null, List.of(values))).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInList(null, nullList)).isFalse();
    assertThat(CompareUtil.elementIsNotContainedInList(element, nullList)).isFalse();
  }

  @Test
  void testIsContainedIn() {
    String element = "test";
    String [] values = {"test", "test1", "test2"};
    String [] values2 = {"test1", "test2"};
    String [] nullValues = null;
    List<String> nullList = null;

    assertThat(CompareUtil.elementIsContainedInArray(element, values)).isTrue();
    assertThat(CompareUtil.elementIsContainedInArray(element, values2)).isFalse();
    assertThat(CompareUtil.elementIsContainedInArray(null, values)).isFalse();
    assertThat(CompareUtil.elementIsContainedInArray(null, nullValues)).isFalse();
    assertThat(CompareUtil.elementIsContainedInArray(element, nullValues)).isFalse();

    assertThat(CompareUtil.elementIsContainedInList(element, List.of(values))).isTrue();
    assertThat(CompareUtil.elementIsContainedInList(element, List.of(values2))).isFalse();
    assertThat(CompareUtil.elementIsContainedInList(null, List.of(values))).isFalse();
    assertThat(CompareUtil.elementIsContainedInList(null, nullList)).isFalse();
    assertThat(CompareUtil.elementIsContainedInList(element, nullList)).isFalse();
  }

  @Test
  void testElementsAreContainedInArray() {
    // Positive case: all elements of subset are in superset
    List<String> subset = List.of("a", "b");
    String[] superset = {"a", "b", "c"};
    assertThat(CompareUtil.elementsAreContainedInArray(subset, superset)).isTrue();

    // Negative case: not all elements of subset are in superset
    String[] notSuperset = {"a", "c"};
    assertThat(CompareUtil.elementsAreContainedInArray(subset, notSuperset)).isFalse();

    // Empty array: should return false
    String[] emptyArray = {};
    assertThat(CompareUtil.elementsAreContainedInArray(subset, emptyArray)).isFalse();

    // Empty subset: should return false
    List<String> emptyList = List.of();
    assertThat(CompareUtil.elementsAreContainedInArray(emptyList, superset)).isFalse();

    // Null parameters: should return false
    assertThat(CompareUtil.elementsAreContainedInArray(null, superset)).isFalse();
    assertThat(CompareUtil.elementsAreContainedInArray(subset, null)).isFalse();
  }
}
