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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test timer expression according to act-865
 *
 * @author Saeid Mirzaei
 */
@ExtendWith(ProcessEngineExtension.class)
class TimeExpressionTest {

  RuntimeService runtimeService;
  ManagementService managementService;

  private Date testExpression(String timeExpression) {
    // Set the clock fixed
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("dueDate", timeExpression);

    // After process start, there should be timer created
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isOne();

    List<Job> jobs = managementService.createJobQuery().executable().list();
    assertThat(jobs).hasSize(1);
    return jobs.get(0).getDuedate();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionComplete() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt));
    assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dueDate))
        .isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt));
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionWithoutSeconds() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt));
    assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dueDate))
        .isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt));
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionWithoutMinutes() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(new Date()));
    assertThat(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dueDate))
        .isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dt));
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionWithoutTime() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    assertThat(new SimpleDateFormat("yyyy-MM-dd").format(dueDate))
        .isEqualTo(new SimpleDateFormat("yyyy-MM-dd").format(dt));
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionWithoutDay() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM").format(new Date()));
    assertThat(new SimpleDateFormat("yyyy-MM").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy-MM").format(dt));
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpression.bpmn20.xml"})
  @Test
  void testTimeExpressionWithoutMonth() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy").format(new Date()));
    assertThat(new SimpleDateFormat("yyyy").format(dueDate)).isEqualTo(new SimpleDateFormat("yyyy").format(dt));
  }
}
