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
package org.operaton.bpm.spring.boot.starter;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.spring.boot.starter.event.PublishHistoryEventHandler;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomHistoryEventHandlersIT extends AbstractOperatonAutoConfigurationIT {

  @Test
  void shouldUsePublishHistoryEventHandler() {
    // given
    ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) processEngine
      .getProcessEngineConfiguration();

    // when
    List<HistoryEventHandler> customHandlersList = configuration.getCustomHistoryEventHandlers();
    HistoryEventHandler handler = configuration.getHistoryEventHandler();

    // then
    // assert that the default DbHistoryHandler is included
    assertThat(handler).isInstanceOf(CompositeDbHistoryEventHandler.class);
    // assert that two extra custom handlers have been added
    assertThat(customHandlersList).hasSize(2);
    assertThat(customHandlersList).extracting("class")
        .containsExactlyInAnyOrder(
          TestApplication.CustomHistoryEventHandler.class,
          PublishHistoryEventHandler.class);
  }
}
