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
package org.operaton.bpm.model.cmmn.impl.instance.operaton;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_RESOURCE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_SCRIPT_FORMAT;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ELEMENT_SCRIPT;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;

import org.operaton.bpm.model.cmmn.impl.instance.CmmnModelElementInstanceImpl;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonScript;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

/**
 * @author Roman Smirnov
 *
 */
public class OperatonScriptImpl extends CmmnModelElementInstanceImpl implements OperatonScript {

  protected static Attribute<String> operatonScriptFormatAttribute;
  protected static Attribute<String> operatonResourceAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonScript.class, OPERATON_ELEMENT_SCRIPT)
      .namespaceUri(CAMUNDA_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonScript>() {
        public OperatonScript newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonScriptImpl(instanceContext);
        }
      });

    operatonScriptFormatAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_SCRIPT_FORMAT)
        .namespace(CAMUNDA_NS)
        .build();

    operatonResourceAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESOURCE)
        .namespace(CAMUNDA_NS)
        .build();

    typeBuilder.build();
  }

  public OperatonScriptImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public String getOperatonScriptFormat() {
    return operatonScriptFormatAttribute.getValue(this);
  }

  public void setOperatonScriptFormat(String scriptFormat) {
    operatonScriptFormatAttribute.setValue(this, scriptFormat);
  }

  public String getOperatonResource() {
    return operatonResourceAttribute.getValue(this);
  }

  public void setOperatonResoure(String resource) {
    operatonResourceAttribute.setValue(this, resource);
  }

}
