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
package org.operaton.bpm.model.bpmn.instance;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.impl.instance.OperationRef;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class MessageEventDefinitionTest extends AbstractEventDefinitionTest {

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return List.of(
      new ChildElementAssumption(OperationRef.class, 0, 1)
    );
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return List.of(
      new AttributeAssumption("messageRef"),
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
  void getEventDefinition() {
    MessageEventDefinition eventDefinition = eventDefinitionQuery.filterByType(MessageEventDefinition.class).singleResult();
    assertThat(eventDefinition).isNotNull();
    assertThat(eventDefinition.getMessage().getId()).isEqualTo("message");
    assertThat(eventDefinition.getOperation()).isNull();
    assertThat(eventDefinition.getOperatonTaskPriority()).isEqualTo("5");
  }

}
