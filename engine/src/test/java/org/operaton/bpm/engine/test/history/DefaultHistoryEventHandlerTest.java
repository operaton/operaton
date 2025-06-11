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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.CompositeHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@Parameterized
public class DefaultHistoryEventHandlerTest {

  @Parameters
  public static Iterable<Object> parameters() {
    return Arrays.asList(new Object[]{
        true, false
    });
  }

  @Parameter
  public boolean isDefaultHandlerEnabled;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests().build();
  
  @BeforeEach
  void setup() {
    engineRule.getProcessEngineConfiguration().setCustomHistoryEventHandlers(Collections.singletonList(new CustomHistoryEventHandler()));
    engineRule.getProcessEngineConfiguration().setEnableDefaultDbHistoryEventHandler(isDefaultHandlerEnabled);
  }

  @TestTemplate
  void shouldUseInstanceOfCompositeHistoryEventHandler() {
    // when
    boolean useDefaultDbHandler = engineRule.getProcessEngineConfiguration()
        .isEnableDefaultDbHistoryEventHandler();
    HistoryEventHandler defaultHandler = engineRule.getProcessEngineConfiguration()
        .getHistoryEventHandler();

    // then
    if (useDefaultDbHandler) {
      assertThat(defaultHandler).isInstanceOf(CompositeDbHistoryEventHandler.class);
    } else {
      assertThat(defaultHandler).isInstanceOf(CompositeHistoryEventHandler.class);
    }
  }

  @TestTemplate
  void shouldProvideCustomHistoryEventHandlers() {
    // when
    List<HistoryEventHandler> eventHandlers = engineRule.getProcessEngineConfiguration().getCustomHistoryEventHandlers();

    // then
    assertThat(eventHandlers).hasSize(1);
    assertThat(eventHandlers.get(0)).isInstanceOf(CustomHistoryEventHandler.class);
  }

  public class CustomHistoryEventHandler implements HistoryEventHandler {

    @Override
    public void handleEvent(HistoryEvent historyEvent) {
    }

    @Override
    public void handleEvents(List<HistoryEvent> historyEvents) {
    }
  }
}
