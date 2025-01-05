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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonConstraint;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValidation;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_VALIDATION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * The BPMN validation operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonValidationImpl extends BpmnModelElementInstanceImpl implements OperatonValidation {

  protected static ChildElementCollection<OperatonConstraint> operatonConstraintCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonValidation.class, OPERATON_ELEMENT_VALIDATION)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(instanceContext -> new OperatonValidationImpl(instanceContext));

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonConstraintCollection = sequenceBuilder.elementCollection(OperatonConstraint.class)
      .build();

    typeBuilder.build();
  }

  public OperatonValidationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<OperatonConstraint> getOperatonConstraints() {
    return operatonConstraintCollection.get(this);
  }
}
