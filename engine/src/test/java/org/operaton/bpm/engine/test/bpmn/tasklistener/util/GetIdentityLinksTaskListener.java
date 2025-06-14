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
package org.operaton.bpm.engine.test.bpmn.tasklistener.util;

import java.util.List;

import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.task.IdentityLink;

public class GetIdentityLinksTaskListener implements TaskListener {

  @Override
  public void notify(DelegateTask delegateTask) {
    TaskService taskService = delegateTask.getProcessEngine().getTaskService();
    List<IdentityLink> identityLinksForTask = taskService.getIdentityLinksForTask(delegateTask.getId());

    delegateTask.getExecution().setVariable("identityLinksSize", identityLinksForTask.size());

    // second call should return the same list size
    identityLinksForTask = taskService.getIdentityLinksForTask(delegateTask.getId());
    delegateTask.getExecution().setVariable("secondCallidentityLinksSize", identityLinksForTask.size());
  }

}
