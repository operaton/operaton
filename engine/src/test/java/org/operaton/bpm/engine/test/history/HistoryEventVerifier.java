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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.engine.impl.history.event.HistoryEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Edoardo Patti
 */
public class HistoryEventVerifier implements AfterEachCallback {

  private final TestEventHandler eventHandler;

  private final List<Condition<HistoryEvent>> hasConditions = new ArrayList<>();
  private final List<Condition<HistoryEvent>> isConditions  = new ArrayList<>();

  public HistoryEventVerifier(TestEventHandler eventHandler) {
    this.eventHandler = eventHandler;
  }

  public void historyEventHas(String message, Predicate<HistoryEvent> predicate) {
    hasConditions.add(getCondition(message, predicate));
  }

  public void historyEventIs(String message, Predicate<HistoryEvent> predicate) {
    isConditions.add(getCondition(message, predicate));
  }

  private Condition<HistoryEvent> getCondition(String message, Predicate<HistoryEvent> predicate) {
    return new Condition<>(message) {
      @Override
      public boolean matches(HistoryEvent value) {
        return predicate.test(value);
      }
    };
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    // Consume all events and verify each one
    while (this.eventHandler.peek() != null) {
      final HistoryEvent evt = this.eventHandler.poll();
      hasConditions.forEach(condition -> assertThat(evt).has(condition));
      isConditions.forEach(condition -> assertThat(evt).is(condition));
    }
  }
}