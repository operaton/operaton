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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.operaton.bpm.engine.ActivityTypes;

public class AdHocStartabilityTest {

  protected final AdHocStartability startability = AdHocStartability.INSTANCE;

  @Test
  public void shouldAcceptTaskLikeAndCallableTypes() {
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_SCRIPT)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_SERVICE)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_BUSINESS_RULE)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_MANUAL_TASK)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_USER_TASK)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_SEND_TASK)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TASK_RECEIVE_TASK)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.CALL_ACTIVITY)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.SUB_PROCESS)).isTrue();
    assertThat(startability.isStartableActivityType(ActivityTypes.TRANSACTION)).isTrue();
  }

  @Test
  public void shouldRejectIntermediateEvents() {
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_CATCH)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_MESSAGE)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_TIMER)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_LINK)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_SIGNAL)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_CONDITIONAL)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_THROW)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_SIGNAL_THROW)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_COMPENSATION_THROW)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_NONE_THROW)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_ESCALATION_THROW)).isFalse();
  }

  @Test
  public void shouldRejectGatewaysAndBoundaryEvents() {
    assertThat(startability.isStartableActivityType(ActivityTypes.GATEWAY_EXCLUSIVE)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.GATEWAY_INCLUSIVE)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.GATEWAY_PARALLEL)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.GATEWAY_COMPLEX)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.GATEWAY_EVENT_BASED)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_TIMER)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_MESSAGE)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_SIGNAL)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_COMPENSATION)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_ERROR)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_ESCALATION)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_CANCEL)).isFalse();
    assertThat(startability.isStartableActivityType(ActivityTypes.BOUNDARY_CONDITIONAL)).isFalse();
  }

  @Test
  public void shouldRejectNullType() {
    assertThat(startability.isStartableActivityType(null)).isFalse();
  }

  @Test
  public void shouldHandleMissingRuntimeContext() {
    assertThat(startability.getStartableActivities(null)).isEmpty();
    assertThat(startability.isStartableActivity(null, null)).isFalse();
    assertThat(startability.isActivityAlreadyActiveInScope(null, "task")).isFalse();
  }
}
