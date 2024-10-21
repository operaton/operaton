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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_ASSIGNEE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_CANDIDATE_GROUPS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_CANDIDATE_USERS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_DUE_DATE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_FOLLOW_UP_DATE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_FORM_KEY;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_PRIORITY;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ATTRIBUTE_PERFORMER_REF;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_HUMAN_TASK;

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
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Roman Smirnov
 *
 */
public class HumanTaskImpl extends TaskImpl implements HumanTask {

  protected static AttributeReference<Role> performerRefAttribute;

  // cmmn 1.0
  @Deprecated
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

  public Role getPerformer() {
    return performerRefAttribute.getReferenceTargetElement(this);
  }

  public void setPerformer(Role performer) {
    performerRefAttribute.setReferenceTargetElement(this, performer);
  }

  public Collection<PlanningTable> getPlanningTables() {
    return planningTableCollection.get(this);
  }

  public PlanningTable getPlanningTable() {
    return planningTableChild.getChild(this);
  }

  public void setPlanningTable(PlanningTable planningTable) {
    planningTableChild.setChild(this, planningTable);
  }

  /** operaton extensions */

  public String getOperatonAssignee() {
    return operatonAssigneeAttribute.getValue(this);
  }

  public void setOperatonAssignee(String operatonAssignee) {
    operatonAssigneeAttribute.setValue(this, operatonAssignee);
  }

  public String getOperatonCandidateGroups() {
    return operatonCandidateGroupsAttribute.getValue(this);
  }

  public void setOperatonCandidateGroups(String operatonCandidateGroups) {
    operatonCandidateGroupsAttribute.setValue(this, operatonCandidateGroups);
  }

  public List<String> getOperatonCandidateGroupsList() {
    String candidateGroups = operatonCandidateGroupsAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(candidateGroups);
  }

  public void setOperatonCandidateGroupsList(List<String> operatonCandidateGroupsList) {
    String candidateGroups = StringUtil.joinCommaSeparatedList(operatonCandidateGroupsList);
    operatonCandidateGroupsAttribute.setValue(this, candidateGroups);
  }

  public String getOperatonCandidateUsers() {
    return operatonCandidateUsersAttribute.getValue(this);
  }

  public void setOperatonCandidateUsers(String operatonCandidateUsers) {
    operatonCandidateUsersAttribute.setValue(this, operatonCandidateUsers);
  }

  public List<String> getOperatonCandidateUsersList() {
    String candidateUsers = operatonCandidateUsersAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(candidateUsers);
  }

  public void setOperatonCandidateUsersList(List<String> operatonCandidateUsersList) {
    String candidateUsers = StringUtil.joinCommaSeparatedList(operatonCandidateUsersList);
    operatonCandidateUsersAttribute.setValue(this, candidateUsers);
  }

  public String getOperatonDueDate() {
    return operatonDueDateAttribute.getValue(this);
  }

  public void setOperatonDueDate(String operatonDueDate) {
    operatonDueDateAttribute.setValue(this, operatonDueDate);
  }

  public String getOperatonFollowUpDate() {
    return operatonFollowUpDateAttribute.getValue(this);
  }

  public void setOperatonFollowUpDate(String operatonFollowUpDate) {
    operatonFollowUpDateAttribute.setValue(this, operatonFollowUpDate);
  }

  public String getOperatonFormKey() {
    return operatonFormKeyAttribute.getValue(this);
  }

  public void setOperatonFormKey(String operatonFormKey) {
    operatonFormKeyAttribute.setValue(this, operatonFormKey);
  }

  public String getOperatonPriority() {
    return operatonPriorityAttribute.getValue(this);
  }

  public void setOperatonPriority(String operatonPriority) {
    operatonPriorityAttribute.setValue(this, operatonPriority);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(HumanTask.class, CMMN_ELEMENT_HUMAN_TASK)
        .namespaceUri(CMMN11_NS)
        .extendsType(Task.class)
        .instanceProvider(new ModelTypeInstanceProvider<HumanTask>() {
          public HumanTask newInstance(ModelTypeInstanceContext instanceContext) {
            return new HumanTaskImpl(instanceContext);
          }
        });

    performerRefAttribute = typeBuilder.stringAttribute(CMMN_ATTRIBUTE_PERFORMER_REF)
        .idAttributeReference(Role.class)
        .build();

    /** operaton extensions */

    operatonAssigneeAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_ASSIGNEE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonCandidateGroupsAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_CANDIDATE_GROUPS)
      .namespace(CAMUNDA_NS)
      .build();

    operatonCandidateUsersAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_CANDIDATE_USERS)
      .namespace(CAMUNDA_NS)
      .build();

    operatonDueDateAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_DUE_DATE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonFollowUpDateAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_FOLLOW_UP_DATE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonFormKeyAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_FORM_KEY)
      .namespace(CAMUNDA_NS)
      .build();

    operatonPriorityAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_PRIORITY)
      .namespace(CAMUNDA_NS)
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