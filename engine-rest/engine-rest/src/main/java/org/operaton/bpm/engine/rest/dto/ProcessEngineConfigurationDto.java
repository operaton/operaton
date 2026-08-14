/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.rest.dto;

import org.operaton.bpm.engine.ProcessEngineConfiguration;

public class ProcessEngineConfigurationDto {

  protected String engineName;
  protected String historyLevel;
  protected boolean authorizationEnabled;
  protected boolean enablePasswordPolicy;

  public static ProcessEngineConfigurationDto fromProcessEngineConfiguration(ProcessEngineConfiguration configuration) {
    ProcessEngineConfigurationDto dto = new ProcessEngineConfigurationDto();
    dto.engineName = configuration.getProcessEngineName();
    dto.historyLevel = configuration.getHistory();
    dto.authorizationEnabled = configuration.isAuthorizationEnabled();
    dto.enablePasswordPolicy = configuration.isEnablePasswordPolicy();
    return dto;
  }

  public String getEngineName() {
    return engineName;
  }

  public void setEngineName(String engineName) {
    this.engineName = engineName;
  }

  public String getHistoryLevel() {
    return historyLevel;
  }

  public void setHistoryLevel(String historyLevel) {
    this.historyLevel = historyLevel;
  }

  public boolean isAuthorizationEnabled() {
    return authorizationEnabled;
  }

  public void setAuthorizationEnabled(boolean authorizationEnabled) {
    this.authorizationEnabled = authorizationEnabled;
  }

  public boolean isEnablePasswordPolicy() {
    return enablePasswordPolicy;
  }

  public void setEnablePasswordPolicy(boolean enablePasswordPolicy) {
    this.enablePasswordPolicy = enablePasswordPolicy;
  }

}
