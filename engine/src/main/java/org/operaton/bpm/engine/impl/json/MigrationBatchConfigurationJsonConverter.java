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

import java.util.List;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.batch.AbstractBatchConfigurationObjectConverter;
import org.operaton.bpm.engine.impl.batch.DeploymentMappingJsonConverter;
import org.operaton.bpm.engine.impl.batch.DeploymentMappings;
import org.operaton.bpm.engine.impl.migration.batch.MigrationBatchConfiguration;
import org.operaton.bpm.engine.impl.util.JsonUtil;

public class MigrationBatchConfigurationJsonConverter
  extends AbstractBatchConfigurationObjectConverter<MigrationBatchConfiguration> {

  public static final String MIGRATION_PLAN = "migrationPlan";
  public static final String PROCESS_INSTANCE_IDS = "processInstanceIds";
  public static final String PROCESS_INSTANCE_ID_MAPPINGS = "processInstanceIdMappings";
  public static final String SKIP_LISTENERS = "skipListeners";
  public static final String SKIP_IO_MAPPINGS = "skipIoMappings";

  @Override
  public JsonObject writeConfiguration(MigrationBatchConfiguration configuration) {
    JsonObject json = JsonUtil.createObject();

    JsonUtil.addField(json, MIGRATION_PLAN, MigrationPlanJsonConverter.INSTANCE, configuration.getMigrationPlan());
    JsonUtil.addListField(json, PROCESS_INSTANCE_IDS, configuration.getIds());
    JsonUtil.addListField(json, PROCESS_INSTANCE_ID_MAPPINGS, DeploymentMappingJsonConverter.INSTANCE, configuration.getIdMappings());
    JsonUtil.addField(json, SKIP_LISTENERS, configuration.isSkipCustomListeners());
    JsonUtil.addField(json, SKIP_IO_MAPPINGS, configuration.isSkipIoMappings());

    return json;
  }

  @Override
  public MigrationBatchConfiguration readConfiguration(JsonObject json) {
    return new MigrationBatchConfiguration(
        readProcessInstanceIds(json),
        readIdMappings(json),
        JsonUtil.asJavaObject(JsonUtil.getObject(json, MIGRATION_PLAN), MigrationPlanJsonConverter.INSTANCE),
        JsonUtil.getBoolean(json, SKIP_LISTENERS),
        JsonUtil.getBoolean(json, SKIP_IO_MAPPINGS));
  }

  protected List<String> readProcessInstanceIds(JsonObject jsonObject) {
    return JsonUtil.asStringList(JsonUtil.getArray(jsonObject, PROCESS_INSTANCE_IDS));
  }

  protected DeploymentMappings readIdMappings(JsonObject json) {
    return JsonUtil.asList(JsonUtil.getArray(json, PROCESS_INSTANCE_ID_MAPPINGS), DeploymentMappingJsonConverter.INSTANCE, DeploymentMappings::new);
  }
}
