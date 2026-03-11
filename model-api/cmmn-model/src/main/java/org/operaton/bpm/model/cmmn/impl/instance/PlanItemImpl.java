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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.EntryCriterion;
import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.ItemControl;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;
import org.operaton.bpm.model.xml.type.reference.AttributeReferenceCollection;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN10_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_DEFINITION_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_ENTRY_CRITERIA_REFS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_EXIT_CRITERIA_REFS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PLAN_ITEM;

/**
 * @author Roman Smirnov
 *
 */
@SuppressWarnings("deprecation")
public class PlanItemImpl extends CmmnElementImpl implements PlanItem {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<PlanItemDefinition> planItemDefinitionRefAttribute;
  protected static ChildElement<ItemControl> itemControlChild;

  // cmmn 1.0
  /**
   * @deprecated CMMN 1.1 entryCriteriaRefCollection is replaced by entryCriterionCollection
   */
  @Deprecated(since = "1.0")
  protected static AttributeReferenceCollection<Sentry> entryCriteriaRefCollection;
  /**
   * @deprecated CMMN 1.1 exitCriteriaRefCollection is replaced by exitCriterionCollection
   */
  @Deprecated(since = "1.0")
  protected static AttributeReferenceCollection<Sentry> exitCriteriaRefCollection;

  // cmmn 1.1
  protected static ChildElementCollection<EntryCriterion> entryCriterionCollection;
  protected static ChildElementCollection<ExitCriterion> exitCriterionCollection;

  public PlanItemImpl(ModelTypeInstanceContext instanceContext) {
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
    return planItemDefinitionRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setDefinition(PlanItemDefinition definition) {
    planItemDefinitionRefAttribute.setReferenceTargetElement(this, definition);
  }

  @Override
  public Collection<Sentry> getEntryCriterias() {
    return entryCriteriaRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<Sentry> getExitCriterias() {
    return exitCriteriaRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<Sentry> getEntryCriteria() {
    if (!isCmmn11()) {
      return Collections.unmodifiableCollection(getEntryCriterias());
    }
    else {
      List<Sentry> sentries = new ArrayList<>();
      Collection<EntryCriterion> entryCriterions = getEntryCriterions();
      for (EntryCriterion entryCriterion : entryCriterions) {
        Sentry sentry = entryCriterion.getSentry();
        if (sentry != null) {
          sentries.add(sentry);
        }
      }
      return Collections.unmodifiableCollection(sentries);
    }
  }

  @Override
  public Collection<Sentry> getExitCriteria() {
    if (!isCmmn11()) {
      return Collections.unmodifiableCollection(getExitCriterias());
    }
    else {
      List<Sentry> sentries = new ArrayList<>();
      Collection<ExitCriterion> exitCriterions = getExitCriterions();
      for (ExitCriterion exitCriterion : exitCriterions) {
        Sentry sentry = exitCriterion.getSentry();
        if (sentry != null) {
          sentries.add(sentry);
        }
      }
      return Collections.unmodifiableCollection(sentries);
    }
  }

  @Override
  public Collection<EntryCriterion> getEntryCriterions() {
    return entryCriterionCollection.get(this);
  }

  @Override
  public Collection<ExitCriterion> getExitCriterions() {
    return exitCriterionCollection.get(this);
  }

  @Override
  public ItemControl getItemControl() {
    return itemControlChild.getChild(this);
  }

  @Override
  public void setItemControl(ItemControl itemControl) {
    itemControlChild.setChild(this, itemControl);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(PlanItem.class, CMMN_ELEMENT_PLAN_ITEM)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(PlanItemImpl::new);

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    planItemDefinitionRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_DEFINITION_REF)
        .idAttributeReference(PlanItemDefinition.class)
        .build();

    entryCriteriaRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_ENTRY_CRITERIA_REFS)
        .namespace(CMMN10_NS)
        .idAttributeReferenceCollection(Sentry.class, CmmnAttributeElementReferenceCollection.class)
        .build();

    exitCriteriaRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_EXIT_CRITERIA_REFS)
        .namespace(CMMN10_NS)
        .idAttributeReferenceCollection(Sentry.class, CmmnAttributeElementReferenceCollection.class)
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
