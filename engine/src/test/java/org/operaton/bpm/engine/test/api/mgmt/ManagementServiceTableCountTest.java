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

import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Saeid Mizaei
 * @author Joram Barrez
 */
public class ManagementServiceTableCountTest extends PluggableProcessEngineTest {

  @Test
  public void testTableCount() {
    Map<String, Long> tableCount = managementService.getTableCount();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    assertThat(tableCount.get(tablePrefix + "ACT_GE_BYTEARRAY")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_RE_DEPLOYMENT")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_RU_EXECUTION")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_ID_GROUP")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_ID_MEMBERSHIP")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_ID_USER")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_RE_PROCDEF")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_RU_TASK")).isEqualTo(Long.valueOf(0));
    assertThat(tableCount.get(tablePrefix + "ACT_RU_IDENTITYLINK")).isEqualTo(Long.valueOf(0));
  }

}
