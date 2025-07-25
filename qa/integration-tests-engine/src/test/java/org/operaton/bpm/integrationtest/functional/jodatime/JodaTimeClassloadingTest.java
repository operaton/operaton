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
package org.operaton.bpm.integrationtest.functional.jodatime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

@ExtendWith(ArquillianExtension.class)
public class JodaTimeClassloadingTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return initWebArchiveDeployment().addAsResource("org/operaton/bpm/integrationtest/functional/jodatime/JodaTimeClassloadingTest.bpmn20.xml");
  }


  private Date testExpression(String timeExpression) {
    // Set the clock fixed
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("dueDate", timeExpression);

    // After process start, there should be timer created
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
    Assertions.assertEquals(1, managementService.createJobQuery().processInstanceId(pi1.getId()).count());

    List<Job> jobs = managementService.createJobQuery().processDefinitionKey("intermediateTimerEventExample").executable().list();
    Assertions.assertEquals(1, jobs.size());
    runtimeService.deleteProcessInstance(pi1.getId(), "test");

    return jobs.get(0).getDuedate();
  }

  @Test
  void testTimeExpressionComplete() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt));
    Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dueDate));
  }

  @Test
  void testTimeExpressionWithoutSeconds() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt));
    Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dt), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(dueDate));
  }

  @Test
  void testTimeExpressionWithoutMinutes() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(new Date()));
    Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dt), new SimpleDateFormat("yyyy-MM-dd'T'HH").format(dueDate));
  }

  @Test
  void testTimeExpressionWithoutTime() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(dt), new SimpleDateFormat("yyyy-MM-dd").format(dueDate));
  }

  @Test
  void testTimeExpressionWithoutDay() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy-MM").format(new Date()));
    Assertions.assertEquals(new SimpleDateFormat("yyyy-MM").format(dt), new SimpleDateFormat("yyyy-MM").format(dueDate));
  }

  @Test
  void testTimeExpressionWithoutMonth() {
    Date dt = new Date();

    Date dueDate = testExpression(new SimpleDateFormat("yyyy").format(new Date()));
    Assertions.assertEquals(new SimpleDateFormat("yyyy").format(dt), new SimpleDateFormat("yyyy").format(dueDate));
  }

}
