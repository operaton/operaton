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
package org.operaton.bpm.model.bpmn.impl.instance.bpmndi;

import org.operaton.bpm.model.bpmn.impl.instance.di.LabeledShapeImpl;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnLabel;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.operaton.bpm.model.bpmn.instance.bpmndi.ParticipantBandKind;
import org.operaton.bpm.model.bpmn.instance.di.LabeledShape;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMNDI BPMNShape element
 *
 * @author Sebastian Menski
 */
public class BpmnShapeImpl extends LabeledShapeImpl implements BpmnShape {

  protected static AttributeReference<BaseElement> bpmnElementAttribute;
  protected static Attribute<Boolean> isHorizontalAttribute;
  protected static Attribute<Boolean> isExpandedAttribute;
  protected static Attribute<Boolean> isMarkerVisibleAttribute;
  protected static Attribute<Boolean> isMessageVisibleAttribute;
  protected static Attribute<ParticipantBandKind> participantBandKindAttribute;
  protected static AttributeReference<BpmnShape> choreographyActivityShapeAttribute;
  protected static ChildElement<BpmnLabel> bpmnLabelChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BpmnShape.class, BPMNDI_ELEMENT_BPMN_SHAPE)
      .namespaceUri(BPMNDI_NS)
      .extendsType(LabeledShape.class)
      .instanceProvider(instanceContext -> new BpmnShapeImpl(instanceContext));

    bpmnElementAttribute = typeBuilder.stringAttribute(BPMNDI_ATTRIBUTE_BPMN_ELEMENT)
      .qNameAttributeReference(BaseElement.class)
      .build();

    isHorizontalAttribute = typeBuilder.booleanAttribute(BPMNDI_ATTRIBUTE_IS_HORIZONTAL)
      .build();

    isExpandedAttribute = typeBuilder.booleanAttribute(BPMNDI_ATTRIBUTE_IS_EXPANDED)
      .build();

    isMarkerVisibleAttribute = typeBuilder.booleanAttribute(BPMNDI_ATTRIBUTE_IS_MARKER_VISIBLE)
      .build();

    isMessageVisibleAttribute = typeBuilder.booleanAttribute(BPMNDI_ATTRIBUTE_IS_MESSAGE_VISIBLE)
      .build();

    participantBandKindAttribute = typeBuilder.enumAttribute(BPMNDI_ATTRIBUTE_PARTICIPANT_BAND_KIND, ParticipantBandKind.class)
      .build();

    choreographyActivityShapeAttribute = typeBuilder.stringAttribute(BPMNDI_ATTRIBUTE_CHOREOGRAPHY_ACTIVITY_SHAPE)
      .qNameAttributeReference(BpmnShape.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bpmnLabelChild = sequenceBuilder.element(BpmnLabel.class)
      .build();

    typeBuilder.build();
  }

  public BpmnShapeImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public BaseElement getBpmnElement() {
    return bpmnElementAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setBpmnElement(BaseElement bpmnElement) {
    bpmnElementAttribute.setReferenceTargetElement(this, bpmnElement);
  }

  @Override
  public boolean isHorizontal() {
    return isHorizontalAttribute.getValue(this);
  }

  @Override
  public void setHorizontal(boolean isHorizontal) {
    isHorizontalAttribute.setValue(this, isHorizontal);
  }

  @Override
  public boolean isExpanded() {
    return isExpandedAttribute.getValue(this);
  }

  @Override
  public void setExpanded(boolean isExpanded) {
    isExpandedAttribute.setValue(this, isExpanded);
  }

  @Override
  public boolean isMarkerVisible() {
    return isMarkerVisibleAttribute.getValue(this);
  }

  @Override
  public void setMarkerVisible(boolean isMarkerVisible) {
    isMarkerVisibleAttribute.setValue(this, isMarkerVisible);
  }

  @Override
  public boolean isMessageVisible() {
    return isMessageVisibleAttribute.getValue(this);
  }

  @Override
  public void setMessageVisible(boolean isMessageVisible) {
    isMessageVisibleAttribute.setValue(this, isMessageVisible);
  }

  @Override
  public ParticipantBandKind getParticipantBandKind() {
    return participantBandKindAttribute.getValue(this);
  }

  @Override
  public void setParticipantBandKind(ParticipantBandKind participantBandKind) {
    participantBandKindAttribute.setValue(this, participantBandKind);
  }

  @Override
  public BpmnShape getChoreographyActivityShape() {
    return choreographyActivityShapeAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setChoreographyActivityShape(BpmnShape choreographyActivityShape) {
    choreographyActivityShapeAttribute.setReferenceTargetElement(this, choreographyActivityShape);
  }

  @Override
  public BpmnLabel getBpmnLabel() {
    return bpmnLabelChild.getChild(this);
  }

  @Override
  public void setBpmnLabel(BpmnLabel bpmnLabel) {
    bpmnLabelChild.setChild(this, bpmnLabel);
  }
}
