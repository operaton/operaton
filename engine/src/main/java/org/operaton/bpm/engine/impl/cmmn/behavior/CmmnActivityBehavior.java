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
package org.operaton.bpm.engine.impl.cmmn.behavior;

import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.core.delegate.CoreActivityBehavior;

/**
 * @author Roman Smirnov
 *
 */
public interface CmmnActivityBehavior extends CoreActivityBehavior<CmmnActivityExecution> {

  void onCreate(CmmnActivityExecution execution);

  void created(CmmnActivityExecution execution);

  void onEnable(CmmnActivityExecution execution);

  void onReenable(CmmnActivityExecution execution);

  void onDisable(CmmnActivityExecution execution);

  void onStart(CmmnActivityExecution execution);

  void onManualStart(CmmnActivityExecution execution);

  void started(CmmnActivityExecution execution);

  void onCompletion(CmmnActivityExecution execution);

  void onManualCompletion(CmmnActivityExecution execution);

  void onTermination(CmmnActivityExecution execution);

  void onParentTermination(CmmnActivityExecution execution);

  void onExit(CmmnActivityExecution execution);

  void onOccur(CmmnActivityExecution execution);

  void onSuspension(CmmnActivityExecution execution);

  void onParentSuspension(CmmnActivityExecution execution);

  void onResume(CmmnActivityExecution execution);

  void onParentResume(CmmnActivityExecution execution);

  void resumed(CmmnActivityExecution execution);

  void onReactivation(CmmnActivityExecution execution);

  void reactivated(CmmnActivityExecution execution);

  void onClose(CmmnActivityExecution execution);

  void fireEntryCriteria(CmmnActivityExecution execution);

  void fireExitCriteria(CmmnActivityExecution execution);

  void repeat(CmmnActivityExecution execution, String standardEvent);

}
