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

import java.util.Collection;

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.DataOutput;
import org.operaton.bpm.model.bpmn.instance.InputSet;
import org.operaton.bpm.model.bpmn.instance.OutputSet;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN outputSet element
 *
 * @author Sebastian Menski
 */
public class OutputSetImpl extends BaseElementImpl implements OutputSet {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<DataOutput, DataOutputRefs> dataOutputRefsCollection;
  protected static ElementReferenceCollection<DataOutput, OptionalOutputRefs> optionalOutputRefsCollection;
  protected static ElementReferenceCollection<DataOutput, WhileExecutingOutputRefs> whileExecutingOutputRefsCollection;
  protected static ElementReferenceCollection<InputSet, InputSetRefs>  inputSetInputSetRefsCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OutputSet.class, BPMN_ELEMENT_OUTPUT_SET)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(OutputSetImpl::new);

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataOutputRefsCollection = sequenceBuilder.elementCollection(DataOutputRefs.class)
      .idElementReferenceCollection(DataOutput.class)
      .build();

    optionalOutputRefsCollection = sequenceBuilder.elementCollection(OptionalOutputRefs.class)
      .idElementReferenceCollection(DataOutput.class)
      .build();

    whileExecutingOutputRefsCollection = sequenceBuilder.elementCollection(WhileExecutingOutputRefs.class)
      .idElementReferenceCollection(DataOutput.class)
      .build();

    inputSetInputSetRefsCollection = sequenceBuilder.elementCollection(InputSetRefs.class)
      .idElementReferenceCollection(InputSet.class)
      .build();

    typeBuilder.build();
  }

  public OutputSetImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
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
  public Collection<DataOutput> getDataOutputRefs() {
    return dataOutputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<DataOutput> getOptionalOutputRefs() {
    return optionalOutputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<DataOutput> getWhileExecutingOutputRefs() {
    return whileExecutingOutputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<InputSet> getInputSetRefs() {
    return inputSetInputSetRefsCollection.getReferenceTargetElements(this);
  }
}
