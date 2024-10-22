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
package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnLogger;
import org.operaton.bpm.dmn.engine.impl.delegate.DmnDecisionTableEvaluationEventImpl;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.BuiltinAggregator;

public abstract class AbstractCollectNumberHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

    /**
   * Returns the BuiltinAggregator used by the class.
   *
   * @return the BuiltinAggregator used
   */
  protected abstract BuiltinAggregator getAggregator();

    /**
   * Applies the result name and value to the provided decision table evaluation event.
   * 
   * @param decisionTableEvaluationEvent the decision table evaluation event to apply the result name and value to
   * @return the updated decision table evaluation event with the result name and value applied
   */
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    String resultName = getResultName(decisionTableEvaluationEvent);
    TypedValue resultValue = getResultValue(decisionTableEvaluationEvent);

    DmnDecisionTableEvaluationEventImpl evaluationEvent = (DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent;
    evaluationEvent.setCollectResultName(resultName);
    evaluationEvent.setCollectResultValue(resultValue);

    return evaluationEvent;
  }

    /**
   * Retrieves the name of the result from the first matching rule's output entry.
   * 
   * @param decisionTableEvaluationEvent the event containing the matching rules
   * @return the name of the result or null if no matching rule has output entries
   */
  protected String getResultName(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    for (DmnEvaluatedDecisionRule matchingRule : decisionTableEvaluationEvent.getMatchingRules()) {
      Map<String, DmnEvaluatedOutput> outputEntries = matchingRule.getOutputEntries();
      if (!outputEntries.isEmpty()) {
        return outputEntries.values().iterator().next().getOutputName();
      }
    }
    return null;
  }

    /**
   * Retrieves a single result value from a decision table evaluation event.
   * 
   * @param decisionTableEvaluationEvent the event containing the decision table evaluation data
   * @return the aggregated result value
   */
  protected TypedValue getResultValue(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<TypedValue> values = collectSingleValues(decisionTableEvaluationEvent);
    return aggregateValues(values);
  }

    /**
   * Collects single values from the evaluated decision table rules.
   *
   * @param decisionTableEvaluationEvent the event containing the evaluated decision table
   * @return a list of TypedValues containing the single output values
   */
  protected List<TypedValue> collectSingleValues(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<TypedValue> values = new ArrayList<TypedValue>();
    for (DmnEvaluatedDecisionRule matchingRule : decisionTableEvaluationEvent.getMatchingRules()) {
      Map<String, DmnEvaluatedOutput> outputEntries = matchingRule.getOutputEntries();
      if (outputEntries.size() > 1) {
        throw LOG.aggregationNotApplicableOnCompoundOutput(getAggregator(), outputEntries);
      }
      else if (outputEntries.size() == 1) {
        TypedValue typedValue = outputEntries.values().iterator().next().getValue();
        values.add(typedValue);
      }
      // ignore empty output entries
    }
    return values;
  }

    /**
   * Aggregates a list of TypedValue objects by calling aggregateNumberValues if the list is not empty.
   * 
   * @param values a list of TypedValue objects to aggregate
   * @return the aggregated TypedValue object or null if the list is empty
   */
  protected TypedValue aggregateValues(List<TypedValue> values) {
    if (!values.isEmpty()) {
      return aggregateNumberValues(values);
    }
    else {
      // return null if no values to aggregate
      return null;
    }

  }

    /**
   * Aggregates a list of TypedValues containing numbers into a single TypedValue.
   * This method attempts to convert the values to integers, longs, and doubles in that order,
   * and aggregates them accordingly. If conversion fails for all types, an exception is thrown.
   *
   * @param values the list of TypedValues to aggregate
   * @return the aggregated TypedValue
   */
  protected TypedValue aggregateNumberValues(List<TypedValue> values) {
    try {
      List<Integer> intValues = convertValuesToInteger(values);
      return Variables.integerValue(aggregateIntegerValues(intValues));
    }
    catch (IllegalArgumentException e) {
      // ignore
    }

    try {
      List<Long> longValues = convertValuesToLong(values);
      return Variables.longValue(aggregateLongValues(longValues));
    }
    catch (IllegalArgumentException e) {
      // ignore
    }

    try {
      List<Double> doubleValues = convertValuesToDouble(values);
      return Variables.doubleValue(aggregateDoubleValues(doubleValues));
    }
    catch (IllegalArgumentException e) {
      // ignore
    }

    throw LOG.unableToConvertValuesToAggregatableTypes(values, Integer.class, Long.class, Double.class);
  }

    /**
   * This method aggregates a list of Integer values in a specific way.
   * The implementation of this method will vary depending on the subclass.
   *
   * @param intValues the list of Integer values to be aggregated
   * @return the result of aggregating the Integer values
   */
  protected abstract Integer aggregateIntegerValues(List<Integer> intValues);

    /**
   * This method aggregates a list of Long values into a single Long value.
   *
   * @param longValues the list of Long values to aggregate
   * @return the aggregated Long value
   */
  protected abstract Long aggregateLongValues(List<Long> longValues);

    /**
   * This method takes a list of Double values and aggregates them in some way to return a Double result.
   *
   * @param doubleValues the list of Double values to be aggregated
   * @return the aggregated Double value
   */
  protected abstract Double aggregateDoubleValues(List<Double> doubleValues);

    /**
   * Converts a list of TypedValues to a list of Integers. Checks if the TypedValues are of type INTEGER
   * or contain Integer values, otherwise throws an IllegalArgumentException.
   * 
   * @param typedValues the list of TypedValues to convert
   * @return a list of Integers extracted from the TypedValues
   * @throws IllegalArgumentException if the TypedValues do not contain valid Integer values
   */
  protected List<Integer> convertValuesToInteger(List<TypedValue> typedValues) throws IllegalArgumentException {
    List<Integer> intValues = new ArrayList<Integer>();
    for (TypedValue typedValue : typedValues) {

      if (ValueType.INTEGER.equals(typedValue.getType())) {
        intValues.add((Integer) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is an integer

        Object value = typedValue.getValue();
        if (value instanceof Integer) {
          intValues.add((Integer) value);

        } else {
          throw new IllegalArgumentException();
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }

    }
    return intValues;
  }

    /**
   * Converts a list of TypedValues to a list of Long values. If a TypedValue is of type LONG, adds its value to the resulting list. 
   * If the type is null, checks if it is a Long or a String representing a number and adds it accordingly. 
   * Throws an IllegalArgumentException for any other TypedValue types.
   * 
   * @param typedValues the list of TypedValues to convert
   * @return a list of Long values extracted from the TypedValues
   * @throws IllegalArgumentException if a TypedValue is not of type LONG or null
   */
  protected List<Long> convertValuesToLong(List<TypedValue> typedValues) throws IllegalArgumentException {
    List<Long> longValues = new ArrayList<Long>();
    for (TypedValue typedValue : typedValues) {

      if (ValueType.LONG.equals(typedValue.getType())) {
        longValues.add((Long) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is a long or a string of a number

        Object value = typedValue.getValue();
        if (value instanceof Long) {
          longValues.add((Long) value);

        } else {
          Long longValue = Long.valueOf(value.toString());
          longValues.add(longValue);
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }

    }
    return longValues;
  }


    /**
   * Converts a list of TypedValue objects to a list of Double values.
   * If the TypedValue is of type DOUBLE, directly adds the value to the list.
   * If the TypedValue is null or not of type DOUBLE, attempts to convert the value to a Double.
   * Throws an IllegalArgumentException if the TypedValue is of a different type.
   * 
   * @param typedValues the list of TypedValue objects to convert
   * @return a list of Double values converted from the TypedValues
   * @throws IllegalArgumentException if a TypedValue is not of type DOUBLE or is null
   */
  protected List<Double> convertValuesToDouble(List<TypedValue> typedValues) throws IllegalArgumentException {
    List<Double> doubleValues = new ArrayList<Double>();
    for (TypedValue typedValue : typedValues) {

      if (ValueType.DOUBLE.equals(typedValue.getType())) {
        doubleValues.add((Double) typedValue.getValue());

      } else if (typedValue.getType() == null) {
        // check if it is a double or a string of a decimal number

        Object value = typedValue.getValue();
        if (value instanceof Double) {
          doubleValues.add((Double) value);

        } else {
          Double doubleValue = Double.valueOf(value.toString());
          doubleValues.add(doubleValue);
        }

      } else {
        // reject other typed values
        throw new IllegalArgumentException();
      }

    }
    return doubleValues;
  }

}
