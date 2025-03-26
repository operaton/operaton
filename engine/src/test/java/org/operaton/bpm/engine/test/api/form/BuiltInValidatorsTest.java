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
package org.operaton.bpm.engine.test.api.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.el.FixedValue;
import org.operaton.bpm.engine.impl.form.FormException;
import org.operaton.bpm.engine.impl.form.handler.FormFieldHandler;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidator;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.operaton.bpm.engine.impl.form.validator.FormValidators;
import org.operaton.bpm.engine.impl.form.validator.MaxLengthValidator;
import org.operaton.bpm.engine.impl.form.validator.MaxValidator;
import org.operaton.bpm.engine.impl.form.validator.MinLengthValidator;
import org.operaton.bpm.engine.impl.form.validator.MinValidator;
import org.operaton.bpm.engine.impl.form.validator.ReadOnlyValidator;
import org.operaton.bpm.engine.impl.form.validator.RequiredValidator;
import org.operaton.bpm.engine.test.api.runtime.util.TestVariableScope;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
public class BuiltInValidatorsTest {
  
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @Test
  public void testDefaultFormFieldValidators() {

    // assert default validators are registered
    FormValidators formValidators = processEngineConfiguration.getFormValidators();

    Map<String, Class<? extends FormFieldValidator>> validators = formValidators.getValidators();
    assertThat(validators)
            .containsEntry("required", RequiredValidator.class)
            .containsEntry("readonly", ReadOnlyValidator.class)
            .containsEntry("min", MinValidator.class)
            .containsEntry("max", MaxValidator.class)
            .containsEntry("maxlength", MaxLengthValidator.class)
            .containsEntry("minlength", MinLengthValidator.class);

  }

  @Test
  public void testRequiredValidator() {
    RequiredValidator validator = new RequiredValidator();
    TestValidatorContext validatorContext = new TestValidatorContext(null);

    assertThat(validator.validate("test", validatorContext)).isTrue();
    assertThat(validator.validate(1, validatorContext)).isTrue();
    assertThat(validator.validate(true, validatorContext)).isTrue();

    // empty string and 'null' are invalid without default
    assertThat(validator.validate("", validatorContext)).isFalse();
    assertThat(validator.validate(null, validatorContext)).isFalse();

    // can submit null if the value already exists
    validatorContext = new TestValidatorContext(null, "fieldName");
    validatorContext.getVariableScope().setVariable("fieldName", "existingValue");
    assertThat(validator.validate(null, validatorContext)).isTrue();

    // can submit null if a default value exists
    validatorContext = new TestValidatorContext(null, "fieldName");
    validatorContext.getFormFieldHandler().setDefaultValueExpression(new FixedValue("defaultValue"));
    assertThat(validator.validate(null, validatorContext)).isTrue();
    assertThat(validatorContext.getVariableScope().getVariable("fieldName")).isEqualTo("defaultValue");
  }

  @Test
  public void testReadOnlyValidator() {
    ReadOnlyValidator validator = new ReadOnlyValidator();

    assertThat(validator.validate("", null)).isFalse();
    assertThat(validator.validate("aaa", null)).isFalse();
    assertThat(validator.validate(11, null)).isFalse();
    assertThat(validator.validate(2d, null)).isFalse();
    assertThat(validator.validate(null, null)).isTrue();
  }

  @Test
  public void testMinValidator() {
    MinValidator validator = new MinValidator();

    assertThat(validator.validate(null, null)).isTrue();

    assertThat(validator.validate(4, new TestValidatorContext("4"))).isTrue();
    assertThat(validator.validate(4, new TestValidatorContext("5"))).isFalse();

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate(4, validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate Integer value 4: configuration 4.4 cannot be parsed as Integer.");

    assertThat(validator.validate(4d, new TestValidatorContext("4.1"))).isFalse();
    assertThat(validator.validate(4.1d, new TestValidatorContext("4.1"))).isTrue();

    assertThat(validator.validate(4f, new TestValidatorContext("4.1"))).isFalse();
    assertThat(validator.validate(4.1f, new TestValidatorContext("4.1"))).isTrue();

  }

  @Test
  public void testMaxValidator() {
    MaxValidator validator = new MaxValidator();

    assertThat(validator.validate(null, null)).isTrue();

    assertThat(validator.validate(3, new TestValidatorContext("4"))).isTrue();
    assertThat(validator.validate(4, new TestValidatorContext("3"))).isFalse();

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate(4, validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate Integer value 4: configuration 4.4 cannot be parsed as Integer.");

    assertThat(validator.validate(4.1d, new TestValidatorContext("4"))).isFalse();
    assertThat(validator.validate(4.1d, new TestValidatorContext("4.2"))).isTrue();

    assertThat(validator.validate(4.1f, new TestValidatorContext("4"))).isFalse();
    assertThat(validator.validate(4.1f, new TestValidatorContext("4.2"))).isTrue();

  }

  @Test
  public void testMaxLengthValidator() {
    MaxLengthValidator validator = new MaxLengthValidator();

    assertThat(validator.validate(null, null)).isTrue();

    assertThat(validator.validate("test", new TestValidatorContext("4"))).isTrue();
    assertThat(validator.validate("test", new TestValidatorContext("3"))).isFalse();

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate("test", validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate \"maxlength\": configuration 4.4 cannot be interpreted as Integer");
  }

  @Test
  public void testMinLengthValidator() {
    MinLengthValidator validator = new MinLengthValidator();

    assertThat(validator.validate(null, null)).isTrue();

    assertThat(validator.validate("test", new TestValidatorContext("4"))).isTrue();
    assertThat(validator.validate("test", new TestValidatorContext("5"))).isFalse();

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate("test", validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate \"minlength\": configuration 4.4 cannot be interpreted as Integer");
  }

  protected static class TestValidatorContext implements FormFieldValidatorContext {

    TestVariableScope variableScope = new TestVariableScope();
    FormFieldHandler formFieldHandler = new FormFieldHandler();
    String configuration;

    public TestValidatorContext(String configuration) {
      this.configuration = configuration;
    }

    public TestValidatorContext(String configuration, String formFieldId) {
      this.configuration = configuration;
      this.formFieldHandler.setId(formFieldId);
    }

    @Override
    public FormFieldHandler getFormFieldHandler() {
      return formFieldHandler;
    }

    @Override
    public DelegateExecution getExecution() {
      return null;
    }

    @Override
    public String getConfiguration() {
      return configuration;
    }

    @Override
    public Map<String, Object> getSubmittedValues() {
      return null;
    }

    @Override
    public VariableScope getVariableScope() {
      return variableScope;
    }
  }

}
