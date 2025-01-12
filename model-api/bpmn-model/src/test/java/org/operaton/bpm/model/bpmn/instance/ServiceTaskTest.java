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
package org.operaton.bpm.model.bpmn.instance;

import java.util.Arrays;
import java.util.Collection;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.BpmnTestConstants;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * @author Sebastian Menski
 */
class ServiceTaskTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(Task.class, false);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return null;
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return Arrays.asList(
      new AttributeAssumption("implementation", false, false, "##WebService"),
      new AttributeAssumption("operationRef"),
      /** operaton extensions */
      new AttributeAssumption(OPERATON_NS, "class"),
      new AttributeAssumption(OPERATON_NS, "delegateExpression"),
      new AttributeAssumption(OPERATON_NS, "expression"),
      new AttributeAssumption(OPERATON_NS, "resultVariable"),
      new AttributeAssumption(OPERATON_NS, "topic"),
      new AttributeAssumption(OPERATON_NS, "type"),
      new AttributeAssumption(OPERATON_NS, "taskPriority")
    );
  }


  @Test
  void testOperatonTaskPriority() {
    //given
    ServiceTask service = modelInstance.newInstance(ServiceTask.class);    
    assertThat(service.getOperatonTaskPriority()).isNull();
    //when
    service.setOperatonTaskPriority(BpmnTestConstants.TEST_PROCESS_TASK_PRIORITY);
    //then
    assertThat(service.getOperatonTaskPriority()).isEqualTo(BpmnTestConstants.TEST_PROCESS_TASK_PRIORITY);    
  }
}
