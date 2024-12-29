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
package org.operaton.bpm.engine.spring.test.transaction.modification;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.logging.LogFactory;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/operaton/bpm/engine/spring/test/transaction/ProcessInstanceModificationInTransactionTest-applicationContext.xml"})
public class ProcessInstanceModificationInTransactionTest {

  @Autowired
  @Rule
  public ProcessEngineRule rule;

  @Autowired
  public ProcessEngine processEngine;

  @Autowired
  RuntimeService runtimeService;

  @Autowired
  RepositoryService repositoryService;

  @Autowired
  UserBean userBean;

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
    VariableInstance variable = rule.getRuntimeService().createVariableInstanceQuery().processInstanceIdIn(procInst.getId()).variableName("createDate").singleResult();
    assertThat(variable).isNotNull();
    HistoricVariableInstance historicVariable = rule.getHistoryService().createHistoricVariableInstanceQuery().singleResult();
    assertThat(historicVariable.getName()).isEqualTo(variable.getName());
    assertThat(historicVariable.getState()).isEqualTo(HistoricVariableInstance.STATE_CREATED);
  }

  private void deployModelInstance(BpmnModelInstance modelInstance) {
    DeploymentBuilder deploymentbuilder = repositoryService.createDeployment();
    deploymentbuilder.addModelInstance("process0.bpmn", modelInstance);
    Deployment deployment = deploymentbuilder.deploy();
    rule.manageDeployment(deployment);
  }
}
