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

import org.operaton.bpm.model.bpmn.BpmnModelException;
import org.operaton.bpm.model.bpmn.Query;
import org.operaton.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.operaton.bpm.model.bpmn.impl.QueryImpl;
import org.operaton.bpm.model.bpmn.instance.FlowElement;
import org.operaton.bpm.model.bpmn.instance.FlowNode;
import org.operaton.bpm.model.bpmn.instance.SequenceFlow;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;
import org.operaton.bpm.model.xml.type.reference.Reference;

import java.util.Collection;
import java.util.HashSet;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN flowNode element
 *
 * @author Sebastian Menski
 */
public abstract class FlowNodeImpl extends FlowElementImpl implements FlowNode {

  protected static ElementReferenceCollection<SequenceFlow, Incoming> incomingCollection;
  protected static ElementReferenceCollection<SequenceFlow, Outgoing> outgoingCollection;

  /** Operaton Attributes */
  protected static Attribute<Boolean> operatonAsyncAfter;
  protected static Attribute<Boolean> operatonAsyncBefore;
  protected static Attribute<Boolean> operatonExclusive;
  protected static Attribute<String> operatonJobPriority;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowNode.class, BPMN_ELEMENT_FLOW_NODE)
      .namespaceUri(BPMN20_NS)
      .extendsType(FlowElement.class)
      .abstractType();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    incomingCollection = sequenceBuilder.elementCollection(Incoming.class)
      .qNameElementReferenceCollection(SequenceFlow.class)
      .build();

    outgoingCollection = sequenceBuilder.elementCollection(Outgoing.class)
      .qNameElementReferenceCollection(SequenceFlow.class)
      .build();

    /** Operaton Attributes */

    operatonAsyncAfter = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_ASYNC_AFTER)
      .namespace(OPERATON_NS)
      .defaultValue(false)
      .build();

    operatonAsyncBefore = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_ASYNC_BEFORE)
      .namespace(OPERATON_NS)
      .defaultValue(false)
      .build();

    operatonExclusive = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_EXCLUSIVE)
      .namespace(OPERATON_NS)
      .defaultValue(true)
      .build();

    operatonJobPriority = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_JOB_PRIORITY)
       .namespace(OPERATON_NS)
       .build();

    typeBuilder.build();
  }

  public FlowNodeImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @SuppressWarnings("rawtypes")
  public AbstractFlowNodeBuilder builder() {
    throw new BpmnModelException("No builder implemented for type " + getElementType().getTypeNamespace() +":" + getElementType().getTypeName());
  }

  @SuppressWarnings("rawtypes")
  public void updateAfterReplacement() {
    super.updateAfterReplacement();
    Collection<Reference> incomingReferences = getIncomingReferencesByType(SequenceFlow.class);
    for (Reference<?> reference : incomingReferences) {
      for (ModelElementInstance sourceElement : reference.findReferenceSourceElements(this)) {
        String referenceIdentifier = reference.getReferenceIdentifier(sourceElement);

        if (referenceIdentifier != null && referenceIdentifier.equals(getId()) && reference instanceof AttributeReference attributeReference) {
          String attributeName = attributeReference.getReferenceSourceAttribute().getAttributeName();
          if (attributeName.equals(BPMN_ATTRIBUTE_SOURCE_REF)) {
            getOutgoing().add((SequenceFlow) sourceElement);
          }
          else if (attributeName.equals(BPMN_ATTRIBUTE_TARGET_REF)) {
            getIncoming().add((SequenceFlow) sourceElement);
          }
        }
      }

    }
  }

  public Collection<SequenceFlow> getIncoming() {
    return incomingCollection.getReferenceTargetElements(this);
  }

  public Collection<SequenceFlow> getOutgoing() {
    return outgoingCollection.getReferenceTargetElements(this);
  }

  public Query<FlowNode> getPreviousNodes() {
    Collection<FlowNode> previousNodes = new HashSet<FlowNode>();
    for (SequenceFlow sequenceFlow : getIncoming()) {
      previousNodes.add(sequenceFlow.getSource());
    }
    return new QueryImpl<FlowNode>(previousNodes);
  }

  public Query<FlowNode> getSucceedingNodes() {
    Collection<FlowNode> succeedingNodes = new HashSet<FlowNode>();
    for (SequenceFlow sequenceFlow : getOutgoing()) {
      succeedingNodes.add(sequenceFlow.getTarget());
    }
    return new QueryImpl<FlowNode>(succeedingNodes);
  }

  /** Operaton Attributes */

  public boolean isOperatonAsyncBefore() {
    return operatonAsyncBefore.getValue(this);
  }

  public void setOperatonAsyncBefore(boolean isOperatonAsyncBefore) {
    operatonAsyncBefore.setValue(this, isOperatonAsyncBefore);
  }

  public boolean isOperatonAsyncAfter() {
    return operatonAsyncAfter.getValue(this);
  }

  public void setOperatonAsyncAfter(boolean isOperatonAsyncAfter) {
    operatonAsyncAfter.setValue(this, isOperatonAsyncAfter);
  }

  public boolean isOperatonExclusive() {
    return operatonExclusive.getValue(this);
  }

  public void setOperatonExclusive(boolean isOperatonExclusive) {
    operatonExclusive.setValue(this, isOperatonExclusive);
  }

  public String getOperatonJobPriority() {
    return operatonJobPriority.getValue(this);
  }

  public void setOperatonJobPriority(String jobPriority) {
    operatonJobPriority.setValue(this, jobPriority);
  }
}
