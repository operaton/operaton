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
package org.operaton.bpm.engine.rest.dto.repository;

import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.management.ProcessDefinitionStatistics;
import org.operaton.bpm.engine.rest.dto.StatisticsResultDto;

import java.util.ArrayList;

public class ProcessDefinitionStatisticsResultDto extends StatisticsResultDto {

  private ProcessDefinitionDto definition;

  public ProcessDefinitionDto getDefinition() {
    return definition;
  }

  public void setDefinition(ProcessDefinitionDto definition) {
    this.definition = definition;
  }

  public static ProcessDefinitionStatisticsResultDto fromProcessDefinitionStatistics(ProcessDefinitionStatistics statistics) {
    ProcessDefinitionStatisticsResultDto dto = new ProcessDefinitionStatisticsResultDto();

    dto.definition = ProcessDefinitionDto.fromProcessDefinition(statistics);
    dto.id = statistics.getId();
    dto.instances = statistics.getInstances();
    dto.failedJobs = statistics.getFailedJobs();

    dto.incidents = new ArrayList<>();
    for (IncidentStatistics incident : statistics.getIncidentStatistics()) {
      IncidentStatisticsResultDto incidentDto = IncidentStatisticsResultDto.fromIncidentStatistics(incident);
      dto.incidents.add(incidentDto);
    }

    return dto;
  }
}
