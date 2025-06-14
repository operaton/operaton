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
package org.operaton.bpm.engine.impl.interceptor;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.DiagnosticsRegistry;
import org.operaton.bpm.engine.impl.util.ClassNameUtil;

public class CommandCounterInterceptor extends CommandInterceptor {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  public CommandCounterInterceptor(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
  }

  @Override
  public <T> T execute(Command<T> command) {
    try {
      return next.execute(command);
    } finally {
      DiagnosticsRegistry diagnosticsRegistry = processEngineConfiguration.getDiagnosticsRegistry();
      if (diagnosticsRegistry != null) {
        String className = ClassNameUtil.getClassNameWithoutPackage(command);
        // anonymous class/lambda implementations of the Command interface are excluded
        if (!command.getClass().isAnonymousClass() && !className.contains("$$Lambda")) {
          className = parseLocalClassName(className);
          diagnosticsRegistry.markOccurrence(className);
        }
      }
    }
  }

  protected String parseLocalClassName(String className) {
    return className.replace("$", "_");
  }

}
