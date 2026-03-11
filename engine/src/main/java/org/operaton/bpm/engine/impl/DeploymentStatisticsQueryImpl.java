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
package org.operaton.bpm.engine.impl;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.db.PermissionCheck;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.DeploymentStatisticsQuery;

public class DeploymentStatisticsQueryImpl extends AbstractQuery<DeploymentStatisticsQuery, DeploymentStatistics>
implements DeploymentStatisticsQuery {

  protected static final long serialVersionUID = 1L;
  protected boolean includeFailedJobs;
  protected boolean includeIncidents;
  protected String includeIncidentsForType;

  // for internal use
  protected transient List<PermissionCheck> processInstancePermissionChecks = new ArrayList<>();
  protected transient List<PermissionCheck> jobPermissionChecks = new ArrayList<>();
  protected transient List<PermissionCheck> incidentPermissionChecks = new ArrayList<>();

  public DeploymentStatisticsQueryImpl(CommandExecutor executor) {
    super(executor);
  }

  @Override
  public DeploymentStatisticsQuery includeFailedJobs() {
    includeFailedJobs = true;
    return this;
  }

  @Override
  public DeploymentStatisticsQuery includeIncidents() {
    includeIncidents = true;
    return this;
  }

  @Override
  public DeploymentStatisticsQuery includeIncidentsForType(String incidentType) {
    this.includeIncidentsForType = incidentType;
    return this;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return
        commandContext
          .getStatisticsManager()
          .getStatisticsCountGroupedByDeployment(this);
  }

  @Override
  public List<DeploymentStatistics> executeList(CommandContext commandContext,
      Page page) {
    checkQueryOk();
    return
      commandContext
        .getStatisticsManager()
        .getStatisticsGroupedByDeployment(this, page);
  }

  public boolean isFailedJobsToInclude() {
    return includeFailedJobs;
  }

  public boolean isIncidentsToInclude() {
    return includeIncidents || includeIncidentsForType != null;
  }

  @Override
  protected void checkQueryOk() {
    super.checkQueryOk();
    if (includeIncidents && includeIncidentsForType != null) {
      throw new ProcessEngineException("Invalid query: It is not possible to use includeIncident() and includeIncidentForType() to execute one query.");
    }
  }

  // getter/setter for authorization check

  public List<PermissionCheck> getProcessInstancePermissionChecks() {
    return processInstancePermissionChecks;
  }

  public void setProcessInstancePermissionChecks(List<PermissionCheck> processInstancePermissionChecks) {
    this.processInstancePermissionChecks = processInstancePermissionChecks;
  }

  public void addProcessInstancePermissionCheck(List<PermissionCheck> permissionChecks) {
    processInstancePermissionChecks.addAll(permissionChecks);
  }

  public List<PermissionCheck> getJobPermissionChecks() {
    return jobPermissionChecks;
  }

  public void setJobPermissionChecks(List<PermissionCheck> jobPermissionChecks) {
    this.jobPermissionChecks = jobPermissionChecks;
  }

  public void addJobPermissionCheck(List<PermissionCheck> permissionChecks) {
    jobPermissionChecks.addAll(permissionChecks);
  }

  public List<PermissionCheck> getIncidentPermissionChecks() {
    return incidentPermissionChecks;
  }

  public void setIncidentPermissionChecks(List<PermissionCheck> incidentPermissionChecks) {
    this.incidentPermissionChecks = incidentPermissionChecks;
  }

  public void addIncidentPermissionCheck(List<PermissionCheck> permissionChecks) {
    incidentPermissionChecks.addAll(permissionChecks);
  }

}
