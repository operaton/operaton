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
package org.operaton.bpm.model.xml.impl.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.validation.ValidationResult;
import org.operaton.bpm.model.xml.validation.ValidationResultCollector;
import org.operaton.bpm.model.xml.validation.ValidationResultType;
import org.operaton.bpm.model.xml.validation.ValidationResults;

/**
 * @author Daniel Meyer
 *
 */
public class ValidationResultsCollectorImpl implements ValidationResultCollector {

  protected ModelElementInstance currentElement;

  protected Map<ModelElementInstance, List<ValidationResult>> collectedResults = new HashMap<>();

  protected int errorCount;
  protected int warningCount;

  @Override
  public void addError(int code, String message) {
    resultsForCurrentElement()
      .add(new ModelValidationResultImpl(currentElement, ValidationResultType.ERROR, code, message));

    ++errorCount;
  }

  @Override
  public void addWarning(int code, String message) {
    resultsForCurrentElement()
      .add(new ModelValidationResultImpl(currentElement, ValidationResultType.WARNING, code, message));

    ++warningCount;
  }

  public void setCurrentElement(ModelElementInstance currentElement) {
    this.currentElement = currentElement;
  }

  public ValidationResults getResults() {
    return new ModelValidationResultsImpl(collectedResults, errorCount, warningCount);
  }

  protected List<ValidationResult> resultsForCurrentElement() {
    return collectedResults.computeIfAbsent(currentElement, k -> new ArrayList<>());
  }

}
