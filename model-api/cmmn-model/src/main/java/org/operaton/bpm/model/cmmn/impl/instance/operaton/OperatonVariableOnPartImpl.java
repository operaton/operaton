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
package org.operaton.bpm.model.cmmn.impl.instance.operaton;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_VARIABLE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ELEMENT_VARIABLE_ON_PART;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;

import org.operaton.bpm.model.cmmn.VariableTransition;
import org.operaton.bpm.model.cmmn.impl.instance.CmmnModelElementInstanceImpl;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonVariableOnPart;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonVariableTransitionEvent;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

public class OperatonVariableOnPartImpl extends CmmnModelElementInstanceImpl implements OperatonVariableOnPart {

  protected static Attribute<String> operatonVariableNameAttribute;
  protected static ChildElement<OperatonVariableTransitionEvent> operatonVariableEventChild; 
  
  public OperatonVariableOnPartImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(ModelBuilder modelBuilder) {

    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonVariableOnPart.class, OPERATON_ELEMENT_VARIABLE_ON_PART)
      .namespaceUri(CAMUNDA_NS)
      .instanceProvider(instanceContext -> new OperatonVariableOnPartImpl(instanceContext));

    operatonVariableNameAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VARIABLE_NAME)
      .namespace(CAMUNDA_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonVariableEventChild = sequenceBuilder.element(OperatonVariableTransitionEvent.class)
      .build();

    typeBuilder.build();
  }

  @Override
  public String getVariableName() {
    return operatonVariableNameAttribute.getValue(this);
  }

  @Override
  public void setVariableName(String name) {
    operatonVariableNameAttribute.setValue(this, name);
  }


  @Override
  public VariableTransition getVariableEvent() {
    OperatonVariableTransitionEvent child = operatonVariableEventChild.getChild(this);
    return child.getValue();
  }

  @Override
  public void setVariableEvent(VariableTransition variableTransition) {
    OperatonVariableTransitionEvent child = operatonVariableEventChild.getChild(this);
    child.setValue(variableTransition);
  }

}
