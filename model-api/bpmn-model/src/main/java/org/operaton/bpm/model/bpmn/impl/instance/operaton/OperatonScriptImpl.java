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
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonScript;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_RESOURCE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_SCRIPT_FORMAT;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_SCRIPT;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * The BPMN script operaton extension element
 *
 * @author Sebastian Menski
 */
public class OperatonScriptImpl extends BpmnModelElementInstanceImpl implements OperatonScript {

  protected static Attribute<String> operatonScriptFormatAttribute;
  protected static Attribute<String> operatonResourceAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonScript.class, OPERATON_ELEMENT_SCRIPT)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonScriptImpl::new);

    operatonScriptFormatAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_SCRIPT_FORMAT)
      .required()
      .build();

    operatonResourceAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESOURCE)
      .build();

    typeBuilder.build();
  }

  public OperatonScriptImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonScriptFormat() {
    return operatonScriptFormatAttribute.getValue(this);
  }

  @Override
  public void setOperatonScriptFormat(String operatonScriptFormat) {
    operatonScriptFormatAttribute.setValue(this, operatonScriptFormat);
  }

  @Override
  public String getOperatonResource() {
    return operatonResourceAttribute.getValue(this);
  }

  @Override
  public void setOperatonResource(String operatonResource) {
    operatonResourceAttribute.setValue(this, operatonResource);
  }
}
