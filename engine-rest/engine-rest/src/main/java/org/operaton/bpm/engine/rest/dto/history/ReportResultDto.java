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
package org.operaton.bpm.engine.rest.dto.history;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.ReportResult;

/**
 * @author Roman Smirnov
 *
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type"
)
@JsonSubTypes({
  @Type(DurationReportResultDto.class)
})
public abstract class ReportResultDto {

  protected int period;
  protected String periodUnit;

  public int getPeriod() {
    return period;
  }

  public String getPeriodUnit() {
    return periodUnit;
  }

  public static ReportResultDto fromReportResult(ReportResult reportResult) {

    ReportResultDto dto = null;

    if (reportResult instanceof DurationReportResult durationReport) {
      dto = DurationReportResultDto.fromDurationReportResult(durationReport);
    } else {
      throw new IllegalArgumentException("Unsupported report result type: " + reportResult.getClass().getName());
    }

    dto.period = reportResult.getPeriod();
    dto.periodUnit = reportResult.getPeriodUnit().toString();

    return dto;
  }

}
