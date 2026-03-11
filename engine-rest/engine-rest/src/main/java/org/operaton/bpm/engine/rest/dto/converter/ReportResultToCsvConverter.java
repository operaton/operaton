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
package org.operaton.bpm.engine.rest.dto.converter;

import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.history.DurationReportResult;
import org.operaton.bpm.engine.history.ReportResult;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceReportDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

/**
 * @author Roman Smirnov
 *
 */
public final class ReportResultToCsvConverter {
  protected static final String DELIMITER = ",";

  protected static final String NEW_LINE_SEPARATOR = "\n";

  public static final String DURATION_HEADER = "PERIOD"
                              + DELIMITER + "PERIOD_UNIT"
                              + DELIMITER + "MINIMUM"
                              + DELIMITER + "MAXIMUM"
                              + DELIMITER + "AVERAGE";

  private ReportResultToCsvConverter() {
  }

  public static String convertReportResult(List<ReportResult> reports, String reportType) {
    if (HistoricProcessInstanceReportDto.REPORT_TYPE_DURATION.equals(reportType)) {
      return convertDurationReportResult(reports);
    }

    throw new InvalidRequestException(Status.BAD_REQUEST, "Unkown report type %s".formatted(reportType));
  }

  protected static String convertDurationReportResult(List<ReportResult> reports) {
    StringBuilder buffer = new StringBuilder();

    buffer.append(DURATION_HEADER);

    for (ReportResult report : reports) {
      DurationReportResult durationReport = (DurationReportResult) report;
      buffer.append(NEW_LINE_SEPARATOR);
      buffer.append(durationReport.getPeriod());
      buffer.append(DELIMITER);
      buffer.append(durationReport.getPeriodUnit().toString());
      buffer.append(DELIMITER);
      buffer.append(durationReport.getMinimum());
      buffer.append(DELIMITER);
      buffer.append(durationReport.getMaximum());
      buffer.append(DELIMITER);
      buffer.append(durationReport.getAverage());
    }

    return buffer.toString();
  }
}
