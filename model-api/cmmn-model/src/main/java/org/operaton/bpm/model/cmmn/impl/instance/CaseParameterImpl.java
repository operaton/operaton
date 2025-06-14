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
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_BINDING_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_CASE_PARAMETER;

import org.operaton.bpm.model.cmmn.instance.BindingRefinementExpression;
import org.operaton.bpm.model.cmmn.instance.CaseFileItem;
import org.operaton.bpm.model.cmmn.instance.CaseParameter;
import org.operaton.bpm.model.cmmn.instance.Parameter;
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
public class CaseParameterImpl extends ParameterImpl implements CaseParameter {

  protected static AttributeReference<CaseFileItem> bindingRefAttribute;
  protected static ChildElement<BindingRefinementExpression> bindingRefinementChild;

  public CaseParameterImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public CaseFileItem getBinding() {
    return bindingRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setBinding(CaseFileItem bindingRef) {
    bindingRefAttribute.setReferenceTargetElement(this, bindingRef);
  }

  @Override
  public BindingRefinementExpression getBindingRefinement() {
    return bindingRefinementChild.getChild(this);
  }

  @Override
  public void setBindingRefinement(BindingRefinementExpression bindingRefinement) {
    bindingRefinementChild.setChild(this, bindingRefinement);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CaseParameter.class, CMMN_ELEMENT_CASE_PARAMETER)
        .namespaceUri(CMMN11_NS)
        .extendsType(Parameter.class)
        .instanceProvider(CaseParameterImpl::new);

    bindingRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_BINDING_REF)
         .idAttributeReference(CaseFileItem.class)
         .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bindingRefinementChild = sequenceBuilder.element(BindingRefinementExpression.class)
        .build();

    typeBuilder.build();
  }


}
