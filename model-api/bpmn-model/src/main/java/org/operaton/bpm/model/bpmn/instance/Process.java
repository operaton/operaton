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
package org.operaton.bpm.model.bpmn.instance;

import java.util.Collection;
import java.util.List;

import org.operaton.bpm.model.bpmn.ProcessType;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;


/**
 * The BPMN process element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public interface Process extends CallableElement {

  @Override
  ProcessBuilder builder();

  ProcessType getProcessType();

  void setProcessType(ProcessType processType);

  boolean isClosed();

  void setClosed(boolean closed);

  boolean isExecutable();

  void setExecutable(boolean executable);

  // TODO: collaboration ref

  Auditing getAuditing();

  void setAuditing(Auditing auditing);

  Monitoring getMonitoring();

  void setMonitoring(Monitoring monitoring);

  Collection<Property> getProperties();

  Collection<LaneSet> getLaneSets();

  Collection<FlowElement> getFlowElements();

  Collection<Artifact> getArtifacts();

  Collection<CorrelationSubscription> getCorrelationSubscriptions();

  Collection<ResourceRole> getResourceRoles();

  Collection<Process> getSupports();

  /** operaton extensions */

  String getOperatonCandidateStarterGroups();

  void setOperatonCandidateStarterGroups(String operatonCandidateStarterGroups);

  List<String> getOperatonCandidateStarterGroupsList();

  void setOperatonCandidateStarterGroupsList(List<String> operatonCandidateStarterGroupsList);

  String getOperatonCandidateStarterUsers();

  void setOperatonCandidateStarterUsers(String operatonCandidateStarterUsers);

  List<String> getOperatonCandidateStarterUsersList();

  void setOperatonCandidateStarterUsersList(List<String> operatonCandidateStarterUsersList);

  String getOperatonJobPriority();

  void setOperatonJobPriority(String jobPriority);

  String getOperatonTaskPriority();

  void setOperatonTaskPriority(String taskPriority);

  /**
   * @deprecated since 1.0, use {@link #getOperatonHistoryTimeToLiveString()} instead
   */
  @Deprecated(since = "1.0")
  Integer getOperatonHistoryTimeToLive();

  /**
   * @deprecated since 1.0, use {@link #setOperatonHistoryTimeToLiveString(String)} instead
   */
  @Deprecated(since = "1.0")
  void setOperatonHistoryTimeToLive(Integer historyTimeToLive);

  String getOperatonHistoryTimeToLiveString();

  void setOperatonHistoryTimeToLiveString(String historyTimeToLive);

  Boolean isOperatonStartableInTasklist();

  void setOperatonIsStartableInTasklist(Boolean isStartableInTasklist);

  String getOperatonVersionTag();

  void setOperatonVersionTag(String versionTag);
}
