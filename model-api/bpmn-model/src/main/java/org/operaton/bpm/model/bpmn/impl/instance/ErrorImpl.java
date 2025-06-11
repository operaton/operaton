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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.instance.Error;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * @author Sebastian Menski
 */
public class ErrorImpl extends RootElementImpl implements Error {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> errorCodeAttribute;
  protected static Attribute<String> operatonErrorMessageAttribute;

  protected static AttributeReference<ItemDefinition> structureRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Error.class, BPMN_ELEMENT_ERROR)
      .namespaceUri(BpmnModelConstants.BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(ErrorImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    errorCodeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ERROR_CODE)
      .build();

    operatonErrorMessageAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ERROR_MESSAGE).namespace(OPERATON_NS)
        .build();

    structureRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_STRUCTURE_REF)
      .qNameAttributeReference(ItemDefinition.class)
      .build();

    typeBuilder.build();
  }

  public ErrorImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public String getErrorCode() {
    return errorCodeAttribute.getValue(this);
  }

  @Override
  public void setErrorCode(String errorCode) {
    errorCodeAttribute.setValue(this, errorCode);
  }

  @Override
  public String getOperatonErrorMessage() {
    return operatonErrorMessageAttribute.getValue(this);
  }

  @Override
  public void setOperatonErrorMessage(String operatonErrorMessage) {
    operatonErrorMessageAttribute.setValue(this, operatonErrorMessage);
  }

  @Override
  public ItemDefinition getStructure() {
    return structureRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setStructure(ItemDefinition structure) {
    structureRefAttribute.setReferenceTargetElement(this, structure);
  }
}
