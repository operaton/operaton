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
package org.operaton.bpm.engine.test.concurrency;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.impl.db.entitymanager.cache.CachedDbEntity;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.model.bpmn.Bpmn.createExecutableProcess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <pre>
 * thread1:
 *  t=1: fetch byte variable
 *  t=4: update byte variable value
 *
 * thread2:
 *  t=2: fetch and delete byte variable and entity
 *  t=3: commit transaction
 * </pre>
 *
 * This test ensures that thread1's command fails with an OptimisticLockingException,
 * not with a NullPointerException or something in that direction.
 *
 * @author Thorben Lindhauer
 */
class CompetingByteVariableAccessTest extends ConcurrencyTestCase {

  @Test
  void testConcurrentVariableRemoval() {
   testRule.deploy(createExecutableProcess("test")
        .startEvent()
          .userTask()
        .endEvent()
        .done());

    final byte[] byteVar = "asd".getBytes();

    String pid = runtimeService.startProcessInstanceByKey("test", createVariables().putValue("byteVar", byteVar)).getId();

    // start a controlled Fetch and Update variable command
    ThreadControl asyncThread = executeControllableCommand(new FetchAndUpdateVariableCmd(pid, "byteVar", "bsd".getBytes()));

    asyncThread.waitForSync();

    // now delete the process instance, deleting the variable and its byte array entity
    runtimeService.deleteProcessInstance(pid, null);

    // make the second thread continue
    // => this will a flush the FetchVariableCmd Context.
    // if the flush performs an update to the variable, it will fail with an OLE
    asyncThread.reportInterrupts();
    asyncThread.waitUntilDone();

    Throwable exception = asyncThread.getException();
    assertThat(exception).isInstanceOf(OptimisticLockingException.class);
  }

  static class FetchAndUpdateVariableCmd extends ControllableCommand<Void> {

    protected String executionId;
    protected String varName;
    protected Object newValue;

    public FetchAndUpdateVariableCmd(String executionId, String varName, Object newValue) {
      this.executionId = executionId;
      this.varName = varName;
      this.newValue = newValue;
    }

    @Override
    public Void execute(CommandContext commandContext) {

      ExecutionEntity execution = commandContext.getExecutionManager()
        .findExecutionById(executionId);

      // fetch the variable instance but not the value (make sure the byte array is lazily fetched)
      VariableInstanceEntity varInstance = (VariableInstanceEntity) execution.getVariableInstanceLocal(varName);
      String byteArrayValueId = varInstance.getByteArrayValueId();
      assertThat(byteArrayValueId).as("Byte array id is expected to be not null").isNotNull();

      CachedDbEntity cachedByteArray = commandContext.getDbEntityManager().getDbEntityCache()
        .getCachedEntity(ByteArrayEntity.class, byteArrayValueId);

      assertThat(cachedByteArray).as("Byte array is expected to be not fetched yet / lazily fetched.").isNull();

      monitor.sync();

      // now update the value
      execution.setVariableLocal(varInstance.getName(), newValue);

      return null;
    }

  }
}
