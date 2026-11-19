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
package org.operaton.bpm.qa.upgrade.scenarios720.compensation;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.qa.upgrade.Origin;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;
import org.operaton.bpm.qa.upgrade.UpgradeTestRule;

import static org.operaton.bpm.qa.upgrade.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.qa.upgrade.util.ActivityInstanceAssert.describeActivityInstanceTree;

/**
 * @author Thorben Lindhauer
 *
 */
@ScenarioUnderTest("SingleActivityCompensationScenario")
@Origin("7.24.0")
public class SingleActivityCompensationScenarioTest {

  @Rule
  public UpgradeTestRule rule = new UpgradeTestRule();

  @Test
  @ScenarioUnderTest("init.triggerCompensation.3")
  public void testInitTriggerCompensationActivityInstanceTree() {
    // given active compensation
    ProcessInstance instance = rule.processInstance();

    // then the activity instance tree is meaningful
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(instance.getId());
    Assert.assertNotNull(activityInstance);
    assertThat(activityInstance).hasStructure(
        describeActivityInstanceTree(instance.getProcessDefinitionId())
          .activity("throwCompensate")
          .activity("undoTask")
        .done());
  }
}
