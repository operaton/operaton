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
package org.operaton.bpm.engine.test.bpmn.event.escalation;

import java.util.Collection;
import java.util.List;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Parse an invalid process definition and assert the error message.
 *
 * @author Philipp Ossler
 */
@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class EscalationEventParseInvalidProcessTest {

  private static final String PROCESS_DEFINITION_DIRECTORY = "org/operaton/bpm/engine/test/bpmn/event/escalation/";

  @Parameters(name = "process definition = {0}, expected error message = {1}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
        { "EscalationEventParseInvalidProcessTest.missingIdOnEscalation.bpmn20.xml", "escalation must have an id", new String[] {} },
        { "EscalationEventParseInvalidProcessTest.invalidAttachement.bpmn20.xml", "An escalation boundary event should only be attached to a subprocess, a call activity or an user task", new String[] { "escalationBoundaryEvent" } },
        { "EscalationEventParseInvalidProcessTest.invalidEscalationRefOnBoundaryEvent.bpmn20.xml", "could not find escalation with id 'invalid-escalation'", new String[] { "escalationBoundaryEvent" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationBoundaryEventsWithSameEscalationCode.bpmn20.xml", "multiple escalation boundary events with the same escalationCode 'escalationCode' are not supported on same scope", new String[] { "escalationBoundaryEvent2" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationBoundaryEventsWithAndWithoutEscalationCode.bpmn20.xml", "The same scope can not contains an escalation boundary event without escalation code and another one with escalation code.", new String[] { "escalationBoundaryEvent2" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationBoundaryEventsWithoutEscalationCode.bpmn20.xml", "The same scope can not contains more than one escalation boundary event without escalation code.", new String[] { "escalationBoundaryEvent2" } },
        { "EscalationEventParseInvalidProcessTest.missingEscalationCodeOnIntermediateThrowingEscalationEvent.bpmn20.xml", "throwing escalation event must have an 'escalationCode'", new String[] { "escalationThrowingEvent" } },
        { "EscalationEventParseInvalidProcessTest.missingEscalationRefOnIntermediateThrowingEvent.bpmn20.xml", "escalationEventDefinition does not have required attribute 'escalationRef'", new String[] { "escalationThrowingEvent" } },
        { "EscalationEventParseInvalidProcessTest.invalidEscalationRefOnIntermediateThrowingEvent.bpmn20.xml", "could not find escalation with id 'invalid-escalation'", new String[] { "escalationThrowingEvent" } },
        { "EscalationEventParseInvalidProcessTest.missingEscalationCodeOnEscalationEndEvent.bpmn20.xml", "escalation end event must have an 'escalationCode'", new String[] { "theEnd" } },
        { "EscalationEventParseInvalidProcessTest.missingEscalationRefOnEndEvent.bpmn20.xml", "escalationEventDefinition does not have required attribute 'escalationRef'", new String[] { "theEnd" } },
        { "EscalationEventParseInvalidProcessTest.invalidEscalationRefOnEndEvent.bpmn20.xml", "could not find escalation with id 'invalid-escalation'", new String[] { "theEnd" } },
        { "EscalationEventParseInvalidProcessTest.invalidEscalationRefOnEscalationEventSubprocess.bpmn20.xml", "could not find escalation with id 'invalid-escalation'", new String[] { "escalationStartEvent" } },
        { "EscalationEventParseInvalidProcessTest.multipleInterruptingEscalationEventSubprocesses.bpmn20.xml", "multiple escalation event subprocesses with the same escalationCode 'escalationCode' are not supported on same scope", new String[] { "escalationStartEvent2" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationEventSubprocessWithSameEscalationCode.bpmn20.xml", "multiple escalation event subprocesses with the same escalationCode 'escalationCode' are not supported on same scope", new String[] { "escalationStartEvent2" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationEventSubprocessWithAndWithoutEscalationCode.bpmn20.xml", "The same scope can not contains an escalation event subprocess without escalation code and another one with escalation code.", new String[] { "escalationStartEvent2" } },
        { "EscalationEventParseInvalidProcessTest.multipleEscalationEventSubprocessWithoutEscalationCode.bpmn20.xml", "The same scope can not contains more than one escalation event subprocess without escalation code.", new String[] { "escalationStartEvent2" } }
    });
  }

  @Parameter(0)
  public String processDefinitionResource;

  @Parameter(1)
  public String expectedErrorMessage;

  @Parameter(2)
  public String[] bpmnElementIds;

  RepositoryService repositoryService;

  @TestTemplate
  void testParseInvalidProcessDefinition() {
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource(PROCESS_DEFINITION_DIRECTORY + processDefinitionResource);

    try {
      deploymentBuilder.deploy();
      fail("exception expected: " + expectedErrorMessage);
    } catch (ParseException e) {
      assertExceptionMessageContainsText(e, expectedErrorMessage);
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      for (int i = 0; i < bpmnElementIds.length; i++) {
        assertThat(errors.get(i).getMainElementId()).isEqualTo(bpmnElementIds[i]);
      }
    }
  }

  public void assertExceptionMessageContainsText(Exception e, String expectedMessage) {
    String actualMessage = e.getMessage();
    if (actualMessage == null || !actualMessage.contains(expectedMessage)) {
      throw new AssertionFailedError("expected presence of [" + expectedMessage + "], but was [" + actualMessage + "]");
    }
  }
}
