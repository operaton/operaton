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

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_DECISION_BINDING;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_DECISION_VERSION;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_DECISION_TENANT_ID;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_MAP_DECISION_RESULT;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_RESULT_VARIABLE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_DECISION_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_DECISION_TASK;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.DecisionRefExpression;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;
import org.operaton.bpm.model.cmmn.instance.ParameterMapping;
import org.operaton.bpm.model.cmmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class DecisionTaskImpl extends TaskImpl implements DecisionTask {

  protected static Attribute<String> decisionRefAttribute;

  protected static ChildElementCollection<ParameterMapping> parameterMappingCollection;
  protected static ChildElement<DecisionRefExpression> decisionRefExpressionChild;

  /** Operaton extensions */
  protected static Attribute<String> operatonResultVariableAttribute;
  protected static Attribute<String> operatonDecisionBindingAttribute;
  protected static Attribute<String> operatonDecisionVersionAttribute;
  protected static Attribute<String> operatonDecisionTenantIdAttribute;
  protected static Attribute<String> operatonMapDecisionResultAttribute;

  public DecisionTaskImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getDecision() {
    return decisionRefAttribute.getValue(this);
  }

  @Override
  public void setDecision(String decision) {
    decisionRefAttribute.setValue(this, decision);
  }

  @Override
  public Collection<ParameterMapping> getParameterMappings() {
    return parameterMappingCollection.get(this);
  }

  @Override
  public DecisionRefExpression getDecisionExpression() {
    return decisionRefExpressionChild.getChild(this);
  }

  @Override
  public void setDecisionExpression(DecisionRefExpression decisionExpression) {
    decisionRefExpressionChild.setChild(this, decisionExpression);
  }

  @Override
  public String getOperatonResultVariable() {
    return operatonResultVariableAttribute.getValue(this);
  }

  @Override
  public void setOperatonResultVariable(String operatonResultVariable) {
    operatonResultVariableAttribute.setValue(this, operatonResultVariable);
  }

  @Override
  public String getOperatonDecisionBinding() {
    return operatonDecisionBindingAttribute.getValue(this);
  }

  @Override
  public void setOperatonDecisionBinding(String operatonDecisionBinding) {
    operatonDecisionBindingAttribute.setValue(this, operatonDecisionBinding);
  }

  @Override
  public String getOperatonDecisionVersion() {
    return operatonDecisionVersionAttribute.getValue(this);
  }

  @Override
  public void setOperatonDecisionVersion(String operatonDecisionVersion) {
    operatonDecisionVersionAttribute.setValue(this, operatonDecisionVersion);
  }

  @Override
  public String getOperatonDecisionTenantId() {
    return operatonDecisionTenantIdAttribute.getValue(this);
  }

  @Override
  public void setOperatonDecisionTenantId(String operatonDecisionTenantId) {
    operatonDecisionTenantIdAttribute.setValue(this, operatonDecisionTenantId);
  }

  @Override
  public String getOperatonMapDecisionResult() {
    return operatonMapDecisionResultAttribute.getValue(this);
  }

  @Override
  public void setOperatonMapDecisionResult(String operatonMapDecisionResult) {
    operatonMapDecisionResultAttribute.setValue(this, operatonMapDecisionResult);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(DecisionTask.class, CMMN_ELEMENT_DECISION_TASK)
        .namespaceUri(CMMN11_NS)
        .extendsType(Task.class)
        .instanceProvider(DecisionTaskImpl::new);

    decisionRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_DECISION_REF)
        .build();

    /** Operaton extensions */

    operatonResultVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_RESULT_VARIABLE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonDecisionBindingAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DECISION_BINDING)
      .namespace(CAMUNDA_NS)
      .build();

    operatonDecisionVersionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DECISION_VERSION)
      .namespace(CAMUNDA_NS)
      .build();

    operatonDecisionTenantIdAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DECISION_TENANT_ID)
        .namespace(CAMUNDA_NS)
        .build();

    operatonMapDecisionResultAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_MAP_DECISION_RESULT)
      .namespace(CAMUNDA_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    parameterMappingCollection = sequenceBuilder.elementCollection(ParameterMapping.class)
        .build();

    decisionRefExpressionChild = sequenceBuilder.element(DecisionRefExpression.class)
        .build();

    typeBuilder.build();
  }

}
