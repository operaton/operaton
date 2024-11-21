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

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_USER_TASK;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_ASSIGNEE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_CANDIDATE_GROUPS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_CANDIDATE_USERS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_DUE_DATE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FOLLOW_UP_DATE;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FORM_HANDLER_CLASS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FORM_KEY;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FORM_REF;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FORM_REF_BINDING;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_FORM_REF_VERSION;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ATTRIBUTE_PRIORITY;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

import java.util.Collection;
import java.util.List;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.UserTaskBuilder;
import org.operaton.bpm.model.bpmn.instance.Rendering;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.bpmn.instance.UserTask;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.StringUtil;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN userTask element
 *
 * @author Sebastian Menski
 */
public class UserTaskImpl extends TaskImpl implements UserTask {

  protected static Attribute<String> implementationAttribute;
  protected static ChildElementCollection<Rendering> renderingCollection;

  /** operaton extensions */

  protected static Attribute<String> operatonAssigneeAttribute;
  protected static Attribute<String> operatonCandidateGroupsAttribute;
  protected static Attribute<String> operatonCandidateUsersAttribute;
  protected static Attribute<String> operatonDueDateAttribute;
  protected static Attribute<String> operatonFollowUpDateAttribute;
  protected static Attribute<String> operatonFormHandlerClassAttribute;
  protected static Attribute<String> operatonFormKeyAttribute;
  protected static Attribute<String> operatonFormRefAttribute;
  protected static Attribute<String> operatonFormRefBindingAttribute;
  protected static Attribute<String> operatonFormRefVersionAttribute;
  protected static Attribute<String> operatonPriorityAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(UserTask.class, BPMN_ELEMENT_USER_TASK)
      .namespaceUri(BPMN20_NS)
      .extendsType(Task.class)
      .instanceProvider(new ModelTypeInstanceProvider<UserTask>() {
        public UserTask newInstance(ModelTypeInstanceContext instanceContext) {
          return new UserTaskImpl(instanceContext);
        }
      });

    implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
      .defaultValue("##unspecified")
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    renderingCollection = sequenceBuilder.elementCollection(Rendering.class)
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

    operatonFormHandlerClassAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_HANDLER_CLASS)
      .namespace(OPERATON_NS)
      .build();

    operatonFormKeyAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_KEY)
      .namespace(OPERATON_NS)
      .build();

    operatonFormRefAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_REF)
        .namespace(OPERATON_NS)
        .build();

    operatonFormRefBindingAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_REF_BINDING)
        .namespace(OPERATON_NS)
        .build();

    operatonFormRefVersionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_FORM_REF_VERSION)
        .namespace(OPERATON_NS)
        .build();

    operatonPriorityAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public UserTaskImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public UserTaskBuilder builder() {
    return new UserTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  public void setImplementation(String implementation) {
    implementationAttribute.setValue(this, implementation);
  }

  public Collection<Rendering> getRenderings() {
    return renderingCollection.get(this);
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

  public String getOperatonFormHandlerClass() {
    return operatonFormHandlerClassAttribute.getValue(this);
  }

  public void setOperatonFormHandlerClass(String operatonFormHandlerClass) {
    operatonFormHandlerClassAttribute.setValue(this, operatonFormHandlerClass);
  }

  public String getOperatonFormKey() {
    return operatonFormKeyAttribute.getValue(this);
  }

  public void setOperatonFormKey(String operatonFormKey) {
    operatonFormKeyAttribute.setValue(this, operatonFormKey);
  }

  public String getOperatonFormRef() {
    return operatonFormRefAttribute.getValue(this);
  }

  public void setOperatonFormRef(String operatonFormRef) {
    operatonFormRefAttribute.setValue(this, operatonFormRef);
  }

  public String getOperatonFormRefBinding() {
    return operatonFormRefBindingAttribute.getValue(this);
  }

  public void setOperatonFormRefBinding(String operatonFormRefBinding) {
    operatonFormRefBindingAttribute.setValue(this, operatonFormRefBinding);
  }

  public String getOperatonFormRefVersion() {
    return operatonFormRefVersionAttribute.getValue(this);
  }

  public void setOperatonFormRefVersion(String operatonFormRefVersion) {
    operatonFormRefVersionAttribute.setValue(this, operatonFormRefVersion);
  }

  public String getOperatonPriority() {
    return operatonPriorityAttribute.getValue(this);
  }

  public void setOperatonPriority(String operatonPriority) {
    operatonPriorityAttribute.setValue(this, operatonPriority);
  }
}
