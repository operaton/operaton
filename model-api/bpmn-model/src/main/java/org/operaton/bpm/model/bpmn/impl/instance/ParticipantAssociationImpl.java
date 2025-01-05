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

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.Participant;
import org.operaton.bpm.model.bpmn.instance.ParticipantAssociation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PARTICIPANT_ASSOCIATION;

/**
 * The BPMN participantAssociation element
 *
 * @author Sebastian Menski
 */
public class ParticipantAssociationImpl extends BaseElementImpl implements ParticipantAssociation {

  protected static ElementReference<Participant, InnerParticipantRef> innerParticipantRefChild;
  protected static ElementReference<Participant, OuterParticipantRef> outerParticipantRefChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ParticipantAssociation.class, BPMN_ELEMENT_PARTICIPANT_ASSOCIATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(instanceContext -> new ParticipantAssociationImpl(instanceContext));

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    innerParticipantRefChild = sequenceBuilder.element(InnerParticipantRef.class)
      .required()
      .qNameElementReference(Participant.class)
      .build();

    outerParticipantRefChild = sequenceBuilder.element(OuterParticipantRef.class)
      .required()
      .qNameElementReference(Participant.class)
      .build();

    typeBuilder.build();
  }

  public ParticipantAssociationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Participant getInnerParticipant() {
    return innerParticipantRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setInnerParticipant(Participant innerParticipant) {
   innerParticipantRefChild.setReferenceTargetElement(this, innerParticipant);
  }

  @Override
  public Participant getOuterParticipant() {
    return outerParticipantRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setOuterParticipant(Participant outerParticipant) {
     outerParticipantRefChild.setReferenceTargetElement(this, outerParticipant);
  }
}
