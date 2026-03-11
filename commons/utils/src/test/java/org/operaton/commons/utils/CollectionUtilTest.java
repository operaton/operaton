/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.operaton.commons.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionUtilTest {

  @Test
  void singletonMapShouldCreateMapWithOneEntry() {
    Map<String, Object> map = CollectionUtil.singletonMap("key", "value");
    Map.Entry<String, String>  expectedEntry = entry("key", "value");
    assertThat(map).isInstanceOf(HashMap.class).hasSize(1).containsExactly(expectedEntry);
  }

  @Test
  void asArrayListShouldConvertArrayToArrayList() {
    String[] array = { "a", "b", "c" };
    List<String> list = CollectionUtil.asArrayList(array);
    assertThat(list).isInstanceOf(ArrayList.class).hasSize(3).containsExactly("a", "b", "c");
  }

  @Test
  void asArrayListShouldThrowExceptionOnNullArray() {
    assertThatThrownBy(() -> CollectionUtil.asArrayList(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void asHashSetShouldConvertElementsToUniqueSet() {
    Set<String> set = CollectionUtil.asHashSet("a", "b", "a");

    assertThat(set).isInstanceOf(HashSet.class).hasSize(2).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void asHashSetShouldThrowExceptionOnNullVarargs() {
    assertThatThrownBy(() -> CollectionUtil.asHashSet(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void addToMapOfListsShouldCreateNewListForNonExistingKey() {
    Map<String, List<String>> map = new HashMap<>();

    CollectionUtil.addToMapOfLists(map, "key", "value");
    assertThat(map).hasSize(1).containsKey("key");
    assertThat(map.get("key")).hasSize(1).containsExactly("value");
  }

  @Test
  void addToMapOfListsShouldAllowDuplicateValuesInList() {
    Map<String, List<String>> map = new HashMap<>();

    CollectionUtil.addToMapOfLists(map, "key", "value");
    CollectionUtil.addToMapOfLists(map, "key", "value");
    CollectionUtil.addToMapOfLists(map, "key", "value");

    assertThat(map).hasSize(1).containsKey("key");
    assertThat(map.get("key")).hasSize(3).containsExactly("value", "value", "value");
  }

  @Test
  void addToMapOfListsShouldAppendValueToExistingList() {
    Map<String, List<String>> map = new HashMap<>();
    map.put("key", new ArrayList<>(Collections.singletonList("value1")));

    CollectionUtil.addToMapOfLists(map, "key", "value2");

    assertThat(map.get("key")).hasSize(2).containsExactly("value1", "value2");
  }

  @Test
  void addToMapOfListsShouldThrowUnsupportedOperationException() {
    Map<String, List<String>> map = Collections.unmodifiableMap(new HashMap<>());
    assertThatThrownBy(() -> CollectionUtil.addToMapOfLists(map, "key", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void mergeMapsOfListsShouldCombineTwoMapsPreservingValues() {
    Map<String, List<String>> map1 = new HashMap<>();
    Map<String, List<String>> map2 = new HashMap<>();

    CollectionUtil.addToMapOfLists(map2, "key1", "value1");
    CollectionUtil.addToMapOfLists(map2, "key2", "value2");

    CollectionUtil.mergeMapsOfLists(map1, map2);

    assertThat(map1).hasSize(2)
        .containsEntry("key1", Collections.singletonList("value1"))
        .containsEntry("key2", Collections.singletonList("value2"));
  }

  @Test
  void mergeMapsOfListsShouldGenerateExceptions() {
    Map<String, List<String>> map1 = Collections.unmodifiableMap(new HashMap<>());
    Map<String, List<String>> map2 = new HashMap<>();
    CollectionUtil.addToMapOfLists(map2, "key1", "value1");
    CollectionUtil.addToMapOfLists(map2, "key2", "value2");

    assertThatThrownBy(() -> CollectionUtil.mergeMapsOfLists(map1, map2))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> CollectionUtil.mergeMapsOfLists(null, map2))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CollectionUtil.mergeMapsOfLists(map1, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void addToMapOfSetsShouldHandleDuplicatesAndMultipleKeys() {
    Map<String, Set<String>> map = new HashMap<>();

    CollectionUtil.addToMapOfSets(map, "key1", "value1");
    CollectionUtil.addToMapOfSets(map, "key1", "value2");
    CollectionUtil.addToMapOfSets(map, "key2", "value3");

    assertThat(map.get("key1")).hasSize(2).containsExactlyInAnyOrder("value1", "value2");
    assertThat(map.get("key2")).hasSize(1).containsExactly("value3");
  }

  @Test
  void addToMapOfSetsShouldHandelExceptions() {
   Map<String, Set<String>> map = Collections.unmodifiableMap(new HashMap<>());

    assertThatThrownBy(() -> CollectionUtil.addToMapOfSets(map, "key", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> CollectionUtil.addToMapOfSets(null, "key", "value"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void addCollectionToMapOfSetsShouldEliminateDuplicates() {
    Map<String, Set<String>> map = new HashMap<>();

    Collection<String> values = List.of("a", "b", "c", "a");
    CollectionUtil.addCollectionToMapOfSets(map, "key", values);

    assertThat(map).hasSize(1).containsKey("key");
    assertThat(map.get("key")).hasSize(3).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  void addCollectionToMapOfSetsShouldMergeNewValuesWithExistingSet() {
    Map<String, Set<String>> map = new HashMap<>();

    Collection<String> buildValues = List.of("a", "b", "c");
    CollectionUtil.addCollectionToMapOfSets(map, "key", buildValues);
    Collection<String> appendValues = List.of("a", "d", "e");
    CollectionUtil.addCollectionToMapOfSets(map, "key", appendValues);

    assertThat(map).hasSize(1).containsKey("key");
    assertThat(map.get("key")).hasSize(5).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
  }

  @Test
  void addCollectionToMapOfSetsShouldGenerateExceptions() {
    Map<String, Set<String>> map = Collections.unmodifiableMap(new HashMap<>());
    Collection<String> values = List.of("a", "b", "c");

    assertThatThrownBy(() -> CollectionUtil.addCollectionToMapOfSets(map, "key", values))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> CollectionUtil.addCollectionToMapOfSets(null, "key", values))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void partitionShouldSplitListIntoSmallerChunksWithLastPartialChunk() {
    List<String> original = List.of("a", "b", "c", "d", "e");

    List<List<String>> partitioned = CollectionUtil.partition(original, 2);

    assertThat(partitioned).hasSize(3).satisfies(parts -> {
      assertThat(parts.get(0)).containsExactly("a", "b");
      assertThat(parts.get(1)).containsExactly("c", "d");
      assertThat(parts.get(2)).containsExactly("e");
    });
  }

  @Test
  void partitionShouldKeepOriginalListWhenSizeMatchesPartitionSize() {
    List<String> original = List.of("a", "b", "c", "d", "e");

    List<List<String>> partitioned = CollectionUtil.partition(original, 5);

    assertThat(partitioned).hasSize(1).contains(original);
  }

  @Test
  void partitionShouldGenerateException() {
    assertThatThrownBy(() -> CollectionUtil.partition(null, 2))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void collectInListShouldConvertIteratorToArrayList() {
    List<String> original = List.of("a", "b", "c");

    List<String> result = CollectionUtil.collectInList(original.iterator());

    assertThat(result).hasSize(3).containsExactlyElementsOf(original);
  }

  @Test
  void getLastElementShouldReturnLastElementFromListOrSetMember() {
    Set<String> set = new HashSet<>(List.of("a", "b", "c"));
    List<String> list = List.of("a", "b", "c");

    String lastFromSet = CollectionUtil.getLastElement(set);
    String lastFromList = CollectionUtil.getLastElement(list);

    assertThat(lastFromSet).isIn("a", "b", "c");
    assertThat(lastFromList).isEqualTo("c");
  }

  @Test
  void isEmptyShouldReturnTrueForNullAndEmptyCollections() {
    assertThat(CollectionUtil.isEmpty(null)).isTrue();
    assertThat(CollectionUtil.isEmpty(Collections.emptyList())).isTrue();
    assertThat(CollectionUtil.isEmpty(Collections.singletonList("element"))).isFalse();
  }

  @Test
  void hasElementsShouldReturnTrueOnlyForNonEmptyCollections() {
    List<String> list = List.of("a", "b");

    assertThat(CollectionUtil.hasElements(list)).isTrue();
    assertThat(CollectionUtil.hasElements(Collections.emptyList())).isFalse();
    assertThat(CollectionUtil.hasElements(null)).isFalse();
  }

  @Test
  void toSetShouldConvertEnumerationToSetAndRemoveDuplicates() {
    // given
    List<String> list = List.of("a", "b", "a");
    Enumeration<String> enumeration = Collections.enumeration(list);

    // when
    Set<String> set = CollectionUtil.toSet(enumeration);

    // then
    assertThat(set).hasSize(2).containsExactlyInAnyOrder("a", "b");
  }
}

