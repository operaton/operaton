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
package org.operaton.bpm.model.cmmn.instance;

import org.operaton.bpm.model.cmmn.PlanItemTransition;

/**
 * @author Roman Smirnov
 *
 */
public interface PlanItemOnPart extends OnPart {

  /**
   * @deprecated since 1.0, use {@link #getExitCriterion()} instead to access the parent exit criterion.
   */
  @Deprecated(since = "1.0")
  Sentry getSentry();

  /**
   * @deprecated since 1.0, use {@link #setExitCriterion(ExitCriterion)} instead to set the parent exit criterion.
   */
  @Deprecated(since = "1.0")
  void setSentry(Sentry sentry);

  ExitCriterion getExitCriterion();

  void setExitCriterion(ExitCriterion exitCriterion);

  PlanItem getSource();

  void setSource(PlanItem source);

  PlanItemTransition getStandardEvent();

  void setStandardEvent(PlanItemTransition standardEvent);

}
