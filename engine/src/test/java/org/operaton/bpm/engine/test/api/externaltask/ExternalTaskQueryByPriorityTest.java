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
package org.operaton.bpm.engine.test.api.externaltask;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByPriority;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySortingAndCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

/**
 * @author Christopher Zell
 */
public class ExternalTaskQueryByPriorityTest extends PluggableProcessEngineTest {

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  public void testOrderByPriority() {
    // given five jobs with priorities from 1 to 5
    //each process has two external tasks - one with priority expression and one without priority
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // then querying and ordering by priority works
    verifySortingAndCount(externalTaskService.createExternalTaskQuery().orderByPriority().asc(), 10, externalTaskByPriority());
    verifySortingAndCount(externalTaskService.createExternalTaskQuery().orderByPriority().desc(), 10, inverted(externalTaskByPriority()));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  public void testFilterByExternalTaskPriorityLowerThanOrEquals() {
    // given five jobs with priorities from 1 to 5
    //each process has two external tasks - one with priority expression and one without priority
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making a external task query and filtering by priority
    // then the correct external tasks are returned
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().priorityLowerThanOrEquals(2).list();
    assertThat(tasks.size()).isEqualTo(8);

    for (ExternalTask task : tasks) {
      assertTrue(task.getPriority() <= 2);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  public void testFilterByExternalTaskPriorityLowerThanOrEqualsAndHigherThanOrEqual() {
    // given five jobs with priorities from 1 to 5
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making a external task query and filtering by disjunctive external task priority
    // then no external task are returned
    assertThat(externalTaskService.createExternalTaskQuery().priorityLowerThanOrEquals(2).priorityHigherThanOrEquals(3).count()).isEqualTo(0);

    // when making a external task query and filtering by external task priority >= 2 and <= 3
    // then two external task are returned
    assertThat(externalTaskService.createExternalTaskQuery().priorityHigherThanOrEquals(2).priorityLowerThanOrEquals(3).count()).isEqualTo(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  public void testFilterByExternalTaskPriorityHigherThanOrEquals() {
    // given five jobs with priorities from 1 to 5
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making a external task query and filtering by external task priority
    // then the correct external task are returned
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().priorityHigherThanOrEquals(2L).list();
    assertThat(tasks.size()).isEqualTo(3);

    Set<String> processInstanceIds = new HashSet<>();
    processInstanceIds.add(instances.get(2).getId());
    processInstanceIds.add(instances.get(3).getId());
    processInstanceIds.add(instances.get(4).getId());

    for (ExternalTask task : tasks) {
      assertTrue(task.getPriority() >= 2);
      assertTrue(processInstanceIds.contains(task.getProcessInstanceId()));
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  public void testFilterByExternalTaskPriorityLowerAndHigher() {
    // given five jobs with priorities from 1 to 5
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making a external task query and filtering by external task priority
    // then the correct external task is returned
    ExternalTask task = externalTaskService.createExternalTaskQuery()
                                           .priorityHigherThanOrEquals(2L)
                                           .priorityLowerThanOrEquals(2L)
                                           .singleResult();
    assertNotNull(task);
    assertThat(task.getPriority()).isEqualTo(2);
    assertThat(task.getProcessInstanceId()).isEqualTo(instances.get(2).getId());
  }
}
