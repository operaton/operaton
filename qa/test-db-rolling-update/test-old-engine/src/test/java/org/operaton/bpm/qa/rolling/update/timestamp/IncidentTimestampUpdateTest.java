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
package org.operaton.bpm.qa.rolling.update.timestamp;

import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Nikola Koevski
 */
@ScenarioUnderTest("IncidentTimestampUpdateScenario")
@Origin("7.11.0")
public class IncidentTimestampUpdateTest extends AbstractTimestampUpdateTest {

  @ScenarioUnderTest("initIncidentTimestamp.1")
  @Test
  public void testIncidentTimestampConversion() {
    // given
    String processInstanceId = rule.jobQuery().singleResult().getProcessInstanceId();

    // when
    Incident incident = runtimeService.createIncidentQuery()
      .processInstanceId(processInstanceId)
      .singleResult();

    // assume
    assertNotNull(incident);

    long incidentCount = runtimeService.createIncidentQuery()
      .processInstanceId(processInstanceId)
      .count();

    // then
    assertThat(incident.getIncidentTimestamp(), is(TIMESTAMP));
  }
}
