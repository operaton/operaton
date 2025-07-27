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
package org.operaton.commons.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * helper/convience methods for working with collections.
 *
 * @author Joram Barrez
 */
public class CollectionUtil {

  // No need to instantiate
  private CollectionUtil() {
  }

  /**
   * Helper method that creates a singleton map.
   * <p>
   * Alternative for Collections.singletonMap(), since that method returns a
   * generic typed map <K,T> depending on the input type, but we often need a
   * <String, Object> map.
   *
   * @param key   The key for the map.
   * @param value The value associated with the key.
   * @return A map containing a single entry with the specified key and value.
   */
  public static Map<String, Object> singletonMap(String key, Object value) {
    Map<String, Object> map = new HashMap<>();
    map.put(key, value);
    return map;
  }

  /**
   * Arrays.asList cannot be reliably used for SQL parameters on MyBatis < 3.3.0
   *
   *  @throws NullPointerException if the elements is null
   *
   * @param <T>    The type of elements in the array.
   * @param values The array to be converted.
   * @return An ArrayList containing the elements of the array.
   */
  public static <T> List<T> asArrayList(T[] values) {
    ArrayList<T> result = new ArrayList<>();
    Collections.addAll(result, values);

    return result;
  }

  /**
   * Creates a new HashSet containing the specified elements.
   *
   *  @throws NullPointerException if the elements is null
   * @param <T>      The type of elements.
   * @param elements The elements to be added to the set.
   * @return A HashSet containing the provided elements.
   */
  public static <T> Set<T> asHashSet(T... elements) {
    Set<T> set = new HashSet<>();
    Collections.addAll(set, elements);

    return set;
  }

  /**
   * Adds a value to a list associated with the specified key in a map of lists.
   *
   * @throws NullPointerException if map is null
   * @throws UnsupportedOperationException if the map is read-only
   * @param <S>   The type of the key.
   * @param <T>   The type of the value.
   * @param map   The map to which the value will be added.
   * @param key   The key for the map entry.
   * @param value The value to be added to the list.
   */
  public static <S, T> void addToMapOfLists(Map<S, List<T>> map, S key, T value) {
    map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }

  /**
   * Merges two maps of lists. Values from the second map are added to the first map.
   *
   * @throws NullPointerException if either map is null, or if toAdd contains null lists
   * @throws UnsupportedOperationException if the destination map is read-only
   * @param <S>   The type of the key.
   * @param <T>   The type of the value.
   * @param map   The map to which values will be added.
   * @param toAdd The map containing values to be merged.
   */
  public static <S, T> void mergeMapsOfLists(Map<S, List<T>> map, Map<S, List<T>> toAdd) {
    for (Entry<S, List<T>> entry : toAdd.entrySet()) {
      for (T listener : entry.getValue()) {
        CollectionUtil.addToMapOfLists(map, entry.getKey(), listener);
      }
    }
  }

  /**
   * Adds a value to a map of sets. If the key does not exist, a new set is created.
   *
   * @param <S>   The type of the key.
   * @param <T>   The type of the value.
   * @param map   The map to which the value will be added.
   * @param key   The key for the map entry.
   * @param value The value to be added to the set.
   */
  public static <S, T> void addToMapOfSets(Map<S, Set<T>> map, S key, T value) {
    map.computeIfAbsent(key, k -> new HashSet<>()).add(value);
  }

  /**
   * Adds a collection of values to a map of sets. If the key does not exist, a new set is created.
   *
   * @param <S>    The type of the key.
   * @param <T>    The type of the values.
   * @param map    The map to which the values will be added.
   * @param key    The key for the map entry.
   * @param values The collection of values to be added to the set.
   */
  public static <S, T> void addCollectionToMapOfSets(Map<S, Set<T>> map, S key, Collection<T> values) {
    Set<T> set = map.computeIfAbsent(key, k -> new HashSet<>());
    set.addAll(values);
  }

  /**
   * Chops a list into non-view sublists of length partitionSize. Note: the argument list
   * may be included in the result.
   *
   * @param <T>           The type of elements in the list.
   * @param list          The list to be partitioned.
   * @param partitionSize The size of each partition.
   * @return A list of sublists, each of size partitionSize (except possibly the last one).
   */
  public static <T> List<List<T>> partition(List<T> list, final int partitionSize) {
    List<List<T>> parts = new ArrayList<>();

    final int listSize = list.size();

    if (listSize <= partitionSize) {
      // no need for partitioning
      parts.add(list);
    } else {
      for (int i = 0; i < listSize; i += partitionSize) {
        parts.add(new ArrayList<>(list.subList(i, Math.min(listSize, i + partitionSize))));
      }
    }

    return parts;
  }

  /**
   * Converts an iterator into a list.
   *
   * @param <T>      The type of elements in the iterator.
   * @param iterator The iterator to be converted.
   * @return A list containing all elements from the iterator.
   */
  public static <T> List<T> collectInList(Iterator<T> iterator) {
    List<T> result = new ArrayList<>();
    while (iterator.hasNext()) {
      result.add(iterator.next());
    }
    return result;
  }

  /**
   * Retrieves the last element from an iterable. If the iterable is a list, the last element is accessed directly.
   *
   * @param <T>      The type of elements in the iterable.
   * @param elements The iterable from which the last element is retrieved.
   * @return The last element, or null if the iterable is empty.
   */
  public static <T> T getLastElement(final Iterable<T> elements) {
    T lastElement = null;

    if (elements instanceof List) {
      return ((List<T>) elements).get(((List<T>) elements).size() - 1);
    }

    for (T element : elements) {
      lastElement = element;
    }

    return lastElement;
  }

  /**
   * Checks if a collection is null or empty.
   *
   * @param collection The collection to check.
   * @return True if the collection is null or empty, false otherwise.
   */
  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Checks if a collection is not null and contains elements.
   *
   * @param collection The collection to check.
   * @return True if the collection is not null and contains elements, false otherwise.
   */
  public static boolean hasElements(Collection<?> collection) {
    return !isEmpty(collection);
  }
}
