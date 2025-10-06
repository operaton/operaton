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
package org.operaton.bpm.model.dmn.impl.instance;

import org.operaton.bpm.model.dmn.instance.DefaultOutputEntry;
import org.operaton.bpm.model.dmn.instance.DmnElement;
import org.operaton.bpm.model.dmn.instance.OutputClause;
import org.operaton.bpm.model.dmn.instance.OutputValues;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ATTRIBUTE_NAME;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ATTRIBUTE_TYPE_REF;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_OUTPUT_CLAUSE;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

public class OutputClauseImpl extends DmnElementImpl implements OutputClause {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> typeRefAttribute;

  protected static ChildElement<OutputValues> outputValuesChild;
  protected static ChildElement<DefaultOutputEntry> defaultOutputEntryChild;

  public OutputClauseImpl(ModelTypeInstanceContext instanceContext) {
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
  public String getTypeRef() {
    return typeRefAttribute.getValue(this);
  }

  @Override
  public void setTypeRef(String typeRef) {
    typeRefAttribute.setValue(this, typeRef);
  }

  @Override
  public OutputValues getOutputValues() {
    return outputValuesChild.getChild(this);
  }

  @Override
  public void setOutputValues(OutputValues outputValues) {
    outputValuesChild.setChild(this, outputValues);
  }

  @Override
  public DefaultOutputEntry getDefaultOutputEntry() {
    return defaultOutputEntryChild.getChild(this);
  }

  @Override
  public void setDefaultOutputEntry(DefaultOutputEntry defaultOutputEntry) {
    defaultOutputEntryChild.setChild(this, defaultOutputEntry);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OutputClause.class, DMN_ELEMENT_OUTPUT_CLAUSE)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(DmnElement.class)
      .instanceProvider(OutputClauseImpl::new);

    nameAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_NAME)
      .build();

    typeRefAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_TYPE_REF)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    outputValuesChild = sequenceBuilder.element(OutputValues.class)
      .build();

    defaultOutputEntryChild = sequenceBuilder.element(DefaultOutputEntry.class)
      .build();

    typeBuilder.build();
  }

}
