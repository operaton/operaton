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

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

/**
 * JobEntity for ever living job, which can be rescheduled and executed again.
 *
 * @author Svetlana Dorokhova
 */
public class EverLivingJobEntity extends JobEntity {

  @Serial private static final long serialVersionUID = 1L;

  private static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  public static final String TYPE = "ever-living";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  protected void postExecute(CommandContext commandContext) {
    LOG.debugJobExecuted(this);
    init(commandContext);
    commandContext.getHistoricJobLogManager().fireJobSuccessfulEvent(this);
  }

  @Override
  public void init(CommandContext commandContext) {
    init(commandContext, false, true);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=%s, revision=%s, duedate=%s, lockOwner=%s, lockExpirationTime=%s, executionId=%s, processInstanceId=%s, isExclusive=".formatted(id, revision, duedate, lockOwner).formatted(lockExpirationTime, executionId, processInstanceId) + isExclusive
           + ", retries=%s, jobHandlerType=%s, jobHandlerConfiguration=%s, exceptionByteArray=%s, exceptionByteArrayId=%s, exceptionMessage=%s, deploymentId=%s]".formatted(retries, jobHandlerType, jobHandlerConfiguration, exceptionByteArray).formatted(exceptionByteArrayId, exceptionMessage, deploymentId);
  }

}
