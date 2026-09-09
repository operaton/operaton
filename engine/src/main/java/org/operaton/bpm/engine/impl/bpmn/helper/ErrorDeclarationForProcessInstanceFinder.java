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
package org.operaton.bpm.engine.impl.bpmn.helper;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.bpmn.parser.ErrorEventDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmScope;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.tree.TreeVisitor;

import static java.util.Objects.requireNonNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public class ErrorDeclarationForProcessInstanceFinder implements TreeVisitor<PvmScope> {
  protected @Nullable Exception exception;
  protected @Nullable String errorCode;
  protected @Nullable PvmActivity errorHandlerActivity;
  protected @Nullable ErrorEventDefinition errorEventDefinition;
  protected PvmActivity currentActivity;

  public ErrorDeclarationForProcessInstanceFinder(@Nullable Exception exception, @Nullable String errorCode, PvmActivity currentActivity) {
    this.exception = exception;
    this.errorCode = errorCode;
    this.currentActivity = currentActivity;
  }

  @Override
  public void visit(PvmScope scope) {
    List<ErrorEventDefinition> errorEventDefinitions = scope.getProperties().get(BpmnProperties.ERROR_EVENT_DEFINITIONS);
    for (ErrorEventDefinition definition : errorEventDefinitions) {
      PvmActivity activityHandler = scope.getProcessDefinition().findActivity(definition.getHandlerActivityId());
      ensureNotNull("Activity Handler '%s' not found".formatted(definition.getHandlerActivityId()), "activityHandler", activityHandler);
      requireNonNull(activityHandler);

      if ((!isReThrowingErrorEventSubprocess(activityHandler)) && ((exception != null && definition.catchesException(exception))
        || (exception == null && definition.catchesError(errorCode)))) {

        errorHandlerActivity = activityHandler;
        this.errorEventDefinition = definition;
        break;
      }
    }
  }

  protected boolean isReThrowingErrorEventSubprocess(PvmActivity activityHandler) {
    ScopeImpl activityHandlerScope = (ScopeImpl)activityHandler;
    return activityHandlerScope.isAncestorFlowScopeOf((ScopeImpl)currentActivity);
  }

  public @Nullable PvmActivity getErrorHandlerActivity() {
    return errorHandlerActivity;
  }

  public @Nullable ErrorEventDefinition getErrorEventDefinition() {
    return errorEventDefinition;
  }

}
