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

import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormData;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormField;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_FORM_DATA;

/**
 * The BPMN formData operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonFormDataImpl extends BpmnModelElementInstanceImpl implements OperatonFormData {

  protected static ChildElementCollection<OperatonFormField> operatonFormFieldCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonFormData.class, OPERATON_ELEMENT_FORM_DATA)
      .namespaceUri(BpmnModelConstants.OPERATON_NS)
      .instanceProvider(instanceContext -> new OperatonFormDataImpl(instanceContext));

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operatonFormFieldCollection = sequenceBuilder.elementCollection(OperatonFormField.class)
      .build();

    typeBuilder.build();
  }

  public OperatonFormDataImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<OperatonFormField> getOperatonFormFields() {
    return operatonFormFieldCollection.get(this);
  }
}
