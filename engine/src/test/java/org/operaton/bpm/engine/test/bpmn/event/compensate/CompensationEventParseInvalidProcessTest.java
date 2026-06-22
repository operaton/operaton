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

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parse an invalid process definition and assert the error message.
 *
 * @author Philipp Ossler
 */
@Parameterized
public class CompensationEventParseInvalidProcessTest {

  private static final String PROCESS_DEFINITION_DIRECTORY = "org/operaton/bpm/engine/test/bpmn/event/compensate/";

  @Parameters(name = "process definition = {0}, expected error message = {1}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
        { "CompensationEventParseInvalidProcessTest.illegalCompensateActivityRefParentScope.bpmn20.xml", "Invalid attribute value for 'activityRef': no activity with id 'someServiceInMainProcess' in scope 'subProcess'", new String[] { "throwCompensate" } },
        { "CompensationEventParseInvalidProcessTest.illegalCompensateActivityRefNestedScope.bpmn20.xml", "Invalid attribute value for 'activityRef': no activity with id 'someServiceInNestedScope' in scope 'subProcess'", new String[] { "throwCompensate" } },
        { "CompensationEventParseInvalidProcessTest.invalidActivityRefFails.bpmn20.xml", "Invalid attribute value for 'activityRef':", new String[]{"throwCompensate"} },
        { "CompensationEventParseInvalidProcessTest.multipleCompensationCatchEventsCompensationAttributeMissingFails.bpmn20.xml", "compensation boundary catch must be connected to element with isForCompensation=true", new String[] { "compensateBookHotelEvt", "undoBookHotel", "Association" } },
        { "CompensationEventParseInvalidProcessTest.multipleCompensationCatchEventsFails.bpmn20.xml", "multiple boundary events with compensateEventDefinition not supported on same activity", new String[] { "compensateBookHotelEvt2" } },
        { "CompensationEventParseInvalidProcessTest.multipleCompensationEventSubProcesses.bpmn20.xml", "multiple event subprocesses with compensation start event are not supported on the same scope", new String[] { "startInCompensationScope2" } },
        { "CompensationEventParseInvalidProcessTest.compensationEventSubProcessesAtProcessLevel.bpmn20.xml", "event subprocess with compensation start event is only supported for embedded subprocess", new String[] { "startInCompensationScope" } },
        { "CompensationEventParseInvalidProcessTest.compensationEventSubprocessAndBoundaryEvent.bpmn20.xml", "compensation boundary event and event subprocess with compensation start event are not supported on the same scope", new String[] { "subprocess", "compensateSubProcess" } },
        { "CompensationEventParseInvalidProcessTest.invalidOutgoingSequenceflow.bpmn20.xml", "Invalid outgoing sequence flow of compensation activity 'undoTask'. A compensation activity should not have an incoming or outgoing sequence flow.", new String[] { "undoTask" } },
        { "CompensationEventParseInvalidProcessTest.invalidIncomingSequenceflow.bpmn20.xml", "Invalid incoming sequence flow of compensation activity 'task'. A compensation activity should not have an incoming or outgoing sequence flow.", new String[] { "task" } }
    });
  }

  @Parameter(0)
  public String processDefinitionResource;

  @Parameter(1)
  public String expectedErrorMessage;

  @Parameter(2)
  public String[] bpmnElementIds;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;

  @TestTemplate
  void testParseInvalidProcessDefinition() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource(PROCESS_DEFINITION_DIRECTORY + processDefinitionResource);

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ParseException.class)
      .hasMessageContaining(expectedErrorMessage)
      .satisfies(e -> {
        ParseException pe = (ParseException) e;
        List<Problem> errors = pe.getResourceReports().get(0).getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMainElementId()).isEqualTo(bpmnElementIds[0]);
        if (bpmnElementIds.length == 2) {
          assertThat(errors.get(0).getElementIds()).containsExactlyInAnyOrder(bpmnElementIds);
        }
      });
  }
}
