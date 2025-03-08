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
package org.operaton.bpm.engine.test.assertions.cmmn;

import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.Failure;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

import org.junit.jupiter.api.Test;

@Deployment(resources = "cmmn/MilestoneAssertIsAvailableTest.cmmn")
class MilestoneAssertIsAvailableTest extends ProcessAssertTestCase {

  @Test
  void is_available_success() {
    CaseInstance caseInstance = caseService().createCaseInstanceByKey("MilestoneAssertIsAvailableTest");

    assertThat(caseInstance).milestone("Milestone").isAvailable();
  }

  @Test
  void is_available_fail() {
    final CaseInstance caseInstance = caseService().createCaseInstanceByKey("MilestoneAssertIsAvailableTest");

    complete(caseExecution("PI_TaskA", caseInstance));

    expect(new Failure() {
      @Override
      public void when() {
        assertThat(caseInstance).milestone("Milestone").isAvailable();
      }
    });
  }
}
