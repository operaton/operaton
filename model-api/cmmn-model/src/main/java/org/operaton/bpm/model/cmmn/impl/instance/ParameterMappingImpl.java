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
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_SOURCE_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_TARGET_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PARAMETER_MAPPING;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.Parameter;
import org.operaton.bpm.model.cmmn.instance.ParameterMapping;
import org.operaton.bpm.model.cmmn.instance.TransformationExpression;
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
public class ParameterMappingImpl extends CmmnElementImpl implements ParameterMapping {

  protected static AttributeReference<Parameter> sourceRefAttribute;
  protected static AttributeReference<Parameter> targetRefAttribute;

  protected static ChildElement<TransformationExpression> transformationChild;

  public ParameterMappingImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Parameter getSource() {
    return sourceRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(Parameter parameter) {
    sourceRefAttribute.setReferenceTargetElement(this, parameter);
  }

  @Override
  public Parameter getTarget() {
    return targetRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(Parameter parameter) {
    targetRefAttribute.setReferenceTargetElement(this, parameter);
  }

  @Override
  public TransformationExpression getTransformation() {
    return transformationChild.getChild(this);
  }

  @Override
  public void setTransformation(TransformationExpression transformationExpression) {
    transformationChild.setChild(this, transformationExpression);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ParameterMapping.class, CMMN_ELEMENT_PARAMETER_MAPPING)
        .extendsType(CmmnElement.class)
        .namespaceUri(CMMN11_NS)
        .instanceProvider(ParameterMappingImpl::new);

    sourceRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_SOURCE_REF)
        .idAttributeReference(Parameter.class)
        .build();

    targetRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_TARGET_REF)
        .idAttributeReference(Parameter.class)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    transformationChild = sequenceBuilder.element(TransformationExpression.class)
        .build();

    typeBuilder.build();
  }

}
