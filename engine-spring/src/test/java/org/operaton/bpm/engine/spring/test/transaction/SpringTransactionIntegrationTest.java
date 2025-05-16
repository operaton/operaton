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
package org.operaton.bpm.engine.spring.test.transaction;

import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;
import static org.operaton.bpm.engine.test.util.JobExecutorHelper.waitForJobExecutorToProcessAllJobs;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Tom Baeyens
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationTest-context.xml")
class SpringTransactionIntegrationTest extends SpringProcessEngineTestCase {

  @Autowired
  protected UserBean userBean;

  @Autowired
  protected DataSource dataSource;

  private static final long WAIT_TIME_MILLIS = 20000L;
  public static final long INTERVAL_MILLIS = 1000L;

  @Deployment
  @Test
  void basicOperatonSpringIntegration() {
    userBean.hello();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo("Hello from Printer!");
  }

  @Deployment
  @Test
  void rollbackTransactionOnOperatonException() {
    // Create a table that the userBean is supposed to fill with some data
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("create table MY_TABLE (MY_TEXT varchar);");

    // The hello() method will start the process. The process will wait in a user task
    userBean.hello();

    jdbcTemplate.update("insert into MY_TABLE values ('test');");
    Integer results = jdbcTemplate.queryForObject("select count(*) from MY_TABLE", Integer.class);
    assertThat(results).isOne();

    // The completeTask() method will write a record to the 'MY_TABLE' table and complete the user task
    String taskId = taskService.createTaskQuery().singleResult().getId();
    assertThatThrownBy(() -> userBean.completeTask(taskId))
      .isInstanceOf(Exception.class);

    // Since the service task after the user tasks throws an exception, both
    // the record and the process must be rolled back !
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("My Task");
    results = jdbcTemplate.queryForObject("select count(*) from MY_TABLE", Integer.class);
    assertThat(results).isEqualTo(1);

    // Cleanup
    jdbcTemplate.execute("drop table MY_TABLE if exists;");
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationTest.testErrorPropagationOnExceptionInTransaction.bpmn20.xml",
    "org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationTest.throwExceptionProcess.bpmn20.xml"
  })
  @Test
  void errorPropagationOnExceptionInTransaction() {
      runtimeService.startProcessInstanceByKey("process");
      waitForJobExecutorToProcessAllJobs(processEngineConfiguration, WAIT_TIME_MILLIS, INTERVAL_MILLIS);
      Incident incident = runtimeService.createIncidentQuery().activityId("servicetask").singleResult();
      assertThat(incident.getIncidentMessage()).isEqualTo("error");
  }

  @Deployment
  @Test
  void transactionRollbackInServiceTask() {
    runtimeService.startProcessInstanceByKey("txRollbackServiceTask");

    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, WAIT_TIME_MILLIS, INTERVAL_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
    assertThat(job.getExceptionMessage()).isEqualTo("Transaction rolled back because it has been marked as rollback-only");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull().contains("Transaction rolled back because it has been marked as rollback-only");
  }

  @Deployment
  @Test
  void transactionRollbackInServiceTaskWithCustomRetryCycle() {
    runtimeService.startProcessInstanceByKey("txRollbackServiceTaskWithCustomRetryCycle");

    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, WAIT_TIME_MILLIS, INTERVAL_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
    assertThat(job.getExceptionMessage()).isEqualTo("Transaction rolled back because it has been marked as rollback-only");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull().contains("Transaction rolled back because it has been marked as rollback-only");
  }

  @Deployment
  @Test
  void failingTransactionListener() {
    runtimeService.startProcessInstanceByKey("failingTransactionListener");

    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, WAIT_TIME_MILLIS, INTERVAL_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isZero();
    assertThat(job.getExceptionMessage()).isEqualTo("exception in transaction listener");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull().contains("java.lang.RuntimeException: exception in transaction listener");
  }

}
