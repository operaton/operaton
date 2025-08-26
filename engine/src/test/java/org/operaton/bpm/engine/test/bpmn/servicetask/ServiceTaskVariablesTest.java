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

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author Daniel Meyer
 */
class ServiceTaskVariablesTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;

  static boolean isNullInDelegate2;
  static boolean isNullInDelegate3;

  public static class Variable implements Serializable {
    private static final long serialVersionUID = 1L;
    public String value;
  }

  public static class Delegate1 implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Variable v = new Variable();
      execution.setVariable("variable", v);
      v.value = "delegate1";
    }

  }

  public static class Delegate2 implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Variable v = (Variable) execution.getVariable("variable");
      synchronized (ServiceTaskVariablesTest.class) {
        // we expect this to be 'true'
        isNullInDelegate2 = ("delegate1".equals(v.value));
      }
      v.value = "delegate2";
    }

  }

  public static class Delegate3 implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      Variable v = (Variable) execution.getVariable("variable");
      synchronized (ServiceTaskVariablesTest.class) {
        // we expect this to be 'true' as well
        isNullInDelegate3 = ("delegate2".equals(v.value));
      }
    }

  }

  @Deployment
  @Test
  void testSerializedVariablesBothAsync() {

    // in this test, there is an async cont. both before the second and the
    // third service task in the sequence

    runtimeService.startProcessInstanceByKey("process");
    testRule.waitForJobExecutorToProcessAllJobs(10000);

    synchronized (ServiceTaskVariablesTest.class) {
      assertThat(isNullInDelegate2).isTrue();
      assertThat(isNullInDelegate3).isTrue();
    }
  }

  @Deployment
  @Test
  void testSerializedVariablesThirdAsync() {

    // in this test, only the third service task is async

    runtimeService.startProcessInstanceByKey("process");
    testRule.waitForJobExecutorToProcessAllJobs(10000);

    synchronized (ServiceTaskVariablesTest.class) {
      assertThat(isNullInDelegate2).isTrue();
      assertThat(isNullInDelegate3).isTrue();
    }

  }

}

