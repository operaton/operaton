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

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.el.FixedValue;
import org.operaton.bpm.engine.impl.form.FormException;
import org.operaton.bpm.engine.impl.form.handler.FormFieldHandler;
import org.operaton.bpm.engine.impl.form.validator.*;
import org.operaton.bpm.engine.test.api.runtime.util.TestVariableScope;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Daniel Meyer
 *
 */
public class BuiltInValidatorsTest extends PluggableProcessEngineTest {

  @Test
  public void testDefaultFormFieldValidators() {

    // assert default validators are registered
    FormValidators formValidators = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getFormValidators();

    Map<String, Class<? extends FormFieldValidator>> validators = formValidators.getValidators();
    assertEquals(RequiredValidator.class, validators.get("required"));
    assertEquals(ReadOnlyValidator.class, validators.get("readonly"));
    assertEquals(MinValidator.class, validators.get("min"));
    assertEquals(MaxValidator.class, validators.get("max"));
    assertEquals(MaxLengthValidator.class, validators.get("maxlength"));
    assertEquals(MinLengthValidator.class, validators.get("minlength"));

  }

  @Test
  public void testRequiredValidator() {
    RequiredValidator validator = new RequiredValidator();
    TestValidatorContext validatorContext = new TestValidatorContext(null);

    assertTrue(validator.validate("test", validatorContext));
    assertTrue(validator.validate(1, validatorContext));
    assertTrue(validator.validate(true, validatorContext));

    // empty string and 'null' are invalid without default
    assertFalse(validator.validate("", validatorContext));
    assertFalse(validator.validate(null, validatorContext));

    // can submit null if the value already exists
    validatorContext = new TestValidatorContext(null, "fieldName");
    validatorContext.getVariableScope().setVariable("fieldName", "existingValue");
    assertTrue(validator.validate(null, validatorContext));

    // can submit null if a default value exists
    validatorContext = new TestValidatorContext(null, "fieldName");
    validatorContext.getFormFieldHandler().setDefaultValueExpression(new FixedValue("defaultValue"));
    assertTrue(validator.validate(null, validatorContext));
    assertEquals("defaultValue", validatorContext.getVariableScope().getVariable("fieldName"));
  }

  @Test
  public void testReadOnlyValidator() {
    ReadOnlyValidator validator = new ReadOnlyValidator();

    assertFalse(validator.validate("", null));
    assertFalse(validator.validate("aaa", null));
    assertFalse(validator.validate(11, null));
    assertFalse(validator.validate(2d, null));
    assertTrue(validator.validate(null, null));
  }

  @Test
  public void testMinValidator() {
    MinValidator validator = new MinValidator();

    assertTrue(validator.validate(null, null));

    assertTrue(validator.validate(4, new TestValidatorContext("4")));
    assertFalse(validator.validate(4, new TestValidatorContext("5")));

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate(4, validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate Integer value 4: configuration 4.4 cannot be parsed as Integer.");

    assertFalse(validator.validate(4d, new TestValidatorContext("4.1")));
    assertTrue(validator.validate(4.1d, new TestValidatorContext("4.1")));

    assertFalse(validator.validate(4f, new TestValidatorContext("4.1")));
    assertTrue(validator.validate(4.1f, new TestValidatorContext("4.1")));

  }

  @Test
  public void testMaxValidator() {
    MaxValidator validator = new MaxValidator();

    assertTrue(validator.validate(null, null));

    assertTrue(validator.validate(3, new TestValidatorContext("4")));
    assertFalse(validator.validate(4, new TestValidatorContext("3")));

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate(4, validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate Integer value 4: configuration 4.4 cannot be parsed as Integer.");

    assertFalse(validator.validate(4.1d, new TestValidatorContext("4")));
    assertTrue(validator.validate(4.1d, new TestValidatorContext("4.2")));

    assertFalse(validator.validate(4.1f, new TestValidatorContext("4")));
    assertTrue(validator.validate(4.1f, new TestValidatorContext("4.2")));

  }

  @Test
  public void testMaxLengthValidator() {
    MaxLengthValidator validator = new MaxLengthValidator();

    assertTrue(validator.validate(null, null));

    assertTrue(validator.validate("test", new TestValidatorContext("4")));
    assertFalse(validator.validate("test", new TestValidatorContext("3")));

    TestValidatorContext validatorContext = new TestValidatorContext("4.4");
    assertThatThrownBy(() -> validator.validate("test", validatorContext))
      .isInstanceOf(FormException.class)
      .hasMessageContaining("Cannot validate \"maxlength\": configuration 4.4 cannot be interpreted as Integer");
  }

  @Test
  public void testMinLengthValidator() {
    MinLengthValidator validator = new MinLengthValidator();

    assertTrue(validator.validate(null, null));

    assertTrue(validator.validate("test", new TestValidatorContext("4")));
    assertFalse(validator.validate("test", new TestValidatorContext("5")));

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
