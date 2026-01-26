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
package org.operaton.bpm.spring.boot.starter.configuration.condition;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NeedsHistoryAutoConfigurationConditionTest {

  @Test
  void isHistoryAutoSupportedTest() {
    NeedsHistoryAutoConfigurationCondition condition = new NeedsHistoryAutoConfigurationCondition();
    condition.historyAutoFieldName = "HISTORY_AUTO";
    assertThat(condition.isHistoryAutoSupported()).isTrue();
    condition.historyAutoFieldName = "DB_SCHEMA_UPDATE_FALSE";
    assertThat(condition.isHistoryAutoSupported()).isFalse();
  }

  @Test
  void needsNoAdditionalConfigurationTest1() {
    NeedsHistoryAutoConfigurationCondition condition = spy(new NeedsHistoryAutoConfigurationCondition());
    ConditionContext context = mock(ConditionContext.class);
    Environment environment = mock(Environment.class);
    when(context.getEnvironment()).thenReturn(environment);
    assertThat(condition.needsAdditionalConfiguration(context)).isFalse();
  }

  @Test
  void needsNoAdditionalConfigurationTest2() {
    NeedsHistoryAutoConfigurationCondition condition = spy(new NeedsHistoryAutoConfigurationCondition());
    ConditionContext context = mock(ConditionContext.class);
    Environment environment = mock(Environment.class);
    when(context.getEnvironment()).thenReturn(environment);
    when(environment.getProperty("operaton.bpm.history-level")).thenReturn(NeedsHistoryAutoConfigurationCondition.HISTORY_AUTO);
    assertThat(condition.needsAdditionalConfiguration(context)).isFalse();
  }

  @Test
  void needsAdditionalConfigurationTest() {
    NeedsHistoryAutoConfigurationCondition condition = spy(new NeedsHistoryAutoConfigurationCondition());
    ConditionContext context = mock(ConditionContext.class);
    Environment environment = mock(Environment.class);
    when(context.getEnvironment()).thenReturn(environment);
    when(environment.getProperty("operaton.bpm.history-level")).thenReturn(NeedsHistoryAutoConfigurationCondition.HISTORY_AUTO);
    when(condition.isHistoryAutoSupported()).thenReturn(false);
    assertThat(condition.needsAdditionalConfiguration(context)).isTrue();
  }

  @Test
  void getMatchOutcomeMatchTest() {
    NeedsHistoryAutoConfigurationCondition condition = spy(new NeedsHistoryAutoConfigurationCondition());
    ConditionContext context = mock(ConditionContext.class);
    Environment environment = mock(Environment.class);
    when(context.getEnvironment()).thenReturn(environment);
    when(environment.getProperty("operaton.bpm.history-level")).thenReturn(NeedsHistoryAutoConfigurationCondition.HISTORY_AUTO);
    when(condition.needsAdditionalConfiguration(context)).thenReturn(true);
    assertThat(condition.getMatchOutcome(context, null).isMatch()).isTrue();
  }

  @Test
  void getMatchOutcomeNoMatchTest() {
    NeedsHistoryAutoConfigurationCondition condition = spy(new NeedsHistoryAutoConfigurationCondition());
    ConditionContext context = mock(ConditionContext.class);
    Environment environment = mock(Environment.class);
    when(context.getEnvironment()).thenReturn(environment);
    when(environment.getProperty("operaton.bpm.history-level")).thenReturn(NeedsHistoryAutoConfigurationCondition.HISTORY_AUTO);
    when(condition.needsAdditionalConfiguration(context)).thenReturn(false);
    assertThat(condition.getMatchOutcome(context, null).isMatch()).isFalse();
  }

}
