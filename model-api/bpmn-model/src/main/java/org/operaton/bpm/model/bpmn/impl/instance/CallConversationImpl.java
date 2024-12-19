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

import org.operaton.bpm.model.bpmn.instance.CallConversation;
import org.operaton.bpm.model.bpmn.instance.ConversationNode;
import org.operaton.bpm.model.bpmn.instance.GlobalConversation;
import org.operaton.bpm.model.bpmn.instance.ParticipantAssociation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN callConversation element
 *
 * @author Sebastian Menski
 */
public class CallConversationImpl extends ConversationNodeImpl implements CallConversation {

  protected static AttributeReference<GlobalConversation> calledCollaborationRefAttribute;
  protected static ChildElementCollection<ParticipantAssociation> participantAssociationCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CallConversation.class, BPMN_ELEMENT_CALL_CONVERSATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(ConversationNode.class)
      .instanceProvider(new ModelTypeInstanceProvider<CallConversation>() {
      @Override
      public CallConversation newInstance(ModelTypeInstanceContext instanceContext) {
          return new CallConversationImpl(instanceContext);
        }
      });

    calledCollaborationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF)
      .qNameAttributeReference(GlobalConversation.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    participantAssociationCollection = sequenceBuilder.elementCollection(ParticipantAssociation.class)
      .build();

    typeBuilder.build();
  }

  public CallConversationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public GlobalConversation getCalledCollaboration() {
    return calledCollaborationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCalledCollaboration(GlobalConversation calledCollaboration) {
    calledCollaborationRefAttribute.setReferenceTargetElement(this, calledCollaboration);
  }

  @Override
  public Collection<ParticipantAssociation> getParticipantAssociations() {
    return participantAssociationCollection.get(this);
  }
}
