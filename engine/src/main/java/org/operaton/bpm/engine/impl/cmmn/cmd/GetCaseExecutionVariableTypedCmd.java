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

import java.io.Serial;
import java.io.Serializable;

import org.operaton.bpm.engine.exception.cmmn.CaseExecutionNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 * @author Daniel Meyer
 *
 */
public class GetCaseExecutionVariableTypedCmd implements Command<TypedValue>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected String caseExecutionId;
  protected String variableName;
  protected boolean isLocal;
  protected boolean deserializeValue;

  public GetCaseExecutionVariableTypedCmd(String caseExecutionId, String variableName, boolean isLocal, boolean deserializeValue) {
    this.caseExecutionId = caseExecutionId;
    this.variableName = variableName;
    this.isLocal = isLocal;
    this.deserializeValue = deserializeValue;
  }

  @Override
  public TypedValue execute(CommandContext commandContext) {
    ensureNotNull("caseExecutionId", caseExecutionId);
    ensureNotNull("variableName", variableName);

    CaseExecutionEntity caseExecution = commandContext
      .getCaseExecutionManager()
      .findCaseExecutionById(caseExecutionId);

    ensureNotNull(CaseExecutionNotFoundException.class, "case execution %s doesn't exist".formatted(caseExecutionId), "caseExecution", caseExecution);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadCaseInstance(caseExecution);
    }

    TypedValue value;

    if (isLocal) {
      value = caseExecution.getVariableLocalTyped(variableName, deserializeValue);
    } else {
      value = caseExecution.getVariableTyped(variableName, deserializeValue);
    }

    return value;
  }

}
