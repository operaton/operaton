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

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ScriptTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.Script;
import org.operaton.bpm.model.bpmn.instance.ScriptTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN scriptTask element
 *
 * @author Sebastian Menski
 */
public class ScriptTaskImpl extends TaskImpl implements ScriptTask {

  protected static Attribute<String> scriptFormatAttribute;
  protected static ChildElement<Script> scriptChild;

  /** operaton extensions */

  protected static Attribute<String> operatonResultVariableAttribute;
  protected static Attribute<String> operatonResourceAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ScriptTask.class, BPMN_ELEMENT_SCRIPT_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(instanceContext -> new ScriptTaskImpl(instanceContext));

    scriptFormatAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_SCRIPT_FORMAT)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    scriptChild = sequenceBuilder.element(Script.class)
      .build();

    /** operaton extensions */

    operatonResultVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESULT_VARIABLE)
      .namespace(OPERATON_NS)
      .build();

    operatonResourceAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESOURCE)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public ScriptTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public ScriptTaskBuilder builder() {
    return new ScriptTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public String getScriptFormat() {
    return scriptFormatAttribute.getValue(this);
  }

  @Override
  public void setScriptFormat(String scriptFormat) {
    scriptFormatAttribute.setValue(this, scriptFormat);
  }

  @Override
  public Script getScript() {
    return scriptChild.getChild(this);
  }

  @Override
  public void setScript(Script script) {
    scriptChild.setChild(this, script);
  }

  /** operaton extensions */

  @Override
  public String getOperatonResultVariable() {
    return operatonResultVariableAttribute.getValue(this);
  }

  @Override
  public void setOperatonResultVariable(String operatonResultVariable) {
    operatonResultVariableAttribute.setValue(this, operatonResultVariable);
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
