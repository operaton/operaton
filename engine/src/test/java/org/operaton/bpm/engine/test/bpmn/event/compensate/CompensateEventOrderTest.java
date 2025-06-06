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
package org.operaton.bpm.engine.test.bpmn.event.compensate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.event.compensate.helper.IncreaseCurrentTimeServiceTask;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.AssociationDirection;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Association;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.BoundaryEvent;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;

/**
 * @author Svetlana Dorokhova
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
class CompensateEventOrderTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  @Test
  void testTwoCompensateEventsInReverseOrder() {
    //given
    BpmnModelInstance model = Bpmn.createExecutableProcess("Process_1")
        .startEvent()
          .serviceTask("serviceTask1")
            .operatonClass(IncreaseCurrentTimeServiceTask.class.getName())
            .boundaryEvent("compensationBoundary1")
            .compensateEventDefinition()
            .compensateEventDefinitionDone()
          .moveToActivity("serviceTask1")
          .serviceTask("serviceTask2")
            .operatonClass(IncreaseCurrentTimeServiceTask.class.getName())
            .boundaryEvent("compensationBoundary2")
            .compensateEventDefinition()
            .compensateEventDefinitionDone()
          .moveToActivity("serviceTask2")
          .intermediateThrowEvent("compensationEvent")
            .compensateEventDefinition()
            .waitForCompletion(true)
            .compensateEventDefinitionDone()
        .endEvent()
        .done();

    addServiceTaskCompensationHandler(model, "compensationBoundary1", "A");
    addServiceTaskCompensationHandler(model, "compensationBoundary2", "B");

    testHelper.deploy(model);

    //when
    engineRule.getRuntimeService().startProcessInstanceByKey("Process_1", Variables.createVariables().putValue("currentTime", new Date()));

    //then compensation activities are executed in the reverse order
    List<HistoricActivityInstance> list = engineRule.getHistoryService().createHistoricActivityInstanceQuery()
        .orderByHistoricActivityInstanceEndTime().asc()
        .list();

    long indexA = searchForActivityIndex(list, "A");
    long indexB = searchForActivityIndex(list, "B");

    assertThat(indexA).isNotEqualTo(-1);
    assertThat(indexB).isNotEqualTo(-1);

    assertThat(indexA)
      .as("Compensation activities were executed in wrong order.")
      .isGreaterThan(indexB);

  }

  private long searchForActivityIndex(List<HistoricActivityInstance> historicActivityInstances, String activityId) {
    for (int i = 0; i < historicActivityInstances.size(); i++) {
      HistoricActivityInstance historicActivityInstance = historicActivityInstances.get(i);
      if (historicActivityInstance.getActivityId().equals(activityId)) {
        return i;
      }
    }
    return -1;
  }

  private void addServiceTaskCompensationHandler(BpmnModelInstance modelInstance, String boundaryEventId, String compensationHandlerId) {

    BoundaryEvent boundaryEvent = modelInstance.getModelElementById(boundaryEventId);
    BaseElement scope = (BaseElement) boundaryEvent.getParentElement();

    ServiceTask compensationHandler = modelInstance.newInstance(ServiceTask.class);
    compensationHandler.setId(compensationHandlerId);
    compensationHandler.setForCompensation(true);
    compensationHandler.setOperatonClass(IncreaseCurrentTimeServiceTask.class.getName());
    scope.addChildElement(compensationHandler);

    Association association = modelInstance.newInstance(Association.class);
    association.setAssociationDirection(AssociationDirection.One);
    association.setSource(boundaryEvent);
    association.setTarget(compensationHandler);
    scope.addChildElement(association);

  }


}
