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
package org.operaton.bpm.engine.test.standalone.pvm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;

import org.assertj.core.api.Assertions;
import org.junit.Assert;

/**
 * @author Daniel Meyer
 *
 */
public class ActivityInstanceVerification extends Assert implements ExecutionListener {

  class ActivityInstance {

    String id;
    String parentId;
    String executionId;
    boolean isCompleteScope;

    public ActivityInstance(String executionId, String actInstanceId, String parentId, boolean isCompleteScope) {
      this.id = actInstanceId;
      this.executionId = executionId;
      this.parentId = parentId;
      this.isCompleteScope = isCompleteScope;
    }

    @Override
    public String toString() {
      return id + " by "+executionId + " parent: "+parentId;
    }

  }

  protected Map<String, List<ActivityInstance>> startedActivityInstances = new HashMap<>();
  protected Map<String, List<ActivityInstance>> endedActivityInstances = new HashMap<>();

  @Override
  public void notify(DelegateExecution e) throws Exception {

    final ActivityExecution execution = (ActivityExecution) e;

    if(execution.getActivityInstanceId() == null) {
      return;
    }

    if(execution.getEventName().equals(EVENTNAME_START)) {
      addActivityInstanceId(execution, startedActivityInstances);

    } else if(execution.getEventName().equals(EVENTNAME_END)) {
      addActivityInstanceId(execution, endedActivityInstances);
    }

  }

  private void addActivityInstanceId(ActivityExecution execution, Map<String, List<ActivityInstance>> instanceMap) {

    String actId = execution.getActivity().getId();
    String actInstanceId = execution.getActivityInstanceId();
    String parentActInstanceId = execution.getParentActivityInstanceId();
    String executionId = String.valueOf(System.identityHashCode(execution));

    // add to instance map
    List<ActivityInstance> instancesForThisAct = instanceMap.computeIfAbsent(actId, k -> new ArrayList<>());
    ActivityInstance activityInstance = new ActivityInstance(executionId, actInstanceId, parentActInstanceId, execution.isCompleteScope());
    instancesForThisAct.add(activityInstance);
  }

  // assertions //////////////////////////////

  public void assertStartInstanceCount(int count, String actId) {

    List<ActivityInstance> startInstancesForThisAct = startedActivityInstances.get(actId);
    if (count == 0 && startInstancesForThisAct == null) {
      return;
    }

    Assertions.assertThat(startInstancesForThisAct)
            .isNotNull()
            .hasSize(count);

    List<ActivityInstance> endInstancesForThisAct = endedActivityInstances.get(actId);
    Assertions.assertThat(endInstancesForThisAct).isNotNull();

    for (ActivityInstance startedActInstance : startInstancesForThisAct) {

      Assertions.assertThat(startedActInstance.id).as("activityInstanceId cannot be null for " + startedActInstance).isNotNull();
      Assertions.assertThat(startedActInstance.executionId).as("executionId cannot be null for " + startedActInstance).isNotNull();
      Assertions.assertThat(startedActInstance.parentId).as("parentId cannot be null for " + startedActInstance).isNotNull();

      boolean foundMatchingEnd = false;
      for (ActivityInstance endedActInstance : endInstancesForThisAct) {
        if(startedActInstance.id.equals(endedActInstance.id)) {
          Assertions.assertThat(endedActInstance.parentId).isEqualTo(startedActInstance.parentId);
          foundMatchingEnd = true;
        }
      }
      if(!foundMatchingEnd) {
        fail("cannot find matching end activity instance for start activity instance " + startedActInstance.id);
      }
    }
  }

  public void assertProcessInstanceParent(String actId, PvmProcessInstance processInstance) {
    assertParentActInstance(actId, String.valueOf(System.identityHashCode(processInstance)));
  }

  public void assertParentActInstance(String actId, String actInstId) {
    List<ActivityInstance> actInstanceList = startedActivityInstances.get(actId);

    for (ActivityInstance activityInstance : actInstanceList) {
      Assertions.assertThat(activityInstance.parentId).isEqualTo(actInstId);
    }

    actInstanceList = endedActivityInstances.get(actId);
    for (ActivityInstance activityInstance : actInstanceList) {
      Assertions.assertThat(activityInstance.parentId).isEqualTo(actInstId);
    }

  }

  public void assertParent(String actId, String parentId) {
    List<ActivityInstance> actInstanceList = startedActivityInstances.get(actId);
    List<ActivityInstance> parentInstances = startedActivityInstances.get(parentId);

    for (ActivityInstance activityInstance : actInstanceList) {
      boolean found = false;
      for (ActivityInstance parentInstance : parentInstances) {
        if(activityInstance.parentId.equals(parentInstance.id)) {
          found = true;
        }
      }
      if(!found) {
        fail("every instance of '" + actId + "' must have a parent which is an instance of '" + parentId);
      }
    }
  }

  public void assertIsCompletingActivityInstance(String activityId) {
    assertIsCompletingActivityInstance(activityId, -1);
  }

  public void assertIsCompletingActivityInstance(String activityId, int count) {
    assertCorrectCompletingState(activityId, count, true);
  }

  public void assertNonCompletingActivityInstance(String activityId) {
    assertNonCompletingActivityInstance(activityId, -1);
  }

  public void assertNonCompletingActivityInstance(String activityId, int count) {
    assertCorrectCompletingState(activityId, count, false);
  }

  private void assertCorrectCompletingState(String activityId, int expectedCount, boolean completing) {
    List<ActivityInstance> endActivityInstances = endedActivityInstances.get(activityId);
    Assertions.assertThat(endActivityInstances).isNotNull();

    for (ActivityInstance instance : endActivityInstances) {
      Assertions.assertThat(instance.isCompleteScope).isEqualTo(completing);
    }

    if (expectedCount != -1) {
      Assertions.assertThat(endActivityInstances).hasSize(expectedCount);
    }
  }

}
