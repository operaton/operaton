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
package org.operaton.bpm.engine.test.api.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Miklas Boskamp
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class ProcessInstanceQueryByLeafInstancesTest {

  RuntimeService runtimeService;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml", "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  void testQueryByLeafInstancesThreeLayers() {
    /*
     * nested structure:
     * superProcessWithNestedSubProcess
     * +-- nestedSubProcess
     *     +-- subProcess
     */
    ProcessInstance threeLayerProcess = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");
    ProcessInstanceQuery simpleSubProcessQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("simpleSubProcess");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3L);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("nestedSubProcessQueryTest").count()).isEqualTo(1L);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("nestedSimpleSubProcess").count()).isEqualTo(1L);
    assertThat(simpleSubProcessQuery.count()).isEqualTo(1L);

    ProcessInstance instance = runtimeService.createProcessInstanceQuery().leafProcessInstances().singleResult();
    assertThat(instance.getRootProcessInstanceId()).isEqualTo(threeLayerProcess.getId());
    assertThat(instance.getId()).isEqualTo(simpleSubProcessQuery.singleResult().getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  void testQueryByLeafInstancesTwoLayers() {
    /*
     * nested structure:
     * nestedSubProcess
     * +-- subProcess
     */
    ProcessInstance twoLayerProcess = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    ProcessInstanceQuery simpleSubProcessQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("simpleSubProcess");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2L);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("nestedSimpleSubProcess").count()).isEqualTo(1L);
    assertThat(simpleSubProcessQuery.count()).isEqualTo(1L);

    ProcessInstance instance = runtimeService.createProcessInstanceQuery().leafProcessInstances().singleResult();
    assertThat(instance.getRootProcessInstanceId()).isEqualTo(twoLayerProcess.getId());
    assertThat(instance.getId()).isEqualTo(simpleSubProcessQuery.singleResult().getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/subProcess.bpmn20.xml"})
  void testQueryByLeafInstancesOneLayer() {
    ProcessInstance process = runtimeService.startProcessInstanceByKey("simpleSubProcess");
    ProcessInstanceQuery simpleSubProcessQuery = runtimeService.createProcessInstanceQuery().processDefinitionKey("simpleSubProcess");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1L);
    assertThat(simpleSubProcessQuery.count()).isEqualTo(1L);

    ProcessInstance instance = runtimeService.createProcessInstanceQuery().leafProcessInstances().singleResult();
    assertThat(instance.getRootProcessInstanceId()).isEqualTo(process.getId());
    assertThat(instance.getId()).isEqualTo(simpleSubProcessQuery.singleResult().getId());
  }
}
