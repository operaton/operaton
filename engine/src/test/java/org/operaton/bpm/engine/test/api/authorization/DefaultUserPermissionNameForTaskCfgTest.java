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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;

/**
 * @author Daniel Meyer
 *
 */
class DefaultUserPermissionNameForTaskCfgTest {

  @Test
  void updateIsDefaultTaskPermission() {
    assertThat(new StandaloneInMemProcessEngineConfiguration().getDefaultUserPermissionNameForTask()).isEqualTo("UPDATE");
  }

  @Test
  void shouldInitUpdatePermission() {
    TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

    // given
    testProcessEngineCfg.setDefaultUserPermissionNameForTask("UPDATE");

    // if
    testProcessEngineCfg.initDefaultUserPermissionForTask();

    // then
    assertThat(testProcessEngineCfg.getDefaultUserPermissionForTask()).isEqualTo(Permissions.UPDATE);
  }

  @Test
  void shouldInitTaskWorkPermission() {
    TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

    // given
    testProcessEngineCfg.setDefaultUserPermissionNameForTask("TASK_WORK");

    // if
    testProcessEngineCfg.initDefaultUserPermissionForTask();

    // then
    assertThat(testProcessEngineCfg.getDefaultUserPermissionForTask()).isEqualTo(Permissions.TASK_WORK);
  }

  @Test
  void shouldThrowExceptionOnUnsupportedPermission() {
    TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

    // given
    testProcessEngineCfg.setDefaultUserPermissionNameForTask("UNSUPPORTED");

    // if
    assertThatThrownBy(testProcessEngineCfg::initDefaultUserPermissionForTask)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Invalid value '%s' for configuration property 'defaultUserPermissionNameForTask'.".formatted("UNSUPPORTED"));
  }

  @Test
  void shouldThrowExceptionOnNullPermissionName() {
    TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

    // given
    testProcessEngineCfg.setDefaultUserPermissionNameForTask(null);

    // if
    assertThatThrownBy(testProcessEngineCfg::initDefaultUserPermissionForTask)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Invalid value 'null' for configuration property 'defaultUserPermissionNameForTask'.".formatted());
  }

  @Test
  void shouldNotInitIfAlreadySet() {
    TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

    // given
    testProcessEngineCfg.setDefaultUserPermissionForTask(Permissions.ALL);

    // if
    testProcessEngineCfg.initDefaultUserPermissionForTask();

    // then
    assertThat(testProcessEngineCfg.getDefaultUserPermissionForTask()).isEqualTo(Permissions.ALL);
  }

  @Test
  void shouldInitTaskPermission() {
    ProcessEngine engine = null;
    try {
      // if
      final TestProcessEngineCfg testProcessEngineCfg = new TestProcessEngineCfg();

      engine = testProcessEngineCfg.setProcessEngineName("DefaultTaskPermissionsCfgTest-engine")
        .setJdbcUrl("jdbc:h2:mem:%s".formatted("DefaultTaskPermissionsCfgTest-engine-db"))
        .setMetricsEnabled(false)
        .setJobExecutorActivate(false)
        .buildProcessEngine();

      // then
      assertThat(testProcessEngineCfg.initMethodCalled).isTrue();
    } finally {
      if(engine != null) {
        engine.close();
      }
    }
  }

  static class TestProcessEngineCfg extends StandaloneInMemProcessEngineConfiguration {

    boolean initMethodCalled;

    @Override
    public void initDefaultUserPermissionForTask() {
      super.initDefaultUserPermissionForTask();
      initMethodCalled = true;
    }
  }


}
