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

import org.operaton.bpm.model.bpmn.instance.EventDefinition;
import org.operaton.bpm.model.bpmn.instance.Signal;
import org.operaton.bpm.model.bpmn.instance.SignalEventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SIGNAL_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SIGNAL_EVENT_DEFINITION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_ASYNC;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN signalEventDefinition element
 *
 * @author Sebastian Menski
 */
public class SignalEventDefinitionImpl extends EventDefinitionImpl implements SignalEventDefinition {

  protected static AttributeReference<Signal> signalRefAttribute;
  protected static Attribute<Boolean> operatonAsyncAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(SignalEventDefinition.class, BPMN_ELEMENT_SIGNAL_EVENT_DEFINITION)
      .namespaceUri(BPMN20_NS)
      .extendsType(EventDefinition.class)
      .instanceProvider(new ModelTypeInstanceProvider<SignalEventDefinition>() {
      @Override
      public SignalEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
          return new SignalEventDefinitionImpl(instanceContext);
        }
      });

    signalRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SIGNAL_REF)
      .qNameAttributeReference(Signal.class)
      .build();

    /** Operaton Attributes */
    operatonAsyncAttribute = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_ASYNC)
      .namespace(OPERATON_NS)
      .defaultValue(false)
      .build();

    typeBuilder.build();
  }

  public SignalEventDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public Signal getSignal() {
    return signalRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSignal(Signal signal) {
    signalRefAttribute.setReferenceTargetElement(this, signal);
  }

  @Override
  public boolean isOperatonAsync() {
    return operatonAsyncAttribute.getValue(this);
  }

  @Override
  public void setOperatonAsync(boolean operatonAsync) {
    operatonAsyncAttribute.setValue(this, operatonAsync);
  }
}
