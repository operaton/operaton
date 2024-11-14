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

import org.operaton.bpm.model.bpmn.instance.Error;
import org.operaton.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.operaton.bpm.model.bpmn.instance.EventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ERROR_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ERROR_EVENT_DEFINITION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_ERROR_CODE_VARIABLE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_ERROR_MESSAGE_VARIABLE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN errorEventDefinition element
 *
 * @author Sebastian Menski
 */
public class ErrorEventDefinitionImpl extends EventDefinitionImpl implements ErrorEventDefinition {

  protected static AttributeReference<Error> errorRefAttribute;

  protected static Attribute<String> operatonErrorCodeVariableAttribute;

  protected static Attribute<String> operatonErrorMessageVariableAttribute;
  
  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ErrorEventDefinition.class, BPMN_ELEMENT_ERROR_EVENT_DEFINITION)
      .namespaceUri(BPMN20_NS)
      .extendsType(EventDefinition.class)
      .instanceProvider(new ModelTypeInstanceProvider<ErrorEventDefinition>() {
        public ErrorEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
          return new ErrorEventDefinitionImpl(instanceContext);
        }
      });

    errorRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ERROR_REF)
      .qNameAttributeReference(Error.class)
      .build();
    
    operatonErrorCodeVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ERROR_CODE_VARIABLE)
        .namespace(OPERATON_NS)
        .build();

    operatonErrorMessageVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ERROR_MESSAGE_VARIABLE)
      .namespace(OPERATON_NS)
      .build();
    
    typeBuilder.build();
  }

  public ErrorEventDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  public Error getError() {
    return errorRefAttribute.getReferenceTargetElement(this);
  }

  public void setError(Error error) {
    errorRefAttribute.setReferenceTargetElement(this, error);
  }

  @Override
  public void setOperatonErrorCodeVariable(String operatonErrorCodeVariable) {
    operatonErrorCodeVariableAttribute.setValue(this, operatonErrorCodeVariable);
  }

  @Override
  public String getOperatonErrorCodeVariable() {
    return operatonErrorCodeVariableAttribute.getValue(this);
  }

  @Override
  public void setOperatonErrorMessageVariable(String operatonErrorMessageVariable) {
    operatonErrorMessageVariableAttribute.setValue(this, operatonErrorMessageVariable);
  }

  @Override
  public String getOperatonErrorMessageVariable() {
    return operatonErrorMessageVariableAttribute.getValue(this);
  }
}
