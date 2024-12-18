/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.interceptor;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;


/**
 * @author Tom Baeyens
 */
public class LogInterceptor extends CommandInterceptor {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  @Override
  public <T> T execute(Command<T> command) {
    LOG.debugStartingCommand(command);
    try {
      return next.execute(command);
    }
    finally {
      LOG.debugFinishingCommand(command);
    }
  }

}
