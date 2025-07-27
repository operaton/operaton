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
package org.operaton.bpm.engine.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class ProcessDataLoggingContextMultipleEnginesTest {

  private static final String PVM_LOGGER = "org.operaton.bpm.engine.pvm";
  private static final String DELEGATE_LOGGER = LogEngineNameDelegate.class.getName();

  private static final String PROCESS = "process";

  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension().watch(PVM_LOGGER, DELEGATE_LOGGER).level(Level.DEBUG);

  protected ProcessEngine engine1;
  protected ProcessEngine engine2;

  private static final String ENGINE1_NAME = "ProcessDataLoggingContextMultipleEnginesTest-engine1";
  private static final String ENGINE2_NAME = "ProcessDataLoggingContextMultipleEnginesTest-engine2";

  @BeforeEach
  void startEngines() {
    engine1 = createProcessEngine(ENGINE1_NAME);
    engine2 = createProcessEngine(ENGINE2_NAME);
  }

  @AfterEach
  void closeEngines() {
    try {
      engine1.close();
    }
    finally {
      engine1 = null;
    }
    try {
      engine2.close();
    }
    finally {
      engine2 = null;
    }
  }

  @Test
  void shouldHaveProcessEngineNameAvailableInMdc() {
    // given
    engine1.getRepositoryService().createDeployment().addModelInstance("test.bpmn", modelOneTaskProcess()).deploy();

    // when produce logging
    engine1.getRuntimeService().startProcessInstanceByKey(PROCESS);
    engine1.getTaskService().complete(engine1.getTaskService().createTaskQuery().singleResult().getId());

    // then
    List<ILoggingEvent> filteredLog = loggingRule.getLog();
    List<String> engineNames = filteredLog.stream().map(log -> log.getMDCPropertyMap().get("engineName")).toList();
    assertThat(engineNames).hasSize(filteredLog.size());
    assertThat(engineNames.stream().distinct().toList()).containsExactly(ENGINE1_NAME);
  }

  @Test
  void shouldHaveProcessEngineNameAvailableInMdcForAllEngines() {
    // given
    engine1.getRepositoryService().createDeployment().addModelInstance("test1.bpmn", modelLogDelegateProcess()).deploy();
    engine2.getRepositoryService().createDeployment().addModelInstance("test2.bpmn", modelLogDelegateProcess()).deploy();

    // when
    engine1.getRuntimeService().startProcessInstanceByKey(PROCESS);
    engine2.getRuntimeService().startProcessInstanceByKey(PROCESS);

    // then
    List<ILoggingEvent> log = loggingRule.getFilteredLog(LogEngineNameDelegate.LOG_MESSAGE);
    List<String> engineNames = log.stream().map(l -> l.getMDCPropertyMap().get("engineName")).toList();
    // make sure all log entries have access to the engineName MDC property
    assertThat(engineNames).hasSize(log.size());
    assertThat(engineNames.stream().distinct().toList()).containsExactlyInAnyOrder(ENGINE1_NAME, ENGINE2_NAME);

    List<ILoggingEvent> filteredLogEngine1 = loggingRule.getFilteredLog(ENGINE1_NAME);
    List<String> engineNamesEngine1 = filteredLogEngine1.stream().map(l -> l.getMDCPropertyMap().get("engineName")).toList();
    assertThat(engineNamesEngine1).hasSameSizeAs(filteredLogEngine1);
    assertThat(engineNamesEngine1.stream().distinct().toList()).containsExactly(ENGINE1_NAME);

    List<ILoggingEvent> filteredLogEngine2 = loggingRule.getFilteredLog(ENGINE2_NAME);
    List<String> engineNamesEngine2 = filteredLogEngine2.stream().map(l -> l.getMDCPropertyMap().get("engineName")).toList();
    assertThat(engineNamesEngine2).hasSameSizeAs(filteredLogEngine2);
    assertThat(engineNamesEngine2.stream().distinct().toList()).containsExactly(ENGINE2_NAME);
  }

  private ProcessEngine createProcessEngine(String name) {
    StandaloneInMemProcessEngineConfiguration processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();

    processEngineConfiguration.setProcessEngineName(name);
    processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:%s".formatted(name));
    processEngineConfiguration.setEnforceHistoryTimeToLive(false);

    return processEngineConfiguration.buildProcessEngine();
  }

  protected BpmnModelInstance modelOneTaskProcess() {
    return Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
          .userTask("waitState")
        .endEvent("end")
        .done();
  }

  protected BpmnModelInstance modelLogDelegateProcess() {
    return Bpmn.createExecutableProcess(PROCESS)
        .startEvent("start")
          .serviceTask()
            .operatonClass(LogEngineNameDelegate.class.getName())
        .endEvent("end")
        .done();
  }


}
