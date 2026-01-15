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
package org.operaton.bpm.engine.impl.batch.update;

import java.util.List;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.batch.AbstractBatchConfigurationObjectConverter;
import org.operaton.bpm.engine.impl.batch.DeploymentMappingJsonConverter;
import org.operaton.bpm.engine.impl.batch.DeploymentMappings;
import org.operaton.bpm.engine.impl.util.JsonUtil;

public class UpdateProcessInstancesSuspendStateBatchConfigurationJsonConverter
  extends AbstractBatchConfigurationObjectConverter<UpdateProcessInstancesSuspendStateBatchConfiguration> {

  public static final String PROCESS_INSTANCE_IDS = "processInstanceIds";
  public static final String PROCESS_INSTANCE_ID_MAPPINGS = "processInstanceIdMappings";
  public static final String SUSPENDING = "suspended";

  @Override
  public JsonObject writeConfiguration(UpdateProcessInstancesSuspendStateBatchConfiguration configuration) {
    JsonObject json = JsonUtil.createObject();

    JsonUtil.addListField(json, PROCESS_INSTANCE_IDS, configuration.getIds());
    JsonUtil.addListField(json, PROCESS_INSTANCE_ID_MAPPINGS, DeploymentMappingJsonConverter.INSTANCE, configuration.getIdMappings());
    JsonUtil.addField(json, SUSPENDING, configuration.getSuspended());
    return json;
  }

  @Override
  public UpdateProcessInstancesSuspendStateBatchConfiguration readConfiguration(JsonObject json) {
    return new UpdateProcessInstancesSuspendStateBatchConfiguration(readProcessInstanceIds(json), readMappings(json),
          JsonUtil.getBoolean(json, SUSPENDING));
  }

  protected List<String> readProcessInstanceIds(JsonObject jsonObject) {
    return JsonUtil.asStringList(JsonUtil.getArray(jsonObject, PROCESS_INSTANCE_IDS));
  }

  protected DeploymentMappings readMappings(JsonObject jsonObject) {
    return JsonUtil.asList(JsonUtil.getArray(jsonObject, PROCESS_INSTANCE_ID_MAPPINGS), DeploymentMappingJsonConverter.INSTANCE, DeploymentMappings::new);
  }
}
