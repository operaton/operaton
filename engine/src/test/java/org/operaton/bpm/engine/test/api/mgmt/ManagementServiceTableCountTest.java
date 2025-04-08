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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Saeid Mizaei
 * @author Joram Barrez
 */
@ExtendWith(ProcessEngineExtension.class)
public class ManagementServiceTableCountTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;

  @Test
  public void testTableCount() {
    Map<String, Long> tableCount = managementService.getTableCount();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    assertThat(tableCount)
            .containsEntry(tablePrefix + "ACT_GE_BYTEARRAY", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_RE_DEPLOYMENT", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_RU_EXECUTION", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_ID_GROUP", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_ID_MEMBERSHIP", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_ID_USER", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_RE_PROCDEF", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_RU_TASK", Long.valueOf(0))
            .containsEntry(tablePrefix + "ACT_RU_IDENTITYLINK", Long.valueOf(0));
  }

}
