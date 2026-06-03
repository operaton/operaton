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

import org.operaton.bpm.engine.ActivityTypes;
import org.junit.Test;

public class AdHocSubProcessValidationHelperTest {

  @Test
  public void shouldAcceptTaskLikeAndCallableTypes() {
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_SCRIPT)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_SERVICE)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_BUSINESS_RULE)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_MANUAL_TASK)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_USER_TASK)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_SEND_TASK)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TASK_RECEIVE_TASK)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.CALL_ACTIVITY)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.SUB_PROCESS)).isTrue();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.TRANSACTION)).isTrue();
  }

  @Test
  public void shouldRejectIntermediateEvents() {
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_CATCH)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_MESSAGE)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_TIMER)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_LINK)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_SIGNAL)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_CONDITIONAL)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_THROW)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_SIGNAL_THROW)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_COMPENSATION_THROW)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_NONE_THROW)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.INTERMEDIATE_EVENT_ESCALATION_THROW)).isFalse();
  }

  @Test
  public void shouldRejectGatewaysAndBoundaryEvents() {
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.GATEWAY_EXCLUSIVE)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.GATEWAY_INCLUSIVE)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.GATEWAY_PARALLEL)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.GATEWAY_COMPLEX)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.GATEWAY_EVENT_BASED)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_TIMER)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_MESSAGE)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_SIGNAL)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_COMPENSATION)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_ERROR)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_ESCALATION)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_CANCEL)).isFalse();
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(ActivityTypes.BOUNDARY_CONDITIONAL)).isFalse();
  }

  @Test
  public void shouldRejectNullType() {
    assertThat(AdHocSubProcessValidationHelper.isStartableActivityType(null)).isFalse();
  }
}
