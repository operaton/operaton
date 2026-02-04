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

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.InputCaseParameter;
import org.operaton.bpm.model.cmmn.instance.InputsCaseParameter;
import org.operaton.bpm.model.cmmn.instance.OutputCaseParameter;
import org.operaton.bpm.model.cmmn.instance.OutputsCaseParameter;
import org.operaton.bpm.model.cmmn.instance.PlanItemDefinition;
import org.operaton.bpm.model.cmmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_IS_BLOCKING;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_TASK;

/**
 * @author Roman Smirnov
 *
 */
@SuppressWarnings("java:S1874") // Use of cmmn1.0 deprecated field
public class TaskImpl extends PlanItemDefinitionImpl implements Task {

  protected static Attribute<Boolean> isBlockingAttribute;

  // cmmn 1.0
  /**
   * @deprecated since 1.0, use inputParameterCollection instead.
   */
  @Deprecated(since = "1.0")
  protected static ChildElementCollection<InputsCaseParameter> inputsCollection;
  /**
   * @deprecated since 1.0, use outputParameterCollection instead.
   */
  @Deprecated(since = "1.0")
  protected static ChildElementCollection<OutputsCaseParameter> outputsCollection;

  // cmmn 1.1
  protected static ChildElementCollection<InputCaseParameter> inputParameterCollection;
  protected static ChildElementCollection<OutputCaseParameter> outputParameterCollection;

  public TaskImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public boolean isBlocking() {
    return isBlockingAttribute.getValue(this);
  }

  @Override
  public void setIsBlocking(boolean isBlocking) {
    isBlockingAttribute.setValue(this, isBlocking);
  }

  @Override
  public Collection<InputsCaseParameter> getInputs() {
    return inputsCollection.get(this);
  }

  @Override
  public Collection<OutputsCaseParameter> getOutputs() {
    return outputsCollection.get(this);
  }

  @Override
  public Collection<InputCaseParameter> getInputParameters() {
    return inputParameterCollection.get(this);
  }

  @Override
  public Collection<OutputCaseParameter> getOutputParameters() {
    return outputParameterCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Task.class, CMMN_ELEMENT_TASK)
        .namespaceUri(CMMN11_NS)
        .extendsType(PlanItemDefinition.class)
        .instanceProvider(TaskImpl::new);

    isBlockingAttribute = typeBuilder.booleanAttribute(CMMN_ATTRIBUTE_IS_BLOCKING)
        .defaultValue(true)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inputsCollection = sequenceBuilder.elementCollection(InputsCaseParameter.class)
        .build();

    outputsCollection = sequenceBuilder.elementCollection(OutputsCaseParameter.class)
        .build();

    inputParameterCollection = sequenceBuilder.elementCollection(InputCaseParameter.class)
        .build();

    outputParameterCollection = sequenceBuilder.elementCollection(OutputCaseParameter.class)
        .build();

    typeBuilder.build();
  }

}
