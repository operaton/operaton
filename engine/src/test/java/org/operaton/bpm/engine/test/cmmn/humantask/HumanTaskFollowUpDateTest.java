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
package org.operaton.bpm.engine.test.cmmn.humantask;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Smirnov
 *
 */
class HumanTaskFollowUpDateTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/humantask/HumanTaskFollowUpDateTest.testHumanTaskFollowUpDate.cmmn"})
  @Test
  void testHumanTaskFollowUpDateExtension() throws Exception {

    Date date = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").parse("01-01-2015 12:10:00");
    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", date);

    String caseInstanceId = caseService.createCaseInstanceByKey("case", variables).getId();

    Task task = taskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult();

    assertThat(task.getFollowUpDate()).isNotNull();
    assertThat(task.getFollowUpDate()).isEqualTo(date);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/humantask/HumanTaskFollowUpDateTest.testHumanTaskFollowUpDate.cmmn"})
  @Test
  void testHumanTaskFollowUpDateStringExtension() throws Exception {

    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", "2015-01-01T12:10:00");

    String caseInstanceId = caseService.createCaseInstanceByKey("case", variables).getId();

    Task task = taskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult();

    assertThat(task.getFollowUpDate()).isNotNull();
    Date date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("01-01-2015 12:10:00");
    assertThat(task.getFollowUpDate()).isEqualTo(date);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/humantask/HumanTaskFollowUpDateTest.testHumanTaskFollowUpDate.cmmn"})
  @Test
  void testHumanTaskRelativeFollowUpDate() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("dateVariable", "P2DT2H30M");

    String caseInstanceId = caseService.createCaseInstanceByKey("case", variables).getId();

    Task task = taskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult();

    Date followUpDate = task.getFollowUpDate();
    assertThat(followUpDate).isNotNull();

    Period period = new Period(task.getCreateTime().getTime(), followUpDate.getTime());
    assertThat(period.getDays()).isEqualTo(2);
    assertThat(period.getHours()).isEqualTo(2);
    assertThat(period.getMinutes()).isEqualTo(30);
  }

}
