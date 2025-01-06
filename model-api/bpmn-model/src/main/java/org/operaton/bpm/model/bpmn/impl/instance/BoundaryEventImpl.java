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

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.BoundaryEventBuilder;
import org.operaton.bpm.model.bpmn.instance.Activity;
import org.operaton.bpm.model.bpmn.instance.BoundaryEvent;
import org.operaton.bpm.model.bpmn.instance.CatchEvent;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN boundaryEvent element
 *
 * @author Sebastian Menski
 */
public class BoundaryEventImpl extends CatchEventImpl implements BoundaryEvent {

  protected static Attribute<Boolean> cancelActivityAttribute;
  protected static AttributeReference<Activity> attachedToRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BoundaryEvent.class, BPMN_ELEMENT_BOUNDARY_EVENT)
      .namespaceUri(BPMN20_NS)
      .extendsType(CatchEvent.class)
      .instanceProvider(BoundaryEventImpl::new);

    cancelActivityAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_CANCEL_ACTIVITY)
      .defaultValue(true)
      .build();

    attachedToRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ATTACHED_TO_REF)
      .required()
      .qNameAttributeReference(Activity.class)
      .build();

    typeBuilder.build();
  }

  public BoundaryEventImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public BoundaryEventBuilder builder() {
    return new BoundaryEventBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public boolean cancelActivity() {
    return cancelActivityAttribute.getValue(this);
  }

  @Override
  public void setCancelActivity(boolean cancelActivity) {
    cancelActivityAttribute.setValue(this, cancelActivity);
  }

  @Override
  public Activity getAttachedTo() {
    return attachedToRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setAttachedTo(Activity attachedTo) {
    attachedToRefAttribute.setReferenceTargetElement(this, attachedTo);
  }

}
