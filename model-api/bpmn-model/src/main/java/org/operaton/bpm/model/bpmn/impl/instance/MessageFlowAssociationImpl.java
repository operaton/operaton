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

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.MessageFlow;
import org.operaton.bpm.model.bpmn.instance.MessageFlowAssociation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN messageFlowAssociation element
 *
 * @author Sebastian Menski
 */
public class MessageFlowAssociationImpl extends BaseElementImpl implements MessageFlowAssociation {

  protected static AttributeReference<MessageFlow> innerMessageFlowRefAttribute;
  protected static AttributeReference<MessageFlow> outerMessageFlowRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(MessageFlowAssociation.class, BPMN_ELEMENT_MESSAGE_FLOW_ASSOCIATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(MessageFlowAssociationImpl::new);

    innerMessageFlowRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_INNER_MESSAGE_FLOW_REF)
      .required()
      .qNameAttributeReference(MessageFlow.class)
      .build();

    outerMessageFlowRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OUTER_MESSAGE_FLOW_REF)
      .required()
      .qNameAttributeReference(MessageFlow.class)
      .build();

    typeBuilder.build();
  }

  public MessageFlowAssociationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public MessageFlow getInnerMessageFlow() {
    return innerMessageFlowRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setInnerMessageFlow(MessageFlow innerMessageFlow) {
    innerMessageFlowRefAttribute.setReferenceTargetElement(this, innerMessageFlow);
  }

  @Override
  public MessageFlow getOuterMessageFlow() {
    return outerMessageFlowRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOuterMessageFlow(MessageFlow outerMessageFlow) {
    outerMessageFlowRefAttribute.setReferenceTargetElement(this, outerMessageFlow);
  }
}
