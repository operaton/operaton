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
package org.operaton.bpm.engine.impl.cmmn.transformer;

import java.util.List;

import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseTask;
import org.operaton.bpm.model.cmmn.instance.DecisionTask;
import org.operaton.bpm.model.cmmn.instance.Definitions;
import org.operaton.bpm.model.cmmn.instance.EventListener;
import org.operaton.bpm.model.cmmn.instance.HumanTask;
import org.operaton.bpm.model.cmmn.instance.Milestone;
import org.operaton.bpm.model.cmmn.instance.PlanItem;
import org.operaton.bpm.model.cmmn.instance.ProcessTask;
import org.operaton.bpm.model.cmmn.instance.Sentry;
import org.operaton.bpm.model.cmmn.instance.Stage;
import org.operaton.bpm.model.cmmn.instance.Task;

/**
 * Listener which can be registered within the engine to receive events during transforming (and
 * maybe influence it). Instead of implementing this interface you might consider to extend
 * the {@link AbstractCmmnTransformListener}, which contains an empty implementation for all methods
 * and makes your implementation easier and more robust to future changes.
 *
 * @author Sebastian Menski
 *
 */
public interface CmmnTransformListener {

  default void transformRootElement(Definitions definitions, List<? extends CmmnCaseDefinition> caseDefinitions) {
  }

  default void transformCase(Case element, CmmnCaseDefinition caseDefinition) {
  }

  /**
   * @deprecated Use {@link #transformCasePlanModel(org.operaton.bpm.model.cmmn.instance.CasePlanModel, CmmnActivity)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  default void transformCasePlanModel(org.operaton.bpm.model.cmmn.impl.instance.CasePlanModel casePlanModel, CmmnActivity caseActivity) {
    transformCasePlanModel((org.operaton.bpm.model.cmmn.instance.CasePlanModel) casePlanModel, caseActivity);
  }

  default void transformCasePlanModel(CasePlanModel casePlanModel, CmmnActivity caseActivity) {
  }

  default void transformHumanTask(PlanItem planItem, HumanTask humanTask, CmmnActivity caseActivity) {
  }

  default void transformProcessTask(PlanItem planItem, ProcessTask processTask, CmmnActivity caseActivity) {
  }

  default void transformCaseTask(PlanItem planItem, CaseTask caseTask, CmmnActivity caseActivity) {
  }

  default void transformDecisionTask(PlanItem planItem, DecisionTask decisionTask, CmmnActivity caseActivity) {
  }

  default void transformTask(PlanItem planItem, Task task, CmmnActivity caseActivity) {
  }

  default void transformStage(PlanItem planItem, Stage stage, CmmnActivity caseActivity) {
  }

  default void transformMilestone(PlanItem planItem, Milestone milestone, CmmnActivity caseActivity) {
  }

  default void transformEventListener(PlanItem planItem, EventListener eventListener, CmmnActivity caseActivity) {
  }

  default void transformSentry(Sentry sentry, CmmnSentryDeclaration sentryDeclaration) {
  }

}
