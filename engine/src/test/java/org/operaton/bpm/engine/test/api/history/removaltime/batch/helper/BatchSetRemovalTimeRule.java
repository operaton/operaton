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
package org.operaton.bpm.engine.test.api.history.removaltime.batch.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.DefaultHistoryRemovalTimeProvider;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.resources.GetByteArrayCommand;
import org.operaton.bpm.engine.test.bpmn.async.FailingExecutionListener;
import org.operaton.bpm.engine.test.util.BatchRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.CallActivityBuilder;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.builder.StartEventBuilder;
import org.junit.runner.Description;

/**
 * @author Tassilo Weidner
 */
public class BatchSetRemovalTimeRule extends BatchRule {

  public static final Date CURRENT_DATE = new GregorianCalendar(2013, Calendar.MARCH, 18, 13, 0, 0).getTime();
  public static final Date REMOVAL_TIME = new Date(1363609000000L);

  public BatchSetRemovalTimeRule(ProcessEngineRule engineRule, ProcessEngineTestRule engineTestRule) {
    super(engineRule, engineTestRule);
  }

  @Override
  protected void starting(Description description) {
    getProcessEngineConfiguration()
      .setHistoryRemovalTimeProvider(new DefaultHistoryRemovalTimeProvider())
      .setHistoryRemovalTimeStrategy(ProcessEngineConfiguration.HISTORY_REMOVAL_TIME_STRATEGY_START)
      .initHistoryRemovalTime();

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        getProcessEngineConfiguration().getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();

    ClockUtil.setCurrentTime(CURRENT_DATE);

    super.starting(description);
  }

  @Override
  protected void finished(Description description) {
    super.finished(description);

    getProcessEngineConfiguration()
      .setHistoryRemovalTimeProvider(null)
      .setHistoryRemovalTimeStrategy(null)
      .initHistoryRemovalTime();

    getProcessEngineConfiguration().setBatchOperationHistoryTimeToLive(null);
    getProcessEngineConfiguration().setBatchOperationsForHistoryCleanup(null);

    getProcessEngineConfiguration().setBatchOperationHistoryTimeToLive(null);
    getProcessEngineConfiguration().setHistoryCleanupStrategy(null);
    getProcessEngineConfiguration().initHistoryCleanup();

    getProcessEngineConfiguration().setInvocationsPerBatchJob(1);

    getProcessEngineConfiguration().setDmnEnabled(true);

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        getProcessEngineConfiguration().getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();

    getProcessEngineConfiguration().setEnableHistoricInstancePermissions(false);
    getProcessEngineConfiguration().setAuthorizationEnabled(false);
  }

  @Override
  public void clearDatabase() {
    super.clearDatabase();
    clearAuthorization();
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public TestProcessBuilder process() {
    return new TestProcessBuilder();
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return engineRule.getProcessEngineConfiguration();
  }

  public void updateHistoryTimeToLive(String key, int ttl) {
    updateHistoryTimeToLive(ttl, key);
  }

  public void updateHistoryTimeToLive(int ttl, String... keys) {
    for (String key : keys) {
      String processDefinitionId = engineRule.getRepositoryService()
        .createProcessDefinitionQuery()
        .processDefinitionKey(key)
        .singleResult()
        .getId();

      engineRule.getRepositoryService().updateProcessDefinitionHistoryTimeToLive(processDefinitionId, ttl);
    }
  }

  public void updateHistoryTimeToLiveDmn(String key, int ttl) {
    updateHistoryTimeToLiveDmn(ttl, key);
  }

  public void updateHistoryTimeToLiveDmn(int ttl, String... keys) {
    for (String key : keys) {
      String decisionDefinitionId = engineRule.getRepositoryService()
        .createDecisionDefinitionQuery()
        .decisionDefinitionKey(key)
        .singleResult()
        .getId();

      engineRule.getRepositoryService().updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, ttl);
    }
  }

