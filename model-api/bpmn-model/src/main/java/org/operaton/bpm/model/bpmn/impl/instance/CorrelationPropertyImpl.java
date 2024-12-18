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

import org.operaton.bpm.model.bpmn.instance.CorrelationProperty;
import org.operaton.bpm.model.bpmn.instance.CorrelationPropertyRetrievalExpression;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN correlationProperty element
 *
 * @author Sebastian Menski
 */
public class CorrelationPropertyImpl extends RootElementImpl implements CorrelationProperty {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<ItemDefinition> typeAttribute;
  protected static ChildElementCollection<CorrelationPropertyRetrievalExpression> correlationPropertyRetrievalExpressionCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder;
    typeBuilder = modelBuilder.defineType(CorrelationProperty.class, BPMN_ELEMENT_CORRELATION_PROPERTY)
      .namespaceUri(BPMN20_NS)
      .extendsType(RootElement.class)
      .instanceProvider(new ModelTypeInstanceProvider<CorrelationProperty>() {
      @Override
      public CorrelationProperty newInstance(ModelTypeInstanceContext instanceContext) {
          return new CorrelationPropertyImpl(instanceContext);
        }
      });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
      .build();

    typeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE)
      .qNameAttributeReference(ItemDefinition.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    correlationPropertyRetrievalExpressionCollection = sequenceBuilder
      .elementCollection(CorrelationPropertyRetrievalExpression.class)
      .required()
      .build();

    typeBuilder.build();
  }

  public CorrelationPropertyImpl(ModelTypeInstanceContext context) {
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
  public ItemDefinition getType() {
    return typeAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setType(ItemDefinition type) {
    typeAttribute.setReferenceTargetElement(this, type);
  }

  @Override
  public Collection<CorrelationPropertyRetrievalExpression> getCorrelationPropertyRetrievalExpressions() {
    return correlationPropertyRetrievalExpressionCollection.get(this);
  }
}
