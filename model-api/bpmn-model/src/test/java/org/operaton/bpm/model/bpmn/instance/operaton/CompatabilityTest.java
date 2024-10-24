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
package org.operaton.bpm.model.bpmn.instance.operaton;

import static org.operaton.bpm.model.bpmn.BpmnTestConstants.PROCESS_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.OperatonExtensionsTest;
import org.operaton.bpm.model.bpmn.impl.BpmnModelConstants;
import org.operaton.bpm.model.bpmn.impl.instance.ProcessImpl;
import org.operaton.bpm.model.bpmn.instance.ExtensionElements;
import org.junit.Test;

/**
 * Test to check the interoperability when changing elements and attributes with
 * the {@link BpmnModelConstants#CAMUNDA_NS}. In contrast to
 * {@link OperatonExtensionsTest} this test uses directly the get*Ns() methods to
 * check the expected value.
 *
 * @author Ronny Bräunlich
 *
 */
public class CompatabilityTest {

  @Test
  public void modifyingElementWithActivitiNsKeepsIt() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatabilityTest.xml"));
    ProcessImpl process = modelInstance.getModelElementById(PROCESS_ID);
    ExtensionElements extensionElements = process.getExtensionElements();
    Collection<OperatonExecutionListener> listeners = extensionElements.getChildElementsByType(OperatonExecutionListener.class);
    String listenerClass = "org.foo.Bar";
    for (OperatonExecutionListener listener : listeners) {
      listener.setOperatonClass(listenerClass);
    }
    for (OperatonExecutionListener listener : listeners) {
      assertThat(listener.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "class"), is(listenerClass));
    }
  }

  @Test
  public void modifyingAttributeWithActivitiNsKeepsIt() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatabilityTest.xml"));
    ProcessImpl process = modelInstance.getModelElementById(PROCESS_ID);
    String priority = "9000";
    process.setOperatonJobPriority(priority);
    process.setOperatonTaskPriority(priority);
    Integer historyTimeToLive = 10;
    process.setOperatonHistoryTimeToLive(historyTimeToLive);
    process.setOperatonIsStartableInTasklist(false);
    process.setOperatonVersionTag("v1.0.0");
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "jobPriority"), is(priority));
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "taskPriority"), is(priority));
    assertThat(process.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "historyTimeToLive"), is(historyTimeToLive.toString()));
    assertThat(process.isOperatonStartableInTasklist(), is(false));
    assertThat(process.getOperatonVersionTag(), is("v1.0.0"));
  }

}
