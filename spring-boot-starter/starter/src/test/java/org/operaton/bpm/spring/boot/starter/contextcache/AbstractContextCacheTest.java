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
package org.operaton.bpm.spring.boot.starter.contextcache;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikola Koevski
 */
public abstract class AbstractContextCacheTest {

  @Autowired
  protected ApplicationContext applicationContext;

  @Autowired
  protected ProcessEngine processEngine;

  @Autowired
  protected RuntimeService runtimeService;

  protected String testName;
  protected static final Map<String, Integer> contextMap = new HashMap<>(3);
  protected String processEngineName;

  @Test
  protected void bpmAssert() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestProcess");

    // then
    assertThat(processInstance).isStarted()
      .task().hasName("do something")
      .isNotAssigned();

    // finally
    taskService().complete(task().getId());
  }

  @Test
  protected void dbIsolation() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestProcess");

    // then
    assertThat(processInstance).isNotNull();
    long numInstances = runtimeService.createProcessInstanceQuery().count();
    assertThat(numInstances).isEqualTo(1);
  }

  @Test
  protected void engineName() {
    // do
    assertThat(processEngine.getName()).isEqualTo(processEngineName);
  }

  @Test
  protected void engineRegistration() {
    // do
    ProcessEngine registeredEngine = ProcessEngines.getProcessEngine("default");
    assertThat(registeredEngine).isNotSameAs(processEngine);
  }
}
