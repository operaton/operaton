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
package org.operaton.bpm.model.bpmn.impl.instance;

import java.util.Collection;
import java.util.List;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.ProcessType;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.instance.Artifact;
import org.operaton.bpm.model.bpmn.instance.Auditing;
import org.operaton.bpm.model.bpmn.instance.CallableElement;
import org.operaton.bpm.model.bpmn.instance.CorrelationSubscription;
import org.operaton.bpm.model.bpmn.instance.FlowElement;
import org.operaton.bpm.model.bpmn.instance.LaneSet;
import org.operaton.bpm.model.bpmn.instance.Monitoring;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.Property;
import org.operaton.bpm.model.bpmn.instance.ResourceRole;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.StringUtil;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMN process element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public class ProcessImpl extends CallableElementImpl implements Process {

  public static final String DEFAULT_HISTORY_TIME_TO_LIVE = "P180D";

  protected static Attribute<ProcessType> processTypeAttribute;
  protected static Attribute<Boolean> isClosedAttribute;
  protected static Attribute<Boolean> isExecutableAttribute;
  protected static ChildElement<Auditing> auditingChild;
  protected static ChildElement<Monitoring> monitoringChild;
  protected static ChildElementCollection<Property> propertyCollection;
  protected static ChildElementCollection<LaneSet> laneSetCollection;
  protected static ChildElementCollection<FlowElement> flowElementCollection;
  protected static ChildElementCollection<Artifact> artifactCollection;
  protected static ChildElementCollection<ResourceRole> resourceRoleCollection;
  protected static ChildElementCollection<CorrelationSubscription> correlationSubscriptionCollection;
  protected static ElementReferenceCollection<Process, Supports> supportsCollection;

  /** operaton extensions */

  protected static Attribute<String> operatonCandidateStarterGroupsAttribute;
  protected static Attribute<String> operatonCandidateStarterUsersAttribute;
  protected static Attribute<String> operatonJobPriorityAttribute;
  protected static Attribute<String> operatonTaskPriorityAttribute;
  protected static Attribute<String> operatonHistoryTimeToLiveAttribute;
  protected static Attribute<Boolean> operatonIsStartableInTasklistAttribute;
  protected static Attribute<String> operatonVersionTagAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Process.class, BPMN_ELEMENT_PROCESS)
      .namespaceUri(BPMN20_NS)
      .extendsType(CallableElement.class)
      .instanceProvider(ProcessImpl::new);

    processTypeAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_PROCESS_TYPE, ProcessType.class)
      .defaultValue(ProcessType.None)
      .build();

    isClosedAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_CLOSED)
      .defaultValue(false)
      .build();

    isExecutableAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_EXECUTABLE)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    auditingChild = sequenceBuilder.element(Auditing.class)
      .build();

    monitoringChild = sequenceBuilder.element(Monitoring.class)
      .build();

    propertyCollection = sequenceBuilder.elementCollection(Property.class)
      .build();

    laneSetCollection = sequenceBuilder.elementCollection(LaneSet.class)
      .build();

    flowElementCollection = sequenceBuilder.elementCollection(FlowElement.class)
      .build();

    artifactCollection = sequenceBuilder.elementCollection(Artifact.class)
      .build();

    resourceRoleCollection = sequenceBuilder.elementCollection(ResourceRole.class)
      .build();

    correlationSubscriptionCollection = sequenceBuilder.elementCollection(CorrelationSubscription.class)
      .build();

    supportsCollection = sequenceBuilder.elementCollection(Supports.class)
      .qNameElementReferenceCollection(Process.class)
      .build();

    /** operaton extensions */

    operatonCandidateStarterGroupsAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CANDIDATE_STARTER_GROUPS)
      .namespace(OPERATON_NS)
      .build();

    operatonCandidateStarterUsersAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_CANDIDATE_STARTER_USERS)
      .namespace(OPERATON_NS)
      .build();

    operatonJobPriorityAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_JOB_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    operatonTaskPriorityAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TASK_PRIORITY)
      .namespace(OPERATON_NS)
      .build();

    operatonHistoryTimeToLiveAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE)
      .namespace(OPERATON_NS)
      .build();

    operatonIsStartableInTasklistAttribute = typeBuilder.booleanAttribute(OPERATON_ATTRIBUTE_IS_STARTABLE_IN_TASKLIST)
      .defaultValue(true)
      .namespace(OPERATON_NS)
      .build();

    operatonVersionTagAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VERSION_TAG)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

  public ProcessImpl(ModelTypeInstanceContext context) {
    super(context);
  }

  @Override
  public ProcessBuilder builder() {
    return new ProcessBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public ProcessType getProcessType() {
    return processTypeAttribute.getValue(this);
  }

  @Override
  public void setProcessType(ProcessType processType) {
    processTypeAttribute.setValue(this, processType);
  }

  @Override
  public boolean isClosed() {
    return isClosedAttribute.getValue(this);
  }

  @Override
  public void setClosed(boolean closed) {
    isClosedAttribute.setValue(this, closed);
  }

  @Override
  public boolean isExecutable() {
    return isExecutableAttribute.getValue(this);
  }

  @Override
  public void setExecutable(boolean executable) {
    isExecutableAttribute.setValue(this, executable);
  }

  @Override
  public Auditing getAuditing() {
    return auditingChild.getChild(this);
  }

  @Override
  public void setAuditing(Auditing auditing) {
    auditingChild.setChild(this, auditing);
  }

  @Override
  public Monitoring getMonitoring() {
    return monitoringChild.getChild(this);
  }

  @Override
  public void setMonitoring(Monitoring monitoring) {
    monitoringChild.setChild(this, monitoring);
  }

  @Override
  public Collection<Property> getProperties() {
    return propertyCollection.get(this);
  }

  @Override
  public Collection<LaneSet> getLaneSets() {
    return laneSetCollection.get(this);
  }

  @Override
  public Collection<FlowElement> getFlowElements() {
    return flowElementCollection.get(this);
  }

  @Override
  public Collection<Artifact> getArtifacts() {
    return artifactCollection.get(this);
  }

  @Override
  public Collection<CorrelationSubscription> getCorrelationSubscriptions() {
    return correlationSubscriptionCollection.get(this);
  }

  @Override
  public Collection<ResourceRole> getResourceRoles() {
    return resourceRoleCollection.get(this);
  }

  @Override
  public Collection<Process> getSupports() {
    return supportsCollection.getReferenceTargetElements(this);
  }

  /** operaton extensions */

  @Override
  public String getOperatonCandidateStarterGroups() {
    return operatonCandidateStarterGroupsAttribute.getValue(this);
  }

  @Override
  public void setOperatonCandidateStarterGroups(String operatonCandidateStarterGroups) {
    operatonCandidateStarterGroupsAttribute.setValue(this, operatonCandidateStarterGroups);
  }

  @Override
  public List<String> getOperatonCandidateStarterGroupsList() {
    String groupsString = operatonCandidateStarterGroupsAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(groupsString);
  }

  @Override
  public void setOperatonCandidateStarterGroupsList(List<String> operatonCandidateStarterGroupsList) {
    String candidateStarterGroups = StringUtil.joinCommaSeparatedList(operatonCandidateStarterGroupsList);
    operatonCandidateStarterGroupsAttribute.setValue(this, candidateStarterGroups);
  }

  @Override
  public String getOperatonCandidateStarterUsers() {
    return operatonCandidateStarterUsersAttribute.getValue(this);
  }

  @Override
  public void setOperatonCandidateStarterUsers(String operatonCandidateStarterUsers) {
    operatonCandidateStarterUsersAttribute.setValue(this, operatonCandidateStarterUsers);
  }

  @Override
  public List<String> getOperatonCandidateStarterUsersList() {
    String candidateStarterUsers = operatonCandidateStarterUsersAttribute.getValue(this);
    return StringUtil.splitCommaSeparatedList(candidateStarterUsers);
  }

  @Override
  public void setOperatonCandidateStarterUsersList(List<String> operatonCandidateStarterUsersList) {
    String candidateStarterUsers = StringUtil.joinCommaSeparatedList(operatonCandidateStarterUsersList);
    operatonCandidateStarterUsersAttribute.setValue(this, candidateStarterUsers);
  }

  @Override
  public String getOperatonJobPriority() {
    return operatonJobPriorityAttribute.getValue(this);
  }

  @Override
  public void setOperatonJobPriority(String jobPriority) {
    operatonJobPriorityAttribute.setValue(this, jobPriority);
  }

  @Override
  public String getOperatonTaskPriority() {
    return operatonTaskPriorityAttribute.getValue(this);
  }

  @Override
  public void setOperatonTaskPriority(String taskPriority) {
    operatonTaskPriorityAttribute.setValue(this, taskPriority);
  }

  @Override
  public String getOperatonHistoryTimeToLiveString() {
    return operatonHistoryTimeToLiveAttribute.getValue(this);
  }

  @Override
  public void setOperatonHistoryTimeToLiveString(String historyTimeToLive) {
    if (historyTimeToLive == null) {
      operatonHistoryTimeToLiveAttribute.removeAttribute(this);
    } else {
      operatonHistoryTimeToLiveAttribute.setValue(this, historyTimeToLive);
    }
  }

  @Override
  public Boolean isOperatonStartableInTasklist() {
    return operatonIsStartableInTasklistAttribute.getValue(this);
  }

  @Override
  public void setOperatonIsStartableInTasklist(Boolean isStartableInTasklist) {
    operatonIsStartableInTasklistAttribute.setValue(this, isStartableInTasklist);
  }

  @Override
  public String getOperatonVersionTag() {
    return operatonVersionTagAttribute.getValue(this);
  }

  @Override
  public void setOperatonVersionTag(String versionTag) {
    operatonVersionTagAttribute.setValue(this, versionTag);
  }
}
