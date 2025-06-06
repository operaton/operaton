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
package org.operaton.bpm.engine.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.migration.MigrationExecutionDto;
import org.operaton.bpm.engine.rest.dto.migration.MigrationPlanDto;
import org.operaton.bpm.engine.rest.dto.migration.MigrationPlanGenerationDto;
import org.operaton.bpm.engine.rest.dto.migration.MigrationPlanReportDto;

public interface MigrationRestService {

  String PATH = "/migration";

  @POST
  @Path("/generate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  MigrationPlanDto generateMigrationPlan(MigrationPlanGenerationDto generationDto);

  @POST
  @Path("/validate")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  MigrationPlanReportDto validateMigrationPlan(MigrationPlanDto migrationPlanDto);

  @POST
  @Path("/execute")
  @Consumes(MediaType.APPLICATION_JSON)
  void executeMigrationPlan(MigrationExecutionDto migrationPlan);

  @POST
  @Path("/executeAsync")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BatchDto executeMigrationPlanAsync(MigrationExecutionDto migrationPlan);

}
