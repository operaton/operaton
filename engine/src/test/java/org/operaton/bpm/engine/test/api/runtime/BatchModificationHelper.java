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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineProvider;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;

public class BatchModificationHelper extends BatchHelper {

  protected List<String> currentProcessInstances;

  public BatchModificationHelper(ProcessEngineProvider engineRule) {
    super(engineRule);
    currentProcessInstances = new ArrayList<>();
  }

  public Batch startAfterAsync(String key, int numberOfProcessInstances, String activityId, String processDefinitionId) {
    List<String> processInstanceIds = startInstances(key, numberOfProcessInstances);

    return getRuntimeService()
      .createModification(processDefinitionId)
      .startAfterActivity(activityId)
      .processInstanceIds(processInstanceIds)
      .executeAsync();
  }

  public Batch startBeforeAsync(String key, int numberOfProcessInstances, String activityId, String processDefinitionId) {
    List<String> processInstanceIds = startInstances(key, numberOfProcessInstances);

    return getRuntimeService().createModification(processDefinitionId).startBeforeActivity(activityId).processInstanceIds(processInstanceIds).executeAsync();
  }

  public Batch startTransitionAsync(String key, int numberOfProcessInstances, String transitionId, String processDefinitionId) {
    List<String> processInstanceIds = startInstances(key, numberOfProcessInstances);

    return getRuntimeService().createModification(processDefinitionId).startTransition(transitionId).processInstanceIds(processInstanceIds).executeAsync();
  }

  public Batch cancelAllAsync(String key, int numberOfProcessInstances, String activityId, String processDefinitionId) {
    List<String> processInstanceIds = startInstances(key, numberOfProcessInstances);

    return getRuntimeService().createModification(processDefinitionId).cancelAllForActivity(activityId).processInstanceIds(processInstanceIds).executeAsync();
  }

  public List<String> startInstances(String key, int numOfInstances) {
    List<String> instances = new ArrayList<>();
    for (int i = 0; i < numOfInstances; i++) {
      ProcessInstance processInstance = getRuntimeService().startProcessInstanceByKey(key);
      instances.add(processInstance.getId());
    }

    currentProcessInstances = instances;
    return instances;
  }

  @Override
  public JobDefinition getExecutionJobDefinition(Batch batch) {
    return getManagementService()
        .createJobDefinitionQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).jobType(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION).singleResult();
  }

}
