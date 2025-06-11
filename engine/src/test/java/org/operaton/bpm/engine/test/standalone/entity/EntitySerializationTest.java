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
package org.operaton.bpm.engine.test.standalone.entity;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.task.DelegationState;

import java.io.*;
import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitySerializationTest {

  @Test
  public void testTaskEntitySerialization() throws Exception {
    TaskEntity task = new TaskEntity();
    task.setDelegationState(DelegationState.RESOLVED);
    task.setExecution(new ExecutionEntity());
    task.setProcessInstance(new ExecutionEntity());
    task.setTaskDefinition(new TaskDefinition(null));

    task.setAssignee("kermit");
    task.setCreateTime(new Date());
    task.setDescription("Test description");
    task.setDueDate(new Date());
    task.setName("myTask");
    task.setEventName("end");
    task.setDeleted(false);
    task.setDelegationStateString(DelegationState.RESOLVED.name());

    byte[] data = writeObject(task);
    task = (TaskEntity) readObject(data);

    assertThat(task.getAssignee()).isEqualTo("kermit");
    assertThat(task.getName()).isEqualTo("myTask");
    assertThat(task.getEventName()).isEqualTo("end");
  }

  @Test
  public void testExecutionEntitySerialization() throws Exception {
   ExecutionEntity execution = new ExecutionEntity();

   ActivityImpl activityImpl = new ActivityImpl("test", null);
   activityImpl.addListener("start", new TestExecutionListener());
   execution.setActivity(activityImpl);

   ProcessDefinitionImpl processDefinitionImpl = new ProcessDefinitionImpl("test");
   processDefinitionImpl.addListener("start", new TestExecutionListener());
   execution.setProcessDefinition(processDefinitionImpl);

   TransitionImpl transitionImpl = new TransitionImpl("test", new ProcessDefinitionImpl("test"));
   transitionImpl.addListener(ExecutionListener.EVENTNAME_TAKE, new TestExecutionListener());
   execution.setTransition(transitionImpl);

   execution.setSuperExecution(new ExecutionEntity());

   execution.setActive(true);
   execution.setCanceled(false);
   execution.setBusinessKey("myBusinessKey");
   execution.setDeleteReason("no reason");
   execution.setActivityInstanceId("123");
   execution.setScope(false);

   byte[] data = writeObject(execution);
   execution = (ExecutionEntity) readObject(data);

    assertThat(execution.getBusinessKey()).isEqualTo("myBusinessKey");
    assertThat(execution.getDeleteReason()).isEqualTo("no reason");
    assertThat(execution.getActivityInstanceId()).isEqualTo("123");

  }

  private byte[] writeObject(Object object) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    ObjectOutputStream outputStream = new ObjectOutputStream(buffer);
    outputStream.writeObject(object);
    outputStream.flush();
    outputStream.close();

    return buffer.toByteArray();
  }

  private Object readObject(byte[] data) throws IOException, ClassNotFoundException {
    InputStream buffer = new ByteArrayInputStream(data);
    ObjectInputStream inputStream = new ObjectInputStream(buffer);
    return inputStream.readObject();
  }

}
