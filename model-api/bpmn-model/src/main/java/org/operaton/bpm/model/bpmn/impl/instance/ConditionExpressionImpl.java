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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.instance.ConditionExpression;
import org.operaton.bpm.model.bpmn.instance.FormalExpression;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONDITION_EXPRESSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_RESOURCE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.XSI_ATTRIBUTE_TYPE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.XSI_NS;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN conditionExpression element of the BPMN tSequenceFlow type
 *
 * @author Sebastian Menski
 */
public class ConditionExpressionImpl extends FormalExpressionImpl implements ConditionExpression {

  protected static Attribute<String> typeAttribute;
  protected static Attribute<String> operatonResourceAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ConditionExpression.class, BPMN_ELEMENT_CONDITION_EXPRESSION)
      .namespaceUri(BPMN20_NS)
      .extendsType(FormalExpression.class)
      .instanceProvider(new ModelTypeInstanceProvider<ConditionExpression>() {
        public ConditionExpression newInstance(ModelTypeInstanceContext instanceContext) {
          return new ConditionExpressionImpl(instanceContext);
        }
      });

    typeAttribute = typeBuilder.stringAttribute(XSI_ATTRIBUTE_TYPE)
      .namespace(XSI_NS)
      .defaultValue("tFormalExpression")
      .build();

    operatonResourceAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESOURCE)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public ConditionExpressionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public String getType() {
    return typeAttribute.getValue(this);
  }

  public void setType(String type) {
    typeAttribute.setValue(this, type);
  }

  public String getOperatonResource() {
    return operatonResourceAttribute.getValue(this);
  }

  public void setOperatonResource(String operatonResource) {
    operatonResourceAttribute.setValue(this, operatonResource);
  }

}
