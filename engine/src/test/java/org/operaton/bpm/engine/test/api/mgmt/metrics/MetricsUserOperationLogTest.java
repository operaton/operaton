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
package org.operaton.bpm.engine.test.api.mgmt.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 *
 * @author Tobias Metzke
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class MetricsUserOperationLogTest {

  protected ManagementService managementService;
  protected HistoryService historyService;
  protected IdentityService identityService;

  @Test
  void testDeleteMetrics() {
    // given
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteMetrics(null);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isNull();
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isNull();
  }

  @Test
  void testDeleteMetricsWithTimestamp() {
    // given
    Date timestamp = ClockUtil.getCurrentTime();
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteMetrics(timestamp);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("timestamp");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(String.valueOf(timestamp.getTime()));
  }

  @Test
  void testDeleteMetricsWithReporterId() {
    // given
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteMetrics(null, "reporter1");
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("reporter");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo("reporter1");
  }

  @Test
  void testDeleteMetricsWithTimestampAndReporterId() {
    // given
    Date timestamp = ClockUtil.getCurrentTime();
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteMetrics(timestamp, "reporter1");
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(2L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().property("reporter").singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("reporter");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo("reporter1");

    logEntry = historyService.createUserOperationLogQuery().property("timestamp").singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("timestamp");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(String.valueOf(timestamp.getTime()));
  }

  @Test
  void shouldLogDeletionOfTaskMetricsWithTimestamp() {
    // given
    Date timestamp = ClockUtil.getCurrentTime();
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteTaskMetrics(timestamp);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().property("timestamp").singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.TASK_METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isEqualTo("timestamp");
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isEqualTo(String.valueOf(timestamp.getTime()));
  }

  @Test
  void shouldLogDeletionOfTaskMetricsWithoutTimestamp() {
    // given
    identityService.setAuthenticatedUserId("userId");

    // when
    managementService.deleteTaskMetrics(null);
    identityService.clearAuthentication();

    // then
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(1L);
    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getEntityType()).isEqualTo(EntityTypes.TASK_METRICS);
    assertThat(logEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(logEntry.getProperty()).isNull();
    assertThat(logEntry.getOrgValue()).isNull();
    assertThat(logEntry.getNewValue()).isNull();
  }
}
