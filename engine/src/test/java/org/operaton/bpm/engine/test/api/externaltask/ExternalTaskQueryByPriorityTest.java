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
package org.operaton.bpm.engine.test.api.externaltask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.externalTaskByPriority;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySortingAndCount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Christopher Zell
 */
@ExtendWith(ProcessEngineExtension.class)
class ExternalTaskQueryByPriorityTest {

  protected RuntimeService runtimeService;
  protected ExternalTaskService externalTaskService;

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testOrderByPriority() {
    // given five jobs with priorities from 1 to 5
    //each process has two external tasks - one with priority expression and one without priority
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i));
    }

    // then querying and ordering by priority works
    verifySortingAndCount(externalTaskService.createExternalTaskQuery().orderByPriority().asc(), 10, externalTaskByPriority());
    verifySortingAndCount(externalTaskService.createExternalTaskQuery().orderByPriority().desc(), 10, inverted(externalTaskByPriority()));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testFilterByExternalTaskPriorityLowerThanOrEquals() {
    // given five jobs with priorities from 1 to 5
    //each process has two external tasks - one with priority expression and one without priority
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i));
    }

    // when making an external task query and filtering by priority
    // then the correct external tasks are returned
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().priorityLowerThanOrEquals(2).list();
    assertThat(tasks).hasSize(8);

    for (ExternalTask task : tasks) {
      assertThat(task.getPriority()).isLessThanOrEqualTo(2);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testFilterByExternalTaskPriorityLowerThanOrEqualsAndHigherThanOrEqual() {
    // given five jobs with priorities from 1 to 5
    for (int i = 0; i < 5; i++) {
      runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i));
    }

    // when making an external task query and filtering by disjunctive external task priority
    // then no external task are returned
    assertThat(externalTaskService.createExternalTaskQuery().priorityLowerThanOrEquals(2).priorityHigherThanOrEquals(3).count()).isZero();

    // when making an external task query and filtering by external task priority >= 2 and <= 3
    // then two external task are returned
    assertThat(externalTaskService.createExternalTaskQuery().priorityHigherThanOrEquals(2).priorityLowerThanOrEquals(3).count()).isEqualTo(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testFilterByExternalTaskPriorityHigherThanOrEquals() {
    // given five jobs with priorities from 1 to 5
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making an external task query and filtering by external task priority
    // then the correct external task are returned
    List<ExternalTask> tasks = externalTaskService.createExternalTaskQuery().priorityHigherThanOrEquals(2L).list();
    assertThat(tasks).hasSize(3);

    Set<String> processInstanceIds = new HashSet<>();
    processInstanceIds.add(instances.get(2).getId());
    processInstanceIds.add(instances.get(3).getId());
    processInstanceIds.add(instances.get(4).getId());

    for (ExternalTask task : tasks) {
      assertThat(task.getPriority()).isGreaterThanOrEqualTo(2);
      assertThat(processInstanceIds).contains(task.getProcessInstanceId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/externalTaskPriorityExpression.bpmn20.xml")
  @Test
  void testFilterByExternalTaskPriorityLowerAndHigher() {
    // given five jobs with priorities from 1 to 5
    List<ProcessInstance> instances = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      instances.add(runtimeService.startProcessInstanceByKey("twoExternalTaskWithPriorityProcess",
          Variables.createVariables().putValue("priority", i)));
    }

    // when making an external task query and filtering by external task priority
    // then the correct external task is returned
    ExternalTask task = externalTaskService.createExternalTaskQuery()
                                           .priorityHigherThanOrEquals(2L)
                                           .priorityLowerThanOrEquals(2L)
                                           .singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getPriority()).isEqualTo(2);
    assertThat(task.getProcessInstanceId()).isEqualTo(instances.get(2).getId());
  }
}
