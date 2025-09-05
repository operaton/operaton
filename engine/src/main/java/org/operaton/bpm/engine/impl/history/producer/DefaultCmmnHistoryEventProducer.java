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
package org.operaton.bpm.engine.impl.history.producer;

import java.util.Optional;

import org.operaton.bpm.engine.delegate.DelegateCaseExecution;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.history.event.HistoricCaseActivityInstanceEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricCaseInstanceEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;

/**
 * @author Sebastian Menski
 */
public class DefaultCmmnHistoryEventProducer implements CmmnHistoryEventProducer {

  @Override
  public HistoryEvent createCaseInstanceCreateEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseInstanceEventEntity evt = newCaseInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_INSTANCE_CREATE);

    // set create time
    evt.setCreateTime(ClockUtil.getCurrentTime());

    // set create user id
    evt.setCreateUserId(Context.getCommandContext().getAuthenticatedUserId());

    // set super case instance id
    CmmnExecution superCaseExecution = caseExecutionEntity.getSuperCaseExecution();
    if (superCaseExecution != null) {
      evt.setSuperCaseInstanceId(superCaseExecution.getCaseInstanceId());
    }

    // set super process instance id
    ExecutionEntity superExecution = caseExecutionEntity.getSuperExecution();
    if (superExecution != null) {
      evt.setSuperProcessInstanceId(superExecution.getProcessInstanceId());
    }

    return evt;
  }

  @Override
  public HistoryEvent createCaseInstanceUpdateEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseInstanceEventEntity evt = loadCaseInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_INSTANCE_UPDATE);

    return evt;
  }

  @Override
  public HistoryEvent createCaseInstanceCloseEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseInstanceEventEntity evt = loadCaseInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_INSTANCE_CLOSE);

    // set end time
    evt.setEndTime(ClockUtil.getCurrentTime());

    if (evt.getStartTime() != null) {
      evt.setDurationInMillis(evt.getEndTime().getTime() - evt.getStartTime().getTime());
    }

    return evt;
  }

  @Override
  public HistoryEvent createCaseActivityInstanceCreateEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseActivityInstanceEventEntity evt = newCaseActivityInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseActivityInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_ACTIVITY_INSTANCE_CREATE);

    // set start time
    evt.setCreateTime(ClockUtil.getCurrentTime());

    return evt;
  }

  @Override
  public HistoryEvent createCaseActivityInstanceUpdateEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseActivityInstanceEventEntity evt = loadCaseActivityInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseActivityInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_ACTIVITY_INSTANCE_UPDATE);

    if (caseExecutionEntity.getTask() != null) {
      evt.setTaskId(caseExecutionEntity.getTask().getId());
    }

    if (caseExecutionEntity.getSubProcessInstance() != null) {
      evt.setCalledProcessInstanceId(caseExecutionEntity.getSubProcessInstance().getId());
    }

    if (caseExecutionEntity.getSubCaseInstance() != null) {
      evt.setCalledCaseInstanceId(caseExecutionEntity.getSubCaseInstance().getId());
    }

    return evt;
  }

  @Override
  public HistoryEvent createCaseActivityInstanceEndEvt(DelegateCaseExecution caseExecution) {
    final CaseExecutionEntity caseExecutionEntity = (CaseExecutionEntity) caseExecution;

    // create event instance
    HistoricCaseActivityInstanceEventEntity evt = loadCaseActivityInstanceEventEntity(caseExecutionEntity);

    // initialize event
    initCaseActivityInstanceEvent(evt, caseExecutionEntity, HistoryEventTypes.CASE_ACTIVITY_INSTANCE_END);

    // set end time
    evt.setEndTime(ClockUtil.getCurrentTime());

    // calculate duration
    if (evt.getStartTime() != null) {
      evt.setDurationInMillis(evt.getEndTime().getTime() - evt.getStartTime().getTime());
    }

    return evt;
  }

  @SuppressWarnings("unused")
  protected HistoricCaseInstanceEventEntity newCaseInstanceEventEntity(CaseExecutionEntity caseExecutionEntity) {
    return new HistoricCaseInstanceEventEntity();
  }

  protected HistoricCaseInstanceEventEntity loadCaseInstanceEventEntity(CaseExecutionEntity caseExecutionEntity) {
    return newCaseInstanceEventEntity(caseExecutionEntity);
  }

  protected void initCaseInstanceEvent(HistoricCaseInstanceEventEntity evt, CaseExecutionEntity caseExecutionEntity, HistoryEventTypes eventType) {
    evt.setId(caseExecutionEntity.getCaseInstanceId());
    evt.setEventType(eventType.getEventName());
    evt.setCaseDefinitionId(caseExecutionEntity.getCaseDefinitionId());
    evt.setCaseInstanceId(caseExecutionEntity.getCaseInstanceId());
    evt.setCaseExecutionId(caseExecutionEntity.getId());
    evt.setBusinessKey(caseExecutionEntity.getBusinessKey());
    evt.setState(caseExecutionEntity.getState());
    evt.setTenantId(caseExecutionEntity.getTenantId());
  }

  protected HistoricCaseActivityInstanceEventEntity newCaseActivityInstanceEventEntity(CaseExecutionEntity caseExecutionEntity) {
    var entity = new HistoricCaseActivityInstanceEventEntity();
    initCaseActivityInstanceEvent(entity, caseExecutionEntity, null);
    return entity;
  }

  protected HistoricCaseActivityInstanceEventEntity loadCaseActivityInstanceEventEntity(CaseExecutionEntity caseExecutionEntity) {
    return newCaseActivityInstanceEventEntity(caseExecutionEntity);
  }

  protected void initCaseActivityInstanceEvent(HistoricCaseActivityInstanceEventEntity evt, CaseExecutionEntity caseExecutionEntity, HistoryEventTypes eventType) {
    evt.setId(caseExecutionEntity.getId());
    evt.setParentCaseActivityInstanceId(caseExecutionEntity.getParentId());
    evt.setEventType(Optional.ofNullable(eventType).map(HistoryEventTypes::getEventName).orElse(null));
    evt.setCaseDefinitionId(caseExecutionEntity.getCaseDefinitionId());
    evt.setCaseInstanceId(caseExecutionEntity.getCaseInstanceId());
    evt.setCaseExecutionId(caseExecutionEntity.getId());
    evt.setCaseActivityInstanceState(caseExecutionEntity.getState());

    evt.setRequired(caseExecutionEntity.isRequired());

    evt.setCaseActivityId(caseExecutionEntity.getActivityId());
    evt.setCaseActivityName(caseExecutionEntity.getActivityName());
    evt.setCaseActivityType(caseExecutionEntity.getActivityType());

    evt.setTenantId(caseExecutionEntity.getTenantId());
  }

}
