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
package org.operaton.bpm.integrationtest.functional.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.functional.el.beans.ResolveFormDataBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

/**
 * @author Stefan Hentschel.
 */
@ExtendWith(ArquillianExtension.class)
public class ElResolveTaskFormDataTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
      .addClass(ResolveFormDataBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/el/elTaskFormProcessWithFormData.bpmn20.xml");
  }

  @Test
  void testTaskFormDataWithDefaultValueExpression() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("elTaskFormProcess");
    Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

    TaskFormData formData = formService.getTaskFormData(task.getId());
    Object defaultValue = formData.getFormFields().get(0).getValue().getValue();

    assertThat(defaultValue).isNotNull();
    Assertions.assertEquals("testString123", defaultValue);
  }

  @Test
  void testTaskFormDataWithLabelExpression() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("elTaskFormProcess");
    Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();

    TaskFormData formData = formService.getTaskFormData(task.getId());

    String label = formData.getFormFields().get(0).getLabel();
    assertThat(label).isNotNull();
    Assertions.assertEquals("testString123", label);
  }

}
