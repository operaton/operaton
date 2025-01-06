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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.AssociationDirection;
import org.operaton.bpm.model.bpmn.instance.Artifact;
import org.operaton.bpm.model.bpmn.instance.Association;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * @author Sebastian Menski
 */
public class AssociationImpl extends ArtifactImpl implements Association {

  protected static AttributeReference<BaseElement> sourceRefAttribute;
  protected static AttributeReference<BaseElement> targetRefAttribute;
  protected static Attribute<AssociationDirection> associationDirectionAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Association.class, BPMN_ELEMENT_ASSOCIATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(Artifact.class)
      .instanceProvider(AssociationImpl::new);

    sourceRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SOURCE_REF)
      .required()
      .qNameAttributeReference(BaseElement.class)
      .build();

    targetRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TARGET_REF)
      .required()
      .qNameAttributeReference(BaseElement.class)
      .build();

    associationDirectionAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_ASSOCIATION_DIRECTION, AssociationDirection.class)
      .defaultValue(AssociationDirection.None)
      .build();

    typeBuilder.build();
  }

  public AssociationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public BaseElement getSource() {
    return sourceRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(BaseElement source) {
    sourceRefAttribute.setReferenceTargetElement(this, source);
  }

  @Override
  public BaseElement getTarget() {
    return targetRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(BaseElement target) {
    targetRefAttribute.setReferenceTargetElement(this, target);
  }

  @Override
  public AssociationDirection getAssociationDirection() {
    return associationDirectionAttribute.getValue(this);
  }

  @Override
  public void setAssociationDirection(AssociationDirection associationDirection) {
    associationDirectionAttribute.setValue(this, associationDirection);
  }

  @Override
  public BpmnEdge getDiagramElement() {
    return (BpmnEdge) super.getDiagramElement();
  }

}
