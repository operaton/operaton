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
package org.operaton.bpm.engine.impl.history.transformer;

import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.impl.cmmn.handler.ItemHandler;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformListener;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.CmmnHistoryEventProducer;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseTask;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;
import org.operaton.bpm.model.cmmn.instance.EventListener;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.Milestone;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;
import org.operaton.bpm.model.cmmn.instance.Stage;
import org.operaton.bpm.model.cmmn.instance.Task;

/**
 * @author Sebastian Menski
 */
public class CmmnHistoryTransformListener implements CmmnTransformListener {

  // Cached listeners
  // listeners can be reused for a given process engine instance but cannot be cached in static fields since
  // different process engine instances on the same Classloader may have different HistoryEventProducer
  // configurations wired
  protected CaseExecutionListener caseInstanceCreateListener;
  protected CaseExecutionListener caseInstanceUpdateListener;
  protected CaseExecutionListener caseInstanceCloseListener;

  protected CaseExecutionListener caseActivityInstanceCreateListener;
  protected CaseExecutionListener caseActivityInstanceUpdateListener;
  protected CaseExecutionListener caseActivityInstanceEndListener;

  // The history level set in the process engine configuration
  protected HistoryLevel historyLevel;

  public CmmnHistoryTransformListener(CmmnHistoryEventProducer historyEventProducer) {
    initCaseExecutionListeners(historyEventProducer);
  }

  protected void initCaseExecutionListeners(CmmnHistoryEventProducer historyEventProducer) {
    caseInstanceCreateListener = new CaseInstanceCreateListener(historyEventProducer);
    caseInstanceUpdateListener = new CaseInstanceUpdateListener(historyEventProducer);
    caseInstanceCloseListener = new CaseInstanceCloseListener(historyEventProducer);

    caseActivityInstanceCreateListener = new CaseActivityInstanceCreateListener(historyEventProducer);
    caseActivityInstanceUpdateListener = new CaseActivityInstanceUpdateListener(historyEventProducer);
    caseActivityInstanceEndListener = new CaseActivityInstanceEndListener(historyEventProducer);
  }


  @Override
  public void transformCasePlanModel(CasePlanModel casePlanModel, CmmnActivity caseActivity) {
    addCasePlanModelHandlers(caseActivity);
  }

  @Override
  public void transformHumanTask(PlanItem planItem, HumanTask humanTask, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformProcessTask(PlanItem planItem, ProcessTask processTask, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformCaseTask(PlanItem planItem, CaseTask caseTask, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformDecisionTask(PlanItem planItem, DecisionTask decisionTask, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformTask(PlanItem planItem, Task task, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformStage(PlanItem planItem, Stage stage, CmmnActivity caseActivity) {
    addTaskOrStageHandlers(caseActivity);
  }

  @Override
  public void transformMilestone(PlanItem planItem, Milestone milestone, CmmnActivity caseActivity) {
    addEventListenerOrMilestoneHandlers(caseActivity);
  }

  @Override
  public void transformEventListener(PlanItem planItem, EventListener eventListener, CmmnActivity caseActivity) {
    addEventListenerOrMilestoneHandlers(caseActivity);
  }


  protected void addCasePlanModelHandlers(CmmnActivity caseActivity) {
    ensureHistoryLevelInitialized();
    if (caseActivity == null) {
      return;
    }

    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_INSTANCE_CREATE, null)) {
      for (String event : ItemHandler.CASE_PLAN_MODEL_CREATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseInstanceCreateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_INSTANCE_UPDATE, null)) {
      for (String event : ItemHandler.CASE_PLAN_MODEL_UPDATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseInstanceUpdateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_INSTANCE_CLOSE, null)) {
      for (String event : ItemHandler.CASE_PLAN_MODEL_CLOSE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseInstanceCloseListener);
      }
    }
  }

  protected void addTaskOrStageHandlers(CmmnActivity caseActivity) {
    ensureHistoryLevelInitialized();
    if (caseActivity == null) {
      return;
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_CREATE, null)) {
      for (String event : ItemHandler.TASK_OR_STAGE_CREATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceCreateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_UPDATE, null)) {
      for (String event : ItemHandler.TASK_OR_STAGE_UPDATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceUpdateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_END, null)) {
      for (String event : ItemHandler.TASK_OR_STAGE_END_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceEndListener);
      }
    }
  }

  protected void addEventListenerOrMilestoneHandlers(CmmnActivity caseActivity) {
    ensureHistoryLevelInitialized();
    if (caseActivity == null) {
      return;
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_CREATE, null)) {
      for (String event : ItemHandler.EVENT_LISTENER_OR_MILESTONE_CREATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceCreateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_UPDATE, null)) {
      for (String event : ItemHandler.EVENT_LISTENER_OR_MILESTONE_UPDATE_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceUpdateListener);
      }
    }
    if (historyLevel.isHistoryEventProduced(HistoryEventTypes.CASE_ACTIVITY_INSTANCE_END, null)) {
      for (String event : ItemHandler.EVENT_LISTENER_OR_MILESTONE_END_EVENTS) {
        caseActivity.addBuiltInListener(event, caseActivityInstanceEndListener);
      }
    }
  }

  protected void ensureHistoryLevelInitialized() {
    if (historyLevel == null) {
      historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    }
  }

}
