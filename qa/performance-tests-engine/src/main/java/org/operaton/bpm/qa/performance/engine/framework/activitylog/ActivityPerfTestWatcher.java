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
package org.operaton.bpm.qa.performance.engine.framework.activitylog;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.qa.performance.engine.framework.*;
import org.operaton.bpm.qa.performance.engine.junit.PerfTestProcessEngine;
import org.operaton.bpm.qa.performance.engine.steps.PerfTestConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ActivityPerfTestWatcher implements PerfTestWatcher {

  public static final List<String> WATCH_ALL_ACTIVITIES = Collections.singletonList("ALL");

  protected final List<String> activityIds;
  protected final boolean watchAllActivities;

  public ActivityPerfTestWatcher(List<String> activityIds) {
    this.activityIds = activityIds;
    watchAllActivities = WATCH_ALL_ACTIVITIES.equals(activityIds);
  }

  @Override
  public void beforePass(PerfTestPass pass) {
    // nothing to do
  }

  @Override
  public void beforeRun(PerfTest test, PerfTestRun run) {
    // nothing to do
  }

  @Override
  public void beforeStep(PerfTestStep step, PerfTestRun run) {
    // nothing to do
  }

  @Override
  public void afterStep(PerfTestStep step, PerfTestRun run) {
    // nothing to do
  }

  @Override
  public void afterRun(PerfTest test, PerfTestRun run) {
    // nothing to do
  }

  @Override
  public void afterPass(PerfTestPass pass) {
    ProcessEngine processEngine = PerfTestProcessEngine.getInstance();
    HistoryService historyService = processEngine.getHistoryService();

    for (PerfTestRun run : pass.getRuns().values()) {
      logActivityResults(pass, run, historyService);
    }
  }

  protected void logActivityResults(PerfTestPass pass, PerfTestRun run, HistoryService historyService) {
    String processInstanceId = run.getVariable(PerfTestConstants.PROCESS_INSTANCE_ID);
    List<ActivityPerfTestResult> activityResults = new ArrayList<>();

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    Date startTime = processInstance.getStartTime();

    List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
      .processInstanceId(processInstanceId)
      .orderByHistoricActivityInstanceStartTime()
      .asc()
      .list();

    for (HistoricActivityInstance activityInstance : activityInstances) {
      if (watchAllActivities || activityIds.contains(activityInstance.getActivityId())) {
        ActivityPerfTestResult result = new ActivityPerfTestResult(activityInstance);
        if (activityInstance.getActivityType().equals("startEvent")) {
          result.setStartTime(startTime);
        }
        activityResults.add(result);
      }
    }

    pass.logActivityResult(processInstanceId, activityResults);
  }

}
