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

import org.operaton.bpm.model.cmmn.instance.CaseRefExpression;
import org.operaton.bpm.model.cmmn.instance.CaseTask;
import org.operaton.bpm.model.cmmn.instance.ParameterMapping;
import org.operaton.bpm.model.cmmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_CASE_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_CASE_TASK;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_CASE_BINDING;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_CASE_TENANT_ID;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_CASE_VERSION;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_NS;

/**
 * @author Roman Smirnov
 *
 */
public class CaseTaskImpl extends TaskImpl implements CaseTask {

  protected static Attribute<String> caseRefAttribute;
  protected static ChildElementCollection<ParameterMapping> parameterMappingCollection;

  // cmmn 1.1
  protected static ChildElement<CaseRefExpression> caseRefExpressionChild;

  protected static Attribute<String> operatonCaseBindingAttribute;
  protected static Attribute<String> operatonCaseVersionAttribute;
  protected static Attribute<String> operatonCaseTenantIdAttribute;

  public CaseTaskImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getCase() {
    return caseRefAttribute.getValue(this);
  }

  @Override
  public void setCase(String caseInstance) {
    caseRefAttribute.setValue(this, caseInstance);
  }

  @Override
  public CaseRefExpression getCaseExpression() {
    return caseRefExpressionChild.getChild(this);
  }

  @Override
  public void setCaseExpression(CaseRefExpression caseExpression) {
    caseRefExpressionChild.setChild(this, caseExpression);
  }

  @Override
  public Collection<ParameterMapping> getParameterMappings() {
    return parameterMappingCollection.get(this);
  }

  @Override
  public String getOperatonCaseBinding() {
    return operatonCaseBindingAttribute.getValue(this);
  }

  @Override
  public void setOperatonCaseBinding(String operatonCaseBinding) {
    operatonCaseBindingAttribute.setValue(this, operatonCaseBinding);
  }

  @Override
  public String getOperatonCaseVersion() {
    return operatonCaseVersionAttribute.getValue(this);
  }

  @Override
  public void setOperatonCaseVersion(String operatonCaseVersion) {
    operatonCaseVersionAttribute.setValue(this, operatonCaseVersion);
  }

  @Override
  public String getOperatonCaseTenantId() {
    return operatonCaseTenantIdAttribute.getValue(this);
  }

  @Override
  public void setOperatonCaseTenantId(String operatonCaseTenantId) {
    operatonCaseTenantIdAttribute.setValue(this, operatonCaseTenantId);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CaseTask.class, CMMN_ELEMENT_CASE_TASK)
        .extendsType(Task.class)
        .namespaceUri(CMMN11_NS)
        .instanceProvider(CaseTaskImpl::new);

    caseRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_CASE_REF)
        .build();

    /** operaton extensions */

    operatonCaseBindingAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CASE_BINDING)
      .namespace(OPERATON_NS)
      .build();

    operatonCaseVersionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CASE_VERSION)
      .namespace(OPERATON_NS)
      .build();

    operatonCaseTenantIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CASE_TENANT_ID)
        .namespace(OPERATON_NS)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    parameterMappingCollection = sequenceBuilder.elementCollection(ParameterMapping.class)
        .build();

    caseRefExpressionChild = sequenceBuilder.element(CaseRefExpression.class)
        .build();

    typeBuilder.build();
  }

}
