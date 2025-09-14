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
package org.operaton.bpm.engine.test.api.queries;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.HistoricVariableInstanceQueryImpl;
import org.operaton.bpm.engine.impl.persistence.StrongUuidGenerator;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class QueryByIdAfterTest {
  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder()
          .configurator(config -> config.setIdGenerator(new StrongUuidGenerator()))
          .build();

  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineExtension);

  HistoryService historyService;
  RuntimeService runtimeService;

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/history/HistoricVariableInstanceTest.testSimple.bpmn20.xml" })
  void shouldVariableInstanceApiReturnOnlyAfterGivenId() {
    // given
    startProcessInstancesByKey("myProc", 10);

    // when querying by idAfter then only expected results are returned
    HistoricVariableInstanceQueryImpl historicVariableInstanceQuery = (HistoricVariableInstanceQueryImpl) historyService.createHistoricVariableInstanceQuery();
    List<HistoricVariableInstance> historicVariableInstances = historicVariableInstanceQuery.orderByVariableId().asc().list();
    String firstId = historicVariableInstances.get(0).getId();
    String middleId = historicVariableInstances.get(9).getId();
    String lastId = historicVariableInstances.get(historicVariableInstances.size() - 1).getId();
    assertEquals(20, historicVariableInstances.size());
    assertEquals(19, historicVariableInstanceQuery.idAfter(firstId).list().size());
    assertEquals(0, historicVariableInstanceQuery.idAfter(lastId).list().size());

    List<HistoricVariableInstance> secondHalf = historicVariableInstanceQuery.idAfter(middleId).list();
    assertEquals(10, secondHalf.size());
    assertTrue(secondHalf.stream().allMatch(variable -> isIdGreaterThan(variable.getId(), middleId)));
  }

  private void startProcessInstancesByKey(String key, int numberOfInstances) {
    for (int i = 0; i < numberOfInstances; i++) {
      Map<String, Object> variables = Collections.singletonMap("message", "exception" + i);

      runtimeService.startProcessInstanceByKey(key, i + "", variables);
    }
    testRule.executeAvailableJobs();
  }

  /**
   * Compares two ids
   * @return true if id1 is greater than id2, false otherwise
   */
  private static boolean isIdGreaterThan(String id1, String id2) {
    return id1.compareTo(id2) > 0;
  }

}
