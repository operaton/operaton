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
package org.operaton.bpm.model.bpmn.impl.instance;

import java.util.List;
import org.operaton.bpm.model.bpmn.instance.Condition;
import org.operaton.bpm.model.bpmn.instance.ConditionalEventDefinition;
import org.operaton.bpm.model.bpmn.instance.EventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONDITIONAL_EVENT_DEFINITION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_VARIABLE_NAME;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import org.operaton.bpm.model.xml.impl.util.StringUtil;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_VARIABLE_EVENTS;

/**
 * The BPMN conditionalEventDefinition element
 *
 * @author Sebastian Menski
 */
public class ConditionalEventDefinitionImpl extends EventDefinitionImpl implements ConditionalEventDefinition {

  protected static ChildElement<Condition> conditionChild;
  protected static Attribute<String> operatonVariableName;
  protected static Attribute<String> operatonVariableEvents;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConditionalEventDefinition.class, BPMN_ELEMENT_CONDITIONAL_EVENT_DEFINITION)
      .namespaceUri(BPMN20_NS)
      .extendsType(EventDefinition.class)
      .instanceProvider(ConditionalEventDefinitionImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    conditionChild = sequenceBuilder.element(Condition.class)
      .required()
      .build();

    /** operaton extensions */

    operatonVariableName = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VARIABLE_NAME)
      .namespace(OPERATON_NS)
      .build();

    operatonVariableEvents = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VARIABLE_EVENTS)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public ConditionalEventDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public Condition getCondition() {
    return conditionChild.getChild(this);
  }

  @Override
  public void setCondition(Condition condition) {
    conditionChild.setChild(this, condition);
  }

  @Override
  public String getOperatonVariableName() {
    return operatonVariableName.getValue(this);
  }

  @Override
  public void setOperatonVariableName(String variableName) {
    operatonVariableName.setValue(this, variableName);
  }

  @Override
  public String getOperatonVariableEvents() {
    return operatonVariableEvents.getValue(this);
  }

  @Override
  public void setOperatonVariableEvents(String variableEvents) {
    operatonVariableEvents.setValue(this, variableEvents);
  }

  @Override
  public List<String> getOperatonVariableEventsList() {
    String variableEvents = operatonVariableEvents.getValue(this);
    return StringUtil.splitCommaSeparatedList(variableEvents);
  }

  @Override
  public void setOperatonVariableEventsList(List<String> variableEventsList) {
    String variableEvents = StringUtil.joinCommaSeparatedList(variableEventsList);
    operatonVariableEvents.setValue(this, variableEvents);
  }
}
