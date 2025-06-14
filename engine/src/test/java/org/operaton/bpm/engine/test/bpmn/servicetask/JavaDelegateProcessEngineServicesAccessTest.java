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
package org.operaton.bpm.engine.test.bpmn.servicetask;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.test.bpmn.common.AbstractProcessEngineServicesAccessTest;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;
import org.operaton.bpm.model.bpmn.instance.Task;

/**
 * @author Daniel Meyer
 *
 */
public class JavaDelegateProcessEngineServicesAccessTest extends AbstractProcessEngineServicesAccessTest {

  protected Class<?> getTestServiceAccessibleClass() {
    return AccessServicesJavaDelegate.class;
  }

  protected Class<?> getQueryClass() {
    return PerformQueryJavaDelegate.class;
  }

  protected Class<?> getStartProcessInstanceClass() {
    return StartProcessJavaDelegate.class;
  }

  protected Class<?> getProcessEngineStartProcessClass() {
    return ProcessEngineStartProcessJavaDelegate.class;
  }

  protected Task createModelAccessTask(BpmnModelInstance modelInstance, Class<?> delegateClass) {
    ServiceTask serviceTask = modelInstance.newInstance(ServiceTask.class);
    serviceTask.setId("serviceTask");
    serviceTask.setOperatonClass(delegateClass.getName());
    return serviceTask;
  }

  public static class AccessServicesJavaDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      assertCanAccessServices(execution.getProcessEngineServices());
    }
  }

  public static class PerformQueryJavaDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      assertCanPerformQuery(execution.getProcessEngineServices());
    }
  }

  public static class StartProcessJavaDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      assertCanStartProcessInstance(execution.getProcessEngineServices());
    }
  }

  public static class ProcessEngineStartProcessJavaDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
      assertCanStartProcessInstance(execution.getProcessEngine());
    }
  }
}
