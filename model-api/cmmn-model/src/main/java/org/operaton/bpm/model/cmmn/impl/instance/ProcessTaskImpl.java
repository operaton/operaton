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
package org.operaton.bpm.model.cmmn.impl.instance;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.ParameterMapping;
import org.operaton.bpm.model.cmmn.instance.ProcessRefExpression;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;
import org.operaton.bpm.model.cmmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_PROCESS_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PROCESS_TASK;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_PROCESS_BINDING;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_PROCESS_TENANT_ID;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_PROCESS_VERSION;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessTaskImpl extends TaskImpl implements ProcessTask {

  protected static Attribute<String> processRefAttribute;
  protected static ChildElementCollection<ParameterMapping> parameterMappingCollection;
  protected static ChildElement<ProcessRefExpression> processRefExpressionChild;

  protected static Attribute<String> operatonProcessBindingAttribute;
  protected static Attribute<String> operatonProcessVersionAttribute;
  protected static Attribute<String> operatonProcessTenantIdAttribute;

  public ProcessTaskImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getProcess() {
    return processRefAttribute.getValue(this);
  }

  @Override
  public void setProcess(String process) {
    processRefAttribute.setValue(this, process);
  }

  @Override
  public ProcessRefExpression getProcessExpression() {
    return processRefExpressionChild.getChild(this);
  }

  @Override
  public void setProcessExpression(ProcessRefExpression processExpression) {
    processRefExpressionChild.setChild(this, processExpression);
  }

  @Override
  public Collection<ParameterMapping> getParameterMappings() {
    return parameterMappingCollection.get(this);
  }

  @Override
  public String getOperatonProcessBinding() {
    return operatonProcessBindingAttribute.getValue(this);
  }

  @Override
  public void setOperatonProcessBinding(String operatonProcessBinding) {
    operatonProcessBindingAttribute.setValue(this, operatonProcessBinding);
  }

  @Override
  public String getOperatonProcessVersion() {
    return operatonProcessVersionAttribute.getValue(this);
  }

  @Override
  public void setOperatonProcessVersion(String operatonProcessVersion) {
    operatonProcessVersionAttribute.setValue(this, operatonProcessVersion);
  }

  @Override
  public String getOperatonProcessTenantId() {
    return operatonProcessTenantIdAttribute.getValue(this);
  }

  @Override
  public void setOperatonProcessTenantId(String operatonProcessTenantId) {
    operatonProcessTenantIdAttribute.setValue(this, operatonProcessTenantId);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ProcessTask.class, CMMN_ELEMENT_PROCESS_TASK)
        .namespaceUri(CMMN11_NS)
        .extendsType(Task.class)
        .instanceProvider(ProcessTaskImpl::new);

    processRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_PROCESS_REF)
        .build();

    /** operaton extensions */

    operatonProcessBindingAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_PROCESS_BINDING)
      .namespace(OPERATON_NS)
      .build();

    operatonProcessVersionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_PROCESS_VERSION)
      .namespace(OPERATON_NS)
      .build();

    operatonProcessTenantIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_PROCESS_TENANT_ID)
        .namespace(OPERATON_NS)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    parameterMappingCollection = sequenceBuilder.elementCollection(ParameterMapping.class)
        .build();

    processRefExpressionChild = sequenceBuilder.element(ProcessRefExpression.class)
        .minOccurs(0)
        .maxOccurs(1)
        .build();

    typeBuilder.build();
  }


}
