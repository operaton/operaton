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
package org.operaton.bpm.engine.impl.cmmn.cmd;

import org.operaton.bpm.engine.exception.cmmn.CaseExecutionNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 *
 */
public class GetCaseExecutionVariableCmd implements Command<Object> {
  protected String caseExecutionId;
  protected String variableName;
  protected boolean isLocal;

  public GetCaseExecutionVariableCmd(String caseExecutionId, String variableName, boolean isLocal) {
    this.caseExecutionId = caseExecutionId;
    this.variableName = variableName;
    this.isLocal = isLocal;
  }

  @Override
  public Object execute(CommandContext commandContext) {
    ensureNotNull("caseExecutionId", caseExecutionId);
    ensureNotNull("variableName", variableName);

    CaseExecutionEntity caseExecution = commandContext
      .getCaseExecutionManager()
      .findCaseExecutionById(caseExecutionId);

    ensureNotNull(CaseExecutionNotFoundException.class, "case execution %s doesn't exist".formatted(caseExecutionId), "caseExecution", caseExecution);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadCaseInstance(caseExecution);
    }

    Object value;

    if (isLocal) {
      value = caseExecution.getVariableLocal(variableName);
    } else {
      value = caseExecution.getVariable(variableName);
    }

    return value;
  }

}
