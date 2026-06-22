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
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.impl.interceptor.Command;

/**
 * Command executed during engine startup to verify or initialize the history level stored in the
 * database.
 *
 * <p>Implementations can be supplied via
 * {@link org.operaton.bpm.engine.ProcessEngineConfiguration#setHistoryLevelCommand} to customize
 * history level setup behavior in engine plugins.
 *
 * <p>The default implementation is {@link DefaultHistoryLevelSetupCommand}.
 *
 * @since 2.1
 * @see DefaultHistoryLevelSetupCommand
 * @see HistoryLevelUtils
 */
public interface HistoryLevelSetupCommand extends Command<Void> {
}
