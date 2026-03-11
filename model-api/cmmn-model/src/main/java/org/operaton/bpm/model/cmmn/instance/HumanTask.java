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
package org.operaton.bpm.model.cmmn.instance;

import java.util.Collection;
import java.util.List;

/**
 * @author Roman Smirnov
 *
 */
public interface HumanTask extends Task {

  Role getPerformer();

  void setPerformer(Role performerRef);

  /**
   * @deprecated since 1.0, use {@link #getPlanningTable()} instead, which returns a single PlanningTable.
   */
  @Deprecated(since = "1.0")
  Collection<PlanningTable> getPlanningTables();

  PlanningTable getPlanningTable();

  void setPlanningTable(PlanningTable planningTable);

  /** operaton extensions */

  String getOperatonAssignee();

  void setOperatonAssignee(String operatonAssignee);

  String getOperatonCandidateGroups();

  void setOperatonCandidateGroups(String operatonCandidateGroups);

  List<String> getOperatonCandidateGroupsList();

  void setOperatonCandidateGroupsList(List<String> operatonCandidateGroupsList);

  String getOperatonCandidateUsers();

  void setOperatonCandidateUsers(String operatonCandidateUsers);

  List<String> getOperatonCandidateUsersList();

  void setOperatonCandidateUsersList(List<String> operatonCandidateUsersList);

  String getOperatonDueDate();

  void setOperatonDueDate(String operatonDueDate);

  String getOperatonFollowUpDate();

  void setOperatonFollowUpDate(String operatonFollowUpDate);

  String getOperatonFormKey();

  void setOperatonFormKey(String operatonFormKey);

  String getOperatonPriority();

  void setOperatonPriority(String operatonPriority);

}
