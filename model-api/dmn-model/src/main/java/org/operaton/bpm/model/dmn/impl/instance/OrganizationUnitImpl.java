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
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_ORGANIZATION_UNIT;

import java.util.Collection;

import org.operaton.bpm.model.dmn.instance.BusinessContextElement;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.DecisionMadeReference;
import org.operaton.bpm.model.dmn.instance.DecisionOwnedReference;
import org.operaton.bpm.model.dmn.instance.OrganizationUnit;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

public class OrganizationUnitImpl extends BusinessContextElementImpl implements OrganizationUnit {

  protected static ElementReferenceCollection<Decision, DecisionMadeReference> decisionDecisionMadeRefCollection;
  protected static ElementReferenceCollection<Decision, DecisionOwnedReference> decisionDecisionOwnedRefCollection;

  public OrganizationUnitImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<Decision> getDecisionsMade() {
    return decisionDecisionMadeRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<Decision> getDecisionsOwned() {
    return decisionDecisionOwnedRefCollection.getReferenceTargetElements(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OrganizationUnit.class, DMN_ELEMENT_ORGANIZATION_UNIT)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(BusinessContextElement.class)
      .instanceProvider(OrganizationUnitImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    decisionDecisionMadeRefCollection = sequenceBuilder.elementCollection(DecisionMadeReference.class)
      .uriElementReferenceCollection(Decision.class)
      .build();

    decisionDecisionOwnedRefCollection = sequenceBuilder.elementCollection(DecisionOwnedReference.class)
      .uriElementReferenceCollection(Decision.class)
      .build();

    typeBuilder.build();
  }

}
