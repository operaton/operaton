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
package org.operaton.bpm.engine.test.api.form;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.form.FormField;
import org.operaton.bpm.engine.form.FormFieldValidationConstraint;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.form.type.DateFormType;
import org.operaton.bpm.engine.impl.form.type.EnumFormType;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * <p>Testcase verifying support for form metadata provided using
 * custom extension elements in BPMN Xml</p>
 *
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class FormDataTest {

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected FormService formService;

  @Deployment
  @Test
  void testGetFormFieldBasicProperties() {

    runtimeService.startProcessInstanceByKey("FormDataTest.testGetFormFieldBasicProperties");

    Task task = taskService.createTaskQuery().singleResult();
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());

    // validate properties:
    List<FormField> formFields = taskFormData.getFormFields();

    // validate field 1
    FormField formField1 = formFields.get(0);
    assertThat(formField1).isNotNull();
    assertThat(formField1.getId()).isEqualTo("formField1");
    assertThat(formField1.getLabel()).isEqualTo("Form Field 1");
    assertThat(formField1.getTypeName()).isEqualTo("string");
    assertThat(formField1.getType()).isNotNull();

    // validate field 2
    FormField formField2 = formFields.get(1);
    assertThat(formField2).isNotNull();
    assertThat(formField2.getId()).isEqualTo("formField2");
    assertThat(formField2.getLabel()).isEqualTo("Form Field 2");
    assertThat(formField2.getTypeName()).isEqualTo("boolean");
    assertThat(formField1.getType()).isNotNull();

  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testGetFormFieldBuiltInTypes() {

    runtimeService.startProcessInstanceByKey("FormDataTest.testGetFormFieldBuiltInTypes");

    Task task = taskService.createTaskQuery().singleResult();

    TaskFormData taskFormData = formService.getTaskFormData(task.getId());

    // validate properties:
    List<FormField> formFields = taskFormData.getFormFields();

    // validate string field
    FormField stringField = formFields.get(0);
    assertThat(stringField).isNotNull();
    assertThat(stringField.getTypeName()).isEqualTo("string");
    assertThat(stringField.getType()).isNotNull();
    assertThat(stringField.getDefaultValue()).isEqualTo("someString");

    // validate long field
    FormField longField = formFields.get(1);
    assertThat(longField).isNotNull();
    assertThat(longField.getTypeName()).isEqualTo("long");
    assertThat(longField.getType()).isNotNull();
    assertThat(longField.getDefaultValue()).isEqualTo(1L);

    // validate boolean field
    FormField booleanField = formFields.get(2);
    assertThat(booleanField).isNotNull();
    assertThat(booleanField.getTypeName()).isEqualTo("boolean");
    assertThat(booleanField.getType()).isNotNull();
    assertThat(booleanField.getDefaultValue()).isEqualTo(Boolean.TRUE);

    // validate date field
    FormField dateField = formFields.get(3);
    assertThat(dateField).isNotNull();
    assertThat(dateField.getTypeName()).isEqualTo("date");
    assertThat(dateField.getType()).isNotNull();
    Date dateValue = (Date) dateField.getDefaultValue();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateValue);
    assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(10);
    assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
    assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2013);

    // validate enum field
    FormField enumField = formFields.get(4);
    assertThat(enumField).isNotNull();
    assertThat(enumField.getTypeName()).isEqualTo("enum");
    assertThat(enumField.getType()).isNotNull();
    EnumFormType enumFormType = (EnumFormType) enumField.getType();
    Map<String, String> values = enumFormType.getValues();
    assertThat(values)
            .containsEntry("a", "A")
            .containsEntry("b", "B")
            .containsEntry("c", "C");

  }

  @Deployment
  @Test
  void testGetFormFieldProperties() {

    runtimeService.startProcessInstanceByKey("FormDataTest.testGetFormFieldProperties");

    Task task = taskService.createTaskQuery().singleResult();

    TaskFormData taskFormData = formService.getTaskFormData(task.getId());

    List<FormField> formFields = taskFormData.getFormFields();

    FormField stringField = formFields.get(0);
    Map<String, String> properties = stringField.getProperties();
    assertThat(properties)
            .containsEntry("p1", "property1")
            .containsEntry("p2", "property2");

  }

  @Deployment
  @Test
  void testGetFormFieldValidationConstraints() {

    runtimeService.startProcessInstanceByKey("FormDataTest.testGetFormFieldValidationConstraints");

    Task task = taskService.createTaskQuery().singleResult();

    TaskFormData taskFormData = formService.getTaskFormData(task.getId());

    List<FormField> formFields = taskFormData.getFormFields();

    FormField field1 = formFields.get(0);
    List<FormFieldValidationConstraint> validationConstraints = field1.getValidationConstraints();
    FormFieldValidationConstraint constraint1 = validationConstraints.get(0);
    assertThat(constraint1.getName()).isEqualTo("maxlength");
    assertThat(constraint1.getConfiguration()).isEqualTo("10");
    FormFieldValidationConstraint constraint2 = validationConstraints.get(1);
    assertThat(constraint2.getName()).isEqualTo("minlength");
    assertThat(constraint2.getConfiguration()).isEqualTo("5");

  }

  @Deployment
  @Test
  void testFormFieldSubmit() {

    // valid submit
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FormDataTest.testFormFieldSubmit");
    Task task = taskService.createTaskQuery().singleResult();
    Map<String, Object> formValues = new HashMap<>();
    formValues.put("stringField", "12345");
    formValues.put("longField", 9L);
    formValues.put("customField", "validValue");
    formService.submitTaskForm(task.getId(), formValues);

    assertThat(runtimeService.getVariables(processInstance.getId())).isEqualTo(formValues);
    runtimeService.deleteProcessInstance(processInstance.getId(), "test complete");

    runtimeService.startProcessInstanceByKey("FormDataTest.testFormFieldSubmit");
    task = taskService.createTaskQuery().singleResult();
    // invalid submit 1

    formValues = new HashMap<>();
    formValues.put("stringField", "1234");
    formValues.put("longField", 9L);
    formValues.put("customField", "validValue");
    String taskId = task.getId();
    var finalFormValues = formValues;
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, finalFormValues))
      .isInstanceOf(FormFieldValidatorException.class)
      .asInstanceOf(type(FormFieldValidatorException.class))
      .extracting(FormFieldValidatorException::getName)
      .isEqualTo("minlength");

    // invalid submit 2
    formValues = new HashMap<>();

    formValues.put("customFieldWithValidationDetails", "C");
    var finalFormValues2 = formValues;
    assertThatThrownBy(() -> formService.submitTaskForm(taskId, finalFormValues2))
      .isInstanceOf(FormFieldValidatorException.class)
      .hasCauseInstanceOf(FormFieldValidationException.class)
      .asInstanceOf(type(FormFieldValidatorException.class))
      .satisfies(e -> {
      assertThat(e.getName()).isEqualTo("validator");
      assertThat(e.getId()).isEqualTo("customFieldWithValidationDetails");
        FormFieldValidationException exception = (FormFieldValidationException) e.getCause();
      assertThat(exception.<String>getDetail()).isEqualTo("EXPIRED");
      });
  }

  @Deployment
  @Test
  void testSubmitFormDataWithEmptyDate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FormDataTest.testSubmitFormDataWithEmptyDate");
    Task task = taskService.createTaskQuery().singleResult();
    Map<String, Object> formValues = new HashMap<>();
    formValues.put("stringField", "12345");
    formValues.put("dateField", "");

    // when
    formService.submitTaskForm(task.getId(), formValues);

    // then
    formValues.put("dateField", null);
    assertThat(runtimeService.getVariables(processInstance.getId())).isEqualTo(formValues);
  }

  @Deployment
  @Test
  void testMissingFormVariables()
  {
    // given process definition with defined form variables
    // when start process instance with no variables
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("date-form-property-test");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // then taskFormData contains form variables with null as values
    TaskFormData taskFormData = formService.getTaskFormData(task.getId());
    assertThat(taskFormData).isNotNull();
    assertThat(taskFormData.getFormFields()).hasSize(5);

    assertThat(taskFormData.getFormFields()).allSatisfy(field -> {
      assertThat(field).isNotNull();
      if (field.getType() instanceof DateFormType) {
        assertThat(field.getValue().getValue()).isNotNull().hasToString("");
      } else {
        assertThat(field.getValue().getValue()).isNull();
      }
    });
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/form/FormDataTest.testDoubleQuotesAreEscapedInGeneratedTaskForms.bpmn20.xml")
  @Test
  void testDoubleQuotesAreEscapedInGeneratedTaskForms() {

    // given
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("foo", "This is a \"Test\" message!");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    Task taskWithForm = taskService.createTaskQuery().singleResult();

    // when
    Object renderedStartForm = formService.getRenderedTaskForm(taskWithForm.getId());
    assertThat(renderedStartForm).isInstanceOf(String.class);

    // then
    String renderedForm = (String) renderedStartForm;
    String expectedFormValueWithEscapedQuotes = "This is a &quot;Test&quot; message!";
    assertThat(renderedForm).contains(expectedFormValueWithEscapedQuotes);

  }

}
