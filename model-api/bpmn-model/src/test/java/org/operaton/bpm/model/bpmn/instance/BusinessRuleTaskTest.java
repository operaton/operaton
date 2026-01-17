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
import java.util.List;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * @author Sebastian Menski
 */
public class BusinessRuleTaskTest extends BpmnModelElementInstanceTest {

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
    return List.of(
      new AttributeAssumption("implementation", false, false, "##unspecified"),
      /** operaton extensions */
      new AttributeAssumption(OPERATON_NS, "class"),
      new AttributeAssumption(OPERATON_NS, "delegateExpression"),
      new AttributeAssumption(OPERATON_NS, "expression"),
      new AttributeAssumption(OPERATON_NS, "resultVariable"),
      new AttributeAssumption(OPERATON_NS, "topic"),
      new AttributeAssumption(OPERATON_NS, "type"),
      new AttributeAssumption(OPERATON_NS, "decisionRef"),
      new AttributeAssumption(OPERATON_NS, "decisionRefBinding"),
      new AttributeAssumption(OPERATON_NS, "decisionRefVersion"),
      new AttributeAssumption(OPERATON_NS, "decisionRefVersionTag"),
      new AttributeAssumption(OPERATON_NS, "decisionRefTenantId"),
      new AttributeAssumption(OPERATON_NS, "mapDecisionResult"),
      new AttributeAssumption(OPERATON_NS, "taskPriority")
    );
  }

}
