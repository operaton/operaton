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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 *
 * @author Anna.Pazola
 *
 */
public class ExtendLockOnExternalTaskCmd extends HandleExternalTaskCmd {

  private final long newLockTime;

  public ExtendLockOnExternalTaskCmd(String externalTaskId, String workerId, long newLockTime) {
    super(externalTaskId, workerId);
    EnsureUtil.ensurePositive(BadUserRequestException.class, "lockTime", newLockTime);
    this.newLockTime = newLockTime;
  }

  @Override
  public String getErrorMessageOnWrongWorkerAccess() {
    return "The lock of the External Task %s cannot be extended by worker '%s'".formatted(externalTaskId, workerId);
  }

  @Override
  protected void execute(ExternalTaskEntity externalTask) {
    EnsureUtil.ensureGreaterThanOrEqual(BadUserRequestException.class, "Cannot extend a lock that expired",
        "lockExpirationTime", externalTask.getLockExpirationTime().getTime(), ClockUtil.getCurrentTime().getTime());
    externalTask.extendLock(newLockTime);
  }
}
