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
package org.operaton.bpm.engine.impl.dmn.batch;

import java.util.List;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.batch.AbstractBatchConfigurationObjectConverter;
import org.operaton.bpm.engine.impl.batch.BatchConfiguration;
import org.operaton.bpm.engine.impl.batch.DeploymentMappingJsonConverter;
import org.operaton.bpm.engine.impl.batch.DeploymentMappings;
import org.operaton.bpm.engine.impl.util.JsonUtil;

public class DeleteHistoricDecisionInstanceBatchConfigurationJsonConverter extends AbstractBatchConfigurationObjectConverter<BatchConfiguration> {

  public static final String HISTORIC_DECISION_INSTANCE_IDS = "historicDecisionInstanceIds";
  public static final String HISTORIC_DECISION_INSTANCE_ID_MAPPINGS = "historicDecisionInstanceIdMappingss";

  @Override
  public JsonObject writeConfiguration(BatchConfiguration configuration) {
    JsonObject json = JsonUtil.createObject();
    JsonUtil.addListField(json, HISTORIC_DECISION_INSTANCE_IDS, configuration.getIds());
    JsonUtil.addListField(json, HISTORIC_DECISION_INSTANCE_ID_MAPPINGS, DeploymentMappingJsonConverter.INSTANCE, configuration.getIdMappings());
    return json;
  }

  @Override
  public BatchConfiguration readConfiguration(JsonObject json) {
    return new BatchConfiguration(readDecisionInstanceIds(json), readMappings(json));
  }

  protected List<String> readDecisionInstanceIds(JsonObject jsonNode) {
    return JsonUtil.asStringList(JsonUtil.getArray(jsonNode, HISTORIC_DECISION_INSTANCE_IDS));
  }

  protected DeploymentMappings readMappings(JsonObject jsonNode) {
    return JsonUtil.asList(JsonUtil.getArray(jsonNode, HISTORIC_DECISION_INSTANCE_ID_MAPPINGS), DeploymentMappingJsonConverter.INSTANCE, DeploymentMappings::new);
  }

}
