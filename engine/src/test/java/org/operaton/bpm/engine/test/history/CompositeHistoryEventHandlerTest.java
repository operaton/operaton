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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.history.handler.CompositeHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.DbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Alexander Tyatenkov
 *
 */
class CompositeHistoryEventHandlerTest extends AbstractCompositeHistoryEventHandlerTest {

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerNonArgumentConstructor() {
    processEngineConfiguration.setHistoryEventHandler(new CompositeHistoryEventHandler());

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseDefaultHistoryEventHandler() {
    // use default DbHistoryEventHandler
    processEngineConfiguration.setHistoryEventHandler(new DbHistoryEventHandler());

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  void shouldUseCompositeHistoryEventHandlerNonArgumentConstructorAddNullEvent() {
    // given
    var compositeHistoryEventHandler = new CompositeHistoryEventHandler();

    // when/then
    assertThatThrownBy(() -> compositeHistoryEventHandler.add(null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("History event handler is null");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerNonArgumentConstructorAddNotNullEvent() {
    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler();
    compositeHistoryEventHandler.add(new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerNonArgumentConstructorAddNotNullTwoEvents() {
    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler();
    compositeHistoryEventHandler.add(new CustomDbHistoryEventHandler());
    compositeHistoryEventHandler.add(new DbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
  }

  @Test
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNullVarargs() {
    // given
    HistoryEventHandler historyEventHandler = null;

    // when/then
    assertThatThrownBy(() -> new CompositeHistoryEventHandler(historyEventHandler))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("History event handler is null");
  }

  @Test
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNullTwoVarargs() {
    // when/then
    assertThatThrownBy(() -> new CompositeHistoryEventHandler(null, null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("History event handler is null");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNotNullVarargsOneEvent() {
    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler(new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNotNullVarargsTwoEvents() {
    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler(new CustomDbHistoryEventHandler(), new DbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithEmptyList() {
    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler(new ArrayList<HistoryEventHandler>());
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isZero();
  }

  @Test
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNotEmptyListNullTwoEvents() {
    // given - prepare the list with two null events
    var historyEventHandlers = new ArrayList<HistoryEventHandler>();
    historyEventHandlers.add(null);
    historyEventHandlers.add(null);

    // when/then
    assertThatThrownBy(() -> new CompositeHistoryEventHandler(historyEventHandlers))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("History event handler is null");
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeHistoryEventHandlerArgumentConstructorWithNotEmptyListNotNullTwoEvents() {
    // prepare the list with two events
    List<HistoryEventHandler> historyEventHandlers = new ArrayList<>();
    historyEventHandlers.add(new CustomDbHistoryEventHandler());
    historyEventHandlers.add(new DbHistoryEventHandler());

    CompositeHistoryEventHandler compositeHistoryEventHandler = new CompositeHistoryEventHandler(historyEventHandlers);
    processEngineConfiguration.setHistoryEventHandler(compositeHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2);
  }

}
