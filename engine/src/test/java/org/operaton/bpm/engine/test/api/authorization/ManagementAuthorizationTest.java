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
package org.operaton.bpm.engine.test.api.authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.authorization.SystemPermissions;
import org.operaton.bpm.engine.authorization.TaskPermissions;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.management.Metrics;
import org.operaton.bpm.engine.management.SchemaLogEntry;
import org.operaton.bpm.engine.management.TableMetaData;
import org.operaton.bpm.engine.management.TablePage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Roman Smirnov
 *
 */
class ManagementAuthorizationTest extends AuthorizationTest {

  protected static final String REQUIRED_ADMIN_AUTH_EXCEPTION = "ENGINE-03029 Required admin authenticated group or user.";
  protected static final String DUMMY_PROPERTY = "dummy-property";
  protected static final String DUMMY_VALUE = "aPropertyValue";
  protected static final String DUMMY_METRIC = "dummyMetric";

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    managementService.deleteProperty(DUMMY_PROPERTY);
  }

  // get table count //////////////////////////////////////////////

  @Test
  void shouldGetTableCountAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    Map<String, Long> tableCount = managementService.getTableCount();

    // then
    assertThat(tableCount).isNotEmpty();
  }

  @Test
  void shouldGetTableCountWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    Map<String, Long> tableCount = managementService.getTableCount();

    // then
    assertThat(tableCount).isNotEmpty();
  }

  @Test
  void shouldGetTableCountWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    Map<String, Long> tableCount = managementService.getTableCount();

    // then
    assertThat(tableCount).isNotEmpty();
  }

  @Test
  void shouldNotGetTableCountWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getTableCount();
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // get table name //////////////////////////////////////////////

  @Test
  void shouldGetTableNameAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    // when
    String tableName = managementService.getTableName(ProcessDefinitionEntity.class);

    // then
    assertThat(tablePrefix + "ACT_RE_PROCDEF").isEqualTo(tableName);
  }

  @Test
  void shouldGetTableNameWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    // when
    String tableName = managementService.getTableName(ProcessDefinitionEntity.class);

    // then
    assertThat(tablePrefix + "ACT_RE_PROCDEF").isEqualTo(tableName);
  }

  @Test
  void shouldGetTableNameAdminAndWithPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    // when
    String tableName = managementService.getTableName(ProcessDefinitionEntity.class);

    // then
    assertThat(tablePrefix + "ACT_RE_PROCDEF").isEqualTo(tableName);
  }

  @Test
  void shouldNotGetTableNameWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getTableName(ProcessDefinitionEntity.class);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // get table meta data //////////////////////////////////////////////

  @Test
  void shouldGetTableMetaDataAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    TableMetaData tableMetaData = managementService.getTableMetaData("ACT_RE_PROCDEF");

    // then
    assertThat(tableMetaData).isNotNull();
  }

  @Test
  void shouldGetTableMetaDataWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    TableMetaData tableMetaData = managementService.getTableMetaData("ACT_RE_PROCDEF");

    // then
    assertThat(tableMetaData).isNotNull();
  }

  @Test
  void shouldGetTableMetaDataWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    TableMetaData tableMetaData = managementService.getTableMetaData("ACT_RE_PROCDEF");

    // then
    assertThat(tableMetaData).isNotNull();
  }


  @Test
  void shouldNotGetTableMetaDataWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getTableMetaData("ACT_RE_PROCDEF");
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // table page query //////////////////////////////////

  @Test
  void shouldNotPerformTablePageQueryWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.createTablePageQuery().tableName("ACT_RE_PROCDEF").listPage(0, Integer.MAX_VALUE);
    })
        // then
        .hasMessage(REQUIRED_ADMIN_AUTH_EXCEPTION);
  }

  @Test
  void shouldPerformTablePageQueryAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    // when
    TablePage page = managementService.createTablePageQuery().tableName(tablePrefix + "ACT_RE_PROCDEF").listPage(0, Integer.MAX_VALUE);

    // then
    assertThat(page).isNotNull();
  }

  // get history level /////////////////////////////////

  @Test
  void shouldGetHistoryLevelAsOperatonAdmin() {
    //given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    int historyLevel = managementService.getHistoryLevel();

    // then
    assertThat(historyLevel).isEqualTo(processEngineConfiguration.getHistoryLevel().getId());
  }

  @Test
  void shouldGetHistoryLevelWithPermission() {
    //given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    int historyLevel = managementService.getHistoryLevel();

    // then
    assertThat(historyLevel).isEqualTo(processEngineConfiguration.getHistoryLevel().getId());
  }

  @Test
  void shouldGetHistoryLevelAdminAndWithPermission() {
    //given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    int historyLevel = managementService.getHistoryLevel();

    // then
    assertThat(historyLevel).isEqualTo(processEngineConfiguration.getHistoryLevel().getId());
  }

  @Test
  void shouldNotGetHistoryLevelWithoutAuthorization() {
    // given
    assertThatThrownBy(() -> {
      // when
      managementService.getHistoryLevel();
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // database schema upgrade ///////////////////////////

  @Test
  void shouldNotPerformDataSchemaUpgradeWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.databaseSchemaUpgrade(null, null, null);
    })
        // then
        .hasMessage(REQUIRED_ADMIN_AUTH_EXCEPTION);
  }

  // get properties  ///////////////////////////


  @Test
  void shouldGetPropertiesAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
      Map<String, String> properties = managementService.getProperties();

    // then
      assertThat(properties).isNotEmpty();
  }

  @Test
  void shouldGetPropertiesWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    Map<String, String> properties = managementService.getProperties();

    // then
    assertThat(properties).isNotEmpty();
  }

  @Test
  void shouldGetPropertiesWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    Map<String, String> properties = managementService.getProperties();

    // then
    assertThat(properties).isNotEmpty();
  }

  @Test
  void shouldNotGetPropertiesWithWrongPermission() {
    // given
    createGrantAuthorization(Resources.TASK, "*", userId, TaskPermissions.DELETE);

    assertThatThrownBy(() -> {
      // when
      managementService.getProperties();
    })
    // then
    .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  @Test
  void shouldNotGetPropertiesWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getProperties();
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // set properties ///////////////////////////

  @Test
  void shouldSetPropertyAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
      managementService.setProperty(DUMMY_PROPERTY, DUMMY_VALUE);

    // then
      disableAuthorization();
    assertThat(managementService.getProperties()).containsEntry(DUMMY_PROPERTY, DUMMY_VALUE);
  }

  @Test
  void shouldSetPropertyWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    // when
    managementService.setProperty(DUMMY_PROPERTY, DUMMY_VALUE);

    // then
    disableAuthorization();
    assertThat(managementService.getProperties()).containsEntry(DUMMY_PROPERTY, DUMMY_VALUE);
  }

  @Test
  void shouldSetPropertyWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    // when
    managementService.setProperty(DUMMY_PROPERTY, DUMMY_VALUE);

    // then
    disableAuthorization();
    assertThat(managementService.getProperties()).containsEntry(DUMMY_PROPERTY, DUMMY_VALUE);
  }

  @Test
  void shouldNotSetPropertyWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.setProperty(DUMMY_PROPERTY, DUMMY_VALUE);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.SET));
  }

  // delete properties ///////////////////////////

  @Test
  void shouldDeletePropertyAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    managementService.setProperty(DUMMY_VALUE, DUMMY_PROPERTY);

    // when
      managementService.deleteProperty(DUMMY_PROPERTY);

    // then
      disableAuthorization();
      assertThat(managementService.getProperties().get(DUMMY_PROPERTY)).isNull();
  }

  @Test
  void shouldDeletePropertyWithPermission() {
    // given
    disableAuthorization();
    managementService.setProperty(DUMMY_VALUE, DUMMY_PROPERTY);
    enableAuthorization();
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);

    // when
    managementService.deleteProperty(DUMMY_PROPERTY);

    // then
    disableAuthorization();
    assertThat(managementService.getProperties().get(DUMMY_PROPERTY)).isNull();
    enableAuthorization();
  }

  @Test
  void shouldDeletePropertyWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);
    managementService.setProperty(DUMMY_VALUE, DUMMY_PROPERTY);

    // when
    managementService.deleteProperty(DUMMY_PROPERTY);

    // then
    assertThat(managementService.getProperties().get(DUMMY_PROPERTY)).isNull();
  }

  @Test
  void shouldNotDeletePropertyWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.deleteProperty(DUMMY_PROPERTY);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.DELETE));
  }

  // delete metrics //////////////////////////////////////

  @Test
  void shouldDeleteMetricsAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    processEngineConfiguration.getDbMetricsReporter().reportValueAtOnce(DUMMY_METRIC, 15);

    // when
    managementService.deleteMetrics(null);

    // then
    assertThat(managementService.createMetricsQuery().name(DUMMY_METRIC).sum()).isZero();
  }

  @Test
  void shouldDeleteMetricsWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);

    processEngineConfiguration.getDbMetricsReporter().reportValueAtOnce(DUMMY_METRIC, 15);

    // when
    managementService.deleteMetrics(null);

    // then
    assertThat(managementService.createMetricsQuery().name(DUMMY_METRIC).sum()).isZero();
  }

  @Test
  void shouldDeleteMetricsWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);

    processEngineConfiguration.getDbMetricsReporter().reportValueAtOnce(DUMMY_METRIC, 15);

    // when
    managementService.deleteMetrics(null);

    // then
    assertThat(managementService.createMetricsQuery().name(DUMMY_METRIC).sum()).isZero();
  }

  @Test
  void shouldNotDeleteMetricsWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.deleteMetrics(null);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.DELETE));
  }

  // delete task metrics /////////////////////////////////

  @Test
  void shouldDeleteTaskMetricsAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    managementService.deleteTaskMetrics(null);

    // then
    // no exception
    assertThat(managementService.createMetricsQuery().name(Metrics.UNIQUE_TASK_WORKERS).sum()).isZero();
  }

  @Test
  void shouldDeleteTaskMetricsWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);

    // when
    managementService.deleteTaskMetrics(null);

    // then
    // no exception
    assertThat(managementService.createMetricsQuery().name(Metrics.UNIQUE_TASK_WORKERS).sum()).isZero();
  }

  @Test
  void shouldDeleteTaskMetricsWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.DELETE);

    // when
    managementService.deleteTaskMetrics(null);

    // then
    // no exception
    assertThat(managementService.createMetricsQuery().name(Metrics.UNIQUE_TASK_WORKERS).sum()).isZero();
  }

  @Test
  void shouldNotDeleteTaskMetricsWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.deleteTaskMetrics(null);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.DELETE));
  }

  // query schema log list //////////////////////////////////////////

  @Test
  void shouldExecuteSchemaLogListAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    List<SchemaLogEntry> schemaLog = managementService.createSchemaLogQuery().list();

    // then
    assertThat(schemaLog).isNotEmpty();
  }

  @Test
  void shouldExecuteSchemaLogListWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    List<SchemaLogEntry> schemaLog = managementService.createSchemaLogQuery().list();

    // then
    assertThat(schemaLog).isNotEmpty();
  }

  @Test
  void shouldExecuteSchemaLogListWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    List<SchemaLogEntry> schemaLog = managementService.createSchemaLogQuery().list();

    // then
    assertThat(schemaLog).isNotEmpty();
  }

  @Test
  void shouldNotExecuteSchemaLogListWithoutAuthorization() {
    // given

    // when
    List<SchemaLogEntry> schemaLog = managementService.createSchemaLogQuery().list();

    // then
    assertThat(schemaLog).isEmpty();
  }

  // query schema log count //////////////////////////////////////////

  @Test
  void shouldExecuteSchemaLogCountAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    // when
    long schemaLog = managementService.createSchemaLogQuery().count();

    // then
    assertThat(schemaLog).isPositive();
  }

  @Test
  void shouldExecuteSchemaLogCountWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    long schemaLog = managementService.createSchemaLogQuery().count();

    // then
    assertThat(schemaLog).isPositive();
  }

  @Test
  void shouldExecuteSchemaLogCountWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    // when
    long schemaLog = managementService.createSchemaLogQuery().count();

    // then
    assertThat(schemaLog).isPositive();
  }

  @Test
  void shouldNotExecuteSchemaLogCountWithoutAuthorization() {
    // given

    // when
    long schemaLog = managementService.createSchemaLogQuery().count();

    // then
    assertThat(schemaLog).isZero();
  }

}
