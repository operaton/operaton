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

import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CaseFileModel;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseRole;
import org.operaton.bpm.model.cmmn.instance.CaseRoles;
import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.InputCaseParameter;
import org.operaton.bpm.model.cmmn.instance.OutputCaseParameter;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.*;

/**
 * @author Roman Smirnov
 *
 */
public class CaseImpl extends CmmnElementImpl implements Case {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> operatonHistoryTimeToLive;

  protected static ChildElement<CaseFileModel> caseFileModelChild;
  protected static ChildElement<CasePlanModel> casePlanModelChild;
  protected static ChildElementCollection<InputCaseParameter> inputCollection;
  protected static ChildElementCollection<OutputCaseParameter> outputCollection;

  // cmmn 1.0
  /**
   * @deprecated since 1.0, use caseRolesChild instead.
   */
  @Deprecated(since = "1.0")
  protected static ChildElementCollection<CaseRole> caseRolesCollection;

  // cmmn 1.1
  protected static ChildElement<CaseRoles> caseRolesChild;

  public CaseImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Collection<CaseRole> getCaseRoles() {
    return caseRolesCollection.get(this);
  }

  @Override
  public CaseRoles getRoles() {
    return caseRolesChild.getChild(this);
  }

  @Override
  public void setRoles(CaseRoles caseRole) {
    caseRolesChild.setChild(this, caseRole);
  }

  @Override
  public Collection<InputCaseParameter> getInputs() {
    return inputCollection.get(this);
  }

  @Override
  public Collection<OutputCaseParameter> getOutputs() {
    return outputCollection.get(this);
  }

  @Override
  public CasePlanModel getCasePlanModel() {
    return casePlanModelChild.getChild(this);
  }

  @Override
  public void setCasePlanModel(CasePlanModel casePlanModel) {
    casePlanModelChild.setChild(this, casePlanModel);
  }

  @Override
  public CaseFileModel getCaseFileModel() {
    return caseFileModelChild.getChild(this);
  }

  @Override
  public void setCaseFileModel(CaseFileModel caseFileModel) {
    caseFileModelChild.setChild(this, caseFileModel);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Case.class, CMMN_ELEMENT_CASE)
        .extendsType(CmmnElement.class)
        .namespaceUri(CMMN11_NS)
        .instanceProvider(CaseImpl::new);

    nameAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_NAME)
        .build();

    operatonHistoryTimeToLive = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE)
        .namespace(OPERATON_NS)
        .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    caseFileModelChild = sequenceBuilder.element(CaseFileModel.class)
        .build();

    casePlanModelChild = sequenceBuilder.element(CasePlanModel.class)
        .build();

    caseRolesCollection = sequenceBuilder.elementCollection(CaseRole.class)
        .build();

    caseRolesChild = sequenceBuilder.element(CaseRoles.class)
        .build();

    inputCollection = sequenceBuilder.elementCollection(InputCaseParameter.class)
        .build();

    outputCollection = sequenceBuilder.elementCollection(OutputCaseParameter.class)
        .build();

    typeBuilder.build();
  }

  @Override
  public String getOperatonHistoryTimeToLiveString() {
    return operatonHistoryTimeToLive.getValue(this);
  }

  @Override
  public void setOperatonHistoryTimeToLiveString(String historyTimeToLive) {
    operatonHistoryTimeToLive.setValue(this, historyTimeToLive);
  }

}
