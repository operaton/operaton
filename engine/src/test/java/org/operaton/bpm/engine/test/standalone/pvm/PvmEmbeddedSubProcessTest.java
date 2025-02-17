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
package org.operaton.bpm.engine.test.standalone.pvm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.impl.pvm.ProcessDefinitionBuilder;
import org.operaton.bpm.engine.impl.pvm.PvmExecution;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.test.standalone.pvm.activities.Automatic;
import org.operaton.bpm.engine.test.standalone.pvm.activities.EmbeddedSubProcess;
import org.operaton.bpm.engine.test.standalone.pvm.activities.End;
import org.operaton.bpm.engine.test.standalone.pvm.activities.ParallelGateway;
import org.operaton.bpm.engine.test.standalone.pvm.activities.WaitState;
import org.junit.Test;


/**
 * @author Tom Baeyens
 */
public class PvmEmbeddedSubProcessTest {

  /**
   *           +------------------------------+
   *           | embedded subprocess          |
   * +-----+   |  +-----------+   +---------+ |   +---+
   * |start|-->|  |startInside|-->|endInside| |-->|end|
   * +-----+   |  +-----------+   +---------+ |   +---+
   *           +------------------------------+
   */
  @Test
  public void testEmbeddedSubProcess() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("endInside")
        .endActivity()
        .createActivity("endInside")
          .behavior(new End())
        .endActivity()
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedActiveActivityIds = new ArrayList<>();
    expectedActiveActivityIds.add("end");

    assertThat(processInstance.findActiveActivityIds()).isEqualTo(expectedActiveActivityIds);
  }

  /**
   *           +----------------------------------------+
   *           | embeddedsubprocess        +----------+ |
   *           |                     +---->|endInside1| |
   *           |                     |     +----------+ |
   *           |                     |                  |
   * +-----+   |  +-----------+   +----+   +----------+ |   +---+
   * |start|-->|  |startInside|-->|fork|-->|endInside2| |-->|end|
   * +-----+   |  +-----------+   +----+   +----------+ |   +---+
   *           |                     |                  |
   *           |                     |     +----------+ |
   *           |                     +---->|endInside3| |
   *           |                           +----------+ |
   *           +----------------------------------------+
   */
  @Test
  public void testMultipleConcurrentEndsInsideEmbeddedSubProcess() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("fork")
        .endActivity()
        .createActivity("fork")
          .behavior(new ParallelGateway())
          .transition("endInside1")
          .transition("endInside2")
          .transition("endInside3")
        .endActivity()
        .createActivity("endInside1")
          .behavior(new End())
        .endActivity()
        .createActivity("endInside2")
          .behavior(new End())
        .endActivity()
        .createActivity("endInside3")
          .behavior(new End())
        .endActivity()
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.isEnded()).isTrue();
  }

  /**
   *           +-------------------------------------------------+
   *           | embeddedsubprocess        +----------+          |
   *           |                     +---->|endInside1|          |
   *           |                     |     +----------+          |
   *           |                     |                           |
   * +-----+   |  +-----------+   +----+   +----+   +----------+ |   +---+
   * |start|-->|  |startInside|-->|fork|-->|wait|-->|endInside2| |-->|end|
   * +-----+   |  +-----------+   +----+   +----+   +----------+ |   +---+
   *           |                     |                           |
   *           |                     |     +----------+          |
   *           |                     +---->|endInside3|          |
   *           |                           +----------+          |
   *           +-------------------------------------------------+
   */
  @Test
  public void testMultipleConcurrentEndsInsideEmbeddedSubProcessWithWaitState() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("fork")
        .endActivity()
        .createActivity("fork")
          .behavior(new ParallelGateway())
          .transition("endInside1")
          .transition("wait")
          .transition("endInside3")
        .endActivity()
        .createActivity("endInside1")
          .behavior(new End())
        .endActivity()
        .createActivity("wait")
          .behavior(new WaitState())
          .transition("endInside2")
        .endActivity()
        .createActivity("endInside2")
          .behavior(new End())
        .endActivity()
        .createActivity("endInside3")
          .behavior(new End())
        .endActivity()
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.isEnded()).isFalse();
    PvmExecution execution = processInstance.findExecution("wait");
    execution.signal(null, null);

    assertThat(processInstance.isEnded()).isTrue();
  }

  /**
   *           +-------------------------------------------------------+
   *           | embedded subprocess                                   |
   *           |                  +--------------------------------+   |
   *           |                  | nested embedded subprocess     |   |
   * +-----+   | +-----------+    |  +-----------+   +---------+   |   |   +---+
   * |start|-->| |startInside|--> |  |startInside|-->|endInside|   |   |-->|end|
   * +-----+   | +-----------+    |  +-----------+   +---------+   |   |   +---+
   *           |                  +--------------------------------+   |
   *           |                                                       |
   *           +-------------------------------------------------------+
   */
  @Test
  public void testNestedSubProcessNoEnd() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("nestedSubProcess")
        .endActivity()
          .createActivity("nestedSubProcess")
          .scope()
          .behavior(new EmbeddedSubProcess())
          .createActivity("startNestedInside")
            .behavior(new Automatic())
            .transition("endInside")
            .endActivity()
          .createActivity("endInside")
            .behavior(new End())
            .endActivity()
        .endActivity()
      .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedActiveActivityIds = new ArrayList<>();
    expectedActiveActivityIds.add("end");

    assertThat(processInstance.findActiveActivityIds()).isEqualTo(expectedActiveActivityIds);
  }

  /**
   *           +------------------------------+
   *           | embedded subprocess          |
   * +-----+   |  +-----------+               |
   * |start|-->|  |startInside|               |
   * +-----+   |  +-----------+               |
   *           +------------------------------+
   */
  @Test
  public void testEmbeddedSubProcessWithoutEndEvents() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
        .endActivity()
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.isEnded()).isTrue();
  }

  /**
   *           +-------------------------------------------------------+
   *           | embedded subprocess                                   |
   *           |                  +--------------------------------+   |
   *           |                  | nested embedded subprocess     |   |
   * +-----+   | +-----------+    |  +-----------+                 |   |
   * |start|-->| |startInside|--> |  |startInside|                 |   |
   * +-----+   | +-----------+    |  +-----------+                 |   |
   *           |                  +--------------------------------+   |
   *           |                                                       |
   *           +-------------------------------------------------------+
   */
  @Test
  public void testNestedSubProcessBothNoEnd() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("nestedSubProcess")
        .endActivity()
          .createActivity("nestedSubProcess")
          .scope()
          .behavior(new EmbeddedSubProcess())
          .createActivity("startNestedInside")
            .behavior(new Automatic())
            .endActivity()
        .endActivity()
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.isEnded()).isTrue();
  }


  /**
   *           +------------------------------+
   *           | embedded subprocess          |
   * +-----+   |  +-----------+   +---------+ |
   * |start|-->|  |startInside|-->|endInside| |
   * +-----+   |  +-----------+   +---------+ |
   *           +------------------------------+
   */
  @Test
  public void testEmbeddedSubProcessNoEnd() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("embeddedsubprocess")
      .endActivity()
      .createActivity("embeddedsubprocess")
        .scope()
        .behavior(new EmbeddedSubProcess())
        .createActivity("startInside")
          .behavior(new Automatic())
          .transition("endInside")
        .endActivity()
        .createActivity("endInside")
          .behavior(new End())
        .endActivity()
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.isEnded()).isTrue();
  }

}
