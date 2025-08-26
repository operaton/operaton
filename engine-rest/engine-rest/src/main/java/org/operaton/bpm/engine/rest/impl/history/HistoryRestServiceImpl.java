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
package org.operaton.bpm.engine.rest.impl.history;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.rest.history.*;
import org.operaton.bpm.engine.rest.impl.AbstractRestProcessEngineAware;

public class HistoryRestServiceImpl extends AbstractRestProcessEngineAware implements HistoryRestService {

  public HistoryRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public HistoricProcessInstanceRestService getProcessInstanceService() {
    return new HistoricProcessInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricCaseInstanceRestService getCaseInstanceService() {
    return new HistoricCaseInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricActivityInstanceRestService getActivityInstanceService() {
    return new HistoricActivityInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricCaseActivityInstanceRestService getCaseActivityInstanceService() {
    return new HistoricCaseActivityInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricVariableInstanceRestService getVariableInstanceService() {
    return new HistoricVariableInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricProcessDefinitionRestService getProcessDefinitionService() {
    return new HistoricProcessDefinitionRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricDecisionDefinitionRestService getDecisionDefinitionService() {
    return new HistoricDecisionDefinitionRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricDecisionStatisticsRestService getDecisionStatisticsService() {
    return new HistoricDecisionStatisticsRestServiceImpl(getProcessEngine());
  }

  @Override
  public HistoricCaseDefinitionRestService getCaseDefinitionService() {
    return new HistoricCaseDefinitionRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public UserOperationLogRestService getUserOperationLogRestService() {
    return new UserOperationLogRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricDetailRestService getDetailService() {
    return new HistoricDetailRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricTaskInstanceRestService getTaskInstanceService() {
    return new HistoricTaskInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricIncidentRestService getIncidentService() {
    return new HistoricIncidentRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricIdentityLinkLogRestService getIdentityLinkService() {
    return new HistoricIdentityLinkLogRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricJobLogRestService getJobLogService() {
    return new HistoricJobLogRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricDecisionInstanceRestService getDecisionInstanceService() {
    return new HistoricDecisionInstanceRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricBatchRestService getBatchService() {
    return new HistoricBatchRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoricExternalTaskLogRestService getExternalTaskLogService() {
    return new HistoricExternalTaskLogRestServiceImpl(getObjectMapper(), getProcessEngine());
  }

  @Override
  public HistoryCleanupRestService getHistoryCleanupRestService() {
    return new HistoryCleanupRestServiceImpl(getObjectMapper(), getProcessEngine());
  }
}
