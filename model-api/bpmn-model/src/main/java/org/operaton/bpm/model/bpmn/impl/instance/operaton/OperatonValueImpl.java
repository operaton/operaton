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
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValue;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;
import static org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

/**
 * The BPMN value operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonValueImpl extends BpmnModelElementInstanceImpl implements OperatonValue {

  protected static Attribute<String> operatonIdAttribute;
  protected static Attribute<String> operatonNameAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonValue.class, OPERATON_ELEMENT_VALUE)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonValue>() {
      @Override
      public OperatonValue newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonValueImpl(instanceContext);
        }
      });

    operatonIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ID)
      .namespace(OPERATON_NS)
      .build();

    operatonNameAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_NAME)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public OperatonValueImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonId() {
    return operatonIdAttribute.getValue(this);
  }

  @Override
  public void setOperatonId(String operatonId) {
    operatonIdAttribute.setValue(this, operatonId);
  }

  @Override
  public String getOperatonName() {
    return operatonNameAttribute.getValue(this);
  }

  @Override
  public void setOperatonName(String operatonName) {
    operatonNameAttribute.setValue(this, operatonName);
  }
}
