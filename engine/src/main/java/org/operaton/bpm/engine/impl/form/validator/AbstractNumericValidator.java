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
package org.operaton.bpm.engine.impl.form.validator;

/**
 * @author Daniel Meyer
 *
 */
public abstract class AbstractNumericValidator implements FormFieldValidator {

  @Override
  public boolean validate(Object submittedValue, FormFieldValidatorContext validatorContext) {

    if (submittedValue == null) {
      return isNullValid();
    }

    String configurationString = validatorContext.getConfiguration();

    if (submittedValue instanceof Double doubleValue) {
      return validateDoubleValue(submittedValue, doubleValue, configurationString);
    }

    if (submittedValue instanceof Float floatValue) {
      return validateFloatValue(submittedValue, floatValue, configurationString);
    }

    if (submittedValue instanceof Long longValue) {
      return validateLongValue(submittedValue, longValue, configurationString);
    }

    if (submittedValue instanceof Integer integerValue) {
      return validateIntegerValue(submittedValue, integerValue, configurationString);
    }

    if (submittedValue instanceof Short shortValue) {
      return validateShortValue(submittedValue, shortValue, configurationString);
    }

    throw new FormFieldValidationException("Numeric validator %s cannot be used on non-numeric value "
        .formatted(getClass().getSimpleName())+submittedValue);
  }

  private boolean validateDoubleValue(Object submittedValue, Double doubleValue, String configurationString) {
    Double configuration = null;
    try {
      configuration = Double.parseDouble(configurationString);
    } catch( NumberFormatException e) {
      throw new FormFieldConfigurationException(configurationString, "Cannot validate Double value %s: configuration %s cannot be parsed as Double."
          .formatted(submittedValue, configurationString));
    }
    return validate(doubleValue, configuration);
  }

  private boolean validateFloatValue(Object submittedValue, Float floatValue, String configurationString) {
    Float configuration = null;
    try {
      configuration = Float.parseFloat(configurationString);
    } catch( NumberFormatException e) {
      throw new FormFieldConfigurationException(configurationString, "Cannot validate Float value %s: configuration %s cannot be parsed as Float."
          .formatted(submittedValue, configurationString));
    }
    return validate(floatValue, configuration);
  }

  private boolean validateLongValue(Object submittedValue, Long longValue, String configurationString) {
    Long configuration = null;
    try {
      configuration = Long.parseLong(configurationString);
    } catch(NumberFormatException e) {
      throw new FormFieldConfigurationException(configurationString, "Cannot validate Long value %s: configuration %s cannot be parsed as Long."
          .formatted(submittedValue, configurationString));
    }
    return validate(longValue, configuration);
  }

  private boolean validateIntegerValue(Object submittedValue, Integer integerValue, String configurationString) {
    Integer configuration = null;
    try {
      configuration = Integer.parseInt(configurationString);
    } catch( NumberFormatException e) {
      throw new FormFieldConfigurationException(configurationString, "Cannot validate Integer value %s: configuration %s cannot be parsed as Integer."
          .formatted(submittedValue, configurationString));
    }
    return validate(integerValue, configuration);
  }

  private boolean validateShortValue(Object submittedValue, Short shortValue, String configurationString) {
    Short configuration = null;
    try {
      configuration = Short.parseShort(configurationString);
    } catch( NumberFormatException e) {
      throw new FormFieldConfigurationException(configurationString, "Cannot validate Short value %s: configuration %s cannot be parsed as Short.".formatted(
          submittedValue, configurationString));
    }
    return validate(shortValue, configuration);
  }


  protected boolean isNullValid() {
    return true;
  }

  protected abstract boolean validate(Integer submittedValue, Integer configuration);

  protected abstract boolean validate(Long submittedValue, Long configuration);

  protected abstract boolean validate(Double submittedValue, Double configuration);

  protected abstract boolean validate(Float submittedValue, Float configuration);

  protected abstract boolean validate(Short submittedValue, Short configuration);
}
