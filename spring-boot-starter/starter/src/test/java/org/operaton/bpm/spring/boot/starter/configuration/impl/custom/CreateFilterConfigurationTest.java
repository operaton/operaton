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
package org.operaton.bpm.spring.boot.starter.configuration.impl.custom;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.filter.FilterQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.test.helper.StandaloneInMemoryTestConfiguration;
import org.operaton.bpm.spring.boot.starter.util.SpringBootProcessEngineLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateFilterConfigurationTest {

  private final OperatonBpmProperties operatonBpmProperties = new OperatonBpmProperties();
  {
    operatonBpmProperties.getFilter().setCreate("All");
  }

  private static CreateFilterConfiguration configuration ;
  {
    configuration = new CreateFilterConfiguration(operatonBpmProperties);
    configuration.init();
  }

  @RegisterExtension
  ProcessEngineLoggingExtension loggingExtension = new ProcessEngineLoggingExtension()
      .watch(SpringBootProcessEngineLogger.PACKAGE);

  @Test
  void createAdminUser() {
    ProcessEngineExtension processEngineExtension = new StandaloneInMemoryTestConfiguration(configuration).extension();
    assertThat(processEngineExtension.getFilterService().createFilterQuery().filterName("All").singleResult()).isNotNull();
    processEngineExtension.getProcessEngine().close();
  }

  @Test
  void fail_if_not_configured_onInit() {
    OperatonBpmProperties bpmProperties = new OperatonBpmProperties();
    final CreateFilterConfiguration filterConfiguration = new CreateFilterConfiguration(bpmProperties);

    assertThatIllegalStateException().isThrownBy(filterConfiguration::init);
  }

  @Test
  void fail_if_not_configured_onExecution() {
    OperatonBpmProperties bpmProperties = new OperatonBpmProperties();
    bpmProperties.getFilter().setCreate("All");
    final CreateFilterConfiguration filterConfiguration = new CreateFilterConfiguration(bpmProperties);
    filterConfiguration.init();
    filterConfiguration.filterName = null;

    ProcessEngine processEngine = mock(ProcessEngine.class);
    assertThatNullPointerException().isThrownBy(() -> filterConfiguration.postProcessEngineBuild(processEngine));
  }

  @Test
  void do_not_create_when_already_exist() {
    OperatonBpmProperties bpmProperties = new OperatonBpmProperties();
    bpmProperties.getFilter().setCreate("All");
    final CreateFilterConfiguration filterConfiguration = new CreateFilterConfiguration(bpmProperties);
    filterConfiguration.init();

    ProcessEngine engine = mock(ProcessEngine.class);
    FilterService filterService = mock(FilterService.class);
    FilterQuery filterQuery = mock(FilterQuery.class);

    when(engine.getFilterService()).thenReturn(filterService);
    when(filterService.createFilterQuery()).thenReturn(filterQuery);
    when(filterQuery.filterName(anyString())).thenReturn(filterQuery);
    when(filterQuery.count()).thenReturn(1L);

    filterConfiguration.postProcessEngineBuild(engine);

    verifyLogs(Level.INFO, "the filter with this name already exists");
    verify(filterService).createFilterQuery();
    verify(filterQuery).filterName("All");
    verify(filterService, never()).newTaskFilter("All");
  }

  protected void verifyLogs(Level logLevel, String message) {
    List<ILoggingEvent> logs = loggingExtension.getLog();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getLevel()).isEqualTo(logLevel);
    assertThat(logs.get(0).getMessage()).containsIgnoringCase(message);
  }
}
