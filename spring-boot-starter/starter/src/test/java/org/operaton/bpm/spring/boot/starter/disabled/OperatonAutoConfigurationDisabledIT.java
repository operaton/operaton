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
package org.operaton.bpm.spring.boot.starter.disabled;

import org.operaton.bpm.engine.ProcessEngine;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = {OperatonAutoConfigurationDisabledIT.NoOperatonApplication.class},
  webEnvironment = WebEnvironment.NONE,
  properties = {"operaton.bpm.enabled=false"}
)
public class OperatonAutoConfigurationDisabledIT {

  @SpringBootApplication
  public static class NoOperatonApplication {

  }

  @Autowired
  private Optional<ProcessEngine> processEngine;

  @Test
  void processEngineNotConfigured() {
    assertThat(processEngine.isPresent()).isFalse();
  }

}
