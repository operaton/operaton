/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.test.Deployment;

/**
 * @author Alexander Tyatenkov
 *
 */
class CompositeDbHistoryEventHandlerTest extends AbstractCompositeHistoryEventHandlerTest {

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerNonArgumentConstructor() {
    processEngineConfiguration.setHistoryEventHandler(new CompositeDbHistoryEventHandler());

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  void shouldUseCompositeDbHistoryEventHandlerNonArgumentConstructorAddNullEvent() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler();
    try {
      compositeDbHistoryEventHandler.add(null);
      fail("NullValueException expected");
    } catch (NullValueException e) {
      assertThat(e.getMessage()).containsIgnoringCase("History event handler is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerNonArgumentConstructorAddNotNullEvent() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler();
    compositeDbHistoryEventHandler.add(new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerNonArgumentConstructorAddTwoNotNullEvents() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler();
    compositeDbHistoryEventHandler.add(new CustomDbHistoryEventHandler());
    compositeDbHistoryEventHandler.add(new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(4);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNullVarargs() {
    HistoryEventHandler historyEventHandler = null;
    try {
      new CompositeDbHistoryEventHandler(historyEventHandler);
      fail("NullValueException expected");
    } catch (NullValueException e) {
      assertThat(e.getMessage()).containsIgnoringCase("History event handler is null");
    }
  }

  @Test
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNullTwoVarargs() {
    try {
      new CompositeDbHistoryEventHandler(null, null);
      fail("NullValueException expected");
    } catch (NullValueException e) {
      assertThat(e.getMessage()).containsIgnoringCase("History event handler is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNotNullVarargsOneEvent() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler(new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(2);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNotNullVarargsTwoEvents() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler(new CustomDbHistoryEventHandler(), new CustomDbHistoryEventHandler());
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(4);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithEmptyList() {
    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler(new ArrayList<HistoryEventHandler>());
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isZero();
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

  @Test
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNotEmptyListNullTwoEvents() {
    // prepare the list with two null events
    List<HistoryEventHandler> historyEventHandlers = new ArrayList<>();
    historyEventHandlers.add(null);
    historyEventHandlers.add(null);

    try {
      new CompositeDbHistoryEventHandler(historyEventHandlers);
      fail("NullValueException expected");
    } catch (NullValueException e) {
      assertThat(e.getMessage()).containsIgnoringCase("History event handler is null");
    }
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoryLevelTest.bpmn20.xml"})
  void shouldUseCompositeDbHistoryEventHandlerArgumentConstructorWithNotEmptyListNotNullTwoEvents() {
    // prepare the list with two events
    List<HistoryEventHandler> historyEventHandlers = new ArrayList<>();
    historyEventHandlers.add(new CustomDbHistoryEventHandler());
    historyEventHandlers.add(new CustomDbHistoryEventHandler());

    CompositeDbHistoryEventHandler compositeDbHistoryEventHandler = new CompositeDbHistoryEventHandler(historyEventHandlers);
    processEngineConfiguration.setHistoryEventHandler(compositeDbHistoryEventHandler);

    startProcessAndCompleteUserTask();

    assertThat(countCustomHistoryEventHandler).isEqualTo(4);
    assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(2L);
  }

}
