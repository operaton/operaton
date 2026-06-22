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
package org.operaton.bpm.engine.impl.json;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.migration.MigrationPlanImpl;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.migration.MigrationPlan;

public class MigrationPlanJsonConverter implements JsonObjectConverter<MigrationPlan> {

  public static final String SOURCE_PROCESS_DEFINITION_ID = "sourceProcessDefinitionId";
  public static final String TARGET_PROCESS_DEFINITION_ID = "targetProcessDefinitionId";
  public static final String INSTRUCTIONS = "instructions";

  private static final MigrationInstructionJsonConverter MIGRATION_INSTRUCTION_JSON_CONVERTER = new MigrationInstructionJsonConverter();

  @Override
  public JsonObject toJsonObject(MigrationPlan migrationPlan) {
    JsonObject json = JsonUtil.createObject();

    JsonUtil.addField(json, SOURCE_PROCESS_DEFINITION_ID, migrationPlan.getSourceProcessDefinitionId());
    JsonUtil.addField(json, TARGET_PROCESS_DEFINITION_ID, migrationPlan.getTargetProcessDefinitionId());
    JsonUtil.addListField(json, INSTRUCTIONS, MIGRATION_INSTRUCTION_JSON_CONVERTER, migrationPlan.getInstructions());

    return json;
  }

  @Override
  public MigrationPlan toObject(JsonObject json) {
    MigrationPlanImpl migrationPlan = new MigrationPlanImpl(JsonUtil.getString(json, SOURCE_PROCESS_DEFINITION_ID), JsonUtil.getString(json, TARGET_PROCESS_DEFINITION_ID));

    migrationPlan.setInstructions(JsonUtil.asList(JsonUtil.getArray(json, INSTRUCTIONS), MIGRATION_INSTRUCTION_JSON_CONVERTER));

    return migrationPlan;
  }

}
