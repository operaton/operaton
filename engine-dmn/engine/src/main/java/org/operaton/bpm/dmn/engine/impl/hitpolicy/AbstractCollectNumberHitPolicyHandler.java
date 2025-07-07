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

public abstract class AbstractCollectNumberHitPolicyHandler implements DmnHitPolicyHandler {

  public static final DmnHitPolicyLogger LOG = DmnLogger.HIT_POLICY_LOGGER;

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

  protected abstract BuiltinAggregator getAggregator();

  @Override
  public DmnDecisionTableEvaluationEvent apply(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    String resultName = getResultName(decisionTableEvaluationEvent);
    TypedValue resultValue = getResultValue(decisionTableEvaluationEvent);

    DmnDecisionTableEvaluationEventImpl evaluationEvent = (DmnDecisionTableEvaluationEventImpl) decisionTableEvaluationEvent;
    evaluationEvent.setCollectResultName(resultName);
    evaluationEvent.setCollectResultValue(resultValue);

    return evaluationEvent;
  }

  protected String getResultName(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    for (DmnEvaluatedDecisionRule matchingRule : decisionTableEvaluationEvent.getMatchingRules()) {
      Map<String, DmnEvaluatedOutput> outputEntries = matchingRule.getOutputEntries();
      if (!outputEntries.isEmpty()) {
        return outputEntries.values().iterator().next().getOutputName();
      }
    }
    return null;
  }

  protected TypedValue getResultValue(DmnDecisionTableEvaluationEvent decisionTableEvaluationEvent) {
    List<TypedValue> values = collectSingleValues(decisionTableEvaluationEvent);
    return aggregateValues(values);
  }

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

  protected TypedValue aggregateValues(List<TypedValue> values) {
    if (!values.isEmpty()) {
      return aggregateNumberValues(values);
    }
    else {
      // return null if no values to aggregate
      return null;
    }
  }

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

  protected abstract Integer aggregateIntegerValues(List<Integer> intValues);

  protected abstract Long aggregateLongValues(List<Long> longValues);

  protected abstract Double aggregateDoubleValues(List<Double> doubleValues);

}
