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

/**
 * @author Roman Smirnov
 *
 */
public interface Case extends CmmnElement {

  String getName();

  void setName(String name);

  @Deprecated(since = "1.0")
  Collection<CaseRole> getCaseRoles();

  CaseRoles getRoles();

  void setRoles(CaseRoles roles);

  Collection<InputCaseParameter> getInputs();

  Collection<OutputCaseParameter> getOutputs();

  CasePlanModel getCasePlanModel();

  void setCasePlanModel(CasePlanModel casePlanModel);

  CaseFileModel getCaseFileModel();

  void setCaseFileModel(CaseFileModel caseFileModel);

  /**
   * @deprecated use {@link #getOperatonHistoryTimeToLiveString()} instead
   */
  @Deprecated(since = "1.0", forRemoval = true)
  default Integer getOperatonHistoryTimeToLive() {
    String ttl = getOperatonHistoryTimeToLiveString();
    if (ttl != null) {
      return Integer.parseInt(ttl);
    }
    return null;

  }

  /**
   * @deprecated use {@link #setOperatonHistoryTimeToLiveString(String)} instead
   */
  @Deprecated(since = "1.0", forRemoval = true)
  default void setOperatonHistoryTimeToLive(Integer historyTimeToLive) {
    setOperatonHistoryTimeToLiveString(historyTimeToLive != null ? String.valueOf(historyTimeToLive) : null);
  }

  String getOperatonHistoryTimeToLiveString();

  void setOperatonHistoryTimeToLiveString(String historyTimeToLive);
}
