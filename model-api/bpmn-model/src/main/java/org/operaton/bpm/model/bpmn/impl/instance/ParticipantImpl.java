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

import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN participant element
 *
 * @author Sebastian Menski
 */
public class ParticipantImpl extends BaseElementImpl implements Participant {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<Process> processRefAttribute;
  protected static ElementReferenceCollection<Interface, InterfaceRef> interfaceRefCollection;
  protected static ElementReferenceCollection<EndPoint, EndPointRef> endPointRefCollection;
  protected static ChildElement<ParticipantMultiplicity> participantMultiplicityChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Participant.class, BPMN_ELEMENT_PARTICIPANT)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(ParticipantImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    processRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_PROCESS_REF)
      .qNameAttributeReference(Process.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    interfaceRefCollection = sequenceBuilder.elementCollection(InterfaceRef.class)
      .qNameElementReferenceCollection(Interface.class)
      .build();

    endPointRefCollection = sequenceBuilder.elementCollection(EndPointRef.class)
      .qNameElementReferenceCollection(EndPoint.class)
      .build();

    participantMultiplicityChild = sequenceBuilder.element(ParticipantMultiplicity.class)
      .build();

    typeBuilder.build();
  }

  public ParticipantImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Process getProcess() {
    return processRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setProcess(Process process) {
    processRefAttribute.setReferenceTargetElement(this, process);
  }

  @Override
  public Collection<Interface> getInterfaces() {
    return interfaceRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<EndPoint> getEndPoints() {
    return endPointRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public ParticipantMultiplicity getParticipantMultiplicity() {
    return participantMultiplicityChild.getChild(this);
  }

  @Override
  public void setParticipantMultiplicity(ParticipantMultiplicity participantMultiplicity) {
    participantMultiplicityChild.setChild(this, participantMultiplicity);
  }
}
