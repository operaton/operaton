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
package org.operaton.bpm.engine.test.history;

import static org.operaton.bpm.engine.impl.util.StringUtil.hasText;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Edoardo Patti
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class HistoryEventDataTest {

  private static final TestEventHandler HANDLER = new TestEventHandler();

  @RegisterExtension
  HistoryEventVerifier verifier = new HistoryEventVerifier(HANDLER);

  private RuntimeService runtimeService;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().configurator(c -> c.setHistoryEventHandler(HANDLER))
    .build();

  @BeforeEach
  void initServices() {
    runtimeService = engineRule.getRuntimeService();

    verifier.historyEventIs("!= null", Objects::nonNull);
    verifier.historyEventHas("processDefinitionId != null", evt -> hasText(evt.getProcessDefinitionId()));
    verifier.historyEventHas("processDefinitionKey != null", evt -> hasText(evt.getProcessDefinitionKey()));
    verifier.historyEventHas("processDefinitionName != null", evt -> hasText(evt.getProcessDefinitionName()));
    verifier.historyEventHas("processDefinitionVersion != null", evt -> evt.getProcessDefinitionVersion() != null);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/threeTasksProcess.bpmn20.xml")
  void verify() {
    assertNotNull(runtimeService.startProcessInstanceByKey("threeTasksProcess"));
  }
}