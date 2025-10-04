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

/**
 * Abstract base class for COLLECT hit policy handlers with numeric aggregation functions.
 *
 * <p>This class provides common functionality for implementing the COLLECT hit policy with
 * aggregation functions (COUNT, SUM, MIN, MAX) as defined in the DMN 1.3 specification
 * (section 8.2.8). It handles the collection of output values, type detection, type conversion,
 * and delegates the actual aggregation logic to subclasses.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Validates that decision tables have only single outputs (aggregation not supported for compound outputs)</li>
 *   <li>Collects output values from all matching rules</li>
 *   <li>Detects the appropriate numeric type (Integer, Long, or Double) for aggregation</li>
 *   <li>Converts values to the detected type</li>
 *   <li>Delegates type-specific aggregation to subclasses</li>
 *   <li>Returns the aggregated result as a typed value</li>
 * </ul>
 *
 * <p><strong>Type Detection Strategy:</strong></p>
 * <p>The handler attempts type conversion in the following priority order:</p>
 * <ol>
 *   <li><strong>Integer:</strong> If all values can be represented as integers</li>
 *   <li><strong>Long:</strong> If all values can be represented as longs (but not all as integers)</li>
 *   <li><strong>Double:</strong> If all values can be represented as doubles (but not as longs)</li>
 * </ol>
 *
 * <p><strong>Subclass Implementations:</strong></p>
 * <ul>
 *   <li>{@link CollectCountHitPolicyHandler} - Counts matching rules (returns count as Integer)</li>
 *   <li>{@link CollectSumHitPolicyHandler} - Sums numeric values</li>
 *   <li>{@link CollectMinHitPolicyHandler} - Returns minimum numeric value</li>
 *   <li>{@link CollectMaxHitPolicyHandler} - Returns maximum numeric value</li>
 * </ul>
 *
 * @see DmnHitPolicyHandler
 * @see CollectHitPolicyHandler
 */
