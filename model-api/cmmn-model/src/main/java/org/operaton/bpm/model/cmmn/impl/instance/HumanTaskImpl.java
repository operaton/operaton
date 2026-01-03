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
import java.util.List;

import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.PlanningTable;
import org.operaton.bpm.model.cmmn.instance.Role;
import org.operaton.bpm.model.cmmn.instance.Task;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.StringUtil;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.*;

/**
 * @author Roman Smirnov
 *
 */
public class HumanTaskImpl extends TaskImpl implements HumanTask {

  protected static AttributeReference<Role> performerRefAttribute;

  // cmmn 1.0
  /**
   * @deprecated since 1.0, use planningTableChild instead.
   */
  @Deprecated(since = "1.0")
  protected static ChildElementCollection<PlanningTable> planningTableCollection;

  // cmmn 1.1
  protected static ChildElement<PlanningTable> planningTableChild;

  /** operaton extensions */
  protected static Attribute<String> operatonAssigneeAttribute;
  protected static Attribute<String> operatonCandidateGroupsAttribute;
  protected static Attribute<String> operatonCandidateUsersAttribute;
  protected static Attribute<String> operatonDueDateAttribute;
  protected static Attribute<String> operatonFollowUpDateAttribute;
  protected static Attribute<String> operatonFormKeyAttribute;
  protected static Attribute<String> operatonPriorityAttribute;

  public HumanTaskImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Role getPerformer() {
    return performerRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setPerformer(Role performer) {
    performerRefAttribute.setReferenceTargetElement(this, performer);
  }

  @Override
  public Collection<PlanningTable> getPlanningTables() {
    return planningTableCollection.get(this);
  }

  @Override
  public PlanningTable getPlanningTable() {
    return planningTableChild.getChild(this);
  }

  @Override
  public void setPlanningTable(PlanningTable planningTable) {
    planningTableChild.setChild(this, planningTable);
  }

  /** operaton extensions */

  @Override
  public String getOperatonAssignee() {
    return operatonAssigneeAttribute.getValue(this);
  }

  @Override
  public void setOperatonAssignee(String operatonAssignee) {
    operatonAssigneeAttribute.setValue(this, operatonAssignee);
  }

  @Override
  public String getOperatonCandidateGroups() {
    return operatonCandidateGroupsAttribute.getValue(this);
  }

  @Override
  public void setOperatonCandidateGroups(String operatonCandidateGroups) {
    operatonCandidateGroupsAttribute.setValue(this, operatonCandidateGroups);
  }

  @Override
  public List<String> getOperatonCandidateGroupsList() {
    String candidateGroups = operatonCandidateGroupsAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(candidateGroups);
  }

  @Override
  public void setOperatonCandidateGroupsList(List<String> operatonCandidateGroupsList) {
    String candidateGroups = StringUtil.joinCommaSeparatedList(operatonCandidateGroupsList);
    operatonCandidateGroupsAttribute.setValue(this, candidateGroups);
  }

  @Override
  public String getOperatonCandidateUsers() {
    return operatonCandidateUsersAttribute.getValue(this);
  }

  @Override
  public void setOperatonCandidateUsers(String operatonCandidateUsers) {
    operatonCandidateUsersAttribute.setValue(this, operatonCandidateUsers);
  }

  @Override
  public List<String> getOperatonCandidateUsersList() {
    String candidateUsers = operatonCandidateUsersAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(candidateUsers);
  }

  @Override
  public void setOperatonCandidateUsersList(List<String> operatonCandidateUsersList) {
    String candidateUsers = StringUtil.joinCommaSeparatedList(operatonCandidateUsersList);
    operatonCandidateUsersAttribute.setValue(this, candidateUsers);
  }

  @Override
  public String getOperatonDueDate() {
    return operatonDueDateAttribute.getValue(this);
  }

  @Override
  public void setOperatonDueDate(String operatonDueDate) {
    operatonDueDateAttribute.setValue(this, operatonDueDate);
  }

  @Override
  public String getOperatonFollowUpDate() {
    return operatonFollowUpDateAttribute.getValue(this);
  }

  @Override
  public void setOperatonFollowUpDate(String operatonFollowUpDate) {
    operatonFollowUpDateAttribute.setValue(this, operatonFollowUpDate);
  }

  @Override
  public String getOperatonFormKey() {
    return operatonFormKeyAttribute.getValue(this);
  }

  @Override
  public void setOperatonFormKey(String operatonFormKey) {
    operatonFormKeyAttribute.setValue(this, operatonFormKey);
  }

  @Override
  public String getOperatonPriority() {
    return operatonPriorityAttribute.getValue(this);
  }

  @Override
  public void setOperatonPriority(String operatonPriority) {
    operatonPriorityAttribute.setValue(this, operatonPriority);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(HumanTask.class, CMMN_ELEMENT_HUMAN_TASK)
        .namespaceUri(CMMN11_NS)
        .extendsType(Task.class)
        .instanceProvider(HumanTaskImpl::new);

    performerRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_PERFORMER_REF)
        .idAttributeReference(Role.class)
        .build();

    /** operaton extensions */

    operatonAssigneeAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_ASSIGNEE)
      .namespace(OPERATON_NS)
      .build();

    operatonCandidateGroupsAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CANDIDATE_GROUPS)
      .namespace(OPERATON_NS)
      .build();

    operatonCandidateUsersAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CANDIDATE_USERS)
      .namespace(OPERATON_NS)
      .build();

    operatonDueDateAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_DUE_DATE)
      .namespace(OPERATON_NS)
      .build();

    operatonFollowUpDateAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FOLLOW_UP_DATE)
      .namespace(OPERATON_NS)
      .build();

    operatonFormKeyAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_KEY)
      .namespace(OPERATON_NS)
      .build();

    operatonPriorityAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    planningTableCollection = sequenceBuilder.elementCollection(PlanningTable.class)
        .build();

    planningTableChild = sequenceBuilder.element(PlanningTable.class)
        .minOccurs(0)
        .maxOccurs(1)
        .build();

    typeBuilder.build();
  }

}
