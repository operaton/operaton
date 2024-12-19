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
package org.operaton.bpm.engine.test.bpmn.executionlistener;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.test.bpmn.common.AbstractProcessEngineServicesAccessTest;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.ManualTask;
import org.operaton.bpm.model.bpmn.instance.Task;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;

/**
 * @author Daniel Meyer
 *
 */
public class ListenerProcessEngineServicesAccessTest extends AbstractProcessEngineServicesAccessTest {

  protected Class<?> getTestServiceAccessibleClass() {
    return AccessServicesListener.class;
  }

  protected Class<?> getQueryClass() {
    return PerformQueryListener.class;
  }

  protected Class<?> getStartProcessInstanceClass() {
    return StartProcessListener.class;
  }

  protected Class<?> getProcessEngineStartProcessClass() {
    return ProcessEngineStartProcessListener.class;
  }

  protected Task createModelAccessTask(BpmnModelInstance modelInstance, Class<?> delegateClass) {
    ManualTask task = modelInstance.newInstance(ManualTask.class);
    task.setId("manualTask");
    OperatonExecutionListener executionListener = modelInstance.newInstance(OperatonExecutionListener.class);
    executionListener.setOperatonEvent(ExecutionListener.EVENTNAME_START);
    executionListener.setOperatonClass(delegateClass.getName());
    task.builder().addExtensionElement(executionListener);
    return task;
  }

  public static class AccessServicesListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
      assertCanAccessServices(execution.getProcessEngineServices());
    }
  }

  public static class PerformQueryListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
      assertCanPerformQuery(execution.getProcessEngineServices());
    }
  }

  public static class StartProcessListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
      assertCanStartProcessInstance(execution.getProcessEngineServices());
    }
  }

  public static class ProcessEngineStartProcessListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
      assertCanStartProcessInstance(execution.getProcessEngine());
    }
  }

}
