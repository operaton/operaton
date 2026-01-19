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
package org.operaton.bpm.engine.test.bpmn.event.signal;
import java.util.Collection;
import java.util.List;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ParseException;
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
public class SignalEventParseInvalidProcessTest {

  private static final String PROCESS_DEFINITION_DIRECTORY = "org/operaton/bpm/engine/test/bpmn/event/signal/";

  @Parameters(name = "process definition = {0}, expected error message = {1}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
        { "InvalidProcessWithDuplicateSignalNames.bpmn20.xml", "duplicate signal name", "alertSignal2" },
        { "InvalidProcessWithNoSignalName.bpmn20.xml", "signal with id 'alertSignal' has no name", "alertSignal" },
        { "InvalidProcessWithSignalNoId.bpmn20.xml", "signal must have an id", null },
        { "InvalidProcessWithSignalNoRef.bpmn20.xml", "signalEventDefinition does not have required property 'signalRef'", "signalEvent" },
        { "InvalidProcessWithMultipleSignalStartEvents.bpmn20.xml", "Cannot have more than one signal event subscription with name 'signal'" , "start2" },
        { "InvalidProcessWithMultipleInterruptingSignalEventSubProcesses.bpmn20.xml", "Cannot have more than one signal event subscription with name 'alert'", "subprocessStartEvent2" }
    });
  }

  @Parameter(0)
  public String processDefinitionResource;

  @Parameter(1)
  public String expectedErrorMessage;

  @Parameter(2)
  public String elementIds;

  RepositoryService repositoryService;

  @TestTemplate
  void testParseInvalidProcessDefinition() {
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource(PROCESS_DEFINITION_DIRECTORY + processDefinitionResource);
    try {
      deploymentBuilder.deploy();

      fail("exception expected: " + expectedErrorMessage);
    } catch (ParseException e) {
      assertTextPresent(expectedErrorMessage, e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo(elementIds);
    }
  }

  public void assertTextPresent(String expected, String actual) {
    if (actual == null || !actual.contains(expected)) {
      throw new AssertionFailedError("expected presence of [%s], but was [%s]".formatted(expected, actual));
    }
  }
}
