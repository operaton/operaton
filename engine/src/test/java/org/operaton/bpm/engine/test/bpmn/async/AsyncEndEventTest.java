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
package org.operaton.bpm.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Stefan Hentschel
 */
public class AsyncEndEventTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testAsyncEndEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncEndEvent");
    long count = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).active().count();

    assertThat(runtimeService.createExecutionQuery().activityId("endEvent").count()).isEqualTo(1);
    assertThat(count).isEqualTo(1);

    testRule.executeAvailableJobs();
    count = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count();

    assertThat(runtimeService.createExecutionQuery().activityId("endEvent").active().count()).isZero();
    assertThat(count).isZero();
  }

  @Deployment
  @Test
  public void testAsyncEndEventListeners() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncEndEvent");
    long count = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).active().count();

    assertThat(runtimeService.getVariable(pi.getId(), "listener")).isNull();
    assertThat(runtimeService.createExecutionQuery().activityId("endEvent").count()).isEqualTo(1);
    assertThat(count).isEqualTo(1);

    // as we are standing at the end event, we execute it.
    testRule.executeAvailableJobs();

    count = runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).active().count();
    assertThat(count).isZero();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      // after the end event we have a event listener
      HistoricVariableInstanceQuery name = historyService.createHistoricVariableInstanceQuery()
                                                          .processInstanceId(pi.getId())
                                                          .variableName("listener");
      assertThat(name).isNotNull();
      assertThat(name.singleResult().getValue()).isEqualTo("listener invoked");
    }
  }

  @Deployment
  @Test
  public void testMultipleAsyncEndEvents() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("multipleAsyncEndEvent");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    // should stop at both end events
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();
    assertThat(jobs).hasSize(2);

    // execute one of the end events
    managementService.executeJob(jobs.get(0).getId());
    jobs = managementService.createJobQuery().withRetriesLeft().list();
    assertThat(jobs).hasSize(1);

    // execute the second one
    managementService.executeJob(jobs.get(0).getId());
    // assert that we have finished our instance now
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      // after the end event we have a event listener
      HistoricVariableInstanceQuery name = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(pi.getId())
        .variableName("message");
      assertThat(name).isNotNull();
      assertThat(name.singleResult().getValue()).isTrue();

    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/async/AsyncEndEventTest.testCallActivity-super.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/async/AsyncEndEventTest.testCallActivity-sub.bpmn20.xml"
  })
  @Test
  public void testCallActivity() {
    runtimeService.startProcessInstanceByKey("super");

    ProcessInstance pi = runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey("sub")
        .singleResult();

    assertThat(pi instanceof ExecutionEntity).isTrue();

    assertThat(((ExecutionEntity) pi).getActivityId()).isEqualTo("theSubEnd");

  }

}
