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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_KEY;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_ENTRY;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import org.operaton.bpm.model.bpmn.instance.operaton.OperatonEntry;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

/**
 * @author Sebastian Menski
 */
public class OperatonEntryImpl extends OperatonGenericValueElementImpl implements OperatonEntry {

  protected static Attribute<String> operatonKeyAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonEntry.class, OPERATON_ELEMENT_ENTRY)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonEntry>() {
      @Override
      public OperatonEntry newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonEntryImpl(instanceContext);
        }
      });

    operatonKeyAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_KEY)
      .namespace(OPERATON_NS)
      .required()
      .build();

    typeBuilder.build();
  }

  public OperatonEntryImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonKey() {
    return operatonKeyAttribute.getValue(this);
  }

  @Override
  public void setOperatonKey(String operatonKey) {
    operatonKeyAttribute.setValue(this, operatonKey);
  }

}
