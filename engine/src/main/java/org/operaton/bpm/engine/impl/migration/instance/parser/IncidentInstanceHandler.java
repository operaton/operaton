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
package org.operaton.bpm.engine.impl.migration.instance.parser;

import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingExternalTaskInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingIncident;
import org.operaton.bpm.engine.impl.migration.instance.MigratingJobInstance;
import org.operaton.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobDefinitionEntity;

import static org.operaton.bpm.engine.runtime.Incident.EXTERNAL_TASK_HANDLER_TYPE;
import static org.operaton.bpm.engine.runtime.Incident.FAILED_JOB_HANDLER_TYPE;

/**
 * @author Thorben Lindhauer
 *
 */
public class IncidentInstanceHandler implements MigratingInstanceParseHandler<IncidentEntity> {

  @Override
  public void handle(MigratingInstanceParseContext parseContext, IncidentEntity incident) {
    if (incident.getConfiguration() != null && isFailedJobIncident(incident)) {
      handleFailedJobIncident(parseContext, incident);
    }
    else if (incident.getConfiguration() != null && isExternalTaskIncident(incident)) {
      handleExternalTaskIncident(parseContext, incident);
    }
    else {
      handleIncident(parseContext, incident);
    }
  }

  protected void handleIncident(MigratingInstanceParseContext parseContext, IncidentEntity incident) {
    MigratingActivityInstance owningInstance = parseContext.getMigratingActivityInstanceById(incident.getExecution().getActivityInstanceId());
    if (owningInstance != null) {
      parseContext.consume(incident);
      MigratingIncident migratingIncident = new MigratingIncident(incident, owningInstance.getTargetScope());
      owningInstance.addMigratingDependentInstance(migratingIncident);
    }
  }

  protected boolean isFailedJobIncident(IncidentEntity incident) {
    return FAILED_JOB_HANDLER_TYPE.equals(incident.getIncidentType());
  }

  protected void handleFailedJobIncident(MigratingInstanceParseContext parseContext, IncidentEntity incident) {
    MigratingJobInstance owningInstance = parseContext.getMigratingJobInstanceById(incident.getConfiguration());
    if (owningInstance != null) {
      parseContext.consume(incident);
      if (owningInstance.migrates()) {
        MigratingIncident migratingIncident = new MigratingIncident(incident, owningInstance.getTargetScope());
        JobDefinitionEntity targetJobDefinitionEntity = owningInstance.getTargetJobDefinitionEntity();
        if (targetJobDefinitionEntity != null) {
          migratingIncident.setTargetJobDefinitionId(targetJobDefinitionEntity.getId());
        }
        owningInstance.addMigratingDependentInstance(migratingIncident);
      }
    }
  }

  protected boolean isExternalTaskIncident(IncidentEntity incident) {
    return EXTERNAL_TASK_HANDLER_TYPE.equals(incident.getIncidentType());
  }

  protected void handleExternalTaskIncident(MigratingInstanceParseContext parseContext, IncidentEntity incident) {
    MigratingExternalTaskInstance owningInstance = parseContext.getMigratingExternalTaskInstanceById(incident.getConfiguration());
    if (owningInstance != null) {
      parseContext.consume(incident);
      MigratingIncident migratingIncident = new MigratingIncident(incident, owningInstance.getTargetScope());
      owningInstance.addMigratingDependentInstance(migratingIncident);
    }
  }

}
