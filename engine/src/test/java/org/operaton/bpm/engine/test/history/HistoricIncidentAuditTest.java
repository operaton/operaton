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

import java.util.Arrays;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.impl.interceptor.Session;
import org.operaton.bpm.engine.impl.interceptor.SessionFactory;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogManager;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.mockito.Mockito;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricIncidentAuditTest {

  private static SessionFactory sessionFactory = Mockito.spy(new MockSessionFactory());

  public static class MockSessionFactory implements SessionFactory {

    @Override
    public Class<?> getSessionType() {
      return HistoricJobLogManager.class;
    }

    @Override
    public Session openSession() {
      return new HistoricJobLogManager();
    }
  }

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration -> {

    configuration.setCustomSessionFactories(Arrays.asList(sessionFactory));
  });

  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  public ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  @Test
  public void shouldNotQueryForHistoricJobLogWhenSettingJobToZeroRetries() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
    .startEvent().operatonAsyncAfter().endEvent().done();

    testRule.deploy(modelInstance);

    RuntimeService runtimeService = engineRule.getRuntimeService();
    runtimeService.startProcessInstanceByKey("process");

    ManagementService managementService = engineRule.getManagementService();
    Job job = managementService.createJobQuery().singleResult();

    Mockito.reset(sessionFactory);

    // when
    managementService.setJobRetries(job.getId(), 0);


    // then
    Mockito.verify(sessionFactory, Mockito.never()).openSession();
  }


  @Test
  public void shouldNotQueryForHistoricJobLogWhenSettingExternalTaskToZeroRetries() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
    .startEvent().serviceTask().operatonExternalTask("topic").endEvent().done();

    testRule.deploy(modelInstance);

    RuntimeService runtimeService = engineRule.getRuntimeService();
    runtimeService.startProcessInstanceByKey("process");

    ExternalTaskService externalTaskService = engineRule.getExternalTaskService();
    ExternalTask externalTask = externalTaskService.createExternalTaskQuery().singleResult();

    Mockito.reset(sessionFactory);

    // when
    externalTaskService.setRetries(externalTask.getId(), 0);

    // then
    Mockito.verify(sessionFactory, Mockito.never()).openSession();
  }
}
