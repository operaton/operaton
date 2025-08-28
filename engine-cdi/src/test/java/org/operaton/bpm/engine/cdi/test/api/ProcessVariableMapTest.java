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
package org.operaton.bpm.engine.cdi.test.api;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Michael Scholz
 */
@RunWith(Arquillian.class)
public class ProcessVariableMapTest extends CdiProcessEngineTestCase {

  private static final String VARNAME_1 = "aVariable";
  private static final String VARNAME_2 = "anotherVariable";

  @Test
  public void testProcessVariableMap() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    VariableMap variables = (VariableMap) getBeanInstance("processVariableMap");
    assertThat(variables).isNotNull();

    // ////////////////////////////////////////////////////////////////
    // Put a variable via BusinessProcess and get it via VariableMap //
    // ////////////////////////////////////////////////////////////////
    String aValue = "aValue";
    businessProcess.setVariable(VARNAME_1, Variables.stringValue(aValue));

    // Legacy API
    assertThat(variables).containsEntry(VARNAME_1, aValue);

    // Typed variable API
    TypedValue aTypedValue = variables.getValueTyped(VARNAME_1);
    assertThat(aTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(aTypedValue.getValue()).isEqualTo(aValue);
    assertThat(variables.getValue(VARNAME_1, String.class)).isEqualTo(aValue);

    // Type API with wrong type
    try {
      variables.getValue(VARNAME_1, Integer.class);
      fail("ClassCastException expected!");
    } catch(ClassCastException ex) {
      assertThat(ex.getMessage()).isEqualTo("Cannot cast variable named 'aVariable' with value 'aValue' to type 'class java.lang.Integer'.");
    }

    // ////////////////////////////////////////////////////////////////
    // Put a variable via VariableMap and get it via BusinessProcess //
    // ////////////////////////////////////////////////////////////////
    String anotherValue = "anotherValue";
    variables.put(VARNAME_2, Variables.stringValue(anotherValue));

    // Legacy API
    assertThat((String)businessProcess.getVariable(VARNAME_2)).isEqualTo(anotherValue);

    // Typed variable API
    TypedValue anotherTypedValue = businessProcess.getVariableTyped(VARNAME_2);
    assertThat(anotherTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(anotherTypedValue.getValue()).isEqualTo(anotherValue);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
  public void testProcessVariableMapLocal() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);
    businessProcess.startProcessByKey("businessProcessBeanTest");

    VariableMap variables = (VariableMap) getBeanInstance("processVariableMapLocal");
    assertThat(variables).isNotNull();

    // ////////////////////////////////////////////////////////////////
    // Put a variable via BusinessProcess and get it via VariableMap //
    // ////////////////////////////////////////////////////////////////
    String aValue = "aValue";
    businessProcess.setVariableLocal(VARNAME_1, Variables.stringValue(aValue));

    // Legacy API
    assertThat(variables).containsEntry(VARNAME_1, aValue);

    // Typed variable API
    TypedValue aTypedValue = variables.getValueTyped(VARNAME_1);
    assertThat(aTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(aTypedValue.getValue()).isEqualTo(aValue);
    assertThat(variables.getValue(VARNAME_1, String.class)).isEqualTo(aValue);

    // Type API with wrong type
    try {
      variables.getValue(VARNAME_1, Integer.class);
      fail("ClassCastException expected!");
    } catch(ClassCastException ex) {
      assertThat(ex.getMessage()).isEqualTo("Cannot cast variable named 'aVariable' with value 'aValue' to type 'class java.lang.Integer'.");
    }

    // ////////////////////////////////////////////////////////////////
    // Put a variable via VariableMap and get it via BusinessProcess //
    // ////////////////////////////////////////////////////////////////
    String anotherValue = "anotherValue";
    variables.put(VARNAME_2, Variables.stringValue(anotherValue));

    // Legacy API
    assertThat((String)businessProcess.getVariableLocal(VARNAME_2)).isEqualTo(anotherValue);

    // Typed variable API
    TypedValue anotherTypedValue = businessProcess.getVariableLocalTyped(VARNAME_2);
    assertThat(anotherTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(anotherTypedValue.getValue()).isEqualTo(anotherValue);
  }
}
