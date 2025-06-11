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
package org.operaton.bpm.model.bpmn.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.validation.ModelElementValidator;
import org.operaton.bpm.model.xml.validation.ValidationResult;
import org.operaton.bpm.model.xml.validation.ValidationResultType;
import org.operaton.bpm.model.xml.validation.ValidationResults;
import org.operaton.bpm.model.bpmn.instance.Process;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
class ValidateProcessTest {

  @Test
  void validationFailsIfNoStartEventFound() {

    List<ModelElementValidator<?>> validators = new ArrayList<>();
    validators.add(new ProcessStartEventValidator());

    BpmnModelInstance bpmnModelInstance = Bpmn.createProcess().done();

    ValidationResults validationResults = bpmnModelInstance.validate(validators);

    assertThat(validationResults.hasErrors()).isTrue();

    Map<ModelElementInstance, List<ValidationResult>> results = validationResults.getResults();
    assertThat(results).hasSize(1);

    Process process = bpmnModelInstance.getDefinitions().getChildElementsByType(Process.class).iterator().next();
    assertThat(results).containsKey(process);

    List<ValidationResult> resultsForProcess = results.get(process);
    assertThat(resultsForProcess).hasSize(1);

    ValidationResult validationResult = resultsForProcess.get(0);
    assertThat(validationResult.getElement()).isEqualTo(process);
    assertThat(validationResult.getCode()).isEqualTo(10);
    assertThat(validationResult.getMessage()).isEqualTo("Process does not have exactly one start event. Got 0.");
    assertThat(validationResult.getType()).isEqualTo(ValidationResultType.ERROR);

  }

}
