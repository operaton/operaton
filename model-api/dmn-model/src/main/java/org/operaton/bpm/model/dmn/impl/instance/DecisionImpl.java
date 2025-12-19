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
package org.operaton.bpm.model.dmn.impl.instance;

import java.util.Collection;

import org.operaton.bpm.model.dmn.instance.AllowedAnswers;
import org.operaton.bpm.model.dmn.instance.AuthorityRequirement;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.DecisionMakerReference;
import org.operaton.bpm.model.dmn.instance.DecisionOwnerReference;
import org.operaton.bpm.model.dmn.instance.DrgElement;
import org.operaton.bpm.model.dmn.instance.Expression;
import org.operaton.bpm.model.dmn.instance.ImpactedPerformanceIndicatorReference;
import org.operaton.bpm.model.dmn.instance.InformationRequirement;
import org.operaton.bpm.model.dmn.instance.KnowledgeRequirement;
import org.operaton.bpm.model.dmn.instance.OrganizationUnit;
import org.operaton.bpm.model.dmn.instance.PerformanceIndicator;
import org.operaton.bpm.model.dmn.instance.Question;
import org.operaton.bpm.model.dmn.instance.SupportedObjectiveReference;
import org.operaton.bpm.model.dmn.instance.UsingProcessReference;
import org.operaton.bpm.model.dmn.instance.UsingTaskReference;
import org.operaton.bpm.model.dmn.instance.Variable;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_DECISION;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_ATTRIBUTE_VERSION_TAG;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_NS;

public class DecisionImpl extends DrgElementImpl implements Decision {

  protected static ChildElement<Question> questionChild;
  protected static ChildElement<AllowedAnswers> allowedAnswersChild;
  protected static ChildElement<Variable> variableChild;
  protected static ChildElementCollection<InformationRequirement> informationRequirementCollection;
  protected static ChildElementCollection<KnowledgeRequirement> knowledgeRequirementCollection;
  protected static ChildElementCollection<AuthorityRequirement> authorityRequirementCollection;
  protected static ChildElementCollection<SupportedObjectiveReference> supportedObjectiveChildElementCollection;
  protected static ElementReferenceCollection<PerformanceIndicator, ImpactedPerformanceIndicatorReference> impactedPerformanceIndicatorRefCollection;
  protected static ElementReferenceCollection<OrganizationUnit, DecisionMakerReference> decisionMakerRefCollection;
  protected static ElementReferenceCollection<OrganizationUnit, DecisionOwnerReference> decisionOwnerRefCollection;
  protected static ChildElementCollection<UsingProcessReference> usingProcessCollection;
  protected static ChildElementCollection<UsingTaskReference> usingTaskCollection;
  protected static ChildElement<Expression> expressionChild;

  // operaton extensions
  protected static Attribute<String> operatonHistoryTimeToLiveAttribute;
  protected static Attribute<String> operatonVersionTag;

  public DecisionImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Question getQuestion() {
    return questionChild.getChild(this);
  }

  @Override
  public void setQuestion(Question question) {
    questionChild.setChild(this, question);
  }

  @Override
  public AllowedAnswers getAllowedAnswers() {
    return allowedAnswersChild.getChild(this);
  }

  @Override
  public void setAllowedAnswers(AllowedAnswers allowedAnswers) {
    allowedAnswersChild.setChild(this, allowedAnswers);
  }

  @Override
  public Variable getVariable() {
    return variableChild.getChild(this);
  }

  @Override
  public void setVariable(Variable variable) {
    variableChild.setChild(this, variable);
  }

  @Override
  public Collection<InformationRequirement> getInformationRequirements() {
    return informationRequirementCollection.get(this);
  }

  @Override
  public Collection<KnowledgeRequirement> getKnowledgeRequirements() {
    return knowledgeRequirementCollection.get(this);
  }

  @Override
  public Collection<AuthorityRequirement> getAuthorityRequirements() {
    return authorityRequirementCollection.get(this);
  }

