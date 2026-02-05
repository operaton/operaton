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
package org.operaton.bpm.engine.test.bpmn.event.error;

import java.util.Collection;
import java.util.List;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parse an invalid process definition with error events and assert the error message.
 */
@Parameterized
@ExtendWith(ProcessEngineExtension.class)
public class ErrorEventParseInvalidProcessTest {
  
  private static final String PROCESS_DEFINITION_DIRECTORY = "org/operaton/bpm/engine/test/bpmn/event/error/";

  @Parameters(name = "process definition = {0}, expected error message = {1}")
  public static Collection<Object[]> data() {
    return List.of(new Object[][] {
        { "ErrorEventParseInvalidProcessTest.operatonErrorEventDefinitionMissingExpression.bpmn20.xml",
            "operaton:errorEventDefinition element must have 'expression' attribute",
            new String[] { "externalTask" } }
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
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource(PROCESS_DEFINITION_DIRECTORY + processDefinitionResource);

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .satisfies(e -> {
          ParseException pe = (ParseException) e;
          assertThat(pe.getMessage()).contains(expectedErrorMessage);
          List<Problem> errors = pe.getResourceReports().get(0).getErrors();
          for (int i = 0; i < bpmnElementIds.length; i++) {
            assertThat(errors.get(i).getMainElementId()).isEqualTo(bpmnElementIds[i]);
          }
        });
  }
}
