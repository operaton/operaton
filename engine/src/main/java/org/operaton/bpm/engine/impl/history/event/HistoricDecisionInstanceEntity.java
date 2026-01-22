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
package org.operaton.bpm.engine.impl.history.event;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.history.HistoricDecisionInputInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionOutputInstance;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;

/**
 * History entry for an evaluated decision.
 *
 * @author Philipp Ossler
 * @author Ingo Richtsmeier
 *
 */
public class HistoricDecisionInstanceEntity extends HistoryEvent implements HistoricDecisionInstance {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  @Serial private static final long serialVersionUID = 1L;

  protected String decisionDefinitionId;
  protected String decisionDefinitionKey;
  protected String decisionDefinitionName;

  protected String activityInstanceId;
  protected String activityId;

  protected Date evaluationTime;

  protected Double collectResultValue;

  protected String rootDecisionInstanceId;
  protected String decisionRequirementsDefinitionId;
  protected String decisionRequirementsDefinitionKey;

  protected String userId;
  protected String tenantId;

  protected transient List<HistoricDecisionInputInstance> inputs;
  protected transient List<HistoricDecisionOutputInstance> outputs;

  @Override
  public String getDecisionDefinitionId() {
    return decisionDefinitionId;
  }

  public void setDecisionDefinitionId(String decisionDefinitionId) {
    this.decisionDefinitionId = decisionDefinitionId;
  }

  @Override
  public String getDecisionDefinitionKey() {
    return decisionDefinitionKey;
  }

  public void setDecisionDefinitionKey(String decisionDefinitionKey) {
    this.decisionDefinitionKey = decisionDefinitionKey;
  }

  @Override
  public String getDecisionDefinitionName() {
    return decisionDefinitionName;
  }

  public void setDecisionDefinitionName(String decisionDefinitionName) {
    this.decisionDefinitionName = decisionDefinitionName;
  }

  @Override
  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public void setActivityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
  }

  @Override
  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @Override
  public Date getEvaluationTime() {
    return evaluationTime;
  }

  public void setEvaluationTime(Date evaluationTime) {
    this.evaluationTime = evaluationTime;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public List<HistoricDecisionInputInstance> getInputs() {
    if(inputs != null) {
      return inputs;
    } else {
      throw LOG.historicDecisionInputInstancesNotFetchedException();
    }
  }

  @Override
  public List<HistoricDecisionOutputInstance> getOutputs() {
    if(outputs != null) {
      return outputs;
    } else {
      throw LOG.historicDecisionOutputInstancesNotFetchedException();
    }
  }

  public void setInputs(List<HistoricDecisionInputInstance> inputs) {
    this.inputs = inputs;
  }

  public void setOutputs(List<HistoricDecisionOutputInstance> outputs) {
    this.outputs = outputs;
  }

  public void delete() {
    Context
      .getCommandContext()
      .getDbEntityManager()
      .delete(this);
  }

  public void addInput(HistoricDecisionInputInstance decisionInputInstance) {
    if(inputs == null) {
      inputs = new ArrayList<>();
    }
    inputs.add(decisionInputInstance);
  }

  public void addOutput(HistoricDecisionOutputInstance decisionOutputInstance) {
    if(outputs == null) {
      outputs = new ArrayList<>();
    }
    outputs.add(decisionOutputInstance);
  }

  @Override
  public Double getCollectResultValue() {
    return collectResultValue;
  }

  public void setCollectResultValue(Double collectResultValue) {
    this.collectResultValue = collectResultValue;
  }

  @Override
  public String getRootDecisionInstanceId() {
    return rootDecisionInstanceId;
  }

  public void setRootDecisionInstanceId(String rootDecisionInstanceId) {
    this.rootDecisionInstanceId = rootDecisionInstanceId;
  }

  @Override
  public String getDecisionRequirementsDefinitionId() {
    return decisionRequirementsDefinitionId;
  }

  public void setDecisionRequirementsDefinitionId(String decisionRequirementsDefinitionId) {
    this.decisionRequirementsDefinitionId = decisionRequirementsDefinitionId;
  }

  @Override
  public String getDecisionRequirementsDefinitionKey() {
    return decisionRequirementsDefinitionKey;
  }

  public void setDecisionRequirementsDefinitionKey(String decisionRequirementsDefinitionKey) {
    this.decisionRequirementsDefinitionKey = decisionRequirementsDefinitionKey;
  }
}
