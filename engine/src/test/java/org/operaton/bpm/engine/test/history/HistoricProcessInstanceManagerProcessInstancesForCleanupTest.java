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
package org.operaton.bpm.engine.test.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Svetlana Dorokhova
 */
@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricProcessInstanceManagerProcessInstancesForCleanupTest {

  protected static final String ONE_TASK_PROCESS = "oneTaskProcess";
  protected static final String TWO_TASKS_PROCESS = "twoTasksProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RuntimeService runtimeService;

  @Parameter(0)
  public int processDefiniotion1TTL;

  @Parameter(1)
  public int processDefiniotion2TTL;

  @Parameter(2)
  public int processInstancesOfProcess1Count;

  @Parameter(3)
  public int processInstancesOfProcess2Count;

  @Parameter(4)
  public int daysPassedAfterProcessEnd;

  @Parameter(5)
  public int batchSize;

  @Parameter(6)
  public int resultCount;

  @Parameters
  public static Collection<Object[]> scenarios() {
    return List.of(new Object[][] {
        { 3, 5, 3, 7, 4, 50, 3 },
        //not enough time has passed
        { 3, 5, 3, 7, 2, 50, 0 },
        //all historic process instances are old enough to be cleaned up
        { 3, 5, 3, 7, 6, 50, 10 },
        //batchSize will reduce the result
        { 3, 5, 3, 7, 6, 4, 4 }
    });
  }

  @TestTemplate
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml", "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml" })
  void testFindHistoricProcessInstanceIdsForCleanup() {

    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {

      //given
      //set different TTL for two process definition
      updateTimeToLive(commandContext, ONE_TASK_PROCESS, processDefiniotion1TTL);
      updateTimeToLive(commandContext, TWO_TASKS_PROCESS, processDefiniotion2TTL);
      return null;
    });
    //start processes
    List<String> ids = prepareHistoricProcesses(ONE_TASK_PROCESS, processInstancesOfProcess1Count);
    ids.addAll(prepareHistoricProcesses(TWO_TASKS_PROCESS, processInstancesOfProcess2Count));

    runtimeService.deleteProcessInstances(ids, null, true, true);

    //some days passed
    ClockUtil.setCurrentTime(DateUtils.addDays(new Date(), daysPassedAfterProcessEnd));

    engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired().execute(commandContext -> {
      //when
      List<String> historicProcessInstanceIdsForCleanup = commandContext.getHistoricProcessInstanceManager().findHistoricProcessInstanceIdsForCleanup(
          batchSize, 0, 60);

      //then
      assertThat(historicProcessInstanceIdsForCleanup).hasSize(resultCount);

      if (resultCount > 0) {

        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
            .processInstanceIds(new HashSet<String>(historicProcessInstanceIdsForCleanup)).list();

        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
          assertThat(historicProcessInstance.getEndTime()).isNotNull();
          List<ProcessDefinition> processDefinitions = engineRule.getRepositoryService().createProcessDefinitionQuery()
              .processDefinitionId(historicProcessInstance.getProcessDefinitionId()).list();
          assertThat(processDefinitions).hasSize(1);
          ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) processDefinitions.get(0);
          assertThat(historicProcessInstance.getEndTime().before(DateUtils.addDays(ClockUtil.getCurrentTime(), processDefinition.getHistoryTimeToLive()))).isTrue();
        }
      }

      return null;
    });

  }

  private void updateTimeToLive(CommandContext commandContext, String businessKey, int timeToLive) {
    List<ProcessDefinition> processDefinitions = engineRule.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(businessKey).list();
    assertThat(processDefinitions).hasSize(1);
    ProcessDefinitionEntity processDefinition1 = (ProcessDefinitionEntity) processDefinitions.get(0);
    processDefinition1.setHistoryTimeToLive(timeToLive);
    commandContext.getDbEntityManager().merge(processDefinition1);
  }

  private List<String> prepareHistoricProcesses(String businessKey, Integer processInstanceCount) {
    List<String> processInstanceIds = new ArrayList<>();

    for (int i = 0; i < processInstanceCount; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(businessKey);
      processInstanceIds.add(processInstance.getId());
    }

    return processInstanceIds;
  }

}
