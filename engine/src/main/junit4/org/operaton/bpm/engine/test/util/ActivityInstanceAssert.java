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
package org.operaton.bpm.engine.test.util;

import java.util.*;

import org.assertj.core.api.Assertions;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.operaton.bpm.engine.impl.persistence.entity.TransitionInstanceImpl;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.TransitionInstance;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Daniel Meyer
 *
 */
public final class ActivityInstanceAssert {

  public static class ActivityInstanceAssertThatClause {

    protected ActivityInstance actual;

    public ActivityInstanceAssertThatClause(ActivityInstance actual) {
      this.actual = actual;
    }

    public void hasStructure(ActivityInstance expected) {
      assertTreeMatch(expected, actual);
    }

    public void hasTotalIncidents(int expected) {
      List<ActivityInstance> activityInstances = new LinkedList<>();
      activityInstances.add(actual);

      int actualIncidents = 0;

      while (!activityInstances.isEmpty()) {
        ActivityInstance current = activityInstances.remove(0);
        actualIncidents += current.getIncidentIds().length;

        for (TransitionInstance transitionInstance : current.getChildTransitionInstances()) {
          actualIncidents += transitionInstance.getIncidentIds().length;
        }

        for (ActivityInstance activityInstance : current.getChildActivityInstances()) {
          activityInstances.add(activityInstance);
        }
      }

      Assertions.assertThat(actualIncidents).isEqualTo(expected);
    }

    protected void assertTreeMatch(ActivityInstance expected, ActivityInstance actual) {
      boolean treesMatch = isTreeMatched(expected, actual);
      if (!treesMatch) {
        fail("Could not match expected tree %n%s %n%n with actual tree %n%n %s".formatted(expected, actual));
      }
    }


    /** if anyone wants to improve this algorithm, feel welcome! */
    protected boolean isTreeMatched(ActivityInstance expectedInstance, ActivityInstance actualInstance) {
      if(!expectedInstance.getActivityId().equals(actualInstance.getActivityId())
          || (expectedInstance.getId() != null && !expectedInstance.getId().equals(actualInstance.getId()))) {
        return false;
      } else {
        if(expectedInstance.getChildActivityInstances().length != actualInstance.getChildActivityInstances().length) {
          return false;
        } else {

          List<ActivityInstance> unmatchedInstances = new ArrayList<>(Arrays.asList(expectedInstance.getChildActivityInstances()));
          for (ActivityInstance actualChild : actualInstance.getChildActivityInstances()) {
            boolean matchFound = false;
            for (ActivityInstance expectedChild : new ArrayList<ActivityInstance>(unmatchedInstances)) {
              if (isTreeMatched(expectedChild, actualChild)) {
                unmatchedInstances.remove(actualChild);
                matchFound = true;
                break;
              }
            }
            if(!matchFound) {
              return false;
            }
          }

          if (expectedInstance.getChildTransitionInstances().length != actualInstance.getChildTransitionInstances().length) {
            return false;
          }

          List<TransitionInstance> unmatchedTransitionInstances =
              new ArrayList<>(Arrays.asList(expectedInstance.getChildTransitionInstances()));
          for (TransitionInstance child : actualInstance.getChildTransitionInstances()) {
            Iterator<TransitionInstance> expectedTransitionInstanceIt = unmatchedTransitionInstances.iterator();

            boolean matchFound = false;
            while (expectedTransitionInstanceIt.hasNext() && !matchFound) {
              TransitionInstance expectedChild = expectedTransitionInstanceIt.next();
              if (expectedChild.getActivityId().equals(child.getActivityId())) {
                matchFound = true;
                expectedTransitionInstanceIt.remove();
              }
            }

            if (!matchFound) {
              return false;
            }
          }

        }
        return true;

      }
    }

  }

  public static class ActivityInstanceTreeBuilder {

    protected ActivityInstanceImpl rootInstance = null;
    protected Stack<ActivityInstanceImpl> activityInstanceStack = new Stack<>();

    public ActivityInstanceTreeBuilder() {
      this(null);
    }

    public ActivityInstanceTreeBuilder(String rootActivityId) {
      rootInstance = new ActivityInstanceImpl();
      rootInstance.setActivityId(rootActivityId);
      activityInstanceStack.push(rootInstance);
    }

    public ActivityInstanceTreeBuilder beginScope(String activityId) {
      return beginScope(activityId, null);
    }

    public ActivityInstanceTreeBuilder beginScope(String activityId, String activityInstanceId) {
      ActivityInstanceImpl newInstance = new ActivityInstanceImpl();
      newInstance.setActivityId(activityId);
      newInstance.setId(activityInstanceId);

      ActivityInstanceImpl parentInstance = activityInstanceStack.peek();
      List<ActivityInstance> childInstances = new ArrayList<>(Arrays.asList(parentInstance.getChildActivityInstances()));
      childInstances.add(newInstance);
      parentInstance.setChildActivityInstances(childInstances.toArray(new ActivityInstance[childInstances.size()]));

      activityInstanceStack.push(newInstance);

      return this;
    }

    public ActivityInstanceTreeBuilder beginMiBody(String activityId) {
      return beginScope(activityId + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX, null);
    }

    public ActivityInstanceTreeBuilder beginMiBody(String activityId, String activityInstanceId) {
      return beginScope(activityId + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX, activityInstanceId);
    }

    public ActivityInstanceTreeBuilder activity(String activityId) {

      return activity(activityId, null);
    }

    public ActivityInstanceTreeBuilder activity(String activityId, String activityInstanceId) {

      beginScope(activityId);
      id(activityInstanceId);
      endScope();

      return this;
    }

    public ActivityInstanceTreeBuilder transition(String activityId) {

      TransitionInstanceImpl newInstance = new TransitionInstanceImpl();
      newInstance.setActivityId(activityId);
      ActivityInstanceImpl parentInstance = activityInstanceStack.peek();

      List<TransitionInstance> childInstances = new ArrayList<>(
          Arrays.asList(parentInstance.getChildTransitionInstances()));
      childInstances.add(newInstance);
      parentInstance.setChildTransitionInstances(childInstances.toArray(new TransitionInstance[childInstances.size()]));

      return this;
    }

    public ActivityInstanceTreeBuilder endScope() {
      activityInstanceStack.pop();
      return this;
    }

    public ActivityInstance done() {
      return rootInstance;
    }

    protected ActivityInstanceTreeBuilder id(String expectedActivityInstanceId) {
      ActivityInstanceImpl activityInstanceImpl = activityInstanceStack.peek();
      activityInstanceImpl.setId(expectedActivityInstanceId);
      return this;
    }
  }

  public static ActivityInstanceTreeBuilder describeActivityInstanceTree() {
    return new ActivityInstanceTreeBuilder();
  }

  public static ActivityInstanceTreeBuilder describeActivityInstanceTree(String rootActivityId) {
    return new ActivityInstanceTreeBuilder(rootActivityId);
  }

  public static ActivityInstanceAssertThatClause assertThat(ActivityInstance actual) {
    return new ActivityInstanceAssertThatClause(actual);
  }

  private ActivityInstanceAssert() {
  }

}
