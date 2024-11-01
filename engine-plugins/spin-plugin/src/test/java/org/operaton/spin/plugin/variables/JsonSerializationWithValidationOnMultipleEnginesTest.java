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
package org.operaton.spin.plugin.variables;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.DeserializationTypeValidator;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.spin.DataFormats;
import org.operaton.spin.json.SpinJsonException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.operaton.bpm.engine.variable.Variables.objectValue;

/**
 * Test cases for multiple engines defining different validators that do not
 * override each other although Spin makes heavy use of the {@link DataFormats}
 * class that holds static references regardless of the number of Spin plugins
 * (data formats are overridden for example because they are held once in the
 * DataFormats class)
 */
public class JsonSerializationWithValidationOnMultipleEnginesTest {

  @RegisterExtension
  static ProcessEngineExtension engineRulePositive = ProcessEngineExtension.builder()
          .configurator(configuration -> {
            DeserializationTypeValidator validatorMock = mock(DeserializationTypeValidator.class);
            when(validatorMock.validate(anyString())).thenReturn(true);
            configuration
                    .setDeserializationTypeValidator(validatorMock)
                    .setDeserializationTypeValidationEnabled(true)
                    .setJdbcUrl("jdbc:h2:mem:positive");
          })
          .build();

  @RegisterExtension
  static ProcessEngineExtension engineRuleNegative = ProcessEngineExtension.builder()
          .configurator(configuration -> {
              DeserializationTypeValidator validatorMock = mock(DeserializationTypeValidator.class);
              when(validatorMock.validate(anyString())).thenReturn(false);
              configuration
                      .setDeserializationTypeValidator(validatorMock)
                      .setDeserializationTypeValidationEnabled(true)
                      .setJdbcUrl("jdbc:h2:mem:negative");
          })
          .build();

  @Test
  void shouldUsePositiveValidator() {
    // given
    engineRulePositive.manageDeployment(engineRulePositive.getRepositoryService().createDeployment()
        .addModelInstance("foo.bpmn", getOneTaskModel())
        .deploy());
    ProcessInstance instance = engineRulePositive.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

    // add serialized value
    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    engineRulePositive.getRuntimeService().setVariable(instance.getId(), "simpleBean",
        objectValue(bean).serializationDataFormat(DataFormats.JSON_DATAFORMAT_NAME).create());

    // when
    Object value = engineRulePositive.getRuntimeService().getVariable(instance.getId(), "simpleBean");

    // then
    assertEquals(bean, value);
  }

  @Test
  void shouldUseNegativeValidator() {
    // given
    engineRuleNegative.manageDeployment(engineRuleNegative.getRepositoryService().createDeployment()
        .addModelInstance("foo.bpmn", getOneTaskModel())
        .deploy());
    ProcessInstance instance = engineRuleNegative.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

    // add serialized value
    JsonSerializable bean = new JsonSerializable("a String", 42, true);

    Assertions.assertThatThrownBy(() -> engineRuleNegative.getRuntimeService().setVariable(instance.getId(), "simpleBean",
            objectValue(bean).serializationDataFormat(DataFormats.JSON_DATAFORMAT_NAME).create()))
            .isExactlyInstanceOf(ProcessEngineException.class)
            .hasMessage("Cannot deserialize")
            .hasCauseExactlyInstanceOf(SpinJsonException.class);

    // when
    engineRuleNegative.getRuntimeService().getVariable(instance.getId(), "simpleBean");
  }

  protected BpmnModelInstance getOneTaskModel() {
    return Bpmn.createExecutableProcess("oneTaskProcess")
        .startEvent()
        .userTask()
        .endEvent()
        .done();
  }
}
