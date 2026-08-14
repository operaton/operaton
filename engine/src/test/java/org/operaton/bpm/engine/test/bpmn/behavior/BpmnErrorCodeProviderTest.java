/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.behavior;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.BpmnErrorCodeProvider;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

class BpmnErrorCodeProviderTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/behavior/BpmnErrorCodeProviderTest.bpmn20.xml"})
  void shouldPropagateProviderErrorCodeToBoundaryEvent() {
    runtimeService.startProcessInstanceByKey("bpmnErrorCodeProviderProcess");

    assertThat(taskService.createTaskQuery().taskDefinitionKey("handledTask").count()).isOne();
  }

  public static class ErrorCodeProviderDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      throw new CustomBpmnException();
    }
  }

  static class CustomBpmnException extends Exception implements BpmnErrorCodeProvider {

    @Override
    public String getErrorCode() {
      return "CUSTOM_ERROR_CODE";
    }
  }
}
