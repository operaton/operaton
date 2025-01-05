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

import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_IO_SPECIFICATION;

/**
 * The BPMN IoSpecification element
 *
 * @author Sebastian Menski
 */
public class IoSpecificationImpl extends BaseElementImpl implements IoSpecification {

  protected static ChildElementCollection<DataInput> dataInputCollection;
  protected static ChildElementCollection<DataOutput> dataOutputCollection;
  protected static ChildElementCollection<InputSet> inputSetCollection;
  protected static ChildElementCollection<OutputSet> outputSetCollection;
  
  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(IoSpecification.class, BPMN_ELEMENT_IO_SPECIFICATION)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(instanceContext -> new IoSpecificationImpl(instanceContext));

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataInputCollection = sequenceBuilder.elementCollection(DataInput.class)
      .build();

    dataOutputCollection = sequenceBuilder.elementCollection(DataOutput.class)
      .build();

    inputSetCollection = sequenceBuilder.elementCollection(InputSet.class)
      .required()
      .build();

    outputSetCollection = sequenceBuilder.elementCollection(OutputSet.class)
      .required()
      .build();

    typeBuilder.build();
  }

  public IoSpecificationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<DataInput> getDataInputs() {
    return dataInputCollection.get(this);
  }

  @Override
  public Collection<DataOutput> getDataOutputs() {
    return dataOutputCollection.get(this);
  }

  @Override
  public Collection<InputSet> getInputSets() {
    return inputSetCollection.get(this);
  }

  @Override
  public Collection<OutputSet> getOutputSets() {
    return outputSetCollection.get(this);
  }
}
