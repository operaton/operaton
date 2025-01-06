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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_DEFINITION_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_DISCRETIONARY_ITEM;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.DiscretionaryItem;
import org.operaton.bpm.model.cmmn.instance.EntryCriterion;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.ItemControl;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.TableItem;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Roman Smirnov
 *
 */
public class DiscretionaryItemImpl extends TableItemImpl implements DiscretionaryItem {

  protected static AttributeReference<PlanItemDefinition> definitionRefAttribute;
  protected static ChildElement<ItemControl> itemControlChild;

  // cmmn 1.1
  protected static Attribute<String> nameAttribute;
  protected static ChildElementCollection<EntryCriterion> entryCriterionCollection;
  protected static ChildElementCollection<ExitCriterion> exitCriterionCollection;

  public DiscretionaryItemImpl(ModelTypeInstanceContext instanceContext) {
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
  public PlanItemDefinition getDefinition() {
    return definitionRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setDefinition(PlanItemDefinition definition) {
    definitionRefAttribute.setReferenceTargetElement(this, definition);
  }

  @Override
  public ItemControl getItemControl() {
    return itemControlChild.getChild(this);
  }

  @Override
  public void setItemControl(ItemControl itemControl) {
    itemControlChild.setChild(this, itemControl);
  }

  @Override
  public Collection<EntryCriterion> getEntryCriterions() {
    return entryCriterionCollection.get(this);
  }

  @Override
  public Collection<ExitCriterion> getExitCriterions() {
    return exitCriterionCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(DiscretionaryItem.class, CMMN_ELEMENT_DISCRETIONARY_ITEM)
        .namespaceUri(CMMN11_NS)
        .extendsType(TableItem.class)
        .instanceProvider(DiscretionaryItemImpl::new);

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    definitionRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_DEFINITION_REF)
        .idAttributeReference(PlanItemDefinition.class)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    itemControlChild = sequenceBuilder.element(ItemControl.class)
        .build();

    entryCriterionCollection = sequenceBuilder.elementCollection(EntryCriterion.class)
        .build();

    exitCriterionCollection = sequenceBuilder.elementCollection(ExitCriterion.class)
        .build();

    typeBuilder.build();
  }

}
