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
package org.operaton.bpm.model.bpmn.instance.operaton;

import java.util.Collection;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Sebastian Menski
 */
class OperatonInputParameterTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(OPERATON_NS, false);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return null;
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return List.of(new AttributeAssumption(OPERATON_NS, "name", false, true));
  }

  @Disabled("Test ignored. CAM-9441: Bug fix needed")
  @Test
  void testInputParameterScriptChildAssignment() {
    assertThatCode(() -> {
      OperatonInputParameter inputParamElement = modelInstance.newInstance(OperatonInputParameter.class);
      inputParamElement.setOperatonName("aVariable");

      OperatonScript scriptElement = modelInstance.newInstance(OperatonScript.class);
      scriptElement.setOperatonScriptFormat("juel");
      scriptElement.setTextContent("${'a script'}");

      inputParamElement.addChildElement(scriptElement);
    }).as("OperatonScript should be accepted as a child element of OperatonInputParameter. Error: ")
      .doesNotThrowAnyException();
  }

  @Disabled("Test ignored. CAM-9441: Bug fix needed")
  @Test
  void testInputParameterListChildAssignment() {
    assertThatCode(() -> {
      OperatonInputParameter inputParamElement = modelInstance.newInstance(OperatonInputParameter.class);
      inputParamElement.setOperatonName("aVariable");

      OperatonList listElement = modelInstance.newInstance(OperatonList.class);

      inputParamElement.addChildElement(listElement);
    }).as("OperatonList should be accepted as a child element of OperatonInputParameter. Error: ")
      .doesNotThrowAnyException();
  }

  @Disabled("Test ignored. CAM-9441: Bug fix needed")
  @Test
  void testInputParameterMapChildAssignment() {
    assertThatCode(() -> {
      OperatonInputParameter inputParamElement = modelInstance.newInstance(OperatonInputParameter.class);
      inputParamElement.setOperatonName("aVariable");

      OperatonMap listElement = modelInstance.newInstance(OperatonMap.class);

      inputParamElement.addChildElement(listElement);
    }).as("OperatonMap should be accepted as a child element of OperatonInputParameter. Error: ")
      .doesNotThrowAnyException();
  }
}
