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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;


/**
 * @author Tom Baeyens
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationTest-context.xml")
class SpringTransactionIntegrationTest extends SpringProcessEngineTestCase {

  @Autowired
  protected UserBean userBean;

  @Autowired
  protected DataSource dataSource;

  private static long WAIT_TIME_MILLIS = TimeUnit.MILLISECONDS.convert(20L, TimeUnit.SECONDS);

  @Deployment
  @Test
  void basicActivitiSpringIntegration() {
    userBean.hello();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo("Hello from Printer!");
  }

  @Deployment
  @Test
  void rollbackTransactionOnActivitiException() {

    // Create a table that the userBean is supposed to fill with some data
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("create table MY_TABLE (MY_TEXT varchar);");

    // The hello() method will start the process. The process will wait in a user task
    userBean.hello();

    jdbcTemplate.update("insert into MY_TABLE values ('test');");
    int results = jdbcTemplate.queryForObject("select count(*) from MY_TABLE", Integer.class);
    assertThat(results).isEqualTo(1);

    // The completeTask() method will write a record to the 'MY_TABLE' table and complete the user task
    try {
      userBean.completeTask(taskService.createTaskQuery().singleResult().getId());
      fail("");
    } catch (Exception e) { }

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
      waitForJobExecutorToProcessAllJobs(WAIT_TIME_MILLIS);
      Incident incident = runtimeService.createIncidentQuery().activityId("servicetask").singleResult();
      assertThat(incident.getIncidentMessage(), is("error"));
  }

  @Deployment
  @Test
  void transactionRollbackInServiceTask() {

    runtimeService.startProcessInstanceByKey("txRollbackServiceTask");

    waitForJobExecutorToProcessAllJobs(WAIT_TIME_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isEqualTo(0);
    assertThat(job.getExceptionMessage()).isEqualTo("Transaction rolled back because it has been marked as rollback-only");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull();
    assertTrue(stacktrace.contains("Transaction rolled back because it has been marked as rollback-only"), "unexpected stacktrace, was <" + stacktrace + ">");
  }

  @Deployment
  @Test
  void transactionRollbackInServiceTaskWithCustomRetryCycle() {

    runtimeService.startProcessInstanceByKey("txRollbackServiceTaskWithCustomRetryCycle");

    waitForJobExecutorToProcessAllJobs(WAIT_TIME_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isEqualTo(0);
    assertThat(job.getExceptionMessage()).isEqualTo("Transaction rolled back because it has been marked as rollback-only");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull();
    assertTrue(stacktrace.contains("Transaction rolled back because it has been marked as rollback-only"), "unexpected stacktrace, was <" + stacktrace + ">");
  }

  @Deployment
  @Test
  void failingTransactionListener() {

    runtimeService.startProcessInstanceByKey("failingTransactionListener");

    waitForJobExecutorToProcessAllJobs(WAIT_TIME_MILLIS);

    Job job = managementService.createJobQuery().singleResult();

    assertThat(job).isNotNull();
    assertThat(job.getRetries()).isEqualTo(0);
    assertThat(job.getExceptionMessage()).isEqualTo("exception in transaction listener");

    String stacktrace = managementService.getJobExceptionStacktrace(job.getId());
    assertThat(stacktrace).isNotNull();
    assertTrue(stacktrace.contains("java.lang.RuntimeException: exception in transaction listener"), "unexpected stacktrace, was <" + stacktrace + ">");
  }


}
