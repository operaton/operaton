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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.List;

import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.IncidentStatistics;

public class DeploymentStatisticsEntity extends DeploymentEntity implements DeploymentStatistics {

  private static final long serialVersionUID = 1L;

  protected int instances;
  protected int failedJobs;
  protected List<IncidentStatistics> incidentStatistics;

  @Override
  public int getInstances() {
    return instances;
  }
  public void setInstances(int instances) {
    this.instances = instances;
  }

  @Override
  public int getFailedJobs() {
    return failedJobs;
  }
  public void setFailedJobs(int failedJobs) {
    this.failedJobs = failedJobs;
  }

  @Override
  public List<IncidentStatistics> getIncidentStatistics() {
    return incidentStatistics;
  }
  public void setIncidentStatistics(List<IncidentStatistics> incidentStatistics) {
    this.incidentStatistics = incidentStatistics;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[instances=" + instances
           + ", failedJobs=" + failedJobs
           + ", id=" + id
           + ", name=" + name
           + ", resources=" + resources
           + ", deploymentTime=" + deploymentTime
           + ", validatingSchema=" + validatingSchema
           + ", isNew=" + isNew
           + "]";
  }
}
