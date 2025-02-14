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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.fail;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author roman.smirnov
 */
public class JobDefinitionQueryTest extends PluggableProcessEngineTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByNoCriteria() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    verifyQueryResults(query, 4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByJobDefinitionId() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerStartEventJobHandler.TYPE).singleResult();

    JobDefinitionQuery query = managementService.createJobDefinitionQuery().jobDefinitionId(jobDefinition.getId());

    verifyQueryResults(query, 1);

    assertThat(query.singleResult().getId()).isEqualTo(jobDefinition.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidJobDefinitionId() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().jobDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.jobDefinitionId(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByActivityId() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().activityIdIn("ServiceTask_1");
    verifyQueryResults(query, 1);

    query = managementService.createJobDefinitionQuery().activityIdIn("ServiceTask_1", "BoundaryEvent_1");
    verifyQueryResults(query, 2);

    query = managementService.createJobDefinitionQuery().activityIdIn("ServiceTask_1", "BoundaryEvent_1", "StartEvent_1");
    verifyQueryResults(query, 3);

    query = managementService.createJobDefinitionQuery().activityIdIn("ServiceTask_1", "BoundaryEvent_1", "StartEvent_1", "IntermediateCatchEvent_1");
    verifyQueryResults(query, 4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidActivityId() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().activityIdIn("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.activityIdIn(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Activity ids is null");
    }

    try {
      jobDefinitionQuery.activityIdIn((String)null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).isEqualTo("Activity ids contains null value");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByProcessDefinitionId() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    JobDefinitionQuery query = managementService.createJobDefinitionQuery().processDefinitionId(processDefinition.getId());
    verifyQueryResults(query, 4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidDefinitionId() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().processDefinitionId("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.processDefinitionId(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByProcessDefinitionKey() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    JobDefinitionQuery query = managementService.createJobDefinitionQuery().processDefinitionKey(processDefinition.getKey());
    verifyQueryResults(query, 4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidDefinitionKey() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().processDefinitionKey("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.processDefinitionKey(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByJobType() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().jobType(AsyncContinuationJobHandler.TYPE);
    verifyQueryResults(query, 1);

    query = managementService.createJobDefinitionQuery().jobType(TimerStartEventJobHandler.TYPE);
    verifyQueryResults(query, 1);

    query = managementService.createJobDefinitionQuery().jobType(TimerCatchIntermediateEventJobHandler.TYPE);
    verifyQueryResults(query, 1);

    query = managementService.createJobDefinitionQuery().jobType(TimerExecuteNestedActivityJobHandler.TYPE);
    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidJobType() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().jobType("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.jobType(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByInvalidJobConfiguration() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().jobConfiguration("invalid");
    verifyQueryResults(query, 0);
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    try {
      jobDefinitionQuery.jobConfiguration(null);
      fail("A ProcessEngineExcpetion was expected.");
    } catch (ProcessEngineException e) {}
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryByActive() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().active();
    verifyQueryResults(query, 4);

    // suspend first one
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().jobType(AsyncContinuationJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only three active job definitions left
    verifyQueryResults(query, 3);

    // Suspend second one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerStartEventJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only two active job definitions left
    verifyQueryResults(query, 2);

    // suspend third one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerCatchIntermediateEventJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only two active job definitions left
    verifyQueryResults(query, 1);

    // suspend fourth one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerExecuteNestedActivityJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // no one is active
    verifyQueryResults(query, 0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryBySuspended() {
    JobDefinitionQuery query = managementService.createJobDefinitionQuery().suspended();
    verifyQueryResults(query, 0);

    // suspend first one
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().jobType(AsyncContinuationJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only one is suspended
    verifyQueryResults(query, 1);

    // Suspend second one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerStartEventJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only two are suspended
    verifyQueryResults(query, 2);

    // suspend third one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerCatchIntermediateEventJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // only three are suspended
    verifyQueryResults(query, 3);

    // suspend fourth one
    jobDefinition = managementService.createJobDefinitionQuery().jobType(TimerExecuteNestedActivityJobHandler.TYPE).singleResult();
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // all are suspended
    verifyQueryResults(query, 4);
  }

  // Pagination //////////////////////////////////////////////////////////

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryPaging() {
    assertThat(managementService.createJobDefinitionQuery().listPage(0, 4)).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().listPage(2, 1)).hasSize(1);
    assertThat(managementService.createJobDefinitionQuery().listPage(1, 2)).hasSize(2);
    assertThat(managementService.createJobDefinitionQuery().listPage(1, 4)).hasSize(3);
  }

  // Sorting /////////////////////////////////////////////////////////////

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQuerySorting() {
    // asc
    assertThat(managementService.createJobDefinitionQuery().orderByActivityId().asc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobConfiguration().asc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobDefinitionId().asc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobType().asc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByProcessDefinitionId().asc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByProcessDefinitionKey().asc().list()).hasSize(4);

    // desc
    assertThat(managementService.createJobDefinitionQuery().orderByActivityId().desc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobConfiguration().desc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobDefinitionId().desc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByJobType().desc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByProcessDefinitionId().desc().list()).hasSize(4);
    assertThat(managementService.createJobDefinitionQuery().orderByProcessDefinitionKey().desc().list()).hasSize(4);

  }

  @Test
  public void testQueryInvalidSortingUsage() {
    var jobDefinitionQuery = managementService.createJobDefinitionQuery().orderByJobDefinitionId();
    try {
      jobDefinitionQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("call asc() or desc() after using orderByXX()", e.getMessage());
    }

    var jobQuery = managementService.createJobQuery();
    try {
      jobQuery.asc();
      fail("");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("You should call any of the orderBy methods first before specifying a direction", e.getMessage());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/JobDefinitionQueryTest.testBase.bpmn"})
  @Test
  public void testQueryWithOverridingJobPriority() {
    // given
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().listPage(0, 1).get(0);
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // when
    JobDefinition queriedDefinition = managementService.createJobDefinitionQuery().withOverridingJobPriority().singleResult();

    // then
    assertThat(queriedDefinition).isNotNull();
    assertThat(queriedDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat((long) queriedDefinition.getOverridingJobPriority()).isEqualTo(42L);

    // and
    assertThat(managementService.createJobDefinitionQuery().withOverridingJobPriority().count()).isEqualTo(1);

  }

  // Test Helpers ////////////////////////////////////////////////////////

  private void verifyQueryResults(JobDefinitionQuery query, int countExpected) {
    assertThat(query.list()).hasSize(countExpected);
    assertThat(query.count()).isEqualTo(countExpected);

    if (countExpected == 1) {
      assertThat(query.singleResult()).isNotNull();
    } else if (countExpected > 1){
      verifySingleResultFails(query);
    } else if (countExpected == 0) {
      assertThat(query.singleResult()).isNull();
    }
  }

  private void verifySingleResultFails(JobDefinitionQuery query) {
    try {
      query.singleResult();
      fail("");
    } catch (ProcessEngineException e) {}
  }

}
