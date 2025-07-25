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

import org.operaton.bpm.model.bpmn.instance.Escalation;
import org.operaton.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.operaton.bpm.model.bpmn.instance.EventDefinition;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN escalationEventDefinition element
 *
 * @author Sebastian Menski
 */
public class EscalationEventDefinitionImpl extends EventDefinitionImpl implements EscalationEventDefinition {

  protected static AttributeReference<Escalation> escalationRefAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(EscalationEventDefinition.class, BPMN_ELEMENT_ESCALATION_EVENT_DEFINITION)
      .namespaceUri(BPMN20_NS)
      .extendsType(EventDefinition.class)
      .instanceProvider(EscalationEventDefinitionImpl::new);

    escalationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ESCALATION_REF)
      .qNameAttributeReference(Escalation.class)
      .build();

    typeBuilder.build();
  }

  public EscalationEventDefinitionImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public Escalation getEscalation() {
    return escalationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setEscalation(Escalation escalation) {
    escalationRefAttribute.setReferenceTargetElement(this, escalation);
  }

}
