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
package org.operaton.bpm.model.bpmn.instance.operaton;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import org.junit.Ignore;
import org.junit.Test;

public class OperatonListTest extends BpmnModelElementInstanceTest {

  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(OPERATON_NS, false);
  }

  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return null;
  }

  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return null;
  }

  @Ignore("Test ignored. CAM-9441: Bug fix needed")
  @Test
  public void testListValueChildAssignment() {
    try {
      OperatonList listElement = modelInstance.newInstance(OperatonList.class);

      OperatonValue valueElement = modelInstance.newInstance(OperatonValue.class);
      valueElement.setTextContent("test");

      listElement.addChildElement(valueElement);

    } catch (Exception e) {
      fail("OperatonValue should be accepted as a child element of OperatonList. Error: " + e.getMessage());
    }
  }
}
