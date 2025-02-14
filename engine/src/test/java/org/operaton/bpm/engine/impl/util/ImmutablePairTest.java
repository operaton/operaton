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
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.fail;

public class ImmutablePairTest {

  @Test
  public void shouldReturnBasicValues() {
    final ImmutablePair<Integer, String> pair = new ImmutablePair<>(0, "foo");
    assertThat(pair.getLeft().intValue()).isEqualTo(0);
    assertThat(pair.getRight()).isEqualTo("foo");
    final ImmutablePair<Object, String> pair2 = new ImmutablePair<>(null, "bar");
    assertThat(pair2.getLeft()).isNull();
    assertThat(pair2.getRight()).isEqualTo("bar");
  }

  @Test
  public void shouldBeCompatibleToMapEntry() {
    final ImmutablePair<Integer, String> pair = new ImmutablePair<>(0, "foo");
    final HashMap<Integer, String> map = new HashMap<>();
    map.put(0, "foo");
    final Entry<Integer, String> entry = map.entrySet().iterator().next();
    assertThat(entry).isEqualTo(pair);
    assertThat(entry.hashCode()).isEqualTo(pair.hashCode());
  }

  @Test
  public void shouldCompareWithLeftFirst() {
    final ImmutablePair<String, String> pair1 = new ImmutablePair<>("A", "D");
    final ImmutablePair<String, String> pair2 = new ImmutablePair<>("B", "C");
    assertThat(pair1.compareTo(pair1)).isEqualTo(0);
    assertThat(pair1.compareTo(pair2) < 0).isTrue();
    assertThat(pair2.compareTo(pair2)).isEqualTo(0);
    assertThat(pair2.compareTo(pair1) > 0).isTrue();
  }

  @Test
  public void shouldCompareWithRightSecond() {
    final ImmutablePair<String, String> pair1 = new ImmutablePair<>("A", "C");
    final ImmutablePair<String, String> pair2 = new ImmutablePair<>("A", "D");
    assertThat(pair1.compareTo(pair1)).isEqualTo(0);
    assertThat(pair1.compareTo(pair2) < 0).isTrue();
    assertThat(pair2.compareTo(pair2)).isEqualTo(0);
    assertThat(pair2.compareTo(pair1) > 0).isTrue();
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
    assertThat(new ImmutablePair<>(null, "foo")).isEqualTo(new ImmutablePair<>(null, "foo"));
    assertThat(new ImmutablePair<>("foo", null)).isNotEqualTo(new ImmutablePair<>("foo", 0));
    assertThat(new ImmutablePair<>("xyz", "bar")).isNotEqualTo(new ImmutablePair<>("foo", "bar"));

    final ImmutablePair<String, String> p = new ImmutablePair<>("foo", "bar");
    assertThat(p).isEqualTo(p);
    assertThat(new Object()).isNotEqualTo(p);
  }

  @Test
  public void shouldHaveSameHashCodeAsEqualObject() {
    assertThat(new ImmutablePair<>(null, "foo").hashCode()).isEqualTo(new ImmutablePair<>(null, "foo").hashCode());
  }
}
