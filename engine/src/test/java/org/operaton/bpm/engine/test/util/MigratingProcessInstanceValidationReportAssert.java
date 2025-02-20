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
package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.engine.migration.MigratingActivityInstanceValidationReport;
import org.operaton.bpm.engine.migration.MigratingProcessInstanceValidationReport;
import org.operaton.bpm.engine.migration.MigratingTransitionInstanceValidationReport;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;

public class MigratingProcessInstanceValidationReportAssert {

  protected MigratingProcessInstanceValidationReport actual;

  public MigratingProcessInstanceValidationReportAssert(MigratingProcessInstanceValidationReport report) {
    this.actual = report;
  }

  public MigratingProcessInstanceValidationReportAssert isNotNull() {
    Assertions.assertThat(actual).as("Expected report to be not null").isNotNull();

    return this;
  }

  public MigratingProcessInstanceValidationReportAssert hasProcessInstance(ProcessInstance processInstance) {
    return hasProcessInstanceId(processInstance.getId());
  }

  public MigratingProcessInstanceValidationReportAssert hasProcessInstanceId(String processInstanceId) {
    isNotNull();

    Assertions.assertThat(actual.getProcessInstanceId())
      .as("Expected report to be for process instance")
      .isEqualTo(processInstanceId);

    return this;
  }

  public MigratingProcessInstanceValidationReportAssert hasActivityInstanceFailures(String sourceScopeId, String... expectedFailures) {
    isNotNull();

    MigratingActivityInstanceValidationReport actualReport = null;
    for (MigratingActivityInstanceValidationReport instanceReport : actual.getActivityInstanceReports()) {
      if (sourceScopeId.equals(instanceReport.getSourceScopeId())) {
        actualReport = instanceReport;
        break;
      }
    }

    Assertions.assertThat(actualReport).as("No validation report found for source scope: " + sourceScopeId).isNotNull();

    assertFailures(sourceScopeId, Arrays.asList(expectedFailures), actualReport.getFailures());

    return this;
  }

  public MigratingProcessInstanceValidationReportAssert hasTransitionInstanceFailures(String sourceScopeId, String... expectedFailures) {
    isNotNull();

    MigratingTransitionInstanceValidationReport actualReport = null;
    for (MigratingTransitionInstanceValidationReport instanceReport : actual.getTransitionInstanceReports()) {
      if (sourceScopeId.equals(instanceReport.getSourceScopeId())) {
        actualReport = instanceReport;
        break;
      }
    }

    Assertions.assertThat(actualReport).as("No validation report found for source scope: " + sourceScopeId).isNotNull();

    assertFailures(sourceScopeId, Arrays.asList(expectedFailures), actualReport.getFailures());

    return this;
  }

  protected void assertFailures(String sourceScopeId, List<String> expectedFailures, List<String> actualFailures) {
    // Transform expected failures into predicates that check for the presence of substrings.
    List<String> unmatchedFailures = expectedFailures.stream()
        .filter(expected -> actualFailures.stream().noneMatch(failure -> failure.contains(expected)))
        .toList();

    // Use AssertJ's assertion with helpful error message formatting.
    Assertions.assertThat(unmatchedFailures)
        .as("Expected failures for source scope: %s%nExpected:%n%s%nBut found:%n%s",
            sourceScopeId,
            joinFailures(expectedFailures),
            joinFailures(actualFailures))
        .isEmpty();
  }

  public static MigratingProcessInstanceValidationReportAssert assertThat(MigratingProcessInstanceValidationReport report) {
    return new MigratingProcessInstanceValidationReportAssert(report);
  }

  public String joinFailures(List<String> failures) {
    StringBuilder builder = new StringBuilder();
    for (Object failure : failures) {
      builder.append("\t\t").append(failure).append("\n");
    }

    return builder.toString();
  }



}
