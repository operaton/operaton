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
package org.operaton.bpm.spring.boot.starter.jdbc;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.HistoryLevelAudit;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryLevelDeterminatorJdbcTemplateImplTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  private OperatonBpmProperties operatonBpmProperties;

  @BeforeEach
  void before() {
    operatonBpmProperties = new OperatonBpmProperties();
  }

  @Test
  void afterPropertiesSetTest1() throws Exception {
    operatonBpmProperties = new OperatonBpmProperties();
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setJdbcTemplate(jdbcTemplate);
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    determinator.afterPropertiesSet();
    assertThat(determinator.defaultHistoryLevel).isEqualTo(ProcessEngineConfiguration.HISTORY_FULL);
  }

  @Test
  void afterPropertiesSetTest2() throws Exception {
    operatonBpmProperties = new OperatonBpmProperties();
    final String historyLevelDefault = "defaultValue";
    operatonBpmProperties.setHistoryLevelDefault(historyLevelDefault);
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setJdbcTemplate(jdbcTemplate);
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    determinator.afterPropertiesSet();
    assertThat(determinator.defaultHistoryLevel).isEqualTo(historyLevelDefault);
  }

  @Test
  void afterPropertiesSetTest3() {
    var historyLevelDeterminatorJdbcTemplate = new HistoryLevelDeterminatorJdbcTemplateImpl();

    assertThatIllegalArgumentException().isThrownBy(historyLevelDeterminatorJdbcTemplate::afterPropertiesSet);
  }

  @Test
  void afterPropertiesSetTest4() {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setJdbcTemplate(jdbcTemplate);

    assertThatIllegalArgumentException().isThrownBy(determinator::afterPropertiesSet);
  }

  @Test
  void afterPropertiesSetTest5() {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setOperatonBpmProperties(operatonBpmProperties);

    assertThatIllegalArgumentException().isThrownBy(determinator::afterPropertiesSet);
  }

  @Test
  void determinedTest() throws Exception {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    final String defaultHistoryLevel = "test";
    determinator.setDefaultHistoryLevel(defaultHistoryLevel);
    determinator.setJdbcTemplate(jdbcTemplate);
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    determinator.afterPropertiesSet();
    HistoryLevel historyLevel = new HistoryLevelAudit();
    when(jdbcTemplate.queryForObject(determinator.getSql(), Integer.class)).thenReturn(historyLevel.getId());
    String determineHistoryLevel = determinator.determineHistoryLevel();
    assertThat(determineHistoryLevel).isEqualTo(historyLevel.getName());
  }

  @Test
  void determinedExceptionIgnoringTest() throws Exception {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    final String defaultHistoryLevel = "test";
    determinator.setDefaultHistoryLevel(defaultHistoryLevel);
    determinator.setJdbcTemplate(jdbcTemplate);
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    determinator.afterPropertiesSet();
    when(jdbcTemplate.queryForObject(determinator.getSql(), Integer.class)).thenThrow(new DataRetrievalFailureException(""));
    String determineHistoryLevel = determinator.determineHistoryLevel();
    assertThat(determineHistoryLevel).isEqualTo(determinator.defaultHistoryLevel);
    verify(jdbcTemplate).queryForObject(determinator.getSql(), Integer.class);
  }

  @Test
  void determinedExceptionNotIgnoringTest() throws Exception {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setIgnoreDataAccessException(false);
    final String defaultHistoryLevel = "test";
    determinator.setDefaultHistoryLevel(defaultHistoryLevel);
    determinator.setJdbcTemplate(jdbcTemplate);
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    determinator.afterPropertiesSet();
    when(jdbcTemplate.queryForObject(determinator.getSql(), Integer.class)).thenThrow(new DataRetrievalFailureException(""));

    assertThatThrownBy(determinator::determineHistoryLevel)
      .isInstanceOf(DataRetrievalFailureException.class);
  }

  @Test
  void getSqlTest() {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    determinator.setOperatonBpmProperties(operatonBpmProperties);
    assertThat(determinator.getSql()).isEqualTo("SELECT VALUE_ FROM ACT_GE_PROPERTY WHERE NAME_='historyLevel'");
    operatonBpmProperties.getDatabase().setTablePrefix("TEST_");
    assertThat(determinator.getSql()).isEqualTo("SELECT VALUE_ FROM TEST_ACT_GE_PROPERTY WHERE NAME_='historyLevel'");
  }

  @Test
  void getHistoryLevelFromTest() {
    HistoryLevelDeterminatorJdbcTemplateImpl determinator = new HistoryLevelDeterminatorJdbcTemplateImpl();
    assertThat(determinator.getHistoryLevelFrom(-1)).isEqualTo(determinator.getDefaultHistoryLevel());
    assertThat(determinator.historyLevels).isNotEmpty();
    HistoryLevel customHistoryLevel = new HistoryLevel() {

      @Override
      public boolean isHistoryEventProduced(HistoryEventType eventType, Object entity) {
        return false;
      }

      @Override
      public String getName() {
        return "custom";
      }

      @Override
      public int getId() {
        return Integer.MAX_VALUE;
      }
    };

    determinator.addCustomHistoryLevels(Collections.singleton(customHistoryLevel));
    assertThat(determinator.historyLevels).contains(customHistoryLevel);

    for (HistoryLevel historyLevel : determinator.historyLevels) {
      assertThat(determinator.getHistoryLevelFrom(historyLevel.getId())).isEqualTo(historyLevel.getName());
    }
  }

}
