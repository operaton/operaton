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

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.CorrelationKey;
import org.operaton.bpm.model.bpmn.instance.CorrelationPropertyBinding;
import org.operaton.bpm.model.bpmn.instance.CorrelationSubscription;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN correlationSubscription element
 *
 * @author Sebastian Menski
 */
public class CorrelationSubscriptionImpl extends BaseElementImpl implements CorrelationSubscription {

  protected static AttributeReference<CorrelationKey> correlationKeyAttribute;
  protected static ChildElementCollection<CorrelationPropertyBinding> correlationPropertyBindingCollection;
  
  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CorrelationSubscription.class, BPMN_ELEMENT_CORRELATION_SUBSCRIPTION)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(new ModelTypeInstanceProvider<CorrelationSubscription>() {
      @Override
      public CorrelationSubscription newInstance(ModelTypeInstanceContext instanceContext) {
          return new CorrelationSubscriptionImpl(instanceContext);
        }
      });

    correlationKeyAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CORRELATION_KEY_REF)
      .required()
      .qNameAttributeReference(CorrelationKey.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    correlationPropertyBindingCollection = sequenceBuilder.elementCollection(CorrelationPropertyBinding.class)
      .build();

    typeBuilder.build();
  }

  public CorrelationSubscriptionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public CorrelationKey getCorrelationKey() {
    return correlationKeyAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCorrelationKey(CorrelationKey correlationKey) {
    correlationKeyAttribute.setReferenceTargetElement(this, correlationKey);
  }

  @Override
  public Collection<CorrelationPropertyBinding> getCorrelationPropertyBindings() {
    return correlationPropertyBindingCollection.get(this);
  }
}
