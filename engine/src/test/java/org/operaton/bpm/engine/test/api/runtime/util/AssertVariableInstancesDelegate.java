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
package org.operaton.bpm.engine.test.api.runtime.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.IntegerValue;
import org.operaton.bpm.engine.variable.value.ObjectValue;

/**
 * @author Thorben Lindhauer
 */
public class AssertVariableInstancesDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    // validate integer variable
    Integer expectedIntValue = 1234;
    assertThat(execution.getVariable("anIntegerVariable")).isEqualTo(expectedIntValue);
    assertThat(execution.getVariableTyped("anIntegerVariable").getValue()).isEqualTo(expectedIntValue);
    assertThat(execution.getVariableTyped("anIntegerVariable").getType()).isEqualTo(ValueType.INTEGER);
    assertThat(execution.getVariableLocal("anIntegerVariable")).isNull();
    assertThat(execution.<IntegerValue>getVariableLocalTyped("anIntegerVariable")).isNull();

    // set an additional local variable
    execution.setVariableLocal("aStringVariable", "aStringValue");

    String expectedStringValue = "aStringValue";
    assertThat(execution.getVariable("aStringVariable")).isEqualTo(expectedStringValue);
    assertThat(execution.getVariableTyped("aStringVariable").getValue()).isEqualTo(expectedStringValue);
    assertThat(execution.getVariableTyped("aStringVariable").getType()).isEqualTo(ValueType.STRING);
    assertThat(execution.getVariableLocal("aStringVariable")).isEqualTo(expectedStringValue);
    assertThat(execution.getVariableLocalTyped("aStringVariable").getValue()).isEqualTo(expectedStringValue);
    assertThat(execution.getVariableLocalTyped("aStringVariable").getType()).isEqualTo(ValueType.STRING);

    SimpleSerializableBean objectValue = (SimpleSerializableBean) execution.getVariable("anObjectValue");
    assertThat(objectValue).isNotNull();
    assertThat(objectValue.getIntProperty()).isEqualTo(10);
    ObjectValue variableTyped = execution.getVariableTyped("anObjectValue");
    assertThat(variableTyped.getValue(SimpleSerializableBean.class).getIntProperty()).isEqualTo(10);
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(Variables.SerializationDataFormats.JAVA.getName());

    objectValue = (SimpleSerializableBean) execution.getVariable("anUntypedObjectValue");
    assertThat(objectValue).isNotNull();
    assertThat(objectValue.getIntProperty()).isEqualTo(30);
    variableTyped = execution.getVariableTyped("anUntypedObjectValue");
    assertThat(variableTyped.getValue(SimpleSerializableBean.class).getIntProperty()).isEqualTo(30);
    assertThat(variableTyped.getSerializationDataFormat()).isEqualTo(Context.getProcessEngineConfiguration().getDefaultSerializationFormat());

  }


}
