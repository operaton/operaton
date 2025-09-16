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
package org.operaton.bpm.integrationtest.jobexecutor;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.integrationtest.jobexecutor.beans.TimerExpressionBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Tobias Metzke
 */
@ExtendWith(ArquillianExtension.class)
public class TimerRecalculationTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/TimerRecalculation.bpmn20.xml")
            .addClass(TimerExpressionBean.class);
  }

  @Test
  void testTimerRecalculationBasedOnProcessVariable() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("timerExpression", "PT10S");
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("TimerRecalculationProcess", variables);
    String processInstanceId = instance.getId();

    ProcessInstanceQuery instancesQuery = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId);
    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstanceId);
    assertThat(instancesQuery.count()).isEqualTo(1);
    assertThat(jobQuery.count()).isEqualTo(1);

    Job job = jobQuery.singleResult();
    Date oldDueDate = job.getDuedate();

    // when
    runtimeService.setVariable(processInstanceId,  "timerExpression", "PT1S");
    managementService.recalculateJobDuedate(job.getId(), true);

    // then
    assertThat(jobQuery.count()).isEqualTo(1);
    Job jobRecalculated = jobQuery.singleResult();
    assertThat(jobRecalculated.getDuedate()).isNotEqualTo(oldDueDate);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(jobRecalculated.getCreateTime());
    calendar.add(Calendar.SECOND, 1);
    Date expectedDate = calendar.getTime();
    assertThat(jobRecalculated.getDuedate()).isEqualTo(expectedDate);

    waitForJobExecutorToProcessAllJobs();

    assertThat(instancesQuery.count()).isZero();
  }
}
