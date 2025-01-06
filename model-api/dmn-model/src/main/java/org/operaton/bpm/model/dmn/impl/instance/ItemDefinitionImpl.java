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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ATTRIBUTE_IS_COLLECTION;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ATTRIBUTE_TYPE_LANGUAGE;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_ITEM_DEFINITION;

import java.util.Collection;

import org.operaton.bpm.model.dmn.instance.AllowedValues;
import org.operaton.bpm.model.dmn.instance.ItemComponent;
import org.operaton.bpm.model.dmn.instance.ItemDefinition;
import org.operaton.bpm.model.dmn.instance.NamedElement;
import org.operaton.bpm.model.dmn.instance.TypeRef;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

public class ItemDefinitionImpl extends NamedElementImpl implements ItemDefinition {

  protected static Attribute<String> typeLanguageAttribute;
  protected static Attribute<Boolean> isCollectionAttribute;

  protected static ChildElement<TypeRef> typeRefChild;
  protected static ChildElement<AllowedValues> allowedValuesChild;
  protected static ChildElementCollection<ItemComponent> itemComponentCollection;

  public ItemDefinitionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getTypeLanguage() {
    return typeLanguageAttribute.getValue(this);
  }

  @Override
  public void setTypeLanguage(String typeLanguage) {
    typeLanguageAttribute.setValue(this, typeLanguage);
  }

  @Override
  public boolean isCollection() {
    return isCollectionAttribute.getValue(this);
  }

  @Override
  public void setCollection(boolean isCollection) {
    isCollectionAttribute.setValue(this, isCollection);
  }

  @Override
  public TypeRef getTypeRef() {
    return typeRefChild.getChild(this);
  }

  @Override
  public void setTypeRef(TypeRef typeRef) {
    typeRefChild.setChild(this, typeRef);
  }

  @Override
  public AllowedValues getAllowedValues() {
    return allowedValuesChild.getChild(this);
  }

  @Override
  public void setAllowedValues(AllowedValues allowedValues) {
    allowedValuesChild.setChild(this, allowedValues);
  }

  @Override
  public Collection<ItemComponent> getItemComponents() {
    return itemComponentCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ItemDefinition.class, DMN_ELEMENT_ITEM_DEFINITION)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(NamedElement.class)
      .instanceProvider(ItemDefinitionImpl::new);

    typeLanguageAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_TYPE_LANGUAGE)
      .build();

    isCollectionAttribute = typeBuilder.booleanAttribute(DMN_ATTRIBUTE_IS_COLLECTION)
      .defaultValue(false)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    typeRefChild = sequenceBuilder.element(TypeRef.class)
      .build();

    allowedValuesChild = sequenceBuilder.element(AllowedValues.class)
      .build();

    itemComponentCollection = sequenceBuilder.elementCollection(ItemComponent.class)
      .build();

    typeBuilder.build();
  }

}
