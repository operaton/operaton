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

import org.operaton.bpm.model.bpmn.instance.EventDefinition;
import org.operaton.bpm.model.bpmn.instance.LinkEventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReference;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN linkEventDefinition element
 *
 * @author Sebastian Menski
 */
public class LinkEventDefinitionImpl extends EventDefinitionImpl implements LinkEventDefinition {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<LinkEventDefinition, Source> sourceCollection;
  protected static ElementReference<LinkEventDefinition, Target> targetChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(LinkEventDefinition.class, BPMN_ELEMENT_LINK_EVENT_DEFINITION)
      .namespaceUri(BPMN20_NS)
      .extendsType(EventDefinition.class)
      .instanceProvider(LinkEventDefinitionImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .required()
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    sourceCollection = sequenceBuilder.elementCollection(Source.class)
      .qNameElementReferenceCollection(LinkEventDefinition.class)
      .build();

    targetChild = sequenceBuilder.element(Target.class)
      .qNameElementReference(LinkEventDefinition.class)
      .build();

    typeBuilder.build();
  }

  public LinkEventDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
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
  public Collection<LinkEventDefinition> getSources() {
    return sourceCollection.getReferenceTargetElements(this);
  }

  @Override
  public LinkEventDefinition getTarget() {
    return targetChild.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(LinkEventDefinition target) {
    targetChild.setReferenceTargetElement(this, target);
  }

}
