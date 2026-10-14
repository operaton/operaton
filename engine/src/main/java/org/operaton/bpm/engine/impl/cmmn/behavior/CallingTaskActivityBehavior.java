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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement.CallableElementBinding;

import static java.util.Objects.requireNonNull;

/**
 * @author Roman Smirnov
 *
 */
public abstract @NullMarked class CallingTaskActivityBehavior extends TaskActivityBehavior {

  protected static final CmmnBehaviorLogger LOG = ProcessEngineLogger.CMNN_BEHAVIOR_LOGGER;

  protected @Nullable BaseCallableElement callableElement;

  @Override
  public void onManualCompletion(CmmnActivityExecution execution) {
    // Throw always an exception!
    // It should not be possible to complete a calling
    // task manually. If the called instance has
    // been completed, the associated task will
    // be notified to complete automatically.
    String id = execution.getId();
    throw LOG.forbiddenManualCompletitionException("complete", id, getTypeName());
  }

  public BaseCallableElement getCallableElement() {
    return requireNonNull(callableElement);
  }

  public void setCallableElement(BaseCallableElement callableElement) {
    this.callableElement = callableElement;
  }

  @SuppressWarnings("unused")
  protected String getDefinitionKey(CmmnActivityExecution execution) {
    CmmnExecution caseExecution = (CmmnExecution) execution;
    return getCallableElement().getDefinitionKey(caseExecution);
  }

  protected @Nullable Integer getVersion(CmmnActivityExecution execution) {
    CmmnExecution caseExecution = (CmmnExecution) execution;
    return getCallableElement().getVersion(caseExecution);
  }

  @SuppressWarnings("unused")
  protected @Nullable String getDeploymentId(CmmnActivityExecution execution) {
    return getCallableElement().getDeploymentId();
  }

  protected CallableElementBinding getBinding() {
    return getCallableElement().getBinding();
  }

  @SuppressWarnings("unused")
  protected boolean isLatestBinding() {
    return getCallableElement().isLatestBinding();
  }

  @SuppressWarnings("unused")
  protected boolean isDeploymentBinding() {
    return getCallableElement().isDeploymentBinding();
  }

  @SuppressWarnings("unused")
  protected boolean isVersionBinding() {
    return getCallableElement().isVersionBinding();
  }

  @SuppressWarnings("unused")
  protected boolean isVersionTagBinding() {
    return getCallableElement().isVersionTagBinding();
  }

}
