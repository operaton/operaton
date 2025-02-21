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
package org.operaton.bpm.engine.test.junit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProcessEngineExtensionRequiredHistoryLevelAuditTest {

  @RegisterExtension
  ProcessEngineExtension extension = ProcessEngineExtension.builder()
      .configurationResource("audithistory.operaton.cfg.xml")
      .build();

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testRequiredHistoryIgnored() {
    fail("the configured history level is too high");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void testRequiredHistoryLevelMatch() {
    assertThat(extension.getProcessEngineConfiguration().getHistoryLevel().getName()).isEqualTo(ProcessEngineConfiguration.HISTORY_AUDIT);
  }
}
