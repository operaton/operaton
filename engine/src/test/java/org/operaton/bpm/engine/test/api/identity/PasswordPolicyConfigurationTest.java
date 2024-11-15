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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.DefaultPasswordPolicyImpl;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class PasswordPolicyConfigurationTest {

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void init() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    processEngineConfiguration.setPasswordPolicy(null).setEnablePasswordPolicy(false);
  }

  @After
  public void tearDown() {
    processEngineConfiguration.setPasswordPolicy(null).setEnablePasswordPolicy(false);
  }

  @Test
  public void testInitialConfiguration() {
    // given initial configuration

    // when
    processEngineConfiguration.initPasswordPolicy();

    // then
    assertThat(processEngineConfiguration.getPasswordPolicy()).isNull();
    assertThat(processEngineConfiguration.isEnablePasswordPolicy()).isEqualTo(false);
  }

  @Test
  public void testAutoConfigurationDefaultPasswordPolicy() {
    // given

    processEngineConfiguration.setEnablePasswordPolicy(true);

    // when
    processEngineConfiguration.initPasswordPolicy();

    // then
    assertThat(processEngineConfiguration.isEnablePasswordPolicy()).isEqualTo(true);
    assertThat(processEngineConfiguration.getPasswordPolicy()).isInstanceOf(DefaultPasswordPolicyImpl.class);
  }

  @Test
  public void testFullPasswordPolicyConfiguration() {
    // given
    processEngineConfiguration.setEnablePasswordPolicy(true);
    processEngineConfiguration.setPasswordPolicy(new DefaultPasswordPolicyImpl());

    // when
    processEngineConfiguration.initPasswordPolicy();

    // then
    assertThat(processEngineConfiguration.isEnablePasswordPolicy()).isEqualTo(true);
    assertThat(processEngineConfiguration.getPasswordPolicy()).isInstanceOf(DefaultPasswordPolicyImpl.class);
  }
}