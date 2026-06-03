/*
 * Copyright 2026 FINOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * Shared validation utilities for ad-hoc subprocess activity filtering and authorization.
 * Centralizes activity type whitelist and scope boundary checks used by both
 * {@link AdHocSubProcessActivityBehavior} and the trigger command.
 */
public class AdHocSubProcessValidationHelper {

  private static final Set<String> STARTABLE_ACTIVITY_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      ActivityTypes.TASK,
      ActivityTypes.TASK_SCRIPT,
      ActivityTypes.TASK_SERVICE,
      ActivityTypes.TASK_BUSINESS_RULE,
      ActivityTypes.TASK_MANUAL_TASK,
      ActivityTypes.TASK_USER_TASK,
      ActivityTypes.TASK_SEND_TASK,
      ActivityTypes.TASK_RECEIVE_TASK,
      ActivityTypes.CALL_ACTIVITY,
      ActivityTypes.SUB_PROCESS,
      ActivityTypes.TRANSACTION)));

  private AdHocSubProcessValidationHelper() {
    // Static utility class
  }

  /**
   * Checks if an activity type is allowed to be started in an ad-hoc subprocess.
   * Only task-like activities, call activities, subprocesses, and transactions are startable.
   * Events (including intermediate), gateways, and boundary events are intentionally excluded.
   *
   * @param type the BPMN activity type
   * @return true if the type is startable in ad-hoc scope
   */
  public static boolean isStartableActivityType(String type) {
    return type != null && STARTABLE_ACTIVITY_TYPES.contains(type);
  }

  /**
   * Checks if an activity has an incoming transition from within the ad-hoc scope.
   * Activities with such transitions are downstream-only and cannot be starter activities.
   *
   * @param adHocScope the ad-hoc subprocess scope
   * @param activity the activity to check
   * @return true if activity has incoming transition from within scope
   */
  public static boolean hasIncomingTransitionFromAdHocScope(ActivityImpl adHocScope, ActivityImpl activity) {
    for (PvmTransition incomingTransition : activity.getIncomingTransitions()) {
      if (incomingTransition.getSource() instanceof ActivityImpl) {
        ActivityImpl source = (ActivityImpl) incomingTransition.getSource();
        if (adHocScope.findActivity(source.getId()) != null) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Determines if an activity can be started within an ad-hoc scope.
    * Rejects compensation handlers, activity types outside the whitelist, and activities
   * with incoming transitions from within the scope.
   *
   * @param adHocScope the ad-hoc subprocess scope
   * @param activity the activity to validate
   * @return true if activity is startable in ad-hoc scope
   */
  public static boolean isStartableActivityInAdHocScope(ActivityImpl adHocScope, ActivityImpl activity) {
    if (activity.isCompensationHandler()) {
      return false;
    }

    String type = (String) activity.getProperty(BpmnProperties.TYPE.name());
    if (!isStartableActivityType(type)) {
      return false;
    }

    return !hasIncomingTransitionFromAdHocScope(adHocScope, activity);
  }
}
