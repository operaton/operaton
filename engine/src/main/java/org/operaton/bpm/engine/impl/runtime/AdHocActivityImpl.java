/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.runtime;

import java.io.Serializable;

import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.runtime.AdHocActivity;

public class AdHocActivityImpl implements AdHocActivity, Serializable {

  private static final long serialVersionUID = 1L;

  protected String activityId;
  protected String activityName;
  protected String activityType;

  public static AdHocActivityImpl fromActivity(ActivityImpl activity) {
    AdHocActivityImpl result = new AdHocActivityImpl();
    result.activityId = activity.getId();
    result.activityName = activity.getName();
    result.activityType = (String) activity.getProperty(BpmnProperties.TYPE.name());
    return result;
  }

  @Override
  public String getActivityId() {
    return activityId;
  }

  @Override
  public String getActivityName() {
    return activityName;
  }

  @Override
  public String getActivityType() {
    return activityType;
  }
}
