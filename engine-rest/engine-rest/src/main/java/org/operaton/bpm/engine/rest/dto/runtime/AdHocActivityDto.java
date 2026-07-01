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
package org.operaton.bpm.engine.rest.dto.runtime;

import java.util.List;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.runtime.AdHocActivity;

public class AdHocActivityDto {

  protected String activityId;
  protected String activityName;
  protected String activityType;

  public static AdHocActivityDto fromAdHocActivity(AdHocActivity activity) {
    AdHocActivityDto dto = new AdHocActivityDto();
    dto.activityId = activity.getActivityId();
    dto.activityName = activity.getActivityName();
    dto.activityType = activity.getActivityType();
    return dto;
  }

  public static List<AdHocActivityDto> fromAdHocActivities(List<AdHocActivity> activities) {
    return activities.stream()
        .map(AdHocActivityDto::fromAdHocActivity)
        .collect(Collectors.toList());
  }

  public String getActivityId() {
    return activityId;
  }

  public String getActivityName() {
    return activityName;
  }

  public String getActivityType() {
    return activityType;
  }
}
