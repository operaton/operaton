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
package org.operaton.bpm.model.cmmn.impl.instance;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.CaseFileItemDefinition;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.Property;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_DEFINITION_TYPE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_STRUCTURE_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_CASE_FILE_ITEM_DEFINITION;

/**
 * @author Roman Smirnov
 *
 */
public class CaseFileItemDefinitionImpl extends CmmnElementImpl implements CaseFileItemDefinition {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> definitionTypeAttribute;
  // structureRef should be a QName, but it is not clear
  // what kind of element the attribute value should reference,
  // that's why we use a simple String
  protected static Attribute<String> structureAttribute;

  protected static ChildElementCollection<Property> propertyCollection;

  public CaseFileItemDefinitionImpl(ModelTypeInstanceContext instanceContext) {
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
  public String getDefinitionType() {
    return definitionTypeAttribute.getValue(this);
  }

  @Override
  public void setDefinitionType(String definitionType) {
    definitionTypeAttribute.setValue(this, definitionType);
  }

  @Override
  public String getStructure() {
    return structureAttribute.getValue(this);
  }

  @Override
  public void setStructure(String structureRef) {
    structureAttribute.setValue(this, structureRef);
  }

  @Override
  public Collection<Property> getProperties() {
    return propertyCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CaseFileItemDefinition.class, CMMN_ELEMENT_CASE_FILE_ITEM_DEFINITION)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(CaseFileItemDefinitionImpl::new);

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    definitionTypeAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_DEFINITION_TYPE)
        .defaultValue("http://www.omg.org/spec/CMMN/DefinitionType/Unspecified")
        .build();

    structureAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_STRUCTURE_REF)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    propertyCollection = sequenceBuilder.elementCollection(Property.class)
        .build();

    typeBuilder.build();
  }

}
