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
package org.operaton.bpm.engine.spring.test.transaction.modification;

import org.apache.ibatis.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:org/operaton/bpm/engine/spring/test/transaction/ProcessInstanceModificationInTransactionTest-applicationContext.xml"})
class ProcessInstanceModificationInTransactionTest {

  @Autowired
  public ProcessEngine processEngine;

  @Autowired
  RuntimeService runtimeService;

  @Autowired
  RepositoryService repositoryService;

  @Autowired
  HistoryService historyService;

  @Autowired
  UserBean userBean;
  private Deployment deployment;

  @BeforeEach
  void init() {
    LogFactory.useSlf4jLogging();
  }

  @Test
  void shouldBeAbleToPerformModification() {

    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("TestProcess")
      .startEvent()
      .intermediateCatchEvent("TimerEvent")
        .timerWithDate("${calculateTimerDate.execute(execution)}")
        .operatonExecutionListenerDelegateExpression("end", "${deleteVariableListener}")
      .endEvent()
      .done();

    deployModelInstance(modelInstance);
    final ProcessInstance procInst = runtimeService.startProcessInstanceByKey("TestProcess");

    // when
    userBean.completeUserTaskAndModifyInstanceInOneTransaction(procInst);

    // then
    VariableInstance variable =
        runtimeService.createVariableInstanceQuery().processInstanceIdIn(procInst.getId()).variableName("createDate").singleResult();
    assertThat(variable).isNotNull();
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  @AfterEach
  void tearDown() {
    if (deployment != null) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  private void deployModelInstance(BpmnModelInstance modelInstance) {
    DeploymentBuilder deploymentbuilder = repositoryService.createDeployment();
    deploymentbuilder.addModelInstance("process0.bpmn", modelInstance);
    deployment = deploymentbuilder.deploy();
  }
}
