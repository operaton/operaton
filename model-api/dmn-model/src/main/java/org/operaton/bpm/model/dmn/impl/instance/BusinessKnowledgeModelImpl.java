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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_BUSINESS_KNOWLEDGE_MODEL;

import java.util.Collection;

import org.operaton.bpm.model.dmn.instance.AuthorityRequirement;
import org.operaton.bpm.model.dmn.instance.BusinessKnowledgeModel;
import org.operaton.bpm.model.dmn.instance.DrgElement;
import org.operaton.bpm.model.dmn.instance.EncapsulatedLogic;
import org.operaton.bpm.model.dmn.instance.KnowledgeRequirement;
import org.operaton.bpm.model.dmn.instance.Variable;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

public class BusinessKnowledgeModelImpl extends DrgElementImpl implements BusinessKnowledgeModel {

  protected static ChildElement<EncapsulatedLogic> encapsulatedLogicChild;
  protected static ChildElement<Variable> variableChild;
  protected static ChildElementCollection<KnowledgeRequirement> knowledgeRequirementCollection;
  protected static ChildElementCollection<AuthorityRequirement> authorityRequirementCollection;

  public BusinessKnowledgeModelImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public EncapsulatedLogic getEncapsulatedLogic() {
    return encapsulatedLogicChild.getChild(this);
  }

  @Override
  public void setEncapsulatedLogic(EncapsulatedLogic encapsulatedLogic) {
    encapsulatedLogicChild.setChild(this, encapsulatedLogic);
  }

  @Override
  public Variable getVariable() {
    return variableChild.getChild(this);
  }

  @Override
  public void setVariable(Variable variable) {
    variableChild.setChild(this, variable);
  }

  @Override
  public Collection<KnowledgeRequirement> getKnowledgeRequirement() {
    return knowledgeRequirementCollection.get(this);
  }

  @Override
  public Collection<AuthorityRequirement> getAuthorityRequirement() {
    return authorityRequirementCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BusinessKnowledgeModel.class, DMN_ELEMENT_BUSINESS_KNOWLEDGE_MODEL)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(DrgElement.class)
      .instanceProvider(BusinessKnowledgeModelImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    encapsulatedLogicChild = sequenceBuilder.element(EncapsulatedLogic.class)
      .build();

    variableChild = sequenceBuilder.element(Variable.class)
      .build();

    knowledgeRequirementCollection = sequenceBuilder.elementCollection(KnowledgeRequirement.class)
      .build();

    authorityRequirementCollection = sequenceBuilder.elementCollection(AuthorityRequirement.class)
      .build();

    typeBuilder.build();
  }

}
