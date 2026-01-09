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
package org.operaton.bpm.engine.impl.migration.validation.activity;

import java.util.List;

import org.operaton.bpm.engine.impl.bpmn.behavior.*;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

/**
 * *Supported* refers to whether an activity instance of a certain activity type can be migrated.
 * This validator is irrelevant for transition instances which can be migrated at any activity type.
 * Thus, this validator is only used during migration instruction generation and migrating activity instance validation,
 * not during migration instruction validation.
 */
public class SupportedActivityValidator implements MigrationActivityValidator {

  public static final SupportedActivityValidator INSTANCE = new SupportedActivityValidator();

  private static final List<Class<? extends ActivityBehavior>> SUPPORTED_ACTIVITY_BEHAVIORS = List.of(
    SubProcessActivityBehavior.class,
    UserTaskActivityBehavior.class,
    BoundaryEventActivityBehavior.class,
    ParallelMultiInstanceActivityBehavior.class,
    SequentialMultiInstanceActivityBehavior.class,
    ReceiveTaskActivityBehavior.class,
    CallActivityBehavior.class,
    CaseCallActivityBehavior.class,
    IntermediateCatchEventActivityBehavior.class,
    EventBasedGatewayActivityBehavior.class,
    EventSubProcessActivityBehavior.class,
    EventSubProcessStartEventActivityBehavior.class,
    ExternalTaskActivityBehavior.class,
    ParallelGatewayActivityBehavior.class,
    InclusiveGatewayActivityBehavior.class,
    IntermediateConditionalEventBehavior.class,
    BoundaryConditionalEventActivityBehavior.class,
    EventSubProcessStartConditionalEventActivityBehavior.class
  );

  @Override
  public boolean valid(ActivityImpl activity) {
    return activity != null && (isSupportedActivity(activity) || isAsync(activity));
  }

  public boolean isSupportedActivity(ActivityImpl activity) {
    return SUPPORTED_ACTIVITY_BEHAVIORS.contains(activity.getActivityBehavior().getClass());
  }

  protected boolean isAsync(ActivityImpl activity) {
    return activity.isAsyncBefore() || activity.isAsyncAfter();
  }

}
