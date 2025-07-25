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

import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN ioBinding element
 *
 * @author Sebastian Menski
 */
public class IoBindingImpl extends BaseElementImpl implements IoBinding {

  protected static AttributeReference<Operation> operationRefAttribute;
  protected static AttributeReference<DataInput> inputDataRefAttribute;
  protected static AttributeReference<DataOutput> outputDataRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IoBinding.class, BPMN_ELEMENT_IO_BINDING)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(IoBindingImpl::new);

    operationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
      .required()
      .qNameAttributeReference(Operation.class)
      .build();

    inputDataRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_INPUT_DATA_REF)
      .required()
      .idAttributeReference(DataInput.class)
      .build();

    outputDataRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OUTPUT_DATA_REF)
      .required()
      .idAttributeReference(DataOutput.class)
      .build();

    typeBuilder.build();
  }

  public IoBindingImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Operation getOperation() {
    return operationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOperation(Operation operation) {
    operationRefAttribute.setReferenceTargetElement(this, operation);
  }

  @Override
  public DataInput getInputData() {
    return inputDataRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setInputData(DataInput inputData) {
    inputDataRefAttribute.setReferenceTargetElement(this, inputData);
  }

  @Override
  public DataOutput getOutputData() {
    return outputDataRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOutputData(DataOutput dataOutput) {
    outputDataRefAttribute.setReferenceTargetElement(this, dataOutput);
  }
}
