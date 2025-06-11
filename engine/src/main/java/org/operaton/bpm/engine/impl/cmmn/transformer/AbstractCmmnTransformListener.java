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
 * Abstract base class for implementing a {@link CmmnTransformListener} without being forced to implement
 * all methods provided, which make the implementation more robust to future changes.
 *
 * @author Sebastian Menski
 */
public class AbstractCmmnTransformListener implements CmmnTransformListener {

  @Override
  public void transformRootElement(Definitions definitions, List<? extends CmmnCaseDefinition> caseDefinitions) {
  }

  @Override
  public void transformCase(Case element, CmmnCaseDefinition caseDefinition) {
  }

  @Override
  public void transformCasePlanModel(CasePlanModel casePlanModel, CmmnActivity activity) {
  }

  @Override
  public void transformHumanTask(PlanItem planItem, HumanTask humanTask, CmmnActivity activity) {
  }

  @Override
  public void transformProcessTask(PlanItem planItem, ProcessTask processTask, CmmnActivity activity) {
  }

  @Override
  public void transformCaseTask(PlanItem planItem, CaseTask caseTask, CmmnActivity activity) {
  }

  @Override
  public void transformDecisionTask(PlanItem planItem, DecisionTask decisionTask, CmmnActivity activity) {
  }

  @Override
  public void transformTask(PlanItem planItem, Task task, CmmnActivity activity) {
  }

  @Override
  public void transformStage(PlanItem planItem, Stage stage, CmmnActivity activity) {
  }

  @Override
  public void transformMilestone(PlanItem planItem, Milestone milestone, CmmnActivity activity) {
  }

  @Override
  public void transformEventListener(PlanItem planItem, EventListener eventListener, CmmnActivity activity) {
  }

  @Override
  public void transformSentry(Sentry sentry, CmmnSentryDeclaration sentryDeclaration) {
  }

}
