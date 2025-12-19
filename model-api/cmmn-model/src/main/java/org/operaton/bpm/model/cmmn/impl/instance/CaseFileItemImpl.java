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

import org.operaton.bpm.model.cmmn.MultiplicityEnum;
import org.operaton.bpm.model.cmmn.instance.CaseFileItem;
import org.operaton.bpm.model.cmmn.instance.CaseFileItemDefinition;
import org.operaton.bpm.model.cmmn.instance.Children;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;
import org.operaton.bpm.model.xml.type.reference.AttributeReferenceCollection;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.*;

/**
 * @author Roman Smirnov
 *
 */
public class CaseFileItemImpl extends CmmnElementImpl implements CaseFileItem {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<MultiplicityEnum> multiplicityAttribute;
  protected static AttributeReference<CaseFileItemDefinition> definitionRefAttribute;
  protected static AttributeReferenceCollection<CaseFileItem> targetRefCollection;
  protected static ChildElement<Children> childrenChild;

  // cmmn 1.0
  /**
   * @deprecated CMMN 1.1 sourceRef is replaced by sourceRefs
   */
  @Deprecated(since = "1.0")
  protected static AttributeReference<CaseFileItem> sourceRefAttribute;

//  cmmn 1.1
  protected static AttributeReferenceCollection<CaseFileItem> sourceRefCollection;



  public CaseFileItemImpl(ModelTypeInstanceContext instanceContext) {
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
  public MultiplicityEnum getMultiplicity() {
    return multiplicityAttribute.getValue(this);
  }

  @Override
  public void setMultiplicity(MultiplicityEnum multiplicity) {
    multiplicityAttribute.setValue(this, multiplicity);
  }

  @Override
  public CaseFileItemDefinition getDefinitionRef() {
    return definitionRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setDefinitionRef(CaseFileItemDefinition caseFileItemDefinition) {
    definitionRefAttribute.setReferenceTargetElement(this, caseFileItemDefinition);
  }

  @Override
  public Collection<CaseFileItem> getSourceRefs() {
    return sourceRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<CaseFileItem> getTargetRefs() {
    return targetRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Children getChildren() {
    return childrenChild.getChild(this);
  }

  @Override
  public void setChildren(Children children) {
    childrenChild.setChild(this, children);
  }

  @SuppressWarnings("deprecation")
  public static void registerType(ModelBuilder modelBuilder) {

    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CaseFileItem.class, CMMN_ELEMENT_CASE_FILE_ITEM)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(CaseFileItemImpl::new);

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    multiplicityAttribute = typeBuilder.enumAttribute(CMMN_ATTRIBUTE_MULTIPLICITY, MultiplicityEnum.class)
        .defaultValue(MultiplicityEnum.Unspecified)
        .build();

    definitionRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_DEFINITION_REF)
        .qNameAttributeReference(CaseFileItemDefinition.class)
        .build();

    sourceRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_SOURCE_REF)
        .namespace(CMMN10_NS)
        .idAttributeReference(CaseFileItem.class)
        .build();

    sourceRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_SOURCE_REFS)
        .idAttributeReferenceCollection(CaseFileItem.class, CmmnAttributeElementReferenceCollection.class)
        .build();

    targetRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_TARGET_REFS)
        .idAttributeReferenceCollection(CaseFileItem.class, CmmnAttributeElementReferenceCollection.class)
        .build();

    SequenceBuilder sequence = typeBuilder.sequence();

    childrenChild = sequence.element(Children.class)
      .build();

    typeBuilder.build();
  }

}
