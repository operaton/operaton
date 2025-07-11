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
package org.operaton.bpm.model.bpmn;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.RootElement;
import org.operaton.bpm.model.bpmn.util.BpmnModelResource;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class ProcessTest extends BpmnModelTest {

  @Test
  @BpmnModelResource
  void shouldImportProcess() {

    ModelElementInstance modelElementById = bpmnModelInstance.getModelElementById("exampleProcessId");
    assertThat(modelElementById).isNotNull();

    Collection<RootElement> rootElements = bpmnModelInstance.getDefinitions().getRootElements();
    assertThat(rootElements).hasSize(1);
    org.operaton.bpm.model.bpmn.instance.Process process = (Process) rootElements.iterator().next();

    assertThat(process.getId()).isEqualTo("exampleProcessId");
    assertThat(process.getName()).isNull();
    assertThat(process.getProcessType()).isEqualTo(ProcessType.None);
    assertThat(process.isExecutable()).isFalse();
    assertThat(process.isClosed()).isFalse();



  }


}