  @Override
  public Collection<SupportedObjectiveReference> getSupportedObjectiveReferences() {
    return supportedObjectiveChildElementCollection.get(this);
  }

  @Override
  public Collection<PerformanceIndicator> getImpactedPerformanceIndicators() {
    return impactedPerformanceIndicatorRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<OrganizationUnit> getDecisionMakers() {
    return decisionMakerRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<OrganizationUnit> getDecisionOwners() {
    return decisionOwnerRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<UsingProcessReference> getUsingProcessReferences() {
    return usingProcessCollection.get(this);
  }

  @Override
  public Collection<UsingTaskReference> getUsingTaskReferences() {
    return usingTaskCollection.get(this);
  }

  @Override
  public Expression getExpression() {
    return expressionChild.getChild(this);
  }

  @Override
  public void setExpression(Expression expression) {
    expressionChild.setChild(this, expression);
  }

  // operaton extensions
  @Override
  public Integer getOperatonHistoryTimeToLive() {
    String ttl = getOperatonHistoryTimeToLiveString();

    if (ttl != null) {
      return Integer.valueOf(ttl);
    }
    return null;
  }

  @Override
  public void setOperatonHistoryTimeToLive(Integer historyTimeToLive) {
    setOperatonHistoryTimeToLiveString(historyTimeToLive != null ? String.valueOf(historyTimeToLive) : null);
  }

  @Override
  public String getOperatonHistoryTimeToLiveString() {
    return operatonHistoryTimeToLiveAttribute.getValue(this);
  }

  @Override
  public void setOperatonHistoryTimeToLiveString(String historyTimeToLive) {
    operatonHistoryTimeToLiveAttribute.setValue(this, historyTimeToLive);
  }

  @Override
  public String getVersionTag() {
    return operatonVersionTag.getValue(this);
  }

  @Override
  public void setVersionTag(String inputVariable) {
    operatonVersionTag.setValue(this, inputVariable);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Decision.class, DMN_ELEMENT_DECISION)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(DrgElement.class)
      .instanceProvider(DecisionImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    questionChild = sequenceBuilder.element(Question.class)
      .build();

    allowedAnswersChild = sequenceBuilder.element(AllowedAnswers.class)
      .build();

    variableChild = sequenceBuilder.element(Variable.class)
      .build();

    informationRequirementCollection = sequenceBuilder.elementCollection(InformationRequirement.class)
      .build();

    knowledgeRequirementCollection = sequenceBuilder.elementCollection(KnowledgeRequirement.class)
      .build();

    authorityRequirementCollection = sequenceBuilder.elementCollection(AuthorityRequirement.class)
      .build();

    supportedObjectiveChildElementCollection = sequenceBuilder.elementCollection(SupportedObjectiveReference.class)
      .build();

    impactedPerformanceIndicatorRefCollection = sequenceBuilder.elementCollection(ImpactedPerformanceIndicatorReference.class)
      .uriElementReferenceCollection(PerformanceIndicator.class)
      .build();

    decisionMakerRefCollection = sequenceBuilder.elementCollection(DecisionMakerReference.class)
      .uriElementReferenceCollection(OrganizationUnit.class)
      .build();

    decisionOwnerRefCollection = sequenceBuilder.elementCollection(DecisionOwnerReference.class)
      .uriElementReferenceCollection(OrganizationUnit.class)
      .build();

    usingProcessCollection = sequenceBuilder.elementCollection(UsingProcessReference.class)
      .build();

    usingTaskCollection = sequenceBuilder.elementCollection(UsingTaskReference.class)
      .build();

    expressionChild = sequenceBuilder.element(Expression.class)
      .build();

    // operaton extensions

    operatonHistoryTimeToLiveAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE)
        .namespace(OPERATON_NS)
        .build();

    operatonVersionTag = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VERSION_TAG)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

}