public abstract class AbstractCollectNumberHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

  /**
   * Enumeration of supported numeric types for aggregation.
   */
  private enum NumberType {
    INTEGER, LONG, DOUBLE
  }

  private static final Map<NumberType, ValueType> TYPE_MAPPING = Map.of(
    NumberType.INTEGER, ValueType.INTEGER,
    NumberType.LONG, ValueType.LONG,
    NumberType.DOUBLE, ValueType.DOUBLE
  );

  private static final List<NumberType> TYPE_PRIORITY = List.of(
    NumberType.INTEGER,
    NumberType.LONG,
    NumberType.DOUBLE
  );

  /**
   * Returns the aggregation function implemented by this handler.
   *
   * @return the built-in aggregator (COUNT, SUM, MIN, or MAX)
   */
  protected abstract BuiltinAggregator getAggregator();

  /**
   * Applies the COLLECT hit policy with aggregation to the evaluation event.
   *
   * <p>This method collects all output values from matching rules, aggregates them
   * according to the specific aggregation function, and stores the result in the
   * evaluation event.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event containing matching rules
   * @return the evaluation event with the aggregated result set
   * @throws DmnHitPolicyException if aggregation cannot be applied (e.g., compound outputs)
   */
  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    String resultName = getResultName(decisionTableEvaluationEvent);
    TypedValue resultValue = getResultValue(decisionTableEvaluationEvent);

    DmnDecisionTableEvaluationEventImpl evaluationEvent = (DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent;
    evaluationEvent.setCollectResultName(resultName);
    evaluationEvent.setCollectResultValue(resultValue);

    return evaluationEvent;
  }

  /**
   * Extracts the output name from the first matching rule.
   *
   * <p>Since aggregation is only supported for single outputs, this method returns
   * the name of the single output column.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event
   * @return the output name, or null if no matching rules exist
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
   * Collects and aggregates the output values from all matching rules.
   *
   * @param decisionTableEvaluationEvent the evaluation event
   * @return the aggregated typed value
   */
  protected TypedValue getResultValue(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<TypedValue> values = collectSingleValues(decisionTableEvaluationEvent);
    return aggregateValues(values);
  }

  /**
   * Collects output values from all matching rules.
   *
   * <p>This method validates that each rule has at most one output (aggregation is not
   * supported for compound outputs). Empty output entries are ignored.</p>
   *
   * @param decisionTableEvaluationEvent the evaluation event
   * @return list of output values to aggregate
   * @throws DmnHitPolicyException if any rule has multiple outputs (compound output)
   */
  protected List<TypedValue> collectSingleValues(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<TypedValue> values = new ArrayList<>();
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
   * Aggregates the collected values.
   *
   * @param values the list of values to aggregate
   * @return the aggregated result, or null if the list is empty
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
   * Aggregates numeric values after detecting and converting to the appropriate type.
   *
   * <p>This method detects the most specific numeric type that can represent all values,
   * converts all values to that type, and delegates to the type-specific aggregation method.</p>
   *
   * @param values the list of values to aggregate
   * @return the aggregated result as a typed value
   * @throws DmnHitPolicyException if values cannot be converted to numeric types
   */
  protected TypedValue aggregateNumberValues(List<TypedValue> values) {
    NumberType detectedType = detectNumberType(values);

    return switch (detectedType) {
      case INTEGER -> {
        List<Integer> intValues = convertToIntegers(values);
        yield Variables.integerValue(aggregateIntegerValues(intValues));
      }
      case LONG -> {
        List<Long> longValues = convertToLongs(values);
        yield Variables.longValue(aggregateLongValues(longValues));
      }
      case DOUBLE -> {
        List<Double> doubleValues = convertToDoubles(values);
        yield Variables.doubleValue(aggregateDoubleValues(doubleValues));
      }
    };
  }

  /**
   * Detects the most specific numeric type that can represent all values.
   *
   * <p>Tries conversion in the order: Integer → Long → Double. Returns the first
   * type where all values can be successfully converted.</p>
   *
   * @param values the values to analyze
   * @return the detected number type
   * @throws DmnHitPolicyException if values cannot be converted to any supported type
   */
  private NumberType detectNumberType(List<TypedValue> values) {
    for (NumberType numberType : TYPE_PRIORITY) {
      ValueType valueType = TYPE_MAPPING.get(numberType);

      boolean allValuesCanBeConverted = values.stream()
        .allMatch(typedValue -> canConvertToType(typedValue, valueType, numberType));

      if (allValuesCanBeConverted) {
        if (LOG.isDebugEnabled()) {
          LOG.numberTypeDetected(valueType.getName(), values.size());
        }
        return numberType;
      }
    }

    throw LOG.unableToConvertValuesToAggregatableTypes(values, Integer.class, Long.class, Double.class);
  }

  private boolean canConvertToType(TypedValue typedValue, ValueType expectedType, NumberType numberType) {
    if (expectedType.equals(typedValue.getType())) {
      return true;
    }

    if (typedValue.getType() == null) {
      Object value = typedValue.getValue();
      return switch (numberType) {
        case INTEGER -> value instanceof Integer;
        case LONG -> value instanceof Long || canParseAsLong(value);
        case DOUBLE -> value instanceof Number || canParseAsDouble(value);
      };
    }

    return false;
  }

  private boolean canParseAsLong(Object value) {
    try {
      Long.valueOf(value.toString());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean canParseAsDouble(Object value) {
    try {
      Double.valueOf(value.toString());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private List<Integer> convertToIntegers(List<TypedValue> typedValues) {
    List<Integer> result = new ArrayList<>();
    for (TypedValue typedValue : typedValues) {
      Object value = typedValue.getValue();
      if (ValueType.INTEGER.equals(typedValue.getType()) || value instanceof Integer) {
        result.add((Integer) value);
      } else {
        throw new IllegalArgumentException("Cannot convert value to Integer: " + value);
      }
    }
    return result;
  }

  private List<Long> convertToLongs(List<TypedValue> typedValues) {
    List<Long> result = new ArrayList<>();
    for (TypedValue typedValue : typedValues) {
      Object value = typedValue.getValue();
      if (ValueType.LONG.equals(typedValue.getType()) || value instanceof Long) {
        result.add((Long) value);
      } else if (typedValue.getType() == null) {
        result.add(Long.valueOf(value.toString()));
      } else {
        throw new IllegalArgumentException("Cannot convert value to Long: " + value);
      }
    }
    return result;
  }

  private List<Double> convertToDoubles(List<TypedValue> typedValues) {
    List<Double> result = new ArrayList<>();
    for (TypedValue typedValue : typedValues) {
      Object value = typedValue.getValue();
      if (ValueType.DOUBLE.equals(typedValue.getType()) || value instanceof Double) {
        result.add((Double) value);
      } else if (value instanceof Number numberValue) {
        result.add(numberValue.doubleValue());
      } else if (typedValue.getType() == null) {
        result.add(Double.valueOf(value.toString()));
      } else {
        throw new IllegalArgumentException("Cannot convert value to Double: " + value);
      }
    }
    return result;
  }

  /**
   * Aggregates a list of integer values according to the specific aggregation function.
   *
   * @param intValues the integer values to aggregate
   * @return the aggregated integer result
   */
  protected abstract Integer aggregateIntegerValues(List<Integer> intValues);

  /**
   * Aggregates a list of long values according to the specific aggregation function.
   *
   * @param longValues the long values to aggregate
   * @return the aggregated long result
   */
  protected abstract Long aggregateLongValues(List<Long> longValues);

  /**
   * Aggregates a list of double values according to the specific aggregation function.
   *
   * @param doubleValues the double values to aggregate
   * @return the aggregated double result
   */
  protected abstract Double aggregateDoubleValues(List<Double> doubleValues);

}
