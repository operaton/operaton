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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.form.FormProperty;
import org.operaton.bpm.engine.form.StartFormData;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

public class FormPropertyDefaultValueTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testDefaultValue() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("FormPropertyDefaultValueTest.testDefaultValue");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    TaskFormData formData = formService.getTaskFormData(task.getId());
    List<FormProperty> formProperties = formData.getFormProperties();
    assertThat(formProperties).hasSize(4);

    for (FormProperty prop : formProperties) {
      if ("booleanProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("true");
      } else if ("stringProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("someString");
      } else if ("longProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("42");
      } else if ("longExpressionProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("23");
      } else {
        assertThat(false).as("Invalid form property: " + prop.getId()).isTrue();
      }
    }

    Map<String, String> formDataUpdate = new HashMap<>();
    formDataUpdate.put("longExpressionProperty", "1");
    formDataUpdate.put("booleanProperty", "false");
    formService.submitTaskFormData(task.getId(), formDataUpdate);

    assertThat(runtimeService.getVariable(processInstance.getId(), "booleanProperty")).isFalse();
    assertThat(runtimeService.getVariable(processInstance.getId(), "stringProperty")).isEqualTo("someString");
    assertThat(runtimeService.getVariable(processInstance.getId(), "longProperty")).isEqualTo(42L);
    assertThat(runtimeService.getVariable(processInstance.getId(), "longExpressionProperty")).isEqualTo(1L);
  }
  
  @Deployment
  @Test
  public void testStartFormDefaultValue() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("FormPropertyDefaultValueTest.testDefaultValue")
      .latestVersion()
      .singleResult()
      .getId();
    
    StartFormData startForm = formService.getStartFormData(processDefinitionId);
    
    
    List<FormProperty> formProperties = startForm.getFormProperties();
    assertThat(formProperties).hasSize(4);

    for (FormProperty prop : formProperties) {
      if ("booleanProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("true");
      } else if ("stringProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("someString");
      } else if ("longProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("42");
      } else if ("longExpressionProperty".equals(prop.getId())) {
        assertThat(prop.getValue()).isEqualTo("23");
      } else {
        assertThat(false).as("Invalid form property: " + prop.getId()).isTrue();
      }
    }

    // Override 2 properties. The others should pe posted as the default-value
    Map<String, String> formDataUpdate = new HashMap<>();
    formDataUpdate.put("longExpressionProperty", "1");
    formDataUpdate.put("booleanProperty", "false");
    ProcessInstance processInstance = formService.submitStartFormData(processDefinitionId, formDataUpdate);

    assertThat(runtimeService.getVariable(processInstance.getId(), "booleanProperty")).isFalse();
    assertThat(runtimeService.getVariable(processInstance.getId(), "stringProperty")).isEqualTo("someString");
    assertThat(runtimeService.getVariable(processInstance.getId(), "longProperty")).isEqualTo(42L);
    assertThat(runtimeService.getVariable(processInstance.getId(), "longExpressionProperty")).isEqualTo(1L);
  }
}
