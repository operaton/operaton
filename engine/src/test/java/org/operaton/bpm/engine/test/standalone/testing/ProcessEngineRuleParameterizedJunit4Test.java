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
package org.operaton.bpm.engine.test.standalone.testing;
import java.util.List;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Thorben Lindhauer
 */
@RunWith(Parameterized.class)
public class ProcessEngineRuleParameterizedJunit4Test {

  @Parameters
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
      { 1 }, { 2 }
    });
  }

  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  public ProcessEngineRuleParameterizedJunit4Test(int parameter) {

  }

  /**
   * Unnamed @Deployment annotations don't work with parameterized Unit tests
   */
  @Test
  @Deployment
  public void ruleUsageExample() {
    RuntimeService runtimeService = engineRule.getRuntimeService();
    runtimeService.startProcessInstanceByKey("ruleUsage");

    TaskService taskService = engineRule.getTaskService();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/standalone/testing/ProcessEngineRuleParameterizedJunit4Test.ruleUsageExample.bpmn20.xml")
  public void ruleUsageExampleWithNamedAnnotation() {
    RuntimeService runtimeService = engineRule.getRuntimeService();
    runtimeService.startProcessInstanceByKey("ruleUsage");

    TaskService taskService = engineRule.getTaskService();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  /**
   * The rule should work with tests that have no deployment annotation
   */
  @Test
  public void testWithoutDeploymentAnnotation() {
    assertThat("aString").isEqualTo("aString");
  }

}
