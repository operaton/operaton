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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import org.operaton.bpm.model.bpmn.impl.instance.ErrorEventDefinitionImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonErrorEventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_EXPRESSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_ERROR_EVENT_DEFINITION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

public class OperatonErrorEventDefinitionImpl extends ErrorEventDefinitionImpl implements OperatonErrorEventDefinition {

  protected static Attribute<String> operatonExpressionAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonErrorEventDefinition.class, OPERATON_ELEMENT_ERROR_EVENT_DEFINITION)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonErrorEventDefinition>() {
      @Override
      public OperatonErrorEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonErrorEventDefinitionImpl(instanceContext);
        }
      });

    operatonExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_EXPRESSION)
        .namespace(OPERATON_NS)
        .build();

    typeBuilder.build();
  }

  public OperatonErrorEventDefinitionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonExpression() {
    return operatonExpressionAttribute.getValue(this);
  }

  @Override
  public void setOperatonExpression(String operatonExpression) {
    operatonExpressionAttribute.setValue(this, operatonExpression);
  }
}
