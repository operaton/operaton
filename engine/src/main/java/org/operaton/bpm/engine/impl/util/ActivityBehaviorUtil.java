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
package org.operaton.bpm.engine.impl.util;

import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmException;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 *
 */
public final class ActivityBehaviorUtil {

  private ActivityBehaviorUtil() {
  }

  public static CmmnActivityBehavior getActivityBehavior(CmmnExecution execution) {
    String id = execution.getId();

    CmmnActivity activity = execution.getActivity();
    ensureNotNull(PvmException.class, "Case execution '%s' has no current activity.".formatted(id), "activity", activity);

    CmmnActivityBehavior behavior = activity.getActivityBehavior();
    ensureNotNull(PvmException.class, "There is no behavior specified in %s for case execution '%s'.".formatted(activity, id), "behavior", behavior);

    return behavior;
  }

  public static ActivityBehavior getActivityBehavior(PvmExecutionImpl execution) {
    String id = execution.getId();

    PvmActivity activity = execution.getActivity();
    ensureNotNull(PvmException.class, "Execution '%s' has no current activity.".formatted(id), "activity", activity);

    ActivityBehavior behavior = activity.getActivityBehavior();
    ensureNotNull(PvmException.class, "There is no behavior specified in %s for execution '%s'.".formatted(activity, id), "behavior", behavior);

    return behavior;
  }

}
