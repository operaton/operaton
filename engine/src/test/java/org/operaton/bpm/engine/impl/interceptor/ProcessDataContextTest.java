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
package org.operaton.bpm.engine.impl.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.commons.logging.MdcAccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDataContextTest {

  private static final String PROCESS_DEFINITION_KEY_MDC_PROPERTY = "processDefinitionKey";
  private static final String PROCESS_DEFINITION_KEY = "invoice";

  @AfterEach
  void clearMdc() {
    MdcAccess.remove(PROCESS_DEFINITION_KEY_MDC_PROPERTY);
  }

  @Test
  void pushSectionUsesStoredProcessDefinitionKeyForMdc() {
    ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration()
        .setLoggingContextActivityId(null)
        .setLoggingContextActivityName(null)
        .setLoggingContextApplicationName(null)
        .setLoggingContextBusinessKey(null)
        .setLoggingContextProcessDefinitionId(null)
        .setLoggingContextProcessDefinitionKey(PROCESS_DEFINITION_KEY_MDC_PROPERTY)
        .setLoggingContextProcessInstanceId(null)
        .setLoggingContextTenantId(null)
        .setLoggingContextEngineName(null);
    ExecutionEntity execution = mock(ExecutionEntity.class);
    ProcessEngine processEngine = mock(ProcessEngine.class);
    when(execution.getProcessEngine()).thenReturn(processEngine);
    when(processEngine.getName()).thenReturn("engine");
    when(execution.getProcessDefinitionKey()).thenReturn(PROCESS_DEFINITION_KEY);

    new ProcessDataContext(configuration).pushSection(execution);

    assertThat(MdcAccess.get(PROCESS_DEFINITION_KEY_MDC_PROPERTY)).isEqualTo(PROCESS_DEFINITION_KEY);
    verify(execution, never()).getProcessDefinition();
  }
}
