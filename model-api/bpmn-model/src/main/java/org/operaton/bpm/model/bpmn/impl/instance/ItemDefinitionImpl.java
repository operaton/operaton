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

import org.operaton.bpm.model.bpmn.ItemKind;
import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * @author Sebastian Menski
 */
public class ItemDefinitionImpl extends RootElementImpl implements ItemDefinition {

  protected static Attribute<String> structureRefAttribute;
  protected static Attribute<Boolean> isCollectionAttribute;
  protected static Attribute<ItemKind> itemKindAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ItemDefinition.class,BpmnModelConstants.BPMN_ELEMENT_ITEM_DEFINITION)
      .namespaceUri(BpmnModelConstants.BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(ItemDefinitionImpl::new);

    structureRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_STRUCTURE_REF)
      .build();

    isCollectionAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_COLLECTION)
      .defaultValue(false)
      .build();

    itemKindAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_ITEM_KIND, ItemKind.class)
      .defaultValue(ItemKind.Information)
      .build();

    typeBuilder.build();
  }

  public ItemDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public String getStructureRef() {
    return structureRefAttribute.getValue(this);
  }

  @Override
  public void setStructureRef(String structureRef) {
    structureRefAttribute.setValue(this, structureRef);
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
  public ItemKind getItemKind() {
    return itemKindAttribute.getValue(this);
  }

  @Override
  public void setItemKind(ItemKind itemKind) {
    itemKindAttribute.setValue(this, itemKind);
  }
}
