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
package org.operaton.bpm.model.bpmn.instance;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.model.bpmn.BpmnTestConstants;
import org.operaton.bpm.model.bpmn.ProcessType;
import org.operaton.bpm.model.bpmn.impl.instance.Supports;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class ProcessTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(CallableElement.class, false);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return List.of(
      new ChildElementAssumption(Auditing.class, 0, 1),
      new ChildElementAssumption(Monitoring.class, 0, 1),
      new ChildElementAssumption(Property.class),
      new ChildElementAssumption(LaneSet.class),
      new ChildElementAssumption(FlowElement.class),
      new ChildElementAssumption(Artifact.class),
      new ChildElementAssumption(ResourceRole.class),
      new ChildElementAssumption(CorrelationSubscription.class),
      new ChildElementAssumption(Supports.class)
    );
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return List.of(
      new AttributeAssumption("processType", false, false, ProcessType.None),
      new AttributeAssumption("isClosed", false, false, false),
      new AttributeAssumption("isExecutable"),
      // TODO: definitionalCollaborationRef
      /** operaton extensions */
      new AttributeAssumption(OPERATON_NS, "candidateStarterGroups"),
      new AttributeAssumption(OPERATON_NS, "candidateStarterUsers"),
      new AttributeAssumption(OPERATON_NS, "jobPriority"),
      new AttributeAssumption(OPERATON_NS, "taskPriority"),
      new AttributeAssumption(OPERATON_NS, "historyTimeToLive"),
      new AttributeAssumption(OPERATON_NS, "isStartableInTasklist", false, false, true),
      new AttributeAssumption(OPERATON_NS, "versionTag")
    );
  }

  @Test
  void testOperatonJobPriority() {
    Process process = modelInstance.newInstance(Process.class);
    assertThat(process.getOperatonJobPriority()).isNull();

    process.setOperatonJobPriority("15");

    assertThat(process.getOperatonJobPriority()).isEqualTo("15");
  }

  @Test
  void testOperatonTaskPriority() {
    //given
    Process proc = modelInstance.newInstance(Process.class);
    assertThat(proc.getOperatonTaskPriority()).isNull();
    //when
    proc.setOperatonTaskPriority(BpmnTestConstants.TEST_PROCESS_TASK_PRIORITY);
    //then
    assertThat(proc.getOperatonTaskPriority()).isEqualTo(BpmnTestConstants.TEST_PROCESS_TASK_PRIORITY);
  }

  @Test
  void testOperatonHistoryTimeToLive() {
    //given
    Process proc = modelInstance.newInstance(Process.class);
    assertThat(proc.getOperatonHistoryTimeToLiveString()).isNull();
    //when
    proc.setOperatonHistoryTimeToLiveString(BpmnTestConstants.TEST_HISTORY_TIME_TO_LIVE.toString());
    //then
    assertThat(proc.getOperatonHistoryTimeToLiveString()).isEqualTo(BpmnTestConstants.TEST_HISTORY_TIME_TO_LIVE.toString());
  }
}
