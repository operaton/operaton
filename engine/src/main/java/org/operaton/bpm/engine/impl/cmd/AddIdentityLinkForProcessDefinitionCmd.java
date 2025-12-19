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
package org.operaton.bpm.engine.impl.cmd;

import java.io.Serial;
import java.io.Serializable;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tijs Rademakers
 */
public class AddIdentityLinkForProcessDefinitionCmd implements Command<Void>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected String processDefinitionId;

  protected String userId;

  protected String groupId;

  public AddIdentityLinkForProcessDefinitionCmd(String processDefinitionId, String userId, String groupId) {
    validateParams(userId, groupId, processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    this.userId = userId;
    this.groupId = groupId;
  }

  protected void validateParams(String userId, String groupId, String processDefinitionId) {
    ensureNotNull("processDefinitionId", processDefinitionId);

    if (userId == null && groupId == null) {
      throw new ProcessEngineException("userId and groupId cannot both be null");
    }
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ProcessDefinitionEntity processDefinition = Context
      .getCommandContext()
      .getProcessDefinitionManager()
      .findLatestProcessDefinitionById(processDefinitionId);

    EnsureUtil.ensureNotNull("Cannot find process definition with id %s".formatted(processDefinitionId), "processDefinition", processDefinition);

    processDefinition.addIdentityLink(userId, groupId);
    return null;
  }

}
