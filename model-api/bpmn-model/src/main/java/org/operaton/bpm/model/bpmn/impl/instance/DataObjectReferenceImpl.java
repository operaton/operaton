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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_DATA_OBJECT_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ITEM_SUBJECT_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_DATA_OBJECT_REFERENCE;

import org.operaton.bpm.model.bpmn.instance.DataObject;
import org.operaton.bpm.model.bpmn.instance.DataObjectReference;
import org.operaton.bpm.model.bpmn.instance.DataState;
import org.operaton.bpm.model.bpmn.instance.FlowElement;
import org.operaton.bpm.model.bpmn.instance.ItemDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Dario Campagna
 */
public class DataObjectReferenceImpl extends FlowElementImpl implements DataObjectReference {

  protected static AttributeReference<ItemDefinition> itemSubjectRefAttribute;
  protected static AttributeReference<DataObject> dataObjectRefAttribute;
  protected static ChildElement<DataState> dataStateChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(DataObjectReference.class, BPMN_ELEMENT_DATA_OBJECT_REFERENCE)
      .namespaceUri(BPMN20_NS)
      .extendsType(FlowElement.class)
      .instanceProvider(DataObjectReferenceImpl::new);

    itemSubjectRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ITEM_SUBJECT_REF)
      .qNameAttributeReference(ItemDefinition.class)
      .build();

    dataObjectRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DATA_OBJECT_REF)
      .idAttributeReference(DataObject.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataStateChild = sequenceBuilder.element(DataState.class)
      .build();

    typeBuilder.build();
  }

  public DataObjectReferenceImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public ItemDefinition getItemSubject() {
    return itemSubjectRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setItemSubject(ItemDefinition itemSubject) {
    itemSubjectRefAttribute.setReferenceTargetElement(this, itemSubject);
  }

  @Override
  public DataState getDataState() {
    return dataStateChild.getChild(this);
  }

  @Override
  public void setDataState(DataState dataState) {
    dataStateChild.setChild(this, dataState);
  }

  @Override
  public DataObject getDataObject() {
    return dataObjectRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setDataObject(DataObject dataObject) {
    dataObjectRefAttribute.setReferenceTargetElement(this, dataObject);
  }
}
