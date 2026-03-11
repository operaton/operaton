/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parse an invalid process definition with error events and assert the error message.
 */
@ExtendWith(ProcessEngineExtension.class)
class ErrorEventParseInvalidProcessTest {

  private static final String PROCESS_DEFINITION_DIRECTORY = "org/operaton/bpm/engine/test/bpmn/event/error/";

  RepositoryService repositoryService;

  @Test
  void shouldFailToParseOperatonErrorEventDefinitionMissingExpression() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource(PROCESS_DEFINITION_DIRECTORY +
            "ErrorEventParseInvalidProcessTest.operatonErrorEventDefinitionMissingExpression.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .satisfies(e -> {
          ParseException pe = (ParseException) e;
          assertThat(pe.getMessage()).contains("operaton:errorEventDefinition element must have 'expression' attribute");
          List<Problem> errors = pe.getResourceReports().get(0).getErrors();
          assertThat(errors.get(0).getMainElementId()).isEqualTo("externalTask");
        });
  }
}
