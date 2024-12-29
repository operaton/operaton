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
package org.operaton.bpm.engine.spring.test.transaction.inner.rollback;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:org/operaton/bpm/engine/spring/test/transaction/SpringInnerTransactionRollbackTest-applicationContext.xml"})
class SpringInnerTransactionRollbackTest extends SpringProcessEngineTestCase {

  @Autowired
  public ProcessEngine processEngine;

  @Autowired
  RuntimeService runtimeService;

  @Autowired
  HistoryService historyService;

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/spring/test/transaction/SpringInnerTransactionRollbackTest.shouldRollbackProcessData-outer.bpmn20.xml",
    "org/operaton/bpm/engine/spring/test/transaction/SpringInnerTransactionRollbackTest.shouldRollbackProcessData-inner.bpmn20.xml"
  })
  void shouldRollbackProcessData() {
    // given

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("OuterTxNestedTransactionTest");

    // then

    // outer process instance should be finished
    ProcessInstance outerProcessInstance = runtimeService.createProcessInstanceQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();
    assertThat(outerProcessInstance).isNull();

    // inner process instance shouldn't exist
    List<ProcessInstance> innerProcessInstances = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey("InnerTxNestedTransactionTest")
      .list();
    assertThat(innerProcessInstances).isEmpty();


    // historic inner PI shouldn't be available
    List<HistoricProcessInstance> historicProcessInstances = historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("InnerTxNestedTransactionTest")
        .list();
    assertThat(historicProcessInstances).isEmpty();
  }
}
