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
package org.operaton.bpm.dmn.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionResultImpl implements DmnDecisionResult {

  private static final long serialVersionUID = 1L;

  public static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final List<DmnDecisionResultEntries> ruleResults;

  public DmnDecisionResultImpl(List<DmnDecisionResultEntries> ruleResults) {
    this.ruleResults = ruleResults;
  }

    /**
   * Returns the first result in the list of decision result entries. 
   * If the list is empty, returns null.
   * 
   * @return the first result in the list, or null if the list is empty
   */
  public DmnDecisionResultEntries getFirstResult() {
    if (size() > 0) {
      return get(0);
    } else {
      return null;
    }
  }

    /**
   * Retrieves a single decision result entry. If there is only one entry, it is returned.
   * If there are no entries, null is returned. If there are multiple entries, an exception is thrown.
   *
   * @return the single decision result entry
   */
  public DmnDecisionResultEntries getSingleResult() {
    if (size() == 1) {
      return get(0);
    } else if (isEmpty()) {
      return null;
    } else {
      throw LOG.decisionResultHasMoreThanOneOutput(this);
    }
  }

    /**
   * Collects entries with the specified output name from the rule results.
   * 
   * @param outputName the name of the output to collect
   * @return a list of values corresponding to the output name
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> collectEntries(String outputName) {
    List<T> outputValues = new ArrayList<T>();

    for (DmnDecisionResultEntries ruleResult : ruleResults) {
      if (ruleResult.containsKey(outputName)) {
        Object value = ruleResult.get(outputName);
        outputValues.add((T) value);
      }
    }

    return outputValues;
  }

    /**
   * Returns a list of maps containing the decision result entries.
   *
   * @return A list of maps containing the decision result entries
   */
  @Override
  public List<Map<String, Object>> getResultList() {
    List<Map<String, Object>> entryMapList = new ArrayList<Map<String, Object>>();

    for (DmnDecisionResultEntries ruleResult : ruleResults) {
      Map<String, Object> entryMap = ruleResult.getEntryMap();
      entryMapList.add(entryMap);
    }

    return entryMapList;
  }

    /**
   * Returns the single entry of the decision result, or null if the result is empty.
   * 
   * @param <T> the type of the single entry
   * @return the single entry of the decision result, or null if the result is empty
   */
  @Override
  public <T> T getSingleEntry() {
    DmnDecisionResultEntries result = getSingleResult();
    if (result != null) {
      return result.getSingleEntry();
    } else {
      return null;
    }
  }

    /**
   * Returns a single entry from the decision result as a typed value.
   *
   * @param <T> the type of the TypedValue
   * @return a single entry from the decision result as a typed value, or null if the result is null
   */
  @Override
  public <T extends TypedValue> T getSingleEntryTyped() {
    DmnDecisionResultEntries result = getSingleResult();
    if (result != null) {
      return result.getSingleEntryTyped();
    } else {
      return null;
    }
  }

    /**
   * Returns an iterator over the DmnDecisionResultEntries in this object.
   *
   * @return an iterator
   */
  @Override
  public Iterator<DmnDecisionResultEntries> iterator() {
    return asUnmodifiableList().iterator();
  }

    /**
   * Returns the size of the rule results list.
   *
   * @return the size of the rule results list
   */
  @Override
  public int size() {
    return ruleResults.size();
  }

    /**
   * Returns true if the ruleResults list is empty, false otherwise.
   *
   * @return true if the ruleResults list is empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return ruleResults.isEmpty();
  }

    /**
   * Retrieves the DmnDecisionResultEntries at the specified index from the ruleResults list.
   * 
   * @param index the index of the DmnDecisionResultEntries to retrieve
   * @return the DmnDecisionResultEntries at the specified index
   */
  @Override
  public DmnDecisionResultEntries get(int index) {
    return ruleResults.get(index);
  }

    /**
   * Returns true if this RuleResult contains the specified object.
   *
   * @param o the object to check for containment
   * @return true if the object is contained in this RuleResult, false otherwise
   */
  @Override
  public boolean contains(Object o) {
    return ruleResults.contains(o);
  }

    /**
   * Returns an array containing all of the elements in this ruleResults object in proper sequence.
   *
   * @return an array containing all of the elements in this ruleResults object
   */
  @Override
  public Object[] toArray() {
    return ruleResults.toArray();
  }

    /**
   * Copies all of the elements of this list to the specified array.
   * 
   * @param a the array into which the elements of this list are to be stored
   * @return an array containing all of the elements in this list in the same order
   * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of the runtime type of every element in this list
   */
  @Override
  public <T> T[] toArray(T[] a) {
    return ruleResults.toArray(a);
  }

    /**
   * Throws UnsupportedOperationException as decision result is immutable.
   *
   * @param e the decision result entries to be added
   * @return {@code false} as operation is not supported
   */
  @Override
  public boolean add(DmnDecisionResultEntries e) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable.
   *
   * @param o the object to be removed
   * @return false
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Returns true if this ruleResults contains all of the elements in the specified collection.
   *
   * @param c collection to be checked for containment in this ruleResults
   * @return true if this ruleResults contains all of the elements in the specified collection
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    return ruleResults.containsAll(c);
  }

    /**
   * {@inheritDoc}
   * 
   * @param c the collection of decision result entries to be added
   * @return false to indicate that the operation is not supported
   * @throws UnsupportedOperationException always, as decision result is immutable
   */
  @Override
  public boolean addAll(Collection<? extends DmnDecisionResultEntries> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable.
   *
   * @param index the index at which to add the collection
   * @param c the collection of DmnDecisionResultEntries to be added
   * @return false as the operation is not supported
   * @throws UnsupportedOperationException always thrown to indicate that the decision result is immutable
   */
  @Override
  public boolean addAll(int index, Collection<? extends DmnDecisionResultEntries> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable and cannot have elements removed.
   * 
   * @param c the collection of elements to be removed
   * @return {@code false} as no elements are removed
   * @throws UnsupportedOperationException always, as this operation is not supported
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws UnsupportedOperationException.
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable and cannot be cleared.
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable.
   *
   * @param index the index at which the specified element is to be inserted
   * @param element the element to be inserted
   * @return the decision result entries
   * @throws UnsupportedOperationException always thrown to indicate that the decision result is immutable
   */
  @Override
  public DmnDecisionResultEntries set(int index, DmnDecisionResultEntries element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable and cannot have elements added at a specific index.
   */
  @Override
  public void add(int index, DmnDecisionResultEntries element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Removes the element at the specified position in this decision result. 
   * 
   * @param index the index of the element to be removed
   * @return the removed element
   * @throws UnsupportedOperationException if attempted to remove an element as the decision result is immutable
   */
  @Override
  public DmnDecisionResultEntries remove(int index) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Returns the index of the specified object in the ruleResults list.
   *
   * @param o the object to search for in the list
   * @return the index of the specified object in the list, or -1 if the object is not found
   */
  @Override
  public int indexOf(Object o) {
    return ruleResults.indexOf(o);
  }

    /**
   * Returns the index of the last occurrence of the specified element in this list,
   * or -1 if this list does not contain the element.
   *
   * @param o the element to search for
   * @return the index of the last occurrence of the element, or -1 if not found
   */
  @Override
  public int lastIndexOf(Object o) {
    return ruleResults.lastIndexOf(o);
  }

    /**
   * Returns a list iterator over the elements in this list
   */
  @Override
  public ListIterator<DmnDecisionResultEntries> listIterator() {
    return asUnmodifiableList().listIterator();
  }

    /**
   * Returns a list iterator over the elements in this list (in proper sequence),
   * starting at the specified position in the list. The list iterator does not allow
   * modifications to the list, ensuring that the original list remains unaltered.
   *
   * @param index the index to start the list iterator from
   * @return a list iterator over the elements in this list, starting at the specified index
   */
  @Override
  public ListIterator<DmnDecisionResultEntries> listIterator(int index) {
    return asUnmodifiableList().listIterator(index);
  }

    /**
   * Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive.
   *
   * @param fromIndex the index of the first element (inclusive) in the sublist
   * @param toIndex the index of the last element (exclusive) in the sublist
   * @return a view of the specified range within this list
   */
  @Override
  public List<DmnDecisionResultEntries> subList(int fromIndex, int toIndex) {
    return asUnmodifiableList().subList(fromIndex, toIndex);
  }

    /**
   * Returns the string representation of the ruleResults.
   *
   * @return the string representation of the ruleResults
   */
  @Override
  public String toString() {
    return ruleResults.toString();
  }

    /**
   * Returns an unmodifiable list of DmnDecisionResultEntries.
   *
   * @return an unmodifiable list of DmnDecisionResultEntries
   */
  protected List<DmnDecisionResultEntries> asUnmodifiableList() {
    return Collections.unmodifiableList(ruleResults);
  }

}
