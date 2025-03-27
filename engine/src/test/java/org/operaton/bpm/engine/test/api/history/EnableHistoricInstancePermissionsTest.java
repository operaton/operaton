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
package org.operaton.bpm.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.HistoricTaskPermissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@ExtendWith(ProcessEngineExtension.class)
public class EnableHistoricInstancePermissionsTest {

  protected ProcessEngineConfigurationImpl config;
  protected AuthorizationService authorizationService;

  @AfterEach
  public void resetConfig() {
    config.setEnableHistoricInstancePermissions(false);
  }

  @Test
  public void shouldBeFalseByDefault() {
    // given

    // when

    // then
    assertThat(config.isEnableHistoricInstancePermissions())
      .isFalse();
  }

  @Test
  public void shouldBeConfiguredToTrue() {
    // given

    // when
    config.setEnableHistoricInstancePermissions(true);

    // then
    assertThat(config.isEnableHistoricInstancePermissions())
        .isTrue();
  }

  @Test
  public void shouldBeConfiguredToFalse() {
    // given

    // when
    config.setEnableHistoricInstancePermissions(false);

    // then
    assertThat(config.isEnableHistoricInstancePermissions())
        .isFalse();
  }

  @Test
  public void shouldThrowExceptionWhenHistoricInstancePermissionsAreDisabled_Task() {
    // given
    config.setEnableHistoricInstancePermissions(false);

    // when/then
    assertThatThrownBy(() -> authorizationService.isUserAuthorized("myUserId", null,
        HistoricTaskPermissions.ALL, Resources.HISTORIC_TASK))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("ENGINE-03090 Historic instance permissions are disabled, " +
          "please check your process engine configuration.");
  }

  @Test
  public void shouldThrowExceptionWhenHistoricInstancePermissionsAreDisabled_ProcessInstance() {
    // given
    config.setEnableHistoricInstancePermissions(false);

    // when/then
    assertThatThrownBy(() -> authorizationService.isUserAuthorized("myUserId", null,
        HistoricTaskPermissions.ALL, Resources.HISTORIC_PROCESS_INSTANCE))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("ENGINE-03090 Historic instance permissions are disabled, " +
          "please check your process engine configuration.");
  }

}
