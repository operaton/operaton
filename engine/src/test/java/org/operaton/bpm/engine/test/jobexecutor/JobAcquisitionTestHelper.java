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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public final class JobAcquisitionTestHelper {


  /**
   * suspends random process instances that are active
   */
  public static void suspendInstances(ProcessEngine processEngine, int numInstances) {
    List<ProcessInstance> instancesToSuspend = processEngine.getRuntimeService().createProcessInstanceQuery()
        .active().listPage(0, numInstances);
    if (instancesToSuspend.size() < numInstances) {
      throw new ProcessEngineException("Cannot suspend %d process instances".formatted(numInstances));
    }

    for (ProcessInstance activeInstance : instancesToSuspend) {
      processEngine.getRuntimeService().suspendProcessInstanceById(activeInstance.getId());
    }
  }

  /**
   * activates random process instances that are active
   */
  public static void activateInstances(ProcessEngine processEngine, int numInstances) {
    List<ProcessInstance> instancesToActivate = processEngine.getRuntimeService().createProcessInstanceQuery()
        .suspended().listPage(0, numInstances);
    if (instancesToActivate.size() < numInstances) {
      throw new ProcessEngineException("Cannot activate %d process instances".formatted(numInstances));
    }

    for (ProcessInstance suspendedInstance : instancesToActivate) {
      processEngine.getRuntimeService().activateProcessInstanceById(suspendedInstance.getId());
    }
  }

  public static void assertInBetween(long minimum, long maximum, long actualValue) {
    assertThat(actualValue >= minimum && actualValue <= maximum).as("Expected '%d' to be between '%d' and '%d'".formatted(actualValue, minimum, maximum)).isTrue();
  }

  private JobAcquisitionTestHelper() {
  }

}
