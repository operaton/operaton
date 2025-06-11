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

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PLAN_FRAGMENT;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.PlanFragment;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class PlanFragmentImpl extends PlanItemDefinitionImpl implements PlanFragment {

  protected static ChildElementCollection<PlanItem> planItemCollection;
  protected static ChildElementCollection<Sentry> sentryCollection;

  public PlanFragmentImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<PlanItem> getPlanItems() {
    return planItemCollection.get(this);
  }

  @Override
  public Collection<Sentry> getSentrys() {
    return sentryCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(PlanFragment.class, CMMN_ELEMENT_PLAN_FRAGMENT)
        .namespaceUri(CMMN11_NS)
        .extendsType(PlanItemDefinition.class)
        .instanceProvider(PlanFragmentImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    planItemCollection = sequenceBuilder.elementCollection(PlanItem.class)
        .build();

    sentryCollection = sequenceBuilder.elementCollection(Sentry.class)
        .build();

    typeBuilder.build();
  }

}
