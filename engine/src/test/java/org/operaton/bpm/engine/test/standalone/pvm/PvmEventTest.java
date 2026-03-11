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
package org.operaton.bpm.engine.test.standalone.pvm;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.pvm.ProcessDefinitionBuilder;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.test.standalone.pvm.activities.Automatic;
import org.operaton.bpm.engine.test.standalone.pvm.activities.EmbeddedSubProcess;
import org.operaton.bpm.engine.test.standalone.pvm.activities.End;
import org.operaton.bpm.engine.test.standalone.pvm.activities.ParallelGateway;
import org.operaton.bpm.engine.test.standalone.pvm.activities.WaitState;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Tom Baeyens
 */
class PvmEventTest {

  /**
   * +-------+   +-----+
   * | start |-->| end |
   * +-------+   +-----+
   */
  @Test
  void testStartEndEvents() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("events")
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .startTransition("end")
          .executionListener(eventCollector)
        .endTransition()
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<>();
    expectedEvents.add("start on ProcessDefinition(events)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("take on (start)-->(end)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(events)");

    assertThat(eventCollector.events).as("expected %s, but was %n%s%n".formatted(expectedEvents, eventCollector)).isEqualTo(expectedEvents);
  }

  /**
   *           +------------------------------+
   * +-----+   | +-----------+   +----------+ |   +---+
   * |start|-->| |startInside|-->|endInsdide| |-->|end|
   * +-----+   | +-----------+   +----------+ |   +---+
   *           +------------------------------+
   */
  @Test
  void testEmbeddedSubProcessEvents() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("events")
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("startInside")
          .behavior(new Automatic())
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
          .transition("endInside")
        .endActivity()
        .createActivity("endInside")
          .behavior(new End())
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .endActivity()
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<>();
    expectedEvents.add("start on ProcessDefinition(events)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(embeddedsubprocess)");
    expectedEvents.add("start on Activity(startInside)");
    expectedEvents.add("end on Activity(startInside)");
    expectedEvents.add("start on Activity(endInside)");
    expectedEvents.add("end on Activity(endInside)");
    expectedEvents.add("end on Activity(embeddedsubprocess)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(events)");

    assertThat(eventCollector.events).as("expected %s, but was %n%s%n".formatted(expectedEvents, eventCollector)).isEqualTo(expectedEvents);
  }


  /**
   *                   +--+
   *              +--->|c1|---+
   *              |    +--+   |
   *              |           v
   * +-----+   +----+       +----+   +---+
   * |start|-->|fork|       |join|-->|end|
   * +-----+   +----+       +----+   +---+
   *              |           ^
   *              |    +--+   |
   *              +--->|c2|---+
   *                   +--+
   */
  @Test
  void testSimpleAutmaticConcurrencyEvents() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("events")
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("fork")
      .endActivity()
      .createActivity("fork")
        .behavior(new ParallelGateway())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("c1")
        .transition("c2")
      .endActivity()
      .createActivity("c1")
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("join")
      .endActivity()
      .createActivity("c2")
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("join")
      .endActivity()
      .createActivity("join")
        .behavior(new ParallelGateway())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<>();
    expectedEvents.add("start on ProcessDefinition(events)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(fork)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(c2)");
    expectedEvents.add("end on Activity(c2)");
    expectedEvents.add("start on Activity(join)");
    expectedEvents.add("start on Activity(c1)");
    expectedEvents.add("end on Activity(c1)");
    expectedEvents.add("start on Activity(join)");
    expectedEvents.add("end on Activity(join)");
    expectedEvents.add("end on Activity(join)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(events)");

    assertThat(eventCollector.events).as("expected %s, but was %n%s%n".formatted(expectedEvents, eventCollector)).isEqualTo(expectedEvents);
  }

  /**
   *           +-----------------------------------------------+
   * +-----+   | +-----------+   +------------+   +----------+ |   +---+
   * |start|-->| |startInside|-->| taskInside |-->|endInsdide| |-->|end|
   * +-----+   | +-----------+   +------------+   +----------+ |   +---+
   *           +-----------------------------------------------+
   */
  @Test
  void testEmbeddedSubProcessEventsDelete() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("events")
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("startInside")
          .behavior(new Automatic())
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
          .transition("taskInside")
        .endActivity()
        .createActivity("taskInside")
          .behavior(new WaitState())
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
          .transition("endInside")
        .endActivity()
        .createActivity("endInside")
          .behavior(new End())
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
        .endActivity()
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    processInstance.deleteCascade("");

    List<String> expectedEvents = new ArrayList<>();
    expectedEvents.add("start on ProcessDefinition(events)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(embeddedsubprocess)");
    expectedEvents.add("start on Activity(startInside)");
    expectedEvents.add("end on Activity(startInside)");
    expectedEvents.add("start on Activity(taskInside)");
    expectedEvents.add("end on Activity(taskInside)");
    expectedEvents.add("end on Activity(embeddedsubprocess)");
    expectedEvents.add("end on ProcessDefinition(events)");

    assertThat(eventCollector.events).as("expected %s, but was %n%s%n".formatted(expectedEvents, eventCollector)).isEqualTo(expectedEvents);
  }
}
