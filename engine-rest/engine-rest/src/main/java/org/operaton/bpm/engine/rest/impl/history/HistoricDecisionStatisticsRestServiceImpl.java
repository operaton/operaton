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
package org.operaton.bpm.engine.rest.impl.history;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatistics;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceStatisticsQuery;
import org.operaton.bpm.engine.rest.dto.history.HistoricDecisionInstanceStatisticsDto;
import org.operaton.bpm.engine.rest.history.HistoricDecisionStatisticsRestService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Askar Akhmerov
 */
public class HistoricDecisionStatisticsRestServiceImpl implements HistoricDecisionStatisticsRestService {

  protected ProcessEngine processEngine;

  public HistoricDecisionStatisticsRestServiceImpl(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Override
  public List<HistoricDecisionInstanceStatisticsDto> getDecisionStatistics(String decisionRequirementsDefinitionId, String decisionInstanceId) {
    List<HistoricDecisionInstanceStatisticsDto> result = new ArrayList<>();
    HistoricDecisionInstanceStatisticsQuery statisticsQuery = processEngine.getHistoryService()
        .createHistoricDecisionInstanceStatisticsQuery(decisionRequirementsDefinitionId);
    if (decisionInstanceId != null) {
      statisticsQuery.decisionInstanceId(decisionInstanceId);
    }

    List<HistoricDecisionInstanceStatistics> statistics = statisticsQuery.unlimitedList();

    for (HistoricDecisionInstanceStatistics stats : statistics) {
      result.add(HistoricDecisionInstanceStatisticsDto.fromDecisionDefinitionStatistics(stats));
    }

    return result;
  }

}
