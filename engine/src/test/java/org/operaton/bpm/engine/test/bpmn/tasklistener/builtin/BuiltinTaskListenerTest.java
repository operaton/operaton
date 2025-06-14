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
package org.operaton.bpm.engine.test.bpmn.tasklistener.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;

class BuiltinTaskListenerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/bpmn/tasklistener/builtin/task.listener.operaton.cfg.xml")
    .build();
  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension();

  RuntimeService runtimeService;
  RepositoryService repositoryService;

  @Test
  @Deployment
  @WatchLogger(loggerNames = {"org.operaton.bpm.engine.test"}, level = "INFO")
  void shouldExecuteBuiltinTaskListerInOrderAfterModification() {
    // given
    // PreParseListener registered as customPreBPMNParseListener, registers a 'create' TaskListener
    // PostParseListener registered as customPostBPMNParseListener, registers a 'create' TaskListener
    // TestTaskListener registered as regular (non-builtin) 'create' TaskListener
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when move token to user task by instance modification with skipCustomListeners=true -> only builtinTaskListeners are executed
    runtimeService.createProcessInstanceModification(processInstance.getId()).startBeforeActivity("theTask").execute(true, true);

    // then
    // TaskListeners should trigger in order: builtin always trigger before regular listeners, builtin and regular listeners trigger in registration order
    List<ILoggingEvent> logEntries = loggingRule.getLog();
    assertThat(logEntries).hasSize(5); // three log events from normal execution (one regular TaskListener, two builtin), two from execution after modification (two builtin TaskListener)
    assertThat(logEntries).extracting("formattedMessage")
      .containsExactly(
          // regular execution
          // builtin TaskListener
          "Executed task listener: PreParseListener",
          "Executed task listener: PostParseListener",
          // regular TaskListener
          "Executed task listener: TestTaskListener",
          // after process instance modification
          // builtin TaskListener
          "Executed task listener: PreParseListener",
          "Executed task listener: PostParseListener");
  }

}
