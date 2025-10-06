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
package org.operaton.bpm.engine.impl.repository;

import java.util.Date;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmd.ActivateProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.cmd.SuspendProcessDefinitionCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.repository.UpdateProcessDefinitionSuspensionStateBuilder;
import org.operaton.bpm.engine.repository.UpdateProcessDefinitionSuspensionStateSelectBuilder;
import org.operaton.bpm.engine.repository.UpdateProcessDefinitionSuspensionStateTenantBuilder;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureOnlyOneNotNull;

public class UpdateProcessDefinitionSuspensionStateBuilderImpl implements UpdateProcessDefinitionSuspensionStateBuilder,
    UpdateProcessDefinitionSuspensionStateSelectBuilder, UpdateProcessDefinitionSuspensionStateTenantBuilder {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  protected final CommandExecutor commandExecutor;

  protected String processDefinitionKey;
  protected String processDefinitionId;

  protected boolean includeProcessInstances;
  protected Date executionDate;

  protected String processDefinitionTenantId;
  protected boolean isTenantIdSet;

  public UpdateProcessDefinitionSuspensionStateBuilderImpl(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  /**
   * Creates a builder without CommandExecutor which can not be used to update
   * the suspension state via {@link #activate()} or {@link #suspend()}. Can be
   * used in combination with your own command.
   */
  public UpdateProcessDefinitionSuspensionStateBuilderImpl() {
    this(null);
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl byProcessDefinitionId(String processDefinitionId) {
    ensureNotNull("processDefinitionId", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl byProcessDefinitionKey(String processDefinitionKey) {
    ensureNotNull("processDefinitionKey", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl includeProcessInstances(boolean includeProcessInstance) {
    this.includeProcessInstances = includeProcessInstance;
    return this;
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl executionDate(Date date) {
    this.executionDate = date;
    return this;
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl processDefinitionWithoutTenantId() {
    this.processDefinitionTenantId = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public UpdateProcessDefinitionSuspensionStateBuilderImpl processDefinitionTenantId(String tenantId) {
    ensureNotNull("tenantId", tenantId);

    this.processDefinitionTenantId = tenantId;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public void activate() {
    validateParameters();

    ActivateProcessDefinitionCmd command = new ActivateProcessDefinitionCmd(this);
    commandExecutor.execute(command);
  }

  @Override
  public void suspend() {
    validateParameters();

    SuspendProcessDefinitionCmd command = new SuspendProcessDefinitionCmd(this);
    commandExecutor.execute(command);
  }

  protected void validateParameters() {
    ensureOnlyOneNotNull("Need to specify either a process instance id or a process definition key.", processDefinitionId, processDefinitionKey);

    if(processDefinitionId != null && isTenantIdSet) {
      throw LOG.exceptionUpdateSuspensionStateForTenantOnlyByProcessDefinitionKey();
    }

    ensureNotNull("commandExecutor", commandExecutor);
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public boolean isIncludeProcessInstances() {
    return includeProcessInstances;
  }

  public Date getExecutionDate() {
    return executionDate;
  }

  public String getProcessDefinitionTenantId() {
    return processDefinitionTenantId;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

}
