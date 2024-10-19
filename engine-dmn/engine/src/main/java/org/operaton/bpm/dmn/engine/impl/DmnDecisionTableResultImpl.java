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

import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionTableResultImpl implements DmnDecisionTableResult {

  private static final long serialVersionUID = 1L;

  public static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;

  protected final List<DmnDecisionRuleResult> ruleResults;

  public DmnDecisionTableResultImpl(List<DmnDecisionRuleResult> ruleResults) {
    this.ruleResults = ruleResults;
  }

    /**
   * Retrieves the first result from the decision rule result list.
   * 
   * @return the first result if the list is not empty, otherwise null
   */
  public DmnDecisionRuleResult getFirstResult() {
    if (size() > 0) {
      return get(0);
    } else {
      return null;
    }
  }

    /**
   * Returns the single decision rule result. If the size is 1, returns the result. If empty, returns null. Otherwise, throws an exception.
   *
   * @return the single decision rule result
   */
  public DmnDecisionRuleResult getSingleResult() {
    if (size() == 1) {
      return get(0);
    } else if (isEmpty()) {
      return null;
    } else {
      throw LOG.decisionResultHasMoreThanOneOutput(this);
    }
  }

    /**
   * Collects entries from rule results based on the given output name.
   * 
   * @param outputName the name of the output to collect entries for
   * @return a list of entries for the given output name
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> collectEntries(String outputName) {
    List<T> outputValues = new ArrayList<T>();

    for (DmnDecisionRuleResult ruleResult : ruleResults) {
      if (ruleResult.containsKey(outputName)) {
        Object value = ruleResult.get(outputName);
        outputValues.add((T) value);
      }
    }

    return outputValues;
  }

    /**
   * Returns a list of maps containing the results of the decision rules.
   * Each map represents the result of a single decision rule.
   *
   * @return the list of maps containing the results
   */
  @Override
  public List<Map<String, Object>> getResultList() {
    List<Map<String, Object>> entryMapList = new ArrayList<Map<String, Object>>();

    for (DmnDecisionRuleResult ruleResult : ruleResults) {
      Map<String, Object> entryMap = ruleResult.getEntryMap();
      entryMapList.add(entryMap);
    }

    return entryMapList;
  }

    /**
   * Retrieves a single entry from the decision rule result.
   * 
   * @param <T> the type of the entry
   * @return the single entry if result is not null, otherwise null
   */
  @Override
  public <T> T getSingleEntry() {
    DmnDecisionRuleResult result = getSingleResult();
    if (result != null) {
      return result.getSingleEntry();
    } else {
      return null;
    }
  }

    /**
   * Retrieves a single typed entry from the decision rule result.
   * 
   * @param <T> the type of the TypedValue
   * @return the single typed entry if result is not null, otherwise return null
   */
  @Override
  public <T extends TypedValue> T getSingleEntryTyped() {
    DmnDecisionRuleResult result = getSingleResult();
    if (result != null) {
      return result.getSingleEntryTyped();
    } else {
      return null;
    }
  }

    /**
   * Returns an iterator over the elements in this DmnDecisionRuleResult list in the same order they are present in the list.
   * The returned iterator does not support the remove operation.
   * 
   * @return an iterator over the elements in this DmnDecisionRuleResult list
   */
  @Override
  public Iterator<DmnDecisionRuleResult> iterator() {
    return asUnmodifiableList().iterator();
  }

    /**
   * Returns the size of the ruleResults list.
   *
   * @return the size of the ruleResults list
   */
  @Override
  public int size() {
    return ruleResults.size();
  }

    /**
   * Checks if the ruleResults list is empty.
   * @return true if the ruleResults list is empty, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return ruleResults.isEmpty();
  }

    /**
   * Returns the decision rule result at the specified index.
   *
   * @param index the index of the decision rule result to retrieve
   * @return the decision rule result at the specified index
   */
  @Override
  public DmnDecisionRuleResult get(int index) {
    return ruleResults.get(index);
  }

    /**
   * Returns true if this object contains the specified element.
   *
   * @param o the element to be checked for containment
   * @return true if this object contains the specified element
   */
  @Override
  public boolean contains(Object o) {
    return ruleResults.contains(o);
  }

    /**
   * Returns an array containing all of the elements in this ruleResults list in proper sequence.
   *
   * @return an array containing all of the elements in this list in proper sequence
   */
  @Override
  public Object[] toArray() {
    return ruleResults.toArray();
  }

    /**
   * Copies all of the elements in this list to the specified array. 
   *
   * @param a the array into which the elements of this list are to be stored
   * @return an array containing all of the elements in this list
   *
   * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of the runtime type of every element in this list
   */
  @Override
  public <T> T[] toArray(T[] a) {
    return ruleResults.toArray(a);
  }

    /**
   * {@inheritDoc}
   * This method throws an UnsupportedOperationException as decision result is immutable.
   */
  @Override
  public boolean add(DmnDecisionRuleResult e) {
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
   * Returns true if all elements in the specified collection are contained in this ruleResults.
   *
   * @param c the collection to be checked for containment in this ruleResults
   * @return true if all elements in the specified collection are contained in this ruleResults
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    return ruleResults.containsAll(c);
  }

    /**
   * Throws an UnsupportedOperationException as decision result is immutable.
   *
   * @param c the collection of decision rule result to be added
   * @return false as no elements can be added
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean addAll(Collection<? extends DmnDecisionRuleResult> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as decision result is immutable.
   *
   * @param index the index at which to add the elements
   * @param c the collection of elements to be added
   * @return false
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean addAll(int index, Collection<? extends DmnDecisionRuleResult> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws an UnsupportedOperationException as the decision result is immutable.
   *
   * @param c the collection to be removed
   * @return false
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Throws UnsupportedOperationException as this operation is not supported.
   *
   * @param c the collection containing the elements to be retained
   * @return UnsupportedOperationException
   * @throws UnsupportedOperationException always
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

    /**
   * Throws an UnsupportedOperationException indicating that the decision result is immutable.
   */
  @Override
  public void clear() {
      throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Replaces the element at the specified position in this list with the specified element.
   * This operation is not supported as decision results are immutable.
   *
   * @param index the index of the element to replace
   * @param element the element to be stored at the specified position
   * @return none
   * @throws UnsupportedOperationException always thrown as decision result is immutable
   */
  @Override
  public DmnDecisionRuleResult set(int index, DmnDecisionRuleResult element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Adds the specified decision rule result at the specified index. 
   * 
   * @param index the index at which the specified element is to be inserted
   * @param element the decision rule result to be added
   * @throws UnsupportedOperationException always thrown as decision result is immutable
   */
  @Override
  public void add(int index, DmnDecisionRuleResult element) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Removes the decision rule result at the specified index. 
   *
   * @param index the index of the decision rule result to be removed
   * @return the removed decision rule result
   * @throws UnsupportedOperationException if attempting to modify an immutable decision result
   */
  @Override
  public DmnDecisionRuleResult remove(int index) {
    throw new UnsupportedOperationException("decision result is immutable");
  }

    /**
   * Returns the index of the first occurrence of the specified element in this list, or -1 if this list does not contain the element.
   *
   * @param o the element to search for
   * @return the index of the specified element in this list, or -1 if this list does not contain the element
   */
  @Override
  public int indexOf(Object o) {
    return ruleResults.indexOf(o);
  }

    /**
   * Returns the index of the last occurrence of the specified element in this list, or -1 if this list does not contain the element.
   *
   * @param o the element to be found in the list
   * @return the index of the last occurrence of the specified element in this list, or -1 if this list does not contain the element
   */
  @Override
  public int lastIndexOf(Object o) {
    return ruleResults.lastIndexOf(o);
  }

    /**
   * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list.
   *
   * @return a list iterator over the elements in this list
   */
  @Override
  public ListIterator<DmnDecisionRuleResult> listIterator() {
    return asUnmodifiableList().listIterator();
  }

    /**
   * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list.
   *
   * @param index the starting index of the list iterator
   * @return a ListIterator of DmnDecisionRuleResult starting at the specified index
   */
  @Override
  public ListIterator<DmnDecisionRuleResult> listIterator(int index) {
    return asUnmodifiableList().listIterator(index);
  }

    /**
   * Returns a view of the portion of this decision rule result list between the specified {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
   *
   * @param fromIndex the index of the first element (inclusive) to be included in the sublist
   * @param toIndex the index of the last element (exclusive) to be included in the sublist
   * @return a view of the specified range within this decision rule result list
   */
  @Override
  public List<DmnDecisionRuleResult> subList(int fromIndex, int toIndex) {
    return asUnmodifiableList().subList(fromIndex, toIndex);
  }

    /**
   * Returns a string representation of the rule results.
   *
   * @return A string representation of the rule results.
   */
  @Override
  public String toString() {
    return ruleResults.toString();
  }

    /**
   * Returns an unmodifiable view of the list of DmnDecisionRuleResult objects.
   *
   * @return an unmodifiable List of DmnDecisionRuleResult objects
   */
  protected List<DmnDecisionRuleResult> asUnmodifiableList() {
    return Collections.unmodifiableList(ruleResults);
  }

    /**
   * Wraps a DmnDecisionResult into a DmnDecisionTableResultImpl by converting each DmnDecisionResultEntries
   * into a DmnDecisionRuleResultImpl and adding them to a list of rule results.
   * 
   * @param decisionResult the DmnDecisionResult to wrap
   * @return a DmnDecisionTableResultImpl containing the wrapped rule results
   */
  public static DmnDecisionTableResultImpl wrap(DmnDecisionResult decisionResult) {
    List<DmnDecisionRuleResult> ruleResults = new ArrayList<DmnDecisionRuleResult>();

    for (DmnDecisionResultEntries result : decisionResult) {
      DmnDecisionRuleResultImpl ruleResult = new DmnDecisionRuleResultImpl();
      ruleResult.putAllValues(result.getEntryMapTyped());

      ruleResults.add(ruleResult);
    }

    return new DmnDecisionTableResultImpl(ruleResults);
  }

}
