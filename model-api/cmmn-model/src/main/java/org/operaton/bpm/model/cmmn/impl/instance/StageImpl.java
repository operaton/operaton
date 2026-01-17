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

import org.operaton.bpm.model.cmmn.instance.ExitCriterion;
import org.operaton.bpm.model.cmmn.instance.PlanFragment;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.PlanningTable;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.cmmn.instance.Stage;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReferenceCollection;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN10_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_AUTO_COMPLETE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_EXIT_CRITERIA_REFS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_STAGE;

/**
 * @author Roman Smirnov
 *
 */
public class StageImpl extends PlanFragmentImpl implements Stage {

  protected static Attribute<Boolean> autoCompleteAttribute;
  protected static ChildElement<PlanningTable> planningTableChild;
  protected static ChildElementCollection<PlanItemDefinition> planItemDefinitionCollection;

  // cmmn 1.0
  /**
   * @deprecated cmmn 1.0 is deprecated
   */
  @Deprecated(since = "1.0")
  protected static AttributeReferenceCollection<Sentry> exitCriteriaRefCollection;

  // cmmn 1.1
  protected static ChildElementCollection<ExitCriterion> exitCriterionCollection;

  public StageImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public boolean isAutoComplete() {
    return autoCompleteAttribute.getValue(this);
  }

  @Override
  public void setAutoComplete(boolean autoComplete) {
    autoCompleteAttribute.setValue(this, autoComplete);
  }

  @Override
  public Collection<Sentry> getExitCriterias() {
    return exitCriteriaRefCollection.getReferenceTargetElements(this);
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
  public Collection<ExitCriterion> getExitCriterions() {
    return exitCriterionCollection.get(this);
  }

  @Override
  public PlanningTable getPlanningTable() {
    return planningTableChild.getChild(this);
  }

  @Override
  public void setPlanningTable(PlanningTable planningTable) {
    planningTableChild.setChild(this, planningTable);
  }

  @Override
  public Collection<PlanItemDefinition> getPlanItemDefinitions() {
    return planItemDefinitionCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Stage.class, CMMN_ELEMENT_STAGE)
        .namespaceUri(CMMN11_NS)
        .extendsType(PlanFragment.class)
        .instanceProvider(StageImpl::new);

    autoCompleteAttribute = typeBuilder.booleanAttribute(CMMN_ATTRIBUTE_AUTO_COMPLETE)
        .defaultValue(false)
        .build();

    exitCriteriaRefCollection = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_EXIT_CRITERIA_REFS)
        .namespace(CMMN10_NS)
        .idAttributeReferenceCollection(Sentry.class, CmmnAttributeElementReferenceCollection.class)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    planningTableChild = sequenceBuilder.element(PlanningTable.class)
        .build();

    planItemDefinitionCollection = sequenceBuilder.elementCollection(PlanItemDefinition.class)
        .build();

    exitCriterionCollection = sequenceBuilder.elementCollection(ExitCriterion.class)
        .build();

    typeBuilder.build();
  }

}
