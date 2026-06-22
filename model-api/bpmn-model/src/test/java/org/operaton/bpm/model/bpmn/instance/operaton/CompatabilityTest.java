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
package org.operaton.bpm.model.bpmn.instance.operaton;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.OperatonExtensionsTest;
import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.impl.instance.ProcessImpl;
import org.operaton.bpm.model.bpmn.instance.ExtensionElements;

import static org.operaton.bpm.model.bpmn.BpmnTestConstants.PROCESS_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to check the interoperability when changing elements and attributes with
 * the {@link BpmnModelConstants#CAMUNDA_NS}. In contrast to
 * {@link OperatonExtensionsTest} this test uses directly the get*Ns() methods to
 * check the expected value.
 *
 * @author Ronny Bräunlich
 *
 */
class CompatabilityTest {

  @Test
  void modifyingElementWithCamundaNsKeepsIt() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatabilityTest.xml"));
    ProcessImpl process = modelInstance.getModelElementById(PROCESS_ID);
    ExtensionElements extensionElements = process.getExtensionElements();
    Collection<OperatonExecutionListener> listeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    String listenerClass = "org.foo.Bar";
    for (OperatonExecutionListener listener : listeners) {
      listener.setOperatonClass(listenerClass);
    }
    for (OperatonExecutionListener listener : listeners) {
      assertThat(listener.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "class")).isEqualTo(listenerClass);
    }
  }

  @Test
  void modifyingAttributeWithCamundaNsKeepsIt() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatabilityTest.xml"));
    ProcessImpl process = modelInstance.getModelElementById(PROCESS_ID);
    String priority = "9000";
    process.setOperatonJobPriority(priority);
    process.setOperatonTaskPriority(priority);
    Integer historyTimeToLive = 10;
    process.setOperatonHistoryTimeToLiveString(historyTimeToLive.toString());
    process.setOperatonIsStartableInTasklist(false);
    process.setOperatonVersionTag("v1.0.0");
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.OPERATON_ATTRIBUTE_JOB_PRIORITY)).isEqualTo(priority);
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.OPERATON_ATTRIBUTE_TASK_PRIORITY)).isEqualTo(priority);
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE)).isEqualTo(historyTimeToLive.toString());
    assertThat(process.isOperatonStartableInTasklist()).isFalse();
    assertThat(process.getOperatonVersionTag()).isEqualTo("v1.0.0");
  }

}
