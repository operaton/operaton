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

import java.util.Collection;

import org.operaton.bpm.model.bpmn.instance.Documentation;
import org.operaton.bpm.model.bpmn.instance.Extension;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN extension element
 *
 * @author Sebastian Menski
 */
public class ExtensionImpl extends BpmnModelElementInstanceImpl implements Extension {

  protected static Attribute<String> definitionAttribute;
  protected static Attribute<Boolean> mustUnderstandAttribute;
  protected static ChildElementCollection<Documentation> documentationCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Extension.class, BPMN_ELEMENT_EXTENSION)
      .namespaceUri(BPMN20_NS)
      .instanceProvider(ExtensionImpl::new);

    definitionAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DEFINITION)
      .build();

    mustUnderstandAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_MUST_UNDERSTAND)
      .defaultValue(false)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    documentationCollection = sequenceBuilder.elementCollection(Documentation.class)
      .build();

    typeBuilder.build();
  }

  public ExtensionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getDefinition() {
    return definitionAttribute.getValue(this);
  }

  @Override
  public void setDefinition(String definition) {
    definitionAttribute.setValue(this, definition);
  }

  @Override
  public boolean mustUnderstand() {
    return mustUnderstandAttribute.getValue(this);
  }

  @Override
  public void setMustUnderstand(boolean mustUnderstand) {
    mustUnderstandAttribute.setValue(this, mustUnderstand);
  }

  @Override
  public Collection<Documentation> getDocumentations() {
    return documentationCollection.get(this);
  }
}