  public void enableAuth() {
    getProcessEngineConfiguration().setAuthorizationEnabled(true);
  }

  public void disableAuth() {
    getProcessEngineConfiguration().setAuthorizationEnabled(false);
  }

  public void clearAuthorization() {
    engineRule.getAuthorizationService()
        .createAuthorizationQuery()
        .list()
        .forEach(authorization -> {
          engineRule.getAuthorizationService()
              .deleteAuthorization(authorization.getId());
        });
  }

  public class TestProcessBuilder {

    protected static final String PROCESS_KEY = "process";
    protected static final String ROOT_PROCESS_KEY = "rootProcess";

    ProcessBuilder builder = Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLiveString(null);

    StartEventBuilder startEventBuilder = builder.startEvent();
    ProcessBuilder rootProcessBuilder = null;
    Integer ttl;
    CallActivityBuilder callActivityBuilder;

    public TestProcessBuilder ttl(Integer ttl) {
      this.ttl = ttl;
      return this;
    }

    public TestProcessBuilder async() {
      startEventBuilder.operatonAsyncBefore();

      return this;
    }

    public TestProcessBuilder ruleTask(String ref) {
      startEventBuilder
        .businessRuleTask()
        .operatonDecisionRef(ref);

      return this;
    }

    public TestProcessBuilder call() {
      rootProcessBuilder = Bpmn.createExecutableProcess(ROOT_PROCESS_KEY)
          .operatonHistoryTimeToLiveString(null);

      callActivityBuilder = rootProcessBuilder
        .startEvent()
        .callActivity()
          .calledElement(PROCESS_KEY);

      return this;
    }

    public TestProcessBuilder passVars(String... vars) {
      for (String variable : vars) {
        callActivityBuilder.operatonIn(variable, variable);
      }

      callActivityBuilder.endEvent();

      return this;
    }

    public TestProcessBuilder userTask() {
      startEventBuilder
        .userTask("userTask")
        .name("userTask")
        .operatonAssignee("anAssignee");

      return this;
    }

    public TestProcessBuilder scriptTask() {
      startEventBuilder
        .scriptTask()
        .scriptFormat("groovy")
        .scriptText("throw new RuntimeException()");

      return this;
    }

    public TestProcessBuilder externalTask() {
      startEventBuilder
        .serviceTask()
        .operatonExternalTask("aTopicName");

      return this;
    }

    public TestProcessBuilder serviceTask() {
      startEventBuilder
        .serviceTask()
        .operatonExpression("${true}");

      return this;
    }

    public TestProcessBuilder failingCustomListener() {
      startEventBuilder
        .userTask()
        .operatonExecutionListenerClass("end", FailingExecutionListener.class);

      return this;
    }

    public TestProcessBuilder deploy() {
      if (ttl != null) {
        if (rootProcessBuilder != null) {
          rootProcessBuilder.operatonHistoryTimeToLive(ttl);
        } else {
          builder.operatonHistoryTimeToLive(ttl);
        }
      }

      BpmnModelInstance process = startEventBuilder.endEvent().done();

      engineTestRule.deploy(process);

      if (rootProcessBuilder != null) {
        engineTestRule.deploy(rootProcessBuilder.done());
      }

      return this;
    }

    public String start() {
      return startWithVariables(null);
    }

    public String startWithVariables(Map<String, Object> variables) {
      String key = null;

      if (rootProcessBuilder != null) {
        key = ROOT_PROCESS_KEY;
      } else {
        key = PROCESS_KEY;
      }

      return engineRule.getRuntimeService().startProcessInstanceByKey(key, variables).getId();
    }
  }

  public static Date addDays(Date date, int amount) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, amount);
    return c.getTime();
  }

  public ByteArrayEntity findByteArrayById(String byteArrayId) {
    CommandExecutor commandExecutor = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired();
    return commandExecutor.execute(new GetByteArrayCommand(byteArrayId));
  }

}
