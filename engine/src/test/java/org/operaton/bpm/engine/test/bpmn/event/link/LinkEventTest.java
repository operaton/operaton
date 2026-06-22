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
package org.operaton.bpm.engine.test.bpmn.event.link;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Bernd Ruecker
 */
class LinkEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  HistoryService historyService;
  RepositoryService repositoryService;

  @Deployment
  @Test
  void testValidEventLink() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("linkEventValid");

    List<String> activeActivities = runtimeService.getActiveActivityIds(pi.getId());
    // assert that now the first receive task is active
    assertThat(activeActivities).isEqualTo(List.of("waitAfterLink1"));

    runtimeService.signal(pi.getId());

    activeActivities = runtimeService.getActiveActivityIds(pi.getId());
    // assert that now the second receive task is active
    assertThat(activeActivities).isEqualTo(List.of("waitAfterLink2"));

    runtimeService.signal(pi.getId());
    testRule.assertProcessEnded(pi.getId());

    // validate history
    if(processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).orderByActivityId().asc().list();
      assertThat(activities).hasSize(4);
      assertThat(activities.get(0).getActivityId()).isEqualTo("EndEvent_1");
      assertThat(activities.get(1).getActivityId()).isEqualTo("StartEvent_1");
      assertThat(activities.get(2).getActivityId()).isEqualTo("waitAfterLink1");
      assertThat(activities.get(3).getActivityId()).isEqualTo("waitAfterLink2");
    }

  }

  @Deployment
  @Test
  void testEventLinkMultipleSources() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("linkEventValid");
    List<String> activeActivities = runtimeService.getActiveActivityIds(pi.getId());

    // assert that the link event was triggered and that we are
    assertThat(activeActivities).isEqualTo(List.of("WaitAfterLink", "WaitAfterLink"));

    runtimeService.deleteProcessInstance(pi.getId(), "test done");

    // validate history
    if(processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).orderByActivityId().asc().list();
      assertThat(activities).hasSize(5);
      assertThat(activities.get(0).getActivityId()).isEqualTo("ManualTask_1");
      assertThat(activities.get(1).getActivityId()).isEqualTo("ParallelGateway_1");
      assertThat(activities.get(2).getActivityId()).isEqualTo("StartEvent_1");
      assertThat(activities.get(3).getActivityId()).isEqualTo("WaitAfterLink");
      assertThat(activities.get(4).getActivityId()).isEqualTo("WaitAfterLink");
    }

  }

  @Test
  void testInvalidEventLinkMultipleTargets() {
    var deploymentBuilder = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/link/LinkEventTest.testInvalidEventLinkMultipleTargets.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("process should not deploy because it contains multiple event link targets which is invalid in the BPMN 2.0 spec");
    }
    catch (ParseException e) {
      assertThat(e.getMessage()).contains("Multiple Intermediate Catch Events with the same link event name ('LinkA') are not allowed");
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("IntermediateCatchEvent_2");
    }
  }

  @Test
  void testCatchLinkEventAfterEventBasedGatewayNotAllowed() {
    var deploymentBuilder = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/link/LinkEventTest.testCatchLinkEventAfterEventBasedGatewayNotAllowed.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("process should not deploy because it contains multiple event link targets which is invalid in the BPMN 2.0 spec");
    }
    catch (ParseException e) {
      assertThat(e.getMessage()).contains("IntermediateCatchLinkEvent is not allowed after an EventBasedGateway.");
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("IntermediateCatchEvent_2");
    }
  }
}
