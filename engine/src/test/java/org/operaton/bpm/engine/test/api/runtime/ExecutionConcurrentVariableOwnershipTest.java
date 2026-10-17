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
package org.operaton.bpm.engine.test.api.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionConcurrentVariableOwnershipTest {

  @RegisterExtension
  static ProcessEngineExtension engine = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension helper = new ProcessEngineTestExtension(engine);

  @Test
  void expandingThenPruningMovesVariableOwnershipWithoutStaleOrResurrectedReferences() {
    ProcessDefinition definition = helper.deployAndGetDefinition(Bpmn.createExecutableProcess("process")
        .startEvent().userTask("plain").endEvent().done());
    ProcessInstance process = engine.getRuntimeService().startProcessInstanceById(definition.getId());
    engine.getRuntimeService().setVariableLocal(process.getId(), "tokenLocal", "retained");

    engine.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(context -> {
      ExecutionEntity scope = context.getExecutionManager().findExecutionById(process.getId());
      ((VariableInstanceEntity) scope.getVariableInstanceLocal("tokenLocal")).setConcurrentLocal(true);
      ExecutionEntity additional = (ExecutionEntity) scope.createConcurrentExecution();
      ExecutionEntity replacement = scope.getReplacedBy();
      assertThat(scope.getVariableLocal("tokenLocal")).isNull();
      assertThat(replacement.getVariableLocal("tokenLocal")).isEqualTo("retained");
      assertThat(((VariableInstanceEntity) replacement.getVariableInstanceLocal("tokenLocal")).getExecution())
          .isSameAs(replacement);
      scope.setVariableLocal("tokenLocal", "new scope value");
      assertThat(replacement.getVariableLocal("tokenLocal")).isEqualTo("retained");
      assertThat(scope.getVariableLocal("tokenLocal")).isEqualTo("new scope value");
      scope.removeVariableLocal("tokenLocal");
      additional.remove();
      scope.tryPruneLastConcurrentChild();
      assertThat(scope.getVariableLocal("tokenLocal")).isEqualTo("retained");
      return null;
    });

    engine.getTaskService().complete(engine.getTaskService().createTaskQuery().processInstanceId(process.getId())
        .singleResult().getId());
    assertThat(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process.getId()).count()).isZero();
  }
}
