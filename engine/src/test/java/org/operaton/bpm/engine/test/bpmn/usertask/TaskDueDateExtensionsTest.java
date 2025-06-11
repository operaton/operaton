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
package org.operaton.bpm.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * @author Frederik Heremans
 */
@ExtendWith(ProcessEngineExtension.class)
class TaskDueDateExtensionsTest {

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testDueDateExtension() throws Exception {

    Date date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").parse("06-07-1986 12:10:00");
    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", date);

    // Start process-instance, passing date that should be used as dueDate
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dueDateExtension", variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(task.getDueDate()).isNotNull();
    assertThat(task.getDueDate()).isEqualTo(date);
  }

  @Deployment
  @Test
  void testDueDateStringExtension() throws Exception {

    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", "1986-07-06T12:10:00");

    // Start process-instance, passing date that should be used as dueDate
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dueDateExtension", variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    assertThat(task.getDueDate()).isNotNull();
    Date date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("06-07-1986 12:10:00");
    assertThat(task.getDueDate()).isEqualTo(date);
  }

  @Deployment
  @Test
  void testRelativeDueDate() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", "P2DT2H30M");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dueDateExtension", variables);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();


    Date dueDate = task.getDueDate();
    assertThat(dueDate).isNotNull();

    Period period = new Period(task.getCreateTime().getTime(), dueDate.getTime());
    assertThat(period.getDays()).isEqualTo(2);
    assertThat(period.getHours()).isEqualTo(2);
    assertThat(period.getMinutes()).isEqualTo(30);
  }
}
