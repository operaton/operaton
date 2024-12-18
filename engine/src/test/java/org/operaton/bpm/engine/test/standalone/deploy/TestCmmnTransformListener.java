/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.standalone.deploy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnSentryDeclaration;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformListener;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.instance.CaseTask;
import org.operaton.bpm.model.cmmn.instance.CmmnModelElementInstance;
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
 * @author Sebastian Menski
 */
public class TestCmmnTransformListener implements CmmnTransformListener {

  public static Set<CmmnModelElementInstance> modelElementInstances = new HashSet<CmmnModelElementInstance>();
  public static Set<CmmnActivity> cmmnActivities = new HashSet<CmmnActivity>();
  public static Set<CmmnSentryDeclaration> sentryDeclarations = new HashSet<CmmnSentryDeclaration>();

  @Override
  public void transformRootElement(Definitions definitions, List<? extends CmmnCaseDefinition> caseDefinitions) {
    modelElementInstances.add(definitions);
    for (CmmnCaseDefinition caseDefinition : caseDefinitions) {
      CaseDefinitionEntity entity = (CaseDefinitionEntity) caseDefinition;
      entity.setKey(entity.getKey().concat("-modified"));
    }
  }

  @Override
  public void transformCase(Case element, CmmnCaseDefinition caseDefinition) {
    modelElementInstances.add(element);
    cmmnActivities.add(caseDefinition);
  }

  @Override
  public void transformCasePlanModel(org.operaton.bpm.model.cmmn.impl.instance.CasePlanModel casePlanModel, CmmnActivity caseActivity) {
    transformCasePlanModel((org.operaton.bpm.model.cmmn.instance.CasePlanModel) casePlanModel, caseActivity);
  }

  @Override
  public void transformCasePlanModel(CasePlanModel casePlanModel, CmmnActivity activity) {
    modelElementInstances.add(casePlanModel);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformHumanTask(PlanItem planItem, HumanTask humanTask, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(humanTask);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformProcessTask(PlanItem planItem, ProcessTask processTask, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(processTask);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformCaseTask(PlanItem planItem, CaseTask caseTask, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(caseTask);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformDecisionTask(PlanItem planItem, DecisionTask decisionTask, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(decisionTask);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformTask(PlanItem planItem, Task task, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(task);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformStage(PlanItem planItem, Stage stage, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(stage);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformMilestone(PlanItem planItem, Milestone milestone, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(milestone);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformEventListener(PlanItem planItem, EventListener eventListener, CmmnActivity activity) {
    modelElementInstances.add(planItem);
    modelElementInstances.add(eventListener);
    cmmnActivities.add(activity);
  }

  @Override
  public void transformSentry(Sentry sentry, CmmnSentryDeclaration sentryDeclaration) {
    modelElementInstances.add(sentry);
    sentryDeclarations.add(sentryDeclaration);
  }

  protected String getNewName(String name) {
    if (name.endsWith("-modified")) {
      return name + "-again";
    }
    else {
      return name + "-modified";
    }
  }

  public static void reset() {
    modelElementInstances = new HashSet<CmmnModelElementInstance>();
    cmmnActivities = new HashSet<CmmnActivity>();
    sentryDeclarations = new HashSet<CmmnSentryDeclaration>();
  }

  public static int numberOfRegistered(Class<? extends CmmnModelElementInstance> modelElementInstanceClass) {
    int count = 0;
    for (CmmnModelElementInstance element : modelElementInstances) {
      if (modelElementInstanceClass.isInstance(element)) {
        count++;
      }
    }
    return count;
  }

}
