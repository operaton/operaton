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
package org.operaton.bpm.engine.impl.util;

import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImmutablePairTest {

  @Test
  public void shouldReturnBasicValues() {
    final ImmutablePair<Integer, String> pair = new ImmutablePair<>(0, "foo");
    assertEquals(0, pair.getLeft().intValue());
    assertEquals("foo", pair.getRight());
    final ImmutablePair<Object, String> pair2 = new ImmutablePair<>(null, "bar");
    assertNull(pair2.getLeft());
    assertEquals("bar", pair2.getRight());
  }

  @Test
  public void shouldBeCompatibleToMapEntry() {
    final ImmutablePair<Integer, String> pair = new ImmutablePair<>(0, "foo");
    final HashMap<Integer, String> map = new HashMap<>();
    map.put(0, "foo");
    final Entry<Integer, String> entry = map.entrySet().iterator().next();
    assertEquals(pair, entry);
    assertEquals(pair.hashCode(), entry.hashCode());
  }

  @Test
  public void shouldCompareWithLeftFirst() {
    final ImmutablePair<String, String> pair1 = new ImmutablePair<>("A", "D");
    final ImmutablePair<String, String> pair2 = new ImmutablePair<>("B", "C");
    assertEquals(0, pair1.compareTo(pair1));
    assertTrue(pair1.compareTo(pair2) < 0);
    assertEquals(0, pair2.compareTo(pair2));
    assertTrue(pair2.compareTo(pair1) > 0);
  }

  @Test
  public void shouldCompareWithRightSecond() {
    final ImmutablePair<String, String> pair1 = new ImmutablePair<>("A", "C");
    final ImmutablePair<String, String> pair2 = new ImmutablePair<>("A", "D");
    assertEquals(0, pair1.compareTo(pair1));
    assertTrue(pair1.compareTo(pair2) < 0);
    assertEquals(0, pair2.compareTo(pair2));
    assertTrue(pair2.compareTo(pair1) > 0);
  }

  @Test
  public void shouldFailWithNonComparableTypes() {
    final ImmutablePair<Object, Object> pair1 = new ImmutablePair<>(new Object(), new Object());
    final ImmutablePair<Object, Object> pair2 = new ImmutablePair<>(new Object(), new Object());
    try {
      pair1.compareTo(pair2);
      fail("Pairs should not be comparable");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("Please provide comparable elements");
    }
  }

  @Test
  public void shouldFulfillEqualityRules() {
    assertEquals(new ImmutablePair<>(null, "foo"), new ImmutablePair<>(null, "foo"));
    assertNotEquals(new ImmutablePair<>("foo", 0), new ImmutablePair<>("foo", null));
    assertNotEquals(new ImmutablePair<>("foo", "bar"), new ImmutablePair<>("xyz", "bar"));

    final ImmutablePair<String, String> p = new ImmutablePair<>("foo", "bar");
    assertEquals(p, p);
    assertNotEquals(p, new Object());
  }

  @Test
  public void shouldHaveSameHashCodeAsEqualObject() {
    assertEquals(new ImmutablePair<>(null, "foo").hashCode(), new ImmutablePair<>(null, "foo").hashCode());
  }
}
