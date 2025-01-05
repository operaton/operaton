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
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_CONTEXT_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_IF_PART;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.CaseFileItem;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.ConditionExpression;
import org.operaton.bpm.model.cmmn.instance.IfPart;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Roman Smirnov
 *
 */
public class IfPartImpl extends CmmnElementImpl implements IfPart {

  protected static AttributeReference<CaseFileItem> contextRefAttribute;

  // cmmn 1.1
  protected static ChildElement<ConditionExpression> conditionChild;

  public IfPartImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public CaseFileItem getContext() {
    return contextRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setContext(CaseFileItem caseFileItem) {
    contextRefAttribute.setReferenceTargetElement(this, caseFileItem);
  }

  @Override
  public Collection<ConditionExpression> getConditions() {
    return conditionChild.get(this);
  }

  @Override
  public ConditionExpression getCondition() {
    return conditionChild.getChild(this);
  }

  @Override
  public void setCondition(ConditionExpression condition) {
    conditionChild.setChild(this, condition);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IfPart.class, CMMN_ELEMENT_IF_PART)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(instanceContext -> new IfPartImpl(instanceContext));

    contextRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_CONTEXT_REF)
        .idAttributeReference(CaseFileItem.class)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    conditionChild = sequenceBuilder.element(ConditionExpression.class)
        .build();

    typeBuilder.build();
  }

}
