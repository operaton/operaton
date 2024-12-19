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
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_IMPLEMENTATION_TYPE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PROCESS;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.InputProcessParameter;
import org.operaton.bpm.model.cmmn.instance.OutputProcessParameter;
import org.operaton.bpm.model.cmmn.instance.Process;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessImpl extends CmmnElementImpl implements Process {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> implementationTypeAttribute;

  protected static ChildElementCollection<InputProcessParameter> inputCollection;
  protected static ChildElementCollection<OutputProcessParameter> outputCollection;

  public ProcessImpl(ModelTypeInstanceContext instanceContext) {
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
  public String getImplementationType() {
    return implementationTypeAttribute.getValue(this);
  }

  @Override
  public void setImplementationType(String implementationType) {
    implementationTypeAttribute.setValue(this, implementationType);
  }

  @Override
  public Collection<InputProcessParameter> getInputs() {
    return inputCollection.get(this);
  }

  @Override
  public Collection<OutputProcessParameter> getOutputs() {
    return outputCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Process.class, CMMN_ELEMENT_PROCESS)
        .extendsType(CmmnElement.class)
        .namespaceUri(CMMN11_NS)
        .instanceProvider(new ModelTypeInstanceProvider<Process>() {
      @Override
      public Process newInstance(ModelTypeInstanceContext instanceContext) {
            return new ProcessImpl(instanceContext);
          }
        });

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    implementationTypeAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_IMPLEMENTATION_TYPE)
        .defaultValue("http://www.omg.org/spec/CMMN/ProcessType/Unspecified")
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inputCollection = sequenceBuilder.elementCollection(InputProcessParameter.class)
        .build();

    outputCollection = sequenceBuilder.elementCollection(OutputProcessParameter.class)
        .build();

    typeBuilder.build();
  }

}
