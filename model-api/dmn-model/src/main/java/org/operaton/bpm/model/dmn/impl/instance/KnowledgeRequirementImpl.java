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
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_KNOWLEDGE_REQUIREMENT;

import org.operaton.bpm.model.dmn.instance.BusinessKnowledgeModel;
import org.operaton.bpm.model.dmn.instance.KnowledgeRequirement;
import org.operaton.bpm.model.dmn.instance.RequiredKnowledgeReference;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReference;

public class KnowledgeRequirementImpl extends DmnModelElementInstanceImpl implements KnowledgeRequirement {

  protected static ElementReference<BusinessKnowledgeModel, RequiredKnowledgeReference> requiredKnowledgeRef;

  public KnowledgeRequirementImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public BusinessKnowledgeModel getRequiredKnowledge() {
    return requiredKnowledgeRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredKnowledge(BusinessKnowledgeModel requiredKnowledge) {
    requiredKnowledgeRef.setReferenceTargetElement(this, requiredKnowledge);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(KnowledgeRequirement.class, DMN_ELEMENT_KNOWLEDGE_REQUIREMENT)
      .namespaceUri(LATEST_DMN_NS)
      .instanceProvider(new ModelTypeInstanceProvider<KnowledgeRequirement>() {
      @Override
      public KnowledgeRequirement newInstance(ModelTypeInstanceContext instanceContext) {
          return new KnowledgeRequirementImpl(instanceContext);
        }
      });

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    requiredKnowledgeRef = sequenceBuilder.element(RequiredKnowledgeReference.class)
      .required()
      .uriElementReference(BusinessKnowledgeModel.class)
      .build();

    typeBuilder.build();
  }

}
