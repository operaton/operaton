/*
 * Copyright 2026 FINOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ORDERING;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_AD_HOC_SUB_PROCESS;

import org.operaton.bpm.model.bpmn.instance.AdHocSubProcess;
import org.operaton.bpm.model.bpmn.instance.CompletionCondition;
import org.operaton.bpm.model.bpmn.instance.SubProcess;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN 2.0 adHocSubProcess element implementation.
 *
 * <p>Note: the {@code ordering} attribute is persisted as BPMN model metadata.
 * In the current engine implementation, ad-hoc runtime activation is
 * parallel-only and "Sequential" BPMN deployments are rejected by the engine.
 *
 */
public class AdHocSubProcessImpl extends SubProcessImpl implements AdHocSubProcess {

  protected static Attribute<String> orderingAttribute;
  protected static Attribute<Boolean> cancelRemainingInstancesAttribute;
  protected static ChildElement<CompletionCondition> completionConditionChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder
      .defineType(AdHocSubProcess.class, BPMN_ELEMENT_AD_HOC_SUB_PROCESS)
      .namespaceUri(BPMN20_NS)
      .extendsType(SubProcess.class)
      .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<AdHocSubProcess>() {
        public AdHocSubProcess newInstance(ModelTypeInstanceContext instanceContext) {
          return new AdHocSubProcessImpl(instanceContext);
        }
      });

    orderingAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ORDERING)
      .defaultValue("Parallel")
      .build();

    cancelRemainingInstancesAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES)
      .defaultValue(true)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    completionConditionChild = sequenceBuilder.element(CompletionCondition.class)
      .minOccurs(0)
      .maxOccurs(1)
      .build();

    typeBuilder.build();
  }

  public AdHocSubProcessImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOrdering() {
    return orderingAttribute.getValue(this);
  }

  @Override
  public void setOrdering(String ordering) {
    orderingAttribute.setValue(this, ordering);
  }

  @Override
  public boolean isCancelRemainingInstances() {
    Boolean value = cancelRemainingInstancesAttribute.getValue(this);
    return value == null ? true : value;
  }

  @Override
  public void setCancelRemainingInstances(boolean cancelRemainingInstances) {
    cancelRemainingInstancesAttribute.setValue(this, cancelRemainingInstances);
  }

  @Override
  public CompletionCondition getCompletionCondition() {
    return completionConditionChild.getChild(this);
  }

  @Override
  public void setCompletionCondition(CompletionCondition completionCondition) {
    completionConditionChild.setChild(this, completionCondition);
  }

}
