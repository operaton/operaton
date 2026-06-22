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
package org.operaton.bpm.engine.test.standalone.variablescope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 * @author Christian Lipphardt
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class VariableScopeTest {

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;

  /**
   * A testcase to produce and fix issue ACT-862.
   */
  @Deployment
  @Test
  void testVariableNamesScope() {

    // After starting the process, the task in the subprocess should be active
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("test", "test");
    varMap.put("helloWorld", "helloWorld");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", varMap);
    Task subProcessTask = taskService.createTaskQuery()
        .processInstanceId(pi.getId())
        .singleResult();
    runtimeService.setVariableLocal(pi.getProcessInstanceId(), "mainProcessLocalVariable", "Hello World");

    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    runtimeService.setVariableLocal(subProcessTask.getExecutionId(), "subProcessLocalVariable", "Hello SubProcess");

    // Returns a set of local variablenames of pi
    List<String> result = processEngineConfiguration.
            getCommandExecutorTxRequired().
            execute(new GetVariableNamesCommand(pi.getProcessInstanceId(), true));

    // pi contains local the variablenames "test", "helloWorld" and "mainProcessLocalVariable" but not "subProcessLocalVariable"
    assertThat(result)
            .contains("test")
            .contains("helloWorld")
            .contains("mainProcessLocalVariable")
            .doesNotContain("subProcessLocalVariable");

    // Returns a set of global variablenames of pi
    result = processEngineConfiguration.
            getCommandExecutorTxRequired().
            execute(new GetVariableNamesCommand(pi.getProcessInstanceId(), false));

    // pi contains global the variablenames "test", "helloWorld" and "mainProcessLocalVariable" but not "subProcessLocalVariable"
    assertThat(result)
            .contains("test")
            .contains("mainProcessLocalVariable")
            .contains("helloWorld")
            .doesNotContain("subProcessLocalVariable");

    // Returns a set of local variablenames of subProcessTask execution
    result = processEngineConfiguration.
            getCommandExecutorTxRequired().
            execute(new GetVariableNamesCommand(subProcessTask.getExecutionId(), true));

    // subProcessTask execution contains local the variablenames "test", "subProcessLocalVariable" but not "helloWorld" and "mainProcessLocalVariable"
    assertThat(result)
            .contains("test")
            .contains("subProcessLocalVariable")
            .doesNotContain("helloWorld")
            .doesNotContain("mainProcessLocalVariable");

    // Returns a set of global variablenames of subProcessTask execution
    result = processEngineConfiguration.
            getCommandExecutorTxRequired().
            execute(new GetVariableNamesCommand(subProcessTask.getExecutionId(), false));

    // subProcessTask execution contains global all defined variablenames
    assertThat(result)
            .contains("test")
            .contains("subProcessLocalVariable")
            .contains("helloWorld")
            .contains("mainProcessLocalVariable");

    taskService.complete(subProcessTask.getId());
  }

  /**
   * A command to get the names of the variables
   * @author Roman Smirnov
   * @author Christian Lipphardt
   */
  private class GetVariableNamesCommand implements Command<List<String>> {

    private String executionId;
    private boolean isLocal;


    public GetVariableNamesCommand(String executionId, boolean isLocal) {
     this.executionId = executionId;
     this.isLocal = isLocal;
    }

    @Override
    public List<String> execute(CommandContext commandContext) {
      ensureNotNull("executionId", executionId);

      ExecutionEntity execution = commandContext
        .getExecutionManager()
        .findExecutionById(executionId);

      ensureNotNull("execution %s doesn't exist".formatted(executionId), "execution", execution);

      List<String> executionVariables;
      if (isLocal) {
        executionVariables = new ArrayList<>(execution.getVariableNamesLocal());
      } else {
        executionVariables = new ArrayList<>(execution.getVariableNames());
      }

      return executionVariables;
    }

  }
}
