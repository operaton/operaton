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
package org.operaton.bpm.engine.test.api.runtime;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import java.util.Set;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class UpdateProcessInstancesSuspendStateTest {

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected HistoryService historyService;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    historyService = engineRule.getHistoryService();
  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchSuspensionById() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(processInstance1.getId(), processInstance2.getId()).suspend();

    // Update the process instances and they are suspended
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchActivationById() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(processInstance1.getId(), processInstance2.getId()).suspend();

    // when they are activated again
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(processInstance1.getId(), processInstance2.getId()).activate();

    // Update the process instances and they are active again
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(p1c.isSuspended());
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(p2c.isSuspended());

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchSuspensionByIdArray() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspend();

    // Update the process instances and they are suspended
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchActivationByIdArray() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).suspend();

    // when they are activated again
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId())).activate();


    // Update the process instances and they are active again
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(p1c.isSuspended());
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(p2c.isSuspended());

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchSuspensionByProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().active()).suspend();

    // Update the process instances and they are suspended
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testBatchActivationByProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");

    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().active()).suspend();


    // when they are activated again
    runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery().suspended()).activate();


    // Update the process instances and they are active again
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(p1c.isSuspended());
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(p2c.isSuspended());

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  public void testBatchSuspensionByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");


    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(
              Set.of(processInstance1.getId(), processInstance2.getId()))).suspend();

    // Update the process instances and they are suspended
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertThat(p1c.isSuspended()).isTrue();
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertThat(p2c.isSuspended()).isTrue();

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  public void testBatchActivationByHistoricProcessInstanceQuery() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");


    // when the process instances are suspended
    runtimeService.updateProcessInstanceSuspensionState()
      .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(Set.of(processInstance1.getId(), processInstance2.getId()))).suspend();

    // when they are activated again
    runtimeService.updateProcessInstanceSuspensionState()
      .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery().processInstanceIds(Set.of(processInstance1.getId(), processInstance2.getId()))).activate();


    // Update the process instances and they are active again
    ProcessInstance p1c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).singleResult();
    assertFalse(p1c.isSuspended());
    ProcessInstance p2c = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance2.getId()).singleResult();
    assertFalse(p2c.isSuspended());

  }


  @Test
  public void testEmptyProcessInstanceListSuspend() {
    // given
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds();

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::suspend)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No process instance ids given");

  }

  @Test
  public void testEmptyProcessInstanceListActivateUpdateProcessInstancesSuspendStateAsyncTest() {
    // given
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds();

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::activate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("No process instance ids given");

  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testNullProcessInstanceListActivate() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId(), null));

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::activate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot be null");

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/externaltask/twoExternalTaskProcess.bpmn20.xml"})
  public void testNullProcessInstanceListSuspend() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneExternalTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("twoExternalTaskProcess");
    var updateProcessInstancesSuspensionStateBuilder = runtimeService.updateProcessInstanceSuspensionState()
      .byProcessInstanceIds(Arrays.asList(processInstance1.getId(), processInstance2.getId(), null));

    // when/then
    assertThatThrownBy(updateProcessInstancesSuspensionStateBuilder::suspend)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot be null");

  }

}
