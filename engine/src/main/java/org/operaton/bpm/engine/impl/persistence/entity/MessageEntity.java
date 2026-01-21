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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.io.Serial;
import java.util.Objects;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;


/**
 * NOTE: instances of Message Entity should be created via {@link MessageJobDeclaration}.
 *
 * @author Tom Baeyens
 */
public class MessageEntity extends JobEntity {

  public static final String TYPE = "message";

  @Serial private static final long serialVersionUID = 1L;

  private static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  private String repeat;

  public String getRepeat() {
    return repeat;
  }
  public void setRepeat(String repeat) {
    this.repeat = repeat;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  protected void postExecute(CommandContext commandContext) {
    LOG.debugJobExecuted(this);
    if (repeat != null && !repeat.isEmpty()) {
      init(commandContext, false, true);
    } else {
      delete(true);
    }
    commandContext.getHistoricJobLogManager().fireJobSuccessfulEvent(this);
  }

  @Override
  public void init(CommandContext commandContext, boolean shouldResetLock, boolean shouldCallDeleteHandler) {
    super.init(commandContext, shouldResetLock, shouldCallDeleteHandler);
    repeat = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    if (!super.equals(o)) {return false;}
    MessageEntity that = (MessageEntity) o;
    return Objects.equals(repeat, that.repeat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), repeat);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[repeat=" + repeat
           + ", id=" + id
           + ", revision=" + revision
           + ", duedate=" + duedate
           + ", lockOwner=" + lockOwner
           + ", lockExpirationTime=" + lockExpirationTime
           + ", executionId=" + executionId
           + ", processInstanceId=" + processInstanceId
           + ", isExclusive=" + isExclusive
           + ", retries=" + retries
           + ", jobHandlerType=" + jobHandlerType
           + ", jobHandlerConfiguration=" + jobHandlerConfiguration
           + ", exceptionByteArray=" + exceptionByteArray
           + ", exceptionByteArrayId=" + exceptionByteArrayId
           + ", exceptionMessage=" + exceptionMessage
           + ", deploymentId=" + deploymentId
           + ", createTime=" + createTime
           + "]";
  }

}
